package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "preferencia_usuario")
public class PreferenciaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preferencia")
    public Long idPreferencia;

    @OneToOne
    @JoinColumn(name = "id_titular", unique = true)
    public Titular titular;

    @Column(name = "tema", length = 20)
    public String tema;

    @Column(name = "presencia_casa")
    public Boolean presenciaCasa = true;

    @Column(name = "auto_cierre_fuga")
    public Boolean autoCierreFuga = true;

    public PreferenciaUsuario() {
    }

    public PreferenciaUsuario(Titular titular, String tema) {
        this.titular = titular;
        this.tema = tema;
        this.presenciaCasa = true;
        this.autoCierreFuga = true;
    }
}
