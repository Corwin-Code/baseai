package com.cloud.baseai.domain.flow.model;

/**
 * <h2>节点类型常量</h2>
 *
 * <p>定义系统支持的所有节点类型常量。</p>
 */
public final class NodeTypes {
    // 基础节点
    public static final String START = "START";
    public static final String END = "END";
    public static final String INPUT = "INPUT";
    public static final String OUTPUT = "OUTPUT";

    // AI节点
    public static final String RETRIEVER = "RETRIEVER";
    public static final String EMBEDDER = "EMBEDDER";
    public static final String LLM = "LLM";
    public static final String CHAT = "CHAT";
    public static final String CLASSIFIER = "CLASSIFIER";

    // 流程控制节点
    public static final String CONDITION = "CONDITION";
    public static final String LOOP = "LOOP";
    public static final String PARALLEL = "PARALLEL";
    public static final String SWITCH = "SWITCH";

    // 工具节点
    public static final String TOOL = "TOOL";
    public static final String HTTP = "HTTP";
    public static final String SCRIPT = "SCRIPT";

    // 数据处理节点
    public static final String MAPPER = "MAPPER";
    public static final String FILTER = "FILTER";
    public static final String VALIDATOR = "VALIDATOR";
    public static final String SPLITTER = "SPLITTER";
    public static final String MERGER = "MERGER";

    private NodeTypes() {
        // 工具类，不允许实例化
    }
}