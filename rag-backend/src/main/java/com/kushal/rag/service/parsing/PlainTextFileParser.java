package com.kushal.rag.service.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class PlainTextFileParser implements FileParser {

	private static final Set<String> SUPPORTED = Set.of("txt", "md");

	@Override
	public boolean supports(String extension) {
		return SUPPORTED.contains(extension);
	}

	@Override
	public String parse(InputStream inputStream) throws IOException {
		return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
	}
}