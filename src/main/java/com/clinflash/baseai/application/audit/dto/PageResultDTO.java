package com.clinflash.baseai.application.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * <h2>分页结果数据传输对象</h2>
 *
 * <p>这个通用的分页DTO就像一个标准化的"数据容器"，它不仅包含了查询结果，
 * 还提供了前端分页组件所需的所有信息。无论是审计日志、用户列表还是
 * 其他任何需要分页的数据，都可以使用这个统一的格式。</p>
 *
 * <p><b>设计优势：</b></p>
 * <p>统一的分页格式让前端组件可以复用，减少了开发和维护成本。
 * 同时，完整的分页信息让用户能够清楚地知道当前查看的数据在整体中的位置。</p>
 *
 * @param content       当前页的数据内容
 * @param totalElements 总记录数
 * @param page          当前页码（从0开始）
 * @param size          每页大小
 * @param totalPages    总页数
 */
@Schema(description = "分页查询结果")
public record PageResultDTO<T>(
        @Schema(description = "当前页数据内容")
        List<T> content,

        @Schema(description = "总记录数", example = "1250")
        long totalElements,

        @Schema(description = "当前页码（从0开始）", example = "2")
        int page,

        @Schema(description = "每页大小", example = "20")
        int size,

        @Schema(description = "总页数", example = "63")
        int totalPages
) {

    /**
     * 构造函数 - 自动计算总页数
     *
     * <p>这个便利构造函数会自动计算总页数，避免了手动计算可能出现的错误。</p>
     */
    public PageResultDTO(List<T> content, long totalElements, int page, int size) {
        this(content, totalElements, page, size, calculateTotalPages(totalElements, size));
    }

    /**
     * 计算总页数
     *
     * <p>使用向上取整的方式计算总页数，确保所有数据都有对应的页面。</p>
     */
    private static int calculateTotalPages(long totalElements, int size) {
        return size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
    }

    /**
     * 判断是否为第一页
     */
    public boolean isFirst() {
        return page == 0;
    }

    /**
     * 判断是否为最后一页
     */
    public boolean isLast() {
        return page >= totalPages - 1;
    }

    /**
     * 判断是否有下一页
     */
    public boolean hasNext() {
        return !isLast();
    }

    /**
     * 判断是否有上一页
     */
    public boolean hasPrevious() {
        return !isFirst();
    }

    /**
     * 获取当前页的记录数量
     */
    public int getNumberOfElements() {
        return content != null ? content.size() : 0;
    }

    /**
     * 判断当前页是否为空
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * 获取分页摘要信息
     *
     * <p>生成类似"显示第21-40条，共1250条记录"的摘要信息。</p>
     */
    public String getPaginationSummary() {
        if (isEmpty()) {
            return "没有找到记录";
        }

        int start = page * size + 1;
        int end = Math.min(start + getNumberOfElements() - 1, (int) totalElements);

        return String.format("显示第%d-%d条，共%d条记录", start, end, totalElements);
    }
}