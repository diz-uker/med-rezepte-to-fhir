package io.github.dizuker.medrezeptetofhir.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
public record MedRezept(
    String rezeptId,
    String rezeptQuelle,
    String fallId,
    String patientId,
    @JsonFormat(without = JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
        Instant rezeptDatum,
    String privatFlag,
    String verschreibung,
    String signatur,
    String rezeptPos,
    String pzn,
    String packageName,
    String ifaPharmFormCode,
    String amountText,
    Double amount,
    Double factor1,
    Double factor2,
    String packageUnitCode,
    @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> atcCodes,
    @JsonSetter(nulls = Nulls.AS_EMPTY) List<Ingredient> ingredients) {
  public static record Ingredient(String name, String ask) {}
}
