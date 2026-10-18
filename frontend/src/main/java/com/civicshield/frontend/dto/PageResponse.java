package com.civicshield.frontend.dto;

import java.util.List;

/** Generic page wrapper ({@code Page<T>} in docs/API.md). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        int pages = size == 0 ? 0 : (int) Math.ceil(all.size() / (double) size);
        return new PageResponse<>(all.subList(from, to), page, size, all.size(), pages);
    }

    public boolean isEmpty() { return content.isEmpty(); }
}
