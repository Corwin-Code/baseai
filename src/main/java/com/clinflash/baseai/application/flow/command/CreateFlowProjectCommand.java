package com.clinflash.baseai.application.flow.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>创建流程项目命令</h2>
 *
 * <p>这个命令封装了创建新流程项目所需的所有信息。在企业环境中，
 * 项目是组织相关工作流的重要方式，它提供了权限边界和逻辑分组。</p>
 */
public record CreateFlowProjectCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotBlank(message = "项目名称不能为空")
        @Size(max = 128, message = "项目名称长度不能超过128个字符")
        String name,

        @Size(max = 512, message = "项目描述长度不能超过512个字符")
        String description,

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 验证项目名称是否符合命名规范
     */
    public boolean isValidName() {
        return name != null && !name.trim().isEmpty() &&
                !name.matches(".*[<>:\"/\\\\|?*].*"); // 排除特殊字符
    }
}