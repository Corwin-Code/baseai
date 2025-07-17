package com.cloud.baseai.infrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * <h1>日期时间工具类</h1>
 *
 * <p>时间就像是软件系统的"生命线"——从用户注册时间到文件创建时间，
 * 从任务执行时间到缓存过期时间，几乎所有的业务操作都离不开时间的记录和计算。
 * 这个工具类就像是一个精密的时钟匠工具箱，提供了处理各种时间需求的专业工具。</p>
 *
 * <p><b>为什么时间处理如此复杂？</b></p>
 * <p>想象一下，你要和全世界的朋友约一个会议时间。你需要考虑时区、夏令时、
 * 不同的日期格式，甚至还要考虑农历或其他历法。软件系统处理时间也面临类似的复杂性。
 * Java 8引入的新时间API（如LocalDateTime、ZonedDateTime等）
 * 解决了很多历史问题，但使用起来仍然需要一些技巧。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>1. <b>类型安全：</b>使用Java 8+的现代时间API</p>
 * <p>2. <b>时区感知：</b>正确处理不同时区的转换</p>
 * <p>3. <b>格式统一：</b>提供标准的时间格式化模式</p>
 * <p>4. <b>业务友好：</b>提供常见业务场景的便捷方法</p>
 */
public final class DateTimeUtils {

    private static final Logger log = LoggerFactory.getLogger(DateTimeUtils.class);

    // 防止实例化：工具类应该只提供静态方法
    private DateTimeUtils() {
        throw new UnsupportedOperationException("DateTimeUtils是工具类，不允许实例化");
    }

    // =================== 常用时间格式常量 (预定义的时间格式，避免在代码中散布格式字符串) ===================

    /**
     * 标准日期时间格式：yyyy-MM-dd HH:mm:ss
     *
     * <p>这是最常用的日期时间格式，既包含完整的日期信息，
     * 又有精确到秒的时间信息，适合大多数业务场景。</p>
     */
    public static final DateTimeFormatter STANDARD_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 日期格式：yyyy-MM-dd
     *
     * <p>纯日期格式，用于只关心日期不关心具体时间的场景，
     * 比如生日、入职日期等。</p>
     */
    public static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 时间格式：HH:mm:ss
     *
     * <p>纯时间格式，用于只关心时间不关心日期的场景，
     * 比如营业时间、课程时间等。</p>
     */
    public static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * ISO 8601标准格式：yyyy-MM-dd'T'HH:mm:ss'Z'
     *
     * <p>国际标准的时间格式，特别适合API接口和国际化应用。
     * 'T'分隔日期和时间，'Z'表示UTC时间。</p>
     */
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * 紧凑格式：yyyyMMddHHmmss
     *
     * <p>去掉所有分隔符的紧凑格式，常用于生成文件名、
     * 临时标识符等场景。</p>
     */
    public static final DateTimeFormatter COMPACT_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 中文格式：yyyy年MM月dd日 HH时mm分ss秒
     *
     * <p>中文友好的时间显示格式，适合面向中文用户的界面显示。</p>
     */
    public static final DateTimeFormatter CHINESE_DATETIME = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒");

    // =================== 当前时间获取方法 ===================

    /**
     * 获取当前的LocalDateTime
     *
     * <p>LocalDateTime表示不带时区信息的本地日期时间，
     * 适合在单一时区环境下使用。</p>
     *
     * @return 当前本地日期时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前的UTC时间
     *
     * <p>UTC（协调世界时）是国际标准时间，不受时区和夏令时影响。
     * 在全球化应用中，使用UTC时间可以避免很多时区相关的问题。</p>
     *
     * @return 当前UTC时间
     */
    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * 获取指定时区的当前时间
     *
     * @param zoneId 时区ID（如"Asia/Shanghai"）
     * @return 指定时区的当前时间
     */
    public static ZonedDateTime nowInZone(String zoneId) {
        try {
            return ZonedDateTime.now(ZoneId.of(zoneId));
        } catch (DateTimeException e) {
            log.warn("无效的时区ID: {}, 返回系统默认时区时间", zoneId);
            return ZonedDateTime.now();
        }
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * <p>时间戳是一个长整数，表示从1970年1月1日UTC午夜开始
     * 经过的毫秒数。它是跨系统、跨语言的通用时间表示方式。</p>
     *
     * @return 当前时间戳
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     *
     * @return 当前时间戳（秒）
     */
    public static long currentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }

    // =================== 时间格式化方法 ===================

