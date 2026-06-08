package com.AquaSmart.repository;

import com.AquaSmart.model.LecturaConsumo;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturaConsumoRepository extends JpaRepository<LecturaConsumo, Long> {

	@EntityGraph(attributePaths = {"medidor", "medidor.estadoValvula"})
	@Query("select l from LecturaConsumo l")
	List<LecturaConsumo> findAllWithDetails();

	@EntityGraph(attributePaths = {"medidor", "tipoFlujo"})
	@Query("select l from LecturaConsumo l where l.medidor.idMedidor = :medidorId and l.fecha >= :from and l.fecha <= :to")
	List<LecturaConsumo> findByMedidorAndDateRange(
			@org.springframework.data.repository.query.Param("medidorId") String medidorId,
			@org.springframework.data.repository.query.Param("from") java.time.LocalDate from,
			@org.springframework.data.repository.query.Param("to") java.time.LocalDate to);

	@org.springframework.data.jpa.repository.Modifying
	@Query("delete from LecturaConsumo l where l.fecha < :cutoffDate")
	void deleteOldReadings(@org.springframework.data.repository.query.Param("cutoffDate") java.time.LocalDate cutoffDate);
}