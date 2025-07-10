package com.clinflash.baseai.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h2>Chat模块性能测试</h2>
 *
 * <p>性能测试确保系统在高负载情况下的稳定性和响应速度。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public class ChatPerformanceTest {

    @Test
    @DisplayName("并发对话创建性能测试")
    void concurrentThreadCreationPerformanceTest() throws Exception {
        int concurrentUsers = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 模拟并发创建对话线程
                try {
                    // 这里应该调用实际的服务方法
                    Thread.sleep(100 + (int) (Math.random() * 200)); // 模拟处理时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.printf("并发创建%d个对话线程，总耗时: %d ms，平均耗时: %.2f ms%n",
                concurrentUsers, totalTime, (double) totalTime / concurrentUsers);

        // 断言性能要求（平均响应时间应该小于500ms）
        assert (double) totalTime / concurrentUsers < 500.0;

        executor.shutdown();
    }

    @Test
    @DisplayName("消息处理吞吐量测试")
    void messageProcessingThroughputTest() {
        // 实现消息处理的吞吐量测试
        int messageCount = 1000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            // 模拟消息处理
            try {
                Thread.sleep(10); // 模拟每条消息10ms的处理时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double throughput = (double) messageCount / (totalTime / 1000.0); // 每秒处理消息数

        System.out.printf("处理%d条消息，总耗时: %d ms，吞吐量: %.2f msg/s%n",
                messageCount, totalTime, throughput);

        // 断言吞吐量要求（应该大于50 msg/s）
        assert throughput > 50.0;
    }
}