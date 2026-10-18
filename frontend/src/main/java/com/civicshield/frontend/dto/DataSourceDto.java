package com.civicshield.frontend.dto;

public record DataSourceDto(Long id, String name, String type, String location, DataStatus status) {
}
