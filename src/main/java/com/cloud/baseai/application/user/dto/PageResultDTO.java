package com.cloud.baseai.application.user.dto;

import java.util.List;

/**
 * 分页结果DTO
 *
 * <p>通用的分页结果包装器，用于所有需要分页的查询结果。
 * 提供了完整的分页信息和数据列表。</p>
 */
public record PageResultDTO<T>(
        List<T> content,        // 当前页数据
        long totalElements,     // 总记录数
        int currentPage,        // 当前页码（从0开始）
        int pageSize,          // 每页大小
        int totalPages,        // 总页数
        boolean hasNext,       // 是否有下一页
        boolean hasPrevious    // 是否有上一页
) {
    /**
     * 便捷构造方法
     */
    public PageResultDTO(List<T> content, long totalElements, int currentPage, int pageSize) {
        this(
                content,
                totalElements,
                currentPage,
                pageSize,
                (int) Math.ceil((double) totalElements / pageSize),
                currentPage < (int) Math.ceil((double) totalElements / pageSize) - 1,
                currentPage > 0
        );
    }

    /**
     * 创建空的分页结果
     */
    public static <T> PageResultDTO<T> empty(int currentPage, int pageSize) {
        return new PageResultDTO<>(
                List.of(),
                0L,
                currentPage,
                pageSize,
                0,
                false,
                false
        );
    }

    /**
     * 判断是否为空结果
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * 获取当前页实际记录数
     */
    public int getCurrentPageSize() {
        return content != null ? content.size() : 0;
    }
}