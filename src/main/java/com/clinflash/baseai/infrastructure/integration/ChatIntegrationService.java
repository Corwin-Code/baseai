package com.clinflash.baseai.infrastructure.integration;

import com.clinflash.baseai.application.flow.command.ExecuteFlowCommand;
import com.clinflash.baseai.application.flow.service.FlowOrchestrationAppService;
import com.clinflash.baseai.application.kb.command.VectorSearchCommand;
import com.clinflash.baseai.application.kb.dto.SearchResultDTO;
import com.clinflash.baseai.application.kb.service.KnowledgeBaseAppService;
import com.clinflash.baseai.application.mcp.command.ExecuteToolCommand;
import com.clinflash.baseai.application.mcp.service.McpApplicationService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <h2>对话集成服务</h2>
 *
 * <p>这个服务是对话系统的"大脑皮层"，负责协调和整合各个子系统的功能。
 * 它就像一个经验丰富的指挥家，知道什么时候该让哪个乐器演奏，
 * 如何让不同的模块协同工作，创造出和谐的用户体验。</p>
 *
 * <p><b>集成的挑战与解决方案：</b></p>
 * <p>在微服务架构中，各个服务都有自己的职责边界，但用户的需求往往
 * 是跨越多个服务的。例如，一个用户问题可能需要：</p>
 * <p>1. 从知识库检索相关信息</p>
 * <p>2. 调用外部工具获取实时数据</p>
 * <p>3. 执行复杂的业务流程</p>
 * <p>4. 生成个性化的回答</p>
 * <p>这个集成服务就是要解决这种复杂的协调问题。</p>
 */
