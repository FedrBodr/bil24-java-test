package com.fedrbodr.bil24.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemplateFilesTest {
  private static final int TEMPLATE_SIZE_BYTES = 512_000;

  @Test
  void compiledTemplatesRemainUsableAfterSourceFilesAreDeleted(@TempDir Path tempDirectory)
      throws Exception {
    for (int i = 1; i <= 3; i++) {
      Path path = tempDirectory.resolve("template-" + i + ".txt");
      String prefix = "Шаблон " + i + ": <user_name> / <area_name>\n";
      String padding = "x".repeat(
          TEMPLATE_SIZE_BYTES - prefix.getBytes(StandardCharsets.UTF_8).length);
      String source = prefix + padding;
      Files.writeString(path, source, StandardCharsets.UTF_8);

      assertEquals(TEMPLATE_SIZE_BYTES, Files.size(path));
      CompiledTemplate template = CompiledTemplate.compile(
          Files.readString(path, StandardCharsets.UTF_8));
      String expected = "Шаблон " + i + ": Ира / Зал\n" + padding;
      Map<String, String> replacementMap = Map.of(
          "user_name", "Ира",
          "area_name", "Зал");

      assertEquals(expected, template.render(replacementMap));
      Files.delete(path);
      assertEquals(expected, template.render(replacementMap));
    }
  }
}
