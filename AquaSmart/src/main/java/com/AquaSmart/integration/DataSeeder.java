package com.AquaSmart.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.AquaSmart.model.Alerta;
import com.AquaSmart.model.EstadoFacturaMensual;
import com.AquaSmart.model.EstadoValvula;
import com.AquaSmart.model.FacturaMensual;
import com.AquaSmart.model.LecturaConsumo;
import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.Titular;
import com.AquaSmart.model.TipoAlerta;
import com.AquaSmart.model.TipoEstado;
import com.AquaSmart.model.TipoFlujo;
import com.AquaSmart.repository.AlertaRepository;
import com.AquaSmart.repository.EstadoFacturaMensualRepository;
import com.AquaSmart.repository.EstadoValvulaRepository;
import com.AquaSmart.repository.FacturaMensualRepository;
import com.AquaSmart.repository.LecturaConsumoRepository;
import com.AquaSmart.repository.MedidorRepository;
import com.AquaSmart.repository.TitularRepository;
import com.AquaSmart.repository.TipoAlertaRepository;
import com.AquaSmart.repository.TipoEstadoRepository;
import com.AquaSmart.repository.TipoFlujoRepository;

import com.AquaSmart.repository.PreferenciaRepository;

@Component
public class DataSeeder {

    private final TitularRepository titularRepository;
    private final EstadoValvulaRepository estadoValvulaRepository;
    private final TipoFlujoRepository tipoFlujoRepository;
    private final TipoAlertaRepository tipoAlertaRepository;
    private final TipoEstadoRepository tipoEstadoRepository;
    private final EstadoFacturaMensualRepository estadoFacturaMensualRepository;
    private final MedidorRepository medidorRepository;
    private final LecturaConsumoRepository lecturaConsumoRepository;
    private final AlertaRepository alertaRepository;
    private final FacturaMensualRepository facturaMensualRepository;
    private final PreferenciaRepository preferenciaRepository;

