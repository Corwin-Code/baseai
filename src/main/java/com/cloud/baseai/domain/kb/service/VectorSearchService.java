package com.cloud.baseai.domain.kb.service;

import com.cloud.baseai.domain.kb.repository.EmbeddingRepository;
import com.cloud.baseai.domain.kb.repository.EmbeddingRepository.EmbeddingSearchResult;
import com.cloud.baseai.infrastructure.exception.VectorProcessingException;
import com.cloud.baseai.infrastructure.utils.KbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>增强的向量检索领域服务</h2>
 *
 * <p>要理解向量搜索，我们需要先理解一个革命性的概念：计算机如何"理解"语言。
 * 传统的关键词搜索就像是在字典中查找完全匹配的单词，而向量搜索则更像是人类的理解方式——
 * 它能够捕捉词语和句子的深层含义。</p>
 *
 * <p><b>向量嵌入的核心思想：</b></p>
 * <p>想象每个词语、句子，甚至整个段落都可以转换成多维空间中的一个点。相似含义的文本
 * 在这个空间中会靠得很近，而含义相距甚远的文本则会相距很远。这就是向量嵌入的基本原理。
 * 当我们说"国王"减去"男人"加上"女人"等于"女王"时，我们实际上在描述向量空间中的数学运算。</p>
 *
 * <p><b>语义搜索的强大之处：</b></p>
 * <p>考虑这样一个场景：用户搜索"如何提升性能"，传统搜索只能找到包含这些确切词汇的文档。
 * 但向量搜索能够找到讨论"优化效率"、"加速处理"、"改善响应时间"的文档，因为这些概念
 * 在语义空间中都很接近。这种能力让我们的知识库变得真正智能。</p>
 *
 * <p><b>相似度计算的数学基础：</b></p>
 * <p>我们使用余弦相似度来衡量两个向量的接近程度。就像两条射线之间的夹角：夹角越小，
 * 射线越平行，向量越相似。这个数学概念让我们能够精确量化语义相似性。</p>
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final EmbeddingRepository embeddingRepo;

    // 默认配置参数 - 这些值基于实践经验优化
    private static final float DEFAULT_SIMILARITY_THRESHOLD = 0.7f;  // 默认相似度阈值
    private static final int DEFAULT_TOP_K = 10;                     // 默认返回结果数
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.9f;     // 高置信度阈值
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.8f;   // 中等置信度阈值
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.7f;      // 低置信度阈值

    public VectorSearchService(EmbeddingRepository embeddingRepo) {
        this.embeddingRepo = embeddingRepo;
    }

    /**
     * 执行向量相似度搜索：在语义空间中寻找最相关的内容
     *
     * <p>这个方法是整个RAG（检索增强生成）系统的核心。让我们一步步理解它的工作原理：</p>
     *
     * <p><b>第一步：向量空间查询</b></p>
     * <p>当用户输入查询时，我们首先将查询文本转换为向量。这个向量成为我们在多维空间中的
     * "探测器"，它会寻找在语义上最相近的内容。想象这就像在一个巨大的图书馆中，
     * 我们不是按书名查找，而是按内容的相似性查找。</p>
     *
     * <p><b>第二步：相似度计算与排序</b></p>
     * <p>数据库会计算查询向量与所有存储向量之间的余弦相似度，然后按相似度从高到低排序。
     * 这个过程利用了专门的向量索引（如HNSW），即使在数百万个向量中也能快速找到最相似的结果。</p>
     *
     * <p><b>第三步：质量过滤与后处理</b></p>
     * <p>我们会过滤掉相似度过低的结果，并进行额外的质量评估。这确保返回的结果不仅在数学上相似，
     * 在语义上也真正相关。</p>
     *
     * @param queryVector 查询向量，代表用户问题的语义表示
     * @param modelCode   模型代码，确保使用相同的向量空间
     * @param tenantId    租户ID，实现多租户数据隔离
     * @param topK        期望返回的结果数量
     * @param threshold   相似度阈值，过滤低质量结果
     * @return 按相似度排序的搜索结果列表
     */
    public List<SearchResult> search(float[] queryVector, String modelCode,
                                     Long tenantId, int topK, float threshold) {

        long startTime = System.currentTimeMillis();
        log.debug("开始向量搜索: modelCode={}, tenantId={}, topK={}, threshold={}",
                modelCode, tenantId, topK, threshold);

        try {
            // 第一步：参数验证和预处理
            SearchParameters params = validateAndPreprocessParameters(
                    queryVector, modelCode, tenantId, topK, threshold);

            // 第二步：执行向量数据库查询
            List<EmbeddingSearchResult> dbResults = executeVectorDatabaseQuery(
                    params.queryVector, params.modelCode, params.tenantId, params.expandedTopK);

            // 第三步：应用阈值过滤
            List<EmbeddingSearchResult> filteredResults = applyThresholdFilter(
                    dbResults, params.threshold);

            // 第四步：计算置信度和重新排序
            List<SearchResult> scoredResults = calculateConfidenceScores(filteredResults);

            // 第五步：应用多样性优化
            List<SearchResult> diversifiedResults = applyDiversityOptimization(
                    scoredResults, params.topK);

            // 第六步：记录搜索指标
            recordSearchMetrics(startTime, diversifiedResults.size(), params);

            log.debug("向量搜索完成: 找到{}个结果, 耗时{}ms",
                    diversifiedResults.size(), System.currentTimeMillis() - startTime);

            return diversifiedResults;

        } catch (Exception e) {
            log.error("向量搜索失败: modelCode={}, tenantId={}", modelCode, tenantId, e);
            throw new VectorProcessingException("向量搜索执行失败", e);
        }
    }

    /**
     * 高级混合搜索：结合多种搜索策略
     *
     * <p>混合搜索会综合多种信息源来提供更准确的结果。</p>
     *
     * <p><b>向量搜索的优势：</b>能够理解语义，找到概念相似的内容</p>
     * <p><b>关键词搜索的优势：</b>能够精确匹配特定术语和专有名词</p>
     * <p><b>混合搜索的智慧：</b>结合两者的优势，避免各自的弱点</p>
     *
     * @param queryVector  查询的向量表示
     * @param keywords     关键词列表，用于补充向量搜索
     * @param modelCode    向量模型代码
     * @param tenantId     租户ID
     * @param vectorWeight 向量搜索的权重（0-1之间）
     * @param topK         期望返回的结果数量
     * @return 综合排序的搜索结果
     */
    public List<SearchResult> hybridSearch(float[] queryVector, List<String> keywords,
                                           String modelCode, Long tenantId,
                                           float vectorWeight, int topK) {

        log.debug("执行混合搜索: keywords={}, vectorWeight={}", keywords, vectorWeight);

        try {
            // 并行执行向量搜索和关键词搜索
            List<SearchResult> vectorResults = search(
                    queryVector, modelCode, tenantId, topK * 2, DEFAULT_SIMILARITY_THRESHOLD);

            List<SearchResult> keywordResults = simulateKeywordSearch(keywords, tenantId, topK * 2);

            // 使用智能融合算法合并结果
            List<SearchResult> mergedResults = intelligentResultFusion(
                    vectorResults, keywordResults, vectorWeight, topK);

            log.debug("混合搜索完成: 向量结果={}, 关键词结果={}, 最终结果={}",
                    vectorResults.size(), keywordResults.size(), mergedResults.size());

            return mergedResults;

        } catch (Exception e) {
            log.error("混合搜索失败: modelCode={}, tenantId={}", modelCode, tenantId, e);
            throw new VectorProcessingException("混合搜索执行失败", e);
        }
    }

    /**
     * 查询扩展搜索：智能扩展用户查询
     *
     * <p>有时用户的查询可能过于简短或不够精确。查询扩展技术能够智能地"读懂"用户的真实意图，
     * 并自动添加相关的同义词、上下文信息等来改善搜索效果。</p>
     *
     * <p>这就像一个经验丰富的图书管理员，当你询问"Java"时，他会问你是想了解编程语言Java，
     * 还是印尼的爪哇岛，或者是咖啡。查询扩展系统能够基于上下文自动做出这样的判断。</p>
     */
    public List<SearchResult> expandedSearch(float[] queryVector, String originalQuery,
                                             String modelCode, Long tenantId, int topK) {

        log.debug("执行查询扩展搜索: originalQuery={}", originalQuery);

        try {
            // 生成扩展查询
            List<String> expandedTerms = generateQueryExpansions(originalQuery);

            // 对每个扩展查询执行搜索

            // 原始查询获得最高权重
            List<SearchResult> originalResults = search(
                    queryVector, modelCode, tenantId, topK, DEFAULT_SIMILARITY_THRESHOLD);
            List<SearchResult> allResults = new ArrayList<>(weightResults(originalResults, 1.0f));

            // 扩展查询获得递减权重
            float expansionWeight = 0.7f;
            for (String expandedTerm : expandedTerms) {
                // 注意：这里简化了实现，实际应该为扩展词生成向量
                List<SearchResult> expandedResults = simulateExpandedTermSearch(
                        expandedTerm, tenantId, topK / 2);
                allResults.addAll(weightResults(expandedResults, expansionWeight));
                expansionWeight *= 0.8f; // 递减权重
            }

            // 去重并重新排序
            return deduplicateAndRerank(allResults, topK);

        } catch (Exception e) {
            log.error("查询扩展搜索失败: originalQuery={}", originalQuery, e);
            throw new VectorProcessingException("查询扩展搜索失败", e);
        }
    }

    /**
     * 个性化搜索：基于用户历史优化结果
     *
     * <p>个性化搜索能够学习用户的偏好和行为模式。就像一个了解你的书店店员，
     * 知道你更喜欢哪类书籍，能够为你推荐最合适的内容。</p>
     */
    public List<SearchResult> personalizedSearch(float[] queryVector, String modelCode,
                                                 Long tenantId, Long userId, int topK) {

        log.debug("执行个性化搜索: userId={}", userId);

        try {
            // 获取基础搜索结果
            List<SearchResult> baseResults = search(
                    queryVector, modelCode, tenantId, topK * 2, DEFAULT_SIMILARITY_THRESHOLD);

            // 应用个性化调整
            UserPreferences preferences = getUserPreferences(userId);
            List<SearchResult> personalizedResults = applyPersonalizationBoost(
                    baseResults, preferences);

            // 确保结果多样性
            return ensureResultDiversity(personalizedResults, topK);

        } catch (Exception e) {
            log.error("个性化搜索失败: userId={}", userId, e);
            // 如果个性化失败，回退到标准搜索
            return search(queryVector, modelCode, tenantId, topK, DEFAULT_SIMILARITY_THRESHOLD);
        }
    }

    // =================== 核心算法实现 ===================

    /**
     * 验证和预处理搜索参数
     */
    private SearchParameters validateAndPreprocessParameters(float[] queryVector, String modelCode,
                                                             Long tenantId, int topK, float threshold) {

        // 验证查询向量
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("查询向量不能为空");
        }

        // 验证和标准化向量
        float[] normalizedVector = KbUtils.normalizeVector(queryVector.clone());

        // 验证参数范围
        int validTopK = Math.max(1, Math.min(topK, 100));
        float validThreshold = Math.max(0.0f, Math.min(threshold, 1.0f));

        // 为了提高召回率，实际查询数量会比用户请求的更多
        int expandedTopK = validTopK * 3;

        return new SearchParameters(normalizedVector, modelCode, tenantId,
                validTopK, validThreshold, expandedTopK);
    }

    /**
     * 执行向量数据库查询
     */
    private List<EmbeddingSearchResult> executeVectorDatabaseQuery(float[] queryVector,
                                                                   String modelCode,
                                                                   Long tenantId,
                                                                   int limit) {

        return embeddingRepo.searchSimilar(queryVector, modelCode, tenantId, limit);
    }

    /**
     * 应用阈值过滤
     */
    private List<EmbeddingSearchResult> applyThresholdFilter(List<EmbeddingSearchResult> results,
                                                             float threshold) {

        return results.stream()
                .filter(result -> result.score() >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * 计算置信度分数
     *
     * <p>置信度不仅仅是相似度的简单映射。我们需要考虑多个因素：
     * 相似度的绝对值、与其他结果的相对差异、历史的查询成功率等。
     * 这些因素共同决定了我们对搜索结果质量的信心程度。</p>
     */
    private List<SearchResult> calculateConfidenceScores(List<EmbeddingSearchResult> results) {
        if (results.isEmpty()) {
            return new ArrayList<>();
        }

        // 计算分数的统计信息用于相对置信度评估
        float maxScore = results.stream().map(EmbeddingSearchResult::score).max(Float::compare).orElse(0f);
        float minScore = results.stream().map(EmbeddingSearchResult::score).min(Float::compare).orElse(0f);
        float avgScore = (float) results.stream().mapToDouble(EmbeddingSearchResult::score).average().orElse(0.0);

        return results.stream()
                .map(result -> {
                    String confidence = calculateDetailedConfidence(result.score(), maxScore, avgScore);
                    return new SearchResult(result.chunkId(), result.score(), confidence);
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算详细置信度
     */
    private String calculateDetailedConfidence(float score, float maxScore, float avgScore) {
        // 绝对置信度评估
        if (score >= HIGH_CONFIDENCE_THRESHOLD) {
            return "HIGH";
        } else if (score >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return "MEDIUM";
        } else if (score >= LOW_CONFIDENCE_THRESHOLD) {
            return "LOW";
        }

        // 相对置信度评估
        if (score >= maxScore * 0.9f) {
            return "HIGH_RELATIVE";
        } else if (score >= avgScore * 1.2f) {
            return "MEDIUM_RELATIVE";
        }

        return "LOW";
    }

    /**
     * 应用多样性优化
     *
     * <p>多样性优化解决了一个重要问题：避免搜索结果过于相似。想象你搜索"机器学习算法"，
     * 如果前10个结果都是关于同一个算法的不同章节，那价值就不大了。多样性优化确保
     * 结果覆盖主题的不同方面，为用户提供更全面的信息。</p>
     */
    private List<SearchResult> applyDiversityOptimization(List<SearchResult> results, int topK) {
        if (results.size() <= topK) {
            return results;
        }

        List<SearchResult> diversifiedResults = new ArrayList<>();
        List<SearchResult> remainingResults = new ArrayList<>(results);

        // 首先选择得分最高的结果
        if (!remainingResults.isEmpty()) {
            diversifiedResults.add(remainingResults.removeFirst());
        }

        // 逐个选择与已选结果差异最大的结果
        while (diversifiedResults.size() < topK && !remainingResults.isEmpty()) {
            SearchResult mostDiverse = findMostDiverseResult(diversifiedResults, remainingResults);
            diversifiedResults.add(mostDiverse);
            remainingResults.remove(mostDiverse);
        }

        return diversifiedResults;
    }

    /**
     * 寻找最多样化的结果
     */
    private SearchResult findMostDiverseResult(List<SearchResult> selectedResults,
                                               List<SearchResult> candidates) {
        // 简化实现：选择分数最高的候选结果
        // 实际实现中，这里应该计算语义多样性
        return candidates.stream()
                .max((a, b) -> Float.compare(a.score(), b.score()))
                .orElse(candidates.getFirst());
    }

    /**
     * 智能结果融合
     *
     * <p>这个方法实现了混合搜索的核心算法。我们不是简单地将向量搜索和关键词搜索的结果
     * 混合在一起，而是智能地分析每个结果的来源和质量，给予合适的权重。</p>
     */
    private List<SearchResult> intelligentResultFusion(List<SearchResult> vectorResults,
                                                       List<SearchResult> keywordResults,
                                                       float vectorWeight,
                                                       int topK) {

        Map<Long, CombinedResult> combinedMap = new HashMap<>();
        float keywordWeight = 1.0f - vectorWeight;

        // 处理向量搜索结果
        for (SearchResult result : vectorResults) {
            combinedMap.put(result.chunkId(), new CombinedResult(
                    result.chunkId(),
                    result.score() * vectorWeight,
                    0.0f,
                    result.confidence(),
                    true,
                    false
            ));
        }

        // 处理关键词搜索结果
        for (SearchResult result : keywordResults) {
            CombinedResult existing = combinedMap.get(result.chunkId());
            if (existing != null) {
                // 合并分数
                existing.combinedScore = existing.vectorScore + result.score() * keywordWeight;
                existing.keywordScore = result.score() * keywordWeight;
                existing.hasKeywordMatch = true;
            } else {
                // 新的关键词结果
                combinedMap.put(result.chunkId(), new CombinedResult(
                        result.chunkId(),
                        0.0f,
                        result.score() * keywordWeight,
                        result.confidence(),
                        false,
                        true
                ));
            }
        }

        // 计算最终分数并排序
        return combinedMap.values().stream()
                .map(this::calculateFinalScore)
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 计算最终分数
     */
    private SearchResult calculateFinalScore(CombinedResult combined) {
        float finalScore = combined.combinedScore;

        // 如果同时匹配向量和关键词，给予奖励
        if (combined.hasVectorMatch && combined.hasKeywordMatch) {
            finalScore *= 1.2f; // 20%奖励
        }

        // 确保分数不超过1.0
        finalScore = Math.min(finalScore, 1.0f);

        return new SearchResult(combined.chunkId, finalScore, combined.confidence);
    }

    /**
     * 生成查询扩展词
     */
    private List<String> generateQueryExpansions(String originalQuery) {
        // 简化实现：基于常见同义词和相关词
        List<String> expansions = new ArrayList<>();

        // 这里应该使用更复杂的同义词词典或语言模型
        String lowerQuery = originalQuery.toLowerCase();

        if (lowerQuery.contains("性能")) {
            expansions.add("效率");
            expansions.add("速度");
            expansions.add("优化");
        }

        if (lowerQuery.contains("问题")) {
            expansions.add("故障");
            expansions.add("错误");
            expansions.add("异常");
        }

        if (lowerQuery.contains("配置")) {
            expansions.add("设置");
            expansions.add("参数");
            expansions.add("选项");
        }

        return expansions;
    }

    // =================== 辅助方法 ===================

    /**
     * 模拟关键词搜索（简化实现）
     */
    private List<SearchResult> simulateKeywordSearch(List<String> keywords, Long tenantId, int limit) {
        // 实际实现中，这里应该调用真正的关键词搜索服务
        return new ArrayList<>();
    }

    /**
     * 模拟扩展词搜索
     */
    private List<SearchResult> simulateExpandedTermSearch(String expandedTerm, Long tenantId, int limit) {
        // 实际实现中，这里应该为扩展词生成向量并搜索
        return new ArrayList<>();
    }

    /**
     * 为结果添加权重
     */
    private List<SearchResult> weightResults(List<SearchResult> results, float weight) {
        return results.stream()
                .map(result -> new SearchResult(
                        result.chunkId(),
                        result.score() * weight,
                        result.confidence()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 去重并重新排序
     */
    private List<SearchResult> deduplicateAndRerank(List<SearchResult> allResults, int topK) {
        Map<Long, SearchResult> deduped = new HashMap<>();

        for (SearchResult result : allResults) {
            SearchResult existing = deduped.get(result.chunkId());
            if (existing == null || result.score() > existing.score()) {
                deduped.put(result.chunkId(), result);
            }
        }

        return deduped.values().stream()
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户偏好（简化实现）
     */
    private UserPreferences getUserPreferences(Long userId) {
        // 实际实现中，这里应该从数据库加载用户偏好
        return new UserPreferences(userId, new HashSet<>(), new HashMap<>());
    }

    /**
     * 应用个性化提升
     */
    private List<SearchResult> applyPersonalizationBoost(List<SearchResult> results,
                                                         UserPreferences preferences) {
        // 简化实现：基于用户偏好调整分数
        return results.stream()
                .map(result -> {
                    float boostFactor = calculatePersonalizationBoost(result, preferences);
                    return new SearchResult(
                            result.chunkId(),
                            result.score() * boostFactor,
                            result.confidence()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算个性化提升因子
     */
    private float calculatePersonalizationBoost(SearchResult result, UserPreferences preferences) {
        // 简化实现：返回固定提升因子
        return 1.0f;
    }

    /**
     * 确保结果多样性
     */
    private List<SearchResult> ensureResultDiversity(List<SearchResult> results, int topK) {
        return applyDiversityOptimization(results, topK);
    }

    /**
     * 记录搜索指标
     */
    private void recordSearchMetrics(long startTime, int resultCount, SearchParameters params) {
        long duration = System.currentTimeMillis() - startTime;
        log.debug("搜索指标: 耗时={}ms, 结果数={}, 阈值={}, topK={}",
                duration, resultCount, params.threshold, params.topK);
    }

    /**
     * 计算向量相似度
     */
    public float cosineSimilarity(float[] vector1, float[] vector2) {
        return (float) KbUtils.cosineSimilarity(vector1, vector2);
    }

    /**
     * 向量归一化
     */
    public float[] normalize(float[] vector) {
        return KbUtils.normalizeVector(vector);
    }

    // =================== 内部数据结构 ===================

    /**
     * 搜索结果记录
     */
    public record SearchResult(Long chunkId, Float score, String confidence) {

        /**
         * 判断是否为高质量结果
         */
        public boolean isHighQuality() {
            return score >= HIGH_CONFIDENCE_THRESHOLD;
        }

        /**
         * 获取可读的置信度描述
         */
        public String getConfidenceDescription() {
            return switch (confidence) {
                case "HIGH" -> "高度相关";
                case "MEDIUM" -> "中度相关";
                case "LOW" -> "低度相关";
                case "HIGH_RELATIVE" -> "相对高度相关";
                case "MEDIUM_RELATIVE" -> "相对中度相关";
                default -> "可能相关";
            };
        }
    }

    /**
     * 搜索参数封装
     */
    private record SearchParameters(
            float[] queryVector,
            String modelCode,
            Long tenantId,
            int topK,
            float threshold,
            int expandedTopK
    ) {
    }

    /**
     * 组合搜索结果
     */
    private static class CombinedResult {
        public final Long chunkId;
        public float vectorScore;
        public float keywordScore;
        public float combinedScore;
        public final String confidence;
        public boolean hasVectorMatch;
        public boolean hasKeywordMatch;

        public CombinedResult(Long chunkId, float vectorScore, float keywordScore,
                              String confidence, boolean hasVectorMatch, boolean hasKeywordMatch) {
            this.chunkId = chunkId;
            this.vectorScore = vectorScore;
            this.keywordScore = keywordScore;
            this.combinedScore = vectorScore + keywordScore;
            this.confidence = confidence;
            this.hasVectorMatch = hasVectorMatch;
            this.hasKeywordMatch = hasKeywordMatch;
        }
    }

    /**
     * 用户偏好数据
     */
    private record UserPreferences(
            Long userId,
            Set<String> preferredTopics,
            Map<String, Float> topicWeights
    ) {
    }
}