package com.clinflash.baseai.infrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * <h1>文件处理工具类</h1>
 *
 * <p>这个工具类包含了处理文件时经常需要的各种功能。
 * 包括：处理文件的读取、写入、校验、格式化等操作。</p>
 *
 * <p><b>为什么需要文件工具类？</b></p>
 * <p>想象一下，如果每次需要计算文件的哈希值、格式化文件大小或验证文件类型时，
 * 都要重新编写代码，那将是多么繁琐和容易出错的事情！通过将这些常用功能
 * 封装到工具类中，我们就能像使用计算器一样方便地调用这些功能。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>1. <b>纯静态方法：</b>所有方法都是静态的，不需要创建实例就能使用</p>
 * <p>2. <b>异常安全：</b>妥善处理各种异常情况，不会因为单个操作失败而崩溃</p>
 * <p>3. <b>性能友好：</b>对于大文件操作，使用流式处理避免内存溢出</p>
 * <p>4. <b>返回明确：</b>每个方法都有清晰的返回值和异常处理机制</p>
 */
public final class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    // 防止实例化：工具类应该只提供静态方法
    private FileUtils() {
        throw new UnsupportedOperationException("FileUtils是工具类，不允许实例化");
    }

    // 定义支持的图片类型 - 这些是Web开发中最常见的图片格式
    private static final List<String> IMAGE_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif",
            "image/bmp", "image/webp", "image/svg+xml"
    );

    // 定义支持的文档类型 - 覆盖了办公和技术文档的主要格式
    private static final List<String> DOCUMENT_MIME_TYPES = Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/markdown", "text/csv", "application/json"
    );

    /**
     * 计算文件的SHA256哈希值
     *
     * <p>SHA256哈希就像是文件的"指纹"——每个文件都有独一无二的哈希值。
     * 这个功能主要用于：</p>
     * <p>1. <b>文件完整性验证：</b>确保文件在传输过程中没有损坏</p>
     * <p>2. <b>去重检测：</b>通过哈希值快速判断两个文件是否相同</p>
     * <p>3. <b>版本控制：</b>跟踪文件的变化历史</p>
     *
     * <p><b>为什么选择SHA256？</b></p>
     * <p>SHA256是目前广泛使用的哈希算法，它提供了很好的安全性和性能平衡。
     * 虽然MD5计算更快，但它已经不够安全；SHA512更安全但计算开销更大。
     * SHA256就像是安全性和性能之间的"黄金平衡点"。</p>
     *
     * @param inputStream 文件输入流
     * @return 文件的SHA256哈希值（十六进制字符串）
     * @throws IOException 当读取文件失败时抛出
     */
    public static String calculateSHA256(InputStream inputStream) throws IOException {
        try {
            // 创建SHA256消息摘要实例 - 这是Java加密库提供的标准实现
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 使用缓冲区分块读取文件 - 这样做的好处是：
            // 1. 避免一次性读取大文件导致内存溢出
            // 2. 提高I/O效率，减少系统调用次数
            byte[] buffer = new byte[8192]; // 8KB缓冲区是经验值，平衡了内存使用和I/O效率
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 将读取的字节更新到摘要计算中
                // 注意这里指定了实际读取的字节数，避免缓冲区末尾的无效数据
                digest.update(buffer, 0, bytesRead);
            }

            // 完成哈希计算并获取结果
            byte[] hashBytes = digest.digest();

            // 将字节数组转换为十六进制字符串
            // 这是哈希值的标准表示方式，便于存储和比较
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            // 这种异常理论上不应该发生，因为SHA-256是Java标准支持的算法
            log.error("SHA-256算法不可用，这可能表示Java环境有问题", e);
            throw new RuntimeException("无法初始化SHA-256算法", e);
        } catch (IOException e) {
            log.error("计算文件SHA256时读取失败", e);
            throw e; // 重新抛出IOException，让调用者处理
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * <p>这个辅助方法将二进制的哈希结果转换为人类可读的十六进制格式。
     * 就像将二进制的计算机语言翻译成我们能理解的文字一样。</p>
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            // 将每个字节转换为两位十六进制数
            // 0xFF是掩码，确保负数字节被正确处理
            // String.format("%02x")确保输出固定两位，不足时前面补0
            result.append(String.format("%02x", b & 0xFF));
        }
        return result.toString();
    }

    /**
     * 格式化文件大小为人类可读的格式
     *
     * <p>计算机存储文件大小时使用字节数，但对人类来说，看到"1048576字节"
     * 远不如看到"1MB"来得直观。这个方法就像是一个翻译器，
     * 将机器语言转换为人类友好的表示方式。</p>
     *
     * <p><b>单位转换的逻辑：</b></p>
     * <p>我们使用1024作为进制（二进制），而不是1000（十进制），
     * 这是因为计算机内部使用二进制系统。虽然硬盘厂商有时使用1000进制，
     * 但在软件开发中，1024进制更为常见和准确。</p>
     *
     * @param sizeInBytes 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 0) {
            return "无效大小";
        }

        // 定义各种大小单位的阈值
        // 使用long类型避免整数溢出
        final long KB = 1024L;
        final long MB = KB * 1024L;
        final long GB = MB * 1024L;
        final long TB = GB * 1024L;

        // 按照从大到小的顺序检查，确保使用最合适的单位
        if (sizeInBytes >= TB) {
            // 对于TB级别的文件，保留两位小数提供足够精度
            return String.format("%.2f TB", (double) sizeInBytes / TB);
        } else if (sizeInBytes >= GB) {
            // GB级别保留两位小数
            return String.format("%.2f GB", (double) sizeInBytes / GB);
        } else if (sizeInBytes >= MB) {
            // MB级别保留一位小数就够了
            return String.format("%.1f MB", (double) sizeInBytes / MB);
        } else if (sizeInBytes >= KB) {
            // KB级别保留一位小数
            return String.format("%.1f KB", (double) sizeInBytes / KB);
        } else {
            // 小于1KB的文件直接显示字节数
            return sizeInBytes + " B";
        }
    }

    /**
     * 从文件名提取文件扩展名
     *
     * <p>文件扩展名就像是文件的"身份证"，告诉我们这是什么类型的文件。
     * 通过扩展名，我们可以判断应该用什么程序打开，以及如何处理这个文件。</p>
     *
     * @param filename 文件名
     * @return 文件扩展名（包含点号），如果没有扩展名则返回空字符串
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }

        // 查找最后一个点号的位置
        // 使用lastIndexOf确保我们找到的是真正的扩展名分隔符
        // 比如"document.backup.pdf"应该返回".pdf"而不是".backup"
        int lastDotIndex = filename.lastIndexOf('.');

        // 检查点号的位置是否有效：
        // 1. 必须存在点号（lastDotIndex != -1）
        // 2. 点号不能在开头（lastDotIndex > 0），避免将".gitignore"这样的文件误判
        // 3. 点号不能在结尾（lastDotIndex < filename.length() - 1）
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex).toLowerCase();
        }

        return "";
    }

    /**
     * 检查文件类型是否为图片
     *
     * <p>这个方法就像是一个图片识别专家，通过MIME类型快速判断
     * 文件是否为图片。MIME类型是互联网标准，比文件扩展名更可靠。</p>
     *
     * @param mimeType 文件的MIME类型
     * @return 如果是图片类型返回true，否则返回false
     */
    public static boolean isImageFile(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return false;
        }

        // 转换为小写进行比较，避免大小写问题
        String normalizedMimeType = mimeType.toLowerCase().trim();

        // 既支持精确匹配，也支持通配符匹配
        return IMAGE_MIME_TYPES.contains(normalizedMimeType) ||
                normalizedMimeType.startsWith("image/");
    }

    /**
     * 检查文件类型是否为支持的文档类型
     *
     * @param mimeType 文件的MIME类型
     * @return 如果是支持的文档类型返回true，否则返回false
     */
    public static boolean isDocumentFile(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return false;
        }

        String normalizedMimeType = mimeType.toLowerCase().trim();
        return DOCUMENT_MIME_TYPES.contains(normalizedMimeType);
    }

    /**
     * 验证文件大小是否在允许范围内
     *
     * <p>这个方法就像是机场安检时的行李重量检查，确保文件大小
     * 符合系统的处理能力和存储限制。</p>
     *
     * @param fileSizeBytes 文件大小（字节）
     * @param maxSizeBytes  允许的最大大小（字节）
     * @return 如果文件大小合规返回true，否则返回false
     */
    public static boolean isValidFileSize(long fileSizeBytes, long maxSizeBytes) {
        // 文件大小必须是正数且不能超过限制
        return fileSizeBytes > 0 && fileSizeBytes <= maxSizeBytes;
    }

    /**
     * 生成安全的文件名
     *
     * <p>原始文件名可能包含特殊字符或过长，这会在文件系统中造成问题。
     * 这个方法就像是一个文件名"清洁工"，移除危险字符并确保长度适中。</p>
     *
     * @param originalName 原始文件名
     * @return 安全的文件名
     */
    public static String sanitizeFilename(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            // 如果原始名称无效，生成一个基于时间的默认名称
            return "file_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        // 移除或替换危险字符
        // 这些字符在不同操作系统中都有特殊含义，可能导致文件操作失败
        String safeName = originalName
                .replaceAll("[\\\\/:*?\"<>|]", "_")  // 替换文件系统保留字符
                .replaceAll("\\s+", "_")             // 将空白字符替换为下划线
                .replaceAll("_{2,}", "_");           // 将连续下划线合并为单个下划线

        // 限制文件名长度，避免某些文件系统的限制
        // 大多数现代文件系统支持255字符的文件名
        if (safeName.length() > 200) {
            String extension = getFileExtension(safeName);
            String baseName = safeName.substring(0, 200 - extension.length());
            safeName = baseName + extension;
        }

        return safeName;
    }

    /**
     * 生成唯一的对象键
     *
     * <p>对象键就像是文件在存储系统中的"地址"，必须保证全局唯一。
     * 我们使用时间戳和哈希值的组合来确保唯一性，同时保持一定的可读性。</p>
     *
     * @param originalFilename 原始文件名
     * @param sha256Hash       文件的SHA256哈希值
     * @return 唯一的对象键
     */
    public static String generateObjectKey(String originalFilename, String sha256Hash) {
        // 获取当前时间戳（毫秒）- 提供时间维度的唯一性
        long timestamp = System.currentTimeMillis();

        // 提取文件扩展名以保持文件类型信息
        String extension = getFileExtension(originalFilename);

        // 使用哈希值的前16位提供内容维度的唯一性
        // 16位已经足够避免冲突，同时保持键的长度适中
        String hashPrefix = sha256Hash != null && sha256Hash.length() >= 16 ?
                sha256Hash.substring(0, 16) :
                "unknown";

        // 组合生成最终的对象键
        // 格式：timestamp/hash_prefix.extension
        // 这种格式既有层次结构（便于管理），又包含足够的唯一性信息
        return String.format("%d/%s%s", timestamp, hashPrefix, extension);
    }

    /**
     * 检查文件是否为空
     *
     * @param filePath 文件路径
     * @return 如果文件为空或不存在返回true，否则返回false
     */
    public static boolean isEmpty(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return !Files.exists(path) || Files.size(path) == 0;
        } catch (Exception e) {
            log.warn("检查文件是否为空时发生异常: {}", filePath, e);
            return true; // 发生异常时认为文件为空，这是更安全的做法
        }
    }

    /**
     * 创建临时文件名
     *
     * <p>临时文件用于存储处理过程中的中间结果，需要确保文件名不会冲突，
     * 同时便于识别和清理。</p>
     *
     * @param prefix    文件名前缀
     * @param extension 文件扩展名
     * @return 临时文件名
     */
    public static String createTempFilename(String prefix, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String randomSuffix = String.valueOf(Math.abs(Thread.currentThread().threadId() % 1000));

        String safePrefix = prefix != null ? sanitizeFilename(prefix) : "temp";
        String safeExtension = extension != null && !extension.isEmpty() ?
                (extension.startsWith(".") ? extension : "." + extension) : "";

        return String.format("%s_%s_%s%s", safePrefix, timestamp, randomSuffix, safeExtension);
    }
}