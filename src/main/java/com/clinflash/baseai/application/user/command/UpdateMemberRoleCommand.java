package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.NotNull;

/**
 * 更新成员角色命令
 */
public record UpdateMemberRoleCommand(
        @NotNull(message = "角色ID不能为空")
        Long roleId,

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
}