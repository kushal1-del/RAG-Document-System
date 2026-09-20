package com.kushal.rag.service.vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kushal.rag.model.SearchResult;
import com.kushal.rag.service.vector.SparseVectorService.SparseVectorData;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.DenseVector;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Fusion;
import io.qdrant.client.grpc.Points.PrefetchQuery;
import io.qdrant.client.grpc.Points.Query;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SparseVector;
import io.qdrant.client.grpc.Points.VectorInput;

@Service
public class QdrantSearchService {

	private final String collectionName;
	private final QdrantClient qdrantClient;
	private final SparseVectorService sparseVectorService;

	public QdrantSearchService(QdrantClient qdrantClient, SparseVectorService sparseVectorService,
			@org.springframework.beans.factory.annotation.Value("${qdrant.collection.name}") String collectionName) {

		this.qdrantClient = qdrantClient;
		this.sparseVectorService = sparseVectorService;
		this.collectionName = collectionName;
	}

	/**
	 * Hybrid search: runs a DENSE (semantic/meaning) search and a SPARSE (exact
	 * keyword, BM25-style) search in parallel, then fuses both rankings into one
	 * final result list using Qdrant's built-in Reciprocal Rank Fusion (RRF).
	 */
	public List<SearchResult> search(String questionText, float[] queryEmbedding, int limit, String ownerId,
			String documentId) {

		// 1. Build the dense query vector from the question's embedding.
		List<Float> denseValues = new ArrayList<>();

		for (float value : queryEmbedding) {
			denseValues.add(value);
		}

		DenseVector denseVector = DenseVector.newBuilder().addAllData(denseValues).build();

		// 2. Build the sparse query vector from the raw question text.
		SparseVectorData sparseData = sparseVectorService.encode(questionText);

		SparseVector sparseVector = SparseVector.newBuilder().addAllIndices(sparseData.getIndices())
				.addAllValues(sparseData.getValues()).build();

		// 3. Build the shared filter — ownerId always required,
		// documentId optional.
		Filter.Builder filterBuilder = Filter.newBuilder().addMust(ConditionFactory.matchKeyword("ownerId", ownerId));

		if (documentId != null && !documentId.isBlank()) {
			filterBuilder.addMust(ConditionFactory.matchKeyword("documentId", documentId));
		}

		Filter filter = filterBuilder.build();

		// 4. Prefetch #1 — dense (semantic) search.
		PrefetchQuery densePrefetch = PrefetchQuery.newBuilder()
				.setQuery(Query.newBuilder().setNearest(VectorInput.newBuilder().setDense(denseVector).build()).build())
				.setUsing("dense").setFilter(filter).setLimit(limit * 2L).build();

		// 5. Prefetch #2 — sparse (exact keyword) search.
		PrefetchQuery sparsePrefetch = PrefetchQuery.newBuilder()
				.setQuery(
						Query.newBuilder().setNearest(VectorInput.newBuilder().setSparse(sparseVector).build()).build())
				.setUsing("sparse").setFilter(filter).setLimit(limit * 2L).build();

		// 6. Final fused query using Reciprocal Rank Fusion (RRF).
		QueryPoints queryRequest = QueryPoints.newBuilder().setCollectionName(collectionName).addPrefetch(densePrefetch)
				.addPrefetch(sparsePrefetch).setQuery(Query.newBuilder().setFusion(Fusion.RRF).build()).setLimit(limit)
				.setWithPayload(WithPayloadSelectorFactory.enable(true)).build();

		try {

			List<ScoredPoint> scoredPoints = qdrantClient.queryAsync(queryRequest).get();

			List<SearchResult> results = new ArrayList<>();

			for (ScoredPoint point : scoredPoints) {

				Map<String, Value> payload = point.getPayloadMap();

				String resultDocumentId = payload.containsKey("documentId") ? payload.get("documentId").getStringValue()
						: "unknown";

				int chunkIndex = payload.containsKey("chunkIndex") ? (int) payload.get("chunkIndex").getIntegerValue()
						: -1;

				String content = payload.containsKey("content") ? payload.get("content").getStringValue() : "";

				SearchResult result = new SearchResult(resultDocumentId, chunkIndex, content, point.getScore());

				results.add(result);
			}

			return results;

		} catch (Exception e) {

			throw new RuntimeException("Failed to search Qdrant", e);
		}
	}
}