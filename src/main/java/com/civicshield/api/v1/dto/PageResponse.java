package com.civicshield.api.v1.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<T> sub = all.subList(from, to);
        int totalPages = (int) Math.ceil((double) all.size() / size);
        return new PageResponse<>(sub, page, size, all.size(), totalPages);
    }
}
