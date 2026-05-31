package io.github.dizuker.medrezeptetofhir.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public record MedRezept(
    @JsonProperty("REZEPT_ID") String rezeptId,
    @JsonProperty("REZEPT_QUELLE") String rezeptQuelle,
    @JsonProperty("FALL_ID") String fallId,
    @JsonProperty("PATIENT_ID") String patientId,
    @JsonProperty("REZEPT_DATUM")
        @JsonFormat(without = JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
        Instant rezeptDatum,
    @JsonProperty("PRIVAT_FLAG") String privatFlag,
    @JsonProperty("VERSCHREIBUNG") String verschreibung,
    @JsonProperty("SIGNATUR") String signatur,
    @JsonProperty("REZEPT_POS") String rezeptPos,
    @JsonProperty("PZN") String pzn,
    @JsonProperty("PACKAGE_NAME") String packageName,
    @JsonProperty("ATC_CODES") @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> atcCodes,
    @JsonProperty("INGREDIENTS") @JsonSetter(nulls = Nulls.AS_EMPTY) List<Ingredient> ingredients) {
  public static record Ingredient(String name, String ask) {}
}
