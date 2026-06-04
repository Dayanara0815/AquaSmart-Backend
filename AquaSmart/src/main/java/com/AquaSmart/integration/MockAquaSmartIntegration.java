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
import com.AquaSmart.dto.ChatMessageDto;
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
    private final com.AquaSmart.repository.PreferenciaRepository preferenciaRepository;

    public MockAquaSmartIntegration(
            TitularRepository titularRepository,
            LecturaConsumoRepository lecturaConsumoRepository,
            AlertaRepository alertaRepository,
            FacturaMensualRepository facturaMensualRepository,
            MedidorRepository medidorRepository,
            com.AquaSmart.repository.PreferenciaRepository preferenciaRepository) {
        this.titularRepository = titularRepository;
        this.lecturaConsumoRepository = lecturaConsumoRepository;
        this.alertaRepository = alertaRepository;
        this.facturaMensualRepository = facturaMensualRepository;
        this.medidorRepository = medidorRepository;
        this.preferenciaRepository = preferenciaRepository;
    }

    @Override
    public DashboardStatusDto fallbackStatus(boolean isHome) {
        long count = medidorRepository.count();
        return new DashboardStatusDto(
                "Óptimo",
                120.0,
                0.60,
                0.0,
                true,
                isHome,
                true,
                "Hace 5 minutos",
                fallbackAlerts().get(0),
                fallbackProjection(),
                count);
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
                1L,
                true,
                "AVISO DE CORTE DE AGUA",
                "Mañana de 8:00 a.m. - 2:00 p.m.",
                "Corte preventivo",
                "Activa",
                "Posible corte programado en la zona.",
                "Hace 10 minutos"));
    }

    @Override
    public String answerQuestion(String question, String email, List<ChatMessageDto> history) {
        if (question == null || question.isBlank()) {
            return "Escribe una consulta para analizar el consumo de agua.";
        }

        String q = question.toLowerCase().trim();

        // 1. Obtener datos del usuario titular según email
        Optional<Titular> tOpt = Optional.empty();
        if (email != null && !email.isBlank()) {
            tOpt = titularRepository.findAll().stream()
                    .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                    .findFirst();
        }
        if (tOpt.isEmpty()) {
            tOpt = titularRepository.findAll().stream().findFirst();
        }
        String nombre = tOpt.map(titular -> titular.nombreTitular).orElse("María Fernanda");
        String rol = tOpt.map(titular -> titular.rol).orElse("DOMESTICO");

        // 2. Obtener datos del medidor del titular
        Optional<Medidor> mOpt = Optional.empty();
        if (tOpt.isPresent()) {
            mOpt = medidorRepository.findByTitular(tOpt.get());
        }
        if (mOpt.isEmpty()) {
            mOpt = medidorRepository.findAllWithDetails().stream().findFirst();
        }
        String medidorId = mOpt.map(medidor -> medidor.idMedidor).orElse("ASM-2048");
        boolean valvulaAbierta = mOpt.map(med -> med.estadoValvula != null && "Abierta".equalsIgnoreCase(med.estadoValvula.nombreEstadoValvula)).orElse(true);

        // 3. Obtener alertas específicas del medidor
        final String finalMedidorIdForAlerts = medidorId;
        List<Alerta> alertas = alertaRepository.findAllWithDetails().stream()
                .filter(a -> a.medidor != null && finalMedidorIdForAlerts.equalsIgnoreCase(a.medidor.idMedidor))
                .toList();
        long alertasActivasCount = alertas.stream()
                .filter(a -> a.tipoEstado != null && ("Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado) || "Pendiente".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)))
                .count();

        StringBuilder alertsBuilder = new StringBuilder();
        if (alertas.isEmpty()) {
            alertsBuilder.append("- No hay alertas registradas en PostgreSQL.\n");
        } else {
            alertas.forEach(a -> {
                String aType = a.tipoAlerta != null ? a.tipoAlerta.nombreTipoAlerta : "Evento";
                String aState = a.tipoEstado != null ? a.tipoEstado.nombreTipoEstado : "Activa";
                alertsBuilder.append(String.format("- [%s] Alerta #%s (%s): %s el %s a las %s\n", 
                        aState, a.idAlerta, aType, a.descripcion, a.fecha, a.hora));
            });
        }
        String alertsListStr = alertsBuilder.toString();

        // 4. Obtener consumo acumulado semanal y diario del medidor específico
        List<LecturaConsumo> lecturas = lecturaConsumoRepository.findAll().stream()
                .filter(l -> l.medidor != null && finalMedidorIdForAlerts.equalsIgnoreCase(l.medidor.idMedidor))
                .toList();
        double totalLiters = lecturas.stream()
                .filter(l -> l.volumenRegistrado != null)
                .mapToDouble(l -> l.volumenRegistrado.doubleValue())
                .sum();
        double todayLiters = lecturas.stream()
                .filter(l -> l.fecha != null && LocalDate.now().equals(l.fecha) && l.volumenRegistrado != null)
                .mapToDouble(l -> l.volumenRegistrado.doubleValue())
                .sum();

        // 5. Obtener proyecciones dinámicas desde PostgreSQL utilizando las lecturas del medidor
        double leakLiters = 0.0;
        double airLiters = 0.0;

        for (LecturaConsumo l : lecturas) {
            if (l.volumenRegistrado != null) {
                double vol = l.volumenRegistrado.doubleValue();
                if (l.tipoFlujo != null) {
                    if ("Anómalo".equalsIgnoreCase(l.tipoFlujo.nombreTipoFlujo)) {
                        leakLiters += vol;
                    } else if ("Paso de aire".equalsIgnoreCase(l.tipoFlujo.nombreTipoFlujo)) {
                        airLiters += vol;
                    }
                }
            }
        }

        long distinctDays = lecturas.stream()
                .map(l -> l.fecha)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        if (distinctDays < 1) {
            distinctDays = 7;
        }

        double averageLitersPerDay = totalLiters / distinctDays;

        double aiEstimate = averageLitersPerDay * 30;

        double montoRecibo = aiEstimate * 0.005;
        montoRecibo = Math.round(montoRecibo * 100.0) / 100.0;

        // Si no hay lecturas de aire registradas en la base de datos, usamos el fallback de la factura
        double aireDescontado = airLiters > 0.0 ? airLiters : 214.9;
        double ahorroAire = aireDescontado * 0.005; // 5 soles por m3 = 0.005 soles por litro

        // 6. Preparar System Prompt para la IA
        boolean isHome = getHomePresence(email);
        String systemPrompt = String.format(
            "Eres AquaBot, el asistente inteligente de AquaSmart para la gestión del agua y telemetría en vivo.\n" +
            "Tu tono es profesional, empático, claro e instructivo. Te comunicas con respuestas fluidas y naturales en español, comportándote como un LLM avanzado.\n\n" +
            "CONTEXTO ACTUAL DEL MEDIDOR Y USUARIO CONECTADO:\n" +
            "- Usuario: %s\n" +
            "- Rol del usuario: %s\n" +
            "- Código del Medidor: %s\n" +
            "- Presencia en casa del Usuario: %s\n" +
            "- Estado de la Válvula Principal: %s\n" +
            "- Consumo acumulado de HOY: %.2f litros\n" +
            "- Consumo acumulado de la ÚLTIMA SEMANA: %.2f litros\n" +
            "- Cantidad de alertas activas en PostgreSQL: %d\n" +
            "- Alertas de base de datos actuales:\n%s\n" +
            "- Proyección del recibo mensual: S/. %.2f\n" +
            "- Aire filtrado descontado: %.1f litros (Ahorro de S/. %.2f)\n\n" +
            "REGLAS OPERATIVAS Y DE COMPORTAMIENTO:\n" +
            "1. ADAPTA TU RESPUESTA AL ROL: \n" +
            "   - Si el rol es 'DOMESTICO', enfócate en consejos de ahorro del hogar, cómo evitar cobros excesivos por aire y alertar ante posibles fugas domésticas detectadas.\n" +
            "   - Si el rol es 'COMERCIO', prioriza la protección del suministro para la lavandería comercial, cómo evitar daños a bombas bloqueando el paso de sedimentos/aire y la estabilidad del caudal comercial.\n" +
            "   - Si el rol es 'TECNICO', asístelo de forma altamente técnica. Puedes hablar de transductores de presión en bar, caudales de flujo, estados de órdenes de trabajo JIRA (Pendiente, En Proceso, En Revisión, Cumplido, Fallado), y cómo auditar fugas en la base de datos.\n" +
            "   - Si el rol es 'MUNICIPAL', asístelo en la vigilancia distrital, correlación de presión cero de las manzanas, reporte de baches y fugas viales en la base de datos.\n" +
            "2. Usa el historial de chat para mantener el hilo conversacional. Si te preguntan '¿y mi válvula?' tras preguntar por el consumo, recuerda de qué venían hablando.\n" +
            "3. NUNCA inventes números que entren en contradicción con el CONTEXTO ACTUAL DE LA TELEMETRÍA provisto arriba.\n" +
            "4. Sé conversacional, responde en párrafos limpios, estructurados con viñetas markdown simples si es necesario.",
            nombre, rol, medidorId, isHome ? "En casa" : "Fuera de casa", valvulaAbierta ? "Abierta (Fluyendo)" : "Cerrada (Bloqueado)", todayLiters, totalLiters, alertasActivasCount, alertsListStr, montoRecibo, aireDescontado, ahorroAire
        );

        // 7. Intentar llamar a Gemini API si hay API Key disponible
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("gemini.api.key", "");
        }

        if (apiKey != null && !apiKey.isBlank()) {
            return callGeminiApi(apiKey, systemPrompt, history, question);
        }

        // 8. Fallback inteligente y conversacional basado en roles (si no hay API Key)
        String welcomePhrase = "Hola " + nombre + " (" + rol + "). ";
        
        if (q.contains("hola") || q.contains("buenos") || q.contains("buenas") || q.contains("quien eres") || q.contains("ayuda")) {
            if ("TECNICO".equalsIgnoreCase(rol)) {
                return welcomePhrase + "Soy tu consola de asistencia técnica de AquaBot. Estoy listo para ayudarte a auditar las lecturas de los medidores, verificar transductores de presión, o gestionar las alertas operativas activas de Puente Piedra en PostgreSQL. ¿Qué sensor deseas auditar hoy?";
            } else if ("MUNICIPAL".equalsIgnoreCase(rol)) {
                return welcomePhrase + "Asistente de Vigilancia Urbana reportándose. Puedo ayudarte a revisar la correlación de presión cero en las manzanas del distrito, monitorear fugas en vía pública, o registrar nuevos baches viales en la base de datos. ¿Qué reporte distrital necesitas analizar?";
            } else if ("COMERCIO".equalsIgnoreCase(rol)) {
                return welcomePhrase + "Bienvenido a tu consola de protección comercial AquaSmart. Estoy monitoreando la entrada de tu línea comercial para asegurar el correcto flujo y evitar el quemado de bombas por aire atrapado tras cortes. ¿Deseas revisar tu consumo comercial o el estado del filtro?";
            } else {
                return welcomePhrase + "Soy tu asistente virtual AquaBot. Estoy aquí para analizar tu consumo doméstico del medidor **" + medidorId + "**, alertarte de fugas y mostrarte cómo la IA te ayuda a descontar el aire acumulado de tus recibos. ¿En qué te puedo asesorar hoy?";
            }
        }

        if (q.contains("fuga") || q.contains("escape") || q.contains("perder") || q.contains("perdiendo")) {
            Optional<Alerta> fugaActiva = alertas.stream()
                    .filter(a -> a.tipoEstado != null && "Activa".equalsIgnoreCase(a.tipoEstado.nombreTipoEstado)
                            && a.tipoAlerta != null && "Fuga".equalsIgnoreCase(a.tipoAlerta.nombreTipoAlerta))
                    .findFirst();

            if (fugaActiva.isPresent()) {
                if ("TECNICO".equalsIgnoreCase(rol)) {
                    return "🛠️ **Incidencia Operativa Detectada**: Se encuentra registrada la fuga #" + fugaActiva.get().idAlerta + " en el medidor **" + medidorId + "**. "
                            + "El reporte indica pérdidas continuas. Puedes ver esta orden de trabajo en tu panel de JIRA en la base de datos para iniciar la reparación remota o en campo.";
                }
                return "🚨 ¡Atención, " + nombre + "! He detectado una **alerta activa de fuga silenciosa** en tu medidor **" + medidorId + "**. "
                        + "Registrada el día " + fugaActiva.get().fecha + " a las " + fugaActiva.get().hora + ". "
                        + "El flujo continuo indica pérdida activa de agua. Te recomiendo verificar las llaves de paso o cerrar la válvula principal desde tu panel de control si vas a salir.";
            } else {
                return "Buenas noticias, " + nombre + ". Los sensores de tu medidor **" + medidorId + "** no registran flujos continuos anómalos o fugas activas en este momento. El consumo nocturno permanece en cero.";
            }
        }

        if (q.contains("consumo") || q.contains("gasto") || q.contains("agua") || q.contains("litros") || q.contains("cuanto he usado") || q.contains("cuanto use")) {
            String valState = valvulaAbierta ? "Abierta" : "Cerrada";
            return "Analizando telemetría del medidor **" + medidorId + "**:\n\n"
                    + "- **Consumo de hoy**: " + String.format("%.2f", todayLiters) + " litros.\n"
                    + "- **Consumo de la última semana**: " + String.format("%.2f", totalLiters) + " litros.\n"
                    + "- **Estado de válvula**: " + valState + ".\n\n"
                    + ("TECNICO".equalsIgnoreCase(rol) ? "Los transductores reportan caudal estable. Recuerda auditar las lecturas en PostgreSQL ante discrepancias." : 
                      ("COMERCIO".equalsIgnoreCase(rol) ? "Tu patrón de consumo comercial se mantiene dentro de los parámetros de producción óptimos." : 
                       "Tu consumo de hoy está " + (todayLiters > 150 ? "ligeramente por encima de la media residencial." : "dentro del rango óptimo residencial.")));
        }

        if (q.contains("valvula") || q.contains("cerrar") || q.contains("abrir") || q.contains("corte") || q.contains("control")) {
            return "El estado de la válvula principal del medidor **" + medidorId + "** se encuentra **" + (valvulaAbierta ? "ABIERTA" : "CERRADA") + "**. "
                    + (valvulaAbierta 
                        ? "Puedes cerrarla remotamente desde la app en caso de fugas o si vas a salir de casa por seguridad."
                        : "El suministro está suspendido. Si ya finalizó el mantenimiento, puedes volver a abrir la válvula desde el panel.");
        }

        if (q.contains("casa") || q.contains("presencia") || q.contains("hogar") || q.contains("encuentra")) {
            return "El estado de presencia configurado para tu cuenta es **" + (isHome ? "EN CASA" : "FUERA DE CASA") + "**. "
                    + (isHome
                        ? "La simulación de consumo adaptativo está activa para flujos residenciales normales. Si sales de casa, puedes desactivar la presencia para activar alertas preventivas ante flujos menores."
                        : "Dado que te encuentras fuera de casa, cualquier caudal de flujo detectado mayor a cero será catalogado como posible fuga o anomalía por la IA.");
        }

        if (q.contains("recibo") || q.contains("pagar") || q.contains("costo") || q.contains("factura") || q.contains("precio") || q.contains("proyeccion") || q.contains("ahorro") || q.contains("aire")) {
            return "Estado financiero de tu medidor **" + medidorId + "**:\n\n"
                    + "- **Proyección de recibo**: S/. " + String.format("%.2f", montoRecibo) + ".\n"
                    + "- **Aire filtrado y descontado**: " + String.format("%.1f", aireDescontado) + " litros.\n"
                    + "- **Ahorro total este mes**: S/. " + String.format("%.2f", ahorroAire) + ".\n\n"
                    + "Gracias a los algoritmos inteligentes de AquaSmart, el paso de aire residual no se factura a tu cuenta, garantizando cobros justos.";
        }

        // Respuesta conversacional basada en contexto histórico
        if (history != null && history.size() > 1) {
            ChatMessageDto lastUserMsg = history.stream().filter(m -> "user".equalsIgnoreCase(m.sender())).reduce((f, s) -> s).orElse(null);
            if (lastUserMsg != null && lastUserMsg.text().toLowerCase().contains("consumo")) {
                return "Respecto a ese consumo de **" + String.format("%.1f", todayLiters) + " litros** que mencionamos, ¿te gustaría que te ayude a calcular la proyección a fin de mes o a revisar si hay alertas?";
            }
        }

        return "Entendido tu consulta, " + nombre + ". Telemetría actual de **" + medidorId + "**: consumo de **" 
                + String.format("%.1f", todayLiters) + " litros hoy**, válvula **" + (valvulaAbierta ? "Abierta" : "Cerrada") + "** y **" 
                + alertasActivasCount + " alertas activas**. ¿En qué otro detalle de AquaSmart te puedo ayudar?";
    }

    private String callGeminiApi(String apiKey, String systemPrompt, List<ChatMessageDto> history, String currentQuestion) {
        try {
            java.net.URL url = new java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            StringBuilder json = new StringBuilder();
            json.append("{");
            
            // System Instruction
            json.append("\"systemInstruction\": {");
            json.append("\"parts\": [{ \"text\": ").append(escapeJson(systemPrompt)).append(" }]");
            json.append("},");
            
            // Contents (History + current question)
            json.append("\"contents\": [");
            boolean first = true;
            if (history != null) {
                for (ChatMessageDto msg : history) {
                    if (msg.sender() == null || msg.text() == null || msg.text().isBlank()) continue;
                    // Ignorar saludos iniciales del bot en el historial para evitar bucles
                    if (msg.text().contains("Asistente virtual") && msg.text().contains("AquaSmart")) continue;
                    
                    if (!first) json.append(",");
                    String role = "user".equalsIgnoreCase(msg.sender()) ? "user" : "model";
                    json.append("{");
                    json.append("\"role\": \"").append(role).append("\",");
                    json.append("\"parts\": [{ \"text\": ").append(escapeJson(msg.text())).append(" }]");
                    json.append("}");
                    first = false;
                }
            }
            if (!first) json.append(",");
            json.append("{");
            json.append("\"role\": \"user\",");
            json.append("\"parts\": [{ \"text\": ").append(escapeJson(currentQuestion)).append(" }]");
            json.append("}");
            
            json.append("]"); // close contents
            json.append("}"); // close root

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                    String respStr = response.toString();
                    int textIndex = respStr.indexOf("\"text\":");
                    if (textIndex != -1) {
                        int startQuote = respStr.indexOf("\"", textIndex + 7);
                        if (startQuote != -1) {
                            StringBuilder result = new StringBuilder();
                            boolean escaped = false;
                            for (int i = startQuote + 1; i < respStr.length(); i++) {
                                char c = respStr.charAt(i);
                                if (escaped) {
                                    if (c == 'n') result.append('\n');
                                    else if (c == 't') result.append('\t');
                                    else result.append(c);
                                    escaped = false;
                                } else if (c == '\\') {
                                    escaped = true;
                                } else if (c == '\"') {
                                    break;
                                } else {
                                    result.append(c);
                                }
                            }
                            return result.toString();
                        }
                    }
                    return "Error al parsear la respuesta de la IA. Formato JSON no convencional.";
                }
            } else {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line.trim());
                    }
                    System.err.println("Gemini API error code: " + code + ", body: " + errorResponse.toString());
                }
                return "⚠️ **Error de API Gemini**: Código HTTP " + code + ". Comprueba tu API Key y conexión a internet.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ **Error de conexión con la IA de Gemini**: " + e.getMessage();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + java.lang.Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private boolean getHomePresence(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;
        }
        Optional<Titular> titularOpt = titularRepository.findAll().stream()
                .filter(t -> email.trim().equalsIgnoreCase(t.correo))
                .findFirst();
        if (titularOpt.isPresent()) {
            Optional<com.AquaSmart.model.PreferenciaUsuario> prefOpt = preferenciaRepository.findByTitular(titularOpt.get());
            if (prefOpt.isPresent() && prefOpt.get().presenciaCasa != null) {
                return prefOpt.get().presenciaCasa;
            }
        }
        return true;
    }
}