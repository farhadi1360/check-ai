package de.checkai.insurance.car.appication.model;

import java.time.Instant;
import java.util.List;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

/**
 * Represents a collection of embeddings with metadata
 */
public record EmbeddingCollection(
        String id,
        String description,
        Instant createdAt,
        int embeddingDimension,
        String embeddingModel,
        int totalChunks,
        List<TextEmbedding> embeddings
) {
}
