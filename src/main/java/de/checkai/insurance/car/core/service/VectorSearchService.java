package de.checkai.insurance.car.core.service;

import de.checkai.insurance.car.appication.model.VectorSearchResponse;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
public interface VectorSearchService {

    VectorSearchResponse createAndDeployVectorSearchIndex(String gcsUri, String batchId);
}
