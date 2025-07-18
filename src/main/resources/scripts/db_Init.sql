-- 插入系统角色
INSERT INTO sys_roles (name, label)
VALUES ('SUPER_ADMIN', '超级管理员'),
       ('SYSTEM_ADMIN', '系统管理员'),
       ('TENANT_OWNER', '租户所有者'),
       ('TENANT_ADMIN', '租户管理员'),
       ('USER', '普通用户'),
       ('AUDITOR', '审计员'),
       ('TOOL_ADMIN', '工具管理员');

-- 创建超级管理员用户
INSERT INTO sys_users (username, email, password_hash)
VALUES ('admin', 'admin@baseai.com', '$2a$12$your-bcrypt-encoded-password');

-- 分配超级管理员角色
INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sys_users u,
     sys_roles r
WHERE u.username = 'admin'
  AND r.name = 'SUPER_ADMIN';

------------------------------------------------------------------------------

-- 初始化节点类型字典数据
INSERT INTO dict_flow_node_types (code, label)
VALUES
-- 基础节点
('START', '开始'),
('END', '结束'),
('INPUT', '输入'),
('OUTPUT', '输出'),

-- AI节点
('RETRIEVER', '知识库检索'),
('EMBEDDER', '向量生成'),
('LLM', '大语言模型'),
('CHAT', '对话'),
('CLASSIFIER', '分类器'),

-- 流程控制节点
('CONDITION', '条件判断'),
('LOOP', '循环'),
('PARALLEL', '并行处理'),
('SWITCH', '分支选择'),

-- 工具节点
('TOOL', 'MCP工具'),
('HTTP', 'HTTP请求'),
('SCRIPT', '脚本执行'),

-- 数据处理节点
('MAPPER', '数据映射'),
('FILTER', '数据过滤'),
('VALIDATOR', '数据验证'),
('SPLITTER', '文本分割'),
('MERGER', '数据合并')
ON CONFLICT (code) DO NOTHING;