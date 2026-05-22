package com.AquaSmart.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.AquaSmart.dto.AlertDto;
import com.AquaSmart.dto.AiProjectionDto;
import com.AquaSmart.dto.DashboardStatusDto;
import com.AquaSmart.model.Alerta;
import com.AquaSmart.model.FacturaMensual;
import com.AquaSmart.model.LecturaConsumo;
import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.Titular;
import com.AquaSmart.repository.AlertaRepository;
import com.AquaSmart.repository.FacturaMensualRepository;
import com.AquaSmart.repository.LecturaConsumoRepository;
import com.AquaSmart.repository.MedidorRepository;
import com.AquaSmart.repository.TitularRepository;

@Component
public class MockAquaSmartIntegration implements AquaSmartIntegration {

    private final TitularRepository titularRepository;
    private final LecturaConsumoRepository lecturaConsumoRepository;
    private final AlertaRepository alertaRepository;
    private final FacturaMensualRepository facturaMensualRepository;
    private final MedidorRepository medidorRepository;

    public MockAquaSmartIntegration(
            TitularRepository titularRepository,
            LecturaConsumoRepository lecturaConsumoRepository,
            AlertaRepository alertaRepository,
            FacturaMensualRepository facturaMensualRepository,
            MedidorRepository medidorRepository) {
        this.titularRepository = titularRepository;
        this.lecturaConsumoRepository = lecturaConsumoRepository;
        this.alertaRepository = alertaRepository;
        this.facturaMensualRepository = facturaMensualRepository;
        this.medidorRepository = medidorRepository;
    }

    @Override
    public DashboardStatusDto fallbackStatus(boolean isHome) {
        return new DashboardStatusDto(
                "Óptimo",
                120.0,
                0.60,
                0.0,
                true,
                isHome,
                "Hace 5 minutos",
                fallbackAlerts().get(0),
                fallbackProjection());
    }

    @Override
    public AiProjectionDto fallbackProjection() {
        return new AiProjectionDto(
                110.0,
                45,
                30,
                8,
                8,
                22,
                "Tu patrón de consumo indica una posible fuga silenciosa durante la madrugada. Solicitamos revisar la válvula de baño. Esto podría inflar tu recibo en S/. 23.50 adicionales si no se corrige.");
    }

    @Override
    public List<AlertDto> fallbackAlerts() {
        return List.of(new AlertDto(
                true,
                "AVISO DE CORTE DE AGUA",
                "Mañana de 8:00 a.m. - 2:00 p.m.",
                "Corte preventivo",
                "Activa",
                "Posible corte programado en la zona.",
                "Hace 10 minutos"));
    }

    @Override
    public String answerQuestion(String question) {
        if (question == null || question.isBlank()) {
            return "Escribe una consulta para analizar el consumo de agua.";
        }

        String q = question.toLowerCase().trim();

        // 1. Obtener datos del usuario titular
        Optional<Titular> tOpt = titularRepository.findAll().stream().findFirst();
        String nombre = tOpt.map(titular -> titular.nombreTitular).orElse("María Fernanda");

        // 2. Obtener datos del medidor
        Optional<Medidor> mOpt = medidorRepository.findAll().stream().findFirst();
        String medidorId = mOpt.map(medidor -> medidor.idMedidor).orElse("ASM-2048");
        boolean valvulaAbierta = mOpt.map(med -> med.estadoValvula != null && "Abierta".equalsIgnoreCase(med.estadoValvula.nombreEstadoValvula)).orElse(true);

        // 3. Obtener alertas
        List<Alerta> alertas = alertaRepository.findAllWithDetails();
        long alertasActivasCount = alertas.stream()
                .filter(a -> a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado))
                .count();

        // 4. Obtener consumo acumulado semanal y diario
        List<LecturaConsumo> lecturas = lecturaConsumoRepository.findAll();
        double totalLiters = lecturas.stream()
                .filter(l -> l.volumenRegistrado != null)
                .mapToDouble(l -> l.volumenRegistrado.doubleValue())
                .sum();
        double todayLiters = lecturas.stream()
                .filter(l -> l.fecha != null && LocalDate.now().equals(l.fecha) && l.volumenRegistrado != null)
                .mapToDouble(l -> l.volumenRegistrado.doubleValue())
                .sum();

        // 5. Obtener última factura y proyecciones
        List<FacturaMensual> facturas = facturaMensualRepository.findAll();
        FacturaMensual ultimaFactura = facturas.stream()
                .max(Comparator.comparing(f -> f.periodoFacturado == null ? "" : f.periodoFacturado))
                .orElse(null);
        double montoRecibo = ultimaFactura != null && ultimaFactura.montoPagar != null ? ultimaFactura.montoPagar.doubleValue() : 4.15;
        double aireDescontado = ultimaFactura != null && ultimaFactura.volumenAireDescontado != null ? ultimaFactura.volumenAireDescontado.doubleValue() : 214.9;
        double ahorroAire = aireDescontado * 0.005; // 5 soles por m3 = 0.005 soles por litro

        // Respuestas según intenciones analizadas por palabras clave
        if (q.contains("hola") || q.contains("buenos") || q.contains("buenas") || q.contains("quien eres") || q.contains("ayuda")) {
            return "¡Hola, " + nombre + "! Soy tu asistente virtual de AquaSmart. "
                    + "Estoy aquí para ayudarte a monitorear el consumo de tu medidor **" + medidorId + "**, analizar tus recibos, alertarte de fugas y explicarte cómo la IA te ayuda a ahorrar agua. ¿En qué te puedo asesorar hoy?";
        }

