package com.cloud.baseai.infrastructure.i18n;

import com.cloud.baseai.infrastructure.exception.ErrorCode;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * <h2>消息管理器（异常可调用）</h2>
 *
 * <p>通过实现 {@link MessageSourceAware}，Spring 启动时会回调注入 MessageSource。
 * 这样避免了自己去拿 ApplicationContext 也避免 setter 过时问题。</p>
 */
@Component
public class MessageManager implements MessageSourceAware {

    private static MessageSource MESSAGE_SOURCE;

    @Override
    public void setMessageSource(@NonNull MessageSource messageSource) {
        MessageManager.MESSAGE_SOURCE = messageSource;
    }

    /**
     * 获取当前线程 Locale 下的消息
     *
     * @param code 消息代码
     * @param args 消息参数
     * @return 本地化后的消息
     */
    public static String getMessage(ErrorCode code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return getMessage(code, locale, args);
    }

    /**
     * 获取指定语言的消息
     *
     * @param code   消息代码
     * @param locale 语言环境
     * @param args   消息参数
     * @return 消息
     */
    public static String getMessage(ErrorCode code, Locale locale, Object... args) {
        Assert.notNull(MESSAGE_SOURCE, "MessageSource has not been initialized yet.");
        return MESSAGE_SOURCE.getMessage(code.getCode(), args, locale);
    }
}