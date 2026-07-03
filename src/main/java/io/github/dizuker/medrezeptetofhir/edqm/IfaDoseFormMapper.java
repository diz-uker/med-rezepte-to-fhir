package io.github.dizuker.medrezeptetofhir.edqm;

import io.github.dizuker.medrezeptetofhir.edqm.EdqmStandardTerms.CodeSystems.EdqmStandardterms;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;

public final class IfaDoseFormMapper {
  // Maps 3-character IFA pharmaceutical form codes to EDQM Standard Terms codes.
  // Source: DIMDI/BfArM IFA code list, mapped to EDQM Standard Terms 1.0.1.
  private static final Map<String, String> IFA_TO_EDQM =
      Map.ofEntries(
          Map.entry("AMP", "30001000"),
          Map.entry("ASO", "50019000"),
          Map.entry("ATO", "50018000"),
          Map.entry("ATR", "10604000"),
          Map.entry("AUB", "10610000"),
          Map.entry("AUC", "10601000"),
          Map.entry("AUG", "10602000"),
          Map.entry("AUS", "10603000"),
          Map.entry("BAD", "10501000"),
          Map.entry("BEU", "30004000"),
          Map.entry("BTA", "10222000"),
          Map.entry("CRE", "10502000"),
          Map.entry("DFL", "30069000"),
          Map.entry("DIS", "11208500"),
          Map.entry("EDP", "30046000"),
          Map.entry("FER", "30051000"),
          Map.entry("FLA", "30008000"),
          Map.entry("FLE", "10104000"),
          Map.entry("FTA", "10221000"),
          Map.entry("GEL", "10503000"),
          Map.entry("GLI", "50078000"),
          Map.entry("GLO", "10231000"),
          Map.entry("GMR", "10206000"),
          Map.entry("GRA", "10204000"),
          Map.entry("GSE", "10113000"),
          Map.entry("GUL", "10301000"),
          Map.entry("HKM", "10212000"),
          Map.entry("HVW", "10217000"),
          Map.entry("IFK", "11213000"),
          Map.entry("IHP", "11109000"),
          Map.entry("ILO", "11201000"),
          Map.entry("IMP", "11301000"),
          Map.entry("INF", "11210000"),
          Map.entry("INL", "50081000"),
          Map.entry("INS", "10202000"),
          Map.entry("IUP", "11901000"),
          Map.entry("KAP", "15012000"),
          Map.entry("KGU", "10229000"),
          Map.entry("KLI", "11005000"),
          Map.entry("KMR", "10212000"),
          Map.entry("KTA", "10228000"),
          Map.entry("LSE", "10105000"),
          Map.entry("LUP", "10230000"),
          Map.entry("LUT", "10321000"),
          Map.entry("NAG", "10802000"),
          Map.entry("NAS", "10808000"),
          Map.entry("NAW", "10521000"),
          Map.entry("NDS", "10808000"),
          Map.entry("NSA", "10803000"),
          Map.entry("OHT", "10704000"),
          Map.entry("PAS", "10323000"),
          Map.entry("PEL", "10236000"),
          Map.entry("PFL", "15042000"),
          Map.entry("PFT", "10519000"),
          Map.entry("PII", "50053500"),
          Map.entry("PIJ", "11205000"),
          Map.entry("REK", "10215000"),
          Map.entry("RET", "10226000"),
          Map.entry("RGR", "10207000"),
          Map.entry("RKA", "11014000"),
          Map.entry("RSC", "11004000"),
          Map.entry("SAF", "10104000"),
          Map.entry("SAL", "10504000"),
          Map.entry("SAM", "10314005"),
          Map.entry("SCH", "10507000"),
          Map.entry("SHA", "10508000"),
          Map.entry("SIR", "10117000"),
          Map.entry("SMF", "10236100"),
          Map.entry("SMT", "10223000"),
          Map.entry("SPL", "12113000"),
          Map.entry("SUE", "10106000"),
          Map.entry("SUP", "11013000"),
          Map.entry("SUT", "10318000"),
          Map.entry("SUV", "11102000"),
          Map.entry("TAB", "10219000"),
          Map.entry("TEE", "10122000"),
          Map.entry("TMR", "10225000"),
          Map.entry("TPN", "15056000"),
          Map.entry("TRO", "15022000"),
          Map.entry("TRT", "10120000"),
          Map.entry("TSD", "10121500"),
          Map.entry("UTA", "10220000"),
          Map.entry("VAL", "10905000"),
          Map.entry("VCR", "10901000"),
          Map.entry("VER", "15021000"),
          Map.entry("VGE", "10902000"),
          Map.entry("VKA", "10910000"),
          Map.entry("VSU", "10909000"),
          Map.entry("VTA", "10912000"),
          Map.entry("WKA", "10211000"),
          Map.entry("WKM", "10213000"));

  private IfaDoseFormMapper() {}

  public static Optional<Coding> lookup(String ifaCode) {
    return Optional.ofNullable(IFA_TO_EDQM.get(ifaCode))
        .flatMap(EdqmStandardterms::fromValue)
        .map(EdqmStandardterms::coding);
  }
}
