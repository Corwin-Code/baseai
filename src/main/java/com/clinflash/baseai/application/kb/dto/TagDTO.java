package com.clinflash.baseai.application.kb.dto;

/**
 * <h2>标签数据传输对象</h2>
 *
 * @param id     标签ID
 * @param name   标签名称
 * @param remark 备注说明
 */
public record TagDTO(Long id, String name, String remark) {

    /**
     * 创建基本标签DTO
     *
     * @param id     标签ID
     * @param name   标签名称
     * @param remark 备注
     * @return 标签DTO
     */
    public static TagDTO basic(Long id, String name, String remark) {
        return new TagDTO(id, name, remark);
    }

    /**
     * 只包含名称的简单标签DTO
     *
     * @param name 标签名称
     * @return 标签DTO
     */
    public static TagDTO simple(String name) {
        return new TagDTO(null, name, null);
    }
}