package com.clinflash.baseai.adapter.web.chat;

import com.clinflash.baseai.application.chat.command.CreateChatThreadCommand;
import com.clinflash.baseai.application.chat.command.SendMessageCommand;
import com.clinflash.baseai.infrastructure.config.ChatTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h2>Chat模块集成测试</h2>
 *
 * <p>集成测试验证各个组件协同工作的能力。这些测试模拟真实的用户场景，
 * 确保整个对话流程的正确性和稳定性。</p>
 *
 * <p><b>测试策略：</b></p>
 * <p>我们采用分层测试策略：</p>
 * <p>1. <strong>单元测试：</strong>测试单个类的功能</p>
 * <p>2. <strong>集成测试：</strong>测试组件间的协作</p>
 * <p>3. <strong>端到端测试：</strong>测试完整的用户场景</p>
 * <p>4. <strong>性能测试：</strong>验证系统的性能表现</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ChatTestConfiguration.class)
@Transactional
public class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_TENANT_ID = 1L;
    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_OPERATOR_ID = 100L;

    @Nested
    @DisplayName("对话线程管理测试")
    class ThreadManagementTests {

        @Test
        @DisplayName("应该能够成功创建对话线程")
        void shouldCreateChatThreadSuccessfully() throws Exception {
            // Given
            CreateChatThreadCommand command = new CreateChatThreadCommand(
                    TEST_TENANT_ID,
                    TEST_USER_ID,
                    "测试对话",
                    "mock-gpt-4o",
                    0.7f,
                    null,
                    "你好，我是AI助手",
                    TEST_OPERATOR_ID
            );

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/chat/threads")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(command)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("测试对话"))
                    .andExpect(jsonPath("$.data.defaultModel").value("mock-gpt-4o"))
                    .andReturn();

            // 验证返回的数据结构
            String responseJson = result.getResponse().getContentAsString();
            ChatController.ApiResult<?> apiResult = objectMapper.readValue(responseJson, ChatController.ApiResult.class);
            assertTrue(apiResult.isSuccess());
            assertNotNull(apiResult.getData());
        }

        @Test
        @DisplayName("应该能够获取对话线程列表")
        void shouldGetThreadListSuccessfully() throws Exception {
            // Given - 先创建一个线程
            CreateChatThreadCommand command = new CreateChatThreadCommand(
                    TEST_TENANT_ID, TEST_USER_ID, "测试对话", "mock-gpt-4o", 0.7f, null, null, TEST_OPERATOR_ID
            );

            mockMvc.perform(post("/api/v1/chat/threads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(command)));

            // When & Then
            mockMvc.perform(get("/api/v1/chat/threads")
                            .param("tenantId", TEST_TENANT_ID.toString())
                            .param("userId", TEST_USER_ID.toString())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").isNumber());
        }
    }

    @Nested
    @DisplayName("消息发送测试")
    class MessageSendingTests {

        @Test
        @DisplayName("应该能够成功发送消息并获得回复")
        void shouldSendMessageAndReceiveReply() throws Exception {
            // Given - 创建对话线程
            Long threadId = createTestThread();

            SendMessageCommand messageCommand = new SendMessageCommand(
                    "你好，请介绍一下你自己",
                    "TEXT",
                    false, // 不启用知识检索
                    false, // 不启用工具调用
                    0.7f,
                    1000,
                    false
            );

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/v1/chat/threads/{threadId}/messages", threadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageCommand)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userMessage").exists())
                    .andExpect(jsonPath("$.data.assistantMessage").exists())
                    .andExpect(jsonPath("$.data.userMessage.content").value("你好，请介绍一下你自己"))
                    .andExpect(jsonPath("$.data.assistantMessage.content").isNotEmpty())
                    .andReturn();

            // 验证响应结构
            String responseJson = result.getResponse().getContentAsString();
            ChatController.ApiResult<?> apiResult = objectMapper.readValue(responseJson, ChatController.ApiResult.class);
            assertTrue(apiResult.isSuccess());
        }

        @Test
        @DisplayName("应该能够处理空消息输入")
        void shouldHandleEmptyMessageInput() throws Exception {
            // Given
            Long threadId = createTestThread();

            SendMessageCommand messageCommand = new SendMessageCommand(
                    "", // 空消息
                    "TEXT",
                    false,
                    false,
                    0.7f,
                    1000,
                    false
            );

            // When & Then
            mockMvc.perform(post("/api/v1/chat/threads/{threadId}/messages", threadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageCommand)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("应该能够处理超长消息输入")
        void shouldHandleLongMessageInput() throws Exception {
            // Given
            Long threadId = createTestThread();

            // 创建一个超长的消息（超过32000字符）
            String longMessage = "测试".repeat(20000);

            SendMessageCommand messageCommand = new SendMessageCommand(
                    longMessage,
                    "TEXT",
                    false,
                    false,
                    0.7f,
                    1000,
                    false
            );

            // When & Then
            mockMvc.perform(post("/api/v1/chat/threads/{threadId}/messages", threadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageCommand)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error_code").value("CONTENT_TOO_LONG"));
        }
    }

    @Nested
    @DisplayName("知识检索集成测试")
    class KnowledgeRetrievalTests {

        @Test
        @DisplayName("应该能够启用知识检索功能")
        void shouldEnableKnowledgeRetrieval() throws Exception {
            // Given
            Long threadId = createTestThread();

            SendMessageCommand messageCommand = new SendMessageCommand(
                    "什么是机器学习？",
                    "TEXT",
                    true, // 启用知识检索
                    false,
                    0.7f,
                    2000,
                    false
            );

            // When & Then
            mockMvc.perform(post("/api/v1/chat/threads/{threadId}/messages", threadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageCommand)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.citations").isArray());
        }
    }

    @Nested
    @DisplayName("流式响应测试")
    class StreamingResponseTests {

        @Test
        @DisplayName("应该能够创建流式响应连接")
        void shouldCreateStreamingConnection() throws Exception {
            // Given
            Long threadId = createTestThread();

            SendMessageCommand messageCommand = new SendMessageCommand(
                    "请写一篇关于Spring Boot的文章",
                    "TEXT",
                    false,
                    false,
                    0.8f,
                    3000,
                    true // 启用流式模式
            );

            // When & Then
            mockMvc.perform(post("/api/v1/chat/threads/{threadId}/messages/stream", threadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageCommand)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"));
        }
    }

    @Nested
    @DisplayName("健康检查测试")
    class HealthCheckTests {

        @Test
        @DisplayName("应该返回系统健康状态")
        void shouldReturnHealthStatus() throws Exception {
            mockMvc.perform(get("/api/v1/chat/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").exists())
                    .andExpect(jsonPath("$.data.components").exists())
                    .andExpect(jsonPath("$.data.responseTimeMs").isNumber());
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("应该正确处理不存在的线程ID")
        void shouldHandleNonExistentThreadId() throws Exception {
            Long nonExistentThreadId = 99999L;

            SendMessageCommand messageCommand = new SendMessageCommand(
                    "测试消息",
                    "TEXT",
                    false,
                    false,
                    0.7f,
                    1000,
                    false
            );

            mockMvc.perform(post("/api/v1/chat/threads/{threadId}/messages", nonExistentThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(messageCommand)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error_code").value("THREAD_NOT_FOUND"));
        }

        @Test
        @DisplayName("应该正确处理无效的请求参数")
        void shouldHandleInvalidRequestParameters() throws Exception {
            CreateChatThreadCommand invalidCommand = new CreateChatThreadCommand(
                    null, // 缺少租户ID
                    TEST_USER_ID,
                    "测试对话",
                    "invalid-model", // 无效模型
                    5.0f, // 超出范围的温度值
                    null,
                    null,
                    TEST_OPERATOR_ID
            );

            mockMvc.perform(post("/api/v1/chat/threads")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidCommand)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =================== 辅助方法 ===================

    /**
     * 创建测试用的对话线程
     */
    private Long createTestThread() throws Exception {
        CreateChatThreadCommand command = new CreateChatThreadCommand(
                TEST_TENANT_ID,
                TEST_USER_ID,
                "测试对话线程",
                "mock-gpt-4o",
                0.7f,
                null,
                null,
                TEST_OPERATOR_ID
        );

        MvcResult result = mockMvc.perform(post("/api/v1/chat/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        return Long.valueOf(data.get("id").toString());
    }
}