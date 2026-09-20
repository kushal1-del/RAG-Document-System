package com.kushal.rag.service.parsing;

import java.io.IOException;
import java.io.InputStream;

/**
 * One implementation per supported file type. DocumentParser
 * (the dispatcher) picks the right one based on file extension.
 */
public interface FileParser {

	/**
	 * @param extension lowercase, no dot — e.g. "pdf", "docx", "txt"
	 */
	boolean supports(String extension);

	String parse(InputStream inputStream) throws IOException;
}