        if (q.contains("fuga") || q.contains("escape") || q.contains("perder") || q.contains("perdiendo")) {
            Optional<Alerta> fugaActiva = alertas.stream()
                    .filter(a -> a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                            && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta))
                    .findFirst();

            if (fugaActiva.isPresent()) {
                return "¡Atención, " + nombre + "! He detectado una **alerta activa de fuga silenciosa** en tu medidor **" + medidorId + "**. "
                        + "La alerta se registró el día " + fugaActiva.get().fecha + " a las " + fugaActiva.get().hora + ". "
                        + "El flujo anómalo detectado durante la madrugada indica que hay pérdida de agua continua. "
                        + "Te recomiendo verificar las llaves de paso, inodoros y duchas. Si sales de casa, recuerda que puedes **cerrar la válvula principal** desde la sección de control de esta app.";
            } else {
                return "Buenas noticias, " + nombre + ". No he detectado ninguna fuga de agua activa en tu red en las últimas 48 horas. "
                        + "El flujo actual del medidor es estable y los análisis de consumo nocturno no muestran anomalías.";
            }
        }

        if (q.contains("consumo") || q.contains("gasto") || q.contains("agua") || q.contains("litros") || q.contains("cuanto he usado") || q.contains("cuanto use")) {
            return "He analizado el consumo de agua del medidor **" + medidorId + "**. Hoy has consumido **" 
                    + String.format("%.2f", todayLiters) + " litros** de agua. "
                    + "En la última semana tu consumo acumulado es de **" + String.format("%.2f", totalLiters) + " litros**. "
                    + "Actualmente la válvula principal está **" + (valvulaAbierta ? "Abierta" : "Cerrada") + "**. "
                    + (valvulaAbierta && todayLiters > 150 ? "\n\nTu consumo de hoy está ligeramente por encima del promedio residencial. ¡Un consumo consciente ayuda al planeta!" : "\n\nTu nivel de consumo se mantiene dentro del rango óptimo residencial.");
        }

        if (q.contains("alerta") || q.contains("notificacion") || q.contains("problema") || q.contains("aviso")) {
            if (alertasActivasCount > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Hola, ").append(nombre).append(". Tienes **").append(alertasActivasCount).append(" alertas activas** en tu sistema:\n\n");
                alertas.stream()
                    .filter(a -> a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado))
                    .forEach(a -> sb.append("- **").append(a.tipoAlerta != null ? a.tipoAlerta.nombreTipoAlerta : "Alerta").append("**: ").append(a.descripcion).append(" (Registrado: ").append(a.fecha).append(" a las ").append(a.hora).append(")\n"));
                sb.append("\nTe recomiendo tomar medidas preventivas o cerrar la válvula si es necesario.");
                return sb.toString();
            } else {
                return "Hola, " + nombre + ". Todo está en orden. No tienes alertas activas en tu medidor **" + medidorId + "** actualmente. El último evento registrado ya fue solucionado y cerrado.";
            }
        }

        if (q.contains("valvula") || q.contains("cerrar") || q.contains("abrir") || q.contains("corte") || q.contains("control")) {
            return "La válvula principal de tu medidor **" + medidorId + "** se encuentra **" + (valvulaAbierta ? "ABIERTA" : "CERRADA") + "**. "
                    + (valvulaAbierta 
                        ? "Si detectas una fuga o estarás fuera de casa por varios días, puedes cerrarla de forma remota presionando el botón 'Cerrar Válvula' en el panel de control."
                        : "La válvula está cortando el flujo de agua hacia la vivienda. Puedes presionar 'Reabrir Válvula' para restaurar el suministro de agua.");
        }

        if (q.contains("recibo") || q.contains("pagar") || q.contains("costo") || q.contains("factura") || q.contains("precio") || q.contains("proyeccion") || q.contains("ahorro") || q.contains("aire")) {
            return "Hola, " + nombre + ". Para el periodo de facturación actual de **" + (ultimaFactura != null ? ultimaFactura.periodoFacturado : "2026-05") + "**, "
                    + "la proyección de tu recibo es de **S/. " + String.format("%.2f", montoRecibo) + "**.\n\n"
                    + "El sistema inteligente de AquaSmart ha detectado un volumen de **" + String.format("%.1f", aireDescontado) + " litros de paso de aire** "
                    + "en la tubería. Este aire **no se te cobrará**, lo que representa un **ahorro de S/. " + String.format("%.2f", ahorroAire) + "** en tu recibo de este mes gracias a nuestro filtro digital de aire. "
                    + "El consumo real neto de agua facturado es de **" + (ultimaFactura != null ? ultimaFactura.consumoRealNeto : "830.1") + " litros**.";
        }

        // Respuesta inteligente por defecto
        return "Entiendo tu consulta, " + nombre + ". Con respecto a tu medidor **" + medidorId + "**, "
                + "actualmente registras un consumo de **" + String.format("%.1f", todayLiters) + " litros hoy**, "
                + "la válvula principal está **" + (valvulaAbierta ? "Abierta" : "Cerrada") + "** y tienes **" 
                + alertasActivasCount + " alertas activas**. "
                + "¿Te gustaría que analice en detalle algún tema como fugas, tu consumo semanal o la proyección de tu recibo?";
    }
}