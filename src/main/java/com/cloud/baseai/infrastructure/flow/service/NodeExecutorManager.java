package com.cloud.baseai.infrastructure.flow.service;

import com.cloud.baseai.infrastructure.flow.executor.NodeExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>节点执行器管理器</h2>
 *
 * <p>这个管理器是执行器生态系统的"调度中心"，负责管理所有类型的节点执行器，
 * 并为流程引擎提供统一的执行器发现和调用接口。它就像一个智能的"工具箱管理员"，
 * 知道每种工具的用途，并能快速找到合适的工具来完成特定任务。</p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>执行器注册：</b>自动发现和注册所有可用的执行器</li>
 * <li><b>路由分发：</b>根据节点类型选择合适的执行器</li>
 * <li><b>优先级管理：</b>支持执行器优先级和替换机制</li>
 * <li><b>健康监控：</b>监控执行器的健康状态</li>
 * </ul>
 */
@Component
public class NodeExecutorManager {

    private static final Logger log = LoggerFactory.getLogger(NodeExecutorManager.class);

    /**
     * 节点类型到执行器的映射
     */
    private final Map<String, NodeExecutor> executorMap = new ConcurrentHashMap<>();

    /**
     * 所有注册的执行器列表
     */
    private final List<NodeExecutor> allExecutors = new ArrayList<>();

    public NodeExecutorManager(List<NodeExecutor> executors) {
        registerExecutors(executors);
    }

    /**
     * 获取指定节点类型的执行器
     *
     * @param nodeType 节点类型代码
     * @return 对应的执行器，如果没找到则返回null
     */
    public NodeExecutor getExecutor(String nodeType) {
        return executorMap.get(nodeType);
    }

    /**
     * 检查是否支持指定的节点类型
     */
    public boolean isNodeTypeSupported(String nodeType) {
        return executorMap.containsKey(nodeType);
    }

    /**
     * 获取所有支持的节点类型
     */
    public Set<String> getSupportedNodeTypes() {
        return new HashSet<>(executorMap.keySet());
    }

    /**
     * 获取所有执行器的健康状态
     */
    public Map<String, Boolean> getExecutorHealthStatus() {
        Map<String, Boolean> healthStatus = new HashMap<>();

        for (NodeExecutor executor : allExecutors) {
            try {
                boolean isHealthy = executor.isHealthy();
                healthStatus.put(executor.getExecutorName(), isHealthy);
            } catch (Exception e) {
                log.warn("检查执行器健康状态失败: {}", executor.getExecutorName(), e);
                healthStatus.put(executor.getExecutorName(), false);
            }
        }

        return healthStatus;
    }

    /**
     * 注册所有执行器
     */
    private void registerExecutors(List<NodeExecutor> executors) {
        log.info("开始注册节点执行器，共 {} 个", executors.size());

        // 按优先级排序
        executors.sort((e1, e2) -> Integer.compare(e2.getPriority(), e1.getPriority()));

        for (NodeExecutor executor : executors) {
            registerExecutor(executor);
        }

        log.info("节点执行器注册完成，支持 {} 种节点类型", executorMap.size());
    }

    /**
     * 注册单个执行器
     */
    private void registerExecutor(NodeExecutor executor) {
        try {
            List<String> supportedTypes = executor.getSupportedNodeTypes();

            for (String nodeType : supportedTypes) {
                NodeExecutor existing = executorMap.get(nodeType);

                if (existing == null || executor.getPriority() > existing.getPriority()) {
                    executorMap.put(nodeType, executor);
                    log.debug("注册执行器: {} -> {}", nodeType, executor.getExecutorName());
                } else {
                    log.debug("跳过低优先级执行器: {} (优先级: {}) < {} (优先级: {})",
                            executor.getExecutorName(), executor.getPriority(),
                            existing.getExecutorName(), existing.getPriority());
                }
            }

            allExecutors.add(executor);

        } catch (Exception e) {
            log.error("注册执行器失败: {}", executor.getClass().getName(), e);
        }
    }
}