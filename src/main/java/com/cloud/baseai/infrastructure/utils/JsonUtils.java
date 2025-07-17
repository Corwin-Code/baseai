package com.cloud.baseai.infrastructure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h1>JSON处理工具类</h1>
 *
 * <p>JSON（JavaScript Object Notation）是现代应用程序中的"通用语言"。
 * 就像人们用英语作为国际交流的通用语言一样，不同的系统、服务和应用
 * 使用JSON来交换数据。这个工具类就像是一个精通多种语言的翻译官，
 * 帮助我们在Java对象和JSON字符串之间进行转换。</p>
 *
 * <p><b>为什么JSON如此重要？</b></p>
 * <p>想象一下，如果你要把一本书的内容通过电话告诉朋友，你需要按照某种
 * 约定的格式来描述：章节号、标题、内容等。JSON就是这样的格式约定，
 * 它让不同的程序能够理解彼此交换的数据结构。</p>
 *
 * <p><b>这个工具类的设计理念：</b></p>
 * <p>1. <b>安全第一：</b>所有操作都有异常处理，不会因为格式错误而崩溃</p>
 * <p>2. <b>使用简单：</b>提供直观的方法名和参数，降低使用门槛</p>
 * <p>3. <b>性能优化：</b>使用单例的ObjectMapper，避免重复创建开销</p>
 * <p>4. <b>灵活配置：</b>支持各种Java时间类型和常见的序列化需求</p>
 */
