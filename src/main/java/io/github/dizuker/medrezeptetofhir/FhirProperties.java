package io.github.dizuker.medrezeptetofhir;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "fhir")
@ConfigurationPropertiesScan
public record FhirProperties(Systems systems, Profiles profiles, String sourceSystemValueTemplate) {
  public record Systems(
      Identifiers identifiers, String medicationrequestCategory, String identifierType) {}

  public record Identifiers(
      String patientId,
      String encounterId,
      String rezeptMedicationRequestId,
      String rezeptMedicationId,
      String deviceId,
      String sourceSystem) {}

  public record Profiles(String miiMedication, String miiMedicationRequest) {}
}