    /**
     * 将LocalDateTime格式化为字符串
     *
     * @param dateTime  要格式化的时间
     * @param formatter 格式化器
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null || formatter == null) {
            return "";
        }
        try {
            return dateTime.format(formatter);
        } catch (Exception e) {
            log.error("时间格式化失败: dateTime={}, formatter={}", dateTime, formatter, e);
            return "";
        }
    }

    /**
     * 使用标准格式格式化时间
     *
     * <p>这是最常用的时间格式化方法，输出形如"2024-01-15 14:30:25"的格式。</p>
     *
     * @param dateTime 要格式化的时间
     * @return 标准格式的时间字符串
     */
    public static String formatStandard(LocalDateTime dateTime) {
        return format(dateTime, STANDARD_DATETIME);
    }

    /**
     * 格式化为日期字符串
     *
     * @param dateTime 要格式化的时间
     * @return 日期字符串（如"2024-01-15"）
     */
    public static String formatDate(LocalDateTime dateTime) {
        return format(dateTime, DATE_ONLY);
    }

    /**
     * 格式化为时间字符串
     *
     * @param dateTime 要格式化的时间
     * @return 时间字符串（如"14:30:25"）
     */
    public static String formatTime(LocalDateTime dateTime) {
        return format(dateTime, TIME_ONLY);
    }

    /**
     * 格式化为ISO标准格式
     *
     * @param dateTime 要格式化的时间
     * @return ISO格式时间字符串
     */
    public static String formatISO(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(ISO_DATETIME);
    }

    // =================== 时间解析方法 ===================

    /**
     * 解析时间字符串为LocalDateTime
     *
     * @param dateTimeStr 时间字符串
     * @param formatter   格式化器
     * @return 解析后的LocalDateTime，解析失败返回null
     */
    public static LocalDateTime parse(String dateTimeStr, DateTimeFormatter formatter) {
        if (ValidationUtils.isEmpty(dateTimeStr) || formatter == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            log.warn("时间字符串解析失败: input={}, formatter={}", dateTimeStr, formatter);
            return null;
        }
    }

    /**
     * 使用标准格式解析时间字符串
     *
     * @param dateTimeStr 标准格式的时间字符串
     * @return 解析后的LocalDateTime
     */
    public static LocalDateTime parseStandard(String dateTimeStr) {
        return parse(dateTimeStr, STANDARD_DATETIME);
    }

