package de.checkai.insurance.car.appication.model;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

import java.time.Instant;

/**
 * Response for vector search index creation
 */
public record VectorSearchResponse(
        String indexId,
        String indexEndpointId,
        String status,
        Instant createdAt,
        String errorMessage
) {
}
