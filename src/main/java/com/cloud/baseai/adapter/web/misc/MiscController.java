package com.cloud.baseai.adapter.web.misc;

import com.cloud.baseai.application.misc.command.CreatePromptTemplateCommand;
import com.cloud.baseai.application.misc.command.UpdatePromptTemplateCommand;
import com.cloud.baseai.application.misc.command.UploadFileCommand;
import com.cloud.baseai.application.misc.dto.*;
import com.cloud.baseai.application.misc.service.MiscApplicationService;
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

/**
 * <h1>基础设施功能REST控制器</h1>
 *
 * <p>这个控制器管理着平台的两个核心基础设施：提示词模板和文件对象管理。</p>
 *
 * <p><b>提示词模板的重要性：</b></p>
 * <p>好的提示词就像是与人工智能对话的"语法规则"。标准化、版本化的
 * 提示词模板不仅能提高AI输出的质量和一致性，还能让团队协作更加高效。</p>
 *
 * <p><b>文件管理的战略意义：</b></p>
 * <p>文件对象管理提供了统一的存储抽象层，让应用程序不需要关心文件实际存储在哪里——
 * 可能是云存储、本地磁盘，或者未来的某种新技术。</p>
 */
@RestController
@RequestMapping("/api/v1/misc")
@Validated
@Tag(name = "基础设施功能管理", description = "Miscellaneous APIs - 提供提示词模板和文件对象管理功能")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MiscController {

    private static final Logger log = LoggerFactory.getLogger(MiscController.class);

    private final MiscApplicationService appService;

    public MiscController(MiscApplicationService appService) {
        this.appService = appService;
    }

    // =================== 提示词模板管理接口 ===================

    /**
     * 创建提示词模板
     *
     * <p>创建模板就像是为AI助手编写工作手册。每个模板都承载着特定的业务逻辑和
     * 交互模式，是人类智慧与人工智能结合的重要载体。</p>
     */
    @PostMapping(value = "/prompt-templates", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建提示词模板",
            description = "创建新的AI提示词模板，支持变量占位符和多模型适配。每个模板都会自动解析变量并估算token消耗。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "模板创建成功",
                    content = @Content(schema = @Schema(implementation = PromptTemplateDTO.class))),
            @ApiResponse(responseCode = "400", description = "模板内容无效"),
            @ApiResponse(responseCode = "409", description = "模板名称已存在")
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'WRITE')")
    public ResponseEntity<ApiResult<PromptTemplateDTO>> createTemplate(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "提示词模板创建信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "代码审查模板",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "name": "代码审查助手",
                                              "content": "你是一个资深的代码审查专家。请仔细审查以下{{language}}代码，关注以下方面：\\n1. 代码质量和最佳实践\\n2. 潜在的安全问题\\n3. 性能优化建议\\n\\n代码内容：\\n{{code}}",
                                              "modelCode": "gpt-4o",
                                              "operatorId": 123
                                            }
                                            """
                            )
                    )
            ) CreatePromptTemplateCommand cmd) {

        log.info("创建提示词模板请求: name={}, modelCode={}", cmd.name(), cmd.modelCode());

        PromptTemplateDTO result = appService.createTemplate(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "提示词模板创建成功"));
    }

    /**
     * 获取模板列表
     *
     * <p>模板列表是用户的创作工具箱。通过合理的分类、搜索和展示，
     * 让用户能够快速找到合适的模板，提高工作效率。</p>
     */
    @GetMapping("/prompt-templates")
    @Operation(
            summary = "获取提示词模板列表",
            description = "分页获取用户可见的所有提示词模板，包括租户模板和系统预置模板。支持关键词搜索。"
    )
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<PromptTemplateDTO>>> listTemplates(
            @Parameter(description = "租户ID", required = true)
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

            @Parameter(description = "搜索关键词", example = "代码审查")
            @RequestParam(required = false) String keyword) {

        log.debug("查询提示词模板列表: tenantId={}, page={}, size={}, keyword={}",
                tenantId, page, size, keyword);

        PageResultDTO<PromptTemplateDTO> result = appService.listTemplates(tenantId, page, size, keyword);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("查询完成，共找到 %d 个模板", result.totalElements())));
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/prompt-templates/{templateId}")
    @Operation(summary = "获取模板详情", description = "获取指定模板的完整信息，包括变量解析和使用统计。")
    @PreAuthorize("hasPermission(#templateId, 'PROMPT_TEMPLATE', 'READ')")
    public ResponseEntity<ApiResult<PromptTemplateDetailDTO>> getTemplateDetail(
            @PathVariable Long templateId) {

        log.debug("获取模板详情: templateId={}", templateId);

        PromptTemplateDetailDTO result = appService.getTemplateDetail(templateId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新模板
     */
    @PutMapping("/prompt-templates/{templateId}")
    @Operation(summary = "更新模板", description = "更新提示词模板内容，会创建新版本保持历史可追溯。")
    @PreAuthorize("hasPermission(#templateId, 'PROMPT_TEMPLATE', 'WRITE')")
    public ResponseEntity<ApiResult<PromptTemplateDTO>> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody UpdatePromptTemplateCommand cmd) {

        log.info("更新提示词模板: templateId={}", templateId);

        PromptTemplateDTO result = appService.updateTemplate(cmd);
        return ResponseEntity.ok(ApiResult.success(result, "模板更新成功"));
    }

    // =================== 文件对象管理接口 ===================

    /**
     * 上传文件
     *
     * <p>文件上传不仅仅是简单的数据传输，更是一个涉及安全检查、去重处理、
     * 元数据管理的复杂过程。我们的设计确保了文件的安全性和系统的高效性。</p>
     */
    @PostMapping(value = "/files", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "上传文件",
            description = "上传文件并自动处理去重、安全检查等。支持大文件上传和断点续传机制。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "文件上传成功",
                    content = @Content(schema = @Schema(implementation = FileObjectDTO.class))),
            @ApiResponse(responseCode = "400", description = "文件格式无效"),
            @ApiResponse(responseCode = "413", description = "文件大小超出限制")
    })
    public ResponseEntity<ApiResult<FileObjectDTO>> uploadFile(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "文件上传信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "图片文件上传",
                                    value = """
                                            {
                                              "bucket": "user-uploads",
                                              "originalName": "profile-avatar.jpg",
                                              "contentType": "image/jpeg",
                                              "sizeBytes": 1048576,
                                              "sha256": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"
                                            }
                                            """
                            )
                    )
            ) UploadFileCommand cmd) {

        log.info("文件上传请求: originalName={}, size={} bytes", cmd.originalName(), cmd.sizeBytes());

        FileObjectDTO result = appService.uploadFile(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "文件上传成功"));
    }

    /**
     * 获取存储统计
     *
     * <p>存储统计为管理员提供了系统资源使用的全貌，这对于容量规划、
     * 成本控制和性能优化都至关重要。</p>
     */
    @GetMapping("/storage/statistics")
    @Operation(summary = "获取存储统计", description = "获取文件存储的详细统计信息，包括各存储桶的使用情况。")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<StorageStatisticsDTO>> getStorageStatistics() {

        StorageStatisticsDTO result = appService.getStorageStatistics();
        return ResponseEntity.ok(ApiResult.success(result));
    }
}