package com.AquaSmart.repository;

import com.AquaSmart.model.Medidor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedidorRepository extends JpaRepository<Medidor, String> {
}