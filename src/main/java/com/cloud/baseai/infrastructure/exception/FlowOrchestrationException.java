package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>流程编排模块专用异常类</h2>
 */
public class FlowOrchestrationException extends BusinessException {

    public FlowOrchestrationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FlowOrchestrationException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public FlowOrchestrationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public FlowOrchestrationException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法

    public static FlowOrchestrationException flowNotFound(String flowId) {
        return new FlowOrchestrationException(ErrorCode.BIZ_FLOW_014, flowId);
    }

    public static FlowOrchestrationException projectNotFound(String projectId) {
        return new FlowOrchestrationException(ErrorCode.BIZ_FLOW_007, projectId);
    }

    public static FlowOrchestrationException invalidFlowStatus(String currentStatus, String requiredStatus) {
        return (FlowOrchestrationException) new FlowOrchestrationException(ErrorCode.BIZ_FLOW_003)
                .addContext("currentStatus", currentStatus)
                .addContext("requiredStatus", requiredStatus);
    }
}