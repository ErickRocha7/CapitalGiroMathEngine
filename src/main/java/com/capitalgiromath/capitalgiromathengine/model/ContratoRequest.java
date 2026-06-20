package com.capitalgiromath.capitalgiromathengine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ContratoRequest {
    private BigDecimal principal;
    private int prazoMeses;
    private String sistema; // "SAC" ou "PRICE"
    private String tipoTaxa; // "PRE" ou "POS"
    private BigDecimal taxaPre; // ex.: 2.0 (%)
    private BigDecimal percentCdi; // ex.: 110.0
    private BigDecimal spreadMensal; // ex.: 1.5 (%)
    private boolean iofFinanciado;
    private BigDecimal iofTotal;
    private LocalDate dataLiberacao;

    // Getters e Setters
    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public int getPrazoMeses() {
        return prazoMeses;
    }

    public void setPrazoMeses(int prazoMeses) {
        this.prazoMeses = prazoMeses;
    }

    public String getSistema() {
        return sistema;
    }

    public void setSistema(String sistema) {
        this.sistema = sistema;
    }

    public String getTipoTaxa() {
        return tipoTaxa;
    }

    public void setTipoTaxa(String tipoTaxa) {
        this.tipoTaxa = tipoTaxa;
    }

    public BigDecimal getTaxaPre() {
        return taxaPre;
    }

    public void setTaxaPre(BigDecimal taxaPre) {
        this.taxaPre = taxaPre;
    }

    public BigDecimal getPercentCdi() {
        return percentCdi;
    }

    public void setPercentCdi(BigDecimal percentCdi) {
        this.percentCdi = percentCdi;
    }

    public BigDecimal getSpreadMensal() {
        return spreadMensal;
    }

    public void setSpreadMensal(BigDecimal spreadMensal) {
        this.spreadMensal = spreadMensal;
    }

    public boolean isIofFinanciado() {
        return iofFinanciado;
    }

    public void setIofFinanciado(boolean iofFinanciado) {
        this.iofFinanciado = iofFinanciado;
    }

    public BigDecimal getIofTotal() {
        return iofTotal;
    }

    public void setIofTotal(BigDecimal iofTotal) {
        this.iofTotal = iofTotal;
    }

    public LocalDate getDataLiberacao() {
        return dataLiberacao;
    }

    public void setDataLiberacao(LocalDate dataLiberacao) {
        this.dataLiberacao = dataLiberacao;
    }
}