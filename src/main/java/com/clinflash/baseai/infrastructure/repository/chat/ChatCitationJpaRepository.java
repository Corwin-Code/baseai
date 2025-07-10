package com.clinflash.baseai.infrastructure.repository.chat;

import com.clinflash.baseai.domain.chat.model.ChatCitation;
import com.clinflash.baseai.domain.chat.repository.ChatCitationRepository;
import com.clinflash.baseai.infrastructure.persistence.chat.entity.ChatCitationEntity;
import com.clinflash.baseai.infrastructure.persistence.chat.mapper.ChatMapper;
import com.clinflash.baseai.infrastructure.repository.chat.spring.SpringChatCitationRepo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>对话引用仓储实现</h2>
 *
 * <p>这个仓储管理着AI系统中最重要的可信度证据：引用关系。每当AI给出回答时，
 * 系统会记录这个回答基于哪些知识库内容，这就像学术论文中的参考文献一样，
 * 让每个AI回答都有据可查。</p>
 *
 * <p><b>RAG系统的核心价值：</b></p>
 * <p>RAG(Retrieval-Augmented Generation)不仅仅是检索和生成的组合，更重要的是
 * 建立可追溯的知识链条。通过引用系统，用户可以验证AI回答的准确性，
 * 开发者可以分析知识库的使用效果，管理员可以优化内容质量。</p>
 *
 * <p><b>设计考量：</b></p>
 * <p>引用关系使用复合主键设计(messageId + chunkId + modelCode)，这确保了：</p>
 * <ul>
 * <li>同一条消息可以引用多个知识块</li>
 * <li>同一个知识块可以被多条消息引用</li>
 * <li>不同模型的检索结果可以并存比较</li>
 * <li>避免重复引用关系的产生</li>
 * </ul>
 */
@Repository
public class ChatCitationJpaRepository implements ChatCitationRepository {

    private final SpringChatCitationRepo springRepo;
    private final ChatMapper mapper;

    /**
     * 构造函数注入
     *
     * <p>依赖注入的好处在于测试友好性和松耦合。在单元测试中，
     * 我们可以轻松地注入Mock对象来验证仓储的行为。</p>
     */
    public ChatCitationJpaRepository(SpringChatCitationRepo springRepo, ChatMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public ChatCitation save(ChatCitation citation) {
        ChatCitationEntity entity = mapper.toEntity(citation);
        ChatCitationEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ChatCitation> findByMessageId(Long messageId) {
        // 获取一条消息的所有引用，按相关性分数降序排列
        // 这样最相关的知识内容会排在前面，有助于用户理解AI回答的主要依据
        List<ChatCitationEntity> entities = springRepo.findByMessageIdOrderByScoreDesc(messageId);
        return mapper.toCitationDomainList(entities);
    }

    @Override
    public void deleteByThreadId(Long threadId) {
        // 当删除对话线程时，需要清理所有相关的引用关系
        // 这确保了数据的一致性，避免孤儿引用的产生
        springRepo.deleteByThreadId(threadId);
    }

    /**
     * 根据知识块ID查找所有引用它的消息
     *
     * <p>这个方法虽然不在接口中定义，但对于知识库管理很有用。
     * 当需要更新或删除某个知识块时，可以先查看哪些对话引用了它。</p>
     */
    public List<ChatCitation> findByChunkId(Long chunkId) {
        List<ChatCitationEntity> entities = springRepo.findByChunkId(chunkId);
        return mapper.toCitationDomainList(entities);
    }

    /**
     * 获取最常被引用的知识块
     *
     * <p>这个统计信息可以帮助内容管理者了解哪些知识最有价值，
     * 指导知识库的优化和扩展方向。</p>
     */
    public List<ChunkCitationStats> getMostCitedChunks(int limit) {
        return springRepo.findMostCitedChunks(limit);
    }

    /**
     * 获取引用质量统计
     *
     * <p>通过分析引用的相关性分数分布，可以评估检索系统的效果。
     * 如果低分引用过多，说明检索算法需要调优。</p>
     */
    public CitationQualityStats getCitationQualityStats(Long tenantId) {
        return springRepo.getCitationQualityStatsByTenantId(tenantId);
    }

    /**
     * 知识块引用统计
     */
    public record ChunkCitationStats(
            Long chunkId,
            Long citationCount,
            Double averageScore
    ) {
    }

    /**
     * 引用质量统计
     */
    public record CitationQualityStats(
            Long totalCitations,
            Double averageScore,
            Long highQualityCitations,  // 分数 >= 0.8 的引用数
            Long mediumQualityCitations, // 分数 0.5-0.8 的引用数
            Long lowQualityCitations    // 分数 < 0.5 的引用数
    ) {
    }
}