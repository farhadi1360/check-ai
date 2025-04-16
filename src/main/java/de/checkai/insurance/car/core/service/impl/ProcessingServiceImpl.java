package de.checkai.insurance.car.core.service.impl;

import de.checkai.insurance.car.appication.model.*;
import de.checkai.insurance.car.core.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessingServiceImpl implements ProcessingService {

    private final PdfExtractionService pdfExtractionService;
    private final VertexEmbeddingsService embeddingsService;
    private final StorageService storageService;
    private final VectorSearchService vectorSearchService;
    @Value("${gcp.vertex.embedding-model}")
    private String embeddingModel;

    @Value("${gcp.vertex.embedding-dimension}")
    private int embeddingDimension;

    private final Map<String, ProcessingStatus> processingStatusMap = new ConcurrentHashMap<>();

    /**
     * Process PDF files, extract text, generate embeddings, and create vector search index
     */
    @Async
    public void processPdfFiles(PdfProcessingRequest request, String batchId) {
        LocalDateTime startTime = LocalDateTime.now();
        List<String> pdfPaths = request.pdfFilePaths();

        try {
            // Initialize processing status
            updateStatus(batchId, "STARTED", 0, pdfPaths.size(), 0);

            // Process each PDF file
            List<TextChunk> allChunks = new ArrayList<>();
            int processedDocs = 0;

            for (String pdfPath : pdfPaths) {
                log.info("Processing PDF file: {}", pdfPath);
                List<TextChunk> chunks = pdfExtractionService.extractTextChunks(Paths.get(pdfPath));
                allChunks.addAll(chunks);

                processedDocs++;
                updateStatus(batchId, "PROCESSING", processedDocs, pdfPaths.size(), allChunks.size());
            }

            // Generate embeddings for all chunks
            updateStatus(batchId, "GENERATING_EMBEDDINGS", processedDocs, pdfPaths.size(), allChunks.size());
            List<TextEmbedding> embeddings = embeddingsService.generateEmbeddings(allChunks);

            // Create embedding collection
            EmbeddingCollection collection = new EmbeddingCollection(
                    batchId,
                    request.description(),
                    Instant.now(),
                    embeddingDimension,
                    embeddingModel,
                    embeddings.size(),
                    embeddings
            );

            // Save embeddings to local file and upload to GCS
            updateStatus(batchId, "SAVING_EMBEDDINGS", processedDocs, pdfPaths.size(), allChunks.size());
            Path embeddingsFile = storageService.saveEmbeddingsToLocalFile(collection);
            String gcsUri = storageService.uploadFileToGcs(embeddingsFile, "application/json");

            // Create and deploy vector search index
            updateStatus(batchId, "CREATING_VECTOR_INDEX", processedDocs, pdfPaths.size(), allChunks.size());
            VectorSearchResponse vectorSearchResponse = vectorSearchService.createAndDeployVectorSearchIndex(gcsUri, batchId);

            // Complete processing status
            updateStatus(batchId, "COMPLETED", processedDocs, pdfPaths.size(), allChunks.size());

            log.info("Successfully processed batch {}. Created vector index: {}",
                    batchId, vectorSearchResponse.indexId());

        } catch (Exception e) {
            log.error("Error processing PDF files for batch {}: {}", batchId, e.getMessage(), e);
            updateStatus(batchId, "FAILED: " + e.getMessage(),
                    processingStatusMap.get(batchId).processedDocuments(),
                    pdfPaths.size(),
                    processingStatusMap.get(batchId).processedChunks());
        }
    }

    /**
     * Update the processing status for a batch
     */
    private void updateStatus(String batchId, String status, int processedDocuments,
                              int totalDocuments, int processedChunks) {
        ProcessingStatus processingStatus = new ProcessingStatus(
                batchId,
                status,
                processedDocuments,
                totalDocuments,
                processedChunks,
                Instant.now()
        );

        processingStatusMap.put(batchId, processingStatus);
        log.debug("Updated processing status for batch {}: {}", batchId, status);
    }

    /**
     * Get the current processing status for a batch
     */
    public ProcessingStatus getProcessingStatus(String batchId) {
        return processingStatusMap.getOrDefault(batchId,
                new ProcessingStatus(batchId, "NOT_FOUND", 0, 0, 0, Instant.now()));
    }
}
