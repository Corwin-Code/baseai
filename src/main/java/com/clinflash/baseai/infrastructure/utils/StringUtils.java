package com.clinflash.baseai.infrastructure.utils;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h1>字符串处理工具类</h1>
 *
 * <p>字符串是编程世界中最基本也是最重要的数据类型之一。就像文字是人类交流的基础一样，
 * 字符串承载着程序中的绝大部分信息——用户输入、文件内容、日志消息、配置参数等等。
 * 这个工具类就像是一个文字处理专家的工具箱，提供了各种字符串操作的专业工具。</p>
 *
 * <p><b>为什么需要字符串工具类？</b></p>
 * <p>虽然Java自带了String类和一些工具方法，但在实际开发中，我们经常需要更复杂的
 * 字符串处理功能：格式化、清理、转换、生成等。这些操作如果每次都重新实现，
 * 不仅浪费时间，还容易出错。有了这个工具类，就像有了一套专业的文字处理工具，
 * 让字符串操作变得简单、安全、高效。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>1. <b>安全第一：</b>所有方法都要处理null值和边界情况</p>
 * <p>2. <b>性能友好：</b>使用StringBuilder、正则表达式缓存等优化技术</p>
 * <p>3. <b>Unicode支持：</b>正确处理国际化字符</p>
 * <p>4. <b>业务导向：</b>提供常见业务场景的便捷方法</p>
 */
public final class StringUtils {

    // 防止实例化：工具类应该只提供静态方法
    private StringUtils() {
        throw new UnsupportedOperationException("StringUtils是工具类，不允许实例化");
    }

    // 预编译的正则表达式，提高性能
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern EMAIL_MASK_PATTERN = Pattern.compile("(?<=.{2})[^@]*(?=.{2}@)");

    // 随机字符生成器
    private static final SecureRandom RANDOM = new SecureRandom();

    // 字符集定义
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALPHANUMERIC_CHARS = LOWERCASE_CHARS + UPPERCASE_CHARS + DIGIT_CHARS;

    // =================== 基础字符串检查方法 ===================

    /**
     * 检查字符串是否为空或null
     *
     * <p>这是字符串处理中最基础但也最重要的方法。空字符串和null值
     * 在业务逻辑中往往需要特殊处理，正确识别它们是后续处理的前提。</p>
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
     * @param str 要检查的字符串
     * @return 如果字符串不为null且包含非空白字符则返回true
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 检查字符串是否为空白
     *
     * <p>空白字符串指的是只包含空格、制表符、换行符等空白字符的字符串。
     * 在用户输入处理中，这种情况很常见。</p>
     *
     * @param str 要检查的字符串
     * @return 如果字符串为null或只包含空白字符则返回true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空白
     *
     * @param str 要检查的字符串
     * @return 如果字符串包含非空白字符则返回true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 安全的字符串长度获取
     *
     * <p>避免在null字符串上调用length()方法导致NullPointerException。</p>
     *
     * @param str 要检查的字符串
     * @return 字符串长度，null时返回0
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }

    // =================== 字符串清理和格式化方法 ===================

    /**
     * 去除字符串首尾空白字符
     *
     * <p>这是对String.trim()的安全包装，避免null值导致的异常。</p>
     *
     * @param str 要处理的字符串
     * @return 去除首尾空白后的字符串，null时返回null
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * 去除字符串首尾空白字符，null时返回空字符串
     *
     * @param str 要处理的字符串
     * @return 去除首尾空白后的字符串，null时返回空字符串
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 去除字符串中的所有空白字符
     *
     * <p>不仅去除首尾空白，还去除字符串中间的所有空格、制表符、换行符等。
     * 适用于处理需要紧凑格式的数据，如电话号码、身份证号等。</p>
     *
     * @param str 要处理的字符串
     * @return 去除所有空白字符后的字符串
     */
    public static String removeAllWhitespace(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return WHITESPACE_PATTERN.matcher(str).replaceAll("");
    }

    /**
     * 将多个连续的空白字符替换为单个空格
     *
     * <p>在处理用户输入或格式化文本时，经常需要将多余的空白字符标准化。
     * 这个方法可以将制表符、换行符、多个空格等都统一为单个空格。</p>
     *
     * @param str 要处理的字符串
     * @return 标准化空白字符后的字符串
     */
    public static String normalizeWhitespace(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return WHITESPACE_PATTERN.matcher(str.trim()).replaceAll(" ");
    }

    /**
     * 移除HTML标签
     *
     * <p>在处理富文本编辑器的内容或爬虫获取的网页内容时，
     * 经常需要提取纯文本内容。这个方法可以简单地移除HTML标签。</p>
     *
     * @param str 包含HTML标签的字符串
     * @return 移除HTML标签后的纯文本
     */
    public static String removeHtmlTags(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return HTML_TAG_PATTERN.matcher(str).replaceAll("");
    }

    // =================== 字符串转换方法 ===================

