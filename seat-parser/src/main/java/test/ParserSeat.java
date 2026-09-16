package test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserSeat {
  private static final Pattern SEAT_PATTERN = Pattern.compile(
      "(?:(?<sector>.*?) (?<sectorName>(?:\\p{Lu}|\\d).*?)|(?<sectorWithoutName>.*)) (?<row>Ряд) (?<rowName>.*) (?<seat>Место) (?<seatName>.*)");

  public ParserSeat() {
  }

  public static ResultParser parser(String seatName, long id) {
    Matcher matcher = SEAT_PATTERN.matcher(seatName);
    if (!matcher.find()) {
      return null;
    }

    ResultParser result = new ResultParser();
    result.setId(id);
    String sectorWithoutName = matcher.group("sectorWithoutName");
    if (sectorWithoutName != null) {
      result.setSector(sectorWithoutName);
      result.setSectorName("");
    } else {
      result.setSector(matcher.group("sector"));
      result.setSectorName(normalizeSectorName(matcher.group("sectorName")));
    }
    result.setRow(matcher.group("row"));
    result.setRowName(matcher.group("rowName"));
    result.setSeat(matcher.group("seat"));
    result.setSeatName(matcher.group("seatName"));
    return result;
  }

  private static String normalizeSectorName(String value) {
    StringBuilder builder = null;
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      char replacement = switch (current) {
        case 'С' -> 'C';
        case 'Е' -> 'E';
        case 'Т' -> 'T';
        case 'Н' -> 'H';
        case 'У' -> 'Y';
        case 'О' -> 'O';
        case 'Р' -> 'P';
        case 'Х' -> 'X';
        case 'А' -> 'A';
        case 'В' -> 'B';
        case 'К' -> 'K';
        case 'М' -> 'M';
        default -> current;
      };
      if (builder == null && current != replacement) {
        builder = new StringBuilder(value.length());
        builder.append(value, 0, i);
      }
      if (builder != null) {
        builder.append(replacement);
      }
    }
    return builder == null ? value : builder.toString();
  }
}
