package com.AquaSmart.repository;

import com.AquaSmart.model.PreferenciaUsuario;
import com.AquaSmart.model.Titular;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferenciaRepository extends JpaRepository<PreferenciaUsuario, Long> {
    Optional<PreferenciaUsuario> findByTitular(Titular titular);
}
