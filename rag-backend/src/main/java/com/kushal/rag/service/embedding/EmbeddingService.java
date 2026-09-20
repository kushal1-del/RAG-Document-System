package com.kushal.rag.service.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;

@Service
public class EmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
	private static final int MAX_RETRIES = 20;

	private final Client client;
	private final String embeddingModel;
	private final int embeddingDimensions;
	private final int batchSize;
	private final int concurrency;
	private final ExecutorService embeddingExecutor;

	public EmbeddingService(
			@Value("${gemini.api.key}") String apiKey,
			@Value("${gemini.embedding.model}") String embeddingModel,
			@Value("${gemini.embedding.dimensions}") int embeddingDimensions,
			@Value("${gemini.embedding.batch-size:5}") int batchSize,
			@Value("${embedding.concurrency:1}") int concurrency,
			ExecutorService embeddingExecutor) {

		this.client = Client.builder().apiKey(apiKey).build();
		this.embeddingModel = embeddingModel;
		this.embeddingDimensions = embeddingDimensions;
		this.batchSize = batchSize;
		this.concurrency = concurrency;
		this.embeddingExecutor = embeddingExecutor;

		log.info("EmbeddingService initialized: model={}, dimensions={}, batchSize={}, concurrency={}",
				embeddingModel, embeddingDimensions, batchSize, concurrency);
	}

	/**
	 * ONE embedding for ONE text — used for search/query embedding,
	 * where we only ever need to embed a single question.
	 */
	public float[] generateEmbedding(String text) {

		int attempt = 0;

		while (true) {
			try {
				return doGenerateEmbedding(text);

			} catch (Exception e) {

				attempt++;

				if (attempt >= MAX_RETRIES) {
					throw new RuntimeException(
							"Failed to generate embedding after " + MAX_RETRIES + " attempts", e);
				}

				long backoffMs = (long) Math.pow(2, attempt) * 1000;

				log.warn("Embedding call failed (attempt {}/{}), retrying in {}ms: {}",
						attempt, MAX_RETRIES, backoffMs, e.getMessage());

				sleep(backoffMs);
			}
		}
	}

	/**
	 * Generates embeddings for MANY texts — used during document upload.
	 *
	 * Splits texts into batches of `batchSize`, sends each batch as
	 * ONE Gemini API call (fewer round-trips than one-call-per-text),
	 * and runs up to `concurrency` batches at once.
	 *
	 * IMPORTANT: batching reduces HTTP overhead, but each individual
	 * text still counts against Gemini's daily/per-minute embedding
	 * quota — batching is a throughput improvement, not a quota
	 * bypass. For free-tier development, keep concurrency=1 and a
	 * modest batchSize to avoid tripping RPM/TPM limits.
	 */
	public List<float[]> generateEmbeddings(List<String> texts) {

		if (texts == null || texts.isEmpty()) {
			return List.of();
		}

		List<List<String>> batches = new ArrayList<>();

		for (int start = 0; start < texts.size(); start += batchSize) {
			int end = Math.min(start + batchSize, texts.size());
			batches.add(new ArrayList<>(texts.subList(start, end)));
		}

		log.info("Starting batch embedding: {} texts, {} batches, batchSize={}, concurrency={}",
				texts.size(), batches.size(), batchSize, concurrency);

		List<Future<List<float[]>>> futures = new ArrayList<>();

		for (int i = 0; i < batches.size(); i++) {

			final int batchNumber = i + 1;
			final List<String> batch = batches.get(i);

			Future<List<float[]>> future = embeddingExecutor.submit(() -> {

				log.info("Embedding batch {}/{} started ({} texts)", batchNumber, batches.size(), batch.size());

				List<float[]> result = generateEmbeddingBatch(batch);

				log.info("Embedding batch {}/{} completed: {} texts -> {} embeddings",
						batchNumber, batches.size(), batch.size(), result.size());

				return result;
			});

			futures.add(future);
		}

		List<float[]> allEmbeddings = new ArrayList<>();

		try {
			for (Future<List<float[]>> future : futures) {
				allEmbeddings.addAll(future.get());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed while waiting for embedding batches", e);
		}

		if (allEmbeddings.size() != texts.size()) {
			throw new RuntimeException(
					"Embedding count mismatch. Expected " + texts.size() + " but received " + allEmbeddings.size());
		}

		log.info("Batch embedding completed successfully: {} texts -> {} embeddings",
				texts.size(), allEmbeddings.size());

		return allEmbeddings;
	}

	/**
	 * ONE Gemini API call for MULTIPLE texts at once.
	 */
	private List<float[]> generateEmbeddingBatch(List<String> texts) {

		int attempt = 0;

		while (true) {
			try {

				EmbedContentResponse response = client.models.embedContent(
						embeddingModel,
						texts,
						EmbedContentConfig.builder().outputDimensionality(embeddingDimensions).build());

				List<ContentEmbedding> embeddings = response.embeddings()
						.orElseThrow(() -> new RuntimeException("No embeddings returned"));

				if (embeddings.size() != texts.size()) {
					throw new RuntimeException(
							"Gemini returned " + embeddings.size() + " embeddings for " + texts.size() + " texts");
				}

				List<float[]> result = new ArrayList<>();
                
				for (int i = 0; i < embeddings.size(); i++) {

				    final int index = i;

				    List<Float> values = embeddings.get(index).values()
				            .orElseThrow(() -> new RuntimeException("Embedding values missing for item " + index));

				    if (values.size() != embeddingDimensions) {
				        throw new RuntimeException("Embedding " + index + " has " + values.size()
				                + " dimensions. Expected " + embeddingDimensions);
				    }

				    float[] vector = new float[values.size()];
				    for (int j = 0; j < values.size(); j++) {
				        vector[j] = values.get(j);
				    }

				    result.add(vector);
				}

				return result;

			} catch (Exception e) {

				attempt++;

				if (attempt >= MAX_RETRIES) {
					throw new RuntimeException(
							"Failed to generate embedding batch after " + MAX_RETRIES + " attempts", e);
				}

				long backoffMs = (long) Math.pow(2, attempt) * 1000;

				log.warn("Embedding batch failed (attempt {}/{}), retrying in {}ms: {}",
						attempt, MAX_RETRIES, backoffMs, e.getMessage());

				sleep(backoffMs);
			}
		}
	}

	private float[] doGenerateEmbedding(String text) {

		List<ContentEmbedding> embeddings = client.models
				.embedContent(embeddingModel, text,
						EmbedContentConfig.builder().outputDimensionality(embeddingDimensions).build())
				.embeddings().orElseThrow(() -> new RuntimeException("No embedding returned"));

		if (embeddings.isEmpty()) {
			throw new RuntimeException("Embedding response is empty");
		}

		List<Float> values = embeddings.get(0).values()
				.orElseThrow(() -> new RuntimeException("Embedding values are missing"));

		if (values.size() != embeddingDimensions) {
			throw new RuntimeException(
					"Expected " + embeddingDimensions + " dimensions but received " + values.size());
		}

		float[] vector = new float[values.size()];
		for (int i = 0; i < values.size(); i++) {
			vector[i] = values.get(i);
		}
		return vector;
	}

	private void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while retrying embedding call", e);
		}
	}
}