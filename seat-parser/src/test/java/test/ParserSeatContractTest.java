package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ParserSeatContractTest {
  @Test
  void exposesPublicNoArgConstructor() {
    assertNotNull(new ParserSeat());
  }

  @Test
  void parsesEveryObservableField() {
    ResultParser result = ParserSeat.parser("Сектор А Ряд 12 Место 34", -7L);

    assertResult(
        result,
        -7L,
        "Сектор",
        "A",
        "Ряд",
        "12",
        "Место",
        "34",
        "A",
        "Ряд 12",
        "Место 34");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("sectorForms")
  void preservesSectorForms(
      String description,
      String input,
      String expectedSector,
      String expectedSectorName,
      String expectedFullSector) {
    ResultParser result = ParserSeat.parser(input, 17L);

    assertEquals(expectedSector, result.getSector());
    assertEquals(expectedSectorName, result.getSectorName());
    assertEquals(expectedFullSector, result.getFullSector());
  }

  private static Stream<Arguments> sectorForms() {
    return Stream.of(
        Arguments.of("ordinary sector", "Ложа 7 Ряд 1 Место 2", "Ложа", "7", "Ложа 7"),
        Arguments.of("parterre", "Партер Ряд 1 Место 2", "Партер", "", "Партер "),
        Arguments.of("sector without name", "Сектор Ряд 1 Место 2", "Сектор", "", ""));
  }

  @ParameterizedTest
  @CsvSource({
      "С,C", "Е,E", "Т,T", "Н,H", "У,Y", "О,O",
      "Р,P", "Х,X", "А,A", "В,B", "К,K", "М,M"
  })
  void replacesEachSupportedCyrillicLetterOnlyInSectorName(
      String source, String replacement) {
    ResultParser result = ParserSeat.parser(
        "Сектор " + source + " Ряд " + source + " Место " + source, 1L);

    assertEquals(replacement, result.getSectorName());
    assertEquals(source, result.getRowName());
    assertEquals(source, result.getSeatName());
  }

  @Test
  void replacesAllSupportedLettersInOnePass() {
    ResultParser result = ParserSeat.parser(
        "Сектор СЕТНУОРХАВКМ Ряд 1 Место 2", 1L);

    assertEquals("CETHYOPXABKM", result.getSectorName());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unchangedSectorNames")
  void preservesUnsupportedSectorNameCharacters(
      String description, String sectorName, String expected) {
    ResultParser result = ParserSeat.parser(
        "Сектор " + sectorName + " Ряд 1 Место 2", 1L);

    assertEquals(expected, result.getSectorName());
  }

  private static Stream<Arguments> unchangedSectorNames() {
    return Stream.of(
        Arguments.of("Latin letters", "CETHYOPXABKM", "CETHYOPXABKM"),
        Arguments.of("lowercase Cyrillic", "Асетн", "Aсетн"),
        Arguments.of("mixed alphabets", "AАBВC", "AABBC"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("spacingAndMarkerCases")
  void preservesSpacingAndMarkerSemantics(
      String description,
      String input,
      String expectedSector,
      String expectedSectorName,
      String expectedRowName,
      String expectedSeatName) {
    ResultParser result = ParserSeat.parser(input, 2L);

    assertEquals(expectedSector, result.getSector());
    assertEquals(expectedSectorName, result.getSectorName());
    assertEquals(expectedRowName, result.getRowName());
    assertEquals(expectedSeatName, result.getSeatName());
  }

  private static Stream<Arguments> spacingAndMarkerCases() {
    return Stream.of(
        Arguments.of(
            "two spaces", "Сектор  А Ряд 1 Место 2", "Сектор ", "A", "1", "2"),
        Arguments.of(
            "empty row", "Сектор A Ряд  Место 2", "Сектор", "A", "", "2"),
        Arguments.of(
            "empty seat", "Сектор A Ряд 1 Место ", "Сектор", "A", "1", ""),
        Arguments.of(
            "repeated row", "Сектор A Ряд 1 Ряд 2 Место 3", "Сектор", "A", "1 Ряд 2", "3"),
        Arguments.of(
            "repeated seat", "Сектор A Ряд 1 Место 2 Место 3", "Сектор", "A", "1 Место 2", "3"),
        Arguments.of(
            "tabs in values", "Сектор A\tB Ряд 1\t2 Место 3\t4", "Сектор", "A\tB", "1\t2", "3\t4"));
  }

  @Test
  void findStartsAfterANewlineBeforeDescription() {
    ResultParser result = ParserSeat.parser("ignored\nСектор A Ряд 1 Место 2", 3L);

    assertEquals("Сектор", result.getSector());
    assertEquals("A", result.getSectorName());
  }

  @Test
  void newlineInsideDescriptionIsNotMatchedByDot() {
    assertNull(ParserSeat.parser("Сектор A Ряд 1\nМесто 2", 3L));
  }

  @Test
  void findStartsAfterCrLfBeforeDescription() {
    ResultParser result = ParserSeat.parser("ignored\r\nПартер Ряд 1 Место 2", 3L);

    assertEquals("Партер", result.getSector());
    assertEquals("", result.getSectorName());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unicodeSectorNames")
  void preservesUnicodeSectorNames(String description, String sectorName) {
    ResultParser result = ParserSeat.parser(
        "Сектор " + sectorName + " Ряд 1 Место 2", 4L);

    assertEquals(sectorName, result.getSectorName());
  }

  private static Stream<Arguments> unicodeSectorNames() {
    return Stream.of(
        Arguments.of("non-ASCII uppercase", "Ω"),
        Arguments.of("supplementary uppercase code point", "\uD801\uDC00"),
        Arguments.of("combining mark", "A\u0301"),
        Arguments.of("non-breaking space", "A\u00A0B"));
  }

  @ParameterizedTest
  @MethodSource("invalidInputs")
  void returnsNullForInvalidInput(String input) {
    assertNull(ParserSeat.parser(input, 5L));
  }

  private static Stream<String> invalidInputs() {
    return Stream.of(
        "",
        "Сектор A Ряд 1",
        "Сектор A Ряд 1Место 2");
  }

  @Test
  void keepsNullInputFailure() {
    assertThrows(NullPointerException.class, () -> ParserSeat.parser(null, 0L));
  }

  @Test
  void preservesLongIdBoundaries() {
    assertEquals(
        Long.MIN_VALUE,
        ParserSeat.parser("Сектор A Ряд 1 Место 2", Long.MIN_VALUE).getId());
    assertEquals(
        Long.MAX_VALUE,
        ParserSeat.parser("Сектор A Ряд 1 Место 2", Long.MAX_VALUE).getId());
  }

  @Test
  void returnsIndependentMutableResults() {
    ResultParser first = ParserSeat.parser("Сектор А Ряд 1 Место 2", 1L);
    ResultParser second = ParserSeat.parser("Сектор А Ряд 1 Место 2", 2L);

    assertNotSame(first, second);
    first.setSectorName("changed");
    assertEquals("A", second.getSectorName());
    assertEquals("A", ParserSeat.parser("Сектор А Ряд 1 Место 2", 3L).getSectorName());
  }

  private static void assertResult(
      ResultParser result,
      long id,
      String sector,
      String sectorName,
      String row,
      String rowName,
      String seat,
      String seatName,
      String fullSector,
      String fullRow,
      String fullSeat) {
    assertEquals(id, result.getId());
    assertEquals(sector, result.getSector());
    assertEquals(sectorName, result.getSectorName());
    assertEquals(row, result.getRow());
    assertEquals(rowName, result.getRowName());
    assertEquals(seat, result.getSeat());
    assertEquals(seatName, result.getSeatName());
    assertEquals(fullSector, result.getFullSector());
    assertEquals(fullRow, result.getFullRow());
    assertEquals(fullSeat, result.getFullSeat());
  }
}
