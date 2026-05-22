package com.AquaSmart.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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
@Table(name = "lectura_consumo")
public class LecturaConsumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lectura")
    public Long idLectura;

    @Column(name = "fecha", nullable = false)
    public LocalDate fecha;

    @Column(name = "hora", nullable = false)
    public LocalTime hora;

    @Column(name = "volumen_registrado", nullable = false, precision = 10, scale = 4)
    public BigDecimal volumenRegistrado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_flujo", nullable = false)
    public TipoFlujo tipoFlujo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medidor", referencedColumnName = "id_medidor", nullable = false)
    public Medidor medidor;

    public LecturaConsumo() {
    }

    public LecturaConsumo(LocalDate fecha, LocalTime hora, BigDecimal volumenRegistrado, TipoFlujo tipoFlujo, Medidor medidor) {
        this.fecha = fecha;
        this.hora = hora;
        this.volumenRegistrado = volumenRegistrado;
        this.tipoFlujo = tipoFlujo;
        this.medidor = medidor;
    }
}