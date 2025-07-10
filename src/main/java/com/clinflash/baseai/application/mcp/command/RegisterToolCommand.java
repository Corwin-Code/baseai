package com.clinflash.baseai.application.mcp.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>注册工具命令</h2>
 *
 * <p>这个命令对象承载了注册新MCP工具所需的全部信息。设计上遵循了
 * "命令查询职责分离"的原则，专门用于数据传输而不包含业务逻辑。</p>
 *
 * <p><b>验证策略：</b></p>
 * <p>我们在输入边界就进行数据验证，确保进入业务逻辑的数据都是
 * 符合规范的。这种"早期验证"的策略能够防止无效数据污染领域模型。</p>
 *
 * @param code         工具唯一编码，用于API调用时的标识
 * @param name         工具显示名称，面向用户的友好名称
 * @param type         工具类型，决定了执行方式和配置要求
 * @param description  工具功能描述，帮助用户理解工具用途
 * @param iconUrl      工具图标URL，用于界面展示
 * @param paramSchema  参数JSON Schema，定义工具接受的输入格式
 * @param resultSchema 结果JSON Schema，定义工具返回的数据格式
 * @param endpoint     HTTP工具的调用端点，其他类型工具可为空
 * @param authType     认证类型，如API_KEY、BEARER等
 * @param operatorId   操作人员ID，用于审计追踪
 */
public record RegisterToolCommand(
        @NotBlank(message = "工具代码不能为空")
        @Size(max = 64, message = "工具代码长度不能超过64个字符")
        String code,

        @NotBlank(message = "工具名称不能为空")
        @Size(max = 128, message = "工具名称长度不能超过128个字符")
        String name,

        @NotBlank(message = "工具类型不能为空")
        String type,

        @Size(max = 1000, message = "工具描述长度不能超过1000个字符")
        String description,

        @Size(max = 256, message = "图标URL长度不能超过256个字符")
        String iconUrl,

        String paramSchema,

        String resultSchema,

        @Size(max = 256, message = "端点URL长度不能超过256个字符")
        String endpoint,

        @Size(max = 32, message = "认证类型长度不能超过32个字符")
        String authType,

        @NotNull(message = "操作人员ID不能为空")
        Long operatorId
) {
    /**
     * 验证HTTP工具的必要配置
     */
    public boolean isValidHttpTool() {
        return !"HTTP".equals(type) || (endpoint != null && !endpoint.trim().isEmpty());
    }

    /**
     * 获取清理后的参数Schema
     */
    public String getCleanParamSchema() {
        return paramSchema != null && !paramSchema.trim().isEmpty() ?
                paramSchema.trim() : "{}";
    }

    /**
     * 获取清理后的结果Schema
     */
    public String getCleanResultSchema() {
        return resultSchema != null && !resultSchema.trim().isEmpty() ?
                resultSchema.trim() : "{}";
    }
}