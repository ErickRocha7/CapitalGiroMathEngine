package com.capitalgiromath.capitalgiromathengine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParcelaResponse {
    private int numero;
    private LocalDate dataVencimento;
    private BigDecimal saldoInicial;
    private BigDecimal juros;
    private BigDecimal amortizacao;
    private BigDecimal prestacao;
    private BigDecimal saldoFinal;
    private BigDecimal fatorCdiPeriodo; // null para pré-fixado

    public ParcelaResponse(int numero, LocalDate dataVencimento, BigDecimal saldoInicial,
            BigDecimal juros, BigDecimal amortizacao, BigDecimal prestacao,
            BigDecimal saldoFinal, BigDecimal fatorCdiPeriodo) {
        this.numero = numero;
        this.dataVencimento = dataVencimento;
        this.saldoInicial = saldoInicial;
        this.juros = juros;
        this.amortizacao = amortizacao;
        this.prestacao = prestacao;
        this.saldoFinal = saldoFinal;
        this.fatorCdiPeriodo = fatorCdiPeriodo;
    }

    // Getters e Setters
    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getJuros() {
        return juros;
    }

    public void setJuros(BigDecimal juros) {
        this.juros = juros;
    }

    public BigDecimal getAmortizacao() {
        return amortizacao;
    }

    public void setAmortizacao(BigDecimal amortizacao) {
        this.amortizacao = amortizacao;
    }

    public BigDecimal getPrestacao() {
        return prestacao;
    }

    public void setPrestacao(BigDecimal prestacao) {
        this.prestacao = prestacao;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public BigDecimal getFatorCdiPeriodo() {
        return fatorCdiPeriodo;
    }

    public void setFatorCdiPeriodo(BigDecimal fatorCdiPeriodo) {
        this.fatorCdiPeriodo = fatorCdiPeriodo;
    }
}