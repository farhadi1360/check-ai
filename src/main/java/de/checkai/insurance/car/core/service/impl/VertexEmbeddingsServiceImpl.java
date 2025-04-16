package de.checkai.insurance.car.core.service.impl;

import com.google.cloud.aiplatform.v1.*;
import com.google.protobuf.util.JsonFormat;
import de.checkai.insurance.car.appication.model.TextChunk;
import de.checkai.insurance.car.appication.model.TextEmbedding;
import de.checkai.insurance.car.core.service.VertexEmbeddingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

@Service
@Slf4j
public class VertexEmbeddingsServiceImpl implements VertexEmbeddingsService {
   @Value("${spring.cloud.gcp.project-id}")
   private String projectId;

   @Value("${gcp.vertex.region}")
   private String region;

   @Value("${gcp.vertex.embedding-model}")
   private String embeddingModel;

   /**
    * Generate embeddings for a list of text chunks using Vertex AI
    *
    * @param textChunks list of text chunks to generate embeddings for
    * @return list of text embeddings
    */
   public List<TextEmbedding> generateEmbeddings(List<TextChunk> textChunks) {
      List<TextEmbedding> embeddings = new ArrayList<>();

      try (PredictionServiceClient predictionServiceClient = createPredictionServiceClient()) {
         // Process chunks in batches of 5 to avoid rate limiting
         List<List<TextChunk>> batches = batchList(textChunks, 5);

         for (List<TextChunk> batch : batches) {
            List<TextEmbedding> batchEmbeddings = processBatch(predictionServiceClient, batch);
            embeddings.addAll(batchEmbeddings);

            // Add a small delay to avoid rate limiting
            try {
               Thread.sleep(500);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         }

         log.info("Generated {} embeddings successfully", embeddings.size());
         return embeddings;

      } catch (IOException e) {
         log.error("Error generating embeddings: {}", e.getMessage(), e);
         throw new RuntimeException("Failed to generate embeddings", e);
      }
   }

   private PredictionServiceClient createPredictionServiceClient() throws IOException {
      String endpoint = String.format("%s-aiplatform.googleapis.com:443", region);
      PredictionServiceSettings predictionServiceSettings = PredictionServiceSettings.newBuilder()
              .setEndpoint(endpoint)
              .build();
      return PredictionServiceClient.create(predictionServiceSettings);
   }

   private List<TextEmbedding> processBatch(PredictionServiceClient client, List<TextChunk> chunks) {
      List<TextEmbedding> batchEmbeddings = new ArrayList<>();

      try {
         // Format the request for the text-embedding model
         String publisherModel = "publishers/google/models/" + embeddingModel;
         EndpointName endpointName = EndpointName.ofProjectLocationPublisherModelName(
                 projectId, region, "google", embeddingModel);

         // Create instances for each text chunk
         List<com.google.protobuf.Value> instances = new ArrayList<>();
         for (TextChunk chunk : chunks) {
            com.google.protobuf.Value.Builder instanceValue = com.google.protobuf.Value.newBuilder();
            JsonFormat.parser().merge(
                    String.format("{\"content\": \"%s\"}",
                            chunk.content().replace("\"", "\\\"")),
                    instanceValue
            );
            instances.add(instanceValue.build());
         }

         // Create parameters (empty in this case)
         com.google.protobuf.Value parameters = com.google.protobuf.Value.newBuilder().build();

         // Make the prediction request
         PredictRequest request = PredictRequest.newBuilder()
                 .setEndpoint(endpointName.toString())
                 .addAllInstances(instances)
                 .setParameters(parameters)
                 .build();

         PredictResponse response = client.predict(request);

         // Process the response
         List<com.google.protobuf.Value> predictions = response.getPredictionsList();

         for (int i = 0; i < predictions.size() && i < chunks.size(); i++) {
            TextChunk chunk = chunks.get(i);
            com.google.protobuf.Value prediction = predictions.get(i);

            // Extract embedding values
            float[] embedding = extractEmbeddingArray(prediction);

            batchEmbeddings.add(new TextEmbedding(
                    chunk.id(),
                    embedding,
                    chunk.content(),
                    chunk.sourceDocument(),
                    chunk.pageNumber(),
                    chunk.position()
            ));
         }

      } catch (Exception e) {
         log.error("Error processing batch for embeddings: {}", e.getMessage(), e);
         throw new RuntimeException("Failed to process text batch for embeddings", e);
      }

      return batchEmbeddings;
   }

   private float[] extractEmbeddingArray(com.google.protobuf.Value prediction) {

      try {
         // Try the expected structure
         var embeddingValues = prediction.getStructValue()
                 .getFieldsMap().get("embeddings").getStructValue()
                 .getFieldsMap().get("values").getListValue();

         float[] embedding = new float[embeddingValues.getValuesCount()];
         for (int i = 0; i < embeddingValues.getValuesCount(); i++) {
            embedding[i] = (float) embeddingValues.getValues(i).getNumberValue();
         }
         return embedding;
      } catch (Exception e) {
         // Fallback method - try alternative response structure
         log.warn("Using fallback method to extract embeddings due to: {}", e.getMessage());
         try {
            // The structure might be different depending on the model version
            var valuesField = prediction.getStructValue().getFieldsMap();

            // Try to find a field that contains the embedding values
            for (Map.Entry<String, com.google.protobuf.Value> entry : valuesField.entrySet()) {
               if (entry.getValue().hasListValue()) {
                  var listValue = entry.getValue().getListValue();
                  float[] embedding = new float[listValue.getValuesCount()];
                  for (int i = 0; i < listValue.getValuesCount(); i++) {
                     embedding[i] = (float) listValue.getValues(i).getNumberValue();
                  }
                  return embedding;
               }
            }

            // If we couldn't find a list value, log and throw exception
            log.error("Could not find embedding values in response: {}", prediction);
            throw new RuntimeException("Failed to extract embedding values from response");
         } catch (Exception fallbackEx) {
            log.error("Failed to extract embeddings using fallback method: {}", fallbackEx.getMessage());
            throw new RuntimeException("Failed to extract embeddings", fallbackEx);
         }
      }
   }

// The convertToJson method is no longer needed since we're using the JsonFormat.parser directly
// for each individual instance

   private String convertToJson(List<Map<String, Object>> instances) {
      StringBuilder json = new StringBuilder();
      json.append("{\"instances\":[");

      for (int i = 0; i < instances.size(); i++) {
         Map<String, Object> instance = instances.get(i);
         json.append("{");

         int j = 0;
         for (Map.Entry<String, Object> entry : instance.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":\"")
                    .append(entry.getValue().toString().replace("\"", "\\\""))
                    .append("\"");

            if (j < instance.size() - 1) {
               json.append(",");
            }
            j++;
         }

         json.append("}");
         if (i < instances.size() - 1) {
            json.append(",");
         }
      }

      json.append("]}");
      return json.toString();
   }
   private <T> List<List<T>> batchList(List<T> list, int batchSize) {
      List<List<T>> batches = new ArrayList<>();
      for (int i = 0; i < list.size(); i += batchSize) {
         batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
      }
      return batches;
   }
}
