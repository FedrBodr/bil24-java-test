package com.fedrbodr.bil24.benchmark;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fedrbodr.bil24.template.CompiledTemplate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TemplateBenchmarkContractTest {
  private static final Pattern ANCHOR_PATTERN =
      Pattern.compile("<([A-Za-z_][A-Za-z0-9_]*)>");

  @Test
  void benchmarkDataAndRequestMapsPreserveBothRenderingContracts() {
    for (int anchorCount : new int[] {10, 100}) {
      TemplateBenchmarkData data = createData(anchorCount);
      TemplateRequestState requestState = new TemplateRequestState();
      requestState.setup(data);

      assertEquals(512_000, data.source.getBytes(UTF_8).length);
      assertEquals(anchorCount, countAnchors(data.source));
      assertEquals(256, requestState.replacementMapList.size());

      for (int requestIndex = 0; requestIndex < 256; requestIndex++) {
        Map<String, String> replacementMap = requestState.replacementMapList.get(requestIndex);
        String parsedForRequest = CompiledTemplate.compile(data.source).render(replacementMap);
        String preparedForRequests = data.compiledTemplate.render(replacementMap);

        assertEquals(parsedForRequest, preparedForRequests);
        assertTrue(preparedForRequests.contains("request-" + requestIndex + "-value-0"));
        assertTrue(preparedForRequests.contains(
            "request-" + requestIndex + "-value-" + (anchorCount - 1)));
        for (int anchorIndex = 0; anchorIndex < anchorCount; anchorIndex++) {
          assertFalse(preparedForRequests.contains("<anchor_" + anchorIndex + ">"));
        }
      }
    }
  }

  @Test
  void benchmarkDataRejectsUnsupportedAnchorCounts() {
    TemplateBenchmarkData data = new TemplateBenchmarkData();
    data.anchorCount = 11;

    assertThrows(IllegalArgumentException.class, data::setup);
  }

  private static TemplateBenchmarkData createData(int anchorCount) {
    TemplateBenchmarkData data = new TemplateBenchmarkData();
    data.anchorCount = anchorCount;
    data.setup();
    return data;
  }

  private static int countAnchors(String source) {
    Matcher matcher = ANCHOR_PATTERN.matcher(source);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
}
