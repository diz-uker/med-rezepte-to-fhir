package io.github.dizuker.medrezeptetofhir;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import java.io.IOException;
import org.approvaltests.Approvals;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.RegExScrubber;
import org.approvaltests.scrubbers.Scrubbers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
class MedRezeptToFhirBundleMapperTest {
  private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
  public static final Scrubber FHIR_DATE_TIME_SCRUBBER =
      Scrubbers.scrubAll(
          new RegExScrubber(
              "\"occurredDateTime\": \"(.*)\"", "\"occurredDateTime\": \"2000-01-01T11:11:11Z\""),
          new RegExScrubber("\"recorded\": \"(.*)\"", "\"recorded\": \"2000-01-01T11:11:11Z\""));
  @Autowired private MedRezeptToFhirBundleMapper sut;

  @ParameterizedTest
  @CsvSource({
    "rezept-1.json",
    "rezept-2.json",
    "rezept-null.json",
  })
  void map_withGivenMedRezeptRecord_shouldCreateExpectedFhirBundle(String sourceFile)
      throws IOException {
    final var recordStream = this.getClass().getClassLoader().getResource("fixtures/" + sourceFile);
    var mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    final var rezept = mapper.readValue(recordStream.openStream(), MedRezept.class);

    var mapped = sut.map(rezept).orElseThrow();

    var fhirParser = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);
    Approvals.verify(
        fhirParser.encodeResourceToString(mapped.dataBundle()),
        Approvals.NAMES
            .withParameters(sourceFile)
            .withScrubber(FHIR_DATE_TIME_SCRUBBER)
            .forFile()
            .withExtension(".data.fhir.json"));

    Approvals.verify(
        fhirParser.encodeResourceToString(mapped.provenanceBundle()),
        Approvals.NAMES
            .withParameters(sourceFile)
            .withScrubber(FHIR_DATE_TIME_SCRUBBER)
            .forFile()
            .withExtension(".provenance.fhir.json"));
  }
}
