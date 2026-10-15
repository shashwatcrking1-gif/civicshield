package com.civicshield.repository;

import com.civicshield.model.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findAllByOrderBySimulationIdDesc();
}
