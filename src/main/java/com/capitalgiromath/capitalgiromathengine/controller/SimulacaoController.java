package com.capitalgiromath.capitalgiromathengine.controller;

import com.capitalgiromath.capitalgiromathengine.model.*;
import com.capitalgiromath.capitalgiromathengine.service.CalculoService;
import com.capitalgiromath.capitalgiromathengine.service.CdiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api")
public class SimulacaoController {

    private final CalculoService calculoService;
    private final CdiService cdiService;

    public SimulacaoController(CalculoService calculoService, CdiService cdiService) {
        this.calculoService = calculoService;
        this.cdiService = cdiService;
    }

    @PostMapping("/simular")
    public FluxoResponse simular(@RequestBody ContratoRequest request) {
        return calculoService.simular(request);
    }

    @PostMapping("/antecipacao/desconto")
    public ResponseEntity<?> calcularAntecipacaoComDesconto(
            @RequestBody ContratoRequest request,
            @RequestParam String dataCorteStr) {

        LocalDate dataCorte = LocalDate.parse(dataCorteStr);
        FluxoResponse fluxoCompleto = calculoService.simular(request);

        // Taxa de desconto: CDI real recente + spread
        BigDecimal taxaEfetivaMensal;
        if ("PRE".equalsIgnoreCase(request.getTipoTaxa())) {
            taxaEfetivaMensal = request.getTaxaPre().divide(BigDecimal.valueOf(100));
        } else {
            BigDecimal cdiEfetivo = cdiService.getTaxaEfetivaMensal(dataCorte, request.getPercentCdi());
            BigDecimal spreadDec = request.getSpreadMensal().divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
            taxaEfetivaMensal = cdiEfetivo.add(spreadDec);
        }

        BigDecimal valorTotalParaQuitacao = BigDecimal.ZERO;
        List<Map<String, Object>> parcelasDescontadas = new ArrayList<>();
        BigDecimal saldoContabil = BigDecimal.ZERO;

        for (ParcelaResponse p : fluxoCompleto.getParcelas()) {
            if (p.getDataVencimento().isAfter(dataCorte)) {
                long dias = ChronoUnit.DAYS.between(dataCorte, p.getDataVencimento());
                double meses = dias / 30.0;
                double pmtDouble = p.getPrestacao().doubleValue();
                double vpDouble = pmtDouble / Math.pow(1 + taxaEfetivaMensal.doubleValue(), meses);
                BigDecimal valorPresente = BigDecimal.valueOf(vpDouble).setScale(2, RoundingMode.HALF_UP);
                valorTotalParaQuitacao = valorTotalParaQuitacao.add(valorPresente);
                parcelasDescontadas.add(Map.of(
                        "parcela", p.getNumero(),
                        "vencimentoOriginal", p.getDataVencimento().toString(),
                        "valorOriginal", p.getPrestacao(),
                        "valorPresenteComDesconto", valorPresente));
            } else {
                saldoContabil = p.getSaldoFinal(); // último saldo antes da data de corte
            }
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dataAntecipacao", dataCorteStr);
        resposta.put("saldoDevedorContabil", saldoContabil.setScale(2, RoundingMode.HALF_UP));
        resposta.put("custoTotalLiquidacao", valorTotalParaQuitacao.setScale(2, RoundingMode.HALF_UP));
        resposta.put("economiaJurosFuturos",
                saldoContabil.subtract(valorTotalParaQuitacao).setScale(2, RoundingMode.HALF_UP));
        resposta.put("detalheParcelasFuturas", parcelasDescontadas);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/parcela/vencida")
    public ResponseEntity<?> calcularParcelaVencida(@RequestBody ParcelaVencidaRequest request) {
        long diasAtraso = ChronoUnit.DAYS.between(request.getDataVencimento(), request.getDataPagamento());
        if (diasAtraso <= 0) {
            return ResponseEntity.ok(Map.of("mensagem", "Parcela sem atrasos acumulados."));
        }

        BigDecimal valorOriginal = request.getValorPrestacao();
        BigDecimal fatorMulta = request.getPercentualMulta().divide(BigDecimal.valueOf(100), MathContext.DECIMAL128);
        BigDecimal multa = valorOriginal.multiply(fatorMulta).setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxaMoraMensalDec = request.getTaxaMoraMensal().divide(BigDecimal.valueOf(100),
                MathContext.DECIMAL128);
        BigDecimal taxaMoraDiaria = taxaMoraMensalDec.divide(BigDecimal.valueOf(30), 8, RoundingMode.HALF_UP);
        BigDecimal jurosMora = valorOriginal.multiply(taxaMoraDiaria).multiply(BigDecimal.valueOf(diasAtraso))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = valorOriginal.add(multa).add(jurosMora);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("diasEmAtraso", diasAtraso);
        resultado.put("valorOriginal", valorOriginal);
        resultado.put("multaAplicada", multa);
        resultado.put("jurosMoraAcumulado", jurosMora);
        resultado.put("totalComEncargos", total.setScale(2, RoundingMode.HALF_UP));
        return ResponseEntity.ok(resultado);
    }
}