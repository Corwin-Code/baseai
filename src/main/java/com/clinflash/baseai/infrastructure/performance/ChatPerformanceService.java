package com.clinflash.baseai.infrastructure.performance;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * <h2>对话性能优化服务</h2>
 *
 * <p>性能优化是企业级应用的重要课题。这个服务提供了多种优化策略，
 * 包括智能缓存、请求去重、资源预热等功能，确保系统在高负载下的稳定运行。</p>
 */
@Service
public class ChatPerformanceService {

    private final ConcurrentHashMap<String, CacheEntry> responseCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> requestDeduplication = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);

    public ChatPerformanceService() {
        // 定期清理缓存
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 智能响应缓存
     *
     * <p>对于相同或相似的问题，缓存AI的回答可以显著提升响应速度。
     * 这个方法实现了基于内容哈希的智能缓存机制。</p>
     */
    @Cacheable(value = "chatResponses", key = "#contentHash")
    public String getCachedResponse(String contentHash) {
        CacheEntry entry = responseCache.get(contentHash);
        if (entry != null && !entry.isExpired()) {
            return entry.content;
        }
        return null;
    }

    /**
     * 缓存响应内容
     */
    public void cacheResponse(String contentHash, String response, Duration ttl) {
        LocalDateTime expiresAt = LocalDateTime.now().plus(ttl);
        responseCache.put(contentHash, new CacheEntry(response, expiresAt));
    }

    /**
     * 请求去重
     *
     * <p>在高并发场景下，可能会有重复的请求同时进入系统。
     * 这个方法可以检测并合并重复请求，避免资源浪费。</p>
     */
    public boolean isDuplicateRequest(String requestKey, long timeWindowMs) {
        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = requestDeduplication.get(requestKey);

        if (lastRequestTime != null && (currentTime - lastRequestTime) < timeWindowMs) {
            return true; // 重复请求
        }

        requestDeduplication.put(requestKey, currentTime);
        return false;
    }

    /**
     * 清理过期缓存条目
     */
    @CacheEvict(value = "chatResponses", allEntries = true)
    public void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        responseCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));

        // 清理请求去重缓存（保留最近1小时的记录）
        long oneHourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        requestDeduplication.entrySet().removeIf(entry -> entry.getValue() < oneHourAgo);
    }

    private record CacheEntry(String content, LocalDateTime expiresAt) {
        boolean isExpired() {
            return isExpired(LocalDateTime.now());
        }

        boolean isExpired(LocalDateTime now) {
            return now.isAfter(expiresAt);
        }
    }
}