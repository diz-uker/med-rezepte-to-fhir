package io.github.dizuker.medrezeptetofhir.mappings;

import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Reference;

public class MedRezeptToFhirBundleMapper {
  private static final String PZN_SYSTEM = "http://fhir.de/CodeSystem/ifa/pzn";

  public Optional<Bundle> map(MedRezept rezept) {
    if (rezept == null
        || isBlank(rezept.rezeptId())
        || isBlank(rezept.patientId())
        || isBlank(rezept.verschreibung())) {
      return Optional.empty();
    }

    var request = new MedicationRequest();
    request.setId(rezept.rezeptId());
    request.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
    request.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
    request.setSubject(new Reference("Patient/" + rezept.patientId()));

    if (rezept.rezeptDatum() != null) {
      request.setAuthoredOn(new Date(rezept.rezeptDatum()));
    }

    request
        .addIdentifier()
        .setSystem("https://diz-uker.github.io/med-rezepte-to-fhir/rezept-id")
        .setValue(rezept.rezeptId());

    if (!isBlank(rezept.rezeptPos())) {
      request
          .addIdentifier(
              new Identifier()
                  .setSystem("https://diz-uker.github.io/med-rezepte-to-fhir/rezept-pos")
                  .setValue(rezept.rezeptPos()));
    }

    var medication = new CodeableConcept().setText(rezept.verschreibung());
    if (!isBlank(rezept.pzn())) {
      medication.addCoding(new Coding().setSystem(PZN_SYSTEM).setCode(rezept.pzn()));
    }
    request.setMedication(medication);

    if (!isBlank(rezept.signatur())) {
      request.addDosageInstruction(new Dosage().setText(rezept.signatur()));
    }

    var bundle = new Bundle();
    bundle.setId(rezept.rezeptId());
    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.addEntry().setResource(request);

    return Optional.of(bundle);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
