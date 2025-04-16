package de.checkai.insurance.car.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.IndexEndpointServiceClient;
import com.google.cloud.aiplatform.v1.IndexServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */

@Configuration
@RequiredArgsConstructor
public class GoogleCloudConfiguration {
    private final GcpProjectIdProvider projectIdProvider;
    private final CredentialsProvider credentialsProvider;

    @Value("${gcp.vertex.region}")
    private String region;

    @Bean
    public Storage storage() throws IOException {
        return StorageOptions.newBuilder()
                .setProjectId(projectIdProvider.getProjectId())
                .setCredentials(credentialsProvider.getCredentials())
                .build()
                .getService();
    }

    @Bean
    public DocumentProcessorServiceClient documentProcessorServiceClient() throws IOException {
        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                .setCredentialsProvider(credentialsProvider)
                .build();
        return DocumentProcessorServiceClient.create(settings);
    }

    @Bean
    public IndexServiceClient indexServiceClient() throws IOException {
        return IndexServiceClient.create();
    }

    @Bean
    public IndexEndpointServiceClient indexEndpointServiceClient() throws IOException {
        return IndexEndpointServiceClient.create();
    }
}
