package com.kushal.rag.service.chunking;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kushal.rag.model.DocumentChunk;

/**
 * Splits text into chunks WITHOUT ever cutting a sentence in half.
 *
 * Approach:
 *   1. Break the text into individual sentences (using Java's
 *      built-in BreakIterator, which correctly handles things
 *      like "Mr. Graham" without treating "Mr." as a sentence end).
 *   2. Greedily pack whole sentences into a chunk until adding
 *      the next one would exceed the target chunk size.
 *   3. Start the next chunk by repeating the last sentence(s) of
 *      the previous chunk (the "overlap"), so an argument that
 *      spans a chunk boundary still reads intact in both chunks.
 *
 * For very large documents (e.g. full books), a bigger target
 * chunk size is used automatically — this reduces the TOTAL
 * number of chunks (and therefore total embedding API calls),
 * trading a small amount of retrieval precision for staying
 * within realistic API quotas on large uploads.
 */
@Service
public class TextChunker {

	private static final Logger log = LoggerFactory.getLogger(TextChunker.class);

	// Target chunk size in characters for normal-sized documents.
	@Value("${chunking.target-size:1000}")
	private int targetSize;

	// How many trailing sentences from the previous chunk get
	// repeated at the start of the next chunk, for continuity.
	@Value("${chunking.overlap-sentences:2}")
	private int overlapSentences;

	// If the document's total text exceeds this many characters,
	// it's considered "large" and uses largeDocTargetSize instead
	// of targetSize, to reduce total chunk/embedding-call count.
	@Value("${chunking.large-doc-char-threshold:200000}")
	private int largeDocThreshold;

	// Target chunk size used for large documents.
	@Value("${chunking.large-doc-target-size:2500}")
	private int largeDocTargetSize;

	public List<DocumentChunk> chunk(String documentId, String text) {

		List<DocumentChunk> chunks = new ArrayList<>();

		if (text == null || text.isBlank()) {
			return chunks;
		}

		// For very large documents, use a bigger target chunk size
		// to reduce the total number of chunks produced.
		int effectiveTargetSize = text.length() > largeDocThreshold ? largeDocTargetSize : targetSize;

		if (text.length() > largeDocThreshold) {
			log.info("Large document detected ({} chars) — using target chunk size {} instead of {}",
					text.length(), largeDocTargetSize, targetSize);
		}

		List<String> sentences = splitIntoSentences(text);

		if (sentences.isEmpty()) {
			return chunks;
		}

		int chunkIndex = 0;
		int i = 0;

		while (i < sentences.size()) {

			StringBuilder currentChunk = new StringBuilder();
			int sentencesInChunk = 0;
			int startOfThisChunk = i;

			// Greedily add sentences until we'd exceed the target size.
			while (i < sentences.size()) {

				String sentence = sentences.get(i);

				// Always include at least ONE sentence per chunk,
				// even if that single sentence alone exceeds the
				// target size (better than producing an empty chunk).
				boolean wouldExceed =
						currentChunk.length() + sentence.length() > effectiveTargetSize;

				if (wouldExceed && sentencesInChunk > 0) {
					break;
				}

				currentChunk.append(sentence).append(" ");
				sentencesInChunk++;
				i++;
			}

			String content = currentChunk.toString().trim();

			if (!content.isBlank()) {
				chunks.add(new DocumentChunk(documentId, chunkIndex, content));
				chunkIndex++;
			}

			// If we've reached the end, stop — no more sentences
			// to overlap into a next chunk.
			if (i >= sentences.size()) {
				break;
			}

			// Step back by a few sentences so the next chunk starts
			// with some overlap from the end of this one, instead
			// of picking up exactly where this chunk left off.
			int overlapStart = Math.max(startOfThisChunk, i - overlapSentences);
			i = overlapStart;

			// Safety net: if overlap somehow didn't move us forward
			// (shouldn't normally happen), force progress to avoid
			// an infinite loop.
			if (i <= startOfThisChunk) {
				i = startOfThisChunk + 1;
			}
		}

		return chunks;
	}

	/**
	 * Uses Java's built-in BreakIterator to split text into
	 * individual sentences, correctly handling abbreviations
	 * ("Mr.", "U.S.", "e.g.") far better than a naive split on
	 * periods would.
	 */
	private List<String> splitIntoSentences(String text) {

		List<String> sentences = new ArrayList<>();

		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
		iterator.setText(text);

		int start = iterator.first();
		int end = iterator.next();

		while (end != BreakIterator.DONE) {

			String sentence = text.substring(start, end).trim();

			if (!sentence.isBlank()) {
				sentences.add(sentence);
			}

			start = end;
			end = iterator.next();
		}

		return sentences;
	}
}