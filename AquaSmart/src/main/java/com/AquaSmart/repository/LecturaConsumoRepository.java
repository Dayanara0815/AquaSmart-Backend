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
}