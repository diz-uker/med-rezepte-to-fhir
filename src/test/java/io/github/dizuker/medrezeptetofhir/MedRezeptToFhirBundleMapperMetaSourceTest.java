package io.github.dizuker.medrezeptetofhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "fhir.meta-source=test-data-source:${app-version}")
@Import(TestChannelBinderConfiguration.class)
class MedRezeptToFhirBundleMapperMetaSourceTest {
  @Autowired private MedRezeptToFhirBundleMapper sut;

  @Test
  void map_withMetaSourceSet_shouldSetMetaSourceOnDataResources() throws IOException {
    final var recordStream = this.getClass().getClassLoader().getResource("fixtures/rezept-6.json");
    var mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    final var rezept = mapper.readValue(recordStream.openStream(), MedRezept.class);

    var mapped = sut.map(rezept).orElseThrow();

    var resources = mapped.dataBundle().getEntry().stream().map(e -> e.getResource()).toList();
    Assertions.assertThat(resources)
        .isNotEmpty()
        .extracting((Resource r) -> r.getMeta().getSource())
        .containsOnly("test-data-source:0.0.0-test");
  }
}
