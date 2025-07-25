package com.cloud.baseai.application.kb.service;

import com.cloud.baseai.application.kb.command.*;
import com.cloud.baseai.application.kb.dto.*;
import com.cloud.baseai.application.metrics.service.MetricsService;
import com.cloud.baseai.domain.kb.model.*;
import com.cloud.baseai.domain.kb.repository.*;
import com.cloud.baseai.domain.kb.service.DocumentProcessingService;
import com.cloud.baseai.domain.kb.service.VectorSearchService;
import com.cloud.baseai.domain.user.service.UserInfoService;
import com.cloud.baseai.infrastructure.config.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.constants.KbConstants;
import com.cloud.baseai.infrastructure.constants.SystemConstants;
import com.cloud.baseai.infrastructure.exception.BusinessException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.exception.KnowledgeBaseException;
import com.cloud.baseai.infrastructure.external.llm.EmbeddingService;
import com.cloud.baseai.infrastructure.utils.KbUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <h2>知识库应用服务</h2>
 *
 * <p>这是知识库模块的核心应用服务，负责编排复杂的业务流程。</p>
 *
 * <p><b>主要功能模块：</b></p>
 * <ul>
 * <li><b>文档管理：</b>支持文档上传、更新、删除、查询等完整生命周期管理</li>
 * <li><b>向量搜索：</b>提供语义相似度搜索、文本检索、混合搜索等多种检索方式</li>
 * <li><b>标签管理：</b>支持知识块的标签分类和组织</li>
 * <li><b>批量处理：</b>支持批量文档上传和向量生成</li>
 * <li><b>统计监控：</b>提供详细的使用统计和健康检查</li>
 * </ul>
 */
