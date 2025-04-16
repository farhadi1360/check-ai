package de.checkai.insurance.car.core.service.impl;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import de.checkai.insurance.car.appication.model.EmbeddingCollection;
import de.checkai.insurance.car.core.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final Storage storage;
    private final ObjectMapper objectMapper;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Save embeddings collection to local JSON file
     *
     * @param collection the embedding collection to save
     * @return the path to the saved file
     */
    public Path saveEmbeddingsToLocalFile(EmbeddingCollection collection) throws IOException {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String filename = String.format("insurance-car-embeddings_%s_%s.json",
                collection.id(), timestamp);

        Path filePath = Path.of(System.getProperty("java.io.tmpdir"), filename);

        // Write embeddings to JSON file
        objectMapper.writeValue(filePath.toFile(), collection);
        log.info("Saved embeddings collection to local file: {}", filePath);

        return filePath;
    }

    /**
     * Upload a file to Google Cloud Storage
     *
     * @param filePath    the path to the file to upload
     * @param contentType the content type of the file
     * @return the GCS URI of the uploaded file
     */
    public String uploadFileToGcs(Path filePath, String contentType) throws IOException {
        String objectName = filePath.getFileName().toString();
        byte[] content = Files.readAllBytes(filePath);

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            storage.create(blobInfo, inputStream);
        }

        String gcsUri = String.format("gs://%s/%s", bucketName, objectName);
        log.info("Successfully uploaded file to GCS: {}", gcsUri);

        return gcsUri;
    }
    /**
     * Save embeddings to JSON and upload to GCS
     *
     * @param collection the embedding collection to save
     * @return the GCS URI of the uploaded file
     */
    public String saveEmbeddingsToGcs(EmbeddingCollection collection) throws IOException {
        Path localFile = saveEmbeddingsToLocalFile(collection);
        return uploadFileToGcs(localFile, "application/json");
    }
}