package com.cloud.baseai.application.chat.service;

import com.cloud.baseai.application.chat.command.*;
import com.cloud.baseai.application.chat.dto.*;
import com.cloud.baseai.application.flow.command.ExecuteFlowCommand;
import com.cloud.baseai.application.flow.service.FlowOrchestrationAppService;
import com.cloud.baseai.application.kb.command.VectorSearchCommand;
import com.cloud.baseai.application.kb.service.KnowledgeBaseAppService;
import com.cloud.baseai.application.mcp.command.ExecuteToolCommand;
import com.cloud.baseai.application.mcp.service.McpApplicationService;
import com.cloud.baseai.application.metrics.service.MetricsService;
import com.cloud.baseai.domain.chat.model.ChatCitation;
import com.cloud.baseai.domain.chat.model.ChatMessage;
import com.cloud.baseai.domain.chat.model.ChatThread;
import com.cloud.baseai.domain.chat.model.MessageRole;
import com.cloud.baseai.domain.chat.repository.ChatCitationRepository;
import com.cloud.baseai.domain.chat.repository.ChatMessageRepository;
import com.cloud.baseai.domain.chat.repository.ChatThreadRepository;
import com.cloud.baseai.domain.chat.repository.ChatUsageRepository;
import com.cloud.baseai.domain.chat.service.ChatProcessingService;
import com.cloud.baseai.domain.chat.service.UsageCalculationService;
import com.cloud.baseai.domain.user.service.UserInfoService;
import com.cloud.baseai.infrastructure.config.ChatProperties;
import com.cloud.baseai.infrastructure.constants.ChatConstants;
import com.cloud.baseai.infrastructure.exception.BusinessException;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.ChatCompletionService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <h2>智能对话应用服务</h2>
 *
 * <p>这是对话系统的核心应用服务，承担着整个智能对话流程的编排工作。
 * 它就像一个智能的对话指挥官，能够协调知识检索、工具调用、流程编排等
 * 各种资源，为用户提供准确、有用、个性化的回答。</p>
 *
 * <p><b>核心架构思想：</b></p>
 * <p>现代的AI对话系统不再是单纯的文本生成器，而是一个复杂的认知系统。
 * 它需要具备记忆能力（维护对话上下文）、学习能力（从知识库检索信息）、
 * 执行能力（调用外部工具）和推理能力（遵循业务流程逻辑）。</p>
 *
 * <p><b>服务编排策略：</b></p>
 * <p>我们采用了"智能路由"的设计模式。根据用户输入的性质和上下文，
 * 系统会自动决定需要调用哪些服务：是否需要检索知识库、是否需要调用工具、
 * 是否需要执行特定的业务流程。这种动态编排确保了响应的准确性和效率。</p>
 */