    public DataSeeder(
            TitularRepository titularRepository,
            EstadoValvulaRepository estadoValvulaRepository,
            TipoFlujoRepository tipoFlujoRepository,
            TipoAlertaRepository tipoAlertaRepository,
            TipoEstadoRepository tipoEstadoRepository,
            EstadoFacturaMensualRepository estadoFacturaMensualRepository,
            MedidorRepository medidorRepository,
            LecturaConsumoRepository lecturaConsumoRepository,
            AlertaRepository alertaRepository,
            FacturaMensualRepository facturaMensualRepository,
            PreferenciaRepository preferenciaRepository) {
        this.titularRepository = titularRepository;
        this.estadoValvulaRepository = estadoValvulaRepository;
        this.tipoFlujoRepository = tipoFlujoRepository;
        this.tipoAlertaRepository = tipoAlertaRepository;
        this.tipoEstadoRepository = tipoEstadoRepository;
        this.estadoFacturaMensualRepository = estadoFacturaMensualRepository;
        this.medidorRepository = medidorRepository;
        this.lecturaConsumoRepository = lecturaConsumoRepository;
        this.alertaRepository = alertaRepository;
        this.facturaMensualRepository = facturaMensualRepository;
        this.preferenciaRepository = preferenciaRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfNeeded() {
        if (titularRepository.count() >= 4) {
            return;
        }

        // Limpieza preventiva para evitar colisiones y asegurar sembrado fresco
        alertaRepository.deleteAll();
        facturaMensualRepository.deleteAll();
        lecturaConsumoRepository.deleteAll();
        preferenciaRepository.deleteAll();
        medidorRepository.deleteAll();
        titularRepository.deleteAll();

        // Limpiar también catálogos para evitar duplicados al re-sembrar
        estadoFacturaMensualRepository.deleteAll();
        estadoValvulaRepository.deleteAll();
        tipoAlertaRepository.deleteAll();
        tipoEstadoRepository.deleteAll();
        tipoFlujoRepository.deleteAll();

        // Forzar la ejecución inmediata de las eliminaciones en PostgreSQL
        titularRepository.flush();

        EstadoValvula abierta = estadoValvulaRepository.save(new EstadoValvula("Abierta"));
        EstadoValvula cerrada = estadoValvulaRepository.save(new EstadoValvula("Cerrada"));

        TipoFlujo normal = tipoFlujoRepository.save(new TipoFlujo("Normal"));
        TipoFlujo anomalo = tipoFlujoRepository.save(new TipoFlujo("Anómalo"));
        TipoFlujo aire = tipoFlujoRepository.save(new TipoFlujo("Paso de aire"));

        TipoAlerta fuga = tipoAlertaRepository.save(new TipoAlerta("Fuga"));
        TipoAlerta corte = tipoAlertaRepository.save(new TipoAlerta("Corte de agua"));
        TipoAlerta aireAlert = tipoAlertaRepository.save(new TipoAlerta("Paso de aire"));

        TipoEstado activa = tipoEstadoRepository.save(new TipoEstado("Activa"));
        TipoEstado cerradaEstado = tipoEstadoRepository.save(new TipoEstado("Cerrada"));

        EstadoFacturaMensual emitida = estadoFacturaMensualRepository.save(new EstadoFacturaMensual("Emitida"));
        EstadoFacturaMensual observada = estadoFacturaMensualRepository.save(new EstadoFacturaMensual("Observada"));

        Titular titular = titularRepository.save(new Titular(
                "María Fernanda", "Quispe", "Rojas", "maria.quispe@example.com", 34, "987654321", "DOMESTICO"));

        Titular comercio = titularRepository.save(new Titular(
                "Luis", "Condori", "Mendoza", "luis.condori@example.com", 38, "987654322", "COMERCIO"));

        Titular tecnico = titularRepository.save(new Titular(
                "Carlos", "Mendoza", "Ramos", "carlos.mendoza@example.com", 47, "987654323", "TECNICO"));

        Titular municipal = titularRepository.save(new Titular(
                "Alexis", "Maza", "Lozada", "alexis.maza@example.com", 28, "987654324", "MUNICIPAL"));

        Medidor medidor = medidorRepository.save(new Medidor(
                "ASM-2048", LocalDate.now().minusMonths(8), titular, abierta));

        Medidor medidorComercio = medidorRepository.save(new Medidor(
                "ASM-LAVANDERIA", LocalDate.now().minusMonths(6), comercio, abierta));

        LocalDate today = LocalDate.now();
        List<LecturaConsumo> lecturas = List.of(
                new LecturaConsumo(today.minusDays(6), LocalTime.of(7, 15), BigDecimal.valueOf(135.40), normal, medidor),
                new LecturaConsumo(today.minusDays(5), LocalTime.of(7, 20), BigDecimal.valueOf(128.10), normal, medidor),
                new LecturaConsumo(today.minusDays(4), LocalTime.of(7, 10), BigDecimal.valueOf(141.80), normal, medidor),
                new LecturaConsumo(today.minusDays(3), LocalTime.of(7, 05), BigDecimal.valueOf(149.60), normal, medidor),
                new LecturaConsumo(today.minusDays(2), LocalTime.of(7, 25), BigDecimal.valueOf(162.30), anomalo, medidor),
                new LecturaConsumo(today.minusDays(1), LocalTime.of(3, 12), BigDecimal.valueOf(214.90), aire, medidor),
                new LecturaConsumo(today, LocalTime.of(8, 30), BigDecimal.valueOf(123.75), normal, medidor));
        lecturaConsumoRepository.saveAll(lecturas);

        alertaRepository.saveAll(List.of(
                new Alerta(today.minusDays(1), LocalTime.of(3, 15), "Posible fuga silenciosa detectada durante la madrugada.", fuga, activa, medidor),
                new Alerta(today.minusDays(2), LocalTime.of(22, 40), "Caída de presión reportada por la red vecinal.", corte, activa, medidor),
                new Alerta(today.minusDays(3), LocalTime.of(6, 05), "Evento de paso de aire sin fuga persistente.", aireAlert, cerradaEstado, medidor)));

        facturaMensualRepository.saveAll(List.of(
                new FacturaMensual("2026-05", BigDecimal.valueOf(1045.00), BigDecimal.valueOf(214.90), BigDecimal.valueOf(830.10), BigDecimal.valueOf(4.15), emitida, medidor),
                new FacturaMensual("2026-04", BigDecimal.valueOf(980.00), BigDecimal.valueOf(120.00), BigDecimal.valueOf(860.00), BigDecimal.valueOf(4.30), observada, medidor)));
    }
}