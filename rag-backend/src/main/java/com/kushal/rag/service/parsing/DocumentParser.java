package com.kushal.rag.service.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kushal.rag.exception.UnsupportedFileTypeException;

@Service
public class DocumentParser {

	private final List<FileParser> parsers;

	public DocumentParser(List<FileParser> parsers) {
		// Spring auto-injects every FileParser @Component bean here —
		// PdfFileParser, PlainTextFileParser, DocxFileParser, and any
		// future format we add, with zero changes needed in this class.
		this.parsers = parsers;
	}

	public String parse(String filename, InputStream inputStream) throws IOException {

		String extension = extractExtension(filename);

		FileParser parser = parsers.stream()
				.filter(p -> p.supports(extension))
				.findFirst()
				.orElseThrow(() -> new UnsupportedFileTypeException(
						"Unsupported file type: ." + extension
								+ ". Supported types: pdf, docx, txt, md"));

		return parser.parse(inputStream);
	}

	private String extractExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			throw new UnsupportedFileTypeException("File has no extension: " + filename);
		}
		return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
	}
}