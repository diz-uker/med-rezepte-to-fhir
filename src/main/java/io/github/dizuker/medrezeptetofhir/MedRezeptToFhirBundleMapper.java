package io.github.dizuker.medrezeptetofhir;

import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import io.github.dizuker.tofhir.ReferenceUtils;
import io.github.dizuker.tofhir.TransactionBuilder;
import io.github.dizuker.tofhir.TransactionBuilder.DataAndProvenanceBundles;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MedRezeptToFhirBundleMapper {
  private static final Logger LOG = LoggerFactory.getLogger(MedRezeptToFhirBundleMapper.class);

  private final MedRezepteToFhirProperties fhirProperties;
  private final DeviceMapper deviceMapper;
  private final MedRezeptToMedicationMapper medicationMapper;
  private final MedRezeptToMedicationRequestMapper medicationRequestMapper;

  public MedRezeptToFhirBundleMapper(
      MedRezepteToFhirProperties properties,
      DeviceMapper deviceMapper,
      MedRezeptToMedicationMapper medicationMapper,
      MedRezeptToMedicationRequestMapper medicationRequestMapper) {
    this.fhirProperties = properties;
    this.deviceMapper = deviceMapper;
    this.medicationMapper = medicationMapper;
    this.medicationRequestMapper = medicationRequestMapper;
  }

  public Optional<DataAndProvenanceBundles> map(MedRezept rezept) {
    if (StringUtils.isBlank(rezept.rezeptId())) {
      LOG.warn("Rezept ID is unset, skipping.");
      return Optional.empty();
    }

    if (StringUtils.isBlank(rezept.rezeptPos())) {
      LOG.warn("Rezept Position is unset, skipping.");
      return Optional.empty();
    }

    if (StringUtils.isBlank(rezept.verschreibung())) {
      LOG.warn("Verschreibung is unset, skipping.");
      return Optional.empty();
    }

    var medication = medicationMapper.map(rezept);
    var medicationReference =
        ReferenceUtils.createReferenceTo(medication).setDisplay(medication.getCode().getText());

    var request = medicationRequestMapper.map(rezept, medicationReference);

    if (StringUtils.isNotBlank(fhirProperties.metaSource())) {
      request.getMeta().setSource(fhirProperties.metaSource());
      medication.getMeta().setSource(fhirProperties.metaSource());
    }

    var device = deviceMapper.map();

    var sourceSystemValue =
        String.format(
            fhirProperties.sourceSystemValueTemplate(), rezept.rezeptId(), rezept.rezeptPos());
    var what =
        new Reference()
            .setIdentifier(
                new Identifier()
                    .setSystem(fhirProperties.systems().identifiers().sourceSystem())
                    .setValue(sourceSystemValue));

    var trxBuilder =
        new TransactionBuilder()
            .withId(request.getId())
            .withType(BundleType.TRANSACTION)
            .failOnDuplicateEntries()
            .withFullUrlBase(fhirProperties.systems().fullUrlBase())
            .withProvenance(device, what)
            .addEntries(request, medication);
    return Optional.of(trxBuilder.buildWithSeparateProvenance());
  }
}
