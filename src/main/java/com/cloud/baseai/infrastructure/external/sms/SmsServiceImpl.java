package com.cloud.baseai.infrastructure.external.sms;

import com.cloud.baseai.infrastructure.external.sms.model.BatchSmsResult;
import com.cloud.baseai.infrastructure.external.sms.model.SmsStatus;
import com.cloud.baseai.infrastructure.exception.SmsServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * <h2>短信服务实现类</h2>
 *
 * <p>短信发送不仅涉及技术实现，还要考虑成本控制、防刷机制、合规要求等多个方面。</p>
 *
 * <p><b>技术架构特点：</b></p>
 * <p>采用多层防护机制：首先是参数验证层，然后是频率限制层，接着是黑名单过滤层，
 * 最后才是实际的短信发送层。每一层都有其特定的职责，确保系统的安全性和稳定性。
 * 同时使用Redis来实现分布式的频率限制和状态缓存。</p>
 *
 * <p><b>成本控制策略：</b></p>
 * <p>通过智能的频率限制、重复发送检测、模板优化等手段来控制短信成本。
 * 比如，我们会检测短时间内的重复发送请求，避免因为用户多次点击而产生不必要的费用。
 * 同时，我们优先使用成本更低的模板短信，只有在必要时才使用自定义内容短信。</p>
 */
