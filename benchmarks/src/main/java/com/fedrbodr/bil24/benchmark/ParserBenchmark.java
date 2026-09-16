package com.fedrbodr.bil24.benchmark;

import org.openjdk.jmh.annotations.Benchmark;

public class ParserBenchmark {
  @Benchmark
  public reference.ResultParser baseline(ParserBenchmarkState state) {
    return reference.ParserSeat.parser(
        state.seatNameList[state.cursor++ & 255], ++state.id);
  }
}