public final class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * 单例的ObjectMapper实例
     *
     * <p>ObjectMapper是Jackson库的核心类，负责JSON的序列化和反序列化。
     * 创建ObjectMapper有一定开销，而且它是线程安全的，所以我们使用单例模式。
     * 这就像是在办公室里共享一台打印机，而不是每个人都买一台。</p>
     */
    private static final ObjectMapper OBJECT_MAPPER;

    // 静态初始化块：在类加载时配置ObjectMapper
    static {
        OBJECT_MAPPER = new ObjectMapper();

        // 注册Java 8时间模块：让ObjectMapper能够处理LocalDateTime、ZonedDateTime等新时间类型
        // 这就像是给翻译官提供了一本新的词典，让他能理解现代的时间表达方式
        OBJECT_MAPPER.registerModule(new JavaTimeModule());

        // 配置序列化选项：将时间写为ISO-8601格式的字符串，而不是时间戳
        // 时间戳对机器友好，但ISO格式对人类更可读，比如"2024-01-15T10:30:00Z"
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 配置反序列化选项：当JSON中有Java类没有的字段时不报错
        // 这提高了系统的向后兼容性，就像向前兼容的软件版本一样
        OBJECT_MAPPER.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );
    }

    // 防止实例化：工具类应该只提供静态方法
    private JsonUtils() {
        throw new UnsupportedOperationException("JsonUtils是工具类，不允许实例化");
    }

    /**
     * 将Java对象转换为JSON字符串
     *
     * <p>这个方法就像是把一个复杂的立体模型"压平"成一张图纸。
     * Java对象在内存中是立体的、有层次的结构，而JSON是扁平的文本格式。
     * 序列化过程就是将这种立体结构按照JSON规则展开成文本。</p>
     *
     * <p><b>使用场景举例：</b></p>
     * <p>- API响应：将查询结果转换为JSON返回给前端</p>
     * <p>- 数据存储：将对象保存到数据库的JSON字段中</p>
     * <p>- 日志记录：将复杂对象以JSON格式记录到日志中</p>
     * <p>- 消息队列：将对象转换为JSON在系统间传递</p>
     *
     * @param object 要转换的Java对象
     * @return JSON字符串，如果转换失败返回空的Optional
     */
    public static Optional<String> toJson(Object object) {
        if (object == null) {
            // null值应该明确返回JSON的null，而不是Java的null
            return Optional.of("null");
        }

        try {
            // 使用ObjectMapper进行序列化
            // 这个过程会递归遍历对象的所有属性，按照JSON格式构建字符串
            String jsonString = OBJECT_MAPPER.writeValueAsString(object);
            return Optional.of(jsonString);

        } catch (JsonProcessingException e) {
            // 记录错误但不抛出异常，让调用者能够优雅地处理失败情况
            log.error("将对象转换为JSON时发生错误，对象类型: {}",
                    object.getClass().getSimpleName(), e);
            return Optional.empty();
        }
    }

    /**
     * 将Java对象转换为格式化的JSON字符串（美观打印）
     *
     * <p>这个方法产生的JSON带有适当的缩进和换行，就像是把压缩的代码
     * "美化"一样。虽然文件会大一些，但人类阅读起来更容易。</p>
     *
     * <p><b>什么时候使用美观打印？</b></p>
     * <p>- 调试时查看JSON结构</p>
     * <p>- 生成配置文件</p>
     * <p>- 创建API文档示例</p>
     * <p>- 记录详细的审计日志</p>
     *
     * @param object 要转换的Java对象
     * @return 格式化的JSON字符串
     */
    public static Optional<String> toPrettyJson(Object object) {
        if (object == null) {
            return Optional.of("null");
        }

        try {
            // 使用writerWithDefaultPrettyPrinter()创建格式化输出
            // 这会添加适当的缩进、换行和空格，让JSON结构更清晰
            String prettyJson = OBJECT_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
            return Optional.of(prettyJson);

        } catch (JsonProcessingException e) {
            log.error("将对象转换为格式化JSON时发生错误，对象类型: {}",
                    object.getClass().getSimpleName(), e);
            return Optional.empty();
        }
    }

    /**
     * 将JSON字符串转换为指定类型的Java对象
     *
     * <p>这个方法是序列化的"逆过程"，就像是根据图纸重新组装立体模型。
     * 我们需要告诉方法应该组装成什么类型的对象，这就是为什么需要传入Class参数。</p>
     *
     * <p><b>泛型的魔法：</b></p>
     * <p>通过使用泛型 &lt;T&gt;，这个方法可以转换为任何类型的对象，
     * 而且返回值的类型是编译时确定的，这提供了类型安全性。</p>
     *
     * @param <T>         目标对象的类型
     * @param jsonString  JSON字符串
     * @param targetClass 目标对象的Class
     * @return 转换后的Java对象，失败时返回空的Optional
     */
    public static <T> Optional<T> fromJson(String jsonString, Class<T> targetClass) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            log.debug("尝试解析空的JSON字符串");
            return Optional.empty();
        }

        try {
            // 使用readValue进行反序列化
            // ObjectMapper会根据JSON的结构和目标类的属性进行映射
            T result = OBJECT_MAPPER.readValue(jsonString, targetClass);
            return Optional.of(result);

        } catch (JsonProcessingException e) {
            // 解析失败可能的原因：
            // 1. JSON格式不正确
            // 2. JSON结构与目标类不匹配
            // 3. 数据类型无法转换
            log.error("将JSON转换为对象时发生错误，目标类型: {}, JSON: {}",
                    targetClass.getSimpleName(),
                    jsonString.length() > 200 ? jsonString.substring(0, 200) + "..." : jsonString,
                    e);
            return Optional.empty();
        }
    }

    /**
     * 将JSON字符串转换为指定类型的Java对象（支持复杂泛型）
     *
     * <p>有些时候我们需要转换为复杂的泛型类型，比如 List&lt;User&gt; 或 Map&lt;String, Object&gt;。
     * 普通的Class参数无法表达这种复杂类型，所以Jackson提供了TypeReference来解决这个问题。</p>
     *
     * <p><b>TypeReference的用法示例：</b></p>
     * <pre>
     * // 转换为List<User>
     * Optional<List<User>> users = JsonUtils.fromJson(json, new TypeReference<List<User>>() {});
     *
     * // 转换为Map<String, Object>
     * Optional<Map<String, Object>> map = JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
     * </pre>
     *
     * @param <T>           目标对象的类型
     * @param jsonString    JSON字符串
     * @param typeReference 类型引用
     * @return 转换后的Java对象
     */
    public static <T> Optional<T> fromJson(String jsonString, TypeReference<T> typeReference) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            log.debug("尝试解析空的JSON字符串");
            return Optional.empty();
        }

        try {
            T result = OBJECT_MAPPER.readValue(jsonString, typeReference);
            return Optional.of(result);

        } catch (JsonProcessingException e) {
            log.error("将JSON转换为复杂类型对象时发生错误，类型: {}, JSON: {}",
                    typeReference.getType(),
                    jsonString.length() > 200 ? jsonString.substring(0, 200) + "..." : jsonString,
                    e);
            return Optional.empty();
        }
    }

    /**
     * 将JSON字符串转换为Map
     *
     * <p>这是一个非常实用的便捷方法。当我们不知道JSON的确切结构，
     * 或者只需要动态访问某些字段时，转换为Map是最灵活的选择。
     * Map就像是一个动态的容器，可以存放任何结构的数据。</p>
     *
     * @param jsonString JSON字符串
     * @return Map对象，键为字符串，值为Object
     */
    public static Optional<Map<String, Object>> toMap(String jsonString) {
        return fromJson(jsonString, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * 将JSON字符串转换为List
     *
     * <p>当JSON表示一个数组结构时，这个方法可以直接转换为List。
     * List中的每个元素都是Map类型，适合处理对象数组的场景。</p>
     *
     * @param jsonString JSON字符串
     * @return List对象，元素为Map
     */
    public static Optional<List<Map<String, Object>>> toList(String jsonString) {
        return fromJson(jsonString, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    /**
     * 验证字符串是否为有效的JSON格式
     *
     * <p>这个方法就像是JSON的"语法检查器"。在处理用户输入或外部数据时，
     * 先验证格式的正确性可以避免后续处理中的错误。</p>
     *
     * <p><b>验证的好处：</b></p>
     * <p>1. 早期发现问题：在数据处理的早期阶段就发现格式错误</p>
     * <p>2. 提升用户体验：可以给用户提供明确的错误提示</p>
     * <p>3. 系统稳定性：避免格式错误导致的系统异常</p>
     *
     * @param jsonString 要验证的字符串
     * @return 如果是有效JSON返回true，否则返回false
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }

        try {
            // 尝试解析JSON，如果能成功解析说明格式正确
            // 使用readTree()方法是因为它只验证格式，不涉及具体类型转换
            OBJECT_MAPPER.readTree(jsonString);
            return true;

        } catch (JsonProcessingException e) {
            // 解析失败说明JSON格式不正确
            log.debug("JSON格式验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从JSON字符串中提取JsonNode
     *
     * <p>JsonNode是Jackson提供的JSON树结构表示，类似于DOM对XML的表示。
     * 当你需要对JSON进行复杂的查询、修改或遍历操作时，JsonNode提供了
     * 比Map更强大和专业的API。</p>
     *
     * <p><b>JsonNode的优势：</b></p>
     * <p>- 提供了丰富的查询方法，如path()、get()、findValue()等</p>
     * <p>- 支持XPath式的路径查询</p>
     * <p>- 类型安全的值提取方法</p>
     * <p>- 支持JSON结构的修改和构建</p>
     *
     * @param jsonString JSON字符串
     * @return JsonNode对象，可用于复杂的JSON操作
     */
    public static Optional<JsonNode> parseToNode(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            JsonNode node = OBJECT_MAPPER.readTree(jsonString);
            return Optional.of(node);

        } catch (JsonProcessingException e) {
            log.error("解析JSON为JsonNode时发生错误: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 深度复制对象（通过JSON序列化和反序列化）
     *
     * <p>这是一个巧妙的深度复制实现。通过先将对象序列化为JSON，
     * 再反序列化回来，我们得到了一个完全独立的对象副本。
     * 这就像是把一个复杂的立体模型完全拆解，然后按照图纸重新组装。</p>
     *
     * <p><b>什么时候需要深度复制？</b></p>
     * <p>当对象包含嵌套的引用类型时，普通的浅复制只会复制引用，
     * 修改复制品仍然会影响原对象。深度复制创建完全独立的副本，
     * 避免了这种"牵一发动全身"的问题。</p>
     *
     * <p><b>注意事项：</b></p>
     * <p>这种方法要求对象能够正确地序列化和反序列化，
     * 对于包含循环引用或非JSON兼容字段的对象可能不适用。</p>
     *
     * @param <T>         对象类型
     * @param original    原始对象
     * @param targetClass 目标类型
     * @return 深度复制的对象
     */
    public static <T> Optional<T> deepCopy(T original, Class<T> targetClass) {
        if (original == null) {
            return Optional.empty();
        }

        // 先序列化为JSON
        Optional<String> jsonOpt = toJson(original);
        if (jsonOpt.isEmpty()) {
            log.warn("深度复制失败：无法序列化原始对象");
            return Optional.empty();
        }

        // 再反序列化为新对象
        return fromJson(jsonOpt.get(), targetClass);
    }

    /**
     * 合并两个JSON对象
     *
     * <p>这个方法实现了JSON对象的"智能合并"。第二个对象的字段会覆盖
     * 第一个对象中的同名字段，但不会影响第一个对象中独有的字段。
     * 这就像是把两张透明胶片叠加在一起，重叠的部分以上层为准。</p>
     *
     * @param baseJson     基础JSON字符串
     * @param overrideJson 覆盖JSON字符串
     * @return 合并后的JSON字符串
     */
    public static Optional<String> mergeJson(String baseJson, String overrideJson) {
        try {
            // 解析两个JSON为JsonNode
            JsonNode baseNode = OBJECT_MAPPER.readTree(baseJson);
            JsonNode overrideNode = OBJECT_MAPPER.readTree(overrideJson);

            // 将基础节点转换为可修改的对象节点
            JsonNode mergedNode = baseNode.deepCopy();

            // 遍历覆盖节点的所有字段
            overrideNode.fields().forEachRemaining(entry -> {
                // 使用ObjectNode的put方法进行字段覆盖
                ((com.fasterxml.jackson.databind.node.ObjectNode) mergedNode)
                        .set(entry.getKey(), entry.getValue());
            });

            // 将合并后的节点转换回JSON字符串
            return toJson(mergedNode);

        } catch (JsonProcessingException e) {
            log.error("合并JSON时发生错误", e);
            return Optional.empty();
        }
    }

    /**
     * 获取ObjectMapper实例（供高级用法使用）
     *
     * <p>有时候开发者可能需要直接使用ObjectMapper进行一些高级操作，
     * 比如自定义序列化器、配置特殊的处理规则等。这个方法提供了
     * 访问内部ObjectMapper实例的途径。</p>
     *
     * <p><b>使用建议：</b></p>
     * <p>除非你确实需要ObjectMapper的高级功能，否则建议使用工具类
     * 提供的其他方法。直接使用ObjectMapper需要处理异常和配置细节。</p>
     *
     * @return 配置好的ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}