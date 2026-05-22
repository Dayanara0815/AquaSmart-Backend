package com.AquaSmart.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "factura_mensual")
public class FacturaMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura_mensual")
    public Long idFacturaMensual;

    @Column(name = "periodo_facturado", nullable = false, length = 7)
    public String periodoFacturado;

    @Column(name = "consumo_bruto", nullable = false, precision = 10, scale = 2)
    public BigDecimal consumoBruto;

    @Column(name = "volumen_aire_descontado", nullable = false, precision = 10, scale = 2)
    public BigDecimal volumenAireDescontado;

    @Column(name = "consumo_real_neto", nullable = false, precision = 10, scale = 2)
    public BigDecimal consumoRealNeto;

    @Column(name = "monto_pagar", nullable = false, precision = 10, scale = 2)
    public BigDecimal montoPagar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_factura_mensual", nullable = false)
    public EstadoFacturaMensual estadoFacturaMensual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medidor", referencedColumnName = "id_medidor", nullable = false)
    public Medidor medidor;

    public FacturaMensual() {
    }

    public FacturaMensual(String periodoFacturado, BigDecimal consumoBruto, BigDecimal volumenAireDescontado, BigDecimal consumoRealNeto, BigDecimal montoPagar, EstadoFacturaMensual estadoFacturaMensual, Medidor medidor) {
        this.periodoFacturado = periodoFacturado;
        this.consumoBruto = consumoBruto;
        this.volumenAireDescontado = volumenAireDescontado;
        this.consumoRealNeto = consumoRealNeto;
        this.montoPagar = montoPagar;
        this.estadoFacturaMensual = estadoFacturaMensual;
        this.medidor = medidor;
    }
}