package com.fedrbodr.bil24.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CompiledTemplateTest {
  @ParameterizedTest(name = "{0}")
  @MethodSource("renderCases")
  void rendersAccordingToAnchorGrammar(
      String description,
      String source,
      Map<String, String> replacementMap,
      String expected) {
    assertEquals(expected, CompiledTemplate.compile(source).render(replacementMap));
  }

  private static Stream<Arguments> renderCases() {
    return Stream.of(
        Arguments.of("empty template", "", Map.of(), ""),
        Arguments.of("plain text", "plain", Map.of("unused", "ignored"), "plain"),
        Arguments.of("adjacent repeated anchors", "<a><a>", Map.of("a", "x"), "xx"),
        Arguments.of(
            "case-sensitive keys", "<Name>/<name>", Map.of("Name", "Ira", "name", "ira"),
            "Ira/ira"),
        Arguments.of("leading digit is invalid", "<1bad>", Map.of("1bad", "x"), "<1bad>"),
        Arguments.of("hyphen is invalid", "<bad-name>", Map.of("bad-name", "x"), "<bad-name>"),
        Arguments.of("unfinished anchor", "<unfinished", Map.of("unfinished", "x"), "<unfinished"),
        Arguments.of("empty anchor", "<>", Map.of("", "x"), "<>"),
        Arguments.of(
            "letters digits and underscore after valid start",
            "Привет, <_user2>!",
            Map.of("_user2", "Ира"),
            "Привет, Ира!"),
        Arguments.of("Unicode text", "你好, <name> — café", Map.of("name", "Ира"),
            "你好, Ира — café"));
  }

  @Test
  void replacementsAreLiteralAndNotRecursive() {
    CompiledTemplate template = CompiledTemplate.compile("<user>/<area>/<user>");
    String result = template.render(Map.of("user", "<area>$\\", "area", "Зал"));

    assertEquals("<area>$\\/Зал/<area>$\\", result);
  }

  @Test
  void replacementLineBreaksAreLiteral() {
    CompiledTemplate template = CompiledTemplate.compile("before:<value>:after");

    assertEquals(
        "before:first\nsecond\r\nthird:after",
        template.render(Map.of("value", "first\nsecond\r\nthird")));
  }

  @Test
  void missingValueRemainsAndEmptyValueRemovesAnchor() {
    CompiledTemplate template = CompiledTemplate.compile("<known>-<missing>-<empty>");

    assertEquals("Ира-<missing>-", template.render(Map.of("known", "Ира", "empty", "")));
  }

  @Test
  void validInnerAnchorStillMatches() {
    assertEquals("<Ира>", CompiledTemplate.compile("<<name>>").render(Map.of("name", "Ира")));
  }

  @Test
  void rendersRemainIndependentAcrossRequests() {
    CompiledTemplate template = CompiledTemplate.compile("Hello, <name>");

    assertEquals("Hello, Ира", template.render(Map.of("name", "Ира")));
    assertEquals("Hello, Олег", template.render(Map.of("name", "Олег")));
    assertEquals("Hello, <name>", template.render(Map.of()));
  }

  @Test
  void usedNullReplacementIsRejectedWithKeyInMessage() {
    Map<String, String> replacementMap = new HashMap<>();
    replacementMap.put("name", null);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> CompiledTemplate.compile("Hello, <name>").render(replacementMap));

    assertTrue(exception.getMessage().contains("name"));
  }

  @Test
  void unusedNullReplacementIsIgnored() {
    Map<String, String> replacementMap = new HashMap<>();
    replacementMap.put("unused", null);

    assertEquals("Hello", CompiledTemplate.compile("Hello").render(replacementMap));
  }

  @Test
  void rejectsNullSourceAndReplacementMap() {
    assertThrows(NullPointerException.class, () -> CompiledTemplate.compile(null));
    CompiledTemplate template = CompiledTemplate.compile("Hello");
    assertThrows(NullPointerException.class, () -> template.render(null));
  }

  @Test
  void doesNotModifyReplacementMap() {
    Map<String, String> mutableMap = new HashMap<>();
    mutableMap.put("name", "Ира");
    mutableMap.put("unused", "value");
    Map<String, String> original = Map.copyOf(mutableMap);

    assertEquals("Hello, Ира", CompiledTemplate.compile("Hello, <name>").render(mutableMap));
    assertEquals(original, mutableMap);
    assertEquals(
        "Hello, Ира",
        CompiledTemplate.compile("Hello, <name>")
            .render(Collections.unmodifiableMap(mutableMap)));
  }
}
