package com.cloud.baseai.domain.kb.model;

import java.time.OffsetDateTime;

/**
 * <h2>文档实体</h2>
 *
 * <p>知识库文档的核心实体，代表用户上传的各种格式文档。
 * 文档经过解析后会被分割成多个知识块（Chunk），并生成对应的向量表示。</p>
 *
 * <p><b>业务规则：</b></p>
 * <ul>
 * <li>文档标题在租户内必须唯一</li>
 * <li>通过 SHA256 哈希值防止重复内容</li>
 * <li>支持软删除机制</li>
 * <li>解析状态变更有严格的状态机约束</li>
 * </ul>
 *
 * @param id            文档唯一标识
 * @param tenantId      归属租户ID，实现数据隔离
 * @param title         文档标题，用于展示和搜索
 * @param sourceType    文档来源类型（PDF、URL、Markdown等）
 * @param sourceUri     文档原始地址或上传路径
 * @param mimeType      MIME类型，用于选择合适的解析器
 * @param langCode      文档主要语言代码（zh-CN、en等）
 * @param parsingStatus 解析状态枚举
 * @param chunkCount    成功解析的知识块数量
 * @param sha256        内容SHA256哈希，用于去重
 * @param createdBy     文档创建人用户ID
 * @param updatedBy     最近修改人用户ID
 * @param createdAt     创建时间戳
 * @param updatedAt     最近更新时间戳
 * @param deletedAt     软删除时间戳，null表示未删除
 */
public record Document(
        Long id,
        Long tenantId,
        String title,
        String sourceType,
        String sourceUri,
        String mimeType,
        String langCode,
        ParsingStatus parsingStatus,
        Integer chunkCount,
        String sha256,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新文档实例的工厂方法
     *
     * <p>新创建的文档初始状态为待解析，分块数量为0。</p>
     *
     * @param tenantId   租户ID
     * @param title      文档标题
     * @param sourceType 来源类型
     * @param sourceUri  源地址
     * @param mimeType   MIME类型
     * @param langCode   语言代码
     * @param sha256     内容哈希
     * @param createdBy  创建人ID
     * @return 新文档实例
     */
    public static Document create(Long tenantId, String title, String sourceType,
                                  String sourceUri, String mimeType, String langCode,
                                  String sha256, Long createdBy) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Document(
                null, tenantId, title, sourceType, sourceUri, mimeType, langCode,
                ParsingStatus.PENDING, 0, sha256, createdBy, null, now, now, null
        );
    }

    /**
     * 更新解析状态
     *
     * <p>文档解析完成后，需要更新状态和分块数量。
     * 这个方法确保状态变更的一致性。</p>
     *
     * @param newStatus  新的解析状态
     * @param chunkCount 分块数量（成功时有效）
     * @return 更新后的文档实例
     * @throws IllegalStateException 如果状态转换不合法
     */
    public Document updateParsingStatus(ParsingStatus newStatus, Integer chunkCount) {
        // 验证状态转换的合法性
        if (this.parsingStatus == ParsingStatus.SUCCESS && newStatus == ParsingStatus.PENDING) {
            throw new IllegalStateException("已成功解析的文档不能回退到待解析状态");
        }

        return new Document(
                this.id, this.tenantId, this.title, this.sourceType, this.sourceUri,
                this.mimeType, this.langCode, newStatus, chunkCount, this.sha256,
                this.createdBy, this.updatedBy, this.createdAt, OffsetDateTime.now(), this.deletedAt
        );
    }

    /**
     * 更新文档基本信息
     *
     * <p>允许更新标题、描述等非核心属性。</p>
     *
     * @param newTitle    新标题
     * @param newLangCode 新语言代码
     * @param updatedBy   修改人ID
     * @return 更新后的文档实例
     */
    public Document updateInfo(String newTitle, String newLangCode, Long updatedBy) {
        return new Document(
                this.id, this.tenantId, newTitle, this.sourceType, this.sourceUri,
                this.mimeType, newLangCode, this.parsingStatus, this.chunkCount, this.sha256,
                this.createdBy, updatedBy, this.createdAt, OffsetDateTime.now(), this.deletedAt
        );
    }

    /**
     * 软删除文档
     *
     * @param deletedBy 删除人ID
     * @return 标记为已删除的文档实例
     */
    public Document markAsDeleted(Long deletedBy) {
        return new Document(
                this.id, this.tenantId, this.title, this.sourceType, this.sourceUri,
                this.mimeType, this.langCode, this.parsingStatus, this.chunkCount, this.sha256,
                this.createdBy, deletedBy, this.createdAt, OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    /**
     * 检查文档是否已被删除
     *
     * @return true如果文档已被软删除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 检查文档是否解析成功
     *
     * @return true如果文档已成功解析
     */
    public boolean isParsed() {
        return this.parsingStatus == ParsingStatus.SUCCESS;
    }
}