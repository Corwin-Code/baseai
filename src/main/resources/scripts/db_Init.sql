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