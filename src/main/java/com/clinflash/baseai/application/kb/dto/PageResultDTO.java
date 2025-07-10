package com.clinflash.baseai.application.kb.dto;

import java.util.List;

/**
 * <h2>分页结果数据传输对象</h2>
 *
 * <p>通用的分页结果封装，用于所有需要分页的查询结果。</p>
 *
 * @param <T>           数据类型
 * @param content       当前页数据
 * @param totalElements 总记录数
 * @param totalPages    总页数
 * @param currentPage   当前页码
 * @param pageSize      每页大小
 * @param hasNext       是否有下一页
 * @param hasPrevious   是否有上一页
 */
public record PageResultDTO<T>(
        List<T> content,
        Long totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize,
        Boolean hasNext,
        Boolean hasPrevious
) {
    /**
     * 创建分页结果的静态工厂方法
     *
     * @param content       数据列表
     * @param totalElements 总记录数
     * @param currentPage   当前页码（从0开始）
     * @param pageSize      每页大小
     * @param <T>           数据类型
     * @return 分页结果DTO
     */
    public static <T> PageResultDTO<T> of(List<T> content, Long totalElements,
                                          Integer currentPage, Integer pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < totalPages - 1;
        boolean hasPrevious = currentPage > 0;

        return new PageResultDTO<>(
                content, totalElements, totalPages, currentPage, pageSize, hasNext, hasPrevious
        );
    }

    /**
     * 检查是否为空结果
     *
     * @return true如果没有数据
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * 获取当前页的记录数
     *
     * @return 当前页记录数
     */
    public int getCurrentPageSize() {
        return content != null ? content.size() : 0;
    }
}