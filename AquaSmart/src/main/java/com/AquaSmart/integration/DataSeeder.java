package com.AquaSmart.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

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
    private final jakarta.persistence.EntityManager entityManager;

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
            PreferenciaRepository preferenciaRepository,
            jakarta.persistence.EntityManager entityManager) {
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
        this.entityManager = entityManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedIfNeeded() {
        // Forzar sembrado fresco en cada inicio para que las fechas sean relativas a hoy
        /*
        if (titularRepository.count() >= 4 && lecturaConsumoRepository.count() >= 50) {
            return;
        }
        */

        // Limpieza preventiva total usando queries nativas en PostgreSQL con CASCADE para evitar violaciones de clave foránea
        entityManager.createNativeQuery("TRUNCATE TABLE lectura_consumo RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE factura_mensual RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE alerta RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE preferencia_usuario RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE medidor RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE titular RESTART IDENTITY CASCADE").executeUpdate();

        entityManager.createNativeQuery("TRUNCATE TABLE estado_factura_mensual RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE estado_valvula RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE tipo_alerta RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE tipo_estado RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE tipo_flujo RESTART IDENTITY CASCADE").executeUpdate();

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
        List<LecturaConsumo> lecturas = new ArrayList<>();

        // Generar 30 días de historial de consumo
        for (int i = 30; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // Consumo Doméstico (María Fernanda)
            double volDom = 90.0 + Math.random() * 50.0;
            TipoFlujo flDom = normal;
            // Forzar algunas anomalías
            if (i == 3 || i == 12 || i == 21 || i == 27) {
                volDom = 230.0 + Math.random() * 50.0;
                flDom = anomalo;
            } else if (i == 1 || i == 15) {
                volDom = 190.0 + Math.random() * 20.0;
                flDom = aire;
            }
            lecturas.add(new LecturaConsumo(date, LocalTime.of(8, 0), BigDecimal.valueOf(volDom), flDom, medidor));

            // Consumo Comercial (Luis Condori)
            double volCom = 220.0 + Math.random() * 80.0;
            TipoFlujo flCom = normal;
            // Forzar algunas anomalías
            if (i == 4 || i == 11 || i == 18 || i == 26) {
                volCom = 490.0 + Math.random() * 80.0;
                flCom = anomalo;
            } else if (i == 2 || i == 16) {
                volCom = 380.0 + Math.random() * 30.0;
                flCom = aire;
            }
            lecturas.add(new LecturaConsumo(date, LocalTime.of(9, 0), BigDecimal.valueOf(volCom), flCom, medidorComercio));
        }
        lecturaConsumoRepository.saveAll(lecturas);

        // Alertas
        List<Alerta> alertasList = new ArrayList<>(List.of(
                new Alerta(today.minusDays(1), LocalTime.of(3, 15), "Posible fuga silenciosa detectada durante la madrugada.", fuga, activa, medidor),
                new Alerta(today.minusDays(2), LocalTime.of(22, 40), "Caída de presión reportada por la red vecinal.", corte, activa, medidor),
                new Alerta(today.minusDays(3), LocalTime.of(6, 5), "Evento de paso de aire sin fuga persistente.", aireAlert, cerradaEstado, medidor),
                new Alerta(today.minusDays(1), LocalTime.of(4, 20), "Consumo comercial atípico en horas de la madrugada.", fuga, activa, medidorComercio)
        ));

        // Sembrar alertas viales municipales distribuidas para el calendario
        alertasList.add(new Alerta(today.minusDays(20), LocalTime.of(10, 30), "Fuga reportada en la vía pública: Calle Los Próceres - Cdra 4 (Resuelta, Asfaltado Completo)", fuga, cerradaEstado, medidor));
        alertasList.add(new Alerta(today.minusDays(12), LocalTime.of(14, 15), "Fuga reportada en la vía pública: Av. Buenos Aires - Lote 12 (En Proceso)", fuga, tipoEstadoRepository.save(new TipoEstado("En Proceso")), medidorComercio));
        alertasList.add(new Alerta(today.minusDays(4), LocalTime.of(9, 45), "Fuga reportada en la vía pública: Jr. Tacna 890 (Pendiente, Fuga Mediana)", fuga, activa, medidor));
        alertasList.add(new Alerta(today.plusDays(3), LocalTime.of(8, 0), "Mantenimiento programado: Corte preventivo de red de matriz (Programado)", corte, activa, medidor));

        alertaRepository.saveAll(alertasList);

        facturaMensualRepository.saveAll(List.of(
                new FacturaMensual("2026-05", BigDecimal.valueOf(1045.00), BigDecimal.valueOf(214.90), BigDecimal.valueOf(830.10), BigDecimal.valueOf(4.15), emitida, medidor),
                new FacturaMensual("2026-04", BigDecimal.valueOf(980.00), BigDecimal.valueOf(120.00), BigDecimal.valueOf(860.00), BigDecimal.valueOf(4.30), observada, medidor),
                
                // Facturas para medidorComercio
                new FacturaMensual("2026-05", BigDecimal.valueOf(2022.50), BigDecimal.valueOf(450.60), BigDecimal.valueOf(1571.90), BigDecimal.valueOf(7.86), emitida, medidorComercio),
                new FacturaMensual("2026-04", BigDecimal.valueOf(1800.00), BigDecimal.valueOf(300.00), BigDecimal.valueOf(1500.00), BigDecimal.valueOf(7.50), observada, medidorComercio)
        ));
    }
}