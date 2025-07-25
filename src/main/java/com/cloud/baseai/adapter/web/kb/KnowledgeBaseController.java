package com.cloud.baseai.adapter.web.kb;

import com.cloud.baseai.application.kb.command.*;
import com.cloud.baseai.application.kb.dto.*;
import com.cloud.baseai.application.kb.service.KnowledgeBaseAppService;
import com.cloud.baseai.infrastructure.exception.ApiResult;
import com.cloud.baseai.infrastructure.exception.ErrorResponse;
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

import java.util.List;
import java.util.Set;

/**
 * <h1>知识库REST控制器完整版</h1>
 *
 * <p>这个控制器是整个知识库系统的API入口，负责接收、验证和路由所有与知识库相关的请求。</p>
 *
 * <p><b>完整功能模块：</b></p>
 * <ul>
 * <li><b>文档生命周期管理：</b>从文档上传、解析、分块到最终删除的完整流程</li>
 * <li><b>智能搜索服务：</b>支持向量搜索、关键词搜索、混合搜索等多种检索方式</li>
 * <li><b>知识组织系统：</b>通过标签实现知识的分类和组织</li>
 * <li><b>批量处理能力：</b>支持大规模文档的批量上传和处理</li>
 * <li><b>系统监控接口：</b>提供健康检查、统计信息等运维功能</li>
 * </ul>
 *
 * <p><b>错误处理策略：</b></p>
 * <p>采用了分层错误处理机制：业务错误返回具体的错误信息和建议，
 * 技术错误则隐藏内部细节以保护系统安全。每个错误都有对应的HTTP状态码，
 * 使得API的使用更加直观和标准化。</p>
 */
