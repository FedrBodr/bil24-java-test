package com.fedrbodr.bil24.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReferenceContractTest {
  @Test
  void preservesRepeatedSeatMarkerBehavior() {
    reference.ResultParser result = reference.ParserSeat.parser(
        "Сектор А Ряд 1 Место 2 Место 3", 42L);

    assertEquals("Сектор", result.getSector());
    assertEquals("A", result.getSectorName());
    assertEquals("Ряд", result.getRow());
    assertEquals("1 Место 2", result.getRowName());
    assertEquals("Место", result.getSeat());
    assertEquals("3", result.getSeatName());
    assertEquals(42L, result.getId());
    assertEquals("A", result.getFullSector());
    assertEquals("Ряд 1 Место 2", result.getFullRow());
    assertEquals("Место 3", result.getFullSeat());
  }

  @Test
  void returnsNullWhenInputDoesNotMatch() {
    assertNull(reference.ParserSeat.parser("неверный билет", 42L));
  }

  @Test
  void rejectsNullInput() {
    assertThrows(
        NullPointerException.class,
        () -> reference.ParserSeat.parser(null, 42L));
  }

  @Test
  void preservesEmptyRowAndSeatNames() {
    reference.ResultParser emptyRow = reference.ParserSeat.parser(
        "Сектор A Ряд  Место 3", Long.MIN_VALUE);
    reference.ResultParser emptySeat = reference.ParserSeat.parser(
        "Сектор A Ряд 1 Место ", Long.MAX_VALUE);

    assertEquals("", emptyRow.getRowName());
    assertEquals("3", emptyRow.getSeatName());
    assertEquals(Long.MIN_VALUE, emptyRow.getId());
    assertEquals("1", emptySeat.getRowName());
    assertEquals("", emptySeat.getSeatName());
    assertEquals(Long.MAX_VALUE, emptySeat.getId());
  }
}
