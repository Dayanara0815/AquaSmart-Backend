package com.AquaSmart.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "medidor")
public class Medidor {

    @Id
    @Column(name = "id_medidor", length = 50)
    public String idMedidor;

    @Column(name = "fecha_instalacion", nullable = false)
    public LocalDate fechaInstalacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_titular", nullable = false)
    public Titular titular;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_valvula", nullable = false)
    public EstadoValvula estadoValvula;

    public Medidor() {
    }

    public Medidor(String idMedidor, LocalDate fechaInstalacion, Titular titular, EstadoValvula estadoValvula) {
        this.idMedidor = idMedidor;
        this.fechaInstalacion = fechaInstalacion;
        this.titular = titular;
        this.estadoValvula = estadoValvula;
    }
}