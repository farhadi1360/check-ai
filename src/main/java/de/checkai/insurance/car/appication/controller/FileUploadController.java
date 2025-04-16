package de.checkai.insurance.car.appication.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
@RestController
@RequestMapping("/api/v1/files")
@Slf4j
@Tag(name = "File Upload", description = "Endpoints for uploading PDF files")
public class FileUploadController {

    @Value("${java.io.tmpdir}")
    private String tempDir;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload PDF files",
            description = "Upload PDF files to be processed for embedding generation")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Received {} files to upload", files.size());
        List<String> uploadedFilePaths = new ArrayList<>();
        List<String> failedUploads = new ArrayList<>();

        // Create a timestamped subfolder
        String timestamp = Instant.now()
                .atZone(ZoneOffset.UTC) // convert to ZonedDateTime in UTC
                .format(DATE_FORMATTER);
        Path uploadDir = Paths.get(tempDir, "insurance-car-pdfs-" + timestamp);

        try {
            Files.createDirectories(uploadDir);

            // Process each file
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    failedUploads.add(file.getOriginalFilename() + " (empty file)");
                    continue;
                }

                if (!file.getContentType().equals("application/pdf")) {
                    failedUploads.add(file.getOriginalFilename() + " (not a PDF)");
                    continue;
                }

                try {
                    // Save the file to the temp directory
                    String fileName = file.getOriginalFilename().replace(" ", "_");
                    Path filePath = uploadDir.resolve(fileName);
                    Files.copy(file.getInputStream(), filePath);

                    uploadedFilePaths.add(filePath.toString());
                    log.info("Uploaded file: {}", filePath);

                } catch (Exception e) {
                    log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                    failedUploads.add(file.getOriginalFilename() + " (" + e.getMessage() + ")");
                }
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("uploadedFiles", uploadedFilePaths);
            response.put("failedUploads", failedUploads);
            response.put("timestamp", timestamp);
            response.put("totalUploaded", uploadedFilePaths.size());
            response.put("uploadDirectory", uploadDir.toString());

            if (!failedUploads.isEmpty()) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to create upload directory: " + e.getMessage(),
                    "timestamp", timestamp
            ));
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List uploaded files",
            description = "List all uploaded PDF files in the temporary directory")
    public ResponseEntity<Map<String, Object>> listFiles() {
        try {
            Path uploadDir = Paths.get(tempDir);
            List<String> pdfFiles = new ArrayList<>();

            Files.walk(uploadDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                    .forEach(p -> pdfFiles.add(p.toString()));

            return ResponseEntity.ok(Map.of(
                    "files", pdfFiles,
                    "count", pdfFiles.size()
            ));

        } catch (IOException e) {
            log.error("Failed to list files: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to list files: " + e.getMessage()
            ));
        }
    }
}