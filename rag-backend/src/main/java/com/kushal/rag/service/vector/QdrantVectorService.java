package com.kushal.rag.service.vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.kushal.rag.model.DocumentChunk;
import com.kushal.rag.service.vector.SparseVectorService.SparseVectorData;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.Modifier;
import io.qdrant.client.grpc.Collections.SparseVectorConfig;
import io.qdrant.client.grpc.Collections.SparseVectorParams;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorParamsMap;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.NamedVectors;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SparseIndices;
import io.qdrant.client.grpc.Points.UpsertPoints;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.Vectors;

@Service
public class QdrantVectorService {
	private static final Logger log = LoggerFactory.getLogger(QdrantVectorService.class);
	private static final int UPSERT_BATCH_SIZE = 100;

	private final String collectionName;

	private final QdrantClient qdrantClient;

	private final SparseVectorService sparseVectorService;

	private final int embeddingDimensions;

	public QdrantVectorService(QdrantClient qdrantClient, SparseVectorService sparseVectorService,
			@org.springframework.beans.factory.annotation.Value("${qdrant.collection.name}") String collectionName,
			@org.springframework.beans.factory.annotation.Value("${gemini.embedding.dimensions}") int embeddingDimensions) {

		this.qdrantClient = qdrantClient;
		this.sparseVectorService = sparseVectorService;
		this.collectionName = collectionName;
		this.embeddingDimensions = embeddingDimensions;
	}

	// =========================================================
	// CREATE COLLECTION
	// =========================================================

	public void createCollection() {

		try {
			boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();

			if (exists) {

				log.info("Qdrant collection '{}' already exists.", collectionName);
				return;
			}

			VectorParams denseParams = VectorParams.newBuilder().setSize(embeddingDimensions)
					.setDistance(Distance.Cosine).build();

			VectorsConfig vectorsConfig = VectorsConfig.newBuilder()
					.setParamsMap(VectorParamsMap.newBuilder().putMap("dense", denseParams).build()).build();

			SparseVectorParams sparseParams = SparseVectorParams.newBuilder().setModifier(Modifier.Idf).build();

			SparseVectorConfig sparseVectorsConfig = SparseVectorConfig.newBuilder().putMap("sparse", sparseParams)
					.build();

			CreateCollection createCollectionRequest = CreateCollection.newBuilder().setCollectionName(collectionName)
					.setVectorsConfig(vectorsConfig).setSparseVectorsConfig(sparseVectorsConfig).build();

			qdrantClient.createCollectionAsync(createCollectionRequest).get();

			log.info("Qdrant collection '{}' created successfully with dense + sparse vectors.", collectionName);

		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();

			throw new RuntimeException("Qdrant collection initialization interrupted", e);

		} catch (ExecutionException e) {

			throw new RuntimeException("Failed to initialize Qdrant collection", e);
		}
	}

	// =========================================================
	// STORE ONE CHUNK (kept for any existing callers)
	// =========================================================

	public void storeChunk(DocumentChunk chunk, float[] embedding, String ownerId) {

		PointStruct point = buildPoint(chunk, embedding, ownerId);

		UpsertPoints request = UpsertPoints.newBuilder().setCollectionName(collectionName).addPoints(point).build();

		try {

			qdrantClient.upsertAsync(request).get();

			log.info("Stored chunk {} in Qdrant. Document ID: {}",
					chunk.getChunkIndex(), chunk.getDocumentId());

		} catch (InterruptedException e) {

			Thread.currentThread().interrupt();

			throw new RuntimeException("Qdrant upsert interrupted.", e);

		} catch (ExecutionException e) {

			throw new RuntimeException("Failed to store chunk in Qdrant.", e);
		}
	}

	// =========================================================
	// STORE MANY CHUNKS AT ONCE (batched upsert)
	// =========================================================

