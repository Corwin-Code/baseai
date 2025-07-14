package com.clinflash.baseai.infrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <h1>数据验证工具类</h1>
 *
 * <p>数据验证就像是系统的"质检员"——在数据进入系统之前，先检查它们是否
 * 符合规格要求。就像工厂的产品在出厂前要经过质量检验一样，用户输入的数据
 * 也需要经过严格的验证，确保它们不会对系统造成危害或混乱。</p>
 *
 * <p><b>为什么数据验证如此重要？</b></p>
 * <p>不合格的数据可能导致程序崩溃、安全漏洞、或者
 * 产生错误的业务结果。数据验证是系统安全和稳定性的第一道防线。</p>
 *
 * <p><b>这个工具类的设计理念：</b></p>
 * <p>1. <b>全面性：</b>涵盖常见的数据验证场景</p>
 * <p>2. <b>性能优化：</b>使用预编译的正则表达式提高性能</p>
 * <p>3. <b>易用性：</b>提供直观的方法名和清晰的返回值</p>
 * <p>4. <b>可扩展：</b>便于添加新的验证规则</p>
 */
public final class ValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtils.class);

    // 防止实例化：工具类应该只提供静态方法
    private ValidationUtils() {
        throw new UnsupportedOperationException("ValidationUtils是工具类，不允许实例化");
    }

    // 预编译的正则表达式模式，提高性能
    // 邮箱验证：遵循RFC 5322标准的简化版本
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // 手机号验证：支持中国大陆手机号格式
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1[3-9]\\d{9}$"
    );

    // 密码强度验证：至少8位，包含大小写字母和数字
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$"
    );

    // 用户名验证：4-20位，只能包含字母、数字和下划线
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{4,20}$"
    );

    // IPv4地址验证
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );

    // URL验证：支持HTTP和HTTPS
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
    );

    // JSON格式的基本验证
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "^\\s*[\\[\\{].*[\\]\\}]\\s*$"
    );

    // =================== 基础验证方法 ===================

    /**
     * 检查字符串是否为空或null
     *
     * <p>这是最基础但也是最重要的验证方法之一。空字符串和null值
     * 在业务逻辑中往往有特殊含义，需要明确识别和处理。</p>
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null、空字符串或只包含空白字符则返回true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空
     *
     * <p>与isEmpty相反的便捷方法，在条件判断中更直观。</p>
     *
     * @param str 要检查的字符串
     * @return 如果字符串不为null且包含非空白字符则返回true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 检查集合是否为空
     *
     * <p>集合的空检查在业务逻辑中经常用到，比如检查查询结果是否有数据、
     * 用户是否选择了选项等。</p>
     *
     * @param collection 要检查的集合
     * @return 如果集合为null或不包含任何元素则返回true
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 检查Map是否为空
     *
     * @param map 要检查的Map
     * @return 如果Map为null或不包含任何键值对则返回true
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 检查数组是否为空
     *
     * @param array 要检查的数组
     * @return 如果数组为null或长度为0则返回true
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    // =================== 字符串长度验证 ===================

    /**
     * 检查字符串长度是否在指定范围内
     *
     * <p>长度验证在很多场景下都需要，比如用户名不能太短或太长、
     * 评论内容要在合理范围内等。这个方法提供了灵活的长度范围验证。</p>
     *
     * @param str       要检查的字符串
     * @param minLength 最小长度（包含）
     * @param maxLength 最大长度（包含）
     * @return 如果字符串长度在指定范围内则返回true
     */
    public static boolean isLengthBetween(String str, int minLength, int maxLength) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 检查字符串是否超过最大长度
     *
     * @param str       要检查的字符串
     * @param maxLength 最大允许长度
     * @return 如果字符串长度超过最大长度则返回true
     */
    public static boolean isLengthExceeded(String str, int maxLength) {
        return str != null && str.length() > maxLength;
    }

    // =================== 格式验证方法 ===================

    /**
     * 验证邮箱地址格式
     *
     * <p>邮箱验证是Web应用中最常见的需求之一。这个方法使用简化的
     * RFC 5322标准，能够识别绝大多数常见的邮箱格式。</p>
     *
     * <p><b>验证规则：</b></p>
     * <p>- 必须包含@符号</p>
     * <p>- @符号前后都要有内容</p>
     * <p>- 域名部分必须包含至少一个点号</p>
     * <p>- 顶级域名至少2个字符</p>
     *
     * @param email 要验证的邮箱地址
     * @return 如果邮箱格式正确则返回true
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 验证手机号码格式（中国大陆）
     *
     * <p>支持中国大陆的11位手机号码格式验证。随着运营商号段的增加，
     * 这个验证规则可能需要定期更新。</p>
     *
     * @param phone 要验证的手机号码
     * @return 如果手机号格式正确则返回true
     */
    public static boolean isValidPhone(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * 验证密码强度
     *
     * <p>强密码是账户安全的重要保障。这个方法检查密码是否符合
     * 基本的安全要求：足够长、包含多种字符类型。</p>
     *
     * <p><b>强密码要求：</b></p>
     * <p>- 至少8个字符</p>
     * <p>- 包含至少一个小写字母</p>
     * <p>- 包含至少一个大写字母</p>
     * <p>- 包含至少一个数字</p>
     * <p>- 可以包含特殊字符</p>
     *
     * @param password 要验证的密码
     * @return 如果密码符合强度要求则返回true
     */
    public static boolean isStrongPassword(String password) {
        if (isEmpty(password)) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 验证用户名格式
     *
     * <p>用户名是用户的身份标识，需要满足一定的格式要求，
     * 既要便于记忆，又要避免特殊字符导致的技术问题。</p>
     *
     * @param username 要验证的用户名
     * @return 如果用户名格式正确则返回true
     */
    public static boolean isValidUsername(String username) {
        if (isEmpty(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * 验证IPv4地址格式
     *
     * <p>在网络配置和安全管理中，经常需要验证IP地址的格式。
     * 这个方法验证标准的IPv4地址格式。</p>
     *
     * @param ip 要验证的IP地址
     * @return 如果IP地址格式正确则返回true
     */
    public static boolean isValidIPv4(String ip) {
        if (isEmpty(ip)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * 验证URL格式
     *
     * <p>URL验证在处理用户提交的链接时很有用，比如个人主页、
     * 参考资料链接等。</p>
     *
     * @param url 要验证的URL
     * @return 如果URL格式正确则返回true
     */
    public static boolean isValidURL(String url) {
        if (isEmpty(url)) {
            return false;
        }
        return URL_PATTERN.matcher(url.trim()).matches();
    }

    // =================== 数值验证方法 ===================

    /**
     * 检查数值是否在指定范围内
     *
     * <p>数值范围验证在很多业务场景中都需要，比如年龄验证、
     * 价格验证、评分验证等。</p>
     *
     * @param value 要检查的数值
     * @param min   最小值（包含）
     * @param max   最大值（包含）
     * @return 如果数值在指定范围内则返回true
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * 检查数值是否在指定范围内（long类型）
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * 检查数值是否在指定范围内（double类型）
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * 检查数值是否为正数
     *
     * @param value 要检查的数值
     * @return 如果数值大于0则返回true
     */
    public static boolean isPositive(Number value) {
        return value != null && value.doubleValue() > 0;
    }

    /**
     * 检查数值是否为非负数
     *
     * @param value 要检查的数值
     * @return 如果数值大于等于0则返回true
     */
    public static boolean isNonNegative(Number value) {
        return value != null && value.doubleValue() >= 0;
    }

    // =================== JSON和数据格式验证 ===================

    /**
     * 基础JSON格式验证
     *
     * <p>这是一个简单的JSON格式检查，用于快速判断字符串是否
     * 可能是JSON格式。更严格的验证需要使用JsonUtils.isValidJson()。</p>
     *
     * @param json 要验证的JSON字符串
     * @return 如果看起来像JSON格式则返回true
     */
    public static boolean looksLikeJson(String json) {
        if (isEmpty(json)) {
            return false;
        }
        String trimmed = json.trim();
        return JSON_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 验证文件扩展名
     *
     * <p>检查文件是否具有允许的扩展名，用于文件上传时的格式控制。</p>
     *
     * @param filename          文件名
     * @param allowedExtensions 允许的扩展名数组（如".jpg", ".png"）
     * @return 如果文件扩展名在允许列表中则返回true
     */
    public static boolean hasAllowedExtension(String filename, String... allowedExtensions) {
        if (isEmpty(filename) || allowedExtensions == null || allowedExtensions.length == 0) {
            return false;
        }

        String extension = FileUtils.getFileExtension(filename).toLowerCase();
        for (String allowed : allowedExtensions) {
            if (extension.equals(allowed.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // =================== 业务规则验证 ===================

    /**
     * 验证年龄是否合理
     *
     * <p>年龄验证在用户注册、实名认证等场景中很常见。
     * 这里定义了合理的年龄范围。</p>
     *
     * @param age 年龄
     * @return 如果年龄在合理范围内（0-150岁）则返回true
     */
    public static boolean isValidAge(int age) {
        return isInRange(age, 0, 150);
    }

    /**
     * 验证百分比值
     *
     * <p>百分比值应该在0-100之间，这在评分、进度、折扣等
     * 场景中经常需要验证。</p>
     *
     * @param percentage 百分比值
     * @return 如果百分比在0-100之间则返回true
     */
    public static boolean isValidPercentage(double percentage) {
        return isInRange(percentage, 0.0, 100.0);
    }

    /**
     * 验证页码和页面大小
     *
     * <p>分页参数的验证在API开发中非常重要，错误的分页参数
     * 可能导致性能问题或安全风险。</p>
     *
     * @param page 页码（从0开始）
     * @param size 页面大小
     * @return 如果分页参数合理则返回true
     */
    public static boolean isValidPagination(int page, int size) {
        return page >= 0 && size > 0 && size <= 1000; // 限制最大页面大小防止性能问题
    }

    // =================== 组合验证方法 ===================

    /**
     * 验证必填字段组合
     *
     * <p>在表单验证中，经常需要同时检查多个必填字段。
     * 这个方法提供了便捷的批量验证。</p>
     *
     * @param fields 要验证的字段数组
     * @return 如果所有字段都不为空则返回true
     */
    public static boolean areAllFieldsPresent(String... fields) {
        if (fields == null || fields.length == 0) {
            return false;
        }

        for (String field : fields) {
            if (isEmpty(field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证至少一个字段存在
     *
     * <p>某些业务场景下，要求用户至少填写几个选项中的一个，
     * 比如联系方式可以是邮箱或手机号。</p>
     *
     * @param fields 要验证的字段数组
     * @return 如果至少有一个字段不为空则返回true
     */
    public static boolean isAtLeastOneFieldPresent(String... fields) {
        if (fields == null || fields.length == 0) {
            return false;
        }

        for (String field : fields) {
            if (isNotEmpty(field)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 批量邮箱验证
     *
     * <p>在邀请用户、批量通知等场景中，需要验证多个邮箱地址。</p>
     *
     * @param emails 邮箱地址数组
     * @return 如果所有邮箱格式都正确则返回true
     */
    public static boolean areAllEmailsValid(String... emails) {
        if (emails == null || emails.length == 0) {
            return false;
        }

        for (String email : emails) {
            if (!isValidEmail(email)) {
                return false;
            }
        }
        return true;
    }
}