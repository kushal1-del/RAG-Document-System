package com.kushal.rag.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a bounded thread pool for running embedding BATCHES
 * concurrently. This controls how many batch API calls run at
 * once — not how many individual chunks. For free-tier
 * development, keep this at 1 to avoid tripping RPM/TPM limits.
 */
@Configuration
public class EmbeddingExecutorConfig {

	@Bean
	public ExecutorService embeddingExecutor(@Value("${embedding.concurrency:1}") int concurrency) {
		return Executors.newFixedThreadPool(concurrency);
	}
}