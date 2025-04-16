package de.checkai.insurance.car.core.service;

import de.checkai.insurance.car.appication.model.TextChunk;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
public interface PdfExtractionService {
    List<TextChunk> extractTextChunks(Path pdfPath);
}
