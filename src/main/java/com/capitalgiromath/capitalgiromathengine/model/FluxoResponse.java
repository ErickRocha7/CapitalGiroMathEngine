package com.capitalgiromath.capitalgiromathengine.model;

import java.math.BigDecimal;
import java.util.List;

public class FluxoResponse {
    private List<ParcelaResponse> parcelas;
    private BigDecimal cet; // Custo Efetivo Total (anual %)

    public List<ParcelaResponse> getParcelas() {
        return parcelas;
    }

    public void setParcelas(List<ParcelaResponse> parcelas) {
        this.parcelas = parcelas;
    }

    public BigDecimal getCet() {
        return cet;
    }

    public void setCet(BigDecimal cet) {
        this.cet = cet;
    }
}