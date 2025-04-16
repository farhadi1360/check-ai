package de.checkai.insurance.car.core.service;

import de.checkai.insurance.car.appication.model.EmbeddingCollection;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
public interface StorageService {

    Path saveEmbeddingsToLocalFile(EmbeddingCollection collection) throws IOException;

    String uploadFileToGcs(Path filePath, String contentType) throws IOException;

    String saveEmbeddingsToGcs(EmbeddingCollection collection) throws IOException;
}
