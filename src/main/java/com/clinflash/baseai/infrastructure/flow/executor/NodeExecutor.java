package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.clinflash.baseai.infrastructure.flow.model.NodeExecutionInfo;

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
     *
     * <p>告诉流程引擎，当前执行器能够处理哪些类型的节点。
     * 引擎会根据这个信息来路由节点到正确的执行器。</p>
     *
     * @return 支持的节点类型代码列表
     */
    List<String> getSupportedNodeTypes();

    /**
     * 执行节点核心逻辑
     *
     * <p>这是执行器的核心方法，负责将抽象的节点配置转换为具体的执行结果。</p>
     *
     * @param nodeInfo 节点执行信息，包含类型、配置等
     * @param input    输入数据，来自上游节点或流程启动参数
     * @param context  执行上下文，提供全局状态和服务
     * @return 执行结果，将传递给下游节点
     * @throws RuntimeException 当节点执行失败时抛出
     */
    Map<String, Object> execute(NodeExecutionInfo nodeInfo, Map<String, Object> input, FlowExecutionContext context);

    /**
     * 验证节点配置的有效性
     *
     * <p>在流程发布之前，系统会调用这个方法来验证所有节点的配置是否正确。
     * 这是一个"预检查"机制，可以在设计阶段就发现配置错误。</p>
     *
     * @param configJson 节点配置JSON字符串
     * @throws IllegalArgumentException 当配置无效时抛出
     */
    default void validateConfig(String configJson) {
        // 默认实现：不进行验证
        // 子类可以重写此方法提供具体的验证逻辑
    }

    /**
     * 检查执行器健康状态
     *
     * <p>用于监控执行器的健康状态，确保所有依赖的服务都正常工作。
     * 比如AI执行器需要检查LLM服务是否可用，HTTP执行器需要检查网络连接等。</p>
     *
     * @return 如果执行器健康则返回true，否则返回false
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * 获取执行器的优先级
     *
     * <p>当多个执行器都支持同一种节点类型时，系统会选择优先级最高的执行器。
     * 这为系统提供了灵活的执行器选择机制。</p>
     *
     * @return 优先级数值，数值越大优先级越高
     */
    default int getPriority() {
        return 0;
    }

    /**
     * 获取执行器名称
     *
     * <p>用于日志记录和监控，了解是哪个执行器在处理节点。</p>
     *
     * @return 执行器的友好名称
     */
    default String getExecutorName() {
        return this.getClass().getSimpleName();
    }
}