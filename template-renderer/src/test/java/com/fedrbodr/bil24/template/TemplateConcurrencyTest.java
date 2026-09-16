package com.fedrbodr.bil24.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TemplateConcurrencyTest {
  private static final int TASK_COUNT = 8;
  private static final int RENDERS_PER_TASK = 100;
  private static final int TIMEOUT_SECONDS = 30;

  @Test
  void concurrentRendersKeepClientDataIndependent() throws Exception {
    CompiledTemplate template = CompiledTemplate.compile("<user>/<area>/<user>");
    ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Void>> futures = new ArrayList<>(TASK_COUNT);
    try {
      for (int threadIndex = 0; threadIndex < TASK_COUNT; threadIndex++) {
        int taskIndex = threadIndex;
        futures.add(executor.submit(() -> {
          start.await();
          for (int iteration = 0; iteration < RENDERS_PER_TASK; iteration++) {
            String user = "u" + taskIndex + "-" + iteration;
            String area = "a" + taskIndex;
            Map<String, String> replacementMap = Map.of("user", user, "area", area);

            assertEquals(
                user + "/" + area + "/" + user,
                template.render(replacementMap),
                "thread=" + taskIndex + ", iteration=" + iteration);
          }
          return null;
        }));
      }

      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
      start.countDown();
      for (Future<Void> future : futures) {
        long remaining = deadline - System.nanoTime();
        future.get(Math.max(0L, remaining), TimeUnit.NANOSECONDS);
      }

      assertEquals(
          "next/single/next",
          template.render(Map.of("user", "next", "area", "single")));
    } finally {
      start.countDown();
      executor.shutdownNow();
      assertTrue(
          executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS),
          "Template render workers did not terminate");
    }
  }
}
