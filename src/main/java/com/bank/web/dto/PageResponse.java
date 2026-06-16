package com.bank.web.dto;

import com.bank.domain.model.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <D, T> PageResponse<T> of(Page<D> page, Function<D, T> mapper) {
        return new PageResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
