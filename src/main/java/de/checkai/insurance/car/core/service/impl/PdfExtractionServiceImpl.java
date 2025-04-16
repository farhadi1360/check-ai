package de.checkai.insurance.car.core.service.impl;

import de.checkai.insurance.car.appication.model.TextChunk;
import de.checkai.insurance.car.core.service.PdfExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
@Service
@Slf4j
public class PdfExtractionServiceImpl implements PdfExtractionService {

    @Value("${pdf.chunk-size:300}")
    private int chunkSize;

    @Value("${pdf.chunk-overlap:50}")
    private int chunkOverlap;

    private static final Pattern SENTENCE_DELIMITER = Pattern.compile("[.!?]\\s+");

    /**
     * Extract text chunks from a PDF file
     *
     * @param pdfPath Path to the PDF file
     * @return List of text chunks extracted from the PDF
     */
    public List<TextChunk> extractTextChunks(Path pdfPath) {
        List<TextChunk> chunks = new ArrayList<>();
        String fileName = pdfPath.getFileName().toString();

        try (PDDocument document = PDDocument.load(new File(pdfPath.toString()))) {
            log.info("Processing PDF: {} with {} pages", fileName, document.getNumberOfPages());

            PDFTextStripper stripper = new PDFTextStripper();

            // Process each page separately
            for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(document);

                // Split text into sentences and create chunks
                List<String> sentences = splitIntoSentences(pageText);
                List<String> textChunks = createChunks(sentences);

                // Create TextChunk objects
                for (int i = 0; i < textChunks.size(); i++) {
                    String chunkContent = textChunks.get(i).trim();
                    // Skip empty chunks
                    if (!chunkContent.isEmpty()) {
                        chunks.add(new TextChunk(
                                UUID.randomUUID(),
                                chunkContent,
                                fileName,
                                pageNum,
                                i
                        ));
                    }
                }
            }

            log.info("Extracted {} chunks from PDF: {}", chunks.size(), fileName);
            return chunks;

        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", fileName, e);
            throw new RuntimeException("Failed to extract text from PDF: " + fileName, e);
        }
    }

    /**
     * Split text into sentences using a regex pattern
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = SENTENCE_DELIMITER.split(text);

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        return sentences;
    }

    /**
     * Create overlapping chunks from sentences
     */
    private List<String> createChunks(List<String> sentences) {
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            // If adding this sentence would exceed chunk size, add current chunk to list
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());

                // Keep some overlap for context
                String[] words = currentChunk.toString().split("\\s+");
                int wordsToKeep = Math.min(chunkOverlap, words.length);

                currentChunk = new StringBuilder();
                // Add the overlap words
                for (int i = words.length - wordsToKeep; i < words.length; i++) {
                    currentChunk.append(words[i]).append(" ");
                }
            }

            currentChunk.append(sentence).append(". ");
        }

        // Add the last chunk if there's any content
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}