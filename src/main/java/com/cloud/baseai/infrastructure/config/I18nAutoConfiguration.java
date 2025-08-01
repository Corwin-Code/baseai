package com.cloud.baseai.infrastructure.config;

import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * <h2>国际化与错误消息配置</h2>
 *
 * <p>职责：</p>
 * <ul>
 *   <li>配置 {@link MessageSource}，统一加载 i18n 资源</li>
 *   <li>配置 {@link LocaleResolver} 与 {@link LocaleChangeInterceptor}，
 *   实现“参数 → Cookie → Header → 默认”顺序的语言选择</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(MessageSourceProperties.class)
public class I18nAutoConfiguration implements WebMvcConfigurer {

    private final MessageSourceProperties msgProps;

    public I18nAutoConfiguration(MessageSourceProperties msgProps) {
        this.msgProps = msgProps;
    }

    /**
     * 使用 ReloadableResourceBundleMessageSource，支持在开发阶段热加载。
     *
     * @return 主消息源
     */
    @Bean
    @Primary
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        messageSource.setBasenames(msgProps.getBasename().toArray(new String[0]));

        messageSource.setDefaultEncoding(msgProps.getEncoding().name());

        Duration cache = msgProps.getCacheDuration();
        messageSource.setCacheMillis(cache == null ? -1 : cache.toMillis()); // 缓存1小时

        messageSource.setFallbackToSystemLocale(msgProps.isFallbackToSystemLocale());
        messageSource.setAlwaysUseMessageFormat(msgProps.isAlwaysUseMessageFormat());

        // 找不到 code 时是否直接返回 code（按需开启）
        messageSource.setUseCodeAsDefaultMessage(false);
        return messageSource;
    }

    /**
     * 配置语言环境解析器
     * 解析顺序：请求参数(lang)写入 → 下次读取 Cookie → 否则 Accept-Language → 默认 zh_CN
     */
    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");
        resolver.setDefaultLocaleFunction(req -> Locale.SIMPLIFIED_CHINESE);
        resolver.setCookieMaxAge(Duration.ofDays(30)); // 30 天
        return resolver;
    }

    /**
     * 配置语言环境拦截器，允许通过 ?lang=xx 切换语言
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}