    /**
     * 解析日期字符串
     *
     * @param dateStr 日期字符串（如"2024-01-15"）
     * @return 解析后的LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if (ValidationUtils.isEmpty(dateStr)) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr.trim(), DATE_ONLY);
        } catch (DateTimeParseException e) {
            log.warn("日期字符串解析失败: {}", dateStr);
            return null;
        }
    }

    /**
     * 从时间戳创建LocalDateTime
     *
     * <p>将时间戳转换为LocalDateTime，使用系统默认时区。</p>
     *
     * @param timestamp 时间戳（毫秒）
     * @return LocalDateTime对象
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * 从时间戳创建UTC时间
     *
     * @param timestamp 时间戳（毫秒）
     * @return UTC时间
     */
    public static OffsetDateTime fromTimestampUtc(long timestamp) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC);
    }

    // =================== 时间计算方法 ===================

    /**
     * 计算两个时间之间的天数差
     *
     * <p>常用于计算年龄、会员期限、账单周期等业务场景。</p>
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 天数差（结束日期减去开始日期）
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * 计算两个时间之间的小时差
     *
     * @param startDateTime 开始时间
     * @param endDateTime   结束时间
     * @return 小时差
     */
    public static long hoursBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(startDateTime, endDateTime);
    }

    /**
     * 计算两个时间之间的分钟差
     *
     * @param startDateTime 开始时间
     * @param endDateTime   结束时间
     * @return 分钟差
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }

    /**
     * 添加指定天数
     *
     * @param dateTime 原始时间
     * @param days     要添加的天数（可以为负数）
     * @return 添加天数后的时间
     */
    public static LocalDateTime addDays(LocalDateTime dateTime, long days) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusDays(days);
    }

    /**
     * 添加指定小时数
     *
     * @param dateTime 原始时间
     * @param hours    要添加的小时数
     * @return 添加小时后的时间
     */
    public static LocalDateTime addHours(LocalDateTime dateTime, long hours) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusHours(hours);
    }

    /**
     * 添加指定分钟数
     *
     * @param dateTime 原始时间
     * @param minutes  要添加的分钟数
     * @return 添加分钟后的时间
     */
    public static LocalDateTime addMinutes(LocalDateTime dateTime, long minutes) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.plusMinutes(minutes);
    }

    // =================== 业务常用方法 ===================

    /**
     * 获取今天的开始时间（00:00:00）
     *
     * <p>在统计今日数据、判断是否为当天操作等场景中很有用。</p>
     *
     * @return 今天的开始时间
     */
    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * 获取今天的结束时间（23:59:59.999）
     *
     * @return 今天的结束时间
     */
    public static LocalDateTime endOfToday() {
        return LocalDate.now().atTime(23, 59, 59, 999_999_999);
    }

    /**
     * 获取本月的第一天
     *
     * <p>用于月度统计、账单周期等场景。</p>
     *
     * @return 本月第一天的开始时间
     */
    public static LocalDateTime startOfMonth() {
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    /**
     * 获取本月的最后一天
     *
     * @return 本月最后一天的结束时间
     */
    public static LocalDateTime endOfMonth() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59, 999_999_999);
    }

    /**
     * 获取本周的第一天（周一）
     *
     * <p>用于周报统计等场景。注意这里将周一定义为一周的第一天，
     * 符合ISO-8601标准和中国人的习惯。</p>
     *
     * @return 本周第一天的开始时间
     */
    public static LocalDateTime startOfWeek() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }

    /**
     * 获取本周的最后一天（周日）
     *
     * @return 本周最后一天的结束时间
     */
    public static LocalDateTime endOfWeek() {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59, 59, 999_999_999);
    }

    /**
     * 判断是否为今天
     *
     * @param dateTime 要判断的时间
     * @return 如果是今天则返回true
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * 判断是否为昨天
     *
     * @param dateTime 要判断的时间
     * @return 如果是昨天则返回true
     */
    public static boolean isYesterday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now().minusDays(1));
    }

    /**
     * 判断是否为本周
     *
     * @param dateTime 要判断的时间
     * @return 如果是本周则返回true
     */
    public static boolean isThisWeek(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate date = dateTime.toLocalDate();
        return !date.isBefore(startOfWeek().toLocalDate()) && !date.isAfter(endOfWeek().toLocalDate());
    }

    /**
     * 判断是否为本月
     *
     * @param dateTime 要判断的时间
     * @return 如果是本月则返回true
     */
    public static boolean isThisMonth(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDate date = dateTime.toLocalDate();
        LocalDate now = LocalDate.now();
        return date.getYear() == now.getYear() && date.getMonth() == now.getMonth();
    }

    // =================== 时区转换方法 ===================

    /**
     * 将LocalDateTime转换为指定时区的时间
     *
     * @param dateTime 本地时间
     * @param fromZone 源时区
     * @param toZone   目标时区
     * @return 转换后的时间
     */
    public static ZonedDateTime convertTimeZone(LocalDateTime dateTime, String fromZone, String toZone) {
        if (dateTime == null) {
            return null;
        }

        try {
            ZonedDateTime fromZonedDateTime = dateTime.atZone(ZoneId.of(fromZone));
            return fromZonedDateTime.withZoneSameInstant(ZoneId.of(toZone));
        } catch (DateTimeException e) {
            log.error("时区转换失败: dateTime={}, fromZone={}, toZone={}", dateTime, fromZone, toZone, e);
            return null;
        }
    }

    // =================== 时间期限和过期检查 ===================

    /**
     * 检查时间是否已过期
     *
     * @param expireTime 过期时间
     * @return 如果已过期则返回true
     */
    public static boolean isExpired(LocalDateTime expireTime) {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    /**
     * 检查时间是否在指定分钟内过期
     *
     * <p>用于提前提醒即将过期的情况，比如会话即将超时、
     * 优惠券即将到期等。</p>
     *
     * @param expireTime 过期时间
     * @param minutes    提前提醒的分钟数
     * @return 如果在指定分钟内过期则返回true
     */
    public static boolean willExpireWithin(LocalDateTime expireTime, long minutes) {
        if (expireTime == null) {
            return false;
        }
        LocalDateTime alertTime = LocalDateTime.now().plusMinutes(minutes);
        return expireTime.isBefore(alertTime);
    }

    /**
     * 格式化剩余时间
     *
     * <p>将剩余时间格式化为友好的显示格式，如"2天3小时"。
     * 常用于倒计时显示、过期提醒等场景。</p>
     *
     * @param targetTime 目标时间
     * @return 格式化的剩余时间字符串
     */
    public static String formatTimeRemaining(LocalDateTime targetTime) {
        if (targetTime == null) {
            return "未知";
        }

        LocalDateTime now = LocalDateTime.now();
        if (targetTime.isBefore(now)) {
            return "已过期";
        }

        Duration duration = Duration.between(now, targetTime);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0 && days == 0) { // 只在天数为0时显示分钟
            sb.append(minutes).append("分钟");
        }

        return !sb.isEmpty() ? sb.toString() : "即将到期";
    }

    /**
     * 计算年龄
     *
     * <p>根据生日计算当前年龄，考虑了是否已过生日。</p>
     *
     * @param birthDate 生日
     * @return 年龄
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}