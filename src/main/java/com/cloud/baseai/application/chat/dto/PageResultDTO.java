package com.cloud.baseai.application.chat.dto;

import java.util.List;

/**
 * 分页结果DTO
 */
public record PageResultDTO<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PageResultDTO<T> of(List<T> content, long totalElements, int currentPage, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < totalPages - 1;
        boolean hasPrevious = currentPage > 0;

        return new PageResultDTO<>(
                content,
                totalElements,
                totalPages,
                currentPage,
                pageSize,
                hasNext,
                hasPrevious
        );
    }
}