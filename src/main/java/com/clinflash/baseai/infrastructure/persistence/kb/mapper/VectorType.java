package com.clinflash.baseai.infrastructure.persistence.kb.mapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * <h2>PostgreSQL向量类型转换器</h2>
 *
 * <p>这个转换器负责在Java的float[]数组和PostgreSQL的vector类型之间进行转换。
 * PostgreSQL的pgvector扩展使用特定的文本格式来表示向量数据。</p>
 *
 * <p><b>转换格式：</b></p>
 * <ul>
 * <li>Java: float[] {0.1f, 0.2f, 0.3f}</li>
 * <li>PostgreSQL: '[0.1,0.2,0.3]'</li>
 * </ul>
 *
 * <p><b>使用方式：</b></p>
 * <p>在JPA实体类的字段上使用 @Convert(converter = VectorType.class) 注解。</p>
 */
@Converter
public class VectorType implements AttributeConverter<float[], String> {

    private static final String VECTOR_START = "[";
    private static final String VECTOR_END = "]";
    private static final String VECTOR_SEPARATOR = ",";

    private static final Logger log = LoggerFactory.getLogger(VectorType.class);

    /**
     * 将Java的float数组转换为数据库的varchar格式
     *
     * <p>这个方法在保存实体到数据库时被调用，将Java对象转换为数据库可以存储的格式。</p>
     *
     * @param attribute Java中的float数组
     * @return PostgreSQL vector类型的字符串表示
     */
    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }

        try {
            // 构建PostgreSQL向量格式: [0.1,0.2,0.3]
            StringBuilder sb = new StringBuilder(VECTOR_START);
            for (int i = 0; i < attribute.length; i++) {
                if (i > 0) {
                    sb.append(VECTOR_SEPARATOR);
                }
                sb.append(attribute[i]);
            }
            sb.append(VECTOR_END);

            return sb.toString();
        } catch (Exception e) {
            log.error("向量转换为数据库格式失败", e);
            throw new RuntimeException("向量序列化失败", e);
        }
    }

    /**
     * 将数据库的varchar格式转换为Java的float数组
     *
     * <p>这个方法在从数据库读取数据时被调用，将数据库格式转换为Java对象。</p>
     *
     * @param dbData 数据库中的字符串数据
     * @return Java的float数组
     * @throws IllegalArgumentException 如果数据格式不正确
     */
    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new float[0];
        }

        // 移除首尾的方括号
        String vectorData = dbData.trim();
        if (!vectorData.startsWith(VECTOR_START) || !vectorData.endsWith(VECTOR_END)) {
            throw new IllegalArgumentException("无效的向量格式，期望格式: [1.0,2.0,3.0]，实际: " + dbData);
        }

        vectorData = vectorData.substring(1, vectorData.length() - 1);

        // 处理空向量的情况
        if (vectorData.trim().isEmpty()) {
            return new float[0];
        }

        // 分割并转换为float数组
        String[] parts = vectorData.split(VECTOR_SEPARATOR);
        float[] result = new float[parts.length];

        try {
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("向量数据包含无效的数值: " + dbData, e);
        }

        return result;
    }

    /**
     * 验证向量数据的有效性
     *
     * @param vector 要验证的向量
     * @throws IllegalArgumentException 如果向量无效
     */
    public static void validateVector(float[] vector) {
        if (vector == null) {
            throw new IllegalArgumentException("向量不能为null");
        }

        if (vector.length == 0) {
            throw new IllegalArgumentException("向量不能为空");
        }

        // 检查是否包含无效值
        for (int i = 0; i < vector.length; i++) {
            float value = vector[i];
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                throw new IllegalArgumentException(
                        String.format("向量第%d个元素包含无效值: %f", i, value));
            }
        }
    }

    /**
     * 计算向量的L2范数（模长）
     *
     * @param vector 输入向量
     * @return 向量的L2范数
     */
    public static double calculateNorm(float[] vector) {
        if (vector == null || vector.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    /**
     * 归一化向量
     *
     * <p>将向量转换为单位向量，保持方向不变但模长为1。</p>
     *
     * @param vector 输入向量
     * @return 归一化后的向量
     */
    public static float[] normalize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return vector;
        }

        double norm = calculateNorm(vector);
        if (norm == 0.0) {
            return vector; // 零向量无法归一化
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }

        return normalized;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 余弦相似度值，范围[-1, 1]
     * @throws IllegalArgumentException 如果向量维度不匹配
     */
    public static double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null) {
            throw new IllegalArgumentException("向量不能为null");
        }

        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度必须相同");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0; // 零向量的相似度定义为0
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 向量数组的深度复制
     *
     * @param original 原始向量数组
     * @return 复制的向量数组
     */
    public static float[] deepCopy(float[] original) {
        if (original == null) {
            return null;
        }
        return Arrays.copyOf(original, original.length);
    }
}