package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 更新租户命令
 */
public record UpdateTenantCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotBlank(message = "组织名称不能为空")
        @Size(min = 2, max = 128, message = "组织名称长度必须在2-128位之间")
        String orgName,

        @Size(max = 32, message = "套餐代码长度不能超过32位")
        String planCode,

        OffsetDateTime expireAt,

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
}