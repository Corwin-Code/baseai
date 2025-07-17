package com.cloud.baseai.application.misc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * <h2>通用分页结果DTO</h2>
 */
public record PageResultDTO<T>(
        @JsonProperty("content")
        List<T> content,

        @JsonProperty("totalElements")
        long totalElements,

        @JsonProperty("totalPages")
        int totalPages,

        @JsonProperty("currentPage")
        int currentPage,

        @JsonProperty("pageSize")
        int pageSize,

        @JsonProperty("hasNext")
        boolean hasNext,

        @JsonProperty("hasPrevious")
        boolean hasPrevious
) {
    public static <T> PageResultDTO<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResultDTO<>(
                content,
                totalElements,
                totalPages,
                page,
                size,
                page < totalPages - 1,
                page > 0
        );
    }
}