@Service
public class ChatIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ChatIntegrationService.class);

    private final KnowledgeBaseAppService kbService;
    private final McpApplicationService mcpService;
    private final FlowOrchestrationAppService flowService;

    public ChatIntegrationService(
            KnowledgeBaseAppService kbService,
            McpApplicationService mcpService,
            FlowOrchestrationAppService flowService) {

        this.kbService = kbService;
        this.mcpService = mcpService;
        this.flowService = flowService;
    }

    /**
     * 智能内容增强
     *
     * <p>这是RAG系统的核心功能。根据用户的问题，智能地从知识库中
     * 检索相关内容，为AI生成提供准确的背景信息。</p>
     */
    public ContentEnhancementResult enhanceWithKnowledge(
            Long tenantId,
            String userQuery,
            String embeddingModel,
            int maxResults,
            float threshold) {

        try {
            log.debug("开始知识增强: tenantId={}, query={}", tenantId, userQuery);

            var searchCommand = new VectorSearchCommand(
                    tenantId,
                    userQuery,
                    embeddingModel,
                    maxResults,
                    threshold,
                    true // 包含元数据
            );

            var searchResults = kbService.vectorSearch(searchCommand);

            List<String> knowledgeContext = searchResults.stream()
                    .map(SearchResultDTO::text)
                    .collect(Collectors.toList());

            List<Long> sourceChunkIds = searchResults.stream()
                    .map(SearchResultDTO::chunkId)
                    .collect(Collectors.toList());

            List<Float> relevanceScores = searchResults.stream()
                    .map(SearchResultDTO::score)
                    .collect(Collectors.toList());

            return new ContentEnhancementResult(
                    knowledgeContext,
                    sourceChunkIds,
                    relevanceScores,
                    embeddingModel
            );

        } catch (Exception e) {
            log.error("知识增强失败: tenantId={}, query={}", tenantId, userQuery, e);
            return ContentEnhancementResult.empty();
        }
    }

    /**
     * 智能工具调用
     *
     * <p>分析用户的意图，自动选择和调用合适的工具。这个功能让AI不再局限于
     * 训练数据，而是能够获取实时信息、执行实际操作。</p>
     */
    public ToolExecutionResult executeIntelligentTools(
            Long tenantId,
            Long userId,
            Long threadId,
            String userQuery,
            Map<String, Object> context) {

        try {
            log.debug("开始智能工具调用: tenantId={}, query={}", tenantId, userQuery);

            // 1. 分析用户意图，识别需要的工具
            List<String> requiredTools = analyzeToolRequirements(userQuery, context);

            if (requiredTools.isEmpty()) {
                return ToolExecutionResult.empty();
            }

            // 2. 并行执行多个工具调用
            List<CompletableFuture<ToolCallResult>> futures = requiredTools.stream()
                    .map(toolCode -> executeToolAsync(toolCode, tenantId, userId, threadId, userQuery, context))
                    .toList();

            // 3. 等待所有工具执行完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            List<ToolCallResult> results = allOf.thenApply(v ->
                    futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            ).get();

            return new ToolExecutionResult(results);

        } catch (Exception e) {
            log.error("智能工具调用失败: tenantId={}, query={}", tenantId, userQuery, e);
            return ToolExecutionResult.empty();
        }
    }

    /**
     * 流程编排执行
     *
     * <p>对于复杂的业务场景，需要按照预定义的流程进行处理。
     * 这个方法执行指定的业务流程，处理复杂的多步骤操作。</p>
     */
    public FlowExecutionResult executeBusinessFlow(
            Long flowSnapshotId,
            Map<String, Object> inputs,
            Long userId) {

        try {
            log.debug("开始流程执行: flowSnapshotId={}", flowSnapshotId);

            var executeCommand = new ExecuteFlowCommand(
                    flowSnapshotId,
                    inputs,
                    false, // 同步执行
                    300,   // 5分钟超时
                    userId
            );

            // 执行流程，获取基本运行信息
            var flowResult = flowService.executeFlow(executeCommand);

            // 如果执行完成，获取详细信息
            if (flowResult.isFinished()) {
                var detailResult = flowService.getRunDetail(flowResult.id());

                return new FlowExecutionResult(
                        detailResult.id(),
                        detailResult.status(),
                        detailResult.resultJson(),
                        detailResult.logs()
                );
            } else {
                // 如果还在运行中，暂时返回空的result和logs
                return new FlowExecutionResult(
                        flowResult.id(),
                        flowResult.status(),
                        null,
                        null
                );
            }

        } catch (Exception e) {
            log.error("流程执行失败: flowSnapshotId={}", flowSnapshotId, e);
            return FlowExecutionResult.failed("流程执行异常: " + e.getMessage());
        }
    }

    /**
     * 构建智能上下文
     *
     * <p>整合各种来源的信息，为AI生成构建丰富的上下文环境。
     * 这个方法是前面几个方法的协调者，确保信息的完整性和一致性。</p>
     */
    public IntelligentContext buildIntelligentContext(
            Long tenantId,
            Long userId,
            Long threadId,
            String userQuery,
            Map<String, Object> baseContext) {

        IntelligentContext.Builder builder = IntelligentContext.builder()
                .withBaseContext(baseContext);

        // 1. 知识增强
        if (shouldEnhanceWithKnowledge(userQuery, baseContext)) {
            ContentEnhancementResult knowledgeResult = enhanceWithKnowledge(
                    tenantId, userQuery, "text-embedding-3-small", 5, 0.7f);
            builder.withKnowledgeContext(knowledgeResult);
        }

        // 2. 工具调用
        if (shouldExecuteTools(userQuery, baseContext)) {
            ToolExecutionResult toolResult = executeIntelligentTools(
                    tenantId, userId, threadId, userQuery, baseContext);
            builder.withToolResults(toolResult);
        }

        // 3. 流程执行
        Long flowSnapshotId = extractFlowSnapshotId(baseContext);
        if (flowSnapshotId != null) {
            Map<String, Object> flowInputs = prepareFlowInputs(userQuery, baseContext);
            FlowExecutionResult flowResult = executeBusinessFlow(flowSnapshotId, flowInputs, userId);
            builder.withFlowResult(flowResult);
        }

        return builder.build();
    }

    // =================== 私有辅助方法 ===================

    /**
     * 分析工具需求
     */
    private List<String> analyzeToolRequirements(String userQuery, Map<String, Object> context) {
        List<String> tools = new ArrayList<>();
        String lowerQuery = userQuery.toLowerCase();

        // 基于关键词的简单意图识别
        if (lowerQuery.contains("天气") || lowerQuery.contains("weather")) {
            tools.add("weather_tool");
        }
        if (lowerQuery.contains("搜索") || lowerQuery.contains("search")) {
            tools.add("search_tool");
        }
        if (lowerQuery.contains("计算") || lowerQuery.contains("calculate")) {
            tools.add("calculator_tool");
        }

        return tools;
    }

    /**
     * 异步执行工具
     */
    private CompletableFuture<ToolCallResult> executeToolAsync(
            String toolCode,
            Long tenantId,
            Long userId,
            Long threadId,
            String userQuery,
            Map<String, Object> context) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                var executeCommand = new ExecuteToolCommand(
                        tenantId, userId, threadId, null,
                        Map.of("query", userQuery, "context", context),
                        false, 30
                );

                var result = mcpService.executeTool(toolCode, executeCommand);
                return new ToolCallResult(toolCode, result, true, null);

            } catch (Exception e) {
                log.warn("工具调用失败: toolCode={}, error={}", toolCode, e.getMessage());
                return new ToolCallResult(toolCode, null, false, e.getMessage());
            }
        });
    }

    /**
     * 判断是否需要知识增强
     */
    private boolean shouldEnhanceWithKnowledge(String userQuery, Map<String, Object> context) {
        // 检查上下文中的配置
        Boolean enableRetrieval = (Boolean) context.get("enableKnowledgeRetrieval");
        if (enableRetrieval != null && !enableRetrieval) {
            return false;
        }

        // 基于查询内容判断
        String lowerQuery = userQuery.toLowerCase();
        return lowerQuery.contains("什么是") ||
                lowerQuery.contains("如何") ||
                lowerQuery.contains("为什么") ||
                lowerQuery.contains("介绍") ||
                userQuery.length() > 10; // 复杂问题通常需要知识支持
    }

    /**
     * 判断是否需要执行工具
     */
    private boolean shouldExecuteTools(String userQuery, Map<String, Object> context) {
        Boolean enableTools = (Boolean) context.get("enableToolCalling");
        if (enableTools != null && !enableTools) {
            return false;
        }

        return !analyzeToolRequirements(userQuery, context).isEmpty();
    }

    /**
     * 提取流程快照ID
     */
    private Long extractFlowSnapshotId(Map<String, Object> context) {
        return (Long) context.get("flowSnapshotId");
    }

    /**
     * 准备流程输入
     */
    private Map<String, Object> prepareFlowInputs(String userQuery, Map<String, Object> context) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("userInput", userQuery);
        inputs.put("context", context);
        inputs.put("timestamp", System.currentTimeMillis());
        return inputs;
    }

    // =================== 数据传输对象 ===================

    /**
     * 内容增强结果
     */
    public record ContentEnhancementResult(
            List<String> knowledgeContext,
            List<Long> sourceChunkIds,
            List<Float> relevanceScores,
            String embeddingModel
    ) {
        public static ContentEnhancementResult empty() {
            return new ContentEnhancementResult(List.of(), List.of(), List.of(), null);
        }

        public boolean hasContent() {
            return !knowledgeContext.isEmpty();
        }
    }

    /**
     * 工具执行结果
     */
    public record ToolExecutionResult(
            List<ToolCallResult> results
    ) {
        public static ToolExecutionResult empty() {
            return new ToolExecutionResult(List.of());
        }

        public boolean hasResults() {
            return !results.isEmpty();
        }

        public boolean hasSuccessfulResults() {
            return results.stream().anyMatch(ToolCallResult::success);
        }
    }

    /**
     * 单个工具调用结果
     */
    public record ToolCallResult(
            String toolCode,
            Object result,
            boolean success,
            String errorMessage
    ) {
    }

    /**
     * 流程执行结果
     */
    public record FlowExecutionResult(
            Long runId,
            String status,
            Object result,
            Object logs
    ) {
        public static FlowExecutionResult failed(String errorMessage) {
            return new FlowExecutionResult(null, "FAILED", null, errorMessage);
        }

        public boolean isSuccessful() {
            return "SUCCESS".equals(status) || "COMPLETED".equals(status);
        }
    }

    /**
     * 智能上下文
     */
    @Getter
    public static class IntelligentContext {
        private final Map<String, Object> baseContext;
        private final ContentEnhancementResult knowledgeContext;
        private final ToolExecutionResult toolResults;
        private final FlowExecutionResult flowResult;

        private IntelligentContext(Builder builder) {
            this.baseContext = builder.baseContext;
            this.knowledgeContext = builder.knowledgeContext;
            this.toolResults = builder.toolResults;
            this.flowResult = builder.flowResult;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * 转换为LLM所需的上下文格式
         */
        public Map<String, Object> toLLMContext() {
            Map<String, Object> context = new HashMap<>(baseContext);

            if (knowledgeContext != null && knowledgeContext.hasContent()) {
                context.put("knowledgeContext", knowledgeContext.knowledgeContext());
                context.put("knowledgeSources", knowledgeContext.sourceChunkIds());
            }

            if (toolResults != null && toolResults.hasResults()) {
                context.put("toolResults", toolResults.results());
            }

            if (flowResult != null && flowResult.isSuccessful()) {
                context.put("flowResult", flowResult.result());
            }

            return context;
        }

        public static class Builder {
            private Map<String, Object> baseContext = new HashMap<>();
            private ContentEnhancementResult knowledgeContext;
            private ToolExecutionResult toolResults;
            private FlowExecutionResult flowResult;

            public Builder withBaseContext(Map<String, Object> baseContext) {
                this.baseContext = new HashMap<>(baseContext);
                return this;
            }

            public Builder withKnowledgeContext(ContentEnhancementResult knowledgeContext) {
                this.knowledgeContext = knowledgeContext;
                return this;
            }

            public Builder withToolResults(ToolExecutionResult toolResults) {
                this.toolResults = toolResults;
                return this;
            }

            public Builder withFlowResult(FlowExecutionResult flowResult) {
                this.flowResult = flowResult;
                return this;
            }

            public IntelligentContext build() {
                return new IntelligentContext(this);
            }
        }
    }
}