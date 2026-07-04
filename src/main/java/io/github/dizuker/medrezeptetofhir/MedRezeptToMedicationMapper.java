package io.github.dizuker.medrezeptetofhir;

import com.github.slugify.Slugify;
import de.medizininformatikinitiative.kerndatensatz.medikation.Medikation;
import io.github.dizuker.medrezeptetofhir.edqm.IfaDoseFormMapper;
import io.github.dizuker.medrezeptetofhir.kbv.KbvDarreichungsform;
import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import io.github.dizuker.tofhir.IdUtils;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Medication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedRezeptToMedicationMapper {
  private static final Logger LOG = LoggerFactory.getLogger(MedRezeptToMedicationMapper.class);
  private static final Slugify slugifier =
      Slugify.builder().lowerCase(true).locale(Locale.GERMAN).build();

  private final MedRezepteToFhirProperties fhirProperties;

  public MedRezeptToMedicationMapper(MedRezepteToFhirProperties fhirProperties) {
    this.fhirProperties = fhirProperties;
  }

  public Medication map(MedRezept rezept) {
    var medication = new Medication();
    var medicationIdentifier =
        new Identifier().setSystem(fhirProperties.systems().identifiers().rezeptMedicationId());

    if (StringUtils.isNotBlank(rezept.pzn())) {
      medicationIdentifier.setValue(rezept.pzn());
    } else {
      medicationIdentifier.setValue(slugifier.slugify(rezept.verschreibung()));
    }

    medication.addIdentifier(medicationIdentifier);
    medication.setId(IdUtils.fromIdentifier(medicationIdentifier));
    medication.getMeta().addProfile(Medikation.Profiles.miiPrMedikationMedication());
    var code = new CodeableConcept().setText(rezept.verschreibung());

    if (StringUtils.isNotBlank(rezept.pzn())) {
      var pznCoding = fhirProperties.codings().pzn().setCode(rezept.pzn());
      if (StringUtils.isNotBlank(rezept.packageName())) {
        pznCoding.setDisplay(rezept.packageName());
        code.setText(rezept.packageName());
      }
      code.addCoding(pznCoding);
    } else {
      LOG.warn("PZN is blank, not adding PZN coding to medication code.");
    }

    // usually this is only filled if the PZN is also filled,
    // so we could move it to the check above. But this reduces
    // nesting a bit.
    if (!rezept.atcCodes().isEmpty()) {
      for (var atcCode : rezept.atcCodes()) {
        var atcCoding = fhirProperties.codings().atc().setCode(atcCode);
        code.addCoding(atcCoding);
      }
    }

    medication.setCode(code);

    if (!rezept.ingredients().isEmpty()) {
      for (var ingredient : rezept.ingredients()) {
        var ingredientConcept = new CodeableConcept().setText(ingredient.name());
        var coding = fhirProperties.codings().ask();

        if (StringUtils.isNotBlank(ingredient.ask())) {
          coding.setCode(ingredient.ask());
        } else {
          coding
              .getCodeElement()
              .addExtension(
                  fhirProperties.extensions().dataAbsentReason().setValue(new CodeType("as-text")));
        }

        ingredientConcept.addCoding(coding);
        medication.addIngredient().setItem(ingredientConcept);
      }
    } else {
      var ingredient = new CodeableConcept();
      ingredient
          .addCoding()
          .addExtension(
              fhirProperties
                  .extensions()
                  .dataAbsentReason()
                  .setValue(new CodeType("asked-unknown")));
      medication.addIngredient().setItem(ingredient);
    }

    if (StringUtils.isNotBlank(rezept.ifaPharmFormCode())) {
      var formConcept = new CodeableConcept().setText(rezept.ifaPharmFormCode());

      IfaDoseFormMapper.lookup(rezept.ifaPharmFormCode())
          .ifPresentOrElse(
              formConcept::addCoding,
              () -> LOG.warn("IFA code {} not found in EDQM mapping", rezept.ifaPharmFormCode()));
      KbvDarreichungsform.CodeSystems.KbvCsSfhirKbvDarreichungsform.fromValue(
              rezept.ifaPharmFormCode())
          .map(c -> c.coding())
          .ifPresentOrElse(
              formConcept::addCoding,
              () -> LOG.warn("IFA code {} not found in KBV mapping", rezept.ifaPharmFormCode()));

      medication.setForm(formConcept);
    }

    return medication;
  }
}
