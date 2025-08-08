package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelInfo;
import com.cloud.baseai.infrastructure.utils.KbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h2>Anthropic向量嵌入服务</h2>
 *
 * <p>Anthropic嵌入服务的实现。需要注意的是，Anthropic主要专注于文本生成和对话，
 * 目前并未提供专门的嵌入模型API。这个服务实现为架构完整性和
 * 未来扩展做准备。</p>
 *
 * <p><b>当前状态：</b></p>
 * <ul>
 * <li><b>功能限制：</b>Anthropic目前不提供专门的向量嵌入API</li>
 * <li><b>架构保留：</b>保持与其他LLM服务的接口一致性</li>
 * <li><b>Spring AI准备：</b>为Spring AI可能添加的Anthropic嵌入支持做准备</li>
 * <li><b>替代方案：</b>建议使用OpenAI或通义千问的嵌入服务</li>
 * </ul>
 *
 * <p><b>技术说明：</b></p>
 * <ul>
 * <li><b>接口兼容：</b>实现EmbeddingService接口，保持API一致性</li>
 * <li><b>错误处理：</b>提供明确的不支持错误信息</li>
 * <li><b>配置管理：</b>保留配置结构以便未来升级</li>
 * <li><b>监控集成：</b>提供基础的健康检查和状态报告</li>
 * </ul>
 *
 * <p><b>使用建议：</b></p>
 * <p>在多LLM环境中，建议使用EmbeddingModelFactory自动选择可用的嵌入服务，
 * 而不是直接使用AnthropicEmbeddingService。这样可以自动回退到OpenAI或通义千问的嵌入服务。</p>
 *
 * <p><b>未来扩展：</b></p>
 * <p>当Anthropic发布嵌入API或Spring AI添加相关支持时，可以在此基础上快速扩展实现。</p>
 */
