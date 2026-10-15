package com.civicshield.service;

import com.civicshield.exception.ResourceNotFoundException;
import com.civicshield.model.DataSource;
import com.civicshield.model.DataStatus;
import com.civicshield.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registry of the (simulated) data feeds CivicShield consumes. */
@Service
public class DataSourceService {

    private final DataSourceRepository repository;

    public DataSourceService(DataSourceRepository repository) {
        this.repository = repository;
    }

    public List<DataSource> all() {
        return repository.findAll(Sort.by("sourceType", "sourceName"));
    }

    public Map<DataStatus, Long> counts() {
        Map<DataStatus, Long> m = new EnumMap<>(DataStatus.class);
        for (DataStatus s : DataStatus.values()) m.put(s, repository.countByDataStatus(s));
        return m;
    }

    @Transactional
    public DataSource updateStatus(Long id, DataStatus status) {
        DataSource ds = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Data source", id));
        ds.setDataStatus(status);
        return ds;
    }
}
