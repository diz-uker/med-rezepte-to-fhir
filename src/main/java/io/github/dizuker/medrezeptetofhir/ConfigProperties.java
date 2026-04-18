package io.github.dizuker.medrezeptetofhir;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties
@ConfigurationPropertiesScan
public record ConfigProperties(String appVersion, FhirProperties fhir) {
  public record FhirProperties(Systems systems, Profiles profiles) {
    public record Systems(
        Identifiers identifiers, String medicationrequestCategory, String identifierType) {}

    public record Identifiers(
        String patientId,
        String encounterId,
        String rezeptMedicationRequestId,
        String rezeptMedicationId,
        String deviceId) {}

    public record Profiles(String miiMedication, String miiMedicationRequest) {}
  }
}
