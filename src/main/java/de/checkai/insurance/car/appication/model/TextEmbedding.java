package de.checkai.insurance.car.appication.model;

import java.util.UUID;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

/**
 * Represents the vector embedding for a text chunk
 */
public record TextEmbedding(
        UUID id,
        float[] embedding,
        String textContent,
        String sourceDocument,
        int pageNumber,
        int position
) {
}
