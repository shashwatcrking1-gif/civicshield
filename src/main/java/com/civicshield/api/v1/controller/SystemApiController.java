package com.civicshield.api.v1.controller;

import com.civicshield.api.v1.dto.CommonDtos.DataSourceDto;
import com.civicshield.api.v1.dto.CommonDtos.SystemStatus;
import com.civicshield.model.DataStatus;
import com.civicshield.service.DataSourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SystemApiController {

    private final DataSourceService dataSourceService;

    @Value("${civicshield.ai.enabled:false}")
    private boolean aiEnabled;

    public SystemApiController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping("/system/status")
    public ResponseEntity<SystemStatus> systemStatus() {
        Map<DataStatus, Long> counts = dataSourceService.counts();
        Map<String, Long> dsMap = new LinkedHashMap<>();
        for (DataStatus s : DataStatus.values()) {
            dsMap.put(s.name(), counts.getOrDefault(s, 0L));
        }

        return ResponseEntity.ok(new SystemStatus(
                "UP",
                "civicshield-backend",
                "1.0.0",
                "v1",
                LocalDateTime.now(),
                "Asia/Kolkata",
                "SIMULATED",
                "JAVA_CASCADE_ENGINE",
                aiEnabled,
                dsMap
        ));
    }

    @GetMapping("/data-sources")
    public ResponseEntity<List<DataSourceDto>> listDataSources() {
        List<DataSourceDto> list = dataSourceService.all().stream()
                .map(DataSourceDto::from)
                .toList();
        return ResponseEntity.ok(list);
    }
}
