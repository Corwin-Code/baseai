package com.cloud.baseai.application.misc.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>创建提示词模板命令</h2>
 *
 * <p>这个命令封装了创建新提示词模板所需的所有信息。设计时遵循了"命令模式"的思想，
 * 将用户的意图转化为可执行的数据结构。每个字段都经过精心设计，既要满足业务需求，
 * 又要保证数据的完整性和安全性。</p>
 */
public record CreatePromptTemplateCommand(
        @JsonProperty("tenantId")
        Long tenantId,

        @JsonProperty("name")
        @NotBlank(message = "模板名称不能为空")
        @Size(max = 128, message = "模板名称长度不能超过128个字符")
        String name,

        @JsonProperty("content")
        @NotBlank(message = "模板内容不能为空")
        @Size(max = 10000, message = "模板内容长度不能超过10000个字符")
        String content,

        @JsonProperty("modelCode")
        @NotBlank(message = "适用模型不能为空")
        @Size(max = 32, message = "模型代码长度不能超过32个字符")
        String modelCode,

        @JsonProperty("operatorId")
        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 检查是否为系统模板创建请求
     *
     * <p>系统模板的tenantId为null，这是一个重要的业务规则。
     * 只有具备特殊权限的管理员才能创建系统模板。</p>
     */
    public boolean isSystemTemplate() {
        return this.tenantId == null;
    }

    /**
     * 验证模板内容的格式
     *
     * <p>可以在这里添加更复杂的内容验证逻辑，比如检查变量占位符的格式、
     * 危险内容过滤等。这种验证逻辑放在Command中可以实现early validation。</p>
     */
    public boolean hasValidContent() {
        return this.content != null &&
                !this.content.trim().isEmpty() &&
                !this.content.contains("<script>"); // 简单的安全检查
    }
}