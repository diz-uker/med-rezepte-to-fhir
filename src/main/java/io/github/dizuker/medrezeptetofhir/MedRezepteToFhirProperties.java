package io.github.dizuker.medrezeptetofhir;

import io.github.dizuker.tofhir.config.FhirProperties;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Extends the to-fhir starter's {@link FhirProperties} with this application's own systems and
 * codings, while still binding from the shared {@code fhir} prefix.
 */
@ConfigurationProperties(prefix = "fhir")
public class MedRezepteToFhirProperties extends FhirProperties {
  private Systems systems = new Systems();
  private Codings codings = new Codings();
  private Profiles profiles = new Profiles(null, null);
  private String sourceSystemValueTemplate;

  @Override
  public Systems systems() {
    return systems;
  }

  /** Used by Spring Boot for property binding. */
  public void setSystems(Systems systems) {
    this.systems = systems;
  }

  @Override
  public Codings codings() {
    return codings;
  }

  /** Used by Spring Boot for property binding. */
  public void setCodings(Codings codings) {
    this.codings = codings;
  }

  /** Returns the FHIR profiles. */
  public Profiles profiles() {
    return profiles;
  }

  /** Used by Spring Boot for property binding. */
  public void setProfiles(Profiles profiles) {
    this.profiles = profiles;
  }

  /** Returns the template used to build the source system identifier value. */
  public String sourceSystemValueTemplate() {
    return sourceSystemValueTemplate;
  }

  /** Used by Spring Boot for property binding. */
  public void setSourceSystemValueTemplate(String sourceSystemValueTemplate) {
    this.sourceSystemValueTemplate = sourceSystemValueTemplate;
  }

  /** Application-specific FHIR systems, extending the to-fhir starter's defaults. */
  public static class Systems extends FhirProperties.Systems {
    private Identifiers identifiers = new Identifiers();
    private String medicationrequestCategory;
    private String identifierType;

    /** Returns the identifier systems. */
    public Identifiers identifiers() {
      return identifiers;
    }

    /** Used by Spring Boot for property binding. */
    public void setIdentifiers(Identifiers identifiers) {
      this.identifiers = identifiers;
    }

    /** Returns the medication request category system. */
    public String medicationrequestCategory() {
      return medicationrequestCategory;
    }

    /** Used by Spring Boot for property binding. */
    public void setMedicationrequestCategory(String medicationrequestCategory) {
      this.medicationrequestCategory = medicationrequestCategory;
    }

    /** Returns the identifier type system. */
    public String identifierType() {
      return identifierType;
    }

    /** Used by Spring Boot for property binding. */
    public void setIdentifierType(String identifierType) {
      this.identifierType = identifierType;
    }
  }

  /** Application-specific identifier systems. */
  public static class Identifiers {
    private String patientId;
    private String encounterId;
    private String rezeptMedicationRequestId;
    private String rezeptMedicationId;
    private String deviceId;
    private String sourceSystem;

    /** Returns the patient identifier system. */
    public String patientId() {
      return patientId;
    }

    /** Used by Spring Boot for property binding. */
    public void setPatientId(String patientId) {
      this.patientId = patientId;
    }

    /** Returns the encounter identifier system. */
    public String encounterId() {
      return encounterId;
    }

    /** Used by Spring Boot for property binding. */
    public void setEncounterId(String encounterId) {
      this.encounterId = encounterId;
    }

    /** Returns the rezept medication request identifier system. */
    public String rezeptMedicationRequestId() {
      return rezeptMedicationRequestId;
    }

    /** Used by Spring Boot for property binding. */
    public void setRezeptMedicationRequestId(String rezeptMedicationRequestId) {
      this.rezeptMedicationRequestId = rezeptMedicationRequestId;
    }

    /** Returns the rezept medication identifier system. */
    public String rezeptMedicationId() {
      return rezeptMedicationId;
    }

    /** Used by Spring Boot for property binding. */
    public void setRezeptMedicationId(String rezeptMedicationId) {
      this.rezeptMedicationId = rezeptMedicationId;
    }

    /** Returns the device identifier system. */
    public String deviceId() {
      return deviceId;
    }

    /** Used by Spring Boot for property binding. */
    public void setDeviceId(String deviceId) {
      this.deviceId = deviceId;
    }

    /** Returns the source system identifier system. */
    public String sourceSystem() {
      return sourceSystem;
    }

    /** Used by Spring Boot for property binding. */
    public void setSourceSystem(String sourceSystem) {
      this.sourceSystem = sourceSystem;
    }
  }

  /** Application-specific FHIR codings, extending the to-fhir starter's defaults. */
  public static class Codings extends FhirProperties.Codings {
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

  /** FHIR profiles used by this application. */
  public record Profiles(String miiMedication, String miiMedicationRequest) {}
}
