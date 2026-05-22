package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_valvula")
public class EstadoValvula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_valvula")
    public Long idEstadoValvula;

    @Column(name = "nombre_estado_valvula", nullable = false, length = 50)
    public String nombreEstadoValvula;

    public EstadoValvula() {
    }

    public EstadoValvula(String nombreEstadoValvula) {
        this.nombreEstadoValvula = nombreEstadoValvula;
    }
}