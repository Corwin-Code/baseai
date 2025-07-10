package com.clinflash.baseai.infrastructure.flow.executor;

import java.util.List;
import java.util.Map;

/**
 * <h2>节点执行器接口</h2>
 *
 * <p>定义了节点执行的统一接口。每种类型的节点都有对应的执行器实现，
 * 负责将抽象的节点配置转换为具体的执行逻辑。</p>
 */
public interface NodeExecutor {

    /**
     * 获取支持的节点类型列表
     */
    List<String> getSupportedNodeTypes();

    /**
     * 执行节点
     *
     * @param nodeInfo 节点信息
     * @param input    输入数据
     * @param context  执行上下文
     * @return 执行结果
     */
    Map<String, Object> execute(Object nodeInfo, Map<String, Object> input, Object context);

    /**
     * 验证节点配置
     */
    default void validateConfig(String configJson) {
        // 默认实现：不进行验证
    }
}