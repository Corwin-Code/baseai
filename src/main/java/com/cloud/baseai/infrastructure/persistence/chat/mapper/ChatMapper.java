package com.cloud.baseai.infrastructure.persistence.chat.mapper;

import com.cloud.baseai.domain.chat.model.ChatCitation;
import com.cloud.baseai.domain.chat.model.ChatMessage;
import com.cloud.baseai.domain.chat.model.ChatThread;
import com.cloud.baseai.domain.chat.model.ChatUsageDaily;
import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatCitationEntity;
import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatMessageEntity;
import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatThreadEntity;
import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatUsageDailyEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>对话模块映射器</h2>
 */
@Component
public class ChatMapper {

    // =================== ChatThread 映射 ===================

    public ChatThreadEntity toEntity(ChatThread domain) {
        return ChatThreadEntity.fromDomain(domain);
    }

    public ChatThread toDomain(ChatThreadEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<ChatThreadEntity> toThreadEntityList(List<ChatThread> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<ChatThread> toThreadDomainList(List<ChatThreadEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== ChatMessage 映射 ===================

    public ChatMessageEntity toEntity(ChatMessage domain) {
        return ChatMessageEntity.fromDomain(domain);
    }

    public ChatMessage toDomain(ChatMessageEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<ChatMessageEntity> toMessageEntityList(List<ChatMessage> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<ChatMessage> toMessageDomainList(List<ChatMessageEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== ChatCitation 映射 ===================

    public ChatCitationEntity toEntity(ChatCitation domain) {
        return ChatCitationEntity.fromDomain(domain);
    }

    public ChatCitation toDomain(ChatCitationEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<ChatCitationEntity> toCitationEntityList(List<ChatCitation> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<ChatCitation> toCitationDomainList(List<ChatCitationEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== ChatUsageDaily 映射 ===================

    public ChatUsageDailyEntity toEntity(ChatUsageDaily domain) {
        return ChatUsageDailyEntity.fromDomain(domain);
    }

    public ChatUsageDaily toDomain(ChatUsageDailyEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<ChatUsageDailyEntity> toUsageEntityList(List<ChatUsageDaily> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<ChatUsageDaily> toUsageDomainList(List<ChatUsageDailyEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}