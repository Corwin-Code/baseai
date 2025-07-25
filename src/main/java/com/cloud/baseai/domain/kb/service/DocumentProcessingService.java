package com.cloud.baseai.domain.kb.service;

import com.cloud.baseai.domain.kb.model.Chunk;
import com.cloud.baseai.domain.kb.model.Document;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.exception.KnowledgeBaseException;
import com.cloud.baseai.infrastructure.utils.KbUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h2>文档处理领域服务</h2>
 *
 * <p>这个服务承担着将原始文档转换为可搜索知识块的核心责任。当用户上传一本厚厚的技术手册时，
 * 我们需要将其"消化"成一个个易于理解和检索的小段落。这个过程需要考虑多个关键因素：</p>
 *
 * <p><b>智能分块的核心思想：</b></p>
 * <p>就像人类阅读时会自然地按段落和句子来理解内容一样，我们的算法也需要识别文本的自然边界。
 * 简单地按字符数量切割文本会破坏语义的完整性，因此我们采用了多层次的智能分块策略：</p>
 *
 * <p>首先在段落边界分割，这能保持思想的完整性；其次在句子边界分割，确保语法结构不被破坏；
 * 最后通过重叠机制保持上下文的连贯性，就像阅读时前后文能够相互印证一样。</p>
 *
 * <p><b>多语言支持的挑战：</b></p>
 * <p>不同语言有着不同的文本特征。中文没有空格分词，日文混合了假名和汉字，而英文则依赖标点符号。
 * 我们的算法需要智能地识别这些差异并采用相应的处理策略。</p>
 */
