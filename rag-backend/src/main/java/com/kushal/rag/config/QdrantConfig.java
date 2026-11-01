package com.kushal.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

@Configuration
public class QdrantConfig {

	@Bean
	public QdrantClient qdrantClient(@Value("${qdrant.host}") String host, @Value("${qdrant.port}") int port,
			@Value("${qdrant.tls:false}") boolean useTls, @Value("${qdrant.api-key:}") String apiKey) {

		// Local Docker Qdrant: no TLS, no API key.
		// Qdrant Cloud: TLS on, API key required.
		QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, useTls);

		if (apiKey != null && !apiKey.isBlank()) {
			builder.withApiKey(apiKey);
		}

		return new QdrantClient(builder.build());
	}
}
