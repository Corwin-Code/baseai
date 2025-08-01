package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>统一错误代码枚举</h2>
 *
 * <p>这个枚举定义了整个系统中所有可能的错误代码。采用分层编码结构，
 * 便于理解和维护。每个错误代码都包含默认的中文消息，同时支持
 * 通过资源文件进行国际化。</p>
 *
 * <p><b>编码规则：</b></p>
 * <p>格式：[系统模块]_[功能域]_[序号]</p>
 * <ul>
 * <li>系统模块：SYS(系统级)、BIZ(业务级)、EXT(外部集成)</li>
 * <li>功能域：AUTH(认证)、PERM(权限)、DATA(数据)、NET(网络)等</li>
 * <li>序号：001-999，预留充足的扩展空间</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    // =================== 系统级错误 (SYS) ===================

    // 认证相关错误 (AUTH)
    SYS_AUTH_001("SYS_AUTH_001", "用户认证失败"),
    SYS_AUTH_002("SYS_AUTH_002", "认证令牌无效"),
    SYS_AUTH_003("SYS_AUTH_003", "认证令牌已过期"),
    SYS_AUTH_004("SYS_AUTH_004", "认证服务不可用"),
    SYS_AUTH_005("SYS_AUTH_005", "多次认证失败，账户已锁定"),

    // 权限相关错误 (PERM)
    SYS_PERM_001("SYS_PERM_001", "权限不足"),
    SYS_PERM_002("SYS_PERM_002", "租户权限验证失败"),
    SYS_PERM_003("SYS_PERM_003", "资源访问被拒绝"),
    SYS_PERM_004("SYS_PERM_004", "操作权限不足"),
    SYS_PERM_005("SYS_PERM_005", "需要系统管理员权限"),

    // 数据相关错误 (DATA)
    SYS_DATA_001("SYS_DATA_001", "数据不存在"),
    SYS_DATA_002("SYS_DATA_002", "数据格式错误"),
    SYS_DATA_003("SYS_DATA_003", "数据完整性校验失败"),
    SYS_DATA_004("SYS_DATA_004", "数据冲突"),
    SYS_DATA_005("SYS_DATA_005", "数据库连接失败"),
    SYS_DATA_006("SYS_DATA_006", "获取系统统计失败"),

    // 网络相关错误 (NET)
    SYS_NET_001("SYS_NET_001", "网络连接超时"),
    SYS_NET_002("SYS_NET_002", "网络连接中断"),
    SYS_NET_003("SYS_NET_003", "网络服务不可用"),
    SYS_NET_004("SYS_NET_004", "网络连接失败，请检查网络状态"),

    // 系统资源错误 (RES)
    SYS_RES_001("SYS_RES_001", "系统繁忙"),
    SYS_RES_002("SYS_RES_002", "内存不足"),
    SYS_RES_003("SYS_RES_003", "磁盘空间不足"),
    SYS_RES_004("SYS_RES_004", "线程池已满"),

    // 健康检查错误 (HEALTH)
    SYS_HEALTH_001("SYS_HEALTH_001", "系统健康检查失败"),
    SYS_HEALTH_002("SYS_HEALTH_002", "依赖服务健康检查失败"),
    SYS_HEALTH_003("SYS_HEALTH_003", "数据库健康检查失败"),

    // 配置相关错误 (SETTING)
    SYS_SETTING_001("SYS_SETTING_001", "获取系统设置失败"),
    SYS_SETTING_002("SYS_SETTING_002", "更新系统设置失败"),
    SYS_SETTING_003("SYS_SETTING_003", "批量更新设置数量超出限制"),
    SYS_SETTING_004("SYS_SETTING_004", "批量更新设置失败"),

    // 任务相关错误 (TASK)
    SYS_TASK_001("SYS_TASK_001", "任务不存在"),
    SYS_TASK_002("SYS_TASK_002", "任务无法重试"),
    SYS_TASK_003("SYS_TASK_003", "重试任务失败"),
    SYS_TASK_004("SYS_TASK_004", "任务参数过大"),
    SYS_TASK_005("SYS_TASK_005", "创建系统任务失败"),
    SYS_TASK_006("SYS_TASK_006", "查询任务列表失败"),
    SYS_TASK_007("SYS_TASK_007", "获取任务详情失败"),
    SYS_TASK_008("SYS_TASK_008", "重试等待被中断"),

    // =================== 业务级错误 (BIZ) ===================

    // 用户管理错误 (USER)
    BIZ_USER_001("BIZ_USER_001", "用户不存在"),
    BIZ_USER_002("BIZ_USER_002", "用户名已存在"),
    BIZ_USER_003("BIZ_USER_003", "用户邮箱已存在"),
    BIZ_USER_004("BIZ_USER_004", "用户密码强度不足"),
    BIZ_USER_005("BIZ_USER_005", "用户账户已禁用"),
    BIZ_USER_006("BIZ_USER_006", "密码不正确"),
    BIZ_USER_007("BIZ_USER_007", "账户已被锁定"),

    BIZ_USER_008("BIZ_USER_008", "用户数据不一致"),
    BIZ_USER_009("BIZ_USER_009", "获取用户资料失败"),
    BIZ_USER_010("BIZ_USER_010", "更新用户资料失败"),
    BIZ_USER_011("BIZ_USER_011", "用户注册处理失败"),
    BIZ_USER_012("BIZ_USER_012", "搜索用户失败"),
    BIZ_USER_013("BIZ_USER_013", "获取用户统计失败"),

    BIZ_USER_014("BIZ_USER_014", "邮箱格式不正确"),
    BIZ_USER_015("BIZ_USER_015", "邮箱已被注册：%s"),
    BIZ_USER_016("BIZ_USER_016", "原密码错误"),
    BIZ_USER_017("BIZ_USER_017", "用户名格式不正确，只能包含字母、数字、下划线和连字符，长度3-32位"),
    BIZ_USER_018("BIZ_USER_018", "密码强度不足，请使用至少8位包含大小写字母、数字和特殊字符的密码"),
    BIZ_USER_019("BIZ_USER_019", "两次输入的密码不一致"),
    BIZ_USER_020("BIZ_USER_020", "修改密码失败"),
    BIZ_USER_021("BIZ_USER_021", "账户已被锁定"),
    BIZ_USER_022("BIZ_USER_022", "账户已被禁用"),

    BIZ_USER_023("BIZ_USER_023", "邀请已过期"),
    BIZ_USER_024("BIZ_USER_024", "邀请链接无效或已过期"),
    BIZ_USER_025("BIZ_USER_025", "邀请无效"),
    BIZ_USER_026("BIZ_USER_026", "无效的邀请操作"),
    BIZ_USER_027("BIZ_USER_027", "获取待处理邀请失败"),
    BIZ_USER_028("BIZ_USER_028", "处理邀请响应失败"),

    BIZ_USER_029("BIZ_USER_029", "激活码已过期"),
    BIZ_USER_030("BIZ_USER_030", "激活码无效"),
    BIZ_USER_031("BIZ_USER_031", "激活码无效或已过期"),
    BIZ_USER_032("BIZ_USER_032", "用户激活失败"),

    // 租户/组织管理错误 (TENANT)
    BIZ_TENANT_001("BIZ_TENANT_001", "租户不存在"),
    BIZ_TENANT_002("BIZ_TENANT_002", "租户名称已存在"),
    BIZ_TENANT_003("BIZ_TENANT_003", "租户成员数量已达上限"),
    BIZ_TENANT_004("BIZ_TENANT_004", "租户状态异常"),

    BIZ_TENANT_005("BIZ_TENANT_005", "获取租户成员失败"),
    BIZ_TENANT_006("BIZ_TENANT_006", "用户不是该租户的成员"),
    BIZ_TENANT_007("BIZ_TENANT_007", "用户已经是该租户的成员"),
    BIZ_TENANT_008("BIZ_TENANT_008", "您已经是该组织的成员"),
    BIZ_TENANT_009("BIZ_TENANT_009", "您没有管理成员的权限"),
    BIZ_TENANT_010("BIZ_TENANT_010", "邀请成员失败"),
    BIZ_TENANT_011("BIZ_TENANT_011", "创建租户失败"),
    BIZ_TENANT_012("BIZ_TENANT_012", "更新租户信息失败"),
    BIZ_TENANT_013("BIZ_TENANT_013", "获取租户详情失败"),
    BIZ_TENANT_014("BIZ_TENANT_014", "获取用户租户列表失败"),
    BIZ_TENANT_015("BIZ_TENANT_015", "移除成员失败"),
    BIZ_TENANT_016("BIZ_TENANT_016", "不能移除自己"),
    BIZ_TENANT_017("BIZ_TENANT_017", "不能移除最后一个管理员"),

    // 角色/权限错误 (ROLE)
    BIZ_ROLE_001("BIZ_ROLE_001", "角色不存在"),
    BIZ_ROLE_002("BIZ_ROLE_002", "系统管理员角色未找到"),
    BIZ_ROLE_003("BIZ_ROLE_003", "获取用户全局角色失败"),
    BIZ_ROLE_004("BIZ_ROLE_004", "获取角色列表失败"),
    BIZ_ROLE_005("BIZ_ROLE_005", "分配全局角色失败"),
    BIZ_ROLE_006("BIZ_ROLE_006", "更新成员角色失败"),

    // 对话与消息相关错误 (CHAT)
    BIZ_CHAT_001("BIZ_CHAT_001", "对话线程不存在，ID: %s"),
    BIZ_CHAT_002("BIZ_CHAT_002", "对话线程不存在或已被删除"),
    BIZ_CHAT_003("BIZ_CHAT_003", "无权限访问此对话线程"),
    BIZ_CHAT_004("BIZ_CHAT_004", "创建对话线程失败"),
    BIZ_CHAT_005("BIZ_CHAT_005", "更新对话线程失败"),
    BIZ_CHAT_006("BIZ_CHAT_006", "删除对话线程失败"),
    BIZ_CHAT_007("BIZ_CHAT_007", "获取对话线程列表失败"),
    BIZ_CHAT_008("BIZ_CHAT_008", "获取对话线程详情失败"),

    BIZ_CHAT_009("BIZ_CHAT_009", "消息不存在，ID: %s"),
    BIZ_CHAT_010("BIZ_CHAT_010", "消息不存在或已被删除"),
    BIZ_CHAT_011("BIZ_CHAT_011", "消息发送失败"),
    BIZ_CHAT_012("BIZ_CHAT_012", "获取对话消息失败"),
    BIZ_CHAT_013("BIZ_CHAT_013", "获取消息详情失败"),
    BIZ_CHAT_014("BIZ_CHAT_014", "删除消息失败"),
    BIZ_CHAT_015("BIZ_CHAT_015", "生成回复失败"),

    BIZ_CHAT_016("BIZ_CHAT_016", "消息内容不能为空"),
    BIZ_CHAT_017("BIZ_CHAT_017", "消息内容超出长度限制"),
    BIZ_CHAT_018("BIZ_CHAT_018", "消息内容过长，最大允许字符：%d"),
    BIZ_CHAT_019("BIZ_CHAT_019", "消息内容过短"),
    BIZ_CHAT_020("BIZ_CHAT_020", "不支持的内容类型"),
    BIZ_CHAT_021("BIZ_CHAT_021", "消息内容格式不正确"),

    BIZ_CHAT_022("BIZ_CHAT_022", "指定的AI模型暂时不可用: %s"),
    BIZ_CHAT_023("BIZ_CHAT_023", "指定的AI模型不存在"),
    BIZ_CHAT_024("BIZ_CHAT_024", "无权限使用指定的AI模型"),
    BIZ_CHAT_025("BIZ_CHAT_025", "AI模型使用配额已超限"),
    BIZ_CHAT_026("BIZ_CHAT_026", "AI模型请求失败"),
    BIZ_CHAT_027("BIZ_CHAT_027", "AI模型响应解析失败"),

    BIZ_CHAT_028("BIZ_CHAT_028", "流式连接建立失败"),
    BIZ_CHAT_029("BIZ_CHAT_029", "流式处理过程出错"),
    BIZ_CHAT_030("BIZ_CHAT_030", "流式响应超时"),
    BIZ_CHAT_031("BIZ_CHAT_031", "客户端断开连接"),

    BIZ_CHAT_032("BIZ_CHAT_032", "消息发送过于频繁，请稍后再试"),
    BIZ_CHAT_033("BIZ_CHAT_033", "用户消息发送频率超限"),
    BIZ_CHAT_034("BIZ_CHAT_034", "租户消息发送频率超限"),
    BIZ_CHAT_035("BIZ_CHAT_035", "并发处理数量超出限制"),

    BIZ_CHAT_036("BIZ_CHAT_036", "知识检索服务异常"),
    BIZ_CHAT_037("BIZ_CHAT_037", "工具调用服务异常"),
    BIZ_CHAT_038("BIZ_CHAT_038", "流程编排服务异常"),
    BIZ_CHAT_039("BIZ_CHAT_039", "指定的流程快照不存在或无效：%s"),

    BIZ_CHAT_040("BIZ_CHAT_040", "提交反馈失败"),
    BIZ_CHAT_041("BIZ_CHAT_041", "生成建议问题失败"),
    BIZ_CHAT_042("BIZ_CHAT_042", "重新生成回复失败"),
    BIZ_CHAT_043("BIZ_CHAT_043", "只能对用户消息重新生成回复"),
    BIZ_CHAT_044("BIZ_CHAT_044", "生成回复失败"),

    BIZ_CHAT_045("BIZ_CHAT_045", "对话服务暂时不可用"),
    BIZ_CHAT_046("BIZ_CHAT_046", "获取对话统计信息失败"),
    BIZ_CHAT_047("BIZ_CHAT_047", "获取使用量统计失败"),

    // 知识库错误 (KB)
    BIZ_KB_001("BIZ_KB_001", "文档不存在："),
    BIZ_KB_002("BIZ_KB_002", "文档内容已存在，标题："),
    BIZ_KB_003("BIZ_KB_003", "租户下已存在同名文档："),
    BIZ_KB_004("BIZ_KB_004", "不支持的文件类型: "),
    BIZ_KB_005("BIZ_KB_005", "文档大小超出限制"),
    BIZ_KB_006("BIZ_KB_006", "文档上传失败"),
    BIZ_KB_007("BIZ_KB_007", "批量上传失败"),
    BIZ_KB_008("BIZ_KB_008", "批量上传数量超出限制，最大允许: "),
    BIZ_KB_009("BIZ_KB_009", "批量文档总大小超出限制"),
    BIZ_KB_010("BIZ_KB_010", "更新文档信息失败"),
    BIZ_KB_011("BIZ_KB_011", "删除文档失败"),
    BIZ_KB_012("BIZ_KB_012", "文档内容过于复杂，预估Token数量: "),
    BIZ_KB_013("BIZ_KB_013", "相似度阈值必须在0-1之间"),
    BIZ_KB_014("BIZ_KB_014", "返回结果数量超出限制，最大允许: "),
    BIZ_KB_015("BIZ_KB_015", "获取文档详情失败"),
    BIZ_KB_016("BIZ_KB_016", "文档列表查询失败"),
    BIZ_KB_017("BIZ_KB_017", "知识块不存在或已被删除"),
    BIZ_KB_018("BIZ_KB_018", "标签不存在或已被删除"),
    BIZ_KB_019("BIZ_KB_019", "向量重新生成失败"),
    BIZ_KB_020("BIZ_KB_020", "批量向量生成失败"),
    BIZ_KB_021("BIZ_KB_021", "向量搜索失败"),
    BIZ_KB_022("BIZ_KB_022", "文本搜索失败"),
    BIZ_KB_023("BIZ_KB_023", "混合搜索失败"),
    BIZ_KB_024("BIZ_KB_024", "文档分块处理失败"),
    BIZ_KB_025("BIZ_KB_025", "获取知识块详情失败"),
    BIZ_KB_026("BIZ_KB_026", "获取知识库统计信息失败"),
    BIZ_KB_027("BIZ_KB_027", "标签名称已存在："),
    BIZ_KB_028("BIZ_KB_028", "添加知识块标签失败"),
    BIZ_KB_029("BIZ_KB_029", "移除知识块标签失败"),
    BIZ_KB_030("BIZ_KB_030", "创建标签失败"),
    BIZ_KB_031("BIZ_KB_031", "查询标签列表失败"),
    BIZ_KB_032("BIZ_KB_032", "获取热门标签失败"),
    BIZ_KB_033("BIZ_KB_033", "文本向量生成失败"),
    BIZ_KB_034("BIZ_KB_034", "查询扩展搜索失败"),
    BIZ_KB_035("BIZ_KB_035", "向量生成被中断"),

    // 流程编排错误 (FLOW)
    BIZ_FLOW_001("BIZ_FLOW_001", "流程不存在"),
    BIZ_FLOW_002("BIZ_FLOW_002", "流程名称已存在"),
    BIZ_FLOW_003("BIZ_FLOW_003", "流程状态不允许此操作"),
    BIZ_FLOW_004("BIZ_FLOW_004", "流程结构验证失败"),
    BIZ_FLOW_005("BIZ_FLOW_005", "流程执行失败"),
    BIZ_FLOW_006("BIZ_FLOW_006", "项目不存在"),

    BIZ_FLOW_007("BIZ_FLOW_007", "项目不存在，ID: %s"),
    BIZ_FLOW_008("BIZ_FLOW_008", "租户下已存在同名项目：%s"),
    BIZ_FLOW_009("BIZ_FLOW_009", "项目创建失败"),
    BIZ_FLOW_010("BIZ_FLOW_010", "项目更新失败"),
    BIZ_FLOW_011("BIZ_FLOW_011", "项目删除失败"),
    BIZ_FLOW_012("BIZ_FLOW_012", "项目列表查询失败"),
    BIZ_FLOW_013("BIZ_FLOW_013", "获取项目详情失败"),

    BIZ_FLOW_014("BIZ_FLOW_014", "流程定义不存在，ID: %s"),
    BIZ_FLOW_015("BIZ_FLOW_015", "项目内已存在同名流程：%s"),
    BIZ_FLOW_016("BIZ_FLOW_016", "只有草稿状态的流程才能修改结构"),
    BIZ_FLOW_017("BIZ_FLOW_017", "只有草稿状态的流程才能发布"),
    BIZ_FLOW_018("BIZ_FLOW_018", "只有已发布的流程才能执行"),
    BIZ_FLOW_019("BIZ_FLOW_019", "流程定义创建失败"),
    BIZ_FLOW_020("BIZ_FLOW_020", "流程定义更新失败"),
    BIZ_FLOW_021("BIZ_FLOW_021", "流程定义删除失败"),
    BIZ_FLOW_022("BIZ_FLOW_022", "流程定义列表查询失败"),
    BIZ_FLOW_023("BIZ_FLOW_023", "流程发布失败"),
    BIZ_FLOW_024("BIZ_FLOW_024", "流程发布验证失败"),

    BIZ_FLOW_025("BIZ_FLOW_025", "流程结构更新失败"),
    BIZ_FLOW_026("BIZ_FLOW_026", "获取流程结构失败"),
    BIZ_FLOW_027("BIZ_FLOW_027", "流程至少需要包含一个节点"),
    BIZ_FLOW_028("BIZ_FLOW_028", "流程必须包含开始节点"),
    BIZ_FLOW_029("BIZ_FLOW_029", "流程必须包含结束节点"),
    BIZ_FLOW_030("BIZ_FLOW_030", "流程只能包含一个开始节点"),
    BIZ_FLOW_031("BIZ_FLOW_031", "流程包含孤立节点"),
    BIZ_FLOW_032("BIZ_FLOW_032", "流程存在循环依赖"),
    BIZ_FLOW_033("BIZ_FLOW_033", "节点连接不正确"),

    BIZ_FLOW_034("BIZ_FLOW_034", "节点不存在"),
    BIZ_FLOW_035("BIZ_FLOW_035", "发现重复的节点Key：%s"),
    BIZ_FLOW_036("BIZ_FLOW_036", "不支持的节点类型：%s"),
    BIZ_FLOW_037("BIZ_FLOW_037", "节点创建失败"),
    BIZ_FLOW_038("BIZ_FLOW_038", "节点更新失败"),
    BIZ_FLOW_039("BIZ_FLOW_039", "节点删除失败"),
    BIZ_FLOW_040("BIZ_FLOW_040", "节点执行失败：%s"),

    BIZ_FLOW_041("BIZ_FLOW_041", "边不存在"),
    BIZ_FLOW_042("BIZ_FLOW_042", "边引用了不存在的源节点：%s"),
    BIZ_FLOW_043("BIZ_FLOW_043", "边引用了不存在的目标节点：%s"),
    BIZ_FLOW_044("BIZ_FLOW_044", "边创建失败"),
    BIZ_FLOW_045("BIZ_FLOW_045", "边更新失败"),
    BIZ_FLOW_046("BIZ_FLOW_046", "边删除失败"),

    BIZ_FLOW_047("BIZ_FLOW_047", "流程执行失败"),
    BIZ_FLOW_048("BIZ_FLOW_048", "无效的执行模式"),
    BIZ_FLOW_049("BIZ_FLOW_049", "流程执行超时"),
    BIZ_FLOW_050("BIZ_FLOW_050", "流程执行已取消"),
    BIZ_FLOW_051("BIZ_FLOW_051", "停止执行失败"),

    BIZ_FLOW_052("BIZ_FLOW_052", "运行实例不存在，ID: %s"),
    BIZ_FLOW_053("BIZ_FLOW_053", "只有运行中的流程才能停止"),
    BIZ_FLOW_054("BIZ_FLOW_054", "获取运行详情失败"),
    BIZ_FLOW_055("BIZ_FLOW_055", "获取运行历史失败"),
    BIZ_FLOW_056("BIZ_FLOW_056", "获取运行日志失败"),

    BIZ_FLOW_057("BIZ_FLOW_057", "流程快照不存在，请重新发布流程"),
    BIZ_FLOW_058("BIZ_FLOW_058", "流程快照不存在，ID: %s"),
    BIZ_FLOW_059("BIZ_FLOW_059", "快照创建失败"),
    BIZ_FLOW_060("BIZ_FLOW_060", "获取快照失败"),
    BIZ_FLOW_061("BIZ_FLOW_061", "快照创建失败: JSON序列化错误"),
    BIZ_FLOW_062("BIZ_FLOW_062", "获取流程统计信息失败"),

    BIZ_FLOW_063("BIZ_FLOW_063", "创建新版本失败"),
    BIZ_FLOW_064("BIZ_FLOW_064", "获取版本历史失败"),
    BIZ_FLOW_065("BIZ_FLOW_065", "版本号不正确"),

    BIZ_FLOW_066("BIZ_FLOW_066", "流程快照不可用，ID: %s"),
    BIZ_FLOW_067("BIZ_FLOW_067", "执行被中断"),
    BIZ_FLOW_068("BIZ_FLOW_068", "未找到节点执行器：%s"),
    BIZ_FLOW_069("BIZ_FLOW_069", "快照解析失败"),

    BIZ_FLOW_070("BIZ_FLOW_070", "流程结构验证失败"),
    BIZ_FLOW_071("BIZ_FLOW_071", "开始节点不能有输入连接"),
    BIZ_FLOW_072("BIZ_FLOW_072", "结束节点不能有输出连接"),
    BIZ_FLOW_073("BIZ_FLOW_073", "节点 %s 配置无效: %s"),
    BIZ_FLOW_074("BIZ_FLOW_074", "无效的连接: %s -> %s"),
    BIZ_FLOW_075("BIZ_FLOW_075", "存在不可达的节点"),
    BIZ_FLOW_076("BIZ_FLOW_076", "关键节点 %s 缺少必要配置"),

    // MCP工具错误 (MCP)
    BIZ_MCP_001("BIZ_MCP_001", "工具不存在"),
    BIZ_MCP_002("BIZ_MCP_002", "工具代码已存在"),
    BIZ_MCP_003("BIZ_MCP_003", "工具已被禁用"),
    BIZ_MCP_004("BIZ_MCP_004", "工具注册失败"),
    BIZ_MCP_005("BIZ_MCP_005", "工具更新失败"),
    BIZ_MCP_006("BIZ_MCP_006", "工具执行超时"),
    BIZ_MCP_007("BIZ_MCP_007", "工具执行失败"),
    BIZ_MCP_008("BIZ_MCP_008", "工具调用配额已用完"),
    BIZ_MCP_009("BIZ_MCP_009", "获取工具列表失败"),
    BIZ_MCP_010("BIZ_MCP_010", "获取工具详情失败"),
    BIZ_MCP_011("BIZ_MCP_011", "工具参数Schema格式不正确"),
    BIZ_MCP_012("BIZ_MCP_012", "工具结果Schema格式不正确"),
    BIZ_MCP_013("BIZ_MCP_013", "工具端点URL不能为空"),
    BIZ_MCP_014("BIZ_MCP_014", "工具服务暂时不可用"),
    BIZ_MCP_015("BIZ_MCP_015", "获取工具执行历史失败"),
    BIZ_MCP_016("BIZ_MCP_016", "获取工具统计信息失败"),
    BIZ_MCP_017("BIZ_MCP_017", "租户 %d 的工具 %s 请求频率过高"),

    // MCP工具授权错误 (TOOL AUTH)
    BIZ_AUTH_001("BIZ_AUTH_001", "租户已被授权使用此工具"),
    BIZ_AUTH_002("BIZ_AUTH_002", "授权记录不存在"),
    BIZ_AUTH_003("BIZ_AUTH_003", "租户未被授权使用此工具"),
    BIZ_AUTH_004("BIZ_AUTH_004", "工具授权已被禁用"),
    BIZ_AUTH_005("BIZ_AUTH_005", "租户授权失败"),
    BIZ_AUTH_006("BIZ_AUTH_006", "撤销授权失败"),
    BIZ_AUTH_007("BIZ_AUTH_007", "获取租户授权工具失败"),

    // 文件管理错误 (FILE)
    BIZ_FILE_001("BIZ_FILE_001", "文件不存在"),
    BIZ_FILE_002("BIZ_FILE_002", "文件大小超出限制"),
    BIZ_FILE_003("BIZ_FILE_003", "文件类型不支持"),
    BIZ_FILE_004("BIZ_FILE_004", "文件上传失败"),
    BIZ_FILE_005("BIZ_FILE_005", "SHA256哈希值格式无效"),

    // 模板管理错误 (TEMPLATE)
    BIZ_TEMPLATE_001("BIZ_TEMPLATE_001", "提示词模板不存在："),
    BIZ_TEMPLATE_002("BIZ_TEMPLATE_002", "模板名称已存在："),
    BIZ_TEMPLATE_003("BIZ_TEMPLATE_003", "模板内容格式无效"),
    BIZ_TEMPLATE_004("BIZ_TEMPLATE_004", "创建提示词模板失败"),
    BIZ_TEMPLATE_005("BIZ_TEMPLATE_005", "系统模板或已删除的模板无法编辑"),
    BIZ_TEMPLATE_006("BIZ_TEMPLATE_006", "模板内容格式无效或包含危险字符"),
    BIZ_TEMPLATE_007("BIZ_TEMPLATE_007", "更新模板失败"),
    BIZ_TEMPLATE_008("BIZ_TEMPLATE_008", "获取模板列表失败"),
    BIZ_TEMPLATE_009("BIZ_TEMPLATE_009", "获取模板详情失败"),

    // 审计服务错误 (AUDIT)
    BIZ_AUDIT_001("BIZ_AUDIT_001", "创建审计记录失败: %s"),
    BIZ_AUDIT_002("BIZ_AUDIT_002", "批量创建审计记录失败: %s"),
    BIZ_AUDIT_003("BIZ_AUDIT_003", "创建安全审计记录失败"),
    BIZ_AUDIT_004("BIZ_AUDIT_004", "审计日志查询失败: %s"),
    BIZ_AUDIT_005("BIZ_AUDIT_005", "安全事件查询失败"),
    BIZ_AUDIT_006("BIZ_AUDIT_006", "对象历史查询失败"),
    BIZ_AUDIT_007("BIZ_AUDIT_007", "审计统计计算失败"),
    BIZ_AUDIT_008("BIZ_AUDIT_008", "用户审计摘要生成失败"),
    BIZ_AUDIT_009("BIZ_AUDIT_009", "审计报告导出失败"),
    BIZ_AUDIT_010("BIZ_AUDIT_010", "报告生成失败: %s"),
    BIZ_AUDIT_011("BIZ_AUDIT_011", "生成摘要报告失败"),
    BIZ_AUDIT_012("BIZ_AUDIT_012", "生成安全分析报告失败"),
    BIZ_AUDIT_013("BIZ_AUDIT_013", "生成合规报告失败"),
    BIZ_AUDIT_014("BIZ_AUDIT_014", "找不到报告生成器: %s"),
    BIZ_AUDIT_015("BIZ_AUDIT_015", "收集报告数据失败"),
    BIZ_AUDIT_016("BIZ_AUDIT_016", "保存报告内容失败"),

    BIZ_AUDIT_017("BIZ_AUDIT_017", "保存审计日志失败: action=%s, userId=%s"),
    BIZ_AUDIT_018("BIZ_AUDIT_018", "审计日志数据验证失败: "),
    BIZ_AUDIT_019("BIZ_AUDIT_019", "保存审计日志时发生未知错误"),
    BIZ_AUDIT_020("BIZ_AUDIT_020", "查询审计日志失败: id=%d"),
    BIZ_AUDIT_021("BIZ_AUDIT_021", "查询用户操作历史失败: userId=%s"),
    BIZ_AUDIT_022("BIZ_AUDIT_022", "查询租户审计日志失败: tenantId=%d"),
    BIZ_AUDIT_023("BIZ_AUDIT_023", "按时间范围查询审计日志失败"),
    BIZ_AUDIT_024("BIZ_AUDIT_024", "统计审计日志数量失败"),
    BIZ_AUDIT_025("BIZ_AUDIT_025", "按操作类型统计审计日志失败"),
    BIZ_AUDIT_026("BIZ_AUDIT_026", "按用户统计审计日志失败"),
    BIZ_AUDIT_027("BIZ_AUDIT_027", "删除旧审计日志失败"),
    BIZ_AUDIT_028("BIZ_AUDIT_028", "查询对象操作历史失败: targetType=%s, targetId=%d"),
    BIZ_AUDIT_029("BIZ_AUDIT_029", "批量保存审计日志失败: 数量=%d"),
    BIZ_AUDIT_030("BIZ_AUDIT_030", "解析审计日志详细信息失败"),
    BIZ_AUDIT_031("BIZ_AUDIT_031", "发布审计事件失败"),
    BIZ_AUDIT_032("BIZ_AUDIT_032", "发布安全审计事件失败"),
    BIZ_AUDIT_033("BIZ_AUDIT_033", "发布数据变更审计事件失败"),
    BIZ_AUDIT_034("BIZ_AUDIT_034", "记录用户操作审计失败"),
    BIZ_AUDIT_035("BIZ_AUDIT_035", "记录系统事件审计失败"),
    BIZ_AUDIT_036("BIZ_AUDIT_036", "记录安全事件审计失败"),
    BIZ_AUDIT_037("BIZ_AUDIT_037", "记录业务操作审计失败"),
    BIZ_AUDIT_038("BIZ_AUDIT_038", "批量记录审计事件失败"),
    BIZ_AUDIT_039("BIZ_AUDIT_039", "查询用户操作历史失败"),
    BIZ_AUDIT_040("BIZ_AUDIT_040", "查询安全事件历史失败"),
    BIZ_AUDIT_041("BIZ_AUDIT_041", "生成审计报告失败"),
    BIZ_AUDIT_042("BIZ_AUDIT_042", "获取审计统计信息失败"),
    BIZ_AUDIT_043("BIZ_AUDIT_043", "配置数据保留策略失败"),

    BIZ_AUDIT_044("BIZ_AUDIT_044", "验证审计数据完整性失败"),
    BIZ_AUDIT_045("BIZ_AUDIT_045", "处理审计事件失败"),

    // =================== 外部集成错误 (EXT) ===================

    // AI模型错误 (AI)
    EXT_AI_001("EXT_AI_001", "AI模型服务不可用"),
    EXT_AI_002("EXT_AI_002", "模型服务未初始化: "),
    EXT_AI_003("EXT_AI_003", "AI模型响应超时"),
    EXT_AI_004("EXT_AI_004", "AI模型配额不足"),
    EXT_AI_005("EXT_AI_005", "不支持的模型: "),
    EXT_AI_006("EXT_AI_006", "文本过长，估算Token数: %d，模型限制: %d"),
    EXT_AI_007("EXT_AI_007", "API请求频率过高，请稍后重试"),

    EXT_AI_008("EXT_AI_008", "OpenAI返回空响应"),
    EXT_AI_009("EXT_AI_009", "OpenAI API调用失败: "),
    EXT_AI_010("EXT_AI_010", "OpenAI批量API调用失败: "),

    EXT_AI_011("EXT_AI_011", "不支持的千问模型: "),
    EXT_AI_012("EXT_AI_012", "千问返回空响应"),
    EXT_AI_013("EXT_AI_013", "千问API调用失败: "),
    EXT_AI_014("EXT_AI_014", "千问批量API调用失败: "),

    EXT_AI_015("EXT_AI_015", "Claude返回空响应"),
    EXT_AI_016("EXT_AI_016", "Claude API调用失败: "),
    EXT_AI_017("EXT_AI_017", "Claude生成聊天完成时发生错误"),
    EXT_AI_018("EXT_AI_018", "Claude流式生成失败"),
    EXT_AI_019("EXT_AI_019", "Claude流式处理被中断"),

    EXT_AI_020("EXT_AI_020", "OpenAI生成聊天完成时发生未知错误"),
    EXT_AI_021("EXT_AI_021", "OpenAI流式响应处理失败"),
    EXT_AI_022("EXT_AI_022", "OpenAI启动流式生成失败"),
    EXT_AI_023("EXT_AI_023", "OpenAI流式处理被中断"),

    EXT_AI_024("EXT_AI_024", "没有可用的聊天服务"),
    EXT_AI_025("EXT_AI_025","所有模型调用失败: "),

    // 第三方API错误 (API)
    EXT_API_001("EXT_API_001", "第三方API调用失败"),
    EXT_API_002("EXT_API_002", "第三方API认证失败"),
    EXT_API_003("EXT_API_003", "第三方API限流"),

    // 存储服务错误 (STORAGE)
    EXT_STORAGE_001("EXT_STORAGE_001", "文件存储服务不可用"),
    EXT_STORAGE_002("EXT_STORAGE_002", "文件上传失败"),
    EXT_STORAGE_003("EXT_STORAGE_003", "文件下载失败"),
    EXT_STORAGE_004("EXT_STORAGE_004", "获取存储统计失败"),

    // 邮件服务错误 (EMAIL)
    EXT_EMAIL_001("EXT_EMAIL_001", "用户邮箱不能为空"),
    EXT_EMAIL_002("EXT_EMAIL_002", "用户名不能为空"),
    EXT_EMAIL_003("EXT_EMAIL_003", "受邀用户邮箱不能为空"),
    EXT_EMAIL_004("EXT_EMAIL_004", "组织名称不能为空"),
    EXT_EMAIL_005("EXT_EMAIL_005", "激活码不能为空"),
    EXT_EMAIL_006("EXT_EMAIL_006", "邀请令牌不能为空"),
    EXT_EMAIL_007("EXT_EMAIL_007", "重置令牌不能为空"),
    EXT_EMAIL_008("EXT_EMAIL_008", "模板名称不能为空"),
    EXT_EMAIL_009("EXT_EMAIL_009", "邮箱列表不能为空"),
    EXT_EMAIL_010("EXT_EMAIL_010", "邮件主题不能为空"),
    EXT_EMAIL_011("EXT_EMAIL_011", "邮件内容不能为空"),
    EXT_EMAIL_012("EXT_EMAIL_012", "邮箱格式不正确: %s"),
    EXT_EMAIL_013("EXT_EMAIL_013", "模板引擎不可用，无法发送模板邮件"),
    EXT_EMAIL_014("EXT_EMAIL_014", "发送邀请邮件失败"),
    EXT_EMAIL_015("EXT_EMAIL_015", "发送激活邮件失败"),
    EXT_EMAIL_016("EXT_EMAIL_016", "发送模板邮件失败"),
    EXT_EMAIL_017("EXT_EMAIL_017", "发送通知邮件失败"),
    EXT_EMAIL_018("EXT_EMAIL_018", "批量发送邮件失败"),
    EXT_EMAIL_019("EXT_EMAIL_019", "发送密码重置邮件失败"),
    EXT_EMAIL_020("EXT_EMAIL_020", "邮件服务商不可用"),

    // 短信服务错误 (SMS)
    EXT_SMS_001("EXT_SMS_001", "发送验证码短信失败"),
    EXT_SMS_002("EXT_SMS_002", "发送安全警报短信失败"),
    EXT_SMS_003("EXT_SMS_003", "发送业务通知短信失败"),
    EXT_SMS_004("EXT_SMS_004", "发送模板短信失败"),
    EXT_SMS_005("EXT_SMS_005", "批量发送短信失败"),
    EXT_SMS_006("EXT_SMS_006", "查询短信状态失败"),
    EXT_SMS_007("EXT_SMS_007", "查询短信余额失败"),
    EXT_SMS_008("EXT_SMS_008", "手机号在黑名单中"),
    EXT_SMS_009("EXT_SMS_009", "发送频率过高，请稍后再试（每分钟限制 %s 条）"),
    EXT_SMS_010("EXT_SMS_010", "发送频率过高，请稍后再试（每小时限制 %s 条）"),
    EXT_SMS_011("EXT_SMS_011", "今日发送次数已达上限（每天限制 %s 条）"),
    EXT_SMS_012("EXT_SMS_012", "请不要重复发送验证码，请等待 %s 分钟后再试"),
    EXT_SMS_013("EXT_SMS_013", "不支持的短信服务商: %s "),

    EXT_SMS_014("EXT_SMS_014", "【%s】您的%s验证码是：%s，有效期%d分钟，请勿泄露给他人。"),
    EXT_SMS_015("EXT_SMS_015", "【%s】您的账户于%s在%s通过%s登录，如非本人操作请立即修改密码。"),
    EXT_SMS_016("EXT_SMS_016", "手机号格式不正确: %s"),
    EXT_SMS_017("EXT_SMS_017", "验证码格式不正确，应为4-6位数字"),
    EXT_SMS_018("EXT_SMS_018", "验证码有效期必须在1-30分钟之间"),
    EXT_SMS_019("EXT_SMS_019", "发送目的不能为空"),
    EXT_SMS_020("EXT_SMS_020", "登录时间不能为空"),
    EXT_SMS_021("EXT_SMS_021", "登录地点不能为空"),

    // =================== 参数验证错误 (PARAM) ===================
    PARAM_001("PARAM_001", "必需参数缺失"),
    PARAM_002("PARAM_002", "参数格式不正确"),
    PARAM_003("PARAM_003", "参数值超出范围"),
    PARAM_004("PARAM_004", "参数组合无效"),
    PARAM_005("PARAM_005", "客户端IP地址为空"),
    PARAM_006("PARAM_006", "IP地址不在白名单中: "),
    PARAM_007("PARAM_007", "API密钥不能为空"),
    PARAM_008("PARAM_008", "API密钥格式无效"),
    PARAM_009("PARAM_009", "参数包含潜在的SQL注入模式: "),
    PARAM_010("PARAM_010", "参数包含潜在的脚本注入模式: "),

    PARAM_011("PARAM_011", "租户ID不能为null"),
    PARAM_012("PARAM_012", "操作类型不能为空"),
    PARAM_013("PARAM_013", "操作描述不能为空"),
    PARAM_014("PARAM_014", "操作类型长度不能超过64个字符"),
    PARAM_015("PARAM_015", "操作描述长度不能超过1000个字符"),
    PARAM_016("PARAM_016", "审计事件列表不能为null"),
    PARAM_017("PARAM_017", "审计事件列表不能为空"),
    PARAM_018("PARAM_018", "批量处理数量不能超过1000条"),
    PARAM_019("PARAM_019", "页码不能小于0"),
    PARAM_020("PARAM_020", "页大小必须在1-100之间"),
    PARAM_021("PARAM_021", "开始时间不能晚于结束时间"),
    PARAM_022("PARAM_022", "不支持的报告类型: %s"),
    PARAM_023("PARAM_023", "页大小必须大于0"),
    PARAM_024("PARAM_024", "页大小不能超过1000，当前值: "),
    PARAM_025("PARAM_025", "开始时间不能晚于结束时间: startTime=%s, endTime=%s"),

    MOCK_001("MOCK_001", "模拟的随机错误，用于测试错误处理"),

    // =================== 未知错误 ===================
    UNKNOWN("UNKNOWN", "未知错误");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}