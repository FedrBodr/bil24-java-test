package com.fedrbodr.bil24.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An immutable template prepared for repeated literal rendering. */
public final class CompiledTemplate {
  private static final Pattern ANCHOR_PATTERN =
      Pattern.compile("<([A-Za-z_][A-Za-z0-9_]*)>");

  private final List<Segment> segmentList;
  private final int sourceLength;

  private CompiledTemplate(List<Segment> segmentList, int sourceLength) {
    this.segmentList = List.copyOf(segmentList);
    this.sourceLength = sourceLength;
  }

  /**
   * Compiles source text whose anchors match {@code <([A-Za-z_][A-Za-z0-9_]*)>}.
   * Text outside matching anchors is preserved literally.
   *
   * @param source source template text
   * @return an immutable compiled template
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public static CompiledTemplate compile(String source) {
    Objects.requireNonNull(source, "source");
    List<Segment> segmentList = new ArrayList<>();
    Matcher matcher = ANCHOR_PATTERN.matcher(source);
    int cursor = 0;
    while (matcher.find()) {
      if (cursor < matcher.start()) {
        segmentList.add(new Segment(source.substring(cursor, matcher.start()), null));
      }
      segmentList.add(new Segment(matcher.group(), matcher.group(1)));
      cursor = matcher.end();
    }
    if (cursor < source.length()) {
      segmentList.add(new Segment(source.substring(cursor), null));
    }
    return new CompiledTemplate(segmentList, source.length());
  }

  /**
   * Renders this template with literal, non-recursive replacements. Missing keys preserve their
   * anchors, while empty values remove them. The caller must not modify {@code replacementMap}
   * during this call; this method neither stores nor modifies the map.
   *
   * @param replacementMap replacements keyed by anchor name without angle brackets
   * @return rendered text
   * @throws NullPointerException if {@code replacementMap} is {@code null}
   * @throws IllegalArgumentException if a used key maps to {@code null}
   */
  public String render(Map<String, String> replacementMap) {
    Objects.requireNonNull(replacementMap, "replacementMap");
    StringBuilder result = new StringBuilder(sourceLength);
    for (Segment segment : segmentList) {
      if (segment.key() == null) {
        result.append(segment.text());
        continue;
      }
      String replacement = replacementMap.get(segment.key());
      if (replacement != null) {
        result.append(replacement);
      } else if (replacementMap.containsKey(segment.key())) {
        throw new IllegalArgumentException("Null replacement for " + segment.key());
      } else {
        result.append(segment.text());
      }
    }
    return result.toString();
  }

  private record Segment(String text, String key) {
  }
}