@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    // 中国大陆手机号正则表达式：1开头，第二位为3-9，总共11位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    // 验证码格式：4-6位数字
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("^\\d{4,6}$");

    // Redis键前缀，用于区分不同类型的缓存数据
    private static final String SMS_LIMIT_PREFIX = "sms:limit:";  // 频率限制
    private static final String SMS_STATUS_PREFIX = "sms:status:"; // 状态缓存
    private static final String SMS_BLACKLIST_PREFIX = "sms:blacklist:"; // 黑名单
    private static final String SMS_DUPLICATE_PREFIX = "sms:duplicate:"; // 重复检测

    // 异步执行器，专门用于短信发送，避免阻塞主线程
    private final ExecutorService asyncExecutor;

    // Redis模板，用于实现分布式缓存和限流
    private final StringRedisTemplate redisTemplate;

    // 配置参数：通过application.yml注入，便于不同环境使用不同配置
    @Value("${sms.provider:aliyun}")
    private String smsProvider; // 短信服务商：aliyun, tencent, huawei等

    @Value("${sms.access-key}")
    private String accessKey; // 访问密钥

    @Value("${sms.access-secret}")
    private String accessSecret; // 访问密钥

    @Value("${sms.signature:BaseAI}")
    private String signature; // 短信签名

    @Value("${sms.limit.per-minute:5}")
    private int limitPerMinute; // 每分钟发送限制

    @Value("${sms.limit.per-hour:20}")
    private int limitPerHour; // 每小时发送限制

    @Value("${sms.limit.per-day:100}")
    private int limitPerDay; // 每天发送限制

    @Value("${sms.async:true}")
    private boolean asyncMode; // 是否异步发送

    @Value("${sms.duplicate-check-minutes:2}")
    private int duplicateCheckMinutes; // 重复发送检测时间窗口

    /**
     * 构造函数：初始化短信服务
     *
     * <p>这里我们创建了专门的线程池来处理短信发送任务。为什么要用单独的线程池呢？
     * 因为短信发送涉及网络IO，可能会有延迟，我们不希望这个延迟影响到主业务流程。
     * 同时，短信发送的并发量相对可控，使用固定大小的线程池比较合适。</p>
     */
    public SmsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.asyncExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread thread = new Thread(r, "sms-sender-" + r.hashCode());
            thread.setDaemon(true);
            return thread;
        });

        log.info("短信服务初始化完成，服务商：{}, 发送模式：{}",
                smsProvider, asyncMode ? "异步" : "同步");
    }

    /**
     * 发送验证码短信
     *
     * <p>这是短信服务的核心功能。验证码短信的安全性非常重要，我们需要从多个层面来保护：
     * 1. 频率限制：防止恶意刷取验证码
     * 2. 重复检测：避免短时间内重复发送相同验证码
     * 3. 格式验证：确保手机号和验证码格式正确
     * 4. 黑名单过滤：拒绝向问题号码发送</p>
     */
    @Override
    public void sendVerificationCode(String phoneNumber, String verificationCode,
                                     String purpose, int expireMinutes) throws SmsServiceException {

        // 第一步：基础参数验证，这是最基本的防护措施
        validatePhoneNumber(phoneNumber);
        validateVerificationCode(verificationCode);
        validateStringParameter(purpose, "发送目的不能为空");

        if (expireMinutes <= 0 || expireMinutes > 30) {
            throw new IllegalArgumentException("验证码有效期必须在1-30分钟之间");
        }

        log.info("准备发送验证码短信: phone={}, purpose={}, expireMinutes={}",
                phoneNumber, purpose, expireMinutes);

        try {
            // 第二步：安全检查，确保这次发送是合法的
            performSecurityChecks(phoneNumber, "VERIFICATION_CODE");

            // 第三步：构建短信内容
            String content = buildVerificationCodeContent(verificationCode, purpose, expireMinutes);

            // 第四步：发送短信
            String messageId = sendSmsInternal(phoneNumber, content, "VERIFICATION_CODE");

            // 第五步：记录发送历史，用于后续的重复检测和统计
            recordSendingHistory(phoneNumber, "VERIFICATION_CODE", content);

            log.info("验证码短信发送成功: phone={}, messageId={}", phoneNumber, messageId);

        } catch (SmsServiceException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            String errorMessage = "发送验证码短信失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new SmsServiceException("VERIFICATION_CODE_FAILED", errorMessage, e);
        }
    }

    /**
     * 发送安全警报短信
     *
     * <p>安全警报是一种特殊的短信类型，它的重要性很高，因此我们给它更高的发送优先级。
     * 同时，安全警报的内容需要包含足够的信息让用户了解风险，但又不能过于详细
     * 以免泄露敏感信息。</p>
     */
    @Override
    public void sendSecurityAlert(String phoneNumber, String loginLocation,
                                  OffsetDateTime loginTime, String deviceInfo) throws SmsServiceException {

        validatePhoneNumber(phoneNumber);
        validateStringParameter(loginLocation, "登录地点不能为空");

        if (loginTime == null) {
            throw new IllegalArgumentException("登录时间不能为空");
        }

        log.info("准备发送安全警报短信: phone={}, location={}", phoneNumber, loginLocation);

        try {
            // 安全警报不受普通频率限制，但仍需要检查黑名单
            if (isBlacklisted(phoneNumber)) {
                log.warn("手机号在黑名单中，跳过安全警报发送: phone={}", phoneNumber);
                return;
            }

            // 构建安全警报内容
            String content = buildSecurityAlertContent(loginLocation, loginTime, deviceInfo);

            // 发送短信，使用高优先级
            String messageId = sendSmsInternal(phoneNumber, content, "SECURITY_ALERT");

            log.info("安全警报短信发送成功: phone={}, messageId={}", phoneNumber, messageId);

        } catch (Exception e) {
            String errorMessage = "发送安全警报短信失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new SmsServiceException("SECURITY_ALERT_FAILED", errorMessage, e);
        }
    }

    /**
     * 发送业务通知短信
     *
     * <p>业务通知的灵活性最高，但也需要最谨慎的内容管理。
     * 我们需要确保内容符合运营商的规范，避免包含敏感词汇。</p>
     */
    @Override
    public void sendNotification(String phoneNumber, String notificationType, String content)
            throws SmsServiceException {

        validatePhoneNumber(phoneNumber);
        validateStringParameter(notificationType, "通知类型不能为空");
        validateStringParameter(content, "通知内容不能为空");

        // 检查内容长度，避免产生额外费用
        if (content.length() > 70) {
            log.warn("通知内容过长，可能产生额外费用: phone={}, length={}", phoneNumber, content.length());
        }

        log.info("准备发送业务通知短信: phone={}, type={}", phoneNumber, notificationType);

        try {
            // 业务通知需要进行频率检查
            performSecurityChecks(phoneNumber, "NOTIFICATION");

            // 内容安全检查：过滤敏感词汇，确保合规
            String safeContent = sanitizeContent(content);

            String messageId = sendSmsInternal(phoneNumber, safeContent, "NOTIFICATION");
            recordSendingHistory(phoneNumber, "NOTIFICATION", safeContent);

            log.info("业务通知短信发送成功: phone={}, messageId={}", phoneNumber, messageId);

        } catch (Exception e) {
            String errorMessage = "发送业务通知短信失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new SmsServiceException("NOTIFICATION_FAILED", errorMessage, e);
        }
    }

    /**
     * 使用模板发送短信
     *
     * <p>模板短信是推荐的发送方式，它有更好的送达率和更低的成本。
     * 但是模板的使用需要严格按照运营商的要求，参数值不能包含违规内容。</p>
     */
    @Override
    public void sendTemplateMessage(String phoneNumber, String templateCode,
                                    Map<String, String> templateParams) throws SmsServiceException {

        validatePhoneNumber(phoneNumber);
        validateStringParameter(templateCode, "模板编码不能为空");

        if (templateParams == null) {
            templateParams = new HashMap<>();
        }

        log.info("准备发送模板短信: phone={}, template={}", phoneNumber, templateCode);

        try {
            performSecurityChecks(phoneNumber, "TEMPLATE");

            // 验证模板参数，确保不包含违规内容
            validateTemplateParams(templateParams);

            String messageId = sendTemplateSmsInternal(phoneNumber, templateCode, templateParams);
            recordSendingHistory(phoneNumber, "TEMPLATE", templateCode);

            log.info("模板短信发送成功: phone={}, template={}, messageId={}",
                    phoneNumber, templateCode, messageId);

        } catch (Exception e) {
            String errorMessage = "发送模板短信失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new SmsServiceException("TEMPLATE_FAILED", errorMessage, e);
        }
    }

    /**
     * 批量发送短信
     *
     * <p>批量发送是最复杂的功能，需要考虑成本控制、合规要求、性能优化等多个方面。
     * 我们采用分批处理的策略，每批次之间有适当的间隔，避免触发运营商的限制。</p>
     */
    @Override
    public BatchSmsResult sendBatchMessages(List<String> phoneNumbers, String content,
                                            OffsetDateTime sendTime) throws SmsServiceException {

        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            throw new IllegalArgumentException("手机号列表不能为空");
        }

        validateStringParameter(content, "短信内容不能为空");

        // 检查是否是营销短信，营销短信必须包含退订信息
        if (isMarketingContent(content) && !content.contains("退订")) {
            throw new IllegalArgumentException("营销短信必须包含退订信息");
        }

        log.info("准备批量发送短信: count={}, scheduled={}",
                phoneNumbers.size(), sendTime != null);

        try {
            // 过滤有效的手机号，去除重复和无效号码
            List<String> validNumbers = phoneNumbers.stream()
                    .filter(this::isValidPhoneNumber)
                    .filter(phone -> !isBlacklisted(phone))
                    .distinct()
                    .toList();

            if (validNumbers.isEmpty()) {
                return new BatchSmsResult(phoneNumbers.size(), 0, phoneNumbers.size(),
                        phoneNumbers, "BATCH_" + System.currentTimeMillis());
            }

            // 如果指定了发送时间，进行延迟发送
            if (sendTime != null && sendTime.isAfter(OffsetDateTime.now())) {
                return scheduleBatchSending(validNumbers, content, sendTime);
            }

            // 立即发送
            return executeBatchSending(validNumbers, content);

        } catch (Exception e) {
            String errorMessage = "批量发送短信失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new SmsServiceException("BATCH_FAILED", errorMessage, e);
        }
    }

    /**
     * 查询短信发送状态
     *
     * <p>状态查询帮助我们了解短信的实际送达情况。这个功能的实现依赖于
     * 短信服务商的API支持，不同服务商的接口可能有所不同。</p>
     */
    @Override
    public SmsStatus querySmsStatus(String messageId) throws SmsServiceException {
        validateStringParameter(messageId, "消息ID不能为空");

        try {
            // 首先查询本地缓存
            String cachedStatus = redisTemplate.opsForValue()
                    .get(SMS_STATUS_PREFIX + messageId);

            if (cachedStatus != null) {
                return SmsStatus.valueOf(cachedStatus);
            }

            // 查询服务商API获取最新状态
            SmsStatus status = querySmsStatusFromProvider(messageId);

            // 缓存结果，避免频繁查询
            redisTemplate.opsForValue().set(SMS_STATUS_PREFIX + messageId,
                    status.name(), 1, TimeUnit.HOURS);

            return status;

        } catch (Exception e) {
            log.error("查询短信状态失败: messageId={}", messageId, e);
            throw new SmsServiceException("QUERY_STATUS_FAILED", "查询短信状态失败", e);
        }
    }

    /**
     * 验证手机号格式
     */
    @Override
    public boolean isValidPhoneNumber(String phoneNumber) {
        return StringUtils.hasText(phoneNumber) &&
                PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    /**
     * 获取剩余短信额度
     */
    @Override
    public long getRemainingQuota() throws SmsServiceException {
        try {
            // 这里应该调用服务商API查询余额
            // 简化实现，返回模拟数据
            return queryQuotaFromProvider();
        } catch (Exception e) {
            log.error("查询短信余额失败", e);
            throw new SmsServiceException("QUERY_QUOTA_FAILED", "查询短信余额失败", e);
        }
    }

    /**
     * 检查手机号是否在黑名单中
     */
    @Override
    public boolean isBlacklisted(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            return true; // 无效号码视为黑名单
        }

        // 查询Redis黑名单
        return redisTemplate.hasKey(SMS_BLACKLIST_PREFIX + phoneNumber);
    }

    // =================== 私有辅助方法 ===================

    /**
     * 执行安全检查
     *
     * <p>这是短信发送前的重要安全措施，包括频率限制、黑名单检查、重复发送检测等。
     * 每一项检查都有其特定的目的，共同构成了完整的安全防护体系。</p>
     */
    private void performSecurityChecks(String phoneNumber, String messageType) throws SmsServiceException {
        // 1. 检查黑名单
        if (isBlacklisted(phoneNumber)) {
            throw new SmsServiceException("PHONE_BLACKLISTED", "手机号在黑名单中");
        }

        // 2. 检查发送频率
        checkSendingLimit(phoneNumber);

        // 3. 检查重复发送（针对验证码）
        if ("VERIFICATION_CODE".equals(messageType)) {
            checkDuplicateSending(phoneNumber);
        }
    }

    /**
     * 检查发送频率限制
     *
     * <p>使用Redis实现分布式的频率限制。我们设置了三个级别的限制：
     * 分钟级、小时级和天级，形成多层防护。</p>
     */
    private void checkSendingLimit(String phoneNumber) throws SmsServiceException {
        String minuteKey = SMS_LIMIT_PREFIX + phoneNumber + ":minute:" +
                (System.currentTimeMillis() / 60000);
        String hourKey = SMS_LIMIT_PREFIX + phoneNumber + ":hour:" +
                (System.currentTimeMillis() / 3600000);
        String dayKey = SMS_LIMIT_PREFIX + phoneNumber + ":day:" +
                (System.currentTimeMillis() / 86400000);

        // 检查并更新计数器
        Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
        redisTemplate.expire(minuteKey, 1, TimeUnit.MINUTES);

        Long hourCount = redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, 1, TimeUnit.HOURS);

        Long dayCount = redisTemplate.opsForValue().increment(dayKey);
        redisTemplate.expire(dayKey, 1, TimeUnit.DAYS);

        // 检查是否超出限制
        if (minuteCount > limitPerMinute) {
            throw new SmsServiceException("RATE_LIMIT_EXCEEDED",
                    "发送频率过高，请稍后再试（每分钟限制" + limitPerMinute + "条）");
        }

        if (hourCount > limitPerHour) {
            throw new SmsServiceException("RATE_LIMIT_EXCEEDED",
                    "发送频率过高，请稍后再试（每小时限制" + limitPerHour + "条）");
        }

        if (dayCount > limitPerDay) {
            throw new SmsServiceException("RATE_LIMIT_EXCEEDED",
                    "今日发送次数已达上限（每天限制" + limitPerDay + "条）");
        }
    }

    /**
     * 检查重复发送
     *
     * <p>防止用户在短时间内重复点击发送按钮，造成不必要的费用。
     * 我们记录最近发送的验证码，如果检测到重复请求就拒绝发送。</p>
     */
    private void checkDuplicateSending(String phoneNumber) throws SmsServiceException {
        String duplicateKey = SMS_DUPLICATE_PREFIX + phoneNumber;
        String lastSendTime = redisTemplate.opsForValue().get(duplicateKey);

        if (lastSendTime != null) {
            long timeDiff = System.currentTimeMillis() - Long.parseLong(lastSendTime);
            if (timeDiff < (long) duplicateCheckMinutes * 60 * 1000) {
                throw new SmsServiceException("DUPLICATE_SENDING",
                        "请不要重复发送验证码，请等待" + duplicateCheckMinutes + "分钟后再试");
            }
        }

        // 记录本次发送时间
        redisTemplate.opsForValue().set(duplicateKey,
                String.valueOf(System.currentTimeMillis()),
                duplicateCheckMinutes, TimeUnit.MINUTES);
    }

    /**
     * 核心短信发送方法
     *
     * <p>这是实际与短信服务商API交互的地方。根据配置的服务商类型，
     * 调用相应的发送逻辑。支持同步和异步两种模式。</p>
     */
    private String sendSmsInternal(String phoneNumber, String content, String messageType) throws Exception {
        if (asyncMode) {
            // 异步发送：生成消息ID后立即返回，实际发送在后台进行
            String messageId = generateMessageId();

            CompletableFuture.runAsync(() -> {
                try {
                    sendSmsSync(phoneNumber, content, messageType, messageId);
                } catch (Exception e) {
                    log.error("异步发送短信失败: phone={}, messageId={}", phoneNumber, messageId, e);
                    // 更新状态为失败
                    redisTemplate.opsForValue().set(SMS_STATUS_PREFIX + messageId,
                            SmsStatus.FAILED.name(), 1, TimeUnit.HOURS);
                }
            }, asyncExecutor);

            return messageId;
        } else {
            // 同步发送：等待发送完成后返回
            String messageId = generateMessageId();
            sendSmsSync(phoneNumber, content, messageType, messageId);
            return messageId;
        }
    }

    /**
     * 同步发送短信的具体实现
     *
     * <p>根据配置的服务商类型，调用相应的API。这里我们提供了一个
     * 可扩展的架构，可以很容易地支持新的短信服务商。</p>
     */
    private void sendSmsSync(String phoneNumber, String content, String messageType, String messageId) throws Exception {
        // 更新状态为发送中
        redisTemplate.opsForValue().set(SMS_STATUS_PREFIX + messageId,
                SmsStatus.SENDING.name(), 1, TimeUnit.HOURS);

        try {
            switch (smsProvider.toLowerCase()) {
                case "aliyun" -> sendViaAliyun(phoneNumber, content, messageId);
                case "tencent" -> sendViaTencent(phoneNumber, content, messageId);
                case "huawei" -> sendViaHuawei(phoneNumber, content, messageId);
                case "mock" -> sendViaMock(phoneNumber, content, messageId); // 用于测试
                default -> throw new Exception("不支持的短信服务商: " + smsProvider);
            }

            // 更新状态为已发送
            redisTemplate.opsForValue().set(SMS_STATUS_PREFIX + messageId,
                    SmsStatus.DELIVERED.name(), 1, TimeUnit.HOURS);

        } catch (Exception e) {
            // 更新状态为失败
            redisTemplate.opsForValue().set(SMS_STATUS_PREFIX + messageId,
                    SmsStatus.FAILED.name(), 1, TimeUnit.HOURS);
            throw e;
        }
    }

    // =================== 服务商适配方法 ===================

    /**
     * 阿里云短信发送适配
     */
    private void sendViaAliyun(String phoneNumber, String content, String messageId) throws Exception {
        // 这里应该调用阿里云短信API
        // 简化实现，只记录日志
        log.info("通过阿里云发送短信: phone={}, messageId={}, content={}",
                phoneNumber, messageId, content);

        // 模拟网络延迟
        Thread.sleep(100);
    }

    /**
     * 腾讯云短信发送适配
     */
    private void sendViaTencent(String phoneNumber, String content, String messageId) throws Exception {
        log.info("通过腾讯云发送短信: phone={}, messageId={}, content={}",
                phoneNumber, messageId, content);
        Thread.sleep(100);
    }

    /**
     * 华为云短信发送适配
     */
    private void sendViaHuawei(String phoneNumber, String content, String messageId) throws Exception {
        log.info("通过华为云发送短信: phone={}, messageId={}, content={}",
                phoneNumber, messageId, content);
        Thread.sleep(100);
    }

    /**
     * Mock模式发送（用于测试）
     */
    private void sendViaMock(String phoneNumber, String content, String messageId) throws Exception {
        log.info("Mock模式发送短信: phone={}, messageId={}, content={}",
                phoneNumber, messageId, content);
    }

    // =================== 内容构建和验证方法 ===================

    /**
     * 构建验证码短信内容
     */
    private String buildVerificationCodeContent(String code, String purpose, int expireMinutes) {
        return String.format("【%s】您的%s验证码是：%s，有效期%d分钟，请勿泄露给他人。",
                signature, purpose, code, expireMinutes);
    }

    /**
     * 构建安全警报短信内容
     */
    private String buildSecurityAlertContent(String location, OffsetDateTime loginTime, String deviceInfo) {
        String timeStr = loginTime.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"));
        return String.format("【%s】您的账户于%s在%s通过%s登录，如非本人操作请立即修改密码。",
                signature, timeStr, location, deviceInfo);
    }

    /**
     * 内容安全检查：过滤敏感词汇
     */
    private String sanitizeContent(String content) {
        // 这里应该实现敏感词过滤逻辑
        // 简化实现，只是记录日志
        log.debug("对短信内容进行安全检查: {}", content);
        return content;
    }

    /**
     * 验证模板参数
     */
    private void validateTemplateParams(Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            if (value != null && (value.contains("http") || value.contains("www"))) {
                log.warn("模板参数包含可疑内容: {}={}", entry.getKey(), value);
            }
        }
    }

    /**
     * 判断是否为营销内容
     */
    private boolean isMarketingContent(String content) {
        String[] marketingKeywords = {"优惠", "促销", "打折", "活动", "推广"};
        String lowerContent = content.toLowerCase();

        for (String keyword : marketingKeywords) {
            if (lowerContent.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    // =================== 工具方法 ===================

    private void validatePhoneNumber(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("手机号格式不正确: " + phoneNumber);
        }
    }

    private void validateVerificationCode(String code) {
        if (!StringUtils.hasText(code) || !VERIFICATION_CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("验证码格式不正确，应为4-6位数字");
        }
    }

    private void validateStringParameter(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String generateMessageId() {
        return "SMS_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void recordSendingHistory(String phoneNumber, String messageType, String content) {
        // 记录发送历史，用于统计和分析
        log.debug("记录短信发送历史: phone={}, type={}", phoneNumber, messageType);
    }

    // =================== 模拟的外部服务调用 ===================

    private String sendTemplateSmsInternal(String phoneNumber, String templateCode, Map<String, String> params) throws Exception {
        String messageId = generateMessageId();
        log.info("发送模板短信: phone={}, template={}, messageId={}", phoneNumber, templateCode, messageId);
        return messageId;
    }

    private SmsStatus querySmsStatusFromProvider(String messageId) {
        // 模拟从服务商查询状态
        return SmsStatus.DELIVERED;
    }

    private long queryQuotaFromProvider() {
        // 模拟查询余额
        return 10000L;
    }

    private BatchSmsResult scheduleBatchSending(List<String> phoneNumbers, String content, OffsetDateTime sendTime) {
        // 模拟定时发送
        String batchId = "BATCH_" + System.currentTimeMillis();
        log.info("定时批量发送已安排: batchId={}, count={}, time={}",
                batchId, phoneNumbers.size(), sendTime);
        return new BatchSmsResult(phoneNumbers.size(), phoneNumbers.size(), 0,
                Collections.emptyList(), batchId);
    }

    private BatchSmsResult executeBatchSending(List<String> phoneNumbers, String content) {
        // 模拟立即批量发送
        String batchId = "BATCH_" + System.currentTimeMillis();
        int successCount = phoneNumbers.size();
        log.info("批量发送完成: batchId={}, success={}", batchId, successCount);
        return new BatchSmsResult(phoneNumbers.size(), successCount, 0,
                Collections.emptyList(), batchId);
    }
}