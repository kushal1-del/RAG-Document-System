package com.kushal.rag.service.parsing;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;

@Component
public class DocxFileParser implements FileParser {

	@Override
	public boolean supports(String extension) {
		return "docx".equals(extension);
	}

	@Override
	public String parse(InputStream inputStream) throws IOException {
		try (XWPFDocument document = new XWPFDocument(inputStream);
				XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
			return extractor.getText();
		}
	}
}