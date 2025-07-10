package com.clinflash.baseai.infrastructure.external.sms;

import com.clinflash.baseai.infrastructure.external.sms.model.BatchSmsResult;
import com.clinflash.baseai.infrastructure.external.sms.model.SmsStatus;
import com.clinflash.baseai.infrastructure.exception.SmsServiceException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * <h2>短信服务接口</h2>
 *
 * <p>这个接口定义了系统中所有短信发送相关的功能契约。在现代互联网应用中，
 * 短信服务扮演着重要的角色，特别是在身份验证、安全通知和重要提醒方面。
 * 相比邮件服务，短信具有更高的到达率和时效性，但成本也相对较高。</p>
 *
 * <p><b>设计哲学：</b></p>
 * <p>短信服务的设计需要特别注意成本控制和安全性。每条短信都有成本，
 * 因此我们需要实现防刷机制、频率限制和内容优化。同时，由于短信通道
 * 的特殊性，我们还需要考虑不同地区的法规要求和运营商政策。</p>
 *
 * <p><b>核心应用场景：</b></p>
 * <ul>
 * <li><b>身份验证：</b>注册验证、登录验证、敏感操作确认</li>
 * <li><b>安全通知：</b>异常登录提醒、密码修改通知</li>
 * <li><b>业务提醒：</b>任务提醒、到期通知、状态变更</li>
 * <li><b>营销推广：</b>活动通知、产品推荐（需遵守相关法规）</li>
 * </ul>
 *
 * <p><b>技术挑战：</b></p>
 * <p>短信服务需要处理多个技术挑战：网络延迟、运营商限制、字符编码、
 * 发送状态追踪等。我们的接口设计考虑了这些因素，提供了灵活且
 * 健壮的解决方案。</p>
 */
public interface SmsService {

    /**
     * 发送验证码短信
     *
     * <p>这是短信服务中最常用的功能。验证码短信需要特别注意安全性：
     * 验证码应该是随机生成的、有时效性的，并且需要防止暴力破解。</p>
     *
     * @param phoneNumber      手机号码，必须是有效的手机号格式
     * @param verificationCode 验证码，通常为4-6位数字
     * @param purpose          发送目的，如"注册验证"、"登录验证"等，用于内容个性化
     * @param expireMinutes    验证码有效期（分钟），用于提示用户
     * @throws SmsServiceException      当短信发送失败时抛出
     * @throws IllegalArgumentException 当传入参数无效时抛出
     */
    void sendVerificationCode(String phoneNumber, String verificationCode,
                              String purpose, int expireMinutes) throws SmsServiceException;

    /**
     * 发送登录异常通知短信
     *
     * <p>当系统检测到用户账户有异常登录行为时，会通过短信及时通知用户。
     * 这是保护用户账户安全的重要措施，需要在不惊扰用户的前提下
     * 提供足够的安全信息。</p>
     *
     * @param phoneNumber   用户手机号
     * @param loginLocation 登录地点，如"北京市"
     * @param loginTime     登录时间
     * @param deviceInfo    设备信息，如"iPhone"、"Chrome浏览器"
     * @throws SmsServiceException 当短信发送失败时抛出
     */
    void sendSecurityAlert(String phoneNumber, String loginLocation,
                           OffsetDateTime loginTime, String deviceInfo) throws SmsServiceException;

    /**
     * 发送业务通知短信
     *
     * <p>用于发送各种业务相关的通知，如任务完成、订单状态变更、
     * 账户变动等。这类短信需要在信息完整性和简洁性之间找到平衡。</p>
     *
     * @param phoneNumber      接收短信的手机号
     * @param notificationType 通知类型，如"任务完成"、"账户变动"
     * @param content          通知内容，应该简洁明了
     * @throws SmsServiceException 当短信发送失败时抛出
     */
    void sendNotification(String phoneNumber, String notificationType, String content) throws SmsServiceException;

    /**
     * 使用模板发送短信
     *
     * <p>模板短信是运营商推荐的发送方式，具有更高的送达率和更好的成本控制。
     * 模板需要预先向运营商报备，通过审核后才能使用。</p>
     *
     * @param phoneNumber    接收短信的手机号
     * @param templateCode   模板编码，由短信服务商提供
     * @param templateParams 模板参数，用于替换模板中的变量
     * @throws SmsServiceException 当短信发送失败时抛出
     */
    void sendTemplateMessage(String phoneNumber, String templateCode,
                             Map<String, String> templateParams) throws SmsServiceException;

    /**
     * 批量发送短信
     *
     * <p>批量发送功能主要用于营销推广或重要通知的群发。由于短信成本较高，
     * 批量发送需要特别注意成本控制和合规性要求。</p>
     *
     * @param phoneNumbers 手机号列表，建议单次不超过1000个
     * @param content      短信内容，营销短信需包含退订信息
     * @param sendTime     发送时间，null表示立即发送
     * @return 批量发送结果，包含成功和失败的统计信息
     * @throws SmsServiceException 当批量发送过程中发生严重错误时抛出
     */
    BatchSmsResult sendBatchMessages(List<String> phoneNumbers, String content,
                                     OffsetDateTime sendTime) throws SmsServiceException;

    /**
     * 查询短信发送状态
     *
     * <p>短信发送是异步过程，从提交到用户接收可能需要几秒到几分钟时间。
     * 通过状态查询可以了解短信的实际送达情况，这对于重要短信特别有价值。</p>
     *
     * @param messageId 短信消息ID，发送时返回
     * @return 短信状态信息
     * @throws SmsServiceException 当查询失败时抛出
     */
    SmsStatus querySmsStatus(String messageId) throws SmsServiceException;

    /**
     * 验证手机号码格式
     *
     * <p>提供手机号码格式验证的工具方法，支持国内外手机号格式。
     * 在发送短信前验证号码格式可以减少不必要的发送费用。</p>
     *
     * @param phoneNumber 待验证的手机号
     * @return 如果手机号格式有效返回true，否则返回false
     */
    boolean isValidPhoneNumber(String phoneNumber);

    /**
     * 获取剩余短信额度
     *
     * <p>查询当前账户的短信余额或剩余额度，用于监控和预警。
     * 当余额不足时，系统可以提前通知管理员进行充值。</p>
     *
     * @return 剩余短信数量，-1表示无限制或查询失败
     * @throws SmsServiceException 当查询失败时抛出
     */
    long getRemainingQuota() throws SmsServiceException;

    /**
     * 检查手机号是否在黑名单中
     *
     * <p>黑名单机制用于防止向特定号码发送短信，这些号码可能是：
     * 用户主动要求停止接收的号码、投诉过的号码或无效号码等。</p>
     *
     * @param phoneNumber 待检查的手机号
     * @return 如果号码在黑名单中返回true，否则返回false
     */
    boolean isBlacklisted(String phoneNumber);
}