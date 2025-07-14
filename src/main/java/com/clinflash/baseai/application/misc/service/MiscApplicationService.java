package com.clinflash.baseai.application.misc.service;

import com.clinflash.baseai.application.misc.command.CreatePromptTemplateCommand;
import com.clinflash.baseai.application.misc.command.UpdatePromptTemplateCommand;
import com.clinflash.baseai.application.misc.command.UploadFileCommand;
import com.clinflash.baseai.application.misc.dto.*;
import com.clinflash.baseai.application.user.service.UserInfoService;
import com.clinflash.baseai.domain.misc.model.FileObject;
import com.clinflash.baseai.domain.misc.model.PromptTemplate;
import com.clinflash.baseai.domain.misc.model.StorageStatistics;
import com.clinflash.baseai.domain.misc.repository.FileObjectRepository;
import com.clinflash.baseai.domain.misc.repository.PromptTemplateRepository;
import com.clinflash.baseai.infrastructure.exception.MiscBusinessException;
import com.clinflash.baseai.infrastructure.exception.MiscTechnicalException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h2>杂项服务应用层</h2>
 *
 * <p>这个服务就像是一个多功能的工具箱，虽然名为"杂项"，但实际上承担着
 * 平台基础设施的重要功能。提示词模板管理让AI交互更加标准化和高效，
 * 文件对象管理则为整个系统提供了可靠的存储抽象层。</p>
 *
 * <p><b>设计思想：</b></p>
 * <p>应用服务层的职责是编排业务流程，而不是实现业务逻辑。它像一个指挥家，
 * 协调各个领域服务、仓储和外部服务，确保复杂的业务操作能够顺利完成。
 * 同时还要处理事务边界、异常转换和性能优化等横切关注点。</p>
 */
