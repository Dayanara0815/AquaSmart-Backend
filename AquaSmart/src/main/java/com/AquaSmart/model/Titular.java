package com.AquaSmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "titular")
public class Titular {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_titular")
    public Long idTitular;

    @Column(name = "nombre_titular", nullable = false, length = 100)
    public String nombreTitular;

    @Column(name = "apellido_paterno", nullable = false, length = 100)
    public String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, length = 100)
    public String apellidoMaterno;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    public String correo;

    @Column(name = "edad", nullable = false)
    public Integer edad;

    @Column(name = "celular", length = 15)
    public String celular;

    @Column(name = "rol", length = 50)
    public String rol;

    @Column(name = "foto_perfil", columnDefinition = "TEXT")
    public String fotoPerfil;

    @Column(name = "contrasena", length = 100)
    public String contrasena;

    public Titular() {
    }

    public Titular(String nombreTitular, String apellidoPaterno, String apellidoMaterno, String correo, Integer edad, String celular, String rol) {
        this.nombreTitular = nombreTitular;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correo = correo;
        this.edad = edad;
        this.celular = celular;
        this.rol = rol;
    }

    public Titular(String nombreTitular, String apellidoPaterno, String apellidoMaterno, String correo, Integer edad, String celular, String rol, String contrasena) {
        this.nombreTitular = nombreTitular;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correo = correo;
        this.edad = edad;
        this.celular = celular;
        this.rol = rol;
        this.contrasena = contrasena;
    }
}