package io.github.dizuker.medrezeptetofhir;

import java.util.Map;
import java.util.Optional;

public final class IfaUnitMapper {

  /**
   * @param unit human-readable UCUM unit string, suitable for use in a FHIR {@code Quantity.unit}
   * @param code machine-readable UCUM code, suitable for use in a FHIR {@code Quantity.code};
   *     {@code null} when no standard UCUM code exists
   */
  public record UcumUnit(String unit, String code) {
    public boolean isMapped() {
      return code != null;
    }
  }

  // Maps IFA molecule-unit codes to their UCUM equivalents.
  // Source: internal IFA unit list (descending frequency order, incomplete — E and MGMW missing).
  private static final Map<String, UcumUnit> IFA_TO_UCUM =
      Map.ofEntries(
          Map.entry("MG", new UcumUnit("mg", "mg")),
          Map.entry("G", new UcumUnit("g", "g")),
          Map.entry("IE", new UcumUnit("[IU]", "[IU]")),
          Map.entry("MMOL", new UcumUnit("mmol", "mmol")),
          Map.entry("MMOL/L", new UcumUnit("mmol/L", "mmol/L")),
          Map.entry("UG", new UcumUnit("ug", "ug")),
          Map.entry("E", new UcumUnit("U", "U")),
          Map.entry("PEE", new UcumUnit("{PEE}", null)),
          Map.entry("ML", new UcumUnit("mL", "mL")),
          Map.entry("MG/H", new UcumUnit("mg/h", "mg/h")),
          Map.entry("MG/D", new UcumUnit("mg/d", "mg/d")),
          Map.entry("SPOON", new UcumUnit("{spoon}", "1")),
          Map.entry("UMOL", new UcumUnit("umol", "umol")),
          Map.entry("DRIPS", new UcumUnit("[drp]", "[drp]")),
          Map.entry("PERCENT", new UcumUnit("%", "%")),
          Map.entry("MRDKEIM", new UcumUnit("10*9[CFU]", "10*9[CFU]")),
          Map.entry("KEIM", new UcumUnit("[CFU]", "[CFU]")),
          Map.entry("FIP-E", new UcumUnit("{FIP_E}", null)),
          Map.entry("MVAL", new UcumUnit("meq", "meq")),
          Map.entry("FIPE", new UcumUnit("{FIP_E}", null)),
          Map.entry("PCS", new UcumUnit("{pcs}", "1")),
          Map.entry("mg", new UcumUnit("mg", "mg")),
          Map.entry("MKEIM", new UcumUnit("10*6[CFU]", "10*6[CFU]")),
          Map.entry("HUB", new UcumUnit("{actuation}", "1")),
          Map.entry("BE", new UcumUnit("{BE}", null)),
          Map.entry("MRD", new UcumUnit("10*9", "10*9")),
          Map.entry("UL", new UcumUnit("uL", "uL")),
          Map.entry("GKID50", new UcumUnit("[TCID_50]", "[TCID_50]")),
          Map.entry("GI", new UcumUnit("10*9", "10*9")),
          Map.entry("VOLPERCEN", new UcumUnit("%{vol}", "%")),
          Map.entry("CELLS", new UcumUnit("{cells}", "1")),
          Map.entry("USPE", new UcumUnit("{USPE}", null)),
          Map.entry("L", new UcumUnit("L", "L")),
          Map.entry("MBQ", new UcumUnit("MBq", "MBq")),
          Map.entry("MCG", new UcumUnit("ug", "ug")),
          Map.entry("MOL", new UcumUnit("uL", "uL")),
          Map.entry("PERC", new UcumUnit("%", "%")),
          Map.entry("MCMOL", new UcumUnit("umol", "umol")),
          Map.entry("MGD", new UcumUnit("mg/d", "mg/d")),
          Map.entry("MG24H", new UcumUnit("mg/(24.h)", "mg/(24.h)")),
          Map.entry("ST", new UcumUnit("{piece}", "1")),
          Map.entry("MIOIE", new UcumUnit("10*6.[IU]", "10*6.[IU]")));

  private IfaUnitMapper() {}

  public static Optional<UcumUnit> lookup(String ifaCode) {
    return Optional.ofNullable(IFA_TO_UCUM.get(ifaCode));
  }
}
