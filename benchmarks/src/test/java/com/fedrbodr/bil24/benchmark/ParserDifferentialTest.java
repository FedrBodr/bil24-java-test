package com.fedrbodr.bil24.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParserDifferentialTest {
  private static final long SEED = 240915L;
  private static final int STRUCTURED_CASES = 3_000;
  private static final int MUTATED_CASES = 3_000;
  private static final int ARBITRARY_CASES = 4_000;
  private static final int TOTAL_CASES =
      STRUCTURED_CASES + MUTATED_CASES + ARBITRARY_CASES;
  private static final int MAX_ARBITRARY_LENGTH = 96;

  // Exact arbitrary-input alphabet: Latin and Cyrillic letters, digits, plain space,
  // tab, CR, LF, NBSP, and isolated high/low UTF-16 surrogate units.
  private static final char[] ARBITRARY_ALPHABET = (
      "ABCXYZabcxyzАВЕКМНОРСТХавекмнорстх012789 \t\r\n\u00A0\uD800\uDC00")
      .toCharArray();

  private static final String[] SECTORS = {
      "Сектор", "Ложа", "Партер", "Sector", "БАЛКОН"
  };
  private static final String[] SECTOR_NAMES = {
      "А", "A", "СЕТНУОРХАВКМ", "MixАВ", "Ω", "7", "A\u0301", "\uD801\uDC00", "а", ""
  };
  private static final String[] ROW_NAMES = {
      "1", "", "А", "1 Ряд 2", "1\t2", "A\u00A0B"
  };
  private static final String[] SEAT_NAMES = {
      "2", "", "А", "2 Место 3", "3\t4", "\uD801\uDC00"
  };

  @ParameterizedTest(name = "fixed case {index}: {0}")
  @MethodSource("fixedCases")
  void fixedCasesMatchReference(String input, long id) {
    assertEquivalent(input, id, -1);
  }

  private static Stream<Arguments> fixedCases() {
    return Stream.of(
        Arguments.of("Сектор А Ряд 12 Место 34", 1L),
        Arguments.of("Партер Ряд 1 Место 2", 2L),
        Arguments.of("Сектор Ряд 1 Место 2", 3L),
        Arguments.of("Сектор СЕТНУОРХАВКМ Ряд 1 Место 2", 4L),
        Arguments.of("Сектор CETHYOPXABKM Ряд 1 Место 2", 5L),
        Arguments.of("Сектор Асетн Ряд 1 Место 2", 6L),
        Arguments.of("Сектор AАBВC Ряд 1 Место 2", 7L),
        Arguments.of("Сектор  А Ряд 1 Место 2", 8L),
        Arguments.of("Сектор A Ряд  Место 2", 9L),
        Arguments.of("Сектор A Ряд 1 Место ", 10L),
        Arguments.of("Сектор A Ряд 1 Ряд 2 Место 3", 11L),
        Arguments.of("Сектор A Ряд 1 Место 2 Место 3", 12L),
        Arguments.of("Сектор A\tB Ряд 1\t2 Место 3\t4", 13L),
        Arguments.of("ignored\nСектор A Ряд 1 Место 2", 14L),
        Arguments.of("Сектор A Ряд 1\nМесто 2", 15L),
        Arguments.of("ignored\r\nПартер Ряд 1 Место 2", 16L),
        Arguments.of("Сектор Ω Ряд 1 Место 2", 17L),
        Arguments.of("Сектор \uD801\uDC00 Ряд 1 Место 2", 18L),
        Arguments.of("Сектор A\u0301 Ряд 1 Место 2", 19L),
        Arguments.of("Сектор A\u00A0B Ряд 1 Место 2", 20L),
        Arguments.of("", 21L),
        Arguments.of("Сектор A Ряд 1", 22L),
        Arguments.of("Сектор A Ряд 1Место 2", 23L),
        Arguments.of(null, 24L),
        Arguments.of("Сектор A Ряд 1 Место 2", Long.MIN_VALUE),
        Arguments.of("Сектор A Ряд 1 Место 2", Long.MAX_VALUE));
  }

  @Test
  void validInputWithExactly4096Utf16UnitsMatchesReference() {
    String prefix = "Сектор ";
    String suffix = " Ряд 1 Место 2";
    String input = prefix + "A".repeat(4096 - prefix.length() - suffix.length()) + suffix;
    assertEquals(4096, input.length());

    assertEquivalent(input, 4096L, -2);
  }

  @Test
  void invalidInputWithExactly128Utf16UnitsMatchesReference() {
    String input = "x".repeat(128);
    assertEquals(128, input.length());

    assertEquivalent(input, 128L, -3);
  }

  @Test
  void tenThousandDeterministicCasesMatchReference() {
    Random random = new Random(SEED);

    // Index ranges are 0..2999 structured, 3000..5999 one-unit mutations,
    // and 6000..9999 arbitrary strings with lengths in 0..96 inclusive.
    for (int index = 0; index < TOTAL_CASES; index++) {
      String input;
      if (index < STRUCTURED_CASES) {
        input = structuredInput(random);
      } else if (index < STRUCTURED_CASES + MUTATED_CASES) {
        input = mutateOneUtf16Unit(structuredInput(random), random);
      } else {
        input = arbitraryInput(random);
      }
      assertEquivalent(input, random.nextLong(), index);
    }

    System.out.println(
        "Parser differential comparisons: " + TOTAL_CASES + " cases (seed=" + SEED + ")");
  }

  private static String structuredInput(Random random) {
    String sector = choose(SECTORS, random);
    String rowName = choose(ROW_NAMES, random);
    String seatName = choose(SEAT_NAMES, random);
    if (random.nextBoolean()) {
      return sector + " Ряд " + rowName + " Место " + seatName;
    }
    return sector + " " + choose(SECTOR_NAMES, random)
        + " Ряд " + rowName + " Место " + seatName;
  }

  private static String mutateOneUtf16Unit(String input, Random random) {
    return switch (random.nextInt(3)) {
      case 0 -> {
        int position = random.nextInt(input.length());
        yield input.substring(0, position) + input.substring(position + 1);
      }
      case 1 -> {
        int position = random.nextInt(input.length() + 1);
        yield input.substring(0, position) + randomUnit(random) + input.substring(position);
      }
      default -> {
        int position = random.nextInt(input.length());
        char original = input.charAt(position);
        char replacement;
        do {
          replacement = randomUnit(random);
        } while (replacement == original);
        yield input.substring(0, position) + replacement + input.substring(position + 1);
      }
    };
  }

  private static String arbitraryInput(Random random) {
    int length = random.nextInt(MAX_ARBITRARY_LENGTH + 1);
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(randomUnit(random));
    }
    return builder.toString();
  }

  private static char randomUnit(Random random) {
    return ARBITRARY_ALPHABET[random.nextInt(ARBITRARY_ALPHABET.length)];
  }

  private static String choose(String[] values, Random random) {
    return values[random.nextInt(values.length)];
  }

  private static void assertEquivalent(String input, long id, int index) {
    Outcome expected = captureReference(input, id);
    Outcome actual = captureOptimized(input, id);

    assertEquals(
        expected,
        actual,
        () -> "seed=" + SEED + ", index=" + index + ", id=" + id
            + ", input=" + escape(input));
  }

  private static Outcome captureReference(String input, long id) {
    try {
      return new Outcome(ParserSnapshot.of(reference.ParserSeat.parser(input, id)), null);
    } catch (Throwable failure) {
      return new Outcome(null, failure.getClass());
    }
  }

  private static Outcome captureOptimized(String input, long id) {
    try {
      return new Outcome(ParserSnapshot.of(test.ParserSeat.parser(input, id)), null);
    } catch (Throwable failure) {
      return new Outcome(null, failure.getClass());
    }
  }

  private static String escape(String input) {
    if (input == null) {
      return "null";
    }
    StringBuilder escaped = new StringBuilder(input.length() + 2).append('"');
    for (int i = 0; i < input.length(); i++) {
      char current = input.charAt(i);
      switch (current) {
        case '\\' -> escaped.append("\\\\");
        case '"' -> escaped.append("\\\"");
        case '\b' -> escaped.append("\\b");
        case '\f' -> escaped.append("\\f");
        case '\n' -> escaped.append("\\n");
        case '\r' -> escaped.append("\\r");
        case '\t' -> escaped.append("\\t");
        default -> {
          if (current < 0x20 || current > 0x7e) {
            escaped.append(String.format("\\u%04X", (int) current));
          } else {
            escaped.append(current);
          }
        }
      }
    }
    return escaped.append('"').toString();
  }

  private record Outcome(ParserSnapshot snapshot, Class<? extends Throwable> failureType) {
  }
}
