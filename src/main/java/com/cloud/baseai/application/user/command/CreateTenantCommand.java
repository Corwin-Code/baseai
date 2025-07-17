package com.cloud.baseai.application.user.command;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 创建租户命令
 */
public record CreateTenantCommand(
        @NotNull(message = "创建者ID不能为空")
        Long creatorId,

        @NotBlank(message = "组织名称不能为空")
        @Size(min = 2, max = 128, message = "组织名称长度必须在2-128位之间")
        String orgName,

        @Size(max = 32, message = "套餐代码长度不能超过32位")
        String planCode,

        @Future(message = "到期时间必须是未来时间")
        OffsetDateTime expireAt
) {
}