package com.kushal.rag.service.vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Converts plain text into a "sparse vector" — the representation
 * Qdrant uses for BM25-style exact-keyword search.
 *
 * Unlike our dense (Gemini) embedding, this involves NO machine
 * learning model. It's pure text processing:
 *
 *   1. Split text into individual words (tokenize).
 *   2. Convert each unique word into a stable numeric ID (hash),
 *      since Qdrant's sparse vector format needs integer indices,
 *      not raw words.
 *   3. Count how many times each word appears in this text.
 *
 * The result is two parallel lists:
 *   indices -> which "word IDs" appeared
 *   values  -> how many times each one appeared
 *
 * Qdrant then applies IDF weighting automatically at query time
 * (because we set Modifier.Idf on the collection), which handles
 * the "common words matter less, rare words matter more" logic
 * for us — we don't need to compute that ourselves.
 */
@Service
public class SparseVectorService {

    /**
     * Simple container for a sparse vector's two parallel arrays.
     */
    public static class SparseVectorData {

        private final List<Integer> indices;
        private final List<Float> values;

        public SparseVectorData(List<Integer> indices, List<Float> values) {
            this.indices = indices;
            this.values = values;
        }

        public List<Integer> getIndices() {
            return indices;
        }

        public List<Float> getValues() {
            return values;
        }
    }

    /**
     * Turns raw text into a sparse vector representation.
     */
    public SparseVectorData encode(String text) {

        // 1. Tokenize: lowercase, split on anything that's not a
        // letter or digit. This is intentionally simple for now —
        // no stemming, no stopword removal. Good enough to start;
        // we can improve tokenization quality later if needed.
        String[] tokens = text
                .toLowerCase()
                .split("[^a-z0-9]+");

        // 2. Count occurrences of each unique word in this text.
        Map<String, Integer> termFrequency = new HashMap<>();

        for (String token : tokens) {

            if (token.isBlank()) {
                continue;
            }

            termFrequency.merge(token, 1, Integer::sum);
        }

        // 3. Convert each word into a stable numeric ID via hashing,
        // and build the parallel indices/values lists Qdrant expects.
        List<Integer> indices = new ArrayList<>();
        List<Float> values = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {

            int wordId = stableHash(entry.getKey());

            indices.add(wordId);
            values.add((float) entry.getValue());
        }

        return new SparseVectorData(indices, values);
    }

    /**
     * Converts a word into a stable, always-positive integer ID.
     *
     * We can't just use String.hashCode() directly, because:
     *   - it can be negative, and Qdrant sparse vector indices
     *     must be non-negative integers
     *   - we want this to be deterministic across app restarts,
     *     which String.hashCode() actually already guarantees in
     *     modern Java for the same string — but we mask off the
     *     sign bit to be safe and explicit about the requirement.
     */
    private int stableHash(String word) {
        return word.hashCode() & 0x7fffffff;
    }
}