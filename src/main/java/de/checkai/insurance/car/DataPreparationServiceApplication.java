package de.checkai.insurance.car;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
/**
 * @author Mostafa.Farhadi
 * @email farhadi.kam@gmail.com
 * @linkdin https://www.linkedin.com/in/mostafa-farhadi-1360/
 * @github https://github.com/farhadi1360
 */
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
@OpenAPIDefinition(
		info = @Info(
				title = "Data Preparation Service API",
				version = "1.0",
				description = "Service for processing insurance PDFs, generating embeddings, and storing them in Vertex AI Vector Search"
		)
)
public class DataPreparationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataPreparationServiceApplication.class, args);
	}

}
