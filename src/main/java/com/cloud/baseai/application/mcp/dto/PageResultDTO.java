package com.cloud.baseai.application.mcp.dto;

import java.util.List;

/**
 * <h2>分页结果DTO</h2>
 *
 * <p>通用的分页数据传输对象，提供完整的分页信息。
 * 这个设计让前端能够正确实现分页导航和数据展示。</p>
 */
public record PageResultDTO<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int size,
        boolean hasNext,
        boolean hasPrevious
) {
    /**
     * 创建分页结果的便捷方法
     */
    public static <T> PageResultDTO<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return new PageResultDTO<>(
                content,
                totalElements,
                totalPages,
                page,
                size,
                hasNext,
                hasPrevious
        );
    }

    /**
     * 创建空的分页结果
     */
    public static <T> PageResultDTO<T> empty(int page, int size) {
        return new PageResultDTO<>(
                List.of(),
                0L,
                0,
                page,
                size,
                false,
                false
        );
    }

    /**
     * 检查是否为空结果
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * 获取当前页的数据数量
     */
    public int getCurrentPageSize() {
        return content != null ? content.size() : 0;
    }
}