package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_alerta")
public class TipoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_alerta")
    public Long idTipoAlerta;

    @Column(name = "nombre_tipo_alerta", nullable = false, length = 50)
    public String nombreTipoAlerta;

    public TipoAlerta() {
    }

    public TipoAlerta(String nombreTipoAlerta) {
        this.nombreTipoAlerta = nombreTipoAlerta;
    }
}