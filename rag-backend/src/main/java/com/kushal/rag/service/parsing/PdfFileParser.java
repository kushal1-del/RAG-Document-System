package com.kushal.rag.service.parsing;

import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfFileParser implements FileParser {

	@Override
	public boolean supports(String extension) {
		return "pdf".equals(extension);
	}

	@Override
	public String parse(InputStream inputStream) throws IOException {
		byte[] pdfBytes = inputStream.readAllBytes();
		try (PDDocument document = Loader.loadPDF(pdfBytes)) {
			PDFTextStripper textStripper = new PDFTextStripper();
			return textStripper.getText(document);
		}
	}
}