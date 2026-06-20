package com.capitalgiromath.capitalgiromathengine.service;

import com.capitalgiromath.capitalgiromathengine.model.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class CalculoService {

    private final CdiService cdiService;
    private static final MathContext MC = MathContext.DECIMAL128;

    public CalculoService(CdiService cdiService) {
        this.cdiService = cdiService;
    }

    public FluxoResponse simular(ContratoRequest req) {
        if ("PRE".equalsIgnoreCase(req.getTipoTaxa())) {
            return simularPre(req);
        } else {
            return simularPos(req);
        }
    }

    private FluxoResponse simularPre(ContratoRequest req) {
        BigDecimal saldoInicial = req.getPrincipal().add(req.isIofFinanciado() ? req.getIofTotal() : BigDecimal.ZERO);
        BigDecimal taxa = req.getTaxaPre().divide(BigDecimal.valueOf(100), MC);
        int n = req.getPrazoMeses();
        List<ParcelaResponse> parcelas = new ArrayList<>();

        if ("SAC".equalsIgnoreCase(req.getSistema())) {
            BigDecimal amort = saldoInicial.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            BigDecimal saldo = saldoInicial;
            for (int i = 1; i <= n; i++) {
                BigDecimal juros = saldo.multiply(taxa).setScale(2, RoundingMode.HALF_UP);
                BigDecimal prestacao = amort.add(juros);
                BigDecimal saldoFinal = (i == n) ? BigDecimal.ZERO : saldo.subtract(amort);
                parcelas.add(new ParcelaResponse(i, req.getDataLiberacao().plusMonths(i),
                        saldo, juros, amort, prestacao, saldoFinal, null));
                saldo = saldoFinal;
            }
        } else { // PRICE
            double baseDouble = BigDecimal.ONE.add(taxa).doubleValue();
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.valueOf(Math.pow(baseDouble, -n)));
            BigDecimal prestacao = saldoInicial.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_UP);

            BigDecimal saldo = saldoInicial;
            for (int i = 1; i <= n; i++) {
                BigDecimal juros = saldo.multiply(taxa).setScale(2, RoundingMode.HALF_UP);
                BigDecimal amort = (i == n) ? saldo : prestacao.subtract(juros);
                BigDecimal prestacaoFinal = (i == n) ? amort.add(juros) : prestacao;
                BigDecimal saldoFinal = (i == n) ? BigDecimal.ZERO : saldo.subtract(amort);
                parcelas.add(new ParcelaResponse(i, req.getDataLiberacao().plusMonths(i),
                        saldo, juros, amort, prestacaoFinal, saldoFinal, null));
                saldo = saldoFinal;
            }
        }

        BigDecimal cet = calcularCet(req.getPrincipal(), parcelas, req.getIofTotal(), req.isIofFinanciado());
        FluxoResponse resp = new FluxoResponse();
        resp.setParcelas(parcelas);
        resp.setCet(cet);
        return resp;
    }

    private FluxoResponse simularPos(ContratoRequest req) {
        BigDecimal saldoInicial = req.getPrincipal().add(req.isIofFinanciado() ? req.getIofTotal() : BigDecimal.ZERO);
        int n = req.getPrazoMeses();
        List<ParcelaResponse> parcelas = new ArrayList<>();

        BigDecimal spreadDecimal = req.getSpreadMensal().divide(BigDecimal.valueOf(100), MC);
        BigDecimal saldo = saldoInicial;
        LocalDate dataBaseAnterior = req.getDataLiberacao();

        for (int i = 1; i <= n; i++) {
            LocalDate vencOriginal = req.getDataLiberacao().plusMonths(i);
            LocalDate dataVenc = cdiService.ajustarProximoDiaUtil(vencOriginal);
            int parcelasRestantes = n - i + 1;

            // Acumula fator CDI diário no período (dias úteis)
            BigDecimal fatorCdiAcum = BigDecimal.ONE;
            LocalDate cursor = dataBaseAnterior.plusDays(1);
            while (!cursor.isAfter(dataVenc)) {
                if (cdiService.isDiaUtil(cursor)) {
                    BigDecimal fd = cdiService.getFatorDiario(cursor, req.getPercentCdi());
                    fatorCdiAcum = fatorCdiAcum.multiply(fd, MC);
                }
                cursor = cursor.plusDays(1);
            }

            BigDecimal fatorTotalPeriodo = fatorCdiAcum.multiply(BigDecimal.ONE.add(spreadDecimal, MC), MC)
                    .subtract(BigDecimal.ONE, MC);
            BigDecimal juros = saldo.multiply(fatorTotalPeriodo).setScale(2, RoundingMode.HALF_UP);

            BigDecimal amort, prestacao;
            if (parcelasRestantes == 1) {
                prestacao = saldo.add(juros);
                amort = saldo;
            } else if ("SAC".equalsIgnoreCase(req.getSistema())) {
                amort = saldoInicial.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
                prestacao = amort.add(juros);
            } else { // PRICE recalculada
                double base = BigDecimal.ONE.add(fatorTotalPeriodo).doubleValue();
                BigDecimal denom = BigDecimal.ONE.subtract(BigDecimal.valueOf(Math.pow(base, -parcelasRestantes)));
                prestacao = saldo.multiply(fatorTotalPeriodo).divide(denom, 2, RoundingMode.HALF_UP);
                amort = prestacao.subtract(juros);
            }

            BigDecimal saldoFinal = (i == n) ? BigDecimal.ZERO : saldo.subtract(amort);
            parcelas.add(new ParcelaResponse(i, dataVenc, saldo, juros, amort, prestacao, saldoFinal, fatorCdiAcum));
            saldo = saldoFinal;
            dataBaseAnterior = dataVenc;
        }

        BigDecimal cet = calcularCet(req.getPrincipal(), parcelas, req.getIofTotal(), req.isIofFinanciado());
        FluxoResponse resp = new FluxoResponse();
        resp.setParcelas(parcelas);
        resp.setCet(cet);
        return resp;
    }

    private BigDecimal calcularCet(BigDecimal principal, List<ParcelaResponse> parcelas,
            BigDecimal iofTotal, boolean iofFinanciado) {
        double fluxoInicial = principal.doubleValue() - (iofFinanciado ? 0 : iofTotal.doubleValue());
        double x0 = 0.02;
        double erro = 1e-7;
        int maxIter = 100;

        for (int iter = 0; iter < maxIter; iter++) {
            double f = -fluxoInicial;
            double df = 0;
            for (ParcelaResponse p : parcelas) {
                int t = p.getNumero();
                double pmt = p.getPrestacao().doubleValue();
                f += pmt / Math.pow(1 + x0, t);
                df -= (t * pmt) / Math.pow(1 + x0, t + 1);
            }
            double x1 = x0 - f / df;
            if (Math.abs(x1 - x0) < erro) {
                double cetAnual = (Math.pow(1 + x1, 12) - 1) * 100;
                return BigDecimal.valueOf(cetAnual).setScale(2, RoundingMode.HALF_UP);
            }
            x0 = x1;
        }
        return BigDecimal.ZERO;
    }
}