package com.rag.raggeneratorapplication.common.page;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Serializable page envelope for list endpoints. Keeps the API contract
 * independent of Spring Data's {@code Page} representation.
 *
 * @author Thakshara Dhananjaya
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <E> PageResponse<E> from(Page<E> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Maps a page of entities to a page of DTOs while preserving paging metadata. */
    public static <E, R> PageResponse<R> from(Page<E> page, Function<? super E, ? extends R> mapper) {
        return from(page.map(mapper));
    }
}