	/**
	 * Stores many chunks in Qdrant using a SMALL NUMBER of batched
	 * upsert calls, instead of one network call per chunk.
	 *
	 * For a 600-page book with ~1500-2000 chunks, this turns
	 * ~1500-2000 sequential Qdrant calls into ~15-20 batched calls
	 * (100 points each) — the actual fix for slow large-document
	 * uploads, alongside concurrent embedding generation.
	 */
	public void storeChunks(List<DocumentChunk> chunks, List<float[]> embeddings, String ownerId) {

		if (chunks.size() != embeddings.size()) {
			throw new IllegalArgumentException(
					"Chunk count (" + chunks.size() + ") and embedding count (" + embeddings.size() + ") must match.");
		}

		List<PointStruct> points = new ArrayList<>(chunks.size());

		for (int i = 0; i < chunks.size(); i++) {
			points.add(buildPoint(chunks.get(i), embeddings.get(i), ownerId));
		}

		for (int start = 0; start < points.size(); start += UPSERT_BATCH_SIZE) {

			int end = Math.min(start + UPSERT_BATCH_SIZE, points.size());
			List<PointStruct> batch = points.subList(start, end);

			UpsertPoints request = UpsertPoints.newBuilder()
					.setCollectionName(collectionName)
					.addAllPoints(batch)
					.build();

			try {

				qdrantClient.upsertAsync(request).get();

				log.info("Upserted batch of {} points ({}-{} of {}) for ownerId {}",
						batch.size(), start, end, points.size(), ownerId);

			} catch (InterruptedException e) {

				Thread.currentThread().interrupt();
				throw new RuntimeException("Qdrant batch upsert interrupted.", e);

			} catch (ExecutionException e) {

				throw new RuntimeException("Failed to batch upsert chunks to Qdrant.", e);
			}
		}
	}

	/**
	 * Builds a single Qdrant point (vector + payload) WITHOUT sending
	 * it — shared by both storeChunk() and storeChunks().
	 */
	private PointStruct buildPoint(DocumentChunk chunk, float[] embedding, String ownerId) {

		if (embedding == null) {
			throw new IllegalArgumentException("Embedding cannot be null.");
		}

		if (embedding.length != embeddingDimensions) {
			throw new IllegalArgumentException(
					"Expected " + embeddingDimensions + " dimensions but received " + embedding.length);
		}

		List<Float> vectorValues = new ArrayList<>(embedding.length);

		for (float value : embedding) {
			vectorValues.add(value);
		}

		Vector denseVector = Vector.newBuilder().addAllData(vectorValues).build();

		SparseVectorData sparseData = sparseVectorService.encode(chunk.getContent());

		SparseIndices sparseIndices = SparseIndices.newBuilder().addAllData(sparseData.getIndices()).build();

		Vector sparseVector = Vector.newBuilder().addAllData(sparseData.getValues()).setIndices(sparseIndices).build();

		NamedVectors namedVectors = NamedVectors.newBuilder()
				.putVectors("dense", denseVector)
				.putVectors("sparse", sparseVector)
				.build();

		Vectors vectors = Vectors.newBuilder().setVectors(namedVectors).build();

		String pointId = UUID.randomUUID().toString();
		PointId qdrantPointId = PointId.newBuilder().setUuid(pointId).build();

		Map<String, Value> payload = new HashMap<>();
		payload.put("documentId", ValueFactory.value(chunk.getDocumentId()));
		payload.put("chunkIndex", ValueFactory.value(chunk.getChunkIndex()));
		payload.put("content", ValueFactory.value(chunk.getContent()));
		payload.put("ownerId", ValueFactory.value(ownerId));

		return PointStruct.newBuilder()
				.setId(qdrantPointId)
				.setVectors(vectors)
				.putAllPayload(payload)
				.build();
	}

	// =========================================================
	// APPLICATION STARTUP
	// =========================================================

	@EventListener(ApplicationReadyEvent.class)
	public void initializeCollection() {

		createCollection();

		log.info("Qdrant collection '{}' is ready.", collectionName);
	}

	// =========================================================
	// DELETE ALL POINTS FOR A USER
	// =========================================================

	public void deleteByOwnerId(String ownerId) {

		Filter filter = Filter.newBuilder().addMust(ConditionFactory.matchKeyword("ownerId", ownerId)).build();

		try {

			qdrantClient.deleteAsync(collectionName, filter).get();

			log.info("Deleted all Qdrant points for ownerId: {}", ownerId);

		} catch (Exception e) {

			throw new RuntimeException("Failed to delete Qdrant points for user: " + ownerId, e);
		}
	}

	public void deleteByDocumentId(String documentId) {

		Filter filter = Filter.newBuilder().addMust(ConditionFactory.matchKeyword("documentId", documentId)).build();

		try {

			qdrantClient.deleteAsync(collectionName, filter).get();

			log.info("Deleted all Qdrant points for documentId: {}", documentId);

		} catch (Exception e) {

			throw new RuntimeException("Failed to delete Qdrant points for document: " + documentId, e);
		}
	}
}