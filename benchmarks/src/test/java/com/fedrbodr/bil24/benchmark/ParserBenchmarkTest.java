package com.fedrbodr.bil24.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParserBenchmarkTest {
  @Test
  void setupCreatesCorpusAndResetsCounters() {
    ParserBenchmarkState state = new ParserBenchmarkState();
    state.dataset = "latin";
    state.cursor = 19;
    state.id = 23L;

    state.setup();

    assertEquals(256, state.seatNameList.length);
    assertEquals(0, state.cursor);
    assertEquals(0L, state.id);
  }

  @Test
  void baselineCyclesThroughTheOriginalParser() {
    ParserBenchmarkState state = new ParserBenchmarkState();
    state.dataset = "latin";
    state.setup();
    ParserBenchmark benchmark = new ParserBenchmark();

    reference.ResultParser first = benchmark.baseline(state);
    reference.ResultParser second = benchmark.baseline(state);

    assertEquals("CETHYOPXABKM0", first.getSectorName());
    assertEquals(1L, first.getId());
    assertEquals("CETHYOPXABKM1", second.getSectorName());
    assertEquals(2L, second.getId());
    assertEquals(2, state.cursor);
  }

  @Test
  void optimizedCyclesThroughTheNewParser() {
    ParserBenchmarkState state = new ParserBenchmarkState();
    state.dataset = "cyrillic";
    state.setup();
    ParserBenchmark benchmark = new ParserBenchmark();

    test.ResultParser first = benchmark.optimized(state);
    test.ResultParser second = benchmark.optimized(state);

    assertEquals("CETHYOPXABKM0", first.getSectorName());
    assertEquals(1L, first.getId());
    assertEquals("CETHYOPXABKM1", second.getSectorName());
    assertEquals(2L, second.getId());
    assertEquals(2, state.cursor);
  }
}
