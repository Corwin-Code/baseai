package com.cloud.baseai.application.flow.dto;

import java.util.List;

/**
 * <h2>分页结果传输对象</h2>
 *
 * <p>通用的分页结果包装器，为所有列表查询提供统一的分页信息结构。</p>
 */
public record PageResultDTO<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    /**
     * 创建分页结果的工厂方法
     */
    public static <T> PageResultDTO<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return new PageResultDTO<>(
                content, totalElements, page, size,
                totalPages, hasNext, hasPrevious
        );
    }

    /**
     * 创建空的分页结果
     */
    public static <T> PageResultDTO<T> empty(int page, int size) {
        return new PageResultDTO<>(
                List.of(), 0L, page, size, 0, false, false
        );
    }

    /**
     * 检查是否为空结果
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * 获取当前页的项目数量
     */
    public int getCurrentPageSize() {
        return content.size();
    }

    /**
     * 获取分页信息的友好显示
     */
    public String getPaginationDisplay() {
        if (totalElements == 0) {
            return "暂无数据";
        }

        int start = page * size + 1;
        int end = Math.min((page + 1) * size, (int) totalElements);

        return String.format("第 %d-%d 条，共 %d 条", start, end, totalElements);
    }
}