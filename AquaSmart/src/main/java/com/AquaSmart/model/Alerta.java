package com.AquaSmart.model;

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
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    public Long idAlerta;

    @Column(name = "fecha", nullable = false)
    public LocalDate fecha;

    @Column(name = "hora", nullable = false)
    public LocalTime hora;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    public String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_alerta", nullable = false)
    public TipoAlerta tipoAlerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_estado", nullable = false)
    public TipoEstado tipoEstado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_medidor", referencedColumnName = "id_medidor", nullable = false)
    public Medidor medidor;

    public Alerta() {
    }

    public Alerta(LocalDate fecha, LocalTime hora, String descripcion, TipoAlerta tipoAlerta, TipoEstado tipoEstado, Medidor medidor) {
        this.fecha = fecha;
        this.hora = hora;
        this.descripcion = descripcion;
        this.tipoAlerta = tipoAlerta;
        this.tipoEstado = tipoEstado;
        this.medidor = medidor;
    }
}