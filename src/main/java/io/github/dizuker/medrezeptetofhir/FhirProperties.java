package io.github.dizuker.medrezeptetofhir;

import org.hl7.fhir.r4.model.Coding;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "fhir")
@ConfigurationPropertiesScan
public record FhirProperties(
    Systems systems, Profiles profiles, String sourceSystemValueTemplate, Codings codings) {
  public record Systems(
      Identifiers identifiers, String medicationrequestCategory, String identifierType) {}

  public record Identifiers(
      String patientId,
      String encounterId,
      String rezeptMedicationRequestId,
      String rezeptMedicationId,
      String deviceId,
      String sourceSystem) {}

  public record Codings(Coding ask) {
    @Override
    public Coding ask() {
      return ask.copy();
    }
  }

  public record Profiles(String miiMedication, String miiMedicationRequest) {}
}
