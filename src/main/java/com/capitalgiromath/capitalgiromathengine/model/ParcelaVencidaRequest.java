package com.capitalgiromath.capitalgiromathengine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParcelaVencidaRequest {
    private BigDecimal valorPrestacao;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private BigDecimal percentualMulta = new BigDecimal("2.0");
    private BigDecimal taxaMoraMensal = new BigDecimal("1.0");

    public BigDecimal getValorPrestacao() {
        return valorPrestacao;
    }

    public void setValorPrestacao(BigDecimal valorPrestacao) {
        this.valorPrestacao = valorPrestacao;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public BigDecimal getPercentualMulta() {
        return percentualMulta != null ? percentualMulta : new BigDecimal("2.0");
    }

    public void setPercentualMulta(BigDecimal percentualMulta) {
        this.percentualMulta = percentualMulta;
    }

    public BigDecimal getTaxaMoraMensal() {
        return taxaMoraMensal != null ? taxaMoraMensal : new BigDecimal("1.0");
    }

    public void setTaxaMoraMensal(BigDecimal taxaMoraMensal) {
        this.taxaMoraMensal = taxaMoraMensal;
    }
}