@Service
public class KnowledgeBaseAppService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseAppService.class);

    // 领域仓储
    private final DocumentRepository documentRepo;
    private final ChunkRepository chunkRepo;
    private final EmbeddingRepository embeddingRepo;
    private final TagRepository tagRepo;
    private final ChunkTagRepository chunkTagRepo;

    // 领域服务
    private final DocumentProcessingService docService;
    private final VectorSearchService vectorService;

    // 外部服务
    private final EmbeddingService embeddingService;

    // 配置
    private final KnowledgeBaseProperties config;

    // 异步执行器
    private final ExecutorService asyncExecutor;

    // 可选的用户信息服务
    @Autowired(required = false)
    private UserInfoService userInfoService;

    // 性能监控（可选）
    @Autowired(required = false)
    private MetricsService metricsService;

    public KnowledgeBaseAppService(
            DocumentRepository documentRepo,
            ChunkRepository chunkRepo,
            EmbeddingRepository embeddingRepo,
            TagRepository tagRepo,
            ChunkTagRepository chunkTagRepo,
            DocumentProcessingService docService,
            VectorSearchService vectorService,
            EmbeddingService embeddingService,
            KnowledgeBaseProperties config) {

        this.documentRepo = documentRepo;
        this.chunkRepo = chunkRepo;
        this.embeddingRepo = embeddingRepo;
        this.tagRepo = tagRepo;
        this.chunkTagRepo = chunkTagRepo;
        this.docService = docService;
        this.vectorService = vectorService;
        this.embeddingService = embeddingService;
        this.config = config;

        // 创建异步执行器
        this.asyncExecutor = Executors.newFixedThreadPool(
                config.getPerformance() != null ?
                        config.getPerformance().getAsyncPoolSize() : 10
        );
    }

    // =================== 文档管理接口实现 ===================

    /**
     * 上传并处理文档
     */
    @Transactional
    public DocumentDTO uploadDocument(UploadDocumentCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("开始上传文档: title={}, size={} bytes", cmd.title(), cmd.content().length());

        try {
            validateUploadCommand(cmd);

            String langCode = cmd.langCode();
            if (KbConstants.LanguageCodes.AUTO.equals(langCode)) {
                langCode = KbUtils.detectLanguage(cmd.content());
                log.debug("自动检测文档语言: {}", langCode);
            }

            String cleanedContent = KbUtils.cleanText(cmd.content());
            String sha256 = KbUtils.calculateSha256(cleanedContent);

            Optional<Document> existingDoc = documentRepo.findBySha256(sha256);
            if (existingDoc.isPresent()) {
                throw new KnowledgeBaseException(ErrorCode.BIZ_KB_002, existingDoc.get().title());
            }

            if (documentRepo.existsByTenantIdAndTitle(cmd.tenantId(), cmd.title())) {
                throw KnowledgeBaseException.duplicateDocumentTitle(cmd.title());
            }

            Document document = Document.create(
                    cmd.tenantId(),
                    cmd.title(),
                    cmd.sourceType(),
                    cmd.sourceUri(),
                    cmd.mimeType(),
                    langCode,
                    sha256,
                    cmd.operatorId()
            );

            document = documentRepo.save(document);
            log.info("文档已创建: id={}", document.id());

            List<Chunk> chunks;
            try {
                chunks = docService.splitIntoChunks(document, cleanedContent, cmd.operatorId());
                chunks = chunkRepo.saveAll(chunks);
                log.info("文档分块完成: 文档id={}, 分块数={}", document.id(), chunks.size());
            } catch (Exception e) {
                log.error("文档分块失败: documentId={}", document.id(), e);
                document = document.updateParsingStatus(ParsingStatus.FAILED, 0);
                documentRepo.save(document);
                throw new KnowledgeBaseException(ErrorCode.BIZ_KB_024);
            }

            document = document.updateParsingStatus(ParsingStatus.SUCCESS, chunks.size());
            document = documentRepo.save(document);

            boolean useAsyncVector = shouldUseAsyncVectorGeneration(chunks.size(), cleanedContent.length());
            if (useAsyncVector) {
                scheduleAsyncVectorGeneration(document.id(), chunks, cmd.operatorId());
                log.info("已安排异步向量生成: documentId={}", document.id());
            } else {
                generateEmbeddingsSync(chunks, config.getVector().getDefaultModel(), cmd.operatorId());
                log.info("同步向量生成完成: documentId={}", document.id());
            }

            recordMetrics("document.upload", startTime, true);
            return toDocumentDTO(document);

        } catch (Exception e) {
            recordMetrics("document.upload", startTime, false);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_006)
                    .cause(e)
                    .context("operation", "uploadDocument")
                    .context("title", cmd.title())
                    .build();
        }
    }

    /**
     * 获取文档列表
     */
    public PageResultDTO<DocumentDTO> listDocuments(Long tenantId, int page, int size) {
        log.debug("查询文档列表: tenantId={}, page={}, size={}", tenantId, page, size);

        try {
            int[] pageParams = KbUtils.validatePagination(page, size, KbConstants.SearchLimits.MAX_PAGE_SIZE);
            int validPage = pageParams[0];
            int validSize = pageParams[1];

            List<Document> documents = documentRepo.findByTenantId(tenantId, validPage, validSize);
            long total = documentRepo.countByTenantId(tenantId);

            List<DocumentDTO> documentDTOs = documents.stream()
                    .map(this::toDocumentDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(documentDTOs, total, validPage, validSize);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_KB_016)
                    .cause(e)
                    .context("operation", "listDocuments")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 获取文档详情
     */
    public DocumentDetailDTO getDocumentDetail(Long documentId) {
        log.debug("获取文档详情: documentId={}", documentId);

        try {
            Document document = documentRepo.findById(documentId)
                    .orElseThrow(() -> KnowledgeBaseException.documentNotFound(String.valueOf(documentId)));

            String creatorName = null;
            if (userInfoService != null) {
                creatorName = userInfoService.getUserName(document.createdBy());
            }

            // 获取可用的模型列表
            List<String> availableModels = Arrays.asList(
                    config.getVector().getDefaultModel()
            );

            // 根据分块数量确定文档大小类别
            String sizeCategory = determineSizeCategory(document.chunkCount());

            return new DocumentDetailDTO(
                    document.id(),
                    document.title(),
                    document.sourceType(),
                    document.sourceUri(),
                    document.mimeType(),
                    document.langCode(),
                    document.parsingStatus().getLabel(),
                    document.chunkCount(),
                    sizeCategory,
                    availableModels,
                    document.createdBy(),
                    creatorName,
                    document.createdAt(),
                    document.updatedAt()
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_015)
                    .cause(e)
                    .context("operation", "getDocumentDetail")
                    .context("documentId", documentId)
                    .build();
        }
    }

    /**
     * 更新文档信息
     */
    @Transactional
    public DocumentDTO updateDocumentInfo(UpdateDocumentInfoCommand cmd) {
        log.info("更新文档信息: documentId={}, newTitle={}", cmd.documentId(), cmd.title());

        try {
            Document document = documentRepo.findById(cmd.documentId())
                    .orElseThrow(() -> KnowledgeBaseException.documentNotFound(String.valueOf(cmd.documentId())));

            // 检查新标题是否与其他文档冲突
            Optional<Document> existingDoc = documentRepo.findByTenantId(document.tenantId(), 0, 1000)
                    .stream()
                    .filter(doc -> !doc.id().equals(cmd.documentId()) && doc.title().equals(cmd.title()))
                    .findFirst();

            if (existingDoc.isPresent()) {
                throw KnowledgeBaseException.duplicateDocumentTitle(cmd.title());
            }

            Document updatedDocument = document.updateInfo(cmd.title(), cmd.langCode(), cmd.operatorId());
            updatedDocument = documentRepo.save(updatedDocument);

            return toDocumentDTO(updatedDocument);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_010)
                    .cause(e)
                    .context("operation", "updateDocumentInfo")
                    .context("documentId", cmd.documentId())
                    .build();
        }
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(DeleteDocumentCommand cmd) {
        log.info("删除文档: documentId={}, cascade={}", cmd.documentId(), cmd.cascade());

        try {
            Document document = documentRepo.findById(cmd.documentId())
                    .orElseThrow(() -> KnowledgeBaseException.documentNotFound(String.valueOf(cmd.documentId())));

            if (cmd.cascade()) {
                // 级联删除知识块
                List<Chunk> chunks = chunkRepo.findByDocumentId(cmd.documentId());
                for (Chunk chunk : chunks) {
                    // 删除知识块的标签关联
                    chunkTagRepo.deleteByChunkId(chunk.id());
                    // 删除知识块的向量
                    embeddingRepo.deleteByChunkId(chunk.id());
                }
                // 删除知识块
                chunkRepo.deleteByDocumentId(cmd.documentId());
            }

            // 软删除文档
            documentRepo.softDelete(cmd.documentId(), cmd.operatorId());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_011)
                    .cause(e)
                    .context("operation", "deleteDocument")
                    .context("documentId", cmd.documentId())
                    .build();
        }
    }

    // =================== 搜索接口实现 ===================

    /**
     * 向量相似度搜索
     */
    public List<SearchResultDTO> vectorSearch(VectorSearchCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.debug("执行向量搜索: query={}, topK={}", cmd.query(), cmd.topK());

        try {
            validateSearchCommand(cmd);

            String optimizedQuery = optimizeSearchQuery(cmd.query());
            float[] queryVector = generateQueryVector(optimizedQuery, cmd.modelCode());

            List<VectorSearchService.SearchResult> searchResults = vectorService.search(
                    queryVector,
                    cmd.modelCode(),
                    cmd.tenantId(),
                    cmd.topK(),
                    cmd.threshold()
            );

            if (searchResults.isEmpty()) {
                recordMetrics("search.vector", startTime, true);
                return List.of();
            }

            List<SearchResultDTO> results = buildSearchResults(searchResults, optimizedQuery);
            recordSearchMetrics(startTime, results.size());

            return results;

        } catch (Exception e) {
            recordMetrics("search.vector", startTime, false);
            throw BusinessException.builder(ErrorCode.BIZ_KB_021)
                    .cause(e)
                    .context("operation", "vectorSearch")
                    .context("query", cmd.query())
                    .build();
        }
    }

    /**
     * 文本关键词搜索
     */
    public PageResultDTO<SearchResultDTO> textSearch(TextSearchCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.debug("执行文本搜索: keywords={}", cmd.keywords());

        try {
            int[] pageParams = KbUtils.validatePagination(cmd.page(), cmd.size(), KbConstants.SearchLimits.MAX_PAGE_SIZE);
            int page = pageParams[0];
            int size = pageParams[1];

            List<Chunk> chunks = chunkRepo.searchByText(cmd.tenantId(), cmd.keywords(), size * 10);

            if (cmd.hasTagFilter()) {
                chunks = filterChunksByTags(chunks, cmd.tagIds());
            }

            if (cmd.hasDocumentFilter()) {
                chunks = filterChunksByDocuments(chunks, cmd.documentIds());
            }

            List<Chunk> pagedChunks = chunks.stream()
                    .skip((long) page * size)
                    .limit(size)
                    .collect(Collectors.toList());

            List<SearchResultDTO> results = buildTextSearchResults(pagedChunks, cmd.keywords());
            recordMetrics("search.text", startTime, true);

            return PageResultDTO.of(results, (long) chunks.size(), page, size);

        } catch (Exception e) {
            recordMetrics("search.text", startTime, false);
            throw BusinessException.builder(ErrorCode.BIZ_KB_022)
                    .cause(e)
                    .context("operation", "textSearch")
                    .context("keywords", cmd.keywords())
                    .build();
        }
    }

    /**
     * 混合搜索
     */
    public List<SearchResultDTO> hybridSearch(HybridSearchCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.debug("执行混合搜索: query={}, vectorWeight={}", cmd.query(), cmd.vectorWeight());

        try {
            CompletableFuture<List<SearchResultDTO>> vectorFuture = CompletableFuture.supplyAsync(() -> {
                VectorSearchCommand vectorCmd = new VectorSearchCommand(
                        cmd.tenantId(), cmd.query(), cmd.modelCode(),
                        cmd.topK() * 2, cmd.threshold(), false
                );
                return vectorSearch(vectorCmd);
            }, asyncExecutor);

            CompletableFuture<List<SearchResultDTO>> textFuture = CompletableFuture.supplyAsync(() -> {
                TextSearchCommand textCmd = new TextSearchCommand(
                        cmd.tenantId(), cmd.query(), cmd.tagIds(), cmd.documentIds(), 0, cmd.topK() * 2
                );
                return textSearch(textCmd).content();
            }, asyncExecutor);

            List<SearchResultDTO> vectorResults = vectorFuture.get();
            List<SearchResultDTO> textResults = textFuture.get();

            List<SearchResultDTO> mergedResults = mergeSearchResults(
                    vectorResults, textResults, cmd.vectorWeight(), cmd.topK()
            );

            recordMetrics("search.hybrid", startTime, true);
            return mergedResults;

        } catch (Exception e) {
            recordMetrics("search.hybrid", startTime, false);
            throw BusinessException.builder(ErrorCode.BIZ_KB_023)
                    .cause(e)
                    .context("operation", "hybridSearch")
                    .context("query", cmd.query())
                    .build();
        }
    }

    // =================== 知识块管理实现 ===================

    /**
     * 获取知识块详情
     */
    public ChunkDetailDTO getChunkDetail(Long chunkId) {
        log.debug("获取知识块详情: chunkId={}", chunkId);

        try {
            Chunk chunk = chunkRepo.findById(chunkId)
                    .orElseThrow(() -> new KnowledgeBaseException(ErrorCode.BIZ_KB_017));

            Document document = documentRepo.findById(chunk.documentId())
                    .orElseThrow(() -> KnowledgeBaseException.documentNotFound(String.valueOf(chunk.documentId())));

            Set<Long> tagIds = chunkTagRepo.findTagIdsByChunkId(chunkId);
            List<ChunkDetailDTO.TagInfo> tagInfos = new ArrayList<>();

            if (!tagIds.isEmpty()) {
                List<Tag> tags = tagRepo.findByIds(new ArrayList<>(tagIds));
                tagInfos = tags.stream()
                        .map(tag -> new ChunkDetailDTO.TagInfo(tag.id(), tag.name()))
                        .collect(Collectors.toList());
            }

            boolean hasEmbedding = embeddingRepo
                    .findByChunkIdAndModel(chunkId, config.getVector().getDefaultModel())
                    .isPresent();

            return new ChunkDetailDTO(
                    chunk.id(),
                    chunk.documentId(),
                    document.title(),
                    chunk.chunkNo(),
                    chunk.text(),
                    chunk.langCode(),
                    chunk.tokenSize(),
                    chunk.vectorVersion(),
                    tagInfos,
                    hasEmbedding
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_025)
                    .cause(e)
                    .context("operation", "getChunkDetail")
                    .context("chunkId", chunkId)
                    .build();
        }
    }

    /**
     * 为知识块添加标签
     */
    @Transactional
    public void addChunkTags(AddChunkTagsCommand cmd) {
        log.info("为知识块添加标签: chunkId={}, tagIds={}", cmd.chunkId(), cmd.tagIds());

        try {
            // 验证知识块存在
            chunkRepo.findById(cmd.chunkId())
                    .orElseThrow(() -> new KnowledgeBaseException(ErrorCode.BIZ_KB_017));

            // 验证标签存在
            List<Tag> tags = tagRepo.findByIds(new ArrayList<>(cmd.tagIds()));
            if (tags.size() != cmd.tagIds().size()) {
                throw new KnowledgeBaseException(ErrorCode.BIZ_KB_018);
            }

            chunkTagRepo.addTags(cmd.chunkId(), cmd.tagIds());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_028)
                    .cause(e)
                    .context("operation", "addChunkTags")
                    .context("chunkId", cmd.chunkId())
                    .build();
        }
    }

    /**
     * 移除知识块标签
     */
    @Transactional
    public void removeChunkTags(Long chunkId, Set<Long> tagIds, Long operatorId) {
        log.info("移除知识块标签: chunkId={}, tagIds={}", chunkId, tagIds);

        try {
            chunkRepo.findById(chunkId)
                    .orElseThrow(() -> new KnowledgeBaseException(ErrorCode.BIZ_KB_017));

            chunkTagRepo.removeTags(chunkId, tagIds);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_029)
                    .cause(e)
                    .context("operation", "removeChunkTags")
                    .context("chunkId", chunkId)
                    .build();
        }
    }

    // =================== 标签管理实现 ===================

    /**
     * 创建标签
     */
    @Transactional
    public TagDTO createTag(CreateTagCommand cmd) {
        log.info("创建标签: name={}", cmd.name());

        try {
            Optional<Tag> existingTag = tagRepo.findByName(cmd.name());
            if (existingTag.isPresent()) {
                throw new KnowledgeBaseException(ErrorCode.BIZ_KB_027, cmd.name());
            }

            Tag tag = Tag.create(cmd.name(), cmd.remark(), cmd.operatorId());
            tag = tagRepo.save(tag);

            return new TagDTO(tag.id(), tag.name(), tag.remark());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_KB_030)
                    .cause(e)
                    .context("operation", "createTag")
                    .context("name", cmd.name())
                    .build();
        }
    }

    /**
     * 获取标签列表
     */
    public PageResultDTO<TagDTO> listTags(int page, int size, String search) {
        log.debug("查询标签列表: page={}, size={}, search={}", page, size, search);

        try {
            int[] pageParams = KbUtils.validatePagination(page, size, 50);
            int validPage = pageParams[0];
            int validSize = pageParams[1];

            List<Tag> tags;
            long total;

            if (search != null && !search.trim().isEmpty()) {
                tags = tagRepo.searchByName(search.trim(), validSize);
                total = tags.size();
            } else {
                tags = tagRepo.findAll(validPage, validSize);
                total = tagRepo.count();
            }

            List<TagDTO> tagDTOs = tags.stream()
                    .map(tag -> new TagDTO(tag.id(), tag.name(), tag.remark()))
                    .collect(Collectors.toList());

            return PageResultDTO.of(tagDTOs, total, validPage, validSize);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_KB_031)
                    .cause(e)
                    .context("operation", "listTags")
                    .context("search", search)
                    .build();
        }
    }

    /**
     * 获取热门标签
     */
    public List<TagUsageDTO> getPopularTags(int limit) {
        log.debug("获取热门标签: limit={}", limit);

        try {
            List<TagRepository.TagUsageInfo> usageInfos = tagRepo.findPopularTags(limit);

            return usageInfos.stream()
                    .map(info -> new TagUsageDTO(
                            info.tag().id(),
                            info.tag().name(),
                            info.tag().remark(),
                            info.count()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_KB_032)
                    .cause(e)
                    .context("operation", "getPopularTags")
                    .build();
        }
    }

    // =================== 向量管理实现 ===================

    /**
     * 重新生成文档向量
     */
    @Transactional
    public VectorRegenerationResult regenerateEmbeddings(GenerateEmbeddingsCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("开始重新生成文档向量: documentId={}, model={}", cmd.documentId(), cmd.modelCode());

        try {
            Document document = documentRepo.findById(cmd.documentId())
                    .orElseThrow(() -> KnowledgeBaseException.documentNotFound(String.valueOf(cmd.documentId())));

            List<Chunk> chunks = chunkRepo.findByDocumentId(cmd.documentId());
            if (chunks.isEmpty()) {
                log.warn("文档没有知识块: documentId={}", cmd.documentId());
                return VectorRegenerationResult.empty(cmd.documentId());
            }

            chunks = chunks.stream()
                    .map(chunk -> chunk.incrementVectorVersion(cmd.operatorId()))
                    .collect(Collectors.toList());
            chunks = chunkRepo.saveAll(chunks);

            VectorGenerationStats stats = generateEmbeddingsWithRetry(
                    chunks, cmd.modelCode(), cmd.operatorId()
            );

            recordMetrics("vector.regeneration", startTime, stats.successCount() > 0);

            return new VectorRegenerationResult(
                    cmd.documentId(),
                    chunks.size(),
                    stats.successCount(),
                    stats.failureCount(),
                    stats.errors()
            );

        } catch (Exception e) {
            recordMetrics("vector.regeneration", startTime, false);
            throw BusinessException.builder(ErrorCode.BIZ_KB_019)
                    .cause(e)
                    .context("operation", "regenerateEmbeddings")
                    .context("documentId", cmd.documentId())
                    .build();
        }
    }

    /**
     * 批量向量生成
     */
    public BatchVectorGenerationResult batchGenerateEmbeddings(BatchVectorGenerationCommand cmd) {
        log.info("批量向量生成: 文档数量={}, model={}", cmd.documentIds().size(), cmd.modelCode());

        try {
            String taskId = UUID.randomUUID().toString();

            // 异步执行批量向量生成
            CompletableFuture.runAsync(() -> {
                try {
                    for (Long documentId : cmd.documentIds()) {
                        GenerateEmbeddingsCommand genCmd = new GenerateEmbeddingsCommand(
                                documentId, cmd.modelCode(), cmd.operatorId()
                        );
                        regenerateEmbeddings(genCmd);
                    }
                    log.info("批量向量生成完成: taskId={}", taskId);
                } catch (Exception e) {
                    log.error("批量向量生成失败: taskId={}", taskId, e);
                }
            }, asyncExecutor);

            return new BatchVectorGenerationResult(
                    taskId,
                    cmd.documentIds().size(),
                    "STARTED",
                    "批量向量生成已启动"
            );

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_KB_020)
                    .cause(e)
                    .context("operation", "batchGenerateEmbeddings")
                    .context("documentIds", cmd.documentIds())
                    .build();
        }
    }

    // =================== 统计和监控实现 ===================

    /**
     * 获取知识库统计信息
     */
    public KbStatisticsDTO getStatistics(Long tenantId) {
        log.debug("获取知识库统计: tenantId={}", tenantId);

        try {
            // 统计文档数量
            long totalDocuments = documentRepo.countByTenantId(tenantId);

            // 统计知识块数量
            List<Document> documents = documentRepo.findByTenantId(tenantId, 0, Integer.MAX_VALUE);
            int totalChunks = documents.stream()
                    .mapToInt(doc -> doc.chunkCount() != null ? doc.chunkCount() : 0)
                    .sum();

            // 统计向量数量
            long totalEmbeddings = embeddingRepo.countByModel(config.getVector().getDefaultModel());

            // 统计标签数量
            long totalTags = tagRepo.count();

            // 按类型统计文档
            Map<String, Integer> documentsByType = documents.stream()
                    .collect(Collectors.groupingBy(
                            Document::sourceType,
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));

            // 按状态统计文档
            Map<String, Integer> documentsByStatus = documents.stream()
                    .collect(Collectors.groupingBy(
                            doc -> doc.parsingStatus().getLabel(),
                            Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                    ));

            // 按模型统计向量（简化实现）
            Map<String, Integer> embeddingsByModel = Map.of(
                    config.getVector().getDefaultModel(), (int) totalEmbeddings
            );

            return new KbStatisticsDTO(
                    tenantId,
                    (int) totalDocuments,
                    totalChunks,
                    (int) totalEmbeddings,
                    (int) totalTags,
                    documentsByType,
                    documentsByStatus,
                    embeddingsByModel
            );

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_KB_026)
                    .cause(e)
                    .context("operation", "getStatistics")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    /**
     * 健康检查
     */
    public HealthStatus checkHealth() {
        log.debug("执行健康检查");

        long startTime = System.currentTimeMillis();
        Map<String, String> components = new HashMap<>();

        String healthyStatus = SystemConstants.HealthCheck.STATUS_HEALTHY;
        String unhealthyStatus = SystemConstants.HealthCheck.STATUS_UNHEALTHY;

        try {
            // 检查数据库连接
            try {
                long count = documentRepo.countByTenantId(1L); // 使用一个测试租户ID
                components.put("database", healthyStatus);
            } catch (Exception e) {
                components.put("database", unhealthyStatus + ": " + e.getMessage());
            }

            // 检查向量服务
            try {
                // 简单的向量操作测试
                float[] testVector = new float[]{0.1f, 0.2f, 0.3f};
                KbUtils.normalizeVector(testVector);
                components.put("vector_service", healthyStatus);
            } catch (Exception e) {
                components.put("vector_service", unhealthyStatus + ": " + e.getMessage());
            }

            // 检查嵌入服务
            try {
                boolean available = embeddingService.isModelAvailable(config.getVector().getDefaultModel());
                components.put("embedding_service", available ? healthyStatus : unhealthyStatus + ": " + " model not available");
            } catch (Exception e) {
                components.put("embedding_service", unhealthyStatus + ": " + e.getMessage());
            }

            // 检查异步执行器
            try {
                if (!asyncExecutor.isShutdown()) {
                    components.put("async_executor", healthyStatus);
                } else {
                    components.put("async_executor", unhealthyStatus + ": " + "executor is shutdown");
                }
            } catch (Exception e) {
                components.put("async_executor", unhealthyStatus + ": " + e.getMessage());
            }

            boolean allHealthy = components.values().stream()
                    .allMatch(status -> status.equals(healthyStatus));

            long responseTime = System.currentTimeMillis() - startTime;

            return new HealthStatus(
                    allHealthy ? healthyStatus : unhealthyStatus,
                    components,
                    responseTime
            );

        } catch (Exception e) {
            return new HealthStatus(
                    unhealthyStatus,
                    Map.of("error", e.getMessage()),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    // =================== 批量文档上传实现 ===================

    /**
     * 批量文档上传
     */
    @Transactional
    public BatchUploadResult batchUploadDocuments(BatchUploadDocumentsCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("开始批量上传文档: 数量={}, 批次名称={}", cmd.documents().size(), cmd.batchName());

        try {
            validateBatchUploadCommand(cmd);

            List<DocumentDTO> successDocs = new ArrayList<>();
            List<BatchUploadResult.FailedDocument> failedDocs = new ArrayList<>();

            for (int i = 0; i < cmd.documents().size(); i++) {
                BatchUploadDocumentsCommand.DocumentInfo docInfo = cmd.documents().get(i);

                try {
                    UploadDocumentCommand singleCmd = new UploadDocumentCommand(
                            cmd.tenantId(),
                            docInfo.title(),
                            docInfo.content(),
                            docInfo.sourceType(),
                            docInfo.sourceUri(),
                            docInfo.mimeType(),
                            KbConstants.LanguageCodes.AUTO,
                            cmd.operatorId()
                    );

                    DocumentDTO result = uploadDocument(singleCmd);
                    successDocs.add(result);

                } catch (Exception e) {
                    log.warn("批量上传中单个文档失败: index={}, title={}", i, docInfo.title(), e);
                    failedDocs.add(new BatchUploadResult.FailedDocument(
                            i, docInfo.title(), e.getMessage()
                    ));
                }
            }

            recordMetrics("document.batch.upload", startTime, true);

            return new BatchUploadResult(
                    cmd.batchName(),
                    successDocs,
                    failedDocs,
                    successDocs.size(),
                    failedDocs.size()
            );

        } catch (Exception e) {
            recordMetrics("document.batch.upload", startTime, false);
            throw BusinessException.builder(ErrorCode.BIZ_KB_007)
                    .cause(e)
                    .context("operation", "batchUploadDocuments")
                    .context("tenantId", cmd.tenantId())
                    .build();
        }
    }

    // =================== 私有辅助方法 ===================

    private void validateUploadCommand(UploadDocumentCommand cmd) {
        if (!cmd.isContentSizeValid((int) config.getDocument().getMaxSizeBytes())) {
            throw KnowledgeBaseException.fileSizeExceeded(
                    cmd.content().getBytes().length,
                    config.getDocument().getMaxSizeBytes() / (1024 * 1024)
            );
        }

        int estimatedTokens = KbUtils.estimateTokenCount(cmd.content(), cmd.langCode());
        if (estimatedTokens > KbConstants.SystemLimits.MAX_TOKENS_PER_DOCUMENT) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_012, estimatedTokens);
        }

        if (cmd.mimeType() != null && !isSupportedMimeType(cmd.mimeType())) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_004, cmd.mimeType());
        }
    }

    private void validateBatchUploadCommand(BatchUploadDocumentsCommand cmd) {
        if (cmd.documents().size() > config.getDocument().getMaxBatchSize()) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_008, config.getDocument().getMaxBatchSize());
        }

        if (!cmd.isBatchSizeValid(config.getDocument().getMaxSizeBytes() * 10)) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_009);
        }
    }

    private void validateSearchCommand(VectorSearchCommand cmd) {
        if (!cmd.isThresholdValid()) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_013);
        }

        if (cmd.topK() > config.getSearch().getMaxTopK()) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_014, config.getSearch().getMaxTopK());
        }
    }

    private boolean shouldUseAsyncVectorGeneration(int chunkCount, int contentLength) {
        return chunkCount > 50 || contentLength > 50000;
    }

    private void scheduleAsyncVectorGeneration(Long documentId, List<Chunk> chunks, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                generateEmbeddingsSync(chunks, config.getVector().getDefaultModel(), userId);
                log.info("异步向量生成完成: documentId={}", documentId);
            } catch (Exception e) {
                log.error("异步向量生成失败: documentId={}", documentId, e);
            }
        }, asyncExecutor);
    }

    private void generateEmbeddingsSync(List<Chunk> chunks, String modelCode, Long userId) {
        List<Embedding> embeddings = new ArrayList<>();

        for (Chunk chunk : chunks) {
            try {
                float[] vector = embeddingService.generateEmbedding(chunk.text(), modelCode);
                embeddings.add(Embedding.create(
                        chunk.id(),
                        modelCode,
                        chunk.vectorVersion(),
                        vector,
                        userId
                ));
            } catch (Exception e) {
                log.error("生成Embedding失败: chunkId={}", chunk.id(), e);
            }
        }

        if (!embeddings.isEmpty()) {
            embeddingRepo.saveAll(embeddings);
            log.info("批量生成向量完成: 数量={}", embeddings.size());
        }
    }

    private VectorGenerationStats generateEmbeddingsWithRetry(List<Chunk> chunks, String modelCode, Long userId) {
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();
        int batchSize = config.getVector().getBatchSize();

        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<Chunk> batch = chunks.subList(i, end);

            for (int retry = 0; retry < 3; retry++) {
                try {
                    generateEmbeddingsSync(batch, modelCode, userId);
                    successCount += batch.size();
                    break;
                } catch (Exception e) {
                    if (retry == 2) {
                        failureCount += batch.size();
                        errors.add("批次 " + (i / batchSize + 1) + " 失败: " + e.getMessage());
                    }
                    log.warn("向量生成重试 {}: batch={}", retry + 1, i / batchSize + 1, e);
                }
            }
        }

        return new VectorGenerationStats(successCount, failureCount, errors);
    }

    private String optimizeSearchQuery(String query) {
        String optimized = KbUtils.cleanText(query);

        String[] words = optimized.split("\\s+");
        optimized = Arrays.stream(words)
                .filter(word -> word.length() > 2)
                .collect(Collectors.joining(" "));

        return optimized.isEmpty() ? query : optimized;
    }

    private float[] generateQueryVector(String query, String modelCode) {
        try {
            return embeddingService.generateEmbedding(query, modelCode);
        } catch (Exception e) {
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_033, e);
        }
    }

    /**
     * 根据向量搜索结果构建 SearchResultDTO 列表
     */
    private List<SearchResultDTO> buildSearchResults(List<VectorSearchService.SearchResult> searchResults, String query) {
        // 批量查 chunk
        List<Long> chunkIds = searchResults.stream()
                .map(VectorSearchService.SearchResult::chunkId)
                .toList();

        List<Chunk> chunks = chunkRepo.findByIds(chunkIds);
        Map<Long, Chunk> chunkMap = chunks.stream()
                .collect(Collectors.toMap(Chunk::id, chunk -> chunk));

        // 批量查文档
        Set<Long> docIds = chunks.stream()
                .map(Chunk::documentId)
                .collect(Collectors.toSet());

        Map<Long, Document> docMap = documentRepo.findByIds(new ArrayList<>(docIds))
                .stream()
                .collect(Collectors.toMap(Document::id, doc -> doc));

        // 批量查标签
        Map<Long, Set<Long>> chunkTagMap = chunkTagRepo.findTagIdsByChunkIds(chunkIds);
        Set<Long> allTagIds = chunkTagMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Map<Long, Tag> tagMap = tagRepo.findByIds(new ArrayList<>(allTagIds))
                .stream()
                .collect(Collectors.toMap(Tag::id, tag -> tag));

        // 组装 DTO
        return searchResults.stream()
                .map(result -> {
                    // 找到对应的 chunk
                    Chunk chunk = chunkMap.get(result.chunkId());
                    if (chunk == null) return null;

                    // 文档标题回退
                    Document doc = docMap.get(chunk.documentId());
                    String docTitle = doc != null ? doc.title() : "未知文档";

                    // 生成高亮
                    List<String> highlights = KbUtils.generateHighlights(
                            chunk.text(),
                            Arrays.asList(query.split("\\s+")),
                            config.getSearch().getMaxHighlights(),
                            config.getSearch().getHighlightLength()
                    );

                    // 关联标签名称
                    List<String> tagNames = chunkTagMap.getOrDefault(chunk.id(), Set.of())
                            .stream()
                            .map(tagMap::get)
                            .filter(Objects::nonNull)
                            .map(Tag::name)
                            .collect(Collectors.toList());

                    return new SearchResultDTO(
                            chunk.id(),
                            chunk.documentId(),
                            docTitle,
                            chunk.text(),
                            result.score(),
                            result.confidence(),
                            highlights,
                            tagNames
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Chunk> filterChunksByTags(List<Chunk> chunks, Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return chunks;
        }

        List<Long> chunkIds = chunks.stream().map(Chunk::id).toList();
        List<Long> filteredChunkIds = chunkTagRepo.findChunkIdsByTags(tagIds, "OR", chunkIds.size());
        Set<Long> filteredIdSet = new HashSet<>(filteredChunkIds);

        return chunks.stream()
                .filter(chunk -> filteredIdSet.contains(chunk.id()))
                .collect(Collectors.toList());
    }

    private List<Chunk> filterChunksByDocuments(List<Chunk> chunks, Set<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return chunks;
        }

        return chunks.stream()
                .filter(chunk -> documentIds.contains(chunk.documentId()))
                .collect(Collectors.toList());
    }

    private List<SearchResultDTO> buildTextSearchResults(List<Chunk> chunks, String keywords) {
        List<VectorSearchService.SearchResult> fakeResults = chunks.stream()
                .map(chunk -> new VectorSearchService.SearchResult(chunk.id(), 0.8f, KbConstants.ConfidenceLevels.MEDIUM))
                .collect(Collectors.toList());

        return buildSearchResults(fakeResults, keywords);
    }

    /**
     * 合并向量搜索结果和文本搜索结果，并根据加权分数取前 topK 条
     *
     * @param vectorResults 向量搜索结果列表
     * @param textResults   文本搜索结果列表
     * @param vectorWeight  向量结果权重（0–1 之间）
     * @param topK          最终返回的条目数
     */
    private List<SearchResultDTO> mergeSearchResults(List<SearchResultDTO> vectorResults,
                                                     List<SearchResultDTO> textResults,
                                                     float vectorWeight,
                                                     int topK) {
        Map<Long, SearchResultDTO> mergedMap = new HashMap<>();

        // 处理向量结果：score * vectorWeight
        for (SearchResultDTO result : vectorResults) {
            SearchResultDTO weighted = new SearchResultDTO(
                    result.chunkId(),
                    result.documentId(),
                    result.documentTitle(),
                    result.text(),
                    result.score() * vectorWeight,
                    result.confidence(),
                    result.highlights(),
                    result.tags()
            );
            mergedMap.put(result.chunkId(), weighted);
        }

        // 处理文本结果：与已有条目合并或新条目
        float textWeight = 1.0f - vectorWeight;
        for (SearchResultDTO result : textResults) {
            SearchResultDTO existing = mergedMap.get(result.chunkId());
            if (existing != null) {
                float combinedScore = existing.score() + (result.score() * textWeight);
                SearchResultDTO combined = new SearchResultDTO(
                        result.chunkId(),
                        result.documentId(),
                        result.documentTitle(),
                        result.text(),
                        combinedScore,
                        result.confidence(),
                        result.highlights(),
                        result.tags()
                );
                mergedMap.put(result.chunkId(), combined);
            } else {
                // 没有：直接放权重后的
                SearchResultDTO weighted = new SearchResultDTO(
                        result.chunkId(),
                        result.documentId(),
                        result.documentTitle(),
                        result.text(),
                        result.score() * textWeight,
                        result.confidence(),
                        result.highlights(),
                        result.tags()
                );
                mergedMap.put(result.chunkId(), weighted);
            }
        }

        return mergedMap.values().stream()
                .sorted((a, b) -> Float.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    private DocumentDTO toDocumentDTO(Document document) {
        String creatorName = null;
        if (userInfoService != null) {
            creatorName = userInfoService.getUserName(document.createdBy());
        }

        return new DocumentDTO(
                document.id(),
                document.title(),
                document.sourceType(),
                document.sourceUri(),
                document.mimeType(),
                document.langCode(),
                document.parsingStatus().getLabel(),
                document.chunkCount(),
                document.createdBy(),
                creatorName,
                document.createdAt(),
                document.updatedAt()
        );
    }

    private String determineSizeCategory(Integer chunkCount) {
        if (chunkCount == null || chunkCount == 0) {
            return "空文档";
        } else if (chunkCount <= 10) {
            return "小型";
        } else if (chunkCount <= 100) {
            return "中型";
        } else {
            return "大型";
        }
    }

    private boolean isSupportedMimeType(String mimeType) {
        String[] supported = config.getDocument().getSupportedMimeTypes();
        return Arrays.asList(supported).contains(mimeType);
    }

    private void recordMetrics(String operation, long startTime, boolean success) {
        if (metricsService != null) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordOperation(operation, duration, success);
        }
    }

    private void recordSearchMetrics(long startTime, int resultCount) {
        if (metricsService != null) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordSearch(duration, resultCount);
        }
    }
}