package com.clinflash.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>创建标签命令</h2>
 *
 * @param name       标签名称
 * @param remark     备注说明
 * @param operatorId 操作人ID
 */
public record CreateTagCommand(
        @NotBlank(message = "标签名称不能为空")
        @Size(max = 64, message = "标签名称长度不能超过64字符")
        String name,

        @Size(max = 500, message = "备注长度不能超过500字符")
        String remark,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {
}