package com.fedrbodr.bil24.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ParserConcurrencyTest {
  private static final int THREAD_COUNT = 8;
  private static final int INPUTS_PER_THREAD = 1_000;
  private static final int TIMEOUT_SECONDS = 30;
  private static final String[] DATASETS = {
      "latin", "cyrillic", "noSectorName", "invalid", "repeatedMarkers"
  };

  @Test
  void concurrentCallsMatchReference() throws Exception {
    String[][] corpora = new String[DATASETS.length][];
    for (int datasetIndex = 0; datasetIndex < DATASETS.length; datasetIndex++) {
      corpora[datasetIndex] = ParserInputCorpus.create(DATASETS[datasetIndex]);
    }

    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>(THREAD_COUNT);
    try {
      for (int threadNumber = 0; threadNumber < THREAD_COUNT; threadNumber++) {
        int taskNumber = threadNumber;
        futures.add(executor.submit(() -> {
          start.await();
          for (int index = 0; index < INPUTS_PER_THREAD; index++) {
            String[] corpus = corpora[index % corpora.length];
            String input = corpus[index & 255];
            long id = taskNumber * INPUTS_PER_THREAD + index;
            assertEquals(
                ParserSnapshot.of(reference.ParserSeat.parser(input, id)),
                ParserSnapshot.of(test.ParserSeat.parser(input, id)),
                "thread=" + taskNumber + ", index=" + index + ", id=" + id);
          }
          return null;
        }));
      }

      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
      start.countDown();
      for (Future<?> future : futures) {
        long remaining = deadline - System.nanoTime();
        future.get(Math.max(0L, remaining), TimeUnit.NANOSECONDS);
      }
      System.out.println(
          "Parser concurrent comparisons: " + THREAD_COUNT * INPUTS_PER_THREAD + " cases");
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertTrue(
          executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "Parser comparison workers did not terminate");
    }
  }

  @Test
  void resultsDoNotShareMutableState() {
    test.ResultParser first = test.ParserSeat.parser("Сектор А Ряд 1 Место 2", 1L);
    test.ResultParser second = test.ParserSeat.parser("Сектор А Ряд 1 Место 2", 2L);

    assertNotSame(first, second);
    first.setSectorName("changed");

    assertEquals("A", second.getSectorName());
    assertEquals(
        "A", test.ParserSeat.parser("Сектор А Ряд 1 Место 2", 3L).getSectorName());
  }
}
