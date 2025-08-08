package com.cloud.baseai.infrastructure.security.jwt;

import com.cloud.baseai.infrastructure.exception.ApiResult;
import com.cloud.baseai.infrastructure.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>JWT认证入口点</h1>
 *
 * <p>当一个未认证的用户尝试访问需要认证的资源时，
 * 这个类会礼貌地告诉用户"您需要先登录"，并提供清晰的错误信息和解决建议。</p>
 *
 * <p><b>为什么需要自定义认证入口点？</b></p>
 * <p>Spring Security的默认行为是重定向到登录页面，这对传统的Web应用很合适。
 * 但在现代的前后端分离架构中，后端API应该返回JSON格式的错误响应，而不是HTML页面。
 * 这个类就是为了解决这个问题。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>1. <strong>用户友好：</strong>提供清晰、易懂的错误信息</p>
 * <p>2. <strong>安全性：</strong>不泄露敏感的系统内部信息</p>
 * <p>3. <strong>一致性：</strong>与应用的其他API保持相同的响应格式</p>
 * <p>4. <strong>可调试性：</strong>为开发人员提供有用的调试信息</p>
 *
 * <p><b>认证失败的常见场景：</b></p>
 * <p>- JWT令牌缺失：用户没有提供认证令牌</p>
 * <p>- JWT令牌无效：令牌格式错误或签名不匹配</p>
 * <p>- JWT令牌过期：令牌已经超过有效期</p>
 * <p>- 用户账户异常：账户被禁用或锁定</p>
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入JSON序列化工具
     *
     * <p>我们使用ObjectMapper来将错误响应对象序列化为JSON格式。
     * 使用注入的方式确保与应用的其他部分使用相同的JSON配置。</p>
     */
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理认证异常的核心方法
     *
     * <p>当Spring Security检测到认证失败时，会调用这个方法。
     * 我们的任务是生成一个标准化的JSON错误响应，让前端能够
     * 正确地处理认证失败的情况。</p>
     *
     * <p><b>响应处理流程：</b></p>
     * <p>1. 分析认证异常的具体原因</p>
     * <p>2. 生成相应的错误代码和消息</p>
     * <p>3. 构建标准化的API响应对象</p>
     * <p>4. 设置正确的HTTP状态码和头部</p>
     * <p>5. 将响应写入HTTP输出流</p>
     *
     * @param request       导致认证异常的HTTP请求
     * @param response      HTTP响应对象，用于写入错误信息
     * @param authException 具体的认证异常
     * @throws IOException      写入响应时可能发生的IO异常
     * @throws ServletException Servlet处理异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIpAddress(request);

        log.warn("认证失败: method={}, uri={}, ip={}, error={}",
                method, requestURI, clientIp, authException.getMessage());

        // 分析认证失败的具体原因
        ErrorDetails errorDetails = analyzeAuthenticationFailure(authException, request);

        ErrorResponse responses = new ErrorResponse(errorDetails.errorCode, errorDetails.userMessage, errorDetails.details, System.currentTimeMillis(), null);
        ApiResult.error(responses);
        // 构建标准化的API错误响应
        ApiResult<Void> errorResponse = ApiResult.error(new ErrorResponse(
                errorDetails.errorCode,
                errorDetails.userMessage,
                errorDetails.details,
                System.currentTimeMillis(),
                null)
        );

        // 设置HTTP响应头
        response.setStatus(errorDetails.httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        try {
            // 将错误响应序列化为JSON并写入响应流
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();

        } catch (Exception e) {
            log.error("写入认证错误响应失败", e);
            // 如果JSON序列化失败，返回简单的文本响应
            response.getWriter().write("{\"success\":false,\"message\":\"认证失败\"}");
        }
    }

    /**
     * 分析认证失败的具体原因
     *
     * <p>不同类型的认证失败需要给用户不同的提示信息。这个方法
     * 根据异常类型和请求上下文，生成最合适的错误代码和消息。</p>
     *
     * <p><b>错误分类策略：</b></p>
     * <p>我们将认证错误分为几个主要类别，每个类别都有对应的
     * 错误代码和用户友好的消息。这样前端可以根据错误代码
     * 实现不同的处理逻辑。</p>
     */
    private ErrorDetails analyzeAuthenticationFailure(AuthenticationException authException,
                                                      HttpServletRequest request) {
        String exceptionMessage = authException.getMessage().toLowerCase();
        String authHeader = request.getHeader("Authorization");

        // 检查是否完全缺少认证信息
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return new ErrorDetails(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_TOKEN_MISSING",
                    "请提供有效的认证令牌",
                    createErrorDetails("缺少Authorization头部", request.getRequestURI())
            );
        }

        // 检查Authorization头部格式是否正确
        if (!authHeader.startsWith("Bearer ")) {
            return new ErrorDetails(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_TOKEN_INVALID_FORMAT",
                    "认证令牌格式错误，请使用Bearer格式",
                    createErrorDetails("无效的Authorization头部格式", request.getRequestURI())
            );
        }

        // 检查是否是令牌过期
        if (exceptionMessage.contains("expired") || exceptionMessage.contains("过期")) {
            return new ErrorDetails(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_TOKEN_EXPIRED",
                    "认证令牌已过期，请重新登录",
                    createErrorDetails("JWT令牌已过期", request.getRequestURI())
            );
        }

        // 检查是否是令牌无效
        if (exceptionMessage.contains("invalid") || exceptionMessage.contains("malformed")) {
            return new ErrorDetails(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_TOKEN_INVALID",
                    "认证令牌无效，请重新登录",
                    createErrorDetails("JWT令牌无效", request.getRequestURI())
            );
        }

        // 检查是否是账户问题
        if (exceptionMessage.contains("disabled") || exceptionMessage.contains("locked")) {
            return new ErrorDetails(
                    HttpServletResponse.SC_FORBIDDEN,
                    "ACCOUNT_DISABLED",
                    "账户已被禁用，请联系管理员",
                    createErrorDetails("用户账户状态异常", request.getRequestURI())
            );
        }

        // 检查是否是权限不足
        if (exceptionMessage.contains("access") || exceptionMessage.contains("forbidden")) {
            return new ErrorDetails(
                    HttpServletResponse.SC_FORBIDDEN,
                    "ACCESS_DENIED",
                    "权限不足，无法访问该资源",
                    createErrorDetails("访问被拒绝", request.getRequestURI())
            );
        }

        // 默认的认证失败响应
        return new ErrorDetails(
                HttpServletResponse.SC_UNAUTHORIZED,
                "AUTH_FAILED",
                "认证失败，请检查您的登录状态",
                createErrorDetails("认证异常", request.getRequestURI())
        );
    }

    /**
     * 创建错误详情对象
     *
     * <p>这个方法创建包含调试信息的错误详情Map。在开发环境中，
     * 这些信息对于问题排查很有帮助；在生产环境中，可以配置
     * 为不返回敏感的调试信息。</p>
     */
    private Map<String, Object> createErrorDetails(String errorType, String path) {
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", LocalDateTime.now().toString());
        details.put("path", path);
        details.put("errorType", errorType);

        // 在开发环境中可以添加更多调试信息
        // if (isDevelopmentMode()) {
        //     details.put("debugInfo", "更多调试信息");
        // }

        return details;
    }

    /**
     * 获取客户端真实IP地址
     *
     * <p>这个方法尝试获取客户端的真实IP地址，考虑了代理服务器和
     * 负载均衡器的情况。IP地址信息对于安全审计很重要。</p>
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 处理多个IP的情况
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 记录安全事件
     *
     * <p>认证失败是重要的安全事件，我们应该记录这些事件用于
     * 安全分析和威胁检测。这个方法可以扩展为向安全信息与
     * 事件管理系统(SIEM)发送告警。</p>
     */
    private void logSecurityEvent(HttpServletRequest request, AuthenticationException authException) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestURI = request.getRequestURI();

        // 构建安全事件日志
        Map<String, Object> securityEvent = new HashMap<>();
        securityEvent.put("eventType", "AUTHENTICATION_FAILURE");
        securityEvent.put("timestamp", LocalDateTime.now());
        securityEvent.put("clientIp", clientIp);
        securityEvent.put("userAgent", userAgent);
        securityEvent.put("requestURI", requestURI);
        securityEvent.put("errorMessage", authException.getMessage());

        // 这里可以发送到审计系统或安全监控系统
        log.warn("安全事件: {}", securityEvent);
    }

    /**
     * 错误详情内部类
     *
     * <p>这个内部类封装了错误响应的所有必要信息，包括HTTP状态码、
     * 错误代码、用户消息和详细信息。使用专门的类让代码更加清晰。</p>
     */
    private static class ErrorDetails {
        final int httpStatus;
        final String errorCode;
        final String userMessage;
        final Map<String, Object> details;

        ErrorDetails(int httpStatus, String errorCode, String userMessage, Map<String, Object> details) {
            this.httpStatus = httpStatus;
            this.errorCode = errorCode;
            this.userMessage = userMessage;
            this.details = details;
        }
    }

    /**
     * 生成建议的解决方案
     *
     * <p>基于不同的错误类型，我们可以为用户提供具体的解决建议。
     * 这大大提升了用户体验，减少了支持请求。</p>
     */
    private String generateSolution(String errorCode) {
        return switch (errorCode) {
            case "AUTH_TOKEN_MISSING" -> "请在请求头中添加Authorization字段，格式为: Bearer <your-token>";
            case "AUTH_TOKEN_EXPIRED" -> "请使用刷新令牌获取新的访问令牌，或重新登录";
            case "AUTH_TOKEN_INVALID" -> "请检查令牌是否正确，或尝试重新登录";
            case "ACCOUNT_DISABLED" -> "请联系系统管理员激活您的账户";
            case "ACCESS_DENIED" -> "请联系管理员申请相应的访问权限";
            default -> "请检查您的登录状态，必要时请重新登录";
        };
    }

    /**
     * 检查是否应该返回详细错误信息
     *
     * <p>在生产环境中，我们可能不希望返回过于详细的错误信息，
     * 以免泄露系统内部结构。这个方法可以根据环境配置决定
     * 错误信息的详细程度。</p>
     */
    private boolean shouldReturnDetailedError() {
        // 这里可以读取配置或环境变量
        // return "development".equals(environment);
        return true; // 简化实现，实际项目中应该基于配置
    }
}