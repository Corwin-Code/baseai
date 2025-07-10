package com.clinflash.baseai.domain.mcp.model;

import lombok.Getter;

/**
 * <h2>工具类型枚举</h2>
 *
 * <p>定义了MCP系统支持的所有工具类型。每种类型代表了不同的
 * 执行方式和配置要求，确保系统能够正确处理各种工具。</p>
 */
@Getter
public enum ToolType {
    /**
     * HTTP API工具 - 通过HTTP请求调用外部服务
     */
    HTTP("HTTP API工具", "通过HTTP请求调用外部API服务"),

    /**
     * 本地代理工具 - 调用本地部署的代理服务
     */
    AGENT("代理工具", "调用本地部署的智能代理服务"),

    /**
     * 函数工具 - 调用预定义的函数逻辑
     */
    FUNCTION("函数工具", "调用系统内置或用户定义的函数"),

    /**
     * 脚本工具 - 执行脚本代码
     */
    SCRIPT("脚本工具", "执行JavaScript、Python等脚本代码"),

    /**
     * 数据库工具 - 进行数据库操作
     */
    DATABASE("数据库工具", "进行数据库查询和操作");

    private final String label;
    private final String description;

    ToolType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * 检查是否为外部调用类型
     */
    public boolean isExternalCall() {
        return this == HTTP || this == AGENT;
    }

    /**
     * 检查是否需要安全沙箱
     */
    public boolean requiresSandbox() {
        return this == SCRIPT || this == FUNCTION;
    }
}