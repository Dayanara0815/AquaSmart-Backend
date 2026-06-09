package com.AquaSmart.controller;

import com.AquaSmart.dto.CurrentUserDto;
import com.AquaSmart.model.*;
import com.AquaSmart.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final TitularRepository titularRepository;
    private final PreferenciaRepository preferenciaRepository;
    private final MedidorRepository medidorRepository;
    private final EstadoValvulaRepository estadoValvulaRepository;
    private final TipoFlujoRepository tipoFlujoRepository;
    private final LecturaConsumoRepository lecturaConsumoRepository;

    public UserController(TitularRepository titularRepository, 
                          PreferenciaRepository preferenciaRepository,
                          MedidorRepository medidorRepository,
                          EstadoValvulaRepository estadoValvulaRepository,
                          TipoFlujoRepository tipoFlujoRepository,
                          LecturaConsumoRepository lecturaConsumoRepository) {
        this.titularRepository = titularRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.medidorRepository = medidorRepository;
        this.estadoValvulaRepository = estadoValvulaRepository;
        this.tipoFlujoRepository = tipoFlujoRepository;
        this.lecturaConsumoRepository = lecturaConsumoRepository;
    }

    @GetMapping("/user/current")
    @Transactional(readOnly = true)
    public Object getCurrentUser(@RequestParam(required = false) String email) {
        try {
            Optional<Titular> t;
            if (email != null && !email.isBlank()) {
                t = titularRepository.findAll().stream()
                        .filter(tit -> email.trim().equalsIgnoreCase(tit.correo))
                        .findFirst();
            } else {
                t = titularRepository.findAll().stream().findFirst();
            }

            if (t.isPresent()) {
                Titular tt = t.get();
                StringBuilder sb = new StringBuilder();
                if (tt.nombreTitular != null && !tt.nombreTitular.isBlank()) sb.append(tt.nombreTitular.trim());
                if (tt.apellidoPaterno != null && !tt.apellidoPaterno.isBlank()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(tt.apellidoPaterno.trim());
                }
                if (tt.apellidoMaterno != null && !tt.apellidoMaterno.isBlank()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(tt.apellidoMaterno.trim());
                }
                String full = sb.length() == 0 ? "Usuario X" : sb.toString();
                return new CurrentUserDto(tt.idTitular, full, tt.correo == null ? "" : tt.correo, tt.rol == null ? "DOMESTICO" : tt.rol, tt.fotoPerfil);
            }
            return new CurrentUserDto(0L, "Usuario X", "user@example.local", "DOMESTICO", null);
        } catch (Exception ex) {
            log.error("Error en getCurrentUser", ex);
            return Map.of("error", ex.getClass().getName(), "message", ex.getMessage());
        }
    }

    @PostMapping("/user/register")
    @Transactional
    public Object registerUser(@RequestBody Map<String, String> body) {
        try {
            String nombre = body.getOrDefault("nombre", "").trim();
            String apellidoPaterno = body.getOrDefault("apellidoPaterno", "").trim();
            String apellidoMaterno = body.getOrDefault("apellidoMaterno", "").trim();
            String correo = body.getOrDefault("correo", "").trim();
            String rol = body.getOrDefault("rol", "DOMESTICO").trim().toUpperCase();

            if (nombre.isBlank() || apellidoPaterno.isBlank() || correo.isBlank()) {
                return Map.of("error", "ValidationFailed", "message", "Nombre, Apellido Paterno y Correo son campos obligatorios.");
            }

            // Validar si ya existe el correo
            boolean exists = titularRepository.findAll().stream()
                    .anyMatch(tit -> correo.equalsIgnoreCase(tit.correo));
            if (exists) {
                return Map.of("error", "DuplicateEmail", "message", "El correo electrónico ya se encuentra registrado.");
            }

            // 1. Guardar Titular en PostgreSQL
            Titular newTitular = new Titular(nombre, apellidoPaterno, apellidoMaterno, correo, 35, "999888777", rol);
            newTitular = titularRepository.save(newTitular);

            // 2. Resolver o Crear Estado de Válvula
            EstadoValvula abierta = estadoValvulaRepository.findAll().stream()
                    .filter(ev -> "Abierta".equalsIgnoreCase(ev.nombreEstadoValvula))
                    .findFirst()
                    .orElseGet(() -> estadoValvulaRepository.save(new EstadoValvula("Abierta")));

            // 3. Crear Medidor asignado al Titular
            String meterId = "ASM-" + (1000 + (int) (Math.random() * 9000));
            Medidor medidor = new Medidor(meterId, LocalDate.now(), newTitular, abierta);
            medidorRepository.save(medidor);

            // 4. Sembrar consumos base (Comentado para la presentación)
            // Se comentó este bloque para que cualquier usuario nuevo registrado en tiempo real aparezca limpio, con consumos en cero y sin historial previo.
            /*
            TipoFlujo normal = tipoFlujoRepository.findAll().stream()
                    .filter(tf -> "Normal".equalsIgnoreCase(tf.nombreTipoFlujo))
                    .findFirst()
                    .orElseGet(() -> tipoFlujoRepository.save(new TipoFlujo("Normal")));

            LocalDate today = LocalDate.now();
            LecturaConsumo l1 = new LecturaConsumo(today.minusDays(2), LocalTime.of(8, 0), BigDecimal.valueOf(120.5), normal, medidor);
            LecturaConsumo l2 = new LecturaConsumo(today.minusDays(1), LocalTime.of(8, 30), BigDecimal.valueOf(118.2), normal, medidor);
            LecturaConsumo l3 = new LecturaConsumo(today, LocalTime.of(9, 0), BigDecimal.valueOf(124.8), normal, medidor);
            lecturaConsumoRepository.saveAll(List.of(l1, l2, l3));
            */

            // 5. Inicializar preferencia de tema
            PreferenciaUsuario pref = new PreferenciaUsuario(newTitular, "light");
            preferenciaRepository.save(pref);

            String full = (nombre + " " + apellidoPaterno).trim();
            return new CurrentUserDto(newTitular.idTitular, full, newTitular.correo, newTitular.rol, newTitular.fotoPerfil);
        } catch (Exception ex) {
            log.error("Error al registrar usuario", ex);
            return Map.of("error", ex.getClass().getName(), "message", ex.getMessage());
        }
    }

    @GetMapping("/user/theme")
    @Transactional(readOnly = true)
    public Map<String, String> getThemeForCurrentUser() {
        Optional<Titular> t = titularRepository.findAll().stream().findFirst();
        if (t.isPresent()) {
            Titular tt = t.get();
            Optional<PreferenciaUsuario> p = preferenciaRepository.findByTitular(tt);
            return Map.of("theme", p.map(pref -> pref.tema == null ? "light" : pref.tema).orElse("light"));
        }
        return Map.of("theme", "light");
    }

    @PostMapping("/user/theme")
    public Map<String, String> setThemeForCurrentUser(@RequestBody Map<String, String> body) {
        String theme = body.getOrDefault("theme", "light").toLowerCase();
        if (!theme.equals("light") && !theme.equals("dark")) {
            return Map.of("status", "error", "message", "Invalid theme");
        }

        Optional<Titular> t = titularRepository.findAll().stream().findFirst();
        if (t.isPresent()) {
            Titular tt = t.get();
            PreferenciaUsuario pref = preferenciaRepository.findByTitular(tt).orElseGet(() -> new PreferenciaUsuario(tt, theme));
            pref.tema = theme;
            preferenciaRepository.save(pref);
            return Map.of("status", "ok", "theme", theme);
        }

        return Map.of("status", "error", "message", "No user found");
    }

    @PostMapping("/user/profile-picture")
    @Transactional
    public Object updateProfilePicture(@RequestBody Map<String, String> body) {
        try {
            String email = body.getOrDefault("email", "").trim();
            String fotoPerfil = body.getOrDefault("fotoPerfil", "").trim();

            if (email.isBlank()) {
                return Map.of("error", "ValidationFailed", "message", "El correo es obligatorio.");
            }

            Optional<Titular> t = titularRepository.findAll().stream()
                    .filter(tit -> email.equalsIgnoreCase(tit.correo))
                    .findFirst();

            if (t.isPresent()) {
                Titular tt = t.get();
                tt.fotoPerfil = fotoPerfil.isBlank() ? null : fotoPerfil;
                titularRepository.save(tt);

                StringBuilder sb = new StringBuilder();
                if (tt.nombreTitular != null && !tt.nombreTitular.isBlank()) sb.append(tt.nombreTitular.trim());
                if (tt.apellidoPaterno != null && !tt.apellidoPaterno.isBlank()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(tt.apellidoPaterno.trim());
                }
                if (tt.apellidoMaterno != null && !tt.apellidoMaterno.isBlank()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(tt.apellidoMaterno.trim());
                }
                String full = sb.length() == 0 ? "Usuario X" : sb.toString();

                return new CurrentUserDto(tt.idTitular, full, tt.correo, tt.rol, tt.fotoPerfil);
            }

            return Map.of("error", "UserNotFound", "message", "El usuario no existe.");
        } catch (Exception ex) {
            log.error("Error al actualizar foto de perfil", ex);
            return Map.of("error", ex.getClass().getName(), "message", ex.getMessage());
        }
    }
}
