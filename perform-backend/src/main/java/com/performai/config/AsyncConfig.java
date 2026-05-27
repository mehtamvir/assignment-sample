/**
 * Perform AI Backend — Async Configuration
 *
 * Defines a named, tunable thread pool executor used exclusively
 * for background analysis job processing.
 *
 * @author Md Ehteshamul Haque Tamvir
 * @email  mtamvir@gmail.com
 */
package com.performai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the async thread pool used by {@code @Async("analysisExecutor")}.
 * Pool settings:
 *   corePoolSize  — 4 threads always alive
 *   maxPoolSize   — up to 10 threads under load
 *   queueCapacity — up to 100 jobs queued before rejection
 */
@Configuration
public class AsyncConfig {

    /**
     * Creates and registers the named executor bean {@code "analysisExecutor"}.
     * Thread names are prefixed with {@code "analysis-worker-"} for easy identification
     * in logs and thread dumps.
     *
     * @return configured {@link Executor} instance
     */
    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analysis-worker-");
        executor.initialize();
        return executor;
    }
}
