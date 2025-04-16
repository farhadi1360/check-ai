package de.checkai.insurance.car.appication.model;

import java.time.Instant;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

/**
 * Status update for PDF processing
 */
public record ProcessingStatus(
        String batchId,
        String status,
        int processedDocuments,
        int totalDocuments,
        int processedChunks,
        Instant lastUpdated
) {
}
