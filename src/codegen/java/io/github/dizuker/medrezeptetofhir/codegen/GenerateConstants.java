package io.github.dizuker.medrezeptetofhir.codegen;

import io.github.dizuker.igcodegen.IgPackageModel;
import io.github.dizuker.igcodegen.IgPackageScanner;
import io.github.dizuker.igcodegen.JavaConstantsGenerator;
import java.io.IOException;
import java.nio.file.Path;
import tools.jackson.databind.ObjectMapper;

/**
 * Regenerates Java constant classes from local FHIR CodeSystem JSON files in {@code
 * src/main/resources}. These are one-off local files, not restorable FHIR IG packages, so they are
 * scanned directly with {@link IgPackageScanner} instead of going through ig-codegen's
 * package.json-driven {@code IgCodegen} entry point. Run via the {@code generateConstants} Gradle
 * task; review the diff and commit.
 *
 * @param args {@code args[0]} = the resources directory to scan, {@code args[1]} = the Java source
 *     root to write the generated classes into
 */
public final class GenerateConstants {
  private GenerateConstants() {}

  public static void main(String[] args) throws IOException {
    Path resourcesDir = Path.of(args[0]);
    Path outputDir = Path.of(args[1]);

    IgPackageScanner scanner = new IgPackageScanner(new ObjectMapper());

    IgPackageModel edqmModel =
        scanner.scan(resourcesDir.resolve("edqm"), "edqm-standardterms", "1.0.1");
    Path edqmGenerated =
        JavaConstantsGenerator.writeTo(
            edqmModel, "io.github.dizuker.medrezeptetofhir.edqm", "EdqmStandardTerms", outputDir);
    System.out.println("Generated " + edqmGenerated);

    IgPackageModel kbvModel =
        scanner.scan(resourcesDir.resolve("kbv"), "KBV-CS-SFHIR-BMP-DARREICHUNGSFORM", "1.03");
    Path kbvGenerated =
        JavaConstantsGenerator.writeTo(
            kbvModel, "io.github.dizuker.medrezeptetofhir.kbv", "KbvDarreichungsform", outputDir);
    System.out.println("Generated " + kbvGenerated);
  }
}
