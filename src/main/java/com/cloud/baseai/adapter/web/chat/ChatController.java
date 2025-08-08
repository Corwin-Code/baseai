package com.cloud.baseai.adapter.web.chat;

import com.cloud.baseai.application.chat.command.*;
import com.cloud.baseai.application.chat.dto.*;
import com.cloud.baseai.application.chat.service.ChatApplicationService;
import com.cloud.baseai.infrastructure.exception.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * <h1>智能对话REST控制器</h1>
 *
 * <p>这个控制器是整个AI对话系统的门户，它像一个经验丰富的客服主管，
 * 不仅要理解用户的问题，还要协调知识库、工具调用、流程编排等各种资源，
 * 为用户提供最准确、最有帮助的回答。</p>
 *
 * <p><b>智能对话的技术架构：</b></p>
 * <p>现代的AI对话系统远不止是简单的问答。它集成了检索增强生成(RAG)、
 * 函数调用(Function Calling)、多轮对话记忆、个性化推荐等多种先进技术。
 * 就像一个超级助手，既有渊博的知识，又有强大的执行能力。</p>
 *
 * <p><b>核心功能模块：</b></p>
 * <ul>
 * <li><b>多轮对话管理：</b>维护对话上下文，支持复杂的多轮交互</li>
 * <li><b>知识增强回答：</b>实时检索相关知识，提供准确可靠的信息</li>
 * <li><b>工具集成调用：</b>扩展AI能力边界，支持各种实用工具</li>
 * <li><b>流程编排支持：</b>处理复杂业务场景的结构化对话流程</li>
 * <li><b>实时流式回复：</b>提供流畅的用户体验，逐步展示生成过程</li>
 * </ul>
 *
 * <p><b>设计理念：</b></p>
 * <p>我们相信优秀的AI助手应该像一个有经验的人类专家：有知识、有工具、
 * 有逻辑、有记忆。这个控制器实现了这样的愿景，让AI真正成为用户的智能伙伴。</p>
 */