    /**
     * 转换为驼峰命名格式
     *
     * <p>将下划线分隔的字符串转换为驼峰命名格式，常用于
     * 数据库字段名到Java属性名的转换。</p>
     *
     * <p>例如："user_name" -> "userName"</p>
     *
     * @param str 下划线分隔的字符串
     * @return 驼峰命名格式的字符串
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }

        String[] words = str.toLowerCase().split("_");
        StringBuilder result = new StringBuilder(words[0]);

        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)))
                        .append(words[i].substring(1));
            }
        }

        return result.toString();
    }

    /**
     * 转换为下划线分隔格式
     *
     * <p>将驼峰命名转换为下划线分隔格式，常用于
     * Java属性名到数据库字段名的转换。</p>
     *
     * <p>例如："userName" -> "user_name"</p>
     *
     * @param str 驼峰命名的字符串
     * @return 下划线分隔的字符串
     */
    public static String toSnakeCase(String str) {
        if (isEmpty(str)) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }

        return result.toString();
    }

    /**
     * 转换为Pascal命名格式（首字母大写的驼峰）
     *
     * <p>例如："user_name" -> "UserName"</p>
     *
     * @param str 要转换的字符串
     * @return Pascal命名格式的字符串
     */
    public static String toPascalCase(String str) {
        if (isEmpty(str)) {
            return str;
        }

        String camelCase = toCamelCase(str);
        if (camelCase.isEmpty()) {
            return camelCase;
        }

        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
    }

    /**
     * 转换为kebab命名格式（短横线分隔）
     *
     * <p>例如："userName" -> "user-name"</p>
     *
     * @param str 要转换的字符串
     * @return kebab命名格式的字符串
     */
    public static String toKebabCase(String str) {
        if (isEmpty(str)) {
            return str;
        }

        return toSnakeCase(str).replace('_', '-');
    }

    // =================== 字符串截取和省略方法 ===================

    /**
     * 安全的字符串截取
     *
     * <p>避免StringIndexOutOfBoundsException，当索引超出范围时自动调整。</p>
     *
     * @param str   要截取的字符串
     * @param start 开始位置
     * @param end   结束位置
     * @return 截取的子字符串
     */
    public static String safeSubstring(String str, int start, int end) {
        if (isEmpty(str)) {
            return str;
        }

        int len = str.length();
        start = Math.max(0, Math.min(start, len));
        end = Math.max(start, Math.min(end, len));

        return str.substring(start, end);
    }

    /**
     * 截取字符串并添加省略号
     *
     * <p>当字符串长度超过指定长度时，截取前面部分并添加"..."。
     * 常用于列表显示、摘要展示等场景。</p>
     *
     * @param str       要截取的字符串
     * @param maxLength 最大长度（包括省略号）
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || maxLength <= 0) {
            return "";
        }

        if (str.length() <= maxLength) {
            return str;
        }

        if (maxLength <= 3) {
            return "...".substring(0, maxLength);
        }

        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 智能截取，在单词边界处截断
     *
     * <p>避免在单词中间截断，保持文本的可读性。</p>
     *
     * @param str       要截取的字符串
     * @param maxLength 最大长度
     * @return 在单词边界截取的字符串
     */
    public static String truncateAtWord(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }

        String truncated = str.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');

        if (lastSpace > maxLength / 2) { // 如果空格位置合理
            return truncated.substring(0, lastSpace) + "...";
        } else {
            return truncate(str, maxLength);
        }
    }

    // =================== 字符串生成方法 ===================

    /**
     * 生成随机字符串
     *
     * <p>生成指定长度的随机字符串，包含大小写字母和数字。
     * 常用于生成临时密码、验证码、唯一标识符等。</p>
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomAlphanumeric(int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            result.append(ALPHANUMERIC_CHARS.charAt(index));
        }

        return result.toString();
    }

    /**
     * 生成随机数字字符串
     *
     * <p>生成只包含数字的随机字符串，常用于生成验证码。</p>
     *
     * @param length 字符串长度
     * @return 随机数字字符串
     */
    public static String randomNumeric(int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(RANDOM.nextInt(10));
        }

        return result.toString();
    }

    /**
     * 生成强随机字符串
     *
     * <p>包含大小写字母、数字和特殊字符，适合生成临时密码。</p>
     *
     * @param length 字符串长度
     * @return 强随机字符串
     */
    public static String randomStrong(int length) {
        if (length <= 0) {
            return "";
        }

        String allChars = ALPHANUMERIC_CHARS + SPECIAL_CHARS;
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(allChars.length());
            result.append(allChars.charAt(index));
        }

        return result.toString();
    }

    /**
     * 重复字符串
     *
     * <p>将指定字符串重复指定次数。</p>
     *
     * @param str   要重复的字符串
     * @param count 重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }

        if (count == 1) {
            return str;
        }

        return str.repeat(count);
    }

    // =================== 字符串掩码和脱敏方法 ===================

    /**
     * 掩码邮箱地址
     *
     * <p>将邮箱地址的用户名部分用星号替换，保护用户隐私。</p>
     * <p>例如："john.doe@example.com" -> "jo***e@example.com"</p>
     *
     * @param email 邮箱地址
     * @return 掩码后的邮箱地址
     */
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }

        return EMAIL_MASK_PATTERN.matcher(email).replaceAll("***");
    }

    /**
     * 掩码手机号码
     *
     * <p>将手机号码的中间部分用星号替换。</p>
     * <p>例如："13812345678" -> "138****5678"</p>
     *
     * @param phone 手机号码
     * @return 掩码后的手机号码
     */
    public static String maskPhone(String phone) {
        if (isEmpty(phone) || phone.length() < 7) {
            return phone;
        }

        if (phone.length() == 11) { // 中国手机号
            return phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            // 通用处理：显示前后各2位
            int len = phone.length();
            int maskLen = len - 4;
            return phone.substring(0, 2) + "*".repeat(maskLen) + phone.substring(len - 2);
        }
    }

    /**
     * 掩码身份证号
     *
     * <p>将身份证号的中间部分用星号替换。</p>
     * <p>例如："110101199001011234" -> "1101**********1234"</p>
     *
     * @param idCard 身份证号
     * @return 掩码后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (isEmpty(idCard) || idCard.length() < 8) {
            return idCard;
        }

        return idCard.substring(0, 4) +
                "*".repeat(idCard.length() - 8) +
                idCard.substring(idCard.length() - 4);
    }

    /**
     * 通用掩码方法
     *
     * <p>保留前面和后面指定位数，中间用星号替换。</p>
     *
     * @param str       要掩码的字符串
     * @param prefixLen 保留前缀长度
     * @param suffixLen 保留后缀长度
     * @return 掩码后的字符串
     */
    public static String mask(String str, int prefixLen, int suffixLen) {
        if (isEmpty(str)) {
            return str;
        }

        int totalLen = str.length();
        int maskLen = totalLen - prefixLen - suffixLen;

        if (maskLen <= 0) {
            return str; // 字符串太短，不进行掩码
        }

        return str.substring(0, prefixLen) +
                "*".repeat(maskLen) +
                str.substring(totalLen - suffixLen);
    }

    // =================== 字符串比较和查找方法 ===================

    /**
     * 忽略大小写的字符串比较
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 如果两个字符串相等（忽略大小写）则返回true
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (Objects.equals(str1, str2)) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equalsIgnoreCase(str2);
    }

    /**
     * 安全的字符串包含检查
     *
     * @param str       主字符串
     * @param searchStr 要查找的字符串
     * @return 如果主字符串包含查找字符串则返回true
     */
    public static boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.contains(searchStr);
    }

    /**
     * 忽略大小写的字符串包含检查
     *
     * @param str       主字符串
     * @param searchStr 要查找的字符串
     * @return 如果主字符串包含查找字符串（忽略大小写）则返回true
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.toLowerCase().contains(searchStr.toLowerCase());
    }

    // =================== 字符串编码和Unicode处理 ===================

    /**
     * Unicode标准化
     *
     * <p>将Unicode字符串标准化，解决不同编码方式导致的字符显示问题。</p>
     *
     * @param str 要标准化的字符串
     * @return 标准化后的字符串
     */
    public static String normalizeUnicode(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Normalizer.normalize(str, Normalizer.Form.NFC);
    }

    /**
     * 检查字符串是否只包含ASCII字符
     *
     * @param str 要检查的字符串
     * @return 如果只包含ASCII字符则返回true
     */
    public static boolean isAscii(String str) {
        if (isEmpty(str)) {
            return true;
        }

        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    /**
     * 统计字符串中的中文字符数量
     *
     * @param str 要统计的字符串
     * @return 中文字符数量
     */
    public static int countChineseCharacters(String str) {
        if (isEmpty(str)) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文Unicode范围
                count++;
            }
        }
        return count;
    }

    // =================== 字符串集合操作 ===================

    /**
     * 将字符串数组连接为单个字符串
     *
     * @param strings   字符串数组
     * @param delimiter 分隔符
     * @return 连接后的字符串
     */
    public static String join(String[] strings, String delimiter) {
        if (strings == null || strings.length == 0) {
            return "";
        }

        return Arrays.stream(strings)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * 将字符串列表连接为单个字符串
     *
     * @param strings   字符串列表
     * @param delimiter 分隔符
     * @return 连接后的字符串
     */
    public static String join(List<String> strings, String delimiter) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }

        return strings.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * 分割字符串并去除空白
     *
     * @param str       要分割的字符串
     * @param delimiter 分隔符
     * @return 分割后的字符串列表（已去除空白和空字符串）
     */
    public static List<String> splitAndTrim(String str, String delimiter) {
        if (isEmpty(str)) {
            return new ArrayList<>();
        }

        return Arrays.stream(str.split(Pattern.quote(delimiter)))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}