@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    // 分块配置常量 - 这些数值是经过实践验证的最优参数
    private static final int DEFAULT_CHUNK_SIZE = 1000;    // 默认分块大小，平衡信息完整性和检索精度
    private static final int MIN_CHUNK_SIZE = 200;         // 最小分块大小，避免信息过于碎片化
    private static final int CHUNK_OVERLAP = 200;          // 分块重叠大小，保持上下文连贯性
    private static final int MAX_CHUNK_SIZE = 4000;        // 最大分块大小，避免单个块过于庞大

    // 语言特定的句子结束符号识别模式
    private static final Pattern CHINESE_SENTENCE_END = Pattern.compile("[。！？；]");
    private static final Pattern ENGLISH_SENTENCE_END = Pattern.compile("[.!?]\\s+");
    private static final Pattern JAPANESE_SENTENCE_END = Pattern.compile("[。！？]");

    // 段落和章节结构识别模式
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n");
    private static final Pattern SECTION_HEADER = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);
    private static final Pattern NUMBERED_LIST = Pattern.compile("^\\d+[.)]\\s+", Pattern.MULTILINE);
    private static final Pattern BULLET_LIST = Pattern.compile("^[-*+]\\s+", Pattern.MULTILINE);

    /**
     * 智能文本分块：将文档内容转换为语义完整的知识块
     *
     * <p>这个方法是整个知识库系统的核心功能之一。它需要解决一个看似简单实则复杂的问题：
     * 如何将一段连续的文本分解为既保持语义完整性，又适合向量检索的小块？</p>
     *
     * <p><b>分块策略的层次结构：</b></p>
     * <p>我们采用了"先粗后细"的分块策略。首先按文档的自然结构（如章节、段落）进行划分，
     * 这样能够保持主题的完整性。然后对于过长的段落，我们会进一步按句子边界细分，
     * 确保每个知识块都包含完整的语义单元。</p>
     *
     * <p><b>重叠机制的重要性：</b></p>
     * <p>想象在阅读一本书时，前一页的结尾和后一页的开头会有语境上的连接。
     * 我们的重叠机制就是模拟这种自然的阅读体验，让相邻的知识块之间有一定的内容重复，
     * 这样在检索时能够更好地捕捉到跨块的语义关系。</p>
     *
     * @param document 文档元信息，包含语言、格式等关键信息
     * @param content  待处理的文档内容
     * @param userId   操作用户ID，用于审计追踪
     * @return 分块后的知识块列表
     * @throws KnowledgeBaseException 当文档结构无法解析时抛出
     */
    public List<Chunk> splitIntoChunks(Document document, String content, Long userId) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("收到空内容的文档: documentId={}", document.id());
            return new ArrayList<>();
        }

        log.info("开始智能分块处理: documentId={}, contentLength={}, language={}",
                document.id(), content.length(), document.langCode());

        try {
            // 第一步：预处理文本，标准化格式并识别结构
            String preprocessedContent = preprocessText(content);

            // 第二步：识别文档结构，如标题、列表、代码块等
            DocumentStructure structure = analyzeDocumentStructure(preprocessedContent);

            // 第三步：基于结构进行智能分块
            List<TextBlock> textBlocks = performStructuralSplit(preprocessedContent, structure, document.langCode());

            // 第四步：优化分块大小，确保每块既不过大也不过小
            List<TextBlock> optimizedBlocks = optimizeBlockSizes(textBlocks, document.langCode());

            // 第五步：生成重叠，增强上下文连贯性
            List<TextBlock> blocksWithOverlap = generateOverlaps(optimizedBlocks);

            // 第六步：转换为知识块对象
            List<Chunk> chunks = convertToChunks(blocksWithOverlap, document, userId);

            log.info("分块处理完成: documentId={}, 原始长度={}, 分块数量={}",
                    document.id(), content.length(), chunks.size());

            return chunks;

        } catch (Exception e) {
            log.error("文档分块处理失败: documentId={}", document.id(), e);
            throw new KnowledgeBaseException(ErrorCode.BIZ_KB_024, e.getMessage(), e);
        }
    }

    /**
     * 预处理文本：清理和标准化输入内容
     *
     * <p>原始的文档内容可能包含各种"杂质"：
     * 多余的空白字符、不规范的格式、特殊的编码字符等。我们需要将这些内容标准化，
     * 为后续的智能处理做好准备。</p>
     */
    private String preprocessText(String content) {
        // 标准化换行符，统一使用Unix风格
        content = content.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // 移除过多的空白行，但保留段落结构
        content = content.replaceAll("\\n{4,}", "\n\n\n");

        // 标准化空格，移除行首行尾多余空格
        String[] lines = content.split("\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            cleaned.append(trimmed).append("\n");
        }

        return cleaned.toString().trim();
    }

    /**
     * 分析文档结构：识别标题、列表、代码块等元素
     *
     * <p>就像建筑师在建造房屋前需要理解土地的地形一样，我们需要先理解文档的"地形"。
     * 不同的文档元素有着不同的语义重要性：标题通常比正文更重要，代码块需要保持完整性，
     * 列表项之间有逻辑关联等。</p>
     */
    private DocumentStructure analyzeDocumentStructure(String content) {
        DocumentStructure structure = new DocumentStructure();

        // 识别标题位置和层级
        Matcher headerMatcher = SECTION_HEADER.matcher(content);
        while (headerMatcher.find()) {
            int level = countHeaderLevel(headerMatcher.group());
            structure.addHeader(headerMatcher.start(), headerMatcher.end(), level);
        }

        // 识别列表结构
        identifyLists(content, structure);

        // 识别代码块（如果是Markdown格式）
        identifyCodeBlocks(content, structure);

        // 识别表格结构
        identifyTables(content, structure);

        return structure;
    }

    /**
     * 执行结构化分割：基于文档结构进行智能分块
     *
     * <p>这是整个分块过程的核心步骤。我们不是简单地按照固定长度切割文本，
     * 而是根据文档的内在结构来决定分割点。这就像是按照文章的逻辑结构来分段，
     * 而不是机械地每隔几行就分一段。</p>
     */
    private List<TextBlock> performStructuralSplit(String content, DocumentStructure structure, String langCode) {
        List<TextBlock> blocks = new ArrayList<>();

        // 如果文档有明显的章节结构，按章节分割
        if (structure.hasHeaders()) {
            blocks = splitByHeaders(content, structure, langCode);
        } else {
            // 否则按段落分割
            blocks = splitByParagraphs(content, langCode);
        }

        return blocks;
    }

    /**
     * 按标题结构分割文本
     */
    private List<TextBlock> splitByHeaders(String content, DocumentStructure structure, String langCode) {
        List<TextBlock> blocks = new ArrayList<>();
        List<DocumentStructure.HeaderInfo> headers = structure.getHeaders();

        int lastEnd = 0;
        for (int i = 0; i < headers.size(); i++) {
            DocumentStructure.HeaderInfo header = headers.get(i);

            // 获取到下一个同级或更高级标题之间的内容
            int sectionEnd = findSectionEnd(headers, i, content.length());

            if (header.start > lastEnd) {
                // 添加标题前的内容
                String beforeHeader = content.substring(lastEnd, header.start).trim();
                if (!beforeHeader.isEmpty()) {
                    blocks.addAll(splitLongText(beforeHeader, langCode, TextBlock.Type.PARAGRAPH));
                }
            }

            // 添加整个章节内容
            String sectionContent = content.substring(header.start, sectionEnd).trim();
            if (!sectionContent.isEmpty()) {
                blocks.addAll(splitLongText(sectionContent, langCode, TextBlock.Type.SECTION));
            }

            lastEnd = sectionEnd;
        }

        // 处理最后剩余的内容
        if (lastEnd < content.length()) {
            String remaining = content.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                blocks.addAll(splitLongText(remaining, langCode, TextBlock.Type.PARAGRAPH));
            }
        }

        return blocks;
    }

    /**
     * 按段落分割文本
     */
    private List<TextBlock> splitByParagraphs(String content, String langCode) {
        List<TextBlock> blocks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_BREAK.split(content);

        StringBuilder currentBlock = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 如果当前块加上新段落会超过最大大小，先保存当前块
            if (currentBlock.length() + paragraph.length() > DEFAULT_CHUNK_SIZE
                    && currentBlock.length() > MIN_CHUNK_SIZE) {

                blocks.add(new TextBlock(currentBlock.toString(), TextBlock.Type.PARAGRAPH));
                currentBlock = new StringBuilder();
            }

            if (!currentBlock.isEmpty()) {
                currentBlock.append("\n\n");
            }
            currentBlock.append(paragraph);
        }

        // 添加最后一个块
        if (!currentBlock.isEmpty()) {
            blocks.add(new TextBlock(currentBlock.toString(), TextBlock.Type.PARAGRAPH));
        }

        return blocks;
    }

    /**
     * 分割过长的文本块
     *
     * <p>当一个文本块仍然过长时，我们需要进一步细分。这时我们会寻找最佳的分割点：
     * 优先选择句子边界，其次是逗号等标点符号，最后才是强制按长度分割。
     * 这样能够最大程度地保持语义的完整性。</p>
     */
    private List<TextBlock> splitLongText(String text, String langCode, TextBlock.Type type) {
        List<TextBlock> blocks = new ArrayList<>();

        if (text.length() <= DEFAULT_CHUNK_SIZE) {
            blocks.add(new TextBlock(text, type));
            return blocks;
        }

        // 按句子分割
        List<String> sentences = splitIntoSentences(text, langCode);
        StringBuilder currentBlock = new StringBuilder();

        for (String sentence : sentences) {
            if (currentBlock.length() + sentence.length() > DEFAULT_CHUNK_SIZE
                    && currentBlock.length() > MIN_CHUNK_SIZE) {

                blocks.add(new TextBlock(currentBlock.toString().trim(), type));
                currentBlock = new StringBuilder();
            }

            if (!currentBlock.isEmpty()) {
                currentBlock.append(" ");
            }
            currentBlock.append(sentence.trim());
        }

        if (!currentBlock.isEmpty()) {
            blocks.add(new TextBlock(currentBlock.toString().trim(), type));
        }

        return blocks;
    }

    /**
     * 按语言特性分割句子
     *
     * <p>不同语言的句子结构差异很大。英文依赖标点符号和空格，中文则更多依赖语义和标点。
     * 我们需要针对不同语言采用相应的分割策略。</p>
     */
    private List<String> splitIntoSentences(String text, String langCode) {
        List<String> sentences = new ArrayList<>();

        Pattern sentencePattern;
        if (langCode.startsWith("zh")) {
            sentencePattern = CHINESE_SENTENCE_END;
        } else if (langCode.startsWith("ja")) {
            sentencePattern = JAPANESE_SENTENCE_END;
        } else {
            sentencePattern = ENGLISH_SENTENCE_END;
        }

        String[] parts = sentencePattern.split(text);
        StringBuilder currentSentence = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;

            currentSentence.append(part);

            // 为非最后一个部分添加句号
            if (i < parts.length - 1) {
                if (langCode.startsWith("zh") || langCode.startsWith("ja")) {
                    currentSentence.append("。");
                } else {
                    currentSentence.append(". ");
                }

                sentences.add(currentSentence.toString());
                currentSentence = new StringBuilder();
            }
        }

        if (!currentSentence.isEmpty()) {
            sentences.add(currentSentence.toString());
        }

        return sentences;
    }

    /**
     * 优化分块大小：确保每个块的大小适中
     *
     * <p>有些块可能太小，信息量不足；有些块可能太大，
     * 难以精确匹配查询。我们需要找到一个平衡点，让每个知识块都能够独立地回答用户的问题。</p>
     */
    private List<TextBlock> optimizeBlockSizes(List<TextBlock> blocks, String langCode) {
        List<TextBlock> optimized = new ArrayList<>();

        for (TextBlock block : blocks) {
            if (block.getText().length() < MIN_CHUNK_SIZE) {
                // 尝试与下一个块合并
                if (!optimized.isEmpty()) {
                    TextBlock lastBlock = optimized.get(optimized.size() - 1);
                    if (lastBlock.getText().length() + block.getText().length() <= DEFAULT_CHUNK_SIZE) {
                        // 合并到上一个块
                        String mergedText = lastBlock.getText() + "\n\n" + block.getText();
                        optimized.set(optimized.size() - 1,
                                new TextBlock(mergedText, lastBlock.getType()));
                        continue;
                    }
                }
            }

            if (block.getText().length() > MAX_CHUNK_SIZE) {
                // 进一步分割过大的块
                optimized.addAll(splitLongText(block.getText(), langCode, block.getType()));
            } else {
                optimized.add(block);
            }
        }

        return optimized;
    }

    /**
     * 生成重叠内容：增强上下文连贯性
     *
     * <p>重叠机制是知识库系统的一个重要创新。想象你在查找某个技术概念的解释，
     * 这个概念可能跨越两个段落。如果没有重叠，搜索结果可能只包含部分信息。
     * 通过重叠，我们确保重要信息不会"掉在缝隙里"。</p>
     */
    private List<TextBlock> generateOverlaps(List<TextBlock> blocks) {
        if (blocks.size() <= 1) {
            return blocks;
        }

        List<TextBlock> withOverlap = new ArrayList<>();

        for (int i = 0; i < blocks.size(); i++) {
            TextBlock currentBlock = blocks.get(i);
            StringBuilder overlappedText = new StringBuilder();

            // 添加前一个块的结尾部分作为重叠
            if (i > 0) {
                String previousOverlap = extractOverlapFromEnd(blocks.get(i - 1).getText());
                if (!previousOverlap.isEmpty()) {
                    overlappedText.append(previousOverlap).append("\n\n");
                }
            }

            // 添加当前块的内容
            overlappedText.append(currentBlock.getText());

            // 添加下一个块的开头部分作为重叠
            if (i < blocks.size() - 1) {
                String nextOverlap = extractOverlapFromStart(blocks.get(i + 1).getText());
                if (!nextOverlap.isEmpty()) {
                    overlappedText.append("\n\n").append(nextOverlap);
                }
            }

            withOverlap.add(new TextBlock(overlappedText.toString(), currentBlock.getType()));
        }

        return withOverlap;
    }

    /**
     * 从文本结尾提取重叠内容
     */
    private String extractOverlapFromEnd(String text) {
        if (text.length() <= CHUNK_OVERLAP) {
            return "";
        }

        String overlap = text.substring(Math.max(0, text.length() - CHUNK_OVERLAP));

        // 尝试在句子边界截断
        int lastSentenceEnd = Math.max(
                overlap.lastIndexOf('。'),
                Math.max(overlap.lastIndexOf('.'), overlap.lastIndexOf('!'))
        );

        if (lastSentenceEnd > 0 && lastSentenceEnd < overlap.length() - 1) {
            overlap = overlap.substring(lastSentenceEnd + 1);
        }

        return overlap.trim();
    }

    /**
     * 从文本开头提取重叠内容
     */
    private String extractOverlapFromStart(String text) {
        if (text.length() <= CHUNK_OVERLAP) {
            return "";
        }

        String overlap = text.substring(0, Math.min(CHUNK_OVERLAP, text.length()));

        // 尝试在句子边界截断
        int firstSentenceEnd = -1;
        for (char c : new char[]{'。', '.', '!', '?'}) {
            int pos = overlap.indexOf(c);
            if (pos > 0 && (firstSentenceEnd == -1 || pos < firstSentenceEnd)) {
                firstSentenceEnd = pos;
            }
        }

        if (firstSentenceEnd > 0) {
            overlap = overlap.substring(0, firstSentenceEnd + 1);
        }

        return overlap.trim();
    }

    /**
     * 转换为知识块对象：完成从文本块到知识块的转换
     */
    private List<Chunk> convertToChunks(List<TextBlock> blocks, Document document, Long userId) {
        List<Chunk> chunks = new ArrayList<>();

        for (int i = 0; i < blocks.size(); i++) {
            TextBlock block = blocks.get(i);

            int tokenSize = KbUtils.estimateTokenCount(block.getText(), document.langCode());

            Chunk chunk = Chunk.create(
                    document.id(),
                    i,  // chunkNo
                    block.getText(),
                    document.langCode(),
                    tokenSize,
                    userId
            );

            chunks.add(chunk);
        }

        return chunks;
    }

    // =================== 辅助方法 ===================

    private void identifyLists(String content, DocumentStructure structure) {
        // 识别编号列表
        Matcher numberedMatcher = NUMBERED_LIST.matcher(content);
        while (numberedMatcher.find()) {
            structure.addListItem(numberedMatcher.start(), numberedMatcher.end(), "numbered");
        }

        // 识别项目符号列表
        Matcher bulletMatcher = BULLET_LIST.matcher(content);
        while (bulletMatcher.find()) {
            structure.addListItem(bulletMatcher.start(), bulletMatcher.end(), "bullet");
        }
    }

    private void identifyCodeBlocks(String content, DocumentStructure structure) {
        // 识别代码块（Markdown格式）
        Pattern codeBlockPattern = Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);
        Matcher codeBlockMatcher = codeBlockPattern.matcher(content);

        while (codeBlockMatcher.find()) {
            structure.addCodeBlock(codeBlockMatcher.start(), codeBlockMatcher.end());
        }
    }

    private void identifyTables(String content, DocumentStructure structure) {
        // 简单的表格识别（基于管道符）
        String[] lines = content.split("\n");
        boolean inTable = false;
        int tableStart = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            boolean isTableRow = line.contains("|") && line.split("\\|").length > 2;

            if (isTableRow && !inTable) {
                inTable = true;
                tableStart = content.indexOf(line);
            } else if (!isTableRow && inTable) {
                inTable = false;
                int tableEnd = content.indexOf(lines[i - 1]) + lines[i - 1].length();
                structure.addTable(tableStart, tableEnd);
            }
        }
    }

    private int countHeaderLevel(String header) {
        int level = 0;
        for (char c : header.toCharArray()) {
            if (c == '#') {
                level++;
            } else {
                break;
            }
        }
        return level;
    }

    private int findSectionEnd(List<DocumentStructure.HeaderInfo> headers, int currentIndex, int contentLength) {
        DocumentStructure.HeaderInfo currentHeader = headers.get(currentIndex);

        for (int i = currentIndex + 1; i < headers.size(); i++) {
            DocumentStructure.HeaderInfo nextHeader = headers.get(i);
            if (nextHeader.level <= currentHeader.level) {
                return nextHeader.start;
            }
        }

        return contentLength;
    }

    /**
     * 计算文档内容的SHA256哈希值
     *
     * <p>这个方法为每个文档生成一个独特的"指纹"。就像人的指纹一样，
     * 这个哈希值能够唯一标识文档的内容，帮助我们检测重复上传的文档。</p>
     */
    public String calculateSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return KbUtils.bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("计算SHA256失败", e);
        }
    }

    // =================== 内部数据结构 ===================

    /**
     * 文本块：分块过程中的中间数据结构
     */
    @Getter
    private static class TextBlock {
        private final String text;
        private final Type type;

        public enum Type {
            PARAGRAPH,    // 普通段落
            SECTION,      // 章节
            LIST_ITEM,    // 列表项
            CODE_BLOCK,   // 代码块
            TABLE         // 表格
        }

        public TextBlock(String text, Type type) {
            this.text = text;
            this.type = type;
        }

    }

    /**
     * 文档结构：记录文档的内在结构信息
     */
    private static class DocumentStructure {
        @Getter
        private final List<HeaderInfo> headers = new ArrayList<>();
        private final List<ListInfo> listItems = new ArrayList<>();
        private final List<BlockInfo> codeBlocks = new ArrayList<>();
        private final List<BlockInfo> tables = new ArrayList<>();

        public void addHeader(int start, int end, int level) {
            headers.add(new HeaderInfo(start, end, level));
        }

        public void addListItem(int start, int end, String type) {
            listItems.add(new ListInfo(start, end, type));
        }

        public void addCodeBlock(int start, int end) {
            codeBlocks.add(new BlockInfo(start, end));
        }

        public void addTable(int start, int end) {
            tables.add(new BlockInfo(start, end));
        }

        public boolean hasHeaders() {
            return !headers.isEmpty();
        }

        public record HeaderInfo(int start, int end, int level) {
        }

        public record ListInfo(int start, int end, String type) {
        }

        public record BlockInfo(int start, int end) {
        }
    }
}