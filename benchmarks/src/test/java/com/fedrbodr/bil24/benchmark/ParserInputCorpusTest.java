package com.fedrbodr.bil24.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ParserInputCorpusTest {
  @Test
  void createsEveryDatasetWithExactly256Entries() {
    for (String dataset : new String[] {
        "latin", "cyrillic", "noSectorName", "invalid", "repeatedMarkers"
    }) {
      assertEquals(256, ParserInputCorpus.create(dataset).length, dataset);
    }
  }

  @Test
  void followsThePinnedGenerationRules() {
    assertEquals(
        "Сектор CETHYOPXABKM0 Ряд 12 Место 34",
        ParserInputCorpus.create("latin")[0]);
    assertEquals(
        "Сектор СЕТНУОРХАВКМ255 Ряд 12 Место 34",
        ParserInputCorpus.create("cyrillic")[255]);
    assertEquals(
        "Партер Ряд 17 Место 34",
        ParserInputCorpus.create("noSectorName")[17]);
    assertEquals(
        "Сектор A ряд 1 Место 3",
        ParserInputCorpus.create("invalid")[3]);
    assertEquals(
        "Сектор A255 Ряд 1 Место 2 Место 3",
        ParserInputCorpus.create("repeatedMarkers")[255]);
    assertThrows(
        IllegalArgumentException.class,
        () -> ParserInputCorpus.create("unknown"));
  }

  @Test
  void originalParserAcceptsValidDatasetsAndRejectsInvalidDataset() {
    for (String dataset : new String[] {
        "latin", "cyrillic", "noSectorName", "repeatedMarkers"
    }) {
      for (String input : ParserInputCorpus.create(dataset)) {
        assertNotNull(reference.ParserSeat.parser(input, 1L), input);
      }
    }
    for (String input : ParserInputCorpus.create("invalid")) {
      assertNull(reference.ParserSeat.parser(input, 1L), input);
    }
  }

  @Test
  void returnsAnIndependentArrayForEveryCall() {
    String[] first = ParserInputCorpus.create("latin");
    String[] second = ParserInputCorpus.create("latin");

    assertNotSame(first, second);
    first[0] = "changed";
    assertEquals(
        "Сектор CETHYOPXABKM0 Ряд 12 Место 34",
        second[0]);
  }
}
