package com.clinflash.baseai.application.mcp.dto;

import java.time.OffsetDateTime;

/**
 * <h2>工具详细信息数据传输对象</h2>
 *
 * <p>提供了工具的完整档案信息。
 * 从基本的标识信息到技术规范，从使用统计到运行状态，
 * 这个对象为工具管理者和使用者提供了全方位的工具视图。</p>
 *
 * <p><b>信息层次结构：</b></p>
 * <ul>
 * <li><b>身份信息：</b>ID、代码、名称等基本标识，确保工具的唯一性</li>
 * <li><b>功能描述：</b>类型、描述、图标等，帮助用户理解工具的用途</li>
 * <li><b>技术规范：</b>参数结构、结果格式、端点配置等，指导技术集成</li>
 * <li><b>运营数据：</b>授权租户数、调用统计等，反映工具的受欢迎程度</li>
 * <li><b>状态信息：</b>启用状态、创建时间等，用于运营管理</li>
 * </ul>
 *
 * <p><b>Schema规范：</b></p>
 * <p>参数Schema和结果Schema采用JSON Schema标准，为AI模型提供了
 * 精确的调用指导。这就像是API的"使用手册"，确保AI能够
 * 正确地构造参数和解析结果。</p>
 *
 * <p><b>使用场景：</b>主要用于工具管理界面的详情页面、
 * API文档生成、以及AI模型的工具选择和调用决策。</p>
 *
 * @param id                工具的唯一标识符
 * @param code              工具代码，用于程序化调用和识别
 * @param name              工具的显示名称，面向用户的友好标识
 * @param type              工具类型，如"HTTP"、"SCRIPT"、"DATABASE"等
 * @param description       工具的功能描述，说明其用途和能力
 * @param iconUrl           工具图标的URL地址，用于界面展示
 * @param paramSchema       参数结构的JSON Schema，定义输入格式
 * @param resultSchema      结果结构的JSON Schema，定义输出格式
 * @param endpoint          工具的访问端点，主要用于HTTP类型的工具
 * @param authType          认证类型，如"API_KEY"、"OAUTH"、"NONE"等
 * @param enabled           工具的启用状态，控制是否可以被调用
 * @param authorizedTenants 已授权的租户数量，反映工具的使用范围
 * @param recentCalls       最近的调用次数，通常指24小时内的调用统计
 * @param createdAt         工具的创建时间，用于审计和管理
 */
public record ToolDetailDTO(
        Long id,
        String code,
        String name,
        String type,
        String description,
        String iconUrl,
        String paramSchema,
        String resultSchema,
        String endpoint,
        String authType,
        Boolean enabled,
        int authorizedTenants,
        int recentCalls,
        OffsetDateTime createdAt
) {
}