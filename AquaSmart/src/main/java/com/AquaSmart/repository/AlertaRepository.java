package com.AquaSmart.repository;

import com.AquaSmart.model.Alerta;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

	@EntityGraph(attributePaths = {"tipoAlerta", "tipoEstado", "medidor"})
	@Query("select a from Alerta a")
	List<Alerta> findAllWithDetails();
}