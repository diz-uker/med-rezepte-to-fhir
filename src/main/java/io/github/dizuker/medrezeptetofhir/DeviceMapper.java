package io.github.dizuker.medrezeptetofhir;

import io.github.dizuker.tofhir.IdUtils;
import io.github.dizuker.tofhir.config.ToFhirProperties;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Device.DeviceNameType;
import org.hl7.fhir.r4.model.Device.FHIRDeviceStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.springframework.stereotype.Service;

@Service
public class DeviceMapper {
  private final ConfigProperties config;
  private ToFhirProperties fhirProperties;

  public DeviceMapper(ToFhirProperties fhirProperties, ConfigProperties config) {
    this.fhirProperties = fhirProperties;
    this.config = config;
  }

  public Device map() {
    var device = new Device();
    var identifier =
        new Identifier()
            .setSystem(config.fhir().systems().identifiers().deviceId())
            .setValue("med-rezepte-to-fhir-v" + config.appVersion());
    device.addIdentifier(identifier);
    device.setId(IdUtils.fromIdentifier(identifier));
    device.setStatus(FHIRDeviceStatus.ACTIVE);
    device.setManufacturer("https://github.com/diz-uker");
    device.addDeviceName().setName("Med Rezepte to FHIR").setType(DeviceNameType.USERFRIENDLYNAME);
    device.setType(
        new CodeableConcept(
            fhirProperties
                .fhir()
                .codings()
                .snomed()
                .setCode("706689003")
                .setDisplay("Application program software (physical object)")));

    device.addVersion().setValue(config.appVersion());
    device
        .addContact()
        .setSystem(ContactPointSystem.URL)
        .setValue("https://github.com/diz-uker/med-rezepte-to-fhir/issues");

    return device;
  }
}
