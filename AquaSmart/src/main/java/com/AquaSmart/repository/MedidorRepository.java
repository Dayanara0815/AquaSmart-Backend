package com.AquaSmart.repository;

import com.AquaSmart.model.Medidor;
import com.AquaSmart.model.Titular;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MedidorRepository extends JpaRepository<Medidor, String> {
    Optional<Medidor> findByTitular(Titular titular);

    @EntityGraph(attributePaths = {"titular", "estadoValvula"})
    @Query("select m from Medidor m")
    List<Medidor> findAllWithDetails();
}