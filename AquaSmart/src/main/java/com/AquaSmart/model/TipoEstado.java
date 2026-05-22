package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_estado")
public class TipoEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_estado")
    public Long idTipoEstado;

    @Column(name = "nombre_tipo_estado", nullable = false, length = 50)
    public String nombreTipoEstado;

    public TipoEstado() {
    }

    public TipoEstado(String nombreTipoEstado) {
        this.nombreTipoEstado = nombreTipoEstado;
    }
}