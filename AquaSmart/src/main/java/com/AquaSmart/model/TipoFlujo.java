package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_flujo")
public class TipoFlujo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_flujo")
    public Long idTipoFlujo;

    @Column(name = "nombre_tipo_flujo", nullable = false, length = 50)
    public String nombreTipoFlujo;

    public TipoFlujo() {
    }

    public TipoFlujo(String nombreTipoFlujo) {
        this.nombreTipoFlujo = nombreTipoFlujo;
    }
}