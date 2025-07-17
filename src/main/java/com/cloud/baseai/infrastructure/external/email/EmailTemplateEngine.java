package com.cloud.baseai.infrastructure.external.email;

import java.util.Map;

/**
 * 邮件模板引擎接口
 *
 * <p>这是一个可选的组件，用于渲染复杂的HTML邮件模板。
 * 可以使用Thymeleaf、FreeMarker等模板引擎实现。</p>
 */
public interface EmailTemplateEngine {
    String renderTemplate(String templateName, Map<String, Object> params);

    String renderSubject(String templateName, Map<String, Object> params);
}