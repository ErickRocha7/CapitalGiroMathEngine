package com.capitalgiromath.capitalgiromathengine.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CdiService {

    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private static final MathContext MC = MathContext.DECIMAL128;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Map<LocalDate, BigDecimal> cacheCdiAnual = new ConcurrentHashMap<>();

    private static final Set<LocalDate> FERIADOS = Set.of(
            LocalDate.of(2026, 1, 1), // Confraternização Universal
            LocalDate.of(2026, 2, 16), // Carnaval
            LocalDate.of(2026, 2, 17), // Carnaval
            LocalDate.of(2026, 4, 3), // Sexta-feira Santa
            LocalDate.of(2026, 4, 21), // Tiradentes
            LocalDate.of(2026, 5, 1), // Dia do Trabalho
            LocalDate.of(2026, 6, 4), // Corpus Christi
            LocalDate.of(2026, 9, 7), // Independência do Brasil
            LocalDate.of(2026, 10, 12), // Nossa Senhora Aparecida
            LocalDate.of(2026, 11, 2), // Finados
            LocalDate.of(2026, 11, 15), // Proclamação da República
            LocalDate.of(2026, 11, 20), // Dia da Consciência Negra
            LocalDate.of(2026, 12, 25) // Natal
    );

    public CdiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.bcb.gov.br")
                .build();
        this.restTemplate = new RestTemplate();
    }

    public boolean isDiaUtil(LocalDate data) {
        DayOfWeek ds = data.getDayOfWeek();
        return !(ds == DayOfWeek.SATURDAY || ds == DayOfWeek.SUNDAY || FERIADOS.contains(data));
    }

    public LocalDate ajustarProximoDiaUtil(LocalDate data) {
        LocalDate temp = data;
        while (!isDiaUtil(temp)) {
            temp = temp.plusDays(1);
        }
        return temp;
    }

    public BigDecimal getFatorDiario(LocalDate data, BigDecimal percentualCdi) {
        BigDecimal cdiAnual = cacheCdiAnual.getOrDefault(data, BigDecimal.valueOf(10.50));
        BigDecimal taxaAnualContratual = cdiAnual
                .divide(BigDecimal.valueOf(100), MC)
                .multiply(percentualCdi.divide(BigDecimal.valueOf(100), MC), MC);
        BigDecimal base = BigDecimal.ONE.add(taxaAnualContratual, MC);
        return BigDecimal.valueOf(Math.pow(base.doubleValue(), 1.0 / 252.0));
    }

    public BigDecimal getTaxaEfetivaMensal(LocalDate dataBase, BigDecimal percentualCdi) {
        BigDecimal fatorAcum = BigDecimal.ONE;
        int diasUteis = 0;
        LocalDate cursor = dataBase;
        while (diasUteis < 21 && cursor.isAfter(dataBase.minusDays(45))) {
            if (isDiaUtil(cursor)) {
                fatorAcum = fatorAcum.multiply(getFatorDiario(cursor, percentualCdi), MC);
                diasUteis++;
            }
            cursor = cursor.minusDays(1);
        }
        if (diasUteis == 0)
            return BigDecimal.valueOf(0.0085);
        return fatorAcum.subtract(BigDecimal.ONE, MC);
    }

    @Scheduled(cron = "0 0 20 * * MON-FRI", zone = "America/Sao_Paulo")
    public void carregarSerieCdi() {
        // Intervalo de datas: últimos 252 dias úteis (aproximadamente 1 ano)
        LocalDate dataFinal = LocalDate.now();
        LocalDate dataInicial = dataFinal.minusDays(365); // 1 ano corrido garante >= 252 dias úteis

        String dataInicialStr = dataInicial.format(FORMATTER);
        String dataFinalStr = dataFinal.format(FORMATTER);

        // Primeira tentativa com WebClient
        try {
            List<Map<String, String>> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/dados/serie/bcdata.sgs.12/dados")
                            .queryParam("formato", "json")
                            .queryParam("dataInicial", dataInicialStr)
                            .queryParam("dataFinal", dataFinalStr)
                            .build())
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, String>>>() {
                    })
                    .block();

            if (response != null && !response.isEmpty()) {
                atualizarCache(response);
                return;
            }
        } catch (Exception e) {
            System.err.println("Tentativa 1 (WebClient) falhou: " + e.getMessage());
        }

        // Segunda tentativa com RestTemplate (fallback síncrono)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<List<Map<String, String>>> resp = restTemplate.exchange(
                    "https://api.bcb.gov.br/dados/serie/bcdata.sgs.12/dados?formato=json&dataInicial=" + dataInicialStr
                            + "&dataFinal=" + dataFinalStr,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, String>>>() {
                    });
            List<Map<String, String>> body = resp.getBody();
            if (body != null && !body.isEmpty()) {
                atualizarCache(body);
                System.out.println("Cache CDI atualizado via RestTemplate.");
            }
        } catch (Exception e) {
            System.err.println("Tentativa 2 (RestTemplate) também falhou: " + e.getMessage());
        }
    }

    private void atualizarCache(List<Map<String, String>> registros) {
        for (Map<String, String> registro : registros) {
            LocalDate data = LocalDate.parse(registro.get("data"), FORMATTER);
            BigDecimal valor = new BigDecimal(registro.get("valor"));
            cacheCdiAnual.put(data, valor);
        }
        System.out.println("Cache CDI atualizado: " + cacheCdiAnual.size() + " registros.");
    }
}