package de.checkai.insurance.car.appication.model;

import java.util.UUID;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

/**
 * Represents a text chunk extracted from an insurance document
 */
public record TextChunk(
        UUID id,
        String content,
        String sourceDocument,
        int pageNumber,
        int position
) {
}
