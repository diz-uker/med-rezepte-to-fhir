package io.github.dizuker.medrezeptetofhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import de.medizininformatikinitiative.kerndatensatz.medikation.Medikation;
import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import io.github.dizuker.tofhir.IdUtils;
import java.time.ZoneId;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedRezeptToMedicationRequestMapper {
  private static final Logger LOG =
      LoggerFactory.getLogger(MedRezeptToMedicationRequestMapper.class);

  private final MedRezepteToFhirProperties fhirProperties;

  public MedRezeptToMedicationRequestMapper(MedRezepteToFhirProperties properties) {
    this.fhirProperties = properties;
  }

  public MedicationRequest map(MedRezept rezept, Reference medicationReference) {

    var request = new MedicationRequest();

    var identifierValue = String.format("%s-%s", rezept.rezeptId(), rezept.rezeptPos());
    var identifier =
        new Identifier()
            .setSystem(fhirProperties.systems().identifiers().rezeptMedicationRequestId())
            .setValue(identifierValue);
    request.addIdentifier(identifier);
    request.setId(IdUtils.fromIdentifier(identifier));
    request.getMeta().addProfile(Medikation.Profiles.miiPrMedikationMedicationRequest());

    request.setStatus(MedicationRequestStatus.UNKNOWN);
    request.setIntent(MedicationRequestIntent.ORDER);
    request.setReported(new BooleanType(false));

    request
        .addCategory()
        .addCoding()
        .setSystem(fhirProperties.systems().medicationrequestCategory())
        .setCode("outpatient")
        .setDisplay("Outpatient");

    var patientIdType = new CodeableConcept();
    patientIdType
        .addCoding()
        .setSystem(fhirProperties.systems().identifierType())
        .setCode("MR")
        .setDisplay("Medical record number");
    var patientIdentifier =
        new Identifier()
            .setSystem(fhirProperties.systems().identifiers().patientId())
            .setValue(rezept.patientId())
            .setType(patientIdType);
    var patientId = IdUtils.fromIdentifier(patientIdentifier, ResourceType.Patient);
    var patientReference = new Reference(patientId).setIdentifier(patientIdentifier);
    request.setSubject(patientReference);

    if (rezept.rezeptDatum() != null) {
      var rezeptDatum = rezept.rezeptDatum().atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
      var authored = new DateTimeType(rezeptDatum.toString());
      authored.setPrecision(TemporalPrecisionEnum.DAY);
      request.setAuthoredOnElement(authored);
    } else {
      LOG.warn("Rezept Datum is unset");
    }

    if (StringUtils.isNotBlank(rezept.signatur())) {
      request.addDosageInstruction().setText(rezept.signatur());
    }

    if (StringUtils.isAllBlank(rezept.fallId())) {
      LOG.warn("Fall ID is unset, not setting encounter reference.");
    } else {
      var encounterIdType = new CodeableConcept();
      encounterIdType
          .addCoding()
          .setSystem(fhirProperties.systems().identifierType())
          .setCode("VN")
          .setDisplay("Visit number");
      var encounterIdentifier =
          new Identifier()
              .setSystem(fhirProperties.systems().identifiers().encounterId())
              .setValue(rezept.fallId())
              .setType(encounterIdType);
      var encounterId = IdUtils.fromIdentifier(encounterIdentifier, ResourceType.Encounter);
      var encounterReference = new Reference(encounterId).setIdentifier(encounterIdentifier);
      request.setEncounter(encounterReference);
    }

    request.setMedication(medicationReference);

    return request;
  }
}
