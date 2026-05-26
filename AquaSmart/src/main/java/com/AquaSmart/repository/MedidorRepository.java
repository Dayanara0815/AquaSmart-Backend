package com.AquaSmart.repository;

import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.Titular;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MedidorRepository extends JpaRepository<Medidor, String> {
    Optional<Medidor> findByTitular(Titular titular);
}