@RestController
@RequestMapping("/api/v1/kb")
@Validated
@Tag(name = "知识库管理", description = "Knowledge Base Management APIs - 提供完整的AI驱动的知识管理解决方案")
@CrossOrigin(origins = "*", maxAge = 3600)
public class KnowledgeBaseController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseController.class);

    private final KnowledgeBaseAppService appService;

    public KnowledgeBaseController(KnowledgeBaseAppService appService) {
        this.appService = appService;
    }

    // =================== 文档管理接口 ===================

    /**
     * 上传单个文档
     *
     * <p>当用户上传一个文档时，系统会自动进行一系列复杂的处理：
     * 文本清理、语言检测、智能分块、向量生成等。</p>
     */
    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "上传文档",
            description = "上传新文档并自动进行AI处理。系统会智能分块、生成向量嵌入，使文档可被语义搜索。支持多种格式和语言。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "文档上传成功，已开始AI处理",
                    content = @Content(schema = @Schema(implementation = DocumentDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "文档内容或标题重复",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "文档大小超出限制",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'WRITE')")
    public ResponseEntity<ApiResult<DocumentDTO>> uploadDocument(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "文档上传信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "技术文档上传示例",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "title": "Spring Boot 开发指南",
                                              "content": "Spring Boot是一个基于Spring框架的快速开发工具...",
                                              "sourceType": "MARKDOWN",
                                              "sourceUri": "https://docs.spring.io/spring-boot/docs/current/reference/html/",
                                              "mimeType": "text/markdown",
                                              "langCode": "zh-CN",
                                              "operatorId": 123
                                            }
                                            """
                            )
                    )
            ) UploadDocumentCommand cmd) {

        log.info("接收文档上传请求: tenantId={}, title={}, size={} bytes",
                cmd.tenantId(), cmd.title(), cmd.content().length());

        DocumentDTO result = appService.uploadDocument(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "文档上传成功，AI处理已开始"));
    }

    /**
     * 批量上传文档
     *
     * <p>批量上传支持一次性处理数百个文档，并提供详细的成功/失败报告。</p>
     */
    @PostMapping(value = "/documents/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "批量上传文档",
            description = "一次性上传多个文档。支持部分成功模式，即使某些文档失败，其他文档仍会正常处理。适合大规模文档迁移场景。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "批量上传完成，查看详细结果"),
            @ApiResponse(responseCode = "400", description = "批量请求参数无效"),
            @ApiResponse(responseCode = "413", description = "批量大小超出限制")
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'WRITE')")
    public ResponseEntity<ApiResult<BatchUploadResult>> batchUploadDocuments(
            @Valid @RequestBody BatchUploadDocumentsCommand cmd) {

        log.info("接收批量文档上传请求: tenantId={}, 文档数量={}",
                cmd.tenantId(), cmd.documents().size());

        BatchUploadResult result = appService.batchUploadDocuments(cmd);

        String message = String.format("批量上传完成，成功: %d, 失败: %d, 成功率: %.1f%%",
                result.successCount(), result.failureCount(), result.getSuccessRate() * 100);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, message));
    }

    /**
     * 获取文档列表
     *
     * <p>用户可以浏览所有文档，了解处理状态，
     * 并通过各种过滤条件快速找到目标文档。</p>
     */
    @GetMapping("/documents")
    @Operation(
            summary = "获取文档列表",
            description = "分页获取租户下的所有文档。支持按状态、类型等条件过滤，提供完整的文档概览。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "400", description = "分页参数无效")
    })
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<DocumentDTO>>> listDocuments(
            @Parameter(description = "租户ID", required = true, example = "1")
            @RequestParam Long tenantId,

            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "页码不能小于0")
            Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100")
            Integer size,

            @Parameter(description = "文档状态过滤", example = "解析成功")
            @RequestParam(required = false) String status,

            @Parameter(description = "来源类型过滤", example = "PDF")
            @RequestParam(required = false) String sourceType) {

        log.debug("查询文档列表: tenantId={}, page={}, size={}", tenantId, page, size);

        PageResultDTO<DocumentDTO> result = appService.listDocuments(tenantId, page, size);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("查询完成，共找到 %d 个文档", result.totalElements())));
    }

    /**
     * 获取文档详情
     *
     * <p>文档详情除了基本信息外，
     * 还包括AI处理的详细状态、分块数量、可用的搜索模型等高级信息。</p>
     */
    @GetMapping("/documents/{documentId}")
    @Operation(
            summary = "获取文档详情",
            description = "获取指定文档的完整信息，包括AI处理状态、分块统计、支持的模型等。"
    )
    @PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'READ')")
    public ResponseEntity<ApiResult<DocumentDetailDTO>> getDocumentDetail(
            @Parameter(description = "文档ID", required = true, example = "123")
            @PathVariable Long documentId) {

        log.debug("获取文档详情: documentId={}", documentId);

        DocumentDetailDTO result = appService.getDocumentDetail(documentId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新文档信息
     */
    @PutMapping("/documents/{documentId}")
    @Operation(summary = "更新文档信息", description = "更新文档的标题、语言等基本信息。注意：不会重新处理内容。")
    @PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'WRITE')")
    public ResponseEntity<ApiResult<DocumentDTO>> updateDocumentInfo(
            @PathVariable Long documentId,
            @Valid @RequestBody UpdateDocumentInfoCommand cmd) {

        DocumentDTO result = appService.updateDocumentInfo(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "文档信息更新成功"));
    }

    /**
     * 删除文档
     *
     * <p>用户可以选择只删除文档记录，
     * 或者彻底清理包括AI向量在内的所有相关数据。</p>
     */
    @DeleteMapping("/documents/{documentId}")
    @Operation(
            summary = "删除文档",
            description = "删除指定文档。支持级联删除模式，可同时清理知识块、向量数据和标签关联。"
    )
    @PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'DELETE')")
    public ResponseEntity<ApiResult<Void>> deleteDocument(
            @PathVariable Long documentId,
            @RequestParam Long operatorId,
            @RequestParam(defaultValue = "true") Boolean cascade) {

        DeleteDocumentCommand cmd = new DeleteDocumentCommand(documentId, operatorId, cascade);
        appService.deleteDocument(cmd);

        return ResponseEntity.ok(ApiResult.success(null,
                cascade ? "文档及相关数据已完全删除" : "文档已删除，相关数据保留"));
    }

    // =================== 智能搜索接口 ===================

    /**
     * 向量语义搜索
     *
     * <p>与传统的关键词搜索不同，向量搜索能够理解查询的语义含义。</p>
     */
    @PostMapping("/search/vector")
    @Operation(
            summary = "向量语义搜索",
            description = "基于AI语义理解进行智能搜索。能够找到概念相关的内容，而不仅仅是关键词匹配。这是RAG系统的核心功能。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜索成功"),
            @ApiResponse(responseCode = "400", description = "搜索参数无效")
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<List<SearchResultDTO>>> vectorSearch(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "向量搜索参数",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "语义搜索示例",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "query": "如何优化系统响应速度",
                                              "modelCode": "text-embedding-3-small",
                                              "topK": 10,
                                              "threshold": 0.7
                                            }
                                            """
                            )
                    )
            ) VectorSearchCommand cmd) {

        log.info("执行向量搜索: tenantId={}, query={}, topK={}",
                cmd.tenantId(), cmd.query(), cmd.topK());

        List<SearchResultDTO> results = appService.vectorSearch(cmd);

        String message = String.format("语义搜索完成，找到 %d 条相关结果", results.size());
        return ResponseEntity.ok(ApiResult.success(results, message));
    }

    /**
     * 文本关键词搜索
     *
     * <p>传统的关键词搜索仍然有其价值，特别是当用户需要查找特定的术语、代码片段或精确匹配时。
     * 我们的实现结合了现代全文检索技术，支持模糊匹配和多关键词组合。</p>
     */
    @PostMapping("/search/text")
    @Operation(
            summary = "文本关键词搜索",
            description = "基于关键词进行全文检索搜索，支持模糊匹配和多关键词组合。适合查找特定术语和精确匹配。"
    )
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<SearchResultDTO>>> textSearch(
            @Valid @RequestBody TextSearchCommand cmd) {

        log.info("执行文本搜索: tenantId={}, keywords={}", cmd.tenantId(), cmd.keywords());

        PageResultDTO<SearchResultDTO> results = appService.textSearch(cmd);

        String message = String.format("文本搜索完成，共找到 %d 条结果", results.totalElements());
        return ResponseEntity.ok(ApiResult.success(results, message));
    }

    /**
     * 混合智能搜索
     *
     * <p>混合搜索代表了搜索技术的最新发展。它智能地结合向量搜索和关键词搜索的优势，
     * 既能理解语义，又能精确匹配。</p>
     */
    @PostMapping("/search/hybrid")
    @Operation(
            summary = "混合智能搜索",
            description = "结合向量语义搜索和关键词搜索，提供最全面和准确的检索结果。自动平衡理解能力和精确性。"
    )
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<List<SearchResultDTO>>> hybridSearch(
            @Valid @RequestBody HybridSearchCommand cmd) {

        log.info("执行混合搜索: tenantId={}, query={}, vectorWeight={}",
                cmd.tenantId(), cmd.query(), cmd.vectorWeight());

        List<SearchResultDTO> results = appService.hybridSearch(cmd);

        String message = String.format("混合搜索完成，智能融合找到 %d 条最佳结果", results.size());
        return ResponseEntity.ok(ApiResult.success(results, message));
    }

    // =================== 知识块管理接口 ===================

    /**
     * 获取知识块详情
     *
     * <p>知识块是文档的最小语义单元。通过这个接口，用户可以查看AI是如何理解和分割文档的，
     * 这对于调优分块策略和理解搜索结果都很有价值。</p>
     */
    @GetMapping("/chunks/{chunkId}")
    @Operation(
            summary = "获取知识块详情",
            description = "获取指定知识块的详细信息，包括文本内容、标签、向量状态等。有助于理解AI的处理逻辑。"
    )
    @PreAuthorize("hasPermission(#chunkId, 'CHUNK', 'READ')")
    public ResponseEntity<ApiResult<ChunkDetailDTO>> getChunkDetail(
            @Parameter(description = "知识块ID", required = true, example = "456")
            @PathVariable Long chunkId) {

        log.debug("获取知识块详情: chunkId={}", chunkId);

        ChunkDetailDTO result = appService.getChunkDetail(chunkId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 为知识块添加标签
     *
     * <p>标签系统让用户能够为AI处理后的内容添加人工的语义标注。
     * 这种人机结合的方式能够显著提升知识组织的质量和搜索的准确性。</p>
     */
    @PostMapping("/chunks/{chunkId}/tags")
    @Operation(
            summary = "为知识块添加标签",
            description = "批量为知识块添加标签，实现人工语义标注。有助于改善搜索精度和知识组织。"
    )
    @PreAuthorize("hasPermission(#chunkId, 'CHUNK', 'WRITE')")
    public ResponseEntity<ApiResult<Void>> addChunkTags(
            @PathVariable Long chunkId,
            @Valid @RequestBody AddChunkTagsCommand body) {

        log.info("为知识块添加标签: chunkId={}, tagIds={}", chunkId, body.tagIds());

        AddChunkTagsCommand cmd = new AddChunkTagsCommand(chunkId, body.tagIds(), body.operatorId());
        appService.addChunkTags(cmd);

        return ResponseEntity.ok(ApiResult.success(null,
                String.format("成功为知识块添加 %d 个标签", body.tagIds().size())));
    }

    /**
     * 移除知识块标签
     */
    @DeleteMapping("/chunks/{chunkId}/tags")
    @Operation(summary = "移除知识块标签", description = "批量移除知识块的指定标签。")
    @PreAuthorize("hasPermission(#chunkId, 'CHUNK', 'WRITE')")
    public ResponseEntity<ApiResult<Void>> removeChunkTags(
            @PathVariable Long chunkId,
            @RequestParam Set<Long> tagIds,
            @RequestParam Long operatorId) {

        log.info("移除知识块标签: chunkId={}, tagIds={}", chunkId, tagIds);

        appService.removeChunkTags(chunkId, tagIds, operatorId);
        return ResponseEntity.ok(ApiResult.success(null,
                String.format("成功移除 %d 个标签", tagIds.size())));
    }

    // =================== 标签管理接口 ===================

    /**
     * 创建标签
     *
     * <p>标签创建是知识组织的起点。好的标签体系能够让知识库变得更加有序和可发现。
     * 我们支持层次化的标签系统，用户可以建立适合自己业务的分类体系。</p>
     */
    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建标签", description = "创建新的知识分类标签。支持层次化标签体系，助力知识有序组织。")
    public ResponseEntity<ApiResult<TagDTO>> createTag(
            @Valid @RequestBody CreateTagCommand cmd) {

        log.info("创建标签: name={}", cmd.name());

        TagDTO result = appService.createTag(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "标签创建成功"));
    }

    /**
     * 获取标签列表
     */
    @GetMapping("/tags")
    @Operation(summary = "获取标签列表", description = "分页获取所有可用的标签，支持名称模糊搜索。")
    public ResponseEntity<ApiResult<PageResultDTO<TagDTO>>> listTags(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String search) {

        log.debug("查询标签列表: page={}, size={}, search={}", page, size, search);

        PageResultDTO<TagDTO> result = appService.listTags(page, size, search);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取热门标签
     *
     * <p>热门标签功能帮助用户发现知识库中的热点主题。这个功能基于标签的使用频率，
     * 能够自动发现最受关注的知识领域，为用户提供智能的标签推荐。</p>
     */
    @GetMapping("/tags/popular")
    @Operation(summary = "获取热门标签", description = "获取使用频率最高的标签列表，发现知识热点，提供智能标签推荐。")
    public ResponseEntity<ApiResult<List<TagUsageDTO>>> getPopularTags(
            @RequestParam(defaultValue = "20") Integer limit) {

        List<TagUsageDTO> result = appService.getPopularTags(limit);
        return ResponseEntity.ok(ApiResult.success(result,
                String.format("获取到 %d 个热门标签", result.size())));
    }

    // =================== 向量管理接口 ===================

    /**
     * 重新生成文档向量
     *
     * <p>随着AI模型的不断更新，重新生成向量是保持系统性能的重要操作。
     * 新的向量模型通常能提供更好的语义理解能力，从而改善搜索质量。</p>
     */
    @PostMapping("/documents/{documentId}/embeddings")
    @Operation(
            summary = "重新生成文档向量",
            description = "使用最新的AI模型为文档重新生成向量表示。适用于模型升级或优化搜索质量的场景。"
    )
    @PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'WRITE')")
    public ResponseEntity<ApiResult<VectorRegenerationResult>> regenerateEmbeddings(
            @PathVariable Long documentId,
            @Valid @RequestBody GenerateEmbeddingsCommand body) {

        log.info("重新生成文档向量: documentId={}, model={}", documentId, body.modelCode());

        GenerateEmbeddingsCommand cmd = new GenerateEmbeddingsCommand(
                documentId, body.modelCode(), body.operatorId()
        );

        VectorRegenerationResult result = appService.regenerateEmbeddings(cmd);

        String message = String.format("向量重新生成完成，成功率: %.1f%% (%d/%d)",
                result.getSuccessRate() * 100, result.successCount(), result.totalChunks());

        return ResponseEntity.ok(ApiResult.success(result, message));
    }

    /**
     * 批量向量生成
     *
     * <p>企业级部署中，可能需要为数千个文档重新生成向量。批量处理功能支持异步执行，
     * 避免阻塞用户操作，并提供进度跟踪功能。</p>
     */
    @PostMapping("/embeddings/batch")
    @Operation(
            summary = "批量向量生成",
            description = "为多个文档批量生成向量，支持异步处理和进度跟踪。适合大规模向量更新场景。"
    )
    public ResponseEntity<ApiResult<BatchVectorGenerationResult>> batchGenerateEmbeddings(
            @Valid @RequestBody BatchVectorGenerationCommand cmd) {

        log.info("批量向量生成: 文档数量={}, model={}", cmd.documentIds().size(), cmd.modelCode());

        BatchVectorGenerationResult result = appService.batchGenerateEmbeddings(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "批量向量生成任务已启动"));
    }

    // =================== 统计和监控接口 ===================

    /**
     * 获取知识库统计信息
     *
     * <p>统计信息可以了解系统的使用情况、内容分布、处理效率等关键指标。</p>
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取知识库统计", description = "获取租户下知识库的全面统计信息，包括文档数量、向量状态、标签分布等。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<KbStatisticsDTO>> getStatistics(
            @RequestParam Long tenantId) {

        KbStatisticsDTO result = appService.getStatistics(tenantId);
        return ResponseEntity.ok(ApiResult.success(result, "统计信息获取成功"));
    }

    /**
     * 系统健康检查
     *
     * <p>健康检查会检查各个关键组件的状态：
     * 数据库连接、向量服务、异步处理等，确保系统运行正常。</p>
     */
    @GetMapping("/health")
    @Operation(summary = "系统健康检查", description = "检查知识库服务的健康状态，包括数据库、向量服务、异步处理等关键组件。")
    public ResponseEntity<ApiResult<HealthStatus>> healthCheck() {

        HealthStatus status = appService.checkHealth();

        HttpStatus httpStatus = "healthy".equals(status.status()) ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus)
                .body(ApiResult.success(status, "健康检查完成"));
    }
}