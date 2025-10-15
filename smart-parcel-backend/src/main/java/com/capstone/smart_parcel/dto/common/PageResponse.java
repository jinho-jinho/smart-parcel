package com.capstone.smart_parcel.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean first,
        boolean last,
        String sort
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, Function.identity());
    }

    public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> mapper) {
        List<R> mapped = page.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        String sort = page.getSort().isSorted() ? page.getSort().toString() : "";
        return new PageResponse<>(
                mapped,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                sort
        );
    }
}
