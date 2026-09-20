package com.kushal.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

@Configuration
public class QdrantConfig {

	@Bean
	public QdrantClient qdrantClient(@Value("${qdrant.host}") String host, @Value("${qdrant.port}") int port) {

		QdrantGrpcClient grpcClient = QdrantGrpcClient.newBuilder(host, port, false).build();

		return new QdrantClient(grpcClient);
	}
}