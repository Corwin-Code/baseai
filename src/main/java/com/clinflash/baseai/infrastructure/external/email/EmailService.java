package com.clinflash.baseai.infrastructure.external.email;

import com.clinflash.baseai.infrastructure.external.email.model.BatchEmailResult;
import com.clinflash.baseai.infrastructure.external.email.model.EmailStatus;
import com.clinflash.baseai.infrastructure.exception.EmailServiceException;

import java.util.Map;

/**
 * <h2>邮件服务接口</h2>
 *
 * <p>这个接口定义了系统中所有邮件发送相关的功能契约。它就像一个专业的邮政系统，
 * 负责将各种类型的邮件准确、及时地送达到用户手中。在现代SaaS系统中，
 * 邮件不仅仅是一个通知工具，更是用户体验和系统可靠性的重要组成部分。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>该接口遵循单一职责原则，专注于邮件发送功能，同时保持足够的灵活性
 * 以适应不同的邮件服务提供商（如阿里云、腾讯云、SendGrid等）。
 * 通过统一的接口，我们可以轻松切换底层实现而不影响上层业务逻辑。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 * <li><b>用户激活邮件：</b>为新注册用户发送账户激活链接</li>
 * <li><b>邀请邮件：</b>向被邀请用户发送组织邀请信息</li>
 * <li><b>密码重置邮件：</b>为忘记密码的用户发送重置链接</li>
 * <li><b>通知邮件：</b>发送系统通知和业务提醒</li>
 * <li><b>模板邮件：</b>支持自定义模板的邮件发送</li>
 * </ul>
 *
 * <p><b>异常处理策略：</b></p>
 * <p>所有方法都可能抛出 {@link EmailServiceException}，调用方需要妥善处理
 * 邮件发送失败的情况。建议实现重试机制和降级策略，确保系统的健壮性。</p>
 *
 * <p><b>性能考虑：</b></p>
 * <p>邮件发送通常是异步操作，建议实现类采用异步处理机制，避免阻塞主要业务流程。
 * 同时应该考虑限流和批量发送等优化策略。</p>
 */
public interface EmailService {

    /**
     * 发送用户激活邮件
     *
     * <p>当用户注册成功后，系统会调用此方法发送激活邮件。邮件中包含激活链接，
     * 用户点击链接后即可完成账户激活。这是确保用户邮箱真实性的重要步骤。</p>
     *
     * @param email          用户邮箱地址，必须是有效的邮箱格式
     * @param username       用户名，用于个性化邮件内容
     * @param activationCode 激活码，用于生成激活链接
     * @throws EmailServiceException    当邮件发送失败时抛出，包含详细的错误信息
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    void sendActivationEmail(String email, String username, String activationCode) throws EmailServiceException;

    /**
     * 发送组织邀请邮件
     *
     * <p>当管理员邀请新成员加入组织时，系统会发送邀请邮件。邮件中包含组织信息、
     * 邀请人信息和加入链接，让受邀用户了解邀请详情并决定是否加入。</p>
     *
     * @param email           受邀用户的邮箱地址
     * @param orgName         组织名称，用于标识邀请方
     * @param invitationToken 邀请令牌，用于生成邀请链接
     * @throws EmailServiceException    当邮件发送失败时抛出
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    void sendInvitationEmail(String email, String orgName, String invitationToken) throws EmailServiceException;

    /**
     * 发送密码重置邮件
     *
     * <p>当用户忘记密码并请求重置时，系统发送包含重置链接的邮件。
     * 这是一个安全敏感的操作，需要确保链接的安全性和时效性。</p>
     *
     * @param email      用户邮箱地址
     * @param username   用户名，用于个性化邮件内容
     * @param resetToken 重置令牌，用于生成重置链接
     * @throws EmailServiceException    当邮件发送失败时抛出
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    void sendPasswordResetEmail(String email, String username, String resetToken) throws EmailServiceException;

    /**
     * 发送通知邮件
     *
     * <p>用于发送各种系统通知和业务提醒，如任务完成通知、系统维护通知等。
     * 这是一个通用的邮件发送接口，支持灵活的内容定制。</p>
     *
     * @param email   接收邮件的用户邮箱
     * @param subject 邮件主题，应该简洁明了
     * @param content 邮件内容，支持HTML格式
     * @throws EmailServiceException    当邮件发送失败时抛出
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    void sendNotificationEmail(String email, String subject, String content) throws EmailServiceException;

    /**
     * 使用自定义模板发送邮件
     *
     * <p>这是最灵活的邮件发送方法，支持使用预定义的模板和动态参数。
     * 模板可以在配置文件中定义，支持多语言和个性化定制。</p>
     *
     * @param email          接收邮件的用户邮箱
     * @param templateName   模板名称，对应配置文件中的模板
     * @param templateParams 模板参数，用于替换模板中的占位符
     * @throws EmailServiceException    当邮件发送失败时抛出
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    void sendTemplateEmail(String email, String templateName, Map<String, Object> templateParams) throws EmailServiceException;

    /**
     * 批量发送邮件
     *
     * <p>用于同时向多个用户发送相同内容的邮件，如群发通知、营销邮件等。
     * 实现应该考虑发送限制和错误处理，避免被邮件服务商标记为垃圾邮件。</p>
     *
     * @param emails  接收邮件的用户邮箱列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果，包含成功和失败的邮箱列表
     * @throws EmailServiceException    当批量发送过程中发生严重错误时抛出
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    BatchEmailResult sendBatchEmails(java.util.List<String> emails, String subject, String content) throws EmailServiceException;

    /**
     * 验证邮箱地址格式
     *
     * <p>提供邮箱地址格式验证的工具方法，在发送邮件前可以先验证地址的有效性。
     * 这可以减少因格式错误导致的发送失败。</p>
     *
     * @param email 待验证的邮箱地址
     * @return 如果邮箱格式有效返回true，否则返回false
     */
    boolean isValidEmail(String email);

    /**
     * 获取邮件发送状态
     *
     * <p>查询指定邮件的发送状态，包括已发送、投递中、投递成功、投递失败等。
     * 这对于重要邮件的状态跟踪很有价值。</p>
     *
     * @param messageId 邮件消息ID，发送时返回
     * @return 邮件发送状态信息
     * @throws EmailServiceException 当查询失败时抛出
     */
    EmailStatus getEmailStatus(String messageId) throws EmailServiceException;
}