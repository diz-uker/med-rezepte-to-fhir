package io.github.dizuker.medrezeptetofhir;

import io.github.dizuker.medrezeptetofhir.models.MedRezept;
import java.util.function.Function;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class MedRezepteToFhirProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(MedRezepteToFhirProcessor.class);
  private final MedRezeptToFhirBundleMapper mapper;
  private final StreamBridge streamBridge;

  public MedRezepteToFhirProcessor(MedRezeptToFhirBundleMapper mapper, StreamBridge streamBridge) {
    super();
    this.mapper = mapper;
    this.streamBridge = streamBridge;
  }

  @Bean
  Function<Message<MedRezept>, Message<Bundle>> sink() {
    return message -> {
      if (message == null) {
        LOG.warn("message is null. Ignoring.");
        return null;
      }

      var rezept = message.getPayload();

      try (var _ = MDC.putCloseable("rezeptId", rezept.rezeptId());
          var _ = MDC.putCloseable("rezeptPos", rezept.rezeptPos()); ) {
        LOG.debug("Processing rezept");
        var mapped = mapper.map(rezept);
        if (mapped.isPresent()) {
          var bundle = mapped.get().dataBundle();
          var messageKey = bundle.getId();

          var provenance = mapped.get().provenanceBundle();

          streamBridge.send(
              "provenance-out-0",
              MessageBuilder.withPayload(provenance)
                  .setHeader(KafkaHeaders.KEY, "provenance-" + messageKey)
                  .build());

          var messageBuilder =
              MessageBuilder.withPayload(bundle).setHeader(KafkaHeaders.KEY, messageKey);

          return messageBuilder.build();
        } else {
          return null;
        }
      }
    };
  }
}
