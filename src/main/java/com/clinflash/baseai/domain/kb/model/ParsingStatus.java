package com.clinflash.baseai.domain.kb.model;

import lombok.Getter;

/**
 * <h2>解析状态枚举</h2>
 *
 * <p>文档解析过程的状态机定义。</p>
 */
@Getter
public enum ParsingStatus {
    /**
     * 待解析 - 文档已上传但尚未开始解析
     */
    PENDING(0, "待解析"),

    /**
     * 解析中 - 正在进行文档解析和分块
     */
    PROCESSING(1, "解析中"),

    /**
     * 解析成功 - 文档已成功解析并生成知识块
     */
    SUCCESS(2, "解析成功"),

    /**
     * 解析失败 - 文档解析过程中出现错误
     */
    FAILED(3, "解析失败");

    /**
     * -- GETTER --
     * 获取状态代码
     */
    private final int code;
    /**
     * -- GETTER --
     * 获取状态显示名称
     */
    private final String label;

    ParsingStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据代码获取状态枚举
     *
     * @param code 状态代码
     * @return 对应的状态枚举
     * @throws IllegalArgumentException 如果代码无效
     */
    public static ParsingStatus fromCode(int code) {
        for (ParsingStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的解析状态代码: " + code);
    }
}