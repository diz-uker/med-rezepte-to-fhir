package io.github.dizuker.medrezeptetofhir.mappings;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import java.io.IOException;
import org.approvaltests.Approvals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MedRezeptToFhirBundleMapperTest {
  private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

  private final MedRezeptToFhirBundleMapper sut = new MedRezeptToFhirBundleMapper();

  @ParameterizedTest
  @CsvSource({
    "rezept-1.json",
  })
  void map_withGivenMedRezeptRecord_shouldCreateExpectedFhirBundle(String sourceFile)
      throws IOException {
    final var recordStream = this.getClass().getClassLoader().getResource("fixtures/" + sourceFile);
    var mapper = new ObjectMapper();
    final var record = mapper.readValue(recordStream.openStream(), MedRezept.class);

    var mapped = sut.map(record).orElseThrow();

    var fhirParser = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);
    var fhirJson = fhirParser.encodeResourceToString(mapped);
    Approvals.verify(
        fhirJson, Approvals.NAMES.withParameters(sourceFile).forFile().withExtension(".fhir.json"));
  }
}