@RestController
@RequestMapping("/api/v1/chat")
@Validated
@Tag(name = "智能对话管理", description = "Chat Management APIs - 提供完整的AI驱动的智能对话解决方案")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatApplicationService appService;

    public ChatController(ChatApplicationService appService) {
        this.appService = appService;
    }

    // =================== 对话线程管理 ===================

    /**
     * 创建对话线程
     *
     * <p>创建对话线程就像开启一段新的谈话。每个线程都有自己的上下文记忆，
     * 能够记住之前的对话内容，为后续的交互提供连贯性支持。</p>
     *
     * <p>用户可以为不同的主题或任务创建不同的对话线程，比如技术咨询、
     * 产品设计、数据分析等，每个线程都会保持其专属的对话风格和知识背景。</p>
     */
    @PostMapping(value = "/threads", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建对话线程",
            description = "创建新的对话线程，支持自定义模型、温度参数和流程编排。每个线程维护独立的对话上下文。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "对话线程创建成功",
                    content = @Content(schema = @Schema(implementation = ChatThreadDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'WRITE')")
    public ResponseEntity<ApiResult<ChatThreadDTO>> createThread(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "对话线程创建信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "创建技术咨询对话线程",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "userId": 123,
                                              "title": "技术架构咨询",
                                              "defaultModel": "gpt-4o",
                                              "temperature": 0.7,
                                              "flowSnapshotId": 456,
                                              "systemPrompt": "你是一个资深的技术架构师，专门帮助用户解决技术架构相关问题。"
                                            }
                                            """
                            )
                    )
            ) CreateChatThreadCommand cmd) {

        log.info("创建对话线程: tenantId={}, userId={}, title={}",
                cmd.tenantId(), cmd.userId(), cmd.title());

        ChatThreadDTO result = appService.createThread(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "对话线程创建成功"));
    }

    /**
     * 获取用户的对话线程列表
     *
     * <p>就像查看通讯录一样，用户可以浏览自己的所有对话线程，
     * 快速回到之前的对话场景，继续未完成的讨论。</p>
     */
    @GetMapping("/threads")
    @Operation(summary = "获取对话线程列表", description = "分页获取用户的所有对话线程，支持按标题搜索过滤。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<ChatThreadDTO>>> listThreads(
            @Parameter(description = "租户ID", required = true)
            @RequestParam Long tenantId,

            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId,

            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "页码不能小于0")
            Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100")
            Integer size,

            @Parameter(description = "标题搜索关键词", example = "技术")
            @RequestParam(required = false) String search) {

        log.debug("查询对话线程列表: tenantId={}, userId={}, page={}, size={}",
                tenantId, userId, page, size);

        PageResultDTO<ChatThreadDTO> result = appService.listThreads(tenantId, userId, page, size, search);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("查询完成，共找到 %d 个对话线程", result.totalElements())));
    }

    /**
     * 获取对话线程详情
     */
    @GetMapping("/threads/{threadId}")
    @Operation(summary = "获取对话线程详情", description = "获取指定对话线程的完整信息，包括统计数据。")
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'READ')")
    public ResponseEntity<ApiResult<ChatThreadDetailDTO>> getThreadDetail(
            @PathVariable Long threadId) {

        log.debug("获取对话线程详情: threadId={}", threadId);

        ChatThreadDetailDTO result = appService.getThreadDetail(threadId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新对话线程
     */
    @PutMapping("/threads/{threadId}")
    @Operation(summary = "更新对话线程", description = "更新对话线程的标题、模型配置等信息。")
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'WRITE')")
    public ResponseEntity<ApiResult<ChatThreadDTO>> updateThread(
            @PathVariable Long threadId,
            @Valid @RequestBody UpdateChatThreadCommand cmd) {

        ChatThreadDTO result = appService.updateThread(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "对话线程更新成功"));
    }

    /**
     * 删除对话线程
     */
    @DeleteMapping("/threads/{threadId}")
    @Operation(summary = "删除对话线程", description = "删除指定对话线程及其所有消息记录。")
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'DELETE')")
    public ResponseEntity<ApiResult<Void>> deleteThread(
            @PathVariable Long threadId,
            @RequestParam Long operatorId) {

        log.info("删除对话线程: threadId={}, operatorId={}", threadId, operatorId);

        appService.deleteThread(threadId, operatorId);

        return ResponseEntity.ok(ApiResult.success(null, "对话线程已删除"));
    }

    // =================== 消息发送和接收 ===================

    /**
     * 发送消息
     *
     * <p>这是整个对话系统的核心功能。当用户发送一条消息时，系统会：</p>
     * <p>1. 理解用户的意图和需求</p>
     * <p>2. 从知识库中检索相关信息</p>
     * <p>3. 调用必要的工具和服务</p>
     * <p>4. 结合上下文生成智能回复</p>
     * <p>5. 记录对话历史和引用来源</p>
     *
     * <p>整个过程就像与一个博学且善于使用工具的专家对话，
     * 既有深度的知识储备，又有强大的执行能力。</p>
     */
    @PostMapping("/threads/{threadId}/messages")
    @Operation(
            summary = "发送消息",
            description = "向指定对话线程发送消息，支持文本、图片等多种类型。系统会自动进行知识检索、工具调用等处理。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "消息发送成功"),
            @ApiResponse(responseCode = "400", description = "消息内容无效"),
            @ApiResponse(responseCode = "429", description = "发送频率过高")
    })
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'WRITE')")
    public ResponseEntity<ApiResult<ChatResponseDTO>> sendMessage(
            @PathVariable Long threadId,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "消息发送信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "发送技术问题消息",
                                    value = """
                                            {
                                              "content": "如何在Spring Boot中实现分布式锁？",
                                              "messageType": "TEXT",
                                              "enableKnowledgeRetrieval": true,
                                              "enableToolCalling": true,
                                              "temperature": 0.7,
                                              "maxTokens": 2000,
                                              "streamMode": false
                                            }
                                            """
                            )
                    )
            ) SendMessageCommand cmd) {

        long startTime = System.currentTimeMillis();
        log.info("接收用户消息: threadId={}, contentLength={}", threadId, cmd.content().length());

        try {
            ChatResponseDTO result = appService.sendMessage(threadId, cmd);

            long duration = System.currentTimeMillis() - startTime;
            log.info("消息处理完成: threadId={}, duration={}ms", threadId, duration);

            return ResponseEntity.ok(ApiResult.success(result, "消息发送成功"));

        } catch (Exception e) {
            log.error("消息处理失败: threadId={}", threadId, e);
            throw e;
        }
    }

    /**
     * 流式发送消息
     *
     * <p>流式响应提供了更流畅的用户体验。就像人类思考时会逐步表达想法一样，
     * AI也会逐步生成回复，让用户能够实时看到思考过程，而不需要等待完整回答。</p>
     *
     * <p>这种方式特别适合长文本生成、代码编写、复杂问题分析等场景，
     * 能够显著提升用户的交互体验和参与感。</p>
     */
    @PostMapping("/threads/{threadId}/messages/stream")
    @Operation(
            summary = "流式发送消息",
            description = "向指定对话线程发送消息并以流式方式返回回复，提供实时的回复体验。"
    )
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'WRITE')")
    public SseEmitter sendMessageStream(
            @PathVariable Long threadId,
            @Valid @RequestBody SendMessageCommand cmd) {

        log.info("开始流式消息处理: threadId={}", threadId);

        // 创建SSE发射器，超时时间为5分钟
        SseEmitter emitter = new SseEmitter(300_000L);

        try {
            appService.sendMessageStream(threadId, cmd, emitter);
            return emitter;

        } catch (Exception e) {
            log.error("流式消息处理失败: threadId={}", threadId, e);
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * 获取对话历史
     *
     * <p>对话历史是AI系统记忆的体现。通过回顾历史对话，
     * 用户可以追溯思路发展过程，AI也能更好地理解上下文语境。</p>
     */
    @GetMapping("/threads/{threadId}/messages")
    @Operation(summary = "获取对话历史", description = "分页获取指定对话线程的消息历史记录。")
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<ChatMessageDTO>>> getMessages(
            @PathVariable Long threadId,

            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "是否包含工具调用详情", example = "false")
            @RequestParam(defaultValue = "false") Boolean includeToolCalls) {

        PageResultDTO<ChatMessageDTO> result = appService.getMessages(threadId, page, size, includeToolCalls);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取单条消息详情
     */
    @GetMapping("/messages/{messageId}")
    @Operation(summary = "获取消息详情", description = "获取指定消息的完整信息，包括引用来源和工具调用详情。")
    @PreAuthorize("hasPermission(#messageId, 'CHAT_MESSAGE', 'READ')")
    public ResponseEntity<ApiResult<ChatMessageDetailDTO>> getMessageDetail(
            @PathVariable Long messageId) {

        ChatMessageDetailDTO result = appService.getMessageDetail(messageId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    // =================== 智能功能接口 ===================

    /**
     * 重新生成回复
     *
     * <p>有时AI的第一次回答可能不够理想，用户可以要求重新生成。
     * 系统会使用稍微不同的参数和策略，尝试提供更好的回答。</p>
     */
    @PostMapping("/messages/{messageId}/regenerate")
    @Operation(
            summary = "重新生成回复",
            description = "对指定的用户消息重新生成AI回复，可以调整生成参数以获得不同的回答。"
    )
    @PreAuthorize("hasPermission(#messageId, 'CHAT_MESSAGE', 'WRITE')")
    public ResponseEntity<ApiResult<ChatResponseDTO>> regenerateResponse(
            @PathVariable Long messageId,
            @Valid @RequestBody RegenerateResponseCommand cmd) {

        log.info("重新生成回复: messageId={}", messageId);

        ChatResponseDTO result = appService.regenerateResponse(messageId, cmd);

        return ResponseEntity.ok(ApiResult.success(result, "回复重新生成成功"));
    }

    /**
     * 消息评价
     *
     * <p>用户的反馈是AI系统持续改进的重要数据源。通过评价机制，
     * 我们可以了解哪些回答是有帮助的，哪些需要改进。</p>
     */
    @PostMapping("/messages/{messageId}/feedback")
    @Operation(summary = "消息评价", description = "对AI生成的消息进行评价，帮助系统持续改进。")
    @PreAuthorize("hasPermission(#messageId, 'CHAT_MESSAGE', 'READ')")
    public ResponseEntity<ApiResult<Void>> submitFeedback(
            @PathVariable Long messageId,
            @Valid @RequestBody SubmitFeedbackCommand cmd) {

        log.info("提交消息评价: messageId={}, rating={}", messageId, cmd.rating());

        appService.submitFeedback(messageId, cmd);

        return ResponseEntity.ok(ApiResult.success(null, "评价提交成功"));
    }

    /**
     * 建议问题
     *
     * <p>当用户不知道该问什么时，系统可以基于当前上下文和知识库内容，
     * 智能推荐一些相关的问题，引导用户进行更深入的探索。</p>
     */
    @GetMapping("/threads/{threadId}/suggestions")
    @Operation(summary = "获取建议问题", description = "基于当前对话上下文，智能推荐相关的问题供用户参考。")
    @PreAuthorize("hasPermission(#threadId, 'CHAT_THREAD', 'READ')")
    public ResponseEntity<ApiResult<List<String>>> getSuggestions(
            @PathVariable Long threadId,
            @RequestParam(defaultValue = "5") Integer count) {

        List<String> suggestions = appService.getSuggestions(threadId, count);

        return ResponseEntity.ok(ApiResult.success(suggestions,
                String.format("生成了 %d 个建议问题", suggestions.size())));
    }

    // =================== 统计和监控接口 ===================

    /**
     * 获取对话统计
     *
     * <p>统计信息帮助用户和管理员了解系统的使用情况，包括对话频率、
     * 响应质量、知识库命中率等关键指标。</p>
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取对话统计", description = "获取租户或用户级别的对话统计信息。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<ChatStatisticsDTO>> getStatistics(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String timeRange) {

        ChatStatisticsDTO result = appService.getStatistics(tenantId, userId, timeRange);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取使用量统计
     */
    @GetMapping("/usage")
    @Operation(summary = "获取使用量统计", description = "获取详细的Token使用量和费用统计。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<UsageStatisticsDTO>> getUsageStatistics(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String groupBy) {

        UsageStatisticsDTO result = appService.getUsageStatistics(tenantId, period, groupBy);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "对话系统健康检查", description = "检查对话服务和相关组件的健康状态。")
    public ResponseEntity<ApiResult<ChatHealthStatus>> healthCheck() {

        ChatHealthStatus status = appService.checkHealth();

        HttpStatus httpStatus = "healthy".equals(status.status()) ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus)
                .body(ApiResult.success(status, "健康检查完成"));
    }
}