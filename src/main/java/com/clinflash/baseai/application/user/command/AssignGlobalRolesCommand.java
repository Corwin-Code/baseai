package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

/**
 * 分配全局角色命令
 */
public record AssignGlobalRolesCommand(
        @NotEmpty(message = "角色列表不能为空")
        Set<@NotNull @Positive Long> roleIds,

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
}