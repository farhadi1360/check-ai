package de.checkai.insurance.car.appication.model;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

import java.time.Instant;

/**
 * Response for PDF processing
 */
public record PdfProcessingResponse(
        String batchId,
        int totalDocuments,
        int totalChunks,
        Instant startTime,
        Instant endTime,
        String status,
        String vectorSearchIndexName,
        String errorMessage
) {
}
