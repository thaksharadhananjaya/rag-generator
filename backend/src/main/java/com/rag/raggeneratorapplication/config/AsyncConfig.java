package com.rag.raggeneratorapplication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables {@code @Async} and provides the executor used for document ingestion.
 * Ingestion methods select it explicitly with
 * {@code @Async(AsyncConfig.INGESTION_EXECUTOR)}.
 *
 * <p>Ingestion is deliberately kept simple: the upload request returns
 * {@code 202 Accepted} and the pipeline runs on this pool, tracking progress via
 * the {@code document.status} column. There is no outbox table or scheduled
 * poller. A restart mid-ingestion leaves the document in a non-terminal status;
 * recovery is an explicit re-ingest call (added with the document module).
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig {

    public static final String INGESTION_EXECUTOR = "ingestionExecutor";

    @Bean(INGESTION_EXECUTOR)
    public ThreadPoolTaskExecutor ingestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ingest-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
