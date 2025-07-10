# Chat模块使用指南

以下是Chat模块的完整使用示例，展示了如何在实际项目中使用这些组件。

## 1\. 基础对话功能

```java
// 创建对话线程
CreateChatThreadCommand createCmd = new CreateChatThreadCommand(
    tenantId, userId, "技术咨询", "gpt-4o", 0.7f, null, "您好，我是AI助手", operatorId
);
ChatThreadDTO thread = chatService.createThread(createCmd);

// 发送消息
SendMessageCommand sendCmd = new SendMessageCommand(
    "如何在Spring Boot中实现分布式锁？", "TEXT", true, false, 0.7f, 2000, false
);
ChatResponseDTO response = chatService.sendMessage(thread.id(), sendCmd);
```

## 2\. 流式对话

```java
@GetMapping("/stream")
public SseEmitter streamChat(@RequestParam Long threadId, @RequestParam String message) {
    SseEmitter emitter = new SseEmitter(300_000L);

    SendMessageCommand cmd = new SendMessageCommand(message, "TEXT", true, true, 0.8f, 2000, true);
    chatService.sendMessageStream(threadId, cmd, emitter);

    return emitter;
}
```

## 3\. 健康检查集成

```java
@Autowired
private ChatHealthIndicator chatHealthIndicator;

public void checkSystemHealth() {
    Health health = chatHealthIndicator.health();
    if (health.getStatus() == Status.DOWN) {
        // 触发告警或故障转移
        alertManager.sendAlert("Chat系统健康检查失败");
    }
}
```

## 4\. 指标监控

```java
@Autowired
private ChatMetricsCollector metricsCollector;

public void recordChatActivity(String model, int tokens, long responseTime) {
    metricsCollector.recordMessageSent(model, tokens);
    metricsCollector.recordMessageReceived(model, tokens, responseTime);
}
```

## 5\. 集成服务使用

```java
@Autowired
private ChatIntegrationService integrationService;

public void handleComplexQuery(String userQuery, Long tenantId, Long userId, Long threadId) {
    // 构建智能上下文
    Map<String, Object> baseContext = Map.of(
        "model", "gpt-4o",
        "temperature", 0.7f,
        "enableKnowledgeRetrieval", true,
        "enableToolCalling", true
    );

    IntelligentContext context = integrationService.buildIntelligentContext(
        tenantId, userId, threadId, userQuery, baseContext
    );

    // 使用增强的上下文生成回复
    Map<String, Object> llmContext = context.toLLMContext();
    ChatCompletionResult result = chatCompletionService.generateCompletion(llmContext);
}
```

## 配置最佳实践

* **环境隔离：** 使用Spring Profiles进行环境配置隔离
* **敏感信息：** 使用环境变量或加密配置存储API密钥
* **性能调优：** 根据实际负载调整线程池和超时参数
* **监控告警：** 配置适当的健康检查和指标阈值
* **故障恢复：** 启用故障转移和重试机制