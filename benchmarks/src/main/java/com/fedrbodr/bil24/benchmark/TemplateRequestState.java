package com.fedrbodr.bil24.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class TemplateRequestState {
  List<Map<String, String>> replacementMapList;
  int cursor;

  @Setup(Level.Trial)
  public void setup(TemplateBenchmarkData data) {
    List<Map<String, String>> replacementMaps = new ArrayList<>(256);
    for (int requestIndex = 0; requestIndex < 256; requestIndex++) {
      Map<String, String> replacementMap = new HashMap<>(data.anchorCount);
      for (int anchorIndex = 0; anchorIndex < data.anchorCount; anchorIndex++) {
        replacementMap.put(
            "anchor_" + anchorIndex,
            "request-" + requestIndex + "-value-" + anchorIndex);
      }
      replacementMaps.add(Map.copyOf(replacementMap));
    }
    replacementMapList = List.copyOf(replacementMaps);
    cursor = 0;
  }

  Map<String, String> nextReplacementMap() {
    return replacementMapList.get(cursor++ & 255);
  }
}
