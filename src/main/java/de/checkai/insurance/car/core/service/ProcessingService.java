package de.checkai.insurance.car.core.service;

import de.checkai.insurance.car.appication.model.PdfProcessingRequest;
import de.checkai.insurance.car.appication.model.ProcessingStatus;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
public interface ProcessingService {

    void processPdfFiles(PdfProcessingRequest request, String batchId);

    ProcessingStatus getProcessingStatus(String batchId);
}
