package de.checkai.insurance.car.core.service.impl;

import com.google.cloud.aiplatform.v1.*;
import com.google.protobuf.Struct;
import de.checkai.insurance.car.appication.model.VectorSearchResponse;
import de.checkai.insurance.car.core.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.time.Instant;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {

    private final IndexServiceClient indexServiceClient;
    private final IndexEndpointServiceClient indexEndpointServiceClient;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${gcp.vertex.region}")
    private String region;

    @Value("${gcp.vertex.embedding-dimension}")
    private int embeddingDimension;

    @Value("${gcp.vertex.vector-search-index-name}")
    private String indexName;

    @Value("${gcp.vertex.vector-search-endpoint-name}")
    private String endpointName;

    /**
     * Create and deploy a vector search index in Vertex AI
     *
     * @param gcsUri GCS URI of the embeddings file
     * @param batchId Batch ID for the processing job
     * @return Response with details of the created index and endpoint
     */
    public VectorSearchResponse createAndDeployVectorSearchIndex(String gcsUri, String batchId) {
        try {
            // Create the index
            String indexId = createIndex(gcsUri, batchId);

            // Create the endpoint
            String endpointId = createEndpoint(batchId);

            // Deploy the index to the endpoint
            deployIndex(indexId, endpointId);

            return new VectorSearchResponse(
                    indexId,
                    endpointId,
                    "DEPLOYED",
                    Instant.now(),
                    null
            );

        } catch (Exception e) {
            log.error("Error creating vector search index: {}", e.getMessage(), e);
            return new VectorSearchResponse(
                    null,
                    null,
                    "FAILED",
                    Instant.now(),
                    e.getMessage()
            );
        }
    }

    /**
     * Create a vector search endpoint
     */
    private String createEndpoint(String batchId) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {

        // Set the location path
        LocationName parent = LocationName.of(projectId, region);

        // Set unique endpoint ID
        String uniqueEndpointId = endpointName + "-" + batchId;

        // Create endpoint object
        IndexEndpoint endpoint = IndexEndpoint.newBuilder()
                .setDisplayName(uniqueEndpointId)
                .setDescription("Insurance car embeddings endpoint for batch " + batchId)
                .build();

        // Create the endpoint - trying a different approach with the Future
        var futureResult = indexEndpointServiceClient.createIndexEndpointAsync(parent, endpoint);

        // Wait for completion (alternative to get with timeout)
        try {
            IndexEndpoint createdEndpoint = futureResult.get();
            String endpointId = createdEndpoint.getName();
            log.info("Created vector search endpoint: {}", endpointId);
            return endpointId;
        } catch (Exception e) {
            log.error("Error creating index endpoint: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deploy an index to an endpoint
     */
    private void deployIndex(String indexId, String endpointId) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {

        // Create the deployed index object - simplifying to only use available methods
        DeployedIndex deployedIndex = DeployedIndex.newBuilder()
                .setId("deployed-" + System.currentTimeMillis())  // Unique ID for deployed index
                .setIndex(indexId)
                .setDisplayName("Deployed Insurance Index")
                // Removing problematic authConfig/privateEndpoints options
                .build();

        // Deploy the index to the endpoint
        DeployIndexRequest request = DeployIndexRequest.newBuilder()
                .setIndexEndpoint(endpointId)
                .setDeployedIndex(deployedIndex)
                .build();

        // Wait for completion without timeout parameter
        try {
            indexEndpointServiceClient.deployIndexAsync(request).get();
            log.info("Deployed index {} to endpoint {}", indexId, endpointId);
        } catch (Exception e) {
            log.error("Error deploying index: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a vector search index
     */
    private String createIndex(String gcsUri, String batchId) throws IOException,
            InterruptedException, ExecutionException, TimeoutException {

        // Set the location path
        LocationName parent = LocationName.of(projectId, region);

        // Set unique index ID
        String uniqueIndexId = indexName + "-" + batchId;

        // Create metadata for the index using Struct
        Struct.Builder metadataStructBuilder = Struct.newBuilder();

        // Create config struct
        Struct.Builder configStruct = Struct.newBuilder();

        // Add dimensions
        configStruct.putFields(
                "dimensions",
                com.google.protobuf.Value.newBuilder().setNumberValue(embeddingDimension).build()
        );

        // Add approximate neighbors count
        configStruct.putFields(
                "approximateNeighborsCount",
                com.google.protobuf.Value.newBuilder().setNumberValue(150).build()
        );

        // Add distance measure type
        configStruct.putFields(
                "distanceMeasureType",
                com.google.protobuf.Value.newBuilder().setStringValue("COSINE").build()
        );

        // Create algorithm config
        Struct.Builder algorithmConfigStruct = Struct.newBuilder();

        // Create tree AH config
        Struct.Builder treeAhConfigStruct = Struct.newBuilder();
        treeAhConfigStruct.putFields(
                "leafNodeEmbeddingCount",
                com.google.protobuf.Value.newBuilder().setNumberValue(1000).build()
        );
        treeAhConfigStruct.putFields(
                "leafNodesToSearchPercent",
                com.google.protobuf.Value.newBuilder().setNumberValue(10).build()
        );

        // Add tree AH config to algorithm config
        algorithmConfigStruct.putFields(
                "treeAhConfig",
                com.google.protobuf.Value.newBuilder().setStructValue(treeAhConfigStruct.build()).build()
        );

        // Add algorithm config to main config
        configStruct.putFields(
                "algorithm_config",
                com.google.protobuf.Value.newBuilder().setStructValue(algorithmConfigStruct.build()).build()
        );

        // Add config to metadata
        metadataStructBuilder.putFields(
                "config",
                com.google.protobuf.Value.newBuilder().setStructValue(configStruct.build()).build()
        );

        // Add contentsDeltaUri to metadata
        metadataStructBuilder.putFields(
                "contentsDeltaUri",
                com.google.protobuf.Value.newBuilder().setStringValue(gcsUri).build()
        );

        // Convert metadata struct to value
        com.google.protobuf.Value metadata = com.google.protobuf.Value.newBuilder()
                .setStructValue(metadataStructBuilder.build())
                .build();

        // Create index object
        Index index = Index.newBuilder()
                .setDisplayName(uniqueIndexId)
                .setMetadata(metadata)
                .setDescription("Insurance car embeddings index for batch " + batchId)
                .build();

        // Create the index - without timeout parameter
        try {
            Index createdIndex = indexServiceClient.createIndexAsync(parent, index).get();
            String indexId = createdIndex.getName();
            log.info("Created vector search index: {}", indexId);
            return indexId;
        } catch (Exception e) {
            log.error("Error creating index: {}", e.getMessage(), e);
            throw e;
        }
    }
}