package com.cloud.baseai.infrastructure.external.email;

import com.cloud.baseai.infrastructure.exception.EmailException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.email.model.BatchEmailResult;
import com.cloud.baseai.infrastructure.external.email.model.EmailStatus;
import com.cloud.baseai.infrastructure.i18n.MessageManager;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * <h2>邮件服务实现类</h2>
 *
 * <p>这是邮件服务的具体实现，就像一个现代化的邮政处理中心。它负责接收各种类型的邮件请求，
 * 进行必要的预处理（如格式验证、模板渲染等），然后通过底层的邮件发送服务将邮件投递出去。</p>
 *
 * <p><b>架构设计思路：</b></p>
 * <p>这个实现类采用了分层处理的方式：首先进行参数验证，然后构建邮件内容，
 * 最后调用底层的邮件发送服务。每一层都有明确的职责，便于维护和扩展。
 * 同时支持同步和异步两种发送模式，根据业务需求灵活选择。</p>
 *
 * <p><b>技术特性：</b></p>
 * <ul>
 * <li><b>多服务商支持：</b>可以轻松切换不同的邮件服务提供商</li>
 * <li><b>模板系统：</b>支持HTML模板和动态参数替换</li>
 * <li><b>异步处理：</b>避免阻塞主业务流程</li>
 * <li><b>错误处理：</b>完善的异常处理和重试机制</li>
 * <li><b>性能优化：</b>连接池和批量发送优化</li>
 * </ul>
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    // 邮箱格式验证的正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Spring Boot的邮件发送器，这是我们与底层邮件服务通信的桥梁
    private final JavaMailSender mailSender;

    // 异步执行器，用于非阻塞的邮件发送
    private final ExecutorService asyncExecutor;

    // 模板引擎，用于渲染邮件模板
    @Autowired(required = false)
    private EmailTemplateEngine templateEngine;

    // 配置参数：这些参数通过application.yml配置文件注入
    @Value("${email.from:noreply@baseai.com}")
    private String fromEmail;

    @Value("${email.from-name:BaseAI System}")
    private String fromName;

    @Value("${email.base-url:https://app.baseai.com}")
    private String baseUrl;

    @Value("${email.async:true}")
    private boolean asyncMode;

    @Value("${email.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${email.batch.size:50}")
    private int batchSize;

    /**
     * 构造函数：依赖注入邮件发送器
     *
     * <p>这里使用了Spring的依赖注入，让Spring容器为我们管理JavaMailSender的生命周期。
     * 同时创建了一个固定大小的线程池用于异步邮件发送。</p>
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        // 创建一个专门用于邮件发送的线程池，避免影响主业务线程
        this.asyncExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread thread = new Thread(r, "email-sender-" + r.hashCode());
            thread.setDaemon(true); // 设置为守护线程，JVM关闭时自动结束
            return thread;
        });

        log.info("邮件服务初始化完成，发送模式：{}", asyncMode ? "异步" : "同步");
    }

    /**
     * 发送用户激活邮件
     *
     * <p>这是新用户注册后的第一封邮件，非常重要。我们需要确保邮件内容友好、
     * 操作指引清晰，让用户能够顺利完成账户激活。</p>
     */
    @Override
    public void sendActivationEmail(String email, String username, String activationCode)
            throws EmailException {

        // 第一步：参数验证，确保传入的数据是有效的
        validateEmailParameters(email, MessageManager.getMessage(ErrorCode.EXT_EMAIL_001));
        validateStringParameter(username, MessageManager.getMessage(ErrorCode.EXT_EMAIL_002));
        validateStringParameter(activationCode, MessageManager.getMessage(ErrorCode.EXT_EMAIL_005));

        log.info("准备发送激活邮件: email={}, username={}", email, username);

        try {
            // 第二步：构建激活链接
            String activationUrl = buildActivationUrl(activationCode);

            // 第三步：准备邮件内容
            String subject = "欢迎加入 BaseAI - 请激活您的账户";

            // 如果有模板引擎，使用模板；否则使用简单的文本邮件
            if (templateEngine != null) {
                // 准备模板参数
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("username", username);
                templateParams.put("activationUrl", activationUrl);
                templateParams.put("baseUrl", baseUrl);

                // 使用模板发送邮件
                sendTemplateEmailInternal(email, "activation", templateParams, subject);
            } else {
                // 使用简单的文本邮件
                String content = buildActivationEmailContent(username, activationUrl);
                sendSimpleEmail(email, subject, content);
            }

            log.info("激活邮件发送成功: email={}", email);

        } catch (Exception e) {
            log.error("发送激活邮件失败: {}", e.getMessage(), e);
            throw EmailException.activationFailed(e.getMessage());
        }
    }

    /**
     * 发送组织邀请邮件
     *
     * <p>邀请邮件需要体现出邀请的正式性和专业性，同时包含足够的信息
     * 让受邀者了解邀请的详情和后续操作。</p>
     */
    @Override
    public void sendInvitationEmail(String email, String orgName, String invitationToken)
            throws EmailException {

        validateEmailParameters(email, MessageManager.getMessage(ErrorCode.EXT_EMAIL_003));
        validateStringParameter(orgName, MessageManager.getMessage(ErrorCode.EXT_EMAIL_004));
        validateStringParameter(invitationToken, MessageManager.getMessage(ErrorCode.EXT_EMAIL_006));

        log.info("准备发送邀请邮件: email={}, orgName={}", email, orgName);

        try {
            String invitationUrl = buildInvitationUrl(invitationToken);
            String subject = String.format("您被邀请加入 %s 组织", orgName);

            if (templateEngine != null) {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("orgName", orgName);
                templateParams.put("invitationUrl", invitationUrl);
                templateParams.put("baseUrl", baseUrl);

                sendTemplateEmailInternal(email, "invitation", templateParams, subject);
            } else {
                String content = buildInvitationEmailContent(orgName, invitationUrl);
                sendSimpleEmail(email, subject, content);
            }

            log.info("邀请邮件发送成功: email={}, orgName={}", email, orgName);

        } catch (Exception e) {
            log.error("发送邀请邮件失败: {}", e.getMessage(), e);
            throw EmailException.invitationFailed(e.getMessage());
        }
    }

    /**
     * 发送密码重置邮件
     *
     * <p>密码重置是一个安全敏感的操作，邮件内容需要包含适当的安全提醒，
     * 让用户了解这个操作的重要性。</p>
     */
    @Override
    public void sendPasswordResetEmail(String email, String username, String resetToken)
            throws EmailException {

        validateEmailParameters(email, MessageManager.getMessage(ErrorCode.EXT_EMAIL_001));
        validateStringParameter(username, MessageManager.getMessage(ErrorCode.EXT_EMAIL_002));
        validateStringParameter(resetToken, MessageManager.getMessage(ErrorCode.EXT_EMAIL_007));

        log.info("准备发送密码重置邮件: email={}, username={}", email, username);

        try {
            String resetUrl = buildPasswordResetUrl(resetToken);
            String subject = "BaseAI - 密码重置请求";

            if (templateEngine != null) {
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("username", username);
                templateParams.put("resetUrl", resetUrl);
                templateParams.put("baseUrl", baseUrl);

                sendTemplateEmailInternal(email, "password-reset", templateParams, subject);
            } else {
                String content = buildPasswordResetEmailContent(username, resetUrl);
                sendSimpleEmail(email, subject, content);
            }

            log.info("密码重置邮件发送成功: email={}", email);

        } catch (Exception e) {
            log.error("发送密码重置邮件失败: {}", e.getMessage(), e);
            throw EmailException.passwordResetFailed(e.getMessage());
        }
    }

    /**
     * 发送通知邮件
     *
     * <p>这是一个通用的邮件发送方法，适用于各种业务通知。
     * 内容由调用方决定，我们只负责发送。</p>
     */
    @Override
    public void sendNotificationEmail(String email, String subject, String content)
            throws EmailException {

        validateEmailParameters(email, MessageManager.getMessage(ErrorCode.EXT_EMAIL_001));
        validateStringParameter(subject, MessageManager.getMessage(ErrorCode.EXT_EMAIL_010));
        validateStringParameter(content, MessageManager.getMessage(ErrorCode.EXT_EMAIL_011));

        log.info("准备发送通知邮件: email={}, subject={}", email, subject);

        try {
            sendSimpleEmail(email, subject, content);
            log.info("通知邮件发送成功: email={}", email);

        } catch (Exception e) {
            log.error("发送通知邮件失败: {}", e.getMessage(), e);
            throw EmailException.notificationFailed(e.getMessage());
        }
    }

    /**
     * 使用模板发送邮件
     *
     * <p>这是最灵活的邮件发送方式，支持复杂的模板和动态内容。
     * 模板可以包含HTML、CSS样式等，提供更好的视觉效果。</p>
     */
    @Override
    public void sendTemplateEmail(String email, String templateName, Map<String, Object> templateParams)
            throws EmailException {

        validateEmailParameters(email, MessageManager.getMessage(ErrorCode.EXT_EMAIL_001));
        validateStringParameter(templateName, MessageManager.getMessage(ErrorCode.EXT_EMAIL_008));

        if (templateParams == null) {
            templateParams = new HashMap<>();
        }

        log.info("准备发送模板邮件: email={}, template={}", email, templateName);

        try {
            if (templateEngine == null) {
                throw EmailException.templateEngineUnavailable();
            }

            // 渲染模板内容
            String subject = templateEngine.renderSubject(templateName, templateParams);
            sendTemplateEmailInternal(email, templateName, templateParams, subject);

            log.info("模板邮件发送成功: email={}, template={}", email, templateName);

        } catch (Exception e) {
            log.error("发送模板邮件失败: {}", e.getMessage(), e);
            throw EmailException.templateSendFailed(templateName, e.getMessage());
        }
    }

    /**
     * 批量发送邮件
     *
     * <p>批量发送需要特别注意性能和错误处理。我们采用分批处理的方式，
     * 避免一次性处理过多邮件导致的性能问题。</p>
     */
    @Override
    public BatchEmailResult sendBatchEmails(List<String> emails, String subject, String content)
            throws EmailException {

        if (emails == null || emails.isEmpty()) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.EXT_EMAIL_009));
        }

        validateStringParameter(subject, MessageManager.getMessage(ErrorCode.EXT_EMAIL_010));
        validateStringParameter(content, MessageManager.getMessage(ErrorCode.EXT_EMAIL_011));

        log.info("准备批量发送邮件: count={}, subject={}", emails.size(), subject);

        // 过滤有效的邮箱地址
        List<String> validEmails = emails.stream()
                .filter(this::isValidEmail)
                .distinct() // 去重
                .toList();

        if (validEmails.isEmpty()) {
            return new BatchEmailResult(emails.size(), 0, emails.size(), emails);
        }

        List<String> failedEmails = new ArrayList<>();
        int successCount = 0;

        try {
            // 分批处理，避免一次性发送过多邮件
            for (int i = 0; i < validEmails.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, validEmails.size());
                List<String> batch = validEmails.subList(i, endIndex);

                // 处理当前批次
                for (String email : batch) {
                    try {
                        sendSimpleEmail(email, subject, content);
                        successCount++;

                        // 添加小延迟，避免触发邮件服务商的限流
                        Thread.sleep(100);

                    } catch (Exception e) {
                        log.warn("发送邮件失败: email={}, error={}", email, e.getMessage());
                        failedEmails.add(email);
                    }
                }

                log.info("批次处理完成: {}/{}", endIndex, validEmails.size());
            }

            int failureCount = validEmails.size() - successCount;
            log.info("批量邮件发送完成: total={}, success={}, failure={}",
                    validEmails.size(), successCount, failureCount);

            return new BatchEmailResult(validEmails.size(), successCount, failureCount, failedEmails);

        } catch (Exception e) {
            log.error("批量发送邮件失败: {}", e.getMessage(), e);
            throw EmailException.batchSendFailed(e.getMessage());
        }
    }

    /**
     * 验证邮箱地址格式
     *
     * <p>使用正则表达式验证邮箱格式。这个验证比较宽松，
     * 主要是为了过滤明显错误的格式。</p>
     */
    @Override
    public boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 获取邮件发送状态
     *
     * <p>这个功能的实现取决于底层邮件服务商的支持。
     * 目前提供了基础的框架，具体实现可以根据需要扩展。</p>
     */
    @Override
    public EmailStatus getEmailStatus(String messageId) throws EmailException {
        // 这里应该调用邮件服务商的API来查询状态
        // 目前返回一个默认状态，实际实现需要根据具体的邮件服务商API来完成
        log.debug("查询邮件状态: messageId={}", messageId);
        return EmailStatus.SENT; // 简化实现
    }

    // =================== 私有辅助方法 ===================

    /**
     * 发送简单文本邮件
     *
     * <p>这是最基础的邮件发送方法，适用于纯文本内容。
     * 根据配置决定是同步还是异步发送。</p>
     */
    private void sendSimpleEmail(String to, String subject, String content) throws Exception {
        if (asyncMode) {
            // 异步发送，不阻塞当前线程
            CompletableFuture.runAsync(() -> {
                try {
                    sendSimpleEmailSync(to, subject, content);
                } catch (Exception e) {
                    log.error("异步发送邮件失败: to={}, subject={}", to, subject, e);
                }
            }, asyncExecutor);
        } else {
            // 同步发送
            sendSimpleEmailSync(to, subject, content);
        }
    }

    /**
     * 同步发送简单邮件的核心实现
     */
    private void sendSimpleEmailSync(String to, String subject, String content) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(String.format("%s <%s>", fromName, fromEmail));
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        // 重试机制：如果发送失败，会自动重试
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                mailSender.send(message);
                log.debug("邮件发送成功 (第{}次尝试): to={}", attempt, to);
                return; // 发送成功，退出重试循环

            } catch (Exception e) {
                lastException = e;
                log.warn("邮件发送失败 (第{}次尝试): to={}, error={}", attempt, to, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    // 等待一段时间后重试，使用指数退避策略
                    Thread.sleep(1000L * attempt);
                }
            }
        }

        // 如果所有重试都失败了，抛出最后一次的异常
        if (lastException != null) {
            throw lastException;
        }
    }

    /**
     * 发送HTML模板邮件
     */
    private void sendTemplateEmailInternal(String to, String templateName,
                                           Map<String, Object> templateParams, String subject) throws Exception {

        String htmlContent = templateEngine.renderTemplate(templateName, templateParams);

        if (asyncMode) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendHtmlEmailSync(to, subject, htmlContent);
                } catch (Exception e) {
                    log.error("异步发送模板邮件失败: to={}, template={}", to, templateName, e);
                }
            }, asyncExecutor);
        } else {
            sendHtmlEmailSync(to, subject, htmlContent);
        }
    }

    /**
     * 同步发送HTML邮件
     */
    private void sendHtmlEmailSync(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true表示这是HTML内容

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                mailSender.send(message);
                log.debug("HTML邮件发送成功 (第{}次尝试): to={}", attempt, to);
                return;

            } catch (Exception e) {
                lastException = e;
                log.warn("HTML邮件发送失败 (第{}次尝试): to={}, error={}", attempt, to, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    Thread.sleep(1000L * attempt);
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    // =================== 内容构建方法 ===================

    /**
     * 构建激活邮件内容
     */
    private String buildActivationEmailContent(String username, String activationUrl) {
        return String.format("""
                亲爱的 %s，
                
                欢迎加入 BaseAI！
                
                为了确保您的账户安全，请点击下面的链接完成账户激活：
                %s
                
                如果您无法点击链接，请复制链接到浏览器地址栏中打开。
                
                此链接将在24小时后失效，请尽快完成激活。
                
                如果您没有注册过 BaseAI 账户，请忽略此邮件。
                
                祝您使用愉快！
                BaseAI 团队
                """, username, activationUrl);
    }

    /**
     * 构建邀请邮件内容
     */
    private String buildInvitationEmailContent(String orgName, String invitationUrl) {
        return String.format("""
                您好，
                
                您被邀请加入 %s 组织！
                
                点击下面的链接查看邀请详情并决定是否加入：
                %s
                
                此邀请链接将在7天后失效。
                
                如果您对此邀请有任何疑问，请联系邀请您的人员。
                
                感谢您的关注！
                BaseAI 团队
                """, orgName, invitationUrl);
    }

    /**
     * 构建密码重置邮件内容
     */
    private String buildPasswordResetEmailContent(String username, String resetUrl) {
        return String.format("""
                亲爱的 %s，
                
                我们收到了您的密码重置请求。
                
                请点击下面的链接重置您的密码：
                %s
                
                此链接将在30分钟后失效，并且只能使用一次。
                
                如果您没有请求重置密码，请忽略此邮件。为了您的账户安全，建议您定期更改密码。
                
                如有任何问题，请联系我们的客服团队。
                
                BaseAI 团队
                """, username, resetUrl);
    }

    // =================== URL构建方法 ===================

    private String buildActivationUrl(String activationCode) {
        return String.format("%s/auth/activate?code=%s", baseUrl, activationCode);
    }

    private String buildInvitationUrl(String invitationToken) {
        return String.format("%s/invitation?token=%s", baseUrl, invitationToken);
    }

    private String buildPasswordResetUrl(String resetToken) {
        return String.format("%s/auth/reset-password?token=%s", baseUrl, resetToken);
    }

    // =================== 参数验证方法 ===================

    private void validateEmailParameters(String email, String errorMessage) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException(errorMessage);
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.EXT_EMAIL_012, email));
        }
    }

    private void validateStringParameter(String value, String errorMessage) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}