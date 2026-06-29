package io.github.dizuker.medrezeptetofhir;

import io.github.dizuker.tofhir.config.FhirProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Extends the to-fhir starter's {@link FhirProperties} with this application's own systems and
 * codings, while still binding from the shared {@code fhir} prefix.
 *
 * <p>Getters use Lombok's fluent (no-"get"-prefix) style to match {@link FhirProperties}'
 * convention; setters stay hand-written with the standard "set" prefix, since Spring's
 * {@code @ConfigurationProperties} binder doesn't recognize fluent (no-prefix) setters.
 */
@Getter
@Accessors(fluent = true)
@ConfigurationProperties(prefix = "fhir")
public class MedRezepteToFhirProperties extends FhirProperties {
  private Systems systems = new Systems();
  private Codings codings = new Codings();
  private String sourceSystemValueTemplate;

  /** Used by Spring Boot for property binding. */
  public void setSystems(Systems systems) {
    this.systems = systems;
  }

  /** Used by Spring Boot for property binding. */
  public void setCodings(Codings codings) {
    this.codings = codings;
  }

  /** Used by Spring Boot for property binding. */
  public void setSourceSystemValueTemplate(String sourceSystemValueTemplate) {
    this.sourceSystemValueTemplate = sourceSystemValueTemplate;
  }

  /** Application-specific FHIR systems, extending the to-fhir starter's defaults. */
  @Getter
  @Accessors(fluent = true)
  public static class Systems extends FhirProperties.Systems {
    private Identifiers identifiers = new Identifiers();
    private String medicationrequestCategory;
    private String identifierType;

    /** Used by Spring Boot for property binding. */
    public void setIdentifiers(Identifiers identifiers) {
      this.identifiers = identifiers;
    }

    /** Used by Spring Boot for property binding. */
    public void setMedicationrequestCategory(String medicationrequestCategory) {
      this.medicationrequestCategory = medicationrequestCategory;
    }

    /** Used by Spring Boot for property binding. */
    public void setIdentifierType(String identifierType) {
      this.identifierType = identifierType;
    }
  }

  /** Application-specific identifier systems. */
  @Getter
  @Accessors(fluent = true)
  public static class Identifiers {
    private String patientId;
    private String encounterId;
    private String rezeptMedicationRequestId;
    private String rezeptMedicationId;
    private String deviceId;
    private String sourceSystem;

    /** Used by Spring Boot for property binding. */
    public void setPatientId(String patientId) {
      this.patientId = patientId;
    }

    /** Used by Spring Boot for property binding. */
    public void setEncounterId(String encounterId) {
      this.encounterId = encounterId;
    }

    /** Used by Spring Boot for property binding. */
    public void setRezeptMedicationRequestId(String rezeptMedicationRequestId) {
      this.rezeptMedicationRequestId = rezeptMedicationRequestId;
    }

    /** Used by Spring Boot for property binding. */
    public void setRezeptMedicationId(String rezeptMedicationId) {
      this.rezeptMedicationId = rezeptMedicationId;
    }

    /** Used by Spring Boot for property binding. */
    public void setDeviceId(String deviceId) {
      this.deviceId = deviceId;
    }

    /** Used by Spring Boot for property binding. */
    public void setSourceSystem(String sourceSystem) {
      this.sourceSystem = sourceSystem;
    }
  }

  /** Application-specific FHIR codings, extending the to-fhir starter's defaults. */
  @Getter
  @Accessors(fluent = true)
  public static class Codings extends FhirProperties.Codings {
    @Getter(AccessLevel.NONE)
    private Coding ask = new Coding();

    /** Returns a fresh copy of the ASK coding. */
    public Coding ask() {
      // return a fresh copy, otherwise the original instance will be modified
      return ask.copy();
    }

    /** Used by Spring Boot for property binding. */
    public void setAsk(Coding ask) {
      this.ask = ask;
    }
  }
}
