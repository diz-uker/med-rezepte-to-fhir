package io.github.dizuker.medrezeptetofhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import io.github.dizuker.tofhir.IdUtils;
import io.github.dizuker.tofhir.ReferenceUtils;
import io.github.dizuker.tofhir.TransactionBuilder;
import io.github.dizuker.tofhir.config.ToFhirProperties;
import java.time.ZoneId;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestIntent;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedRezeptToFhirBundleMapper {
  private static final Logger LOG = LoggerFactory.getLogger(MedRezeptToFhirBundleMapper.class);
  private final FhirProperties fhirProperties;
  private final ToFhirProperties toFhirProperties;
  private final DeviceMapper deviceMapper;

  public MedRezeptToFhirBundleMapper(
      FhirProperties properties, ToFhirProperties toFhirProperties, DeviceMapper deviceMapper) {
    this.fhirProperties = properties;
    this.toFhirProperties = toFhirProperties;
    this.deviceMapper = deviceMapper;
  }

  public Optional<Bundle> map(MedRezept rezept) {
    if (StringUtils.isBlank(rezept.pzn())) {
      LOG.warn("PZN is unset, skipping.");
      return Optional.empty();
    }

    if (StringUtils.isBlank(rezept.rezeptId())) {
      LOG.warn("Rezept ID is unset, skipping.");
      return Optional.empty();
    }

    if (StringUtils.isBlank(rezept.rezeptPos())) {
      LOG.warn("Rezept Position is unset, skipping.");
      return Optional.empty();
    }

    var request = new MedicationRequest();

    var identifierValue = String.format("%s-%s", rezept.rezeptId(), rezept.rezeptPos());
    var identifier =
        new Identifier()
            .setSystem(fhirProperties.systems().identifiers().rezeptMedicationRequestId())
            .setValue(identifierValue);
    request.addIdentifier(identifier);
    request.setId(IdUtils.fromIdentifier(identifier));
    request.getMeta().addProfile(fhirProperties.profiles().miiMedicationRequest());

    request.setStatus(MedicationRequestStatus.ACTIVE);
    request.setIntent(MedicationRequestIntent.ORDER);
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

    request.addDosageInstruction().setText(rezept.signatur());

    if (StringUtils.isAllBlank(rezept.fallId())) {
      LOG.warn("Fall ID is unset, skipping encounter reference.");
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

    var medication = mapMedication(rezept);
    var medicationReference =
        ReferenceUtils.createReferenceTo(medication).setDisplay(rezept.verschreibung());
    request.setMedication(medicationReference);

    var device = deviceMapper.map();
    var what =
        new Reference()
            .setDisplay("Rezept ID: " + rezept.rezeptId() + ", Rezept Pos: " + rezept.rezeptPos());

    var trxBuilder =
        new TransactionBuilder()
            .withId(request.getId())
            .withType(BundleType.TRANSACTION)
            .withProvenance(ReferenceUtils.createReferenceTo(device), what)
            .failOnDuplicateEntries()
            .addEntry(request)
            .addEntry(medication)
            .addEntry(device);
    return Optional.of(trxBuilder.build());
  }

  private Medication mapMedication(MedRezept rezept) {
    var medication = new Medication();
    var medicationIdentifier =
        new Identifier()
            .setSystem(fhirProperties.systems().identifiers().rezeptMedicationId())
            .setValue(rezept.pzn());
    medication.addIdentifier(medicationIdentifier);
    medication.setId(IdUtils.fromIdentifier(medicationIdentifier));
    medication.getMeta().addProfile(fhirProperties.profiles().miiMedication());
    var coding = toFhirProperties.fhir().codings().pzn().setCode(rezept.pzn());
    var code = new CodeableConcept(coding).setText(rezept.verschreibung());
    medication.setCode(code);

    var ingredient = toFhirProperties.fhir().codings().snomed();
    ingredient
        .getCodeElement()
        .addExtension(
            toFhirProperties
                .fhir()
                .extensions()
                .dataAbsentReason()
                .setValue(new CodeType("unknown")));
    medication.addIngredient().setItem(new CodeableConcept().addCoding(ingredient));
    return medication;
  }
}
