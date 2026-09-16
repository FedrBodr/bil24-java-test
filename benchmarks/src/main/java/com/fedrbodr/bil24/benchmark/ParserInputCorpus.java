package com.fedrbodr.bil24.benchmark;

public final class ParserInputCorpus {
  private static final int SIZE = 256;

  private ParserInputCorpus() {
  }

  public static String[] create(String dataset) {
    String[] values = new String[SIZE];
    for (int i = 0; i < values.length; i++) {
      String value = switch (dataset) {
        case "latin" -> "Сектор CETHYOPXABKM" + i + " Ряд 12 Место 34";
        case "cyrillic" -> "Сектор СЕТНУОРХАВКМ" + i + " Ряд 12 Место 34";
        case "noSectorName" -> "Партер Ряд " + i + " Место 34";
        case "invalid" -> switch (i % 4) {
          case 0 -> "неверный-билет-" + i;
          case 1 -> "Сектор A Ряд " + i;
          case 2 -> "Сектор A Место " + i;
          default -> "Сектор A ряд 1 Место " + i;
        };
        case "repeatedMarkers" -> "Сектор A" + i + " Ряд 1 Место 2 Место 3";
        default -> throw new IllegalArgumentException("Unknown dataset: " + dataset);
      };
      values[i] = value;
    }
    return values;
  }
}
