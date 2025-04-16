package de.checkai.insurance.car.appication.controller;

import de.checkai.insurance.car.appication.model.PdfProcessingRequest;
import de.checkai.insurance.car.appication.model.PdfProcessingResponse;
import de.checkai.insurance.car.appication.model.ProcessingStatus;
import de.checkai.insurance.car.core.service.ProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

@RestController
@RequestMapping("/api/v1/processing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PDF Processing", description = "Endpoints for processing PDFs and generating embeddings")
public class ProcessingController {
    private final ProcessingService processingService;

    @PostMapping("/pdf")
    @Operation(summary = "Process PDF files",
            description = "Extract text from PDFs, generate embeddings, and create a vector search index")
    public ResponseEntity<PdfProcessingResponse> processPdfFiles(@Valid @RequestBody PdfProcessingRequest request) {
        // Generate a unique batch ID
        String batchId = UUID.randomUUID().toString();
        log.info("Starting PDF processing batch {} with {} files", batchId, request.pdfFilePaths().size());

        // Start asynchronous processing
        processingService.processPdfFiles(request, batchId);

        // Return response with batch ID
        PdfProcessingResponse response = new PdfProcessingResponse(
                batchId,
                request.pdfFilePaths().size(),
                0,  // Initially, no chunks processed
                Instant.now(),
                null,  // End time is null until processing completes
                "PROCESSING",
                "insurance-car-index-" + batchId,
                null  // No error message
        );

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status/{batchId}")
    @Operation(summary = "Get processing status",
            description = "Get the current status of a PDF processing batch")
    public ResponseEntity<ProcessingStatus> getProcessingStatus(@PathVariable String batchId) {
        ProcessingStatus status = processingService.getProcessingStatus(batchId);

        if ("NOT_FOUND".equals(status.status())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }
}