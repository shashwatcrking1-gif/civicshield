package com.civicshield.repository;

import com.civicshield.model.DataSource;
import com.civicshield.model.DataStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    long countByDataStatus(DataStatus status);
}