@Service
public class AnthropicEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicEmbeddingService.class);

    /**
     * 支持的模型信息（预留配置）
     */
    private static final Map<String, ModelInfo> SUPPORTED_MODELS = initializeSupportedModels();

    private final LlmProperties llmProperties;

    /**
     * 构造函数，初始化Anthropic嵌入服务
     *
     * @param llmProperties LLM配置属性
     */
    public AnthropicEmbeddingService(LlmProperties llmProperties) {

        this.llmProperties = llmProperties;

        log.info("Anthropic嵌入服务初始化完成 (当前不支持嵌入功能): baseUrl={}",
                llmProperties.getAnthropic().getBaseUrl());
        log.warn("注意: Anthropic目前不提供专门的嵌入API，建议使用OpenAI或通义千问的嵌入服务");
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        log.debug("尝试使用Anthropic生成向量嵌入: model={}, textLength={}",
                modelCode, text != null ? text.length() : 0);

        // Anthropic目前不支持嵌入，抛出明确的错误信息
        throw new ChatException(ErrorCode.EXT_EMB_012,
                "Anthropic目前不提供向量嵌入服务，请使用OpenAI或通义千问的嵌入服务");
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts, String modelCode) {
        log.debug("尝试使用Anthropic批量生成向量嵌入: model={}, count={}",
                modelCode, texts != null ? texts.size() : 0);

        throw new ChatException(ErrorCode.EXT_EMB_012,
                "Anthropic目前不提供向量嵌入服务，请使用OpenAI或通义千问的嵌入服务");
    }

    @Override
    public EmbeddingResult generateEmbeddingWithDetails(String text, String modelCode) {
        log.debug("尝试使用Anthropic生成详细向量嵌入: model={}", modelCode);

        throw new ChatException(ErrorCode.EXT_EMB_012,
                "Anthropic目前不提供向量嵌入服务，请使用OpenAI或通义千问的嵌入服务");
    }

    @Override
    public List<EmbeddingResult> generateEmbeddingsWithDetails(List<String> texts, String modelCode) {
        log.debug("尝试使用Anthropic批量生成详细向量嵌入: model={}, count={}",
                modelCode, texts != null ? texts.size() : 0);

        throw new ChatException(ErrorCode.EXT_EMB_012,
                "Anthropic目前不提供向量嵌入服务，请使用OpenAI或通义千问的嵌入服务");
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        // Anthropic嵌入模型当前都不可用
        log.debug("检查Anthropic嵌入模型可用性: model={}, 结果=false (不支持)", modelCode);
        return false;
    }

    @Override
    public int getVectorDimension(String modelCode) {
        // 返回预留的向量维度配置
        ModelInfo modelInfo = SUPPORTED_MODELS.get(modelCode);
        int dimension = modelInfo != null ? modelInfo.dimension() : 1536;

        log.debug("获取Anthropic模型向量维度: model={}, dimension={} (预留配置)", modelCode, dimension);
        return dimension;
    }

    @Override
    public ModelInfo getModelInfo(String modelCode) {
        ModelInfo baseInfo = SUPPORTED_MODELS.get(modelCode);
        if (baseInfo == null) {
            return null;
        }

        // 返回模型信息，但标记为不可用
        return new ModelInfo(
                baseInfo.name(),
                baseInfo.provider(),
                baseInfo.dimension(),
                baseInfo.maxTokens(),
                baseInfo.costPerToken(),
                baseInfo.description() + " (当前不支持嵌入功能)",
                false // 标记为不可用
        );
    }

    @Override
    public List<String> getSupportedModels() {
        // 返回空列表，因为当前不支持任何嵌入模型
        log.debug("获取Anthropic支持的嵌入模型列表: 当前为空 (不支持嵌入功能)");
        return Collections.emptyList();
    }

    @Override
    public boolean isHealthy() {
        // 嵌入服务当前不可用，但Anthropic聊天服务可能健康
        boolean anthropicServiceEnabled = llmProperties.getAnthropic().getEnabled();
        log.debug("Anthropic嵌入服务健康检查: enabled={}, healthy=false (不支持嵌入)", anthropicServiceEnabled);
        return false;
    }

    @Override
    public boolean warmupModel(String modelCode) {
        log.debug("尝试预热Anthropic嵌入模型: model={}, 结果=false (不支持)", modelCode);
        return false;
    }

    @Override
    public int estimateTokenCount(String text, String modelCode) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // 使用通用的token估算方法
        String language = KbUtils.detectLanguage(text);
        int tokenCount = KbUtils.estimateTokenCount(text, language);

        log.debug("估算Anthropic嵌入token数量: model={}, tokens={}", modelCode, tokenCount);
        return tokenCount;
    }

    // =================== 公共方法（用于监控和管理）===================

    /**
     * 获取Anthropic嵌入服务的状态信息
     *
     * @return 状态信息映射
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("serviceName", "Anthropic Embedding Service");
        status.put("provider", "anthropic");
        status.put("supported", false);
        status.put("reason", "Anthropic目前不提供专门的向量嵌入API");
        status.put("alternative", Arrays.asList("openai", "qwen"));
        status.put("baseUrl", llmProperties.getAnthropic().getBaseUrl());
        status.put("enabled", llmProperties.getAnthropic().getEnabled());
        status.put("springAiIntegration", true);
        status.put("futureSupport", "等待Anthropic或Spring AI添加嵌入支持");

        log.debug("获取Anthropic嵌入服务状态: {}", status);
        return status;
    }

    /**
     * 检查Spring AI是否添加了Anthropic嵌入支持
     *
     * <p>这个方法预留用于检查Spring AI是否新增了Anthropic嵌入模型支持。
     * 可以通过反射检查类是否存在来确定。</p>
     *
     * @return 是否检测到嵌入功能
     */
    public boolean checkForSpringAiEmbeddingSupport() {
        try {
            log.debug("检查Spring AI是否新增了Anthropic嵌入支持");

            // 检查是否存在AnthropicEmbeddingModel类
            try {
                Class.forName("org.springframework.ai.anthropic.AnthropicEmbeddingModel");
                log.info("检测到Spring AI Anthropic嵌入模型支持！");
                return true;
            } catch (ClassNotFoundException e) {
                log.debug("Spring AI暂未提供Anthropic嵌入模型支持");
                return false;
            }

        } catch (Exception e) {
            log.debug("检查Spring AI Anthropic嵌入功能支持时发生错误", e);
            return false;
        }
    }

    /**
     * 获取推荐的替代嵌入服务
     *
     * @return 推荐的嵌入服务提供商列表
     */
    public List<String> getRecommendedAlternatives() {
        List<String> alternatives = new ArrayList<>();

        // 根据当前配置推荐可用的服务
        if (llmProperties.getOpenai().getEnabled()) {
            alternatives.add("openai");
        }

        if (llmProperties.getQwen().getEnabled()) {
            alternatives.add("qwen");
        }

        log.debug("Anthropic嵌入服务推荐的替代方案: {}", alternatives);
        return alternatives;
    }

    /**
     * 获取架构兼容性信息
     *
     * <p>提供与其他嵌入服务的架构兼容性信息，便于系统集成。</p>
     *
     * @return 兼容性信息
     */
    public Map<String, Object> getArchitectureCompatibility() {
        Map<String, Object> compatibility = new HashMap<>();
        compatibility.put("interfaceCompatible", true);
        compatibility.put("springAiReady", true);
        compatibility.put("configurationReady", true);
        compatibility.put("factoryIntegration", true);
        compatibility.put("fallbackSupported", true);
        compatibility.put("monitoringEnabled", true);

        log.debug("Anthropic嵌入服务架构兼容性: {}", compatibility);
        return compatibility;
    }

    // =================== 私有方法 ===================

    /**
     * 初始化支持的模型信息（预留配置）
     *
     * <p>这些是预留的模型配置，为未来Anthropic可能推出的嵌入服务或
     * Spring AI可能添加的支持做准备。</p>
     */
    private static Map<String, ModelInfo> initializeSupportedModels() {
        Map<String, ModelInfo> models = new HashMap<>();

        // 预留的Anthropic嵌入模型配置
        // 基于Claude现有模型的命名规律和可能的嵌入模型规格

        models.put("claude-3-haiku-embedding", ModelInfo.create(
                "claude-3-haiku-embedding", "anthropic", 1536, 8192, 0.0001,
                "Claude 3 Haiku嵌入模型（预留配置，当前不可用）"
        ));

        models.put("claude-3-sonnet-embedding", ModelInfo.create(
                "claude-3-sonnet-embedding", "anthropic", 1536, 8192, 0.0002,
                "Claude 3 Sonnet嵌入模型（预留配置，当前不可用）"
        ));

        models.put("claude-3-opus-embedding", ModelInfo.create(
                "claude-3-opus-embedding", "anthropic", 2048, 8192, 0.0005,
                "Claude 3 Opus嵌入模型（预留配置，当前不可用）"
        ));

        // 为3.5 Sonnet预留配置
        models.put("claude-3-5-sonnet-embedding", ModelInfo.create(
                "claude-3-5-sonnet-embedding", "anthropic", 1536, 8192, 0.0002,
                "Claude 3.5 Sonnet嵌入模型（预留配置，当前不可用）"
        ));

        log.debug("初始化Anthropic预留嵌入模型配置: {} 个模型", models.size());
        return models;
    }

    /**
     * 未来可能的嵌入实现方法（预留）
     *
     * <p>当Anthropic推出嵌入API或Spring AI添加支持时，可以在这里实现具体的调用逻辑。</p>
     */
    @SuppressWarnings("unused")
    private float[] generateEmbeddingInternal(String text, String modelCode) {
        // 预留的实现框架
        // 可能的实现方式：
        // 1. 使用Spring AI的AnthropicEmbeddingModel（如果存在）
        // 2. 直接调用Anthropic嵌入API（如果推出）
        // 3. 使用Claude聊天模型生成伪嵌入（不推荐，但可作为兜底）

        throw new UnsupportedOperationException("Anthropic嵌入功能尚未实现");
    }

    /**
     * 未来可能的批量嵌入实现方法（预留）
     */
    @SuppressWarnings("unused")
    private List<float[]> generateEmbeddingsInternal(List<String> texts, String modelCode) {
        // 预留的批量实现框架
        throw new UnsupportedOperationException("Anthropic批量嵌入功能尚未实现");
    }
}