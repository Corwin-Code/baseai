package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.AsyncProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <h2>异步处理配置类</h2>
 *
 * <p>该配置类负责设置应用程序的异步处理能力，所有的执行器
 * 都基于 {@link AsyncProperties} 中定义的配置参数，确保了配置的一致性和可维护性。</p>
 */
@EnableAsync
@Configuration
public class AsyncAutoConfiguration extends BaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AsyncAutoConfiguration.class);

    /**
     * 异步配置属性
     */
    private final AsyncProperties asyncProps;

    /**
     * 构造函数注入异步配置属性
     *
     * @param asyncProps 异步配置属性实例
     */
    public AsyncAutoConfiguration(AsyncProperties asyncProps) {
        this.asyncProps = asyncProps;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "异步任务处理";
    }

    @Override
    protected String getModuleName() {
        return "ASYNC";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证异步配置参数...");

        // 基础参数验证
        validateNotNull(asyncProps.getCorePoolSize(), "核心线程池大小");
        validateNotNull(asyncProps.getMaxPoolSize(), "最大线程池大小");
        validateNotNull(asyncProps.getQueueCapacity(), "队列容量");
        validateNotNull(asyncProps.getKeepAliveSeconds(), "线程存活时间");
        validateNotBlank(asyncProps.getThreadNamePrefix(), "线程名称前缀");
        validateNotBlank(asyncProps.getRejectionPolicy(), "拒绝策略");

        // 数值范围验证
        validateRange(asyncProps.getCorePoolSize(), 1, 1000, "核心线程池大小");
        validateRange(asyncProps.getQueueCapacity(), 10, 100000, "队列容量");
        validateRange(asyncProps.getKeepAliveSeconds(), 10, 3600, "线程存活时间");

        // 逻辑关系验证
        validateCombination(
                asyncProps.getMaxPoolSize() >= asyncProps.getCorePoolSize(),
                "最大线程池大小必须大于等于核心线程池大小"
        );

        // 拒绝策略验证
        List<String> validPolicies = List.of("ABORT", "CALLER_RUNS", "DISCARD", "DISCARD_OLDEST");
        validateEnum(asyncProps.getRejectionPolicy().toUpperCase(), validPolicies, "拒绝策略");

        // 性能建议
        performanceRecommendations();

        logSuccess("异步配置验证通过");
    }

    /**
     * 性能建议
     */
    private void performanceRecommendations() {
        int corePoolSize = asyncProps.getCorePoolSize();
        int maxPoolSize = asyncProps.getMaxPoolSize();
        int queueCapacity = asyncProps.getQueueCapacity();

        // CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();

        if (corePoolSize > cpuCores * 2) {
            logWarning("核心线程数 (%d) 超过CPU核心数的2倍 (%d)，可能导致过多的上下文切换",
                    corePoolSize, cpuCores * 2);
        }

        if (maxPoolSize > cpuCores * 4) {
            logWarning("最大线程数 (%d) 超过CPU核心数的4倍 (%d)，建议根据业务类型调整",
                    maxPoolSize, cpuCores * 4);
        }

        if (queueCapacity < 100) {
            logWarning("队列容量 (%d) 较小，高并发时可能频繁触发拒绝策略", queueCapacity);
        }

        logInfo("当前系统CPU核心数: %d，建议配置参考值已输出", cpuCores);
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("核心线程池大小", asyncProps.getCorePoolSize());
        summary.put("最大线程池大小", asyncProps.getMaxPoolSize());
        summary.put("队列容量", asyncProps.getQueueCapacity());
        summary.put("线程名称前缀", asyncProps.getThreadNamePrefix());
        summary.put("线程存活时间(秒)", asyncProps.getKeepAliveSeconds());
        summary.put("允许核心线程超时", asyncProps.getAllowCoreThreadTimeout());
        summary.put("拒绝策略", asyncProps.getRejectionPolicy());
        summary.put("系统CPU核心数", Runtime.getRuntime().availableProcessors());
        return summary;
    }

    /**
     * <h3>创建主要的异步任务执行器</h3>
     *
     * <p>这是应用程序的主要异步执行器，用于处理大部分的异步任务。
     * 该执行器被标记为 {@code @Primary}，意味着当没有明确指定执行器名称时，
     * Spring 会默认使用这个执行器。</p>
     *
     * <h4>配置特点：</h4>
     * <ul>
     *   <li>使用标准的线程池配置</li>
     *   <li>支持安全上下文传播</li>
     *   <li>具有合理的默认拒绝策略</li>
     * </ul>
     *
     * @return 配置好的任务执行器
     */
    @Primary
    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        logBeanCreation("taskExecutor", "主要异步任务执行器");

        ThreadPoolTaskExecutor executor = createBaseExecutor("taskExecutor");

        // 使用通用的线程名前缀
        executor.setThreadNamePrefix(asyncProps.getThreadNamePrefix());

        logInfo("主执行器配置 - 核心线程: %d, 最大线程: %d, 队列容量: %d",
                asyncProps.getCorePoolSize(), asyncProps.getMaxPoolSize(), asyncProps.getQueueCapacity());

        AsyncTaskExecutor securityAwareExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);
        logBeanSuccess("taskExecutor");

        return securityAwareExecutor;
    }

    /**
     * <h3>创建Chat模块专用的异步执行器</h3>
     *
     * <p>这个执行器专门用于处理Chat相关的异步任务。
     * 该执行器采用了针对Chat场景优化的配置：较小的核心线程数以降低资源消耗，
     * 但具有较高的最大线程数以应对突发的并发需求。</p>
     *
     * @return 配置好的Chat异步任务执行器
     */
    @Bean(name = "chatAsyncExecutor")
    public AsyncTaskExecutor chatAsyncExecutor() {
        logBeanCreation("chatAsyncExecutor", "Chat模块专用异步执行器");

        ThreadPoolTaskExecutor executor = createBaseExecutor("chatAsyncExecutor");

        // 使用较大的最大线程数，以应对聊天高峰期的并发需求
        int chatMaxPoolSize = Math.max(20, asyncProps.getMaxPoolSize());
        // 使用较小的核心线程数，因为大部分时间Chat流量相对稳定
        int chatCorePoolSize = Math.max(5, asyncProps.getCorePoolSize() / 2);
        // Chat场景的队列容量：平衡内存使用和响应性
        int chatQueueCapacity = Math.min(100, asyncProps.getQueueCapacity());

        executor.setMaxPoolSize(chatMaxPoolSize);
        executor.setCorePoolSize(chatCorePoolSize);
        executor.setQueueCapacity(chatQueueCapacity);

        // Chat专用的线程名前缀，便于监控和调试
        executor.setThreadNamePrefix("BaseAI-Chat-Async-");
        // Chat任务使用调用者运行策略，确保聊天请求不会被丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        logInfo("Chat执行器配置 - 核心线程: %d, 最大线程: %d, 队列容量: %d",
                chatCorePoolSize, chatMaxPoolSize, chatQueueCapacity);

        // 包装执行器以支持安全上下文传播，确保Chat操作的安全性
        AsyncTaskExecutor securityAwareExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);
        logBeanSuccess("chatAsyncExecutor");

        return securityAwareExecutor;
    }

    /**
     * <h3>创建文件处理专用的异步执行器</h3>
     *
     * <p>这个执行器专门用于处理文件相关的异步操作。文件处理具有典型的IO密集型特征：</p>
     * <ul>
     *   <li><b>IO等待时间长</b>：大量时间花费在磁盘读写和网络传输上</li>
     *   <li><b>CPU使用率低</b>：线程多数时间处于阻塞等待状态</li>
     *   <li><b>可高并发</b>：由于线程阻塞，可以配置较多线程数</li>
     *   <li><b>操作耗时不定</b>：文件大小和网络状况影响处理时间</li>
     * </ul>
     *
     * <p>文件处理的主要应用场景包括：</p>
     * <ul>
     *   <li>用户文件上传的异步处理和存储</li>
     *   <li>文档格式转换（PDF、Word、Excel等）</li>
     *   <li>图片和视频的异步压缩、缩放</li>
     *   <li>大文件的分片上传和合并</li>
     *   <li>文件内容的异步扫描和病毒检测</li>
     *   <li>批量文件的打包和解压操作</li>
     *   <li>文件元数据的提取和索引</li>
     * </ul>
     *
     * <p>针对IO密集型特点，该执行器采用了以下优化策略：</p>
     * <ul>
     *   <li>较高的线程数配置，充分利用IO等待时间</li>
     *   <li>较大的队列容量，应对文件处理的不确定性</li>
     *   <li>使用调用者运行策略，确保重要文件操作不被丢弃</li>
     *   <li>较长的线程存活时间，减少线程创建销毁开销</li>
     * </ul>
     *
     * @return 配置好的文件处理异步任务执行器
     */
    @Bean(name = "fileProcessingExecutor")
    public AsyncTaskExecutor fileProcessingExecutor() {
        logBeanCreation("fileProcessingExecutor", "文件处理专用异步执行器(IO密集型优化)");

        ThreadPoolTaskExecutor executor = createBaseExecutor("fileProcessingExecutor");

        // IO密集型优化配置
        // 最大线程数：IO密集型可以设置更高，通常为CPU核心数的4-6倍
        int fileMaxPoolSize = Math.max(50, asyncProps.getMaxPoolSize() * 2);
        // 核心线程数：考虑到IO等待，可以设置为CPU核心数的2-3倍
        int fileCorePoolSize = Math.max(10, asyncProps.getCorePoolSize() * 2);
        // 队列容量：文件处理可能耗时较长，设置较大队列容纳更多任务
        int fileQueueCapacity = Math.max(200, asyncProps.getQueueCapacity() * 2);

        executor.setMaxPoolSize(fileMaxPoolSize);
        executor.setCorePoolSize(fileCorePoolSize);
        executor.setQueueCapacity(fileQueueCapacity);

        // 文件处理专用的线程名前缀，便于监控文件操作
        executor.setThreadNamePrefix("BaseAI-File-Async-");
        // 线程存活时间：IO密集型任务间隔可能较长，延长线程存活时间
        executor.setKeepAliveSeconds(Math.max(300, asyncProps.getKeepAliveSeconds()));
        // 文件处理使用调用者运行策略，确保重要的文件操作不会被丢弃，特别是用户上传的文件，丢失会严重影响用户体验
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        logInfo("文件处理执行器配置 - 核心线程: %d, 最大线程: %d, 队列容量: %d, 存活时间: %d秒",
                fileCorePoolSize, fileMaxPoolSize, fileQueueCapacity, executor.getKeepAliveSeconds());

        // 包装执行器以支持安全上下文传播，确保文件操作的权限控制
        AsyncTaskExecutor securityAwareExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);
        logBeanSuccess("fileProcessingExecutor");

        return securityAwareExecutor;
    }

    /**
     * <h3>创建模板处理专用的异步执行器</h3>
     *
     * <p>这个执行器专门用于处理模板相关的异步操作。模板处理具有典型的CPU密集型特征：</p>
     * <ul>
     *   <li><b>CPU计算密集</b>：模板解析、变量替换、表达式计算消耗大量CPU</li>
     *   <li><b>内存使用较高</b>：需要加载模板内容和构建语法树</li>
     *   <li><b>执行时间相对固定</b>：主要取决于模板复杂度而非外部IO</li>
     *   <li><b>并发受限</b>：受CPU核心数限制，过多线程反而降低性能</li>
     * </ul>
     *
     * <p>模板处理的主要应用场景包括：</p>
     * <ul>
     *   <li>动态邮件模板的异步渲染和发送</li>
     *   <li>报表模板的数据填充和格式化</li>
     *   <li>代码模板的异步生成和编译</li>
     *   <li>文档模板的批量生成（合同、发票等）</li>
     *   <li>配置文件模板的动态生成</li>
     *   <li>网页模板的预渲染和缓存</li>
     *   <li>多语言模板的本地化处理</li>
     * </ul>
     *
     * <p>针对CPU密集型特点，该执行器采用了以下优化策略：</p>
     * <ul>
     *   <li>保守的线程数配置，避免过度的上下文切换</li>
     *   <li>适中的队列容量，平衡内存使用和响应性</li>
     *   <li>使用中止策略，在系统过载时快速失败</li>
     *   <li>较短的线程存活时间，及时释放资源</li>
     * </ul>
     *
     * @return 配置好的模板处理异步任务执行器
     */
    @Bean(name = "templateProcessingExecutor")
    public AsyncTaskExecutor templateProcessingExecutor() {
        logBeanCreation("templateProcessingExecutor", "模板处理专用异步执行器(CPU密集型优化)");

        ThreadPoolTaskExecutor executor = createBaseExecutor("templateProcessingExecutor");

        // CPU密集型优化配置
        // 最大线程数：对于CPU密集型，不宜设置过高，通常为核心线程数的1.5-2倍
        int templateMaxPoolSize = Math.max(8, (int) (asyncProps.getMaxPoolSize() * 1.5));
        // 核心线程数：CPU密集型通常设置为CPU核心数或略少，避免过度竞争
        int templateCorePoolSize = Math.max(4, asyncProps.getCorePoolSize());
        // 队列容量：CPU密集型任务执行时间相对可预测，适中的队列即可
        int templateQueueCapacity = Math.max(50, asyncProps.getQueueCapacity());

        executor.setMaxPoolSize(templateMaxPoolSize);
        executor.setCorePoolSize(templateCorePoolSize);
        executor.setQueueCapacity(templateQueueCapacity);

        // 模板处理专用的线程名前缀，便于监控模板操作性能
        executor.setThreadNamePrefix("BaseAI-Template-Async-");
        // 线程存活时间：CPU密集型任务通常连续性较好，可以使用较短的存活时间
        executor.setKeepAliveSeconds(Math.min(60, asyncProps.getKeepAliveSeconds()));
        // 模板处理使用中止策略：当系统过载时，快速失败比延迟处理更好，这样可以及时发现性能瓶颈并进行优化
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        logInfo("模板处理执行器配置 - 核心线程: %d, 最大线程: %d, 队列容量: %d, 存活时间: %d秒",
                templateCorePoolSize, templateMaxPoolSize, templateQueueCapacity, executor.getKeepAliveSeconds());

        // 包装执行器以支持安全上下文传播，确保模板处理中的权限检查
        AsyncTaskExecutor securityAwareExecutor = new DelegatingSecurityContextAsyncTaskExecutor(executor);
        logBeanSuccess("templateProcessingExecutor");

        return securityAwareExecutor;
    }

    /**
     * <h3>创建审计专用的异步执行器</h3>
     *
     * <p>这个执行器专门用于处理审计相关的异步任务。审计任务具有以下特殊要求：</p>
     * <ul>
     *   <li><b>高可靠性</b>：审计数据不能丢失</li>
     *   <li><b>安全隔离</b>：与其他业务线程池隔离</li>
     *   <li><b>上下文保持</b>：保持用户和安全上下文信息</li>
     * </ul>
     *
     * <p>该执行器采用了更保守的配置策略，包括使用调用者运行策略
     * 来确保在线程池满载时任务仍能被执行。</p>
     *
     * @return 配置好的审计任务执行器
     */
    @Bean("auditTaskExecutor")
    @ConditionalOnMissingBean(name = "auditTaskExecutor")
    @ConditionalOnProperty(prefix = "baseai.async", name = "logging.enable-async", havingValue = "true", matchIfMissing = true)
    public AsyncTaskExecutor auditTaskExecutor() {
        logBeanCreation("auditTaskExecutor", "审计专用异步执行器");

        ThreadPoolTaskExecutor executor = createBaseExecutor("auditTaskExecutor");

        // 审计专用的线程名前缀
        executor.setThreadNamePrefix("BaseAI-Audit-Async-");
        // 审计任务使用更保守的拒绝策略，确保任务不会丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        logInfo("审计执行器使用保守配置确保数据不丢失");

        // 包装执行器以支持安全上下文传播和审计上下文复制
        AsyncTaskExecutor auditExecutor = new DelegatingSecurityContextAsyncTaskExecutor(
                new AuditContextCopyingTaskExecutor(executor));

        logBeanSuccess("auditTaskExecutor");
        return auditExecutor;
    }

    /**
     * <h3>创建流程编排专用的异步执行器</h3>
     *
     * <p>这个执行器专门用于处理流程编排相关的异步任务。流程编排采用了以下策略：</p>
     * <ul>
     *   <li>中等偏高的线程数配置：平衡IO等待和CPU计算的需求</li>
     *   <li>较大的队列容量：容纳更多长时间运行的流程任务</li>
     *   <li>较长的线程存活时间：适应流程间隔可能较长的特点</li>
     *   <li>调用者运行策略：确保重要业务流程不会被丢弃</li>
     *   <li>优雅关闭支持：保证流程完整性，避免数据不一致</li>
     *   <li>安全上下文传播：确保流程执行过程中的权限控制</li>
     * </ul>
     *
     * @return 配置好的流程编排异步任务执行器
     */
    @Bean(name = "flowAsyncExecutor")
    public AsyncTaskExecutor flowAsyncExecutor() {
        logBeanCreation("flowAsyncExecutor", "流程编排专用异步执行器");

        ThreadPoolTaskExecutor executor = createBaseExecutor("flowAsyncExecutor");

        // 流程编排优化配置
        // 最大线程数：流程编排可能出现突发的并发需求，设置为核心线程数的2-3倍，这样可以应对流程高峰期，同时避免系统资源过度消耗
        int flowMaxPoolSize = Math.max(24, asyncProps.getMaxPoolSize() * 3);
        // 核心线程数：考虑到流程编排的混合特性（IO + CPU），设置为CPU核心数的1.5-2倍，既能处理IO等待，又不会因线程过多导致上下文切换开销
        int flowCorePoolSize = Math.max(8, (int) (asyncProps.getCorePoolSize() * 1.5));
        // 队列容量：流程编排任务通常执行时间较长且数量可能较多，设置较大的队列容量以容纳更多待执行的流程
        int flowQueueCapacity = Math.max(500, asyncProps.getQueueCapacity() * 3);

        executor.setMaxPoolSize(flowMaxPoolSize);
        executor.setCorePoolSize(flowCorePoolSize);
        executor.setQueueCapacity(flowQueueCapacity);

        // 流程编排专用的线程名前缀，便于监控和调试流程执行情况
        executor.setThreadNamePrefix("BaseAI-Flow-Async-");
        // 线程存活时间：流程编排任务间隔可能较长，延长线程存活时间减少创建销毁开销，同时考虑到流程可能有定时触发的特点
        executor.setKeepAliveSeconds(Math.max(600, asyncProps.getKeepAliveSeconds() * 2));
        // 流程编排使用调用者运行策略：确保重要的业务流程不会被丢弃，特别是关键业务流程，丢失可能导致业务中断和数据不一致
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 允许核心线程超时：在流程空闲期间可以释放核心线程，节约系统资源
        executor.setAllowCoreThreadTimeOut(true);
        // 优雅关闭配置：给流程充足的时间完成当前步骤，避免数据不一致
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 流程编排需要更长的关闭等待时间
        executor.setAwaitTerminationSeconds(120);

        logInfo("流程编排执行器配置 - 核心线程: %d, 最大线程: %d, 队列容量: %d, 存活时间: %d秒",
                flowCorePoolSize, flowMaxPoolSize, flowQueueCapacity, executor.getKeepAliveSeconds());

        // 包装执行器以支持安全上下文传播，确保流程执行过程中的权限控制，同时添加流程上下文复制功能，保持流程执行的完整性
        AsyncTaskExecutor flowExecutor = new DelegatingSecurityContextAsyncTaskExecutor(
                new FlowContextCopyingTaskExecutor(executor));

        logBeanSuccess("flowAsyncExecutor");
        return flowExecutor;
    }

    /**
     * <h3>创建基础的线程池执行器</h3>
     *
     * <p>这是一个私有方法，用于创建具有标准配置的 ThreadPoolTaskExecutor。
     * 所有的公共执行器 Bean 都基于这个方法创建，确保配置的一致性。</p>
     *
     * @param executorName 执行器名称，用于日志记录
     * @return 配置好的基础执行器
     */
    private ThreadPoolTaskExecutor createBaseExecutor(String executorName) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程池配置
        executor.setCorePoolSize(asyncProps.getCorePoolSize());
        executor.setMaxPoolSize(calculateMaxPoolSize());
        executor.setQueueCapacity(asyncProps.getQueueCapacity());

        // 线程生命周期配置
        executor.setKeepAliveSeconds(asyncProps.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(asyncProps.getAllowCoreThreadTimeout());

        // 优雅关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 拒绝策略配置
        executor.setRejectedExecutionHandler(createRejectionHandler());

        // 初始化执行器
        executor.initialize();

        logInfo("%s 基础配置完成", executorName);
        return executor;
    }

    /**
     * <h3>计算最大线程池大小</h3>
     *
     * <p>如果配置中的最大线程数小于等于0，则使用核心线程数的2倍作为最大线程数。
     * 这是一个常见的最佳实践，可以在保证性能的同时避免线程过多。</p>
     *
     * @return 计算后的最大线程池大小
     */
    private int calculateMaxPoolSize() {
        int maxPoolSize = asyncProps.getMaxPoolSize();
        if (maxPoolSize <= 0) {
            maxPoolSize = asyncProps.getCorePoolSize() * 2;
            logInfo("最大线程池大小未配置，使用计算值: %d", maxPoolSize);
        }
        return maxPoolSize;
    }

    /**
     * <h3>创建拒绝策略处理器</h3>
     *
     * <p>根据配置中的拒绝策略字符串创建对应的处理器。支持以下策略：</p>
     * <ul>
     *   <li><b>ABORT</b>：抛出异常（默认策略）</li>
     *   <li><b>CALLER_RUNS</b>：调用者线程执行</li>
     *   <li><b>DISCARD</b>：静默丢弃任务</li>
     *   <li><b>DISCARD_OLDEST</b>：丢弃最老的任务</li>
     * </ul>
     *
     * @return 对应的拒绝策略处理器
     */
    private RejectedExecutionHandler createRejectionHandler() {
        String policy = asyncProps.getRejectionPolicy();

        return switch (policy.toUpperCase()) {
            case "CALLER_RUNS" -> {
                logInfo("使用 CALLER_RUNS 拒绝策略 - 调用者线程执行");
                yield new ThreadPoolExecutor.CallerRunsPolicy();
            }
            case "DISCARD" -> {
                logInfo("使用 DISCARD 拒绝策略 - 静默丢弃任务");
                yield new ThreadPoolExecutor.DiscardPolicy();
            }
            case "DISCARD_OLDEST" -> {
                logInfo("使用 DISCARD_OLDEST 拒绝策略 - 丢弃最老任务");
                yield new ThreadPoolExecutor.DiscardOldestPolicy();
            }
            default -> {
                logInfo("使用 ABORT 拒绝策略 - 抛出异常（默认）");
                yield new ThreadPoolExecutor.AbortPolicy();
            }
        };
    }

    /**
     * <h3>应用关闭时的清理方法</h3>
     *
     * <p>在应用程序关闭时执行清理操作，确保所有线程池能够优雅地关闭。
     * 这个方法会在 Spring 容器销毁时自动调用。</p>
     */
    @PreDestroy
    public void shutdown() {
        logInfo("开始执行异步配置清理...");
        // Spring 会自动处理 Bean 的销毁，这里主要用于日志记录
        logSuccess("异步配置清理完成");
    }

    /**
     * <h2>审计上下文复制任务执行器</h2>
     *
     * <p>这是一个内部类，用于包装 ThreadPoolTaskExecutor，确保在执行审计任务时
     * 能够正确地复制和传播审计相关的上下文信息。</p>
     *
     * <p>审计上下文包括但不限于：</p>
     * <ul>
     *   <li>当前用户信息</li>
     *   <li>请求追踪ID</li>
     *   <li>操作时间戳</li>
     *   <li>客户端信息</li>
     * </ul>
     */
    private record AuditContextCopyingTaskExecutor(ThreadPoolTaskExecutor delegate) implements AsyncTaskExecutor {

        /**
         * 构造函数
         *
         * @param delegate 被包装的原始执行器
         */
        private AuditContextCopyingTaskExecutor {
        }

        /**
         * <h3>执行任务并复制上下文</h3>
         *
         * <p>在执行任务前，会复制当前线程的审计上下文到新线程中，
         * 确保审计信息的完整性和准确性。</p>
         *
         * @param task 要执行的任务
         */
        @Override
        public void execute(@NonNull Runnable task) {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            SecurityContext securityContext = SecurityContextHolder.getContext();
            delegate.execute(() -> {
                try {
                    // 在新线程中恢复审计上下文
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                    SecurityContextHolder.setContext(securityContext);
                    // 执行原始任务
                    task.run();
                } finally {
                    // 清理线程本地变量，避免内存泄漏
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            });
        }
    }

    /**
     * <h2>流程上下文复制任务执行器</h2>
     *
     * <p>这是一个内部类，用于包装 ThreadPoolTaskExecutor，确保在执行流程任务时
     * 能够正确地复制和传播流程相关的上下文信息。</p>
     */
    private record FlowContextCopyingTaskExecutor(ThreadPoolTaskExecutor delegate) implements AsyncTaskExecutor {

        /**
         * 构造函数
         *
         * @param delegate 被包装的原始执行器
         */
        private FlowContextCopyingTaskExecutor {
        }

        /**
         * <h3>执行任务并复制流程上下文</h3>
         *
         * <p>在执行流程任务前，会复制当前线程的所有相关上下文到新线程中，
         * 确保流程执行的完整性和数据一致性。</p>
         *
         * @param task 要执行的流程任务
         */
        @Override
        public void execute(@NonNull Runnable task) {
            // 复制当前线程的上下文信息
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            SecurityContext securityContext = SecurityContextHolder.getContext();

            // 这里可以扩展复制其他流程相关的上下文
            // 例如：FlowContext flowContext = FlowContextHolder.getContext();
            // 例如：TraceContext traceContext = TraceContextHolder.getContext();

            delegate.execute(() -> {
                try {
                    // 在新线程中恢复所有上下文信息
                    if (requestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(requestAttributes);
                    }
                    if (securityContext != null) {
                        SecurityContextHolder.setContext(securityContext);
                    }

                    // 恢复流程相关上下文
                    // FlowContextHolder.setContext(flowContext);
                    // TraceContextHolder.setContext(traceContext);

                    // 执行原始流程任务
                    task.run();

                } catch (Exception e) {
                    // 记录流程执行异常，便于问题排查
                    log.error("流程任务执行异常", e);
                    throw e;
                } finally {
                    // 清理线程本地变量，避免内存泄漏
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();

                    // 清理流程相关上下文
                    // FlowContextHolder.clearContext();
                    // TraceContextHolder.clearContext();
                }
            });
        }

        /**
         * <h3>提交任务并返回Future</h3>
         *
         * <p>支持异步任务的提交，返回Future对象用于获取执行结果。
         * 同样会进行上下文复制和清理。</p>
         *
         * @param task 要执行的任务
         * @return 任务执行的Future对象
         */
        @Override
        public Future<?> submit(@NonNull Runnable task) {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            SecurityContext securityContext = SecurityContextHolder.getContext();

            return delegate.submit(() -> {
                try {
                    if (requestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(requestAttributes);
                    }
                    if (securityContext != null) {
                        SecurityContextHolder.setContext(securityContext);
                    }
                    task.run();
                } catch (Exception e) {
                    log.error("流程任务执行异常", e);
                    throw e;
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            });
        }

        /**
         * <h3>提交Callable任务</h3>
         *
         * @param task 要执行的Callable任务
         * @param <T>  返回值类型
         * @return 任务执行的Future对象
         */
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            SecurityContext securityContext = SecurityContextHolder.getContext();

            return delegate.submit(() -> {
                try {
                    if (requestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(requestAttributes);
                    }
                    if (securityContext != null) {
                        SecurityContextHolder.setContext(securityContext);
                    }
                    return task.call();
                } catch (Exception e) {
                    log.error("流程任务执行异常", e);
                    throw e;
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    SecurityContextHolder.clearContext();
                }
            });
        }
    }
}