@Service
public class MiscApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MiscApplicationService.class);

    // 用于提取模板变量的正则表达式，支持 {{variable}} 格式
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    // 领域仓储
    private final PromptTemplateRepository templateRepo;
    private final FileObjectRepository fileObjectRepo;

    // 可选的用户信息服务，用于获取用户名等展示信息
    @Autowired(required = false)
    private UserInfoService userInfoService;

    public MiscApplicationService(
            PromptTemplateRepository templateRepo,
            FileObjectRepository fileObjectRepo) {
        this.templateRepo = templateRepo;
        this.fileObjectRepo = fileObjectRepo;
    }

    // =================== 提示词模板管理 ===================

    /**
     * 创建提示词模板
     *
     * <p>创建模板是一个看似简单但实际复杂的过程。我们需要验证模板的唯一性、
     * 解析其中的变量、估算token消耗，还要考虑权限控制和安全检查。</p>
     */
    @Transactional
    public PromptTemplateDTO createTemplate(CreatePromptTemplateCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("创建提示词模板: name={}, modelCode={}, isSystem={}",
                cmd.name(), cmd.modelCode(), cmd.isSystemTemplate());

        try {
            // 第一步：业务规则验证
            validateTemplateCreation(cmd);

            // 第二步：检查名称唯一性
            if (templateRepo.existsByTenantIdAndName(cmd.tenantId(), cmd.name())) {
                throw new MiscBusinessException(
                        "DUPLICATE_TEMPLATE_NAME",
                        "模板名称已存在：" + cmd.name()
                );
            }

            // 第三步：创建领域对象
            PromptTemplate template;
            if (cmd.isSystemTemplate()) {
                template = PromptTemplate.createSystemTemplate(
                        cmd.name(), cmd.content(), cmd.modelCode(), cmd.operatorId());
            } else {
                template = PromptTemplate.create(
                        cmd.tenantId(), cmd.name(), cmd.content(),
                        cmd.modelCode(), cmd.operatorId());
            }

            // 第四步：持久化保存
            PromptTemplate savedTemplate = templateRepo.save(template);

            log.info("提示词模板创建成功: id={}, 耗时={}ms",
                    savedTemplate.id(), System.currentTimeMillis() - startTime);

            return toPromptTemplateDTO(savedTemplate);

        } catch (Exception e) {
            log.error("提示词模板创建失败: name={}", cmd.name(), e);
            if (e instanceof MiscBusinessException) {
                throw e;
            }
            throw new MiscTechnicalException("TEMPLATE_CREATE_ERROR", "创建提示词模板失败", e);
        }
    }

    /**
     * 获取模板列表
     *
     * <p>这个方法需要平衡功能性和性能。我们要显示用户有权限看到的所有模板，
     * 包括租户自己的和系统预置的，同时还要保证大量模板时的查询效率。</p>
     */
    public PageResultDTO<PromptTemplateDTO> listTemplates(Long tenantId, int page, int size, String keyword) {
        log.debug("查询提示词模板列表: tenantId={}, page={}, size={}, keyword={}",
                tenantId, page, size, keyword);

        try {
            List<PromptTemplate> templates;
            long total;

            // 根据是否有搜索关键词选择不同的查询策略
            if (keyword != null && !keyword.trim().isEmpty()) {
                templates = templateRepo.searchTemplates(tenantId, keyword.trim(), page, size);
                total = templates.size(); // 简化实现，实际应该有专门的count查询
            } else {
                templates = templateRepo.findVisibleTemplates(tenantId, page, size);
                total = templateRepo.countVisibleTemplates(tenantId);
            }

            // 转换为DTO并填充创建者信息
            List<PromptTemplateDTO> templateDTOs = templates.stream()
                    .map(this::toPromptTemplateDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(templateDTOs, total, page, size);

        } catch (Exception e) {
            throw new MiscTechnicalException("TEMPLATE_LIST_ERROR", "获取模板列表失败", e);
        }
    }

    /**
     * 获取模板详情
     *
     * <p>详情信息不仅包含模板的基本属性，还要提供一些派生信息，
     * 比如变量列表、预估token数等，这些信息对用户使用模板很有帮助。</p>
     */
    public PromptTemplateDetailDTO getTemplateDetail(Long templateId) {
        log.debug("获取提示词模板详情: templateId={}", templateId);

        try {
            PromptTemplate template = templateRepo.findById(templateId)
                    .orElseThrow(() -> new MiscBusinessException(
                            "TEMPLATE_NOT_FOUND",
                            "提示词模板不存在：" + templateId
                    ));

            // 解析模板中的变量
            List<String> variables = extractVariables(template.content());

            // 估算token数量（简化实现）
            int estimatedTokens = estimateTokenCount(template.content());

            // 获取使用统计（这里返回模拟数据，实际应该从统计表查询）
            int usageCount = 0; // TODO: 实现真实的使用统计

            String creatorName = null;
            if (userInfoService != null && template.createdBy() != null) {
                creatorName = userInfoService.getUserName(template.createdBy());
            }

            return new PromptTemplateDetailDTO(
                    template.id(),
                    template.tenantId(),
                    template.name(),
                    template.content(),
                    template.modelCode(),
                    template.version(),
                    template.isSystem(),
                    variables,
                    estimatedTokens,
                    usageCount,
                    creatorName,
                    template.createdAt()
            );

        } catch (Exception e) {
            if (e instanceof MiscBusinessException) {
                throw e;
            }
            throw new MiscTechnicalException("TEMPLATE_DETAIL_ERROR", "获取模板详情失败", e);
        }
    }

    /**
     * 更新模板
     */
    @Transactional
    public PromptTemplateDTO updateTemplate(UpdatePromptTemplateCommand cmd) {
        log.info("更新提示词模板: templateId={}", cmd.templateId());

        try {
            PromptTemplate template = templateRepo.findById(cmd.templateId())
                    .orElseThrow(() -> new MiscBusinessException(
                            "TEMPLATE_NOT_FOUND",
                            "提示词模板不存在：" + cmd.templateId()
                    ));

            // 检查是否有编辑权限
            if (!template.isEditable()) {
                throw new MiscBusinessException(
                        "TEMPLATE_NOT_EDITABLE",
                        "系统模板或已删除的模板无法编辑"
                );
            }

            // 构建更新后的内容，只更新非null的字段
            String newContent = cmd.content() != null ? cmd.content() : template.content();
            String newModelCode = cmd.modelCode() != null ? cmd.modelCode() : template.modelCode();

            // 执行更新（这会创建新版本）
            PromptTemplate updatedTemplate = template.updateContent(newContent, newModelCode, cmd.operatorId());
            updatedTemplate = templateRepo.save(updatedTemplate);

            return toPromptTemplateDTO(updatedTemplate);

        } catch (Exception e) {
            if (e instanceof MiscBusinessException) {
                throw e;
            }
            throw new MiscTechnicalException("TEMPLATE_UPDATE_ERROR", "更新模板失败", e);
        }
    }

    // =================== 文件对象管理 ===================

    /**
     * 上传文件
     *
     * <p>文件上传涉及多个步骤：验证、去重检查、元数据保存等。
     * 我们采用先保存元数据、后处理存储的策略，这样可以快速响应用户，
     * 并且便于实现断点续传等高级功能。</p>
     */
    @Transactional
    public FileObjectDTO uploadFile(UploadFileCommand cmd) {
        log.info("处理文件上传: originalName={}, size={} bytes", cmd.originalName(), cmd.sizeBytes());

        try {
            // 验证文件上传请求
            validateFileUpload(cmd);

            // 检查是否已存在相同内容的文件（去重）
            Optional<FileObject> existingFile = fileObjectRepo.findBySha256(cmd.sha256());
            if (existingFile.isPresent() && existingFile.get().exists()) {
                log.info("发现重复文件，返回已存在的记录: sha256={}", cmd.sha256());
                return toFileObjectDTO(existingFile.get(), cmd.originalName());
            }

            // 生成对象键
            String objectKey = cmd.generateObjectKey();

            // 创建文件对象记录
            FileObject fileObject = FileObject.create(cmd.bucket(), objectKey, cmd.sizeBytes(), cmd.sha256());
            FileObject savedFileObject = fileObjectRepo.save(fileObject);

            log.info("文件元数据保存成功: id={}, objectKey={}", savedFileObject.id(), objectKey);

            return toFileObjectDTO(savedFileObject, cmd.originalName());

        } catch (Exception e) {
            if (e instanceof MiscBusinessException) {
                throw e;
            }
            throw new MiscTechnicalException("FILE_UPLOAD_ERROR", "文件上传失败", e);
        }
    }

    /**
     * 获取存储统计
     */
    public StorageStatisticsDTO getStorageStatistics() {
        try {
            List<StorageStatistics> stats = fileObjectRepo.getStorageStatistics();

            long totalFiles = stats.stream().mapToLong(StorageStatistics::fileCount).sum();
            long totalSizeBytes = stats.stream().mapToLong(StorageStatistics::totalSizeBytes).sum();

            List<StorageStatisticsDTO.BucketStatistics> bucketStats = stats.stream()
                    .map(stat -> new StorageStatisticsDTO.BucketStatistics(
                            stat.bucket(),
                            stat.fileCount(),
                            stat.totalSizeBytes(),
                            formatFileSize(stat.totalSizeBytes())
                    ))
                    .collect(Collectors.toList());

            return new StorageStatisticsDTO(
                    totalFiles,
                    totalSizeBytes,
                    formatFileSize(totalSizeBytes),
                    bucketStats
            );

        } catch (Exception e) {
            throw new MiscTechnicalException("STORAGE_STATS_ERROR", "获取存储统计失败", e);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证模板创建请求
     */
    private void validateTemplateCreation(CreatePromptTemplateCommand cmd) {
        if (!cmd.hasValidContent()) {
            throw new MiscBusinessException(
                    "INVALID_TEMPLATE_CONTENT",
                    "模板内容格式无效或包含危险字符"
            );
        }

        // 系统模板创建需要特殊权限（这里简化处理）
        if (cmd.isSystemTemplate()) {
            // TODO: 实现权限检查逻辑
            log.warn("创建系统模板，请确保操作者具有相应权限: operatorId={}", cmd.operatorId());
        }
    }

    /**
     * 验证文件上传请求
     */
    private void validateFileUpload(UploadFileCommand cmd) {
        if (!cmd.isValidSize()) {
            throw new MiscBusinessException(
                    "INVALID_FILE_SIZE",
                    "文件大小超出允许范围"
            );
        }

        // 验证SHA256格式
        if (!cmd.sha256().matches("^[a-fA-F0-9]{64}$")) {
            throw new MiscBusinessException(
                    "INVALID_SHA256",
                    "SHA256哈希值格式无效"
            );
        }
    }

    /**
     * 从模板内容中提取变量
     *
     * <p>这个方法展示了如何使用正则表达式来解析结构化文本。
     * 提取变量列表对于模板的动态填充和验证很重要。</p>
     */
    private List<String> extractVariables(String content) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        return matcher.results()
                .map(result -> result.group(1))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 估算token数量
     *
     * <p>这是一个简化的实现，实际的token估算需要考虑具体的模型和编码方式。
     * 不同语言、不同模型的token计算规则都不相同。</p>
     */
    private int estimateTokenCount(String content) {
        // 简化的估算：英文约4个字符=1个token，中文约1.5个字符=1个token
        // 实际应该使用专门的tokenizer库
        return (int) Math.ceil(content.length() / 3.5);
    }

    /**
     * 格式化文件大小为人类可读格式
     */
    private String formatFileSize(long sizeBytes) {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else if (sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", sizeBytes / (1024.0 * 1024 * 1024));
        }
    }

    // =================== DTO转换方法 ===================

    private PromptTemplateDTO toPromptTemplateDTO(PromptTemplate template) {
        String creatorName = null;
        if (userInfoService != null && template.createdBy() != null) {
            creatorName = userInfoService.getUserName(template.createdBy());
        }

        return new PromptTemplateDTO(
                template.id(),
                template.name(),
                template.content(),
                template.modelCode(),
                template.version(),
                template.isSystem(),
                creatorName,
                template.createdAt()
        );
    }

    private FileObjectDTO toFileObjectDTO(FileObject fileObject, String originalName) {
        return new FileObjectDTO(
                fileObject.id(),
                fileObject.bucket(),
                fileObject.objectKey(),
                originalName,
                fileObject.sizeBytes(),
                formatFileSize(fileObject.sizeBytes()),
                null, // contentType需要从其他地方获取
                generateDownloadUrl(fileObject), // 生成下载链接
                fileObject.createdAt()
        );
    }

    /**
     * 生成文件下载链接
     *
     * <p>实际实现中，这里应该调用对象存储服务生成预签名URL，
     * 确保文件访问的安全性和时效性。</p>
     */
    private String generateDownloadUrl(FileObject fileObject) {
        // 简化实现，实际应该生成预签名URL
        return "/api/v1/files/" + fileObject.id() + "/download";
    }
}