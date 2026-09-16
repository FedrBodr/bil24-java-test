package com.fedrbodr.bil24.benchmark;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class ParserBenchmarkState {
  @Param({"latin", "cyrillic", "noSectorName", "invalid", "repeatedMarkers"})
  public String dataset;

  String[] seatNameList;
  int cursor;
  long id;

  @Setup(Level.Trial)
  public void setup() {
    seatNameList = ParserInputCorpus.create(dataset);
    cursor = 0;
    id = 0L;
  }
}
