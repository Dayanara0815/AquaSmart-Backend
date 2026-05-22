package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_factura_mensual")
public class EstadoFacturaMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_factura_mensual")
    public Long idEstadoFacturaMensual;

    @Column(name = "nombre_estado_factura_mensual", nullable = false, length = 50)
    public String nombreEstadoFacturaMensual;

    public EstadoFacturaMensual() {
    }

    public EstadoFacturaMensual(String nombreEstadoFacturaMensual) {
        this.nombreEstadoFacturaMensual = nombreEstadoFacturaMensual;
    }
}