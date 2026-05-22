package com.AquaSmart.controller;

import com.AquaSmart.dto.CurrentUserDto;
import com.AquaSmart.model.Titular;
import com.AquaSmart.repository.TitularRepository;
import com.AquaSmart.repository.PreferenciaRepository;
import com.AquaSmart.model.PreferenciaUsuario;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public UserController(TitularRepository titularRepository, PreferenciaRepository preferenciaRepository) {
        this.titularRepository = titularRepository;
        this.preferenciaRepository = preferenciaRepository;
    }

    @GetMapping("/user/current")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Object getCurrentUser() {
        try {
            Optional<Titular> t = titularRepository.findAll().stream().findFirst();
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
                return new CurrentUserDto(tt.idTitular, full, tt.correo == null ? "" : tt.correo);
            }
            return new CurrentUserDto(0L, "Usuario X", "user@example.local");
        } catch (Exception ex) {
            log.error("Error en getCurrentUser", ex);
            return Map.of("error", ex.getClass().getName(), "message", ex.getMessage());
        }
    }

    @GetMapping("/user/theme")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
}
