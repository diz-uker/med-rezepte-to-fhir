package io.github.dizuker.medrezeptetofhir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationPropertiesScan
public class MedRezepteToFhirApplication {

  public static void main(String[] args) {
    SpringApplication.run(MedRezepteToFhirApplication.class, args);
  }
}
