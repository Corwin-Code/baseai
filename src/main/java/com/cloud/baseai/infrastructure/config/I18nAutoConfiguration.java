package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.*;

/**
 * <h2>国际化与错误消息配置</h2>
 *
 * <p>该配置类负责设置应用程序的国际化支持，包括消息源配置、
 * 语言环境解析和语言切换功能。</p>
 *
 * <p><b>配置职责：</b></p>
 * <ul>
 *   <li>配置 {@link MessageSource}，统一加载 i18n 资源</li>
 *   <li>配置 {@link LocaleResolver} 与 {@link LocaleChangeInterceptor}，
 *   实现"参数 → Cookie → Header → 默认"顺序的语言选择</li>
 *   <li>提供多语言资源的热重载支持</li>
 *   <li>支持自定义消息格式和回退机制</li>
 * </ul>
 */
@Configuration
public class I18nAutoConfiguration extends BaseAutoConfiguration implements WebMvcConfigurer {

    private final MessageSourceProperties msgProps;

    /**
     * 支持的语言列表
     */
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
            Locale.SIMPLIFIED_CHINESE,  // zh_CN
            Locale.TRADITIONAL_CHINESE, // zh_TW
            Locale.ENGLISH,             // en
            Locale.JAPANESE,            // ja
            Locale.KOREAN,              // ko
            Locale.FRENCH,              // fr
            Locale.GERMAN,              // de
            Locale.ITALIAN              // it
    );

    /**
     * 默认语言环境
     */
    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    public I18nAutoConfiguration(MessageSourceProperties msgProps) {
        this.msgProps = msgProps;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "国际化与多语言支持";
    }

    @Override
    protected String getModuleName() {
        return "I18N";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证国际化配置...");

        // 基础配置验证
        validateNotNull(msgProps, "消息源配置属性");
        validateNotEmpty(msgProps.getBasename(), "消息基础名称列表");
        validateNotNull(msgProps.getEncoding(), "消息编码");

        // 编码验证
        if (!"UTF-8".equalsIgnoreCase(msgProps.getEncoding().name())) {
            logWarning("消息编码不是UTF-8 (%s)，可能导致中文乱码", msgProps.getEncoding().name());
        }

        // 缓存配置验证
        Duration cacheDuration = msgProps.getCacheDuration();
        if (cacheDuration != null) {
            if (cacheDuration.isNegative()) {
                logInfo("消息缓存已禁用（开发模式）");
            } else if (cacheDuration.toMinutes() > 60) {
                logWarning("消息缓存时间较长 (%s)，资源文件更新可能不及时", cacheDuration);
            }
        }

        // 验证资源文件存在性
        validateMessageResources();

        // 验证语言环境设置
        validateLocaleSettings();

        logSuccess("国际化配置验证通过");
    }

    /**
     * 验证消息资源文件
     */
    private void validateMessageResources() {
        logInfo("检查消息资源文件...");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        int foundResources = 0;

        for (String basename : msgProps.getBasename()) {
            try {
                // 检查默认资源文件
                String defaultPattern = basename + ".properties";
                Resource[] defaultResources = resolver.getResources("classpath*:" + defaultPattern);

                if (defaultResources.length > 0) {
                    foundResources += defaultResources.length;
                    logInfo("✓ 发现默认消息资源: %s (%d个文件)", basename, defaultResources.length);
                } else {
                    logWarning("✗ 未发现默认消息资源: %s", basename);
                }

                // 检查语言特定资源文件
                for (Locale locale : SUPPORTED_LOCALES) {
                    String localePattern = basename + "_" + locale.toString() + ".properties";
                    Resource[] localeResources = resolver.getResources("classpath*:" + localePattern);
                    if (localeResources.length > 0) {
                        foundResources += localeResources.length;
                        logInfo("✓ 发现 %s 语言资源: %d个文件", locale.getDisplayName(), localeResources.length);
                    }
                }

            } catch (Exception e) {
                logWarning("检查资源文件时出错: %s - %s", basename, e.getMessage());
            }
        }

        if (foundResources == 0) {
            logWarning("未发现任何消息资源文件，国际化功能可能无法正常工作");
        } else {
            logInfo("总计发现 %d 个消息资源文件", foundResources);
        }
    }

    /**
     * 验证语言环境设置
     */
    private void validateLocaleSettings() {
        logInfo("验证语言环境设置...");

        // 检查系统默认语言环境
        Locale systemDefault = Locale.getDefault();
        logInfo("系统默认语言环境: %s (%s)", systemDefault.getDisplayName(), systemDefault.toString());

        // 检查是否在支持列表中
        boolean isSupported = SUPPORTED_LOCALES.contains(systemDefault) ||
                SUPPORTED_LOCALES.stream().anyMatch(locale ->
                        locale.getLanguage().equals(systemDefault.getLanguage()));

        if (isSupported) {
            logInfo("✓ 系统默认语言在支持列表中");
        } else {
            logInfo("系统默认语言不在支持列表中，将使用配置的默认语言: %s",
                    DEFAULT_LOCALE.getDisplayName());
        }

        // 输出支持的语言列表
        logInfo("支持的语言环境:");
        SUPPORTED_LOCALES.forEach(locale ->
                logInfo("  - %s (%s)", locale.getDisplayName(), locale.toString()));
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 基础配置
        summary.put("消息基础名称", String.join(", ", msgProps.getBasename()));
        summary.put("消息编码", msgProps.getEncoding().name());
        summary.put("系统回退", msgProps.isFallbackToSystemLocale() ? "启用" : "禁用");
        summary.put("消息格式", msgProps.isAlwaysUseMessageFormat() ? "始终使用" : "按需使用");

        // 缓存配置
        Duration cacheDuration = msgProps.getCacheDuration();
        if (cacheDuration == null || cacheDuration.isNegative()) {
            summary.put("消息缓存", "禁用（开发模式）");
        } else {
            summary.put("消息缓存时间", cacheDuration.toString());
        }

        // 语言环境配置
        summary.put("默认语言环境", DEFAULT_LOCALE.getDisplayName());
        summary.put("支持的语言数量", SUPPORTED_LOCALES.size());
        summary.put("系统当前语言", Locale.getDefault().getDisplayName());

        // Cookie配置
        summary.put("语言Cookie名称", "LOCALE");
        summary.put("Cookie有效期", "30天");
        summary.put("语言切换参数", "lang");

        return summary;
    }

    /**
     * 使用 ReloadableResourceBundleMessageSource，支持在开发阶段热加载。
     *
     * @return 主消息源
     */
    @Bean
    @Primary
    public MessageSource messageSource() {
        logBeanCreation("MessageSource", "可重载的消息源");

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // 设置基础名称
        String[] basenames = msgProps.getBasename().toArray(new String[0]);
        messageSource.setBasenames(basenames);
        logInfo("配置消息基础名称: %s", Arrays.toString(basenames));

        // 设置编码
        messageSource.setDefaultEncoding(msgProps.getEncoding().name());
        logInfo("消息编码: %s", msgProps.getEncoding().name());

        // 设置缓存时间
        Duration cache = msgProps.getCacheDuration();
        if (cache == null) {
            messageSource.setCacheMillis(-1); // 永久缓存
            logInfo("消息缓存: 永久缓存");
        } else if (cache.isNegative()) {
            messageSource.setCacheMillis(-1); // 禁用缓存（开发模式）
            logInfo("消息缓存: 已禁用（开发模式）");
        } else {
            messageSource.setCacheMillis(cache.toMillis());
            logInfo("消息缓存时间: %s", cache);
        }

        // 其他配置
        messageSource.setFallbackToSystemLocale(msgProps.isFallbackToSystemLocale());
        messageSource.setAlwaysUseMessageFormat(msgProps.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(false); // 找不到 code 时是否直接返回 code

        logInfo("消息源配置 - 系统回退: %s, 消息格式: %s",
                msgProps.isFallbackToSystemLocale() ? "启用" : "禁用",
                msgProps.isAlwaysUseMessageFormat() ? "始终使用" : "按需使用");

        logBeanSuccess("MessageSource");
        return messageSource;
    }

    /**
     * 配置语言环境解析器
     * 解析顺序：请求参数(lang)写入 → 下次读取 Cookie → 否则 Accept-Language → 默认 zh_CN
     */
    @Bean
    public LocaleResolver localeResolver() {
        logBeanCreation("LocaleResolver", "Cookie语言环境解析器");

        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");

        // 设置默认语言环境
        resolver.setDefaultLocaleFunction(request -> {
            // 优先使用Accept-Language头中支持的语言
            Enumeration<Locale> requestLocales = request.getLocales();
            while (requestLocales.hasMoreElements()) {
                Locale requestLocale = requestLocales.nextElement();
                for (Locale supportedLocale : SUPPORTED_LOCALES) {
                    if (supportedLocale.getLanguage().equals(requestLocale.getLanguage())) {
                        logInfo("使用请求头语言环境: %s", supportedLocale.getDisplayName());
                        return supportedLocale;
                    }
                }
            }

            // 回退到默认语言环境
            logInfo("使用默认语言环境: %s", DEFAULT_LOCALE.getDisplayName());
            return DEFAULT_LOCALE;
        });

        // Cookie有效期30天
        resolver.setCookieMaxAge(Duration.ofDays(30));

        // Cookie路径和安全设置
        resolver.setCookiePath("/");
        resolver.setCookieHttpOnly(true);
        // 生产环境建议启用
        // resolver.setCookieSecure(true);

        logInfo("语言环境解析器配置 - Cookie: LOCALE, 有效期: 30天, 默认: %s",
                DEFAULT_LOCALE.getDisplayName());

        logBeanSuccess("LocaleResolver");
        return resolver;
    }

    /**
     * 配置语言环境拦截器，允许通过 ?lang=xx 切换语言
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        logBeanCreation("LocaleChangeInterceptor", "语言切换拦截器");

        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");

        // 只在参数有效时才进行切换
        interceptor.setIgnoreInvalidLocale(true);

        logInfo("语言切换参数: lang (例如: ?lang=en)");
        logInfo("支持的语言代码: %s",
                SUPPORTED_LOCALES.stream()
                        .map(Locale::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));

        logBeanSuccess("LocaleChangeInterceptor");
        return interceptor;
    }

    /**
     * 注册语言切换拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        logInfo("语言切换拦截器已注册");
    }

    /**
     * 提供语言环境工具方法Bean
     */
    @Bean
    public LocaleUtils localeUtils() {
        logBeanCreation("LocaleUtils", "语言环境工具类");

        LocaleUtils utils = new LocaleUtils(SUPPORTED_LOCALES, DEFAULT_LOCALE);
        logBeanSuccess("LocaleUtils");

        return utils;
    }

    /**
     * 语言环境工具类
     */
    public record LocaleUtils(List<Locale> supportedLocales, Locale defaultLocale) {

        /**
         * 检查语言环境是否支持
         */
        public boolean isSupported(Locale locale) {
            return supportedLocales.contains(locale) ||
                    supportedLocales.stream().anyMatch(l -> l.getLanguage().equals(locale.getLanguage()));
        }

        /**
         * 获取支持的语言环境列表
         */
        @Override
        public List<Locale> supportedLocales() {
            return new ArrayList<>(supportedLocales);
        }

        /**
         * 获取默认语言环境
         */
        @Override
        public Locale defaultLocale() {
            return defaultLocale;
        }

        /**
         * 根据语言代码获取语言环境
         */
        public Optional<Locale> getLocaleByLanguageTag(String languageTag) {
            try {
                Locale locale = Locale.forLanguageTag(languageTag);
                return isSupported(locale) ? Optional.of(locale) : Optional.empty();
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        /**
         * 获取语言环境的显示名称
         */
        public String getDisplayName(Locale locale) {
            return locale.getDisplayName(locale); // 使用目标语言显示
        }

        /**
         * 获取所有支持语言的映射
         */
        public Map<String, String> getSupportedLanguageMap() {
            Map<String, String> map = new LinkedHashMap<>();
            for (Locale locale : supportedLocales) {
                map.put(locale.toString(), locale.getDisplayName());
            }
            return map;
        }
    }
}