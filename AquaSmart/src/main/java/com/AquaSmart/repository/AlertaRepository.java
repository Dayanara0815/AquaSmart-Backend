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

	@org.springframework.data.jpa.repository.Modifying
	@Query("delete from Alerta a where a.fecha < :cutoffDate")
	void deleteOldAlerts(@org.springframework.data.repository.query.Param("cutoffDate") java.time.LocalDate cutoffDate);
}