@Service
public class ChatApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ChatApplicationService.class);

    // 领域仓储
    private final ChatThreadRepository threadRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatCitationRepository citationRepo;
    private final ChatUsageRepository usageRepo;

    // 领域服务
    private final ChatProcessingService chatService;
    private final UsageCalculationService usageService;

    // 外部服务
    private final ChatCompletionService llmService;

    // 集成服务
    private final KnowledgeBaseAppService kbService;
    private final McpApplicationService mcpService;
    private final FlowOrchestrationAppService flowService;

    // 配置
    private final ChatProperties config;

    // 异步执行器
    private final ExecutorService asyncExecutor;

    // 可选服务
    @Autowired(required = false)
    private UserInfoService userInfoService;

    @Autowired(required = false)
    private MetricsService metricsService;

    public ChatApplicationService(
            ChatThreadRepository threadRepo,
            ChatMessageRepository messageRepo,
            ChatCitationRepository citationRepo,
            ChatUsageRepository usageRepo,
            ChatProcessingService chatService,
            UsageCalculationService usageService,
            ChatCompletionService llmService,
            KnowledgeBaseAppService kbService,
            McpApplicationService mcpService,
            FlowOrchestrationAppService flowService,
            ChatProperties config) {

        this.threadRepo = threadRepo;
        this.messageRepo = messageRepo;
        this.citationRepo = citationRepo;
        this.usageRepo = usageRepo;
        this.chatService = chatService;
        this.usageService = usageService;
        this.llmService = llmService;
        this.kbService = kbService;
        this.mcpService = mcpService;
        this.flowService = flowService;
        this.config = config;

        this.asyncExecutor = Executors.newFixedThreadPool(
                config.getPerformance() != null ?
                        config.getPerformance().getAsyncPoolSize() : 10
        );
    }

    // =================== 对话线程管理 ===================

    /**
     * 创建对话线程
     *
     * <p>创建对话线程是开启智能对话的第一步。每个线程都会配置特定的AI模型、
     * 对话风格、知识域等参数，确保后续对话的一致性和针对性。</p>
     */
    @Transactional
    public ChatThreadDTO createThread(CreateChatThreadCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("创建对话线程: tenantId={}, userId={}, title={}",
                cmd.tenantId(), cmd.userId(), cmd.title());

        try {
            // 验证模型可用性
            if (!llmService.isModelAvailable(cmd.defaultModel())) {
                throw ChatException.modelUnavailable(cmd.defaultModel());
            }

            // 验证流程快照（如果指定）
            if (cmd.flowSnapshotId() != null) {
                validateFlowSnapshot(cmd.flowSnapshotId());
            }

            // 创建线程
            ChatThread thread = ChatThread.create(
                    cmd.tenantId(),
                    cmd.userId(),
                    cmd.title(),
                    cmd.defaultModel(),
                    cmd.temperature(),
                    cmd.flowSnapshotId(),
                    cmd.operatorId()
            );

            thread = threadRepo.save(thread);

            // 发送系统消息（如果有系统提示词）
            if (cmd.systemPrompt() != null && !cmd.systemPrompt().trim().isEmpty()) {
                sendSystemMessage(thread.id(), cmd.systemPrompt(), cmd.operatorId());
            }

            recordMetrics("thread.create", startTime, true);
            return toChatThreadDTO(thread);

        } catch (Exception e) {
            recordMetrics("thread.create", startTime, false);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_004)
                    .cause(e)
                    .context("operation", "createThread")
                    .context("title", cmd.title())
                    .build();
        }
    }

    /**
     * 获取对话线程列表
     */
    public PageResultDTO<ChatThreadDTO> listThreads(Long tenantId, Long userId,
                                                    int page, int size, String search) {
        log.debug("查询对话线程列表: tenantId={}, userId={}, page={}, size={}",
                tenantId, userId, page, size);

        try {
            List<ChatThread> threads;
            long total;

            if (search != null && !search.trim().isEmpty()) {
                threads = threadRepo.searchByTitle(tenantId, userId, search.trim(), page, size);
                total = threadRepo.countByTenantIdAndUserIdAndTitleContaining(tenantId, userId, search.trim());
            } else {
                threads = threadRepo.findByTenantIdAndUserId(tenantId, userId, page, size);
                total = threadRepo.countByTenantIdAndUserId(tenantId, userId);
            }

            List<ChatThreadDTO> threadDTOs = threads.stream()
                    .map(this::toChatThreadDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(threadDTOs, total, page, size);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_007)
                    .cause(e)
                    .context("operation", "listThreads")
                    .context("tenantId", tenantId)
                    .context("userId", userId)
                    .build();
        }
    }

    /**
     * 获取对话线程详情
     */
    public ChatThreadDetailDTO getThreadDetail(Long threadId) {
        log.debug("获取对话线程详情: threadId={}", threadId);

        try {
            ChatThread thread = threadRepo.findById(threadId)
                    .orElseThrow(() -> ChatException.threadNotFound(String.valueOf(threadId)));

            // 获取统计信息
            int messageCount = messageRepo.countByThreadId(threadId);
            int userMessageCount = messageRepo.countByThreadIdAndRole(threadId, MessageRole.USER);
            int assistantMessageCount = messageRepo.countByThreadIdAndRole(threadId, MessageRole.ASSISTANT);

            // 获取最近的消息
            List<ChatMessage> recentMessages = messageRepo.findByThreadIdOrderByCreatedAtDesc(threadId, 5);

            String creatorName = null;
            if (userInfoService != null) {
                creatorName = userInfoService.getUserName(thread.userId());
            }

            return new ChatThreadDetailDTO(
                    thread.id(),
                    thread.title(),
                    thread.defaultModel(),
                    thread.temperature(),
                    thread.flowSnapshotId(),
                    messageCount,
                    userMessageCount,
                    assistantMessageCount,
                    thread.userId(),
                    creatorName,
                    recentMessages.stream().map(this::toChatMessageDTO).collect(Collectors.toList()),
                    thread.createdAt(),
                    thread.updatedAt()
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_008)
                    .cause(e)
                    .context("operation", "getThreadDetail")
                    .context("threadId", threadId)
                    .build();
        }
    }

    /**
     * 更新对话线程
     */
    @Transactional
    public ChatThreadDTO updateThread(UpdateChatThreadCommand cmd) {
        log.info("更新对话线程: threadId={}", cmd.threadId());

        try {
            ChatThread thread = threadRepo.findById(cmd.threadId())
                    .orElseThrow(() -> ChatException.threadNotFound(String.valueOf(cmd.threadId())));

            // 验证新模型（如果更改）
            if (cmd.defaultModel() != null && !cmd.defaultModel().equals(thread.defaultModel())) {
                if (!llmService.isModelAvailable(cmd.defaultModel())) {
                    throw ChatException.modelUnavailable(cmd.defaultModel());
                }
            }

            thread = thread.update(
                    cmd.title(),
                    cmd.defaultModel(),
                    cmd.temperature(),
                    cmd.flowSnapshotId(),
                    cmd.operatorId()
            );

            thread = threadRepo.save(thread);
            return toChatThreadDTO(thread);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_005)
                    .cause(e)
                    .context("operation", "updateThread")
                    .context("threadId", cmd.threadId())
                    .build();
        }
    }

    /**
     * 删除对话线程
     */
    @Transactional
    public void deleteThread(Long threadId, Long operatorId) {
        log.info("删除对话线程: threadId={}", threadId);

        try {
            ChatThread thread = threadRepo.findById(threadId)
                    .orElseThrow(() -> ChatException.threadNotFound(String.valueOf(threadId)));

            // 软删除所有相关消息
            messageRepo.softDeleteByThreadId(threadId, operatorId);

            // 删除引用关系
            citationRepo.deleteByThreadId(threadId);

            // 软删除线程
            threadRepo.softDelete(threadId, operatorId);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_006)
                    .cause(e)
                    .context("operation", "deleteThread")
                    .context("threadId", threadId)
                    .build();
        }
    }

    // =================== 消息处理核心逻辑 ===================

    /**
     * 发送消息
     *
     * <p>这是整个对话系统最核心的方法。它需要协调多个服务，
     * 理解用户意图，检索相关知识，调用必要工具，生成智能回复。</p>
     *
     * <p>处理流程包括：</p>
     * <p>1. 意图理解和预处理</p>
     * <p>2. 知识库检索（如果需要）</p>
     * <p>3. 工具调用准备</p>
     * <p>4. 上下文构建</p>
     * <p>5. LLM生成</p>
     * <p>6. 后处理和存储</p>
     */
    @Transactional
    public ChatResponseDTO sendMessage(Long threadId, SendMessageCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("处理用户消息: threadId={}, contentLength={}", threadId, cmd.content().length());

        try {
            // 验证线程存在
            ChatThread thread = threadRepo.findById(threadId)
                    .orElseThrow(() -> ChatException.threadNotFound(String.valueOf(threadId)));

            // 验证消息内容
            validateMessageContent(cmd.content());

            // 检查速率限制
            checkRateLimit(thread.tenantId(), thread.userId());

            // 保存用户消息
            ChatMessage userMessage = saveUserMessage(thread, cmd);

            // 智能路由：决定处理策略
            ProcessingStrategy strategy = determineProcessingStrategy(thread, cmd);

            // 执行处理策略
            ChatResponseContext context = executeProcessingStrategy(thread, userMessage, strategy);

            // 生成AI回复
            ChatMessage assistantMessage = generateAssistantResponse(thread, context);

            // 保存使用统计
            saveUsageStatistics(thread, context.usage);

            recordMetrics("message.send", startTime, true);

            return new ChatResponseDTO(
                    toChatMessageDTO(userMessage),
                    toChatMessageDTO(assistantMessage),
                    context.citations.stream().map(this::toCitationDTO).collect(Collectors.toList()),
                    new ArrayList<>(context.toolCalls),
                    context.usage
            );

        } catch (Exception e) {
            recordMetrics("message.send", startTime, false);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_011)
                    .cause(e)
                    .context("operation", "sendMessage")
                    .context("threadId", threadId)
                    .build();
        }
    }

    /**
     * 流式发送消息
     *
     * <p>流式处理提供了更流畅的用户体验。我们将生成过程分解为多个步骤，
     * 实时向用户推送进度和部分结果。</p>
     */
    public void sendMessageStream(Long threadId, SendMessageCommand cmd, SseEmitter emitter) {
        log.info("开始流式消息处理: threadId={}", threadId);

        // 异步执行流式处理
        CompletableFuture.runAsync(() -> {
            try {
                processMessageStream(threadId, cmd, emitter);
            } catch (Exception e) {
                log.error("流式消息处理失败: threadId={}", threadId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("error", e.getMessage())));
                    emitter.completeWithError(e);
                } catch (Exception sendError) {
                    log.error("发送错误事件失败", sendError);
                }
            }
        }, asyncExecutor);
    }

    /**
     * 获取对话消息
     */
    public PageResultDTO<ChatMessageDTO> getMessages(Long threadId, int page, int size, Boolean includeToolCalls) {
        try {
            List<ChatMessage> messages = messageRepo.findByThreadId(threadId, page, size);
            long total = messageRepo.countByThreadId(threadId);

            List<ChatMessageDTO> messageDTOs = messages.stream()
                    .map(msg -> toChatMessageDTO(msg, includeToolCalls))
                    .collect(Collectors.toList());

            return PageResultDTO.of(messageDTOs, total, page, size);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_012)
                    .cause(e)
                    .context("operation", "getMessages")
                    .context("threadId", threadId)
                    .build();
        }
    }

    /**
     * 获取消息详情
     */
    public ChatMessageDetailDTO getMessageDetail(Long messageId) {
        try {
            ChatMessage message = messageRepo.findById(messageId)
                    .orElseThrow(() -> new ChatException(ErrorCode.BIZ_CHAT_009, messageId));

            // 获取引用信息
            List<ChatCitation> citations = citationRepo.findByMessageId(messageId);

            return new ChatMessageDetailDTO(
                    message.id(),
                    message.threadId(),
                    message.role().name(),
                    message.content(),
                    message.toolCall(),
                    message.tokenIn(),
                    message.tokenOut(),
                    message.latencyMs(),
                    citations.stream().map(this::toCitationDTO).collect(Collectors.toList()),
                    message.createdAt()
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_013)
                    .cause(e)
                    .context("operation", "getMessageDetail")
                    .context("messageId", messageId)
                    .build();
        }
    }

    // =================== 智能功能实现 ===================

    /**
     * 重新生成回复
     */
    @Transactional
    public ChatResponseDTO regenerateResponse(Long messageId, RegenerateResponseCommand cmd) {
        log.info("重新生成回复: messageId={}", messageId);

        try {
            ChatMessage userMessage = messageRepo.findById(messageId)
                    .orElseThrow(() -> new ChatException(ErrorCode.BIZ_CHAT_009, messageId));

            if (userMessage.role() != MessageRole.USER) {
                throw new ChatException(ErrorCode.BIZ_CHAT_043);
            }

            ChatThread thread = threadRepo.findById(userMessage.threadId())
                    .orElseThrow(() -> new ChatException(ErrorCode.BIZ_CHAT_002));

            // 删除原有的助手回复（如果存在）
            messageRepo.deleteAssistantResponseAfter(userMessage.id());

            // 使用新参数重新生成
            SendMessageCommand newCmd = new SendMessageCommand(
                    userMessage.content(),
                    ChatConstants.ContentTypes.TEXT,
                    cmd.enableKnowledgeRetrieval(),
                    cmd.enableToolCalling(),
                    cmd.temperature(),
                    cmd.maxTokens(),
                    false // 重新生成不使用流式
            );

            return sendMessage(thread.id(), newCmd);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_042)
                    .cause(e)
                    .context("operation", "regenerateResponse")
                    .context("messageId", messageId)
                    .build();
        }
    }

    /**
     * 提交反馈
     */
    @Transactional
    public void submitFeedback(Long messageId, SubmitFeedbackCommand cmd) {
        try {
            ChatMessage message = messageRepo.findById(messageId)
                    .orElseThrow(() -> new ChatException(ErrorCode.BIZ_CHAT_009, messageId));

            // 记录反馈信息（这里可以扩展为独立的反馈表）
            log.info("收到消息反馈: messageId={}, rating={}, comment={}",
                    messageId, cmd.rating(), cmd.comment());

            // 如果有度量服务，记录反馈指标
            if (metricsService != null) {
                metricsService.recordFeedback(messageId, cmd.rating(), cmd.comment());
            }

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_040)
                    .cause(e)
                    .context("operation", "submitFeedback")
                    .context("messageId", messageId)
                    .build();
        }
    }

    /**
     * 获取建议问题
     */
    public List<String> getSuggestions(Long threadId, Integer count) {
        try {
            ChatThread thread = threadRepo.findById(threadId)
                    .orElseThrow(() -> ChatException.threadNotFound(String.valueOf(threadId)));

            // 获取最近的对话历史
            List<ChatMessage> recentMessages = messageRepo.findByThreadIdOrderByCreatedAtDesc(threadId, 10);

            // 基于上下文生成建议问题
            return chatService.generateSuggestions(recentMessages, count);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_041)
                    .cause(e)
                    .context("operation", "getSuggestions")
                    .context("threadId", threadId)
                    .build();
        }
    }

    // =================== 统计和监控 ===================

    /**
     * 获取对话统计
     */
    public ChatStatisticsDTO getStatistics(Long tenantId, Long userId, String timeRange) {
        try {
            OffsetDateTime since = calculateSinceTime(timeRange);

            // 基础统计
            int totalThreads = threadRepo.countByTenantId(tenantId);
            int totalMessages = messageRepo.countByTenantIdSince(tenantId, since);
            int userMessages = messageRepo.countByTenantIdAndRoleSince(tenantId, MessageRole.USER, since);
            int assistantMessages = messageRepo.countByTenantIdAndRoleSince(tenantId, MessageRole.ASSISTANT, since);

            // 用户特定统计（如果指定）
            if (userId != null) {
                totalThreads = threadRepo.countByTenantIdAndUserId(tenantId, userId);
                totalMessages = messageRepo.countByUserIdSince(userId, since);
                userMessages = messageRepo.countByUserIdAndRoleSince(userId, MessageRole.USER, since);
                assistantMessages = messageRepo.countByUserIdAndRoleSince(userId, MessageRole.ASSISTANT, since);
            }

            // 计算平均响应时间
            Double avgResponseTime = messageRepo.getAverageResponseTime(tenantId, since);

            // 获取热门模型
            List<ModelUsageDTO> topModels = getTopUsedModels(tenantId, since, 5);

            return new ChatStatisticsDTO(
                    tenantId,
                    userId,
                    totalThreads,
                    totalMessages,
                    userMessages,
                    assistantMessages,
                    avgResponseTime != null ? avgResponseTime.longValue() : 0L,
                    topModels,
                    timeRange
            );

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_046)
                    .cause(e)
                    .context("operation", "getStatistics")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 获取使用量统计
     */
    public UsageStatisticsDTO getUsageStatistics(Long tenantId, String period, String groupBy) {
        try {
            return usageService.getUsageStatistics(tenantId, period, groupBy);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_047)
                    .cause(e)
                    .context("operation", "getUsageStatistics")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 健康检查
     */
    public ChatHealthStatus checkHealth() {
        Map<String, String> components = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 检查数据库连接
            try {
                threadRepo.count();
                components.put("database", "healthy");
            } catch (Exception e) {
                components.put("database", "unhealthy: " + e.getMessage());
            }

            // 检查LLM服务
            try {
                boolean available = llmService.isHealthy();
                components.put("llm_service", available ? "healthy" : "unhealthy");
            } catch (Exception e) {
                components.put("llm_service", "unhealthy: " + e.getMessage());
            }

            // 检查知识库服务
            try {
                var kbHealth = kbService.checkHealth();
                components.put("knowledge_base", kbHealth.status());
            } catch (Exception e) {
                components.put("knowledge_base", "unhealthy: " + e.getMessage());
            }

            // 检查工具服务
            try {
                var mcpHealth = mcpService.checkHealth();
                components.put("mcp_service", mcpHealth.status());
            } catch (Exception e) {
                components.put("mcp_service", "unhealthy: " + e.getMessage());
            }

            boolean allHealthy = components.values().stream()
                    .allMatch(status -> status.equals("healthy"));

            long responseTime = System.currentTimeMillis() - startTime;

            return new ChatHealthStatus(
                    allHealthy ? "healthy" : "unhealthy",
                    components,
                    responseTime
            );

        } catch (Exception e) {
            return new ChatHealthStatus(
                    "unhealthy",
                    Map.of("error", e.getMessage()),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 流式处理消息的核心逻辑
     */
    private void processMessageStream(Long threadId, SendMessageCommand cmd, SseEmitter emitter) {
        try {
            // 发送开始事件
            emitter.send(SseEmitter.event()
                    .name("start")
                    .data(Map.of("status", "processing")));

            // 验证和预处理
            ChatThread thread = threadRepo.findById(threadId)
                    .orElseThrow(() -> ChatException.threadNotFound(String.valueOf(threadId)));

            // 发送处理步骤事件
            emitter.send(SseEmitter.event()
                    .name("step")
                    .data(Map.of("step", "knowledge_retrieval", "status", "processing")));

            // 执行知识检索（如果需要）
            List<ChatCitation> citations = new ArrayList<>();
            if (cmd.enableKnowledgeRetrieval()) {
                citations = performKnowledgeRetrieval(thread, cmd.content());
            }

            // 发送工具调用步骤
            emitter.send(SseEmitter.event()
                    .name("step")
                    .data(Map.of("step", "tool_calling", "status", "processing")));

            // 执行工具调用（如果需要）
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            if (cmd.enableToolCalling()) {
                toolCalls = performToolCalling(thread, cmd.content());
            }

            // 发送生成步骤
            emitter.send(SseEmitter.event()
                    .name("step")
                    .data(Map.of("step", "generating", "status", "processing")));

            // 流式生成回复
            StringBuilder responseBuilder = new StringBuilder();
            llmService.generateStreamResponse(
                    buildStreamContext(thread, cmd, citations, toolCalls),
                    chunk -> {
                        try {
                            responseBuilder.append(chunk);
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(Map.of("content", chunk)));
                        } catch (Exception e) {
                            log.error("发送流式数据失败", e);
                        }
                    }
            );

            // 发送完成事件
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(Map.of(
                            "content", responseBuilder.toString(),
                            "citations", citations,
                            "toolCalls", toolCalls
                    )));

            emitter.complete();

        } catch (Exception e) {
            log.error("流式处理失败", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("error", e.getMessage())));
                emitter.completeWithError(e);
            } catch (Exception sendError) {
                log.error("发送错误事件失败", sendError);
            }
        }
    }

    /**
     * 验证消息内容
     */
    private void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ChatException(ErrorCode.BIZ_CHAT_016);
        }

        if (content.length() > config.getMaxMessageLength()) {
            throw new ChatException(ErrorCode.BIZ_CHAT_018, config.getMaxMessageLength());
        }
    }

    /**
     * 检查速率限制
     */
    private void checkRateLimit(Long tenantId, Long userId) {
        // 实现速率限制检查逻辑
        if (config.isRateLimitEnabled()) {
            int recentMessages = messageRepo.countRecentMessages(userId, config.getRateLimitWindow());
            if (recentMessages >= config.getRateLimitMax()) {
                throw new ChatException(ErrorCode.BIZ_CHAT_032);
            }
        }
    }

    /**
     * 保存用户消息
     */
    private ChatMessage saveUserMessage(ChatThread thread, SendMessageCommand cmd) {
        ChatMessage userMessage = ChatMessage.create(
                thread.id(),
                MessageRole.USER,
                cmd.content(),
                null, // 用户消息没有工具调用
                null, // 创建人ID（从线程获取）
                thread.userId()
        );

        return messageRepo.save(userMessage);
    }

    /**
     * 发送系统消息
     */
    private void sendSystemMessage(Long threadId, String systemPrompt, Long operatorId) {
        ChatMessage systemMessage = ChatMessage.create(
                threadId,
                MessageRole.SYSTEM,
                systemPrompt,
                null,
                operatorId,
                operatorId
        );

        messageRepo.save(systemMessage);
    }

    /**
     * 验证流程快照
     */
    private void validateFlowSnapshot(Long flowSnapshotId) {
        try {
            flowService.getSnapshot(flowSnapshotId);
        } catch (Exception e) {
            throw new ChatException(ErrorCode.BIZ_CHAT_039, flowSnapshotId);
        }
    }

    /**
     * 决定处理策略
     */
    private ProcessingStrategy determineProcessingStrategy(ChatThread thread, SendMessageCommand cmd) {
        ProcessingStrategy strategy = new ProcessingStrategy();

        // 基于用户配置
        strategy.useKnowledgeRetrieval = cmd.enableKnowledgeRetrieval();
        strategy.useToolCalling = cmd.enableToolCalling();

        // 基于线程配置
        strategy.useFlowOrchestration = thread.flowSnapshotId() != null;

        // 智能分析内容特征
        String content = cmd.content().toLowerCase();
        if (content.contains("搜索") || content.contains("查找") || content.contains("什么是")) {
            strategy.useKnowledgeRetrieval = true;
        }

        if (content.contains("执行") || content.contains("调用") || content.contains("工具")) {
            strategy.useToolCalling = true;
        }

        return strategy;
    }

    /**
     * 执行处理策略
     */
    private ChatResponseContext executeProcessingStrategy(ChatThread thread, ChatMessage userMessage,
                                                          ProcessingStrategy strategy) {
        ChatResponseContext context = new ChatResponseContext();

        // 知识检索
        if (strategy.useKnowledgeRetrieval) {
            context.citations = performKnowledgeRetrieval(thread, userMessage.content());
        }

        // 工具调用
        if (strategy.useToolCalling) {
            context.toolCalls = performToolCalling(thread, userMessage.content());
        }

        // 流程编排（如果配置）
        if (strategy.useFlowOrchestration) {
            context.flowResult = performFlowOrchestration(thread, userMessage.content());
        }

        return context;
    }

    /**
     * 执行知识检索
     */
    private List<ChatCitation> performKnowledgeRetrieval(ChatThread thread, String content) {
        try {
            var searchCmd = new VectorSearchCommand(
                    thread.tenantId(),
                    content,
                    config.getDefaultEmbeddingModel(),
                    config.getKnowledgeRetrievalTopK(),
                    config.getKnowledgeRetrievalThreshold(),
                    false
            );

            var searchResults = kbService.vectorSearch(searchCmd);

            return searchResults.stream()
                    .map(result -> ChatCitation.create(
                            null, // messageId 稍后设置
                            result.chunkId(),
                            result.score(),
                            config.getDefaultEmbeddingModel()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("知识检索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 执行工具调用
     */
    private List<Map<String, Object>> performToolCalling(ChatThread thread, String content) {
        try {
            // 分析内容中的工具调用意图
            List<String> toolIntents = chatService.analyzeToolIntents(content);

            List<Map<String, Object>> results = new ArrayList<>();
            for (String toolCode : toolIntents) {
                try {
                    var executeCmd = new ExecuteToolCommand(
                            thread.tenantId(),
                            thread.userId(),
                            thread.id(),
                            null,
                            Map.of("query", content), // 简化的参数
                            false,
                            30
                    );

                    var result = mcpService.executeTool(toolCode, executeCmd);
                    results.add(Map.of(
                            "toolCode", toolCode,
                            "result", result
                    ));

                } catch (Exception e) {
                    log.warn("工具调用失败: toolCode={}, error={}", toolCode, e.getMessage());
                }
            }

            return results;

        } catch (Exception e) {
            log.warn("工具调用分析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 执行流程编排
     */
    private Map<String, Object> performFlowOrchestration(ChatThread thread, String content) {
        try {
            var executeCmd = new ExecuteFlowCommand(
                    thread.flowSnapshotId(),
                    Map.of("userInput", content),
                    false, // 同步执行
                    5, // 5分钟超时
                    thread.userId()
            );

            var result = flowService.executeFlow(executeCmd);
            return Map.of("flowResult", result);

        } catch (Exception e) {
            log.warn("流程编排执行失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 生成助手回复
     */
    private ChatMessage generateAssistantResponse(ChatThread thread, ChatResponseContext context) {
        try {
            // 构建上下文
            Map<String, Object> llmContext = buildLLMContext(thread, context);

            // 调用LLM生成回复
            var completionResult = llmService.generateCompletion(llmContext);

            // 创建助手消息
            ChatMessage assistantMessage = ChatMessage.create(
                    thread.id(),
                    MessageRole.ASSISTANT,
                    completionResult.content(),
                    completionResult.toolCall(),
                    thread.userId(),
                    thread.userId()
            );

            // 设置使用统计
            assistantMessage = assistantMessage.updateUsage(
                    completionResult.tokenIn(),
                    completionResult.tokenOut(),
                    completionResult.latencyMs()
            );

            assistantMessage = messageRepo.save(assistantMessage);

            // 保存引用关系
            if (!context.citations.isEmpty()) {
                for (ChatCitation citation : context.citations) {
                    citation = citation.setMessageId(assistantMessage.id());
                    citationRepo.save(citation);
                }
            }

            // 记录使用统计
            context.usage = new UsageStatisticsDTO.UsageDetail(
                    completionResult.tokenIn(),
                    completionResult.tokenOut(),
                    completionResult.cost()
            );

            return assistantMessage;

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_CHAT_044)
                    .cause(e)
                    .context("operation", "generateAssistantResponse")
                    .context("threadId", thread.id())
                    .build();
        }
    }

    /**
     * 构建LLM上下文
     */
    private Map<String, Object> buildLLMContext(ChatThread thread, ChatResponseContext context) {
        Map<String, Object> llmContext = new HashMap<>();

        // 基本配置
        llmContext.put("model", thread.defaultModel());
        llmContext.put("temperature", thread.temperature());

        // 对话历史
        List<ChatMessage> history = messageRepo.findByThreadIdOrderByCreatedAtDesc(thread.id(), 20);
        llmContext.put("messages", history.stream()
                .map(this::toMessageMap)
                .collect(Collectors.toList()));

        // 知识上下文
        if (!context.citations.isEmpty()) {
            List<String> knowledgeContext = context.citations.stream()
                    .map(citation -> getChunkContent(citation.chunkId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            llmContext.put("knowledgeContext", knowledgeContext);
        }

        // 工具调用结果
        if (!context.toolCalls.isEmpty()) {
            llmContext.put("toolResults", context.toolCalls);
        }

        // 流程结果
        if (context.flowResult != null) {
            llmContext.put("flowContext", context.flowResult);
        }

        return llmContext;
    }

    /**
     * 构建流式上下文
     */
    private Map<String, Object> buildStreamContext(ChatThread thread, SendMessageCommand cmd,
                                                   List<ChatCitation> citations,
                                                   List<Map<String, Object>> toolCalls) {
        Map<String, Object> context = new HashMap<>();

        context.put("model", thread.defaultModel());
        context.put("temperature", cmd.temperature() != null ? cmd.temperature() : thread.temperature());
        context.put("maxTokens", cmd.maxTokens());

        // 添加当前消息
        context.put("currentMessage", cmd.content());

        // 添加知识上下文
        if (!citations.isEmpty()) {
            List<String> knowledgeContext = citations.stream()
                    .map(citation -> getChunkContent(citation.chunkId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            context.put("knowledgeContext", knowledgeContext);
        }

        // 添加工具结果
        if (!toolCalls.isEmpty()) {
            context.put("toolResults", toolCalls);
        }

        return context;
    }

    /**
     * 获取知识块内容
     */
    private String getChunkContent(Long chunkId) {
        try {
            var chunkDetail = kbService.getChunkDetail(chunkId);
            return chunkDetail.text();
        } catch (Exception e) {
            log.warn("获取知识块内容失败: chunkId={}", chunkId);
            return null;
        }
    }

    /**
     * 保存使用统计
     */
    private void saveUsageStatistics(ChatThread thread, UsageStatisticsDTO.UsageDetail usage) {
        if (usage != null) {
            usageService.recordUsage(
                    thread.tenantId(),
                    thread.defaultModel(),
                    usage.promptTokens(),
                    usage.completionTokens(),
                    usage.cost()
            );
        }
    }

    /**
     * 计算时间范围
     */
    private OffsetDateTime calculateSinceTime(String timeRange) {
        if (timeRange == null) {
            return OffsetDateTime.now().minusDays(7);
        }

        return switch (timeRange.toLowerCase()) {
            case "1h", "hour" -> OffsetDateTime.now().minusHours(1);
            case "1d", "day" -> OffsetDateTime.now().minusDays(1);
            case "7d", "week" -> OffsetDateTime.now().minusDays(7);
            case "30d", "month" -> OffsetDateTime.now().minusDays(30);
            default -> OffsetDateTime.now().minusDays(7);
        };
    }

    /**
     * 获取热门模型
     */
    private List<ModelUsageDTO> getTopUsedModels(Long tenantId, OffsetDateTime since, int limit) {
        // 实现获取热门模型的逻辑
        return usageRepo.getTopUsedModels(tenantId, since, limit);
    }

    /**
     * 记录指标
     */
    private void recordMetrics(String operation, long startTime, boolean success) {
        if (metricsService != null) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordOperation(operation, duration, success);
        }
    }

    // =================== DTO转换方法 ===================

    private ChatThreadDTO toChatThreadDTO(ChatThread thread) {
        String creatorName = null;
        if (userInfoService != null) {
            creatorName = userInfoService.getUserName(thread.userId());
        }

        return new ChatThreadDTO(
                thread.id(),
                thread.title(),
                thread.defaultModel(),
                thread.temperature(),
                thread.flowSnapshotId(),
                thread.userId(),
                creatorName,
                thread.createdAt(),
                thread.updatedAt()
        );
    }

    private ChatMessageDTO toChatMessageDTO(ChatMessage message) {
        return toChatMessageDTO(message, false);
    }

    private ChatMessageDTO toChatMessageDTO(ChatMessage message, Boolean includeToolCalls) {
        return new ChatMessageDTO(
                message.id(),
                message.threadId(),
                message.role().name(),
                message.content(),
                includeToolCalls ? message.toolCall() : null,
                message.tokenIn(),
                message.tokenOut(),
                message.latencyMs(),
                message.createdAt()
        );
    }

    private ChatCitationDTO toCitationDTO(ChatCitation citation) {
        return new ChatCitationDTO(
                citation.messageId(),
                citation.chunkId(),
                citation.score(),
                citation.modelCode()
        );
    }

    private Map<String, Object> toMessageMap(ChatMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", message.role().name().toLowerCase());
        map.put("content", message.content());
        if (message.toolCall() != null) {
            map.put("tool_calls", message.toolCall());
        }
        return map;
    }

    // =================== 内部数据结构 ===================

    /**
     * 处理策略
     */
    private static class ProcessingStrategy {
        boolean useKnowledgeRetrieval = false;
        boolean useToolCalling = false;
        boolean useFlowOrchestration = false;
    }

    /**
     * 响应上下文
     */
    private static class ChatResponseContext {
        List<ChatCitation> citations = new ArrayList<>();
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        Map<String, Object> flowResult = new HashMap<>();
        UsageStatisticsDTO.UsageDetail usage;
    }
}