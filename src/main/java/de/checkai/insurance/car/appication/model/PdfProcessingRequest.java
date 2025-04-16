package de.checkai.insurance.car.appication.model;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

import java.util.List;
import java.util.Map;

/**
 * Request for PDF processing
 */
public record PdfProcessingRequest(
        List<String> pdfFilePaths,
        String description,
        Map<String, String> metadata
) {
}
