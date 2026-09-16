package com.fedrbodr.bil24.benchmark;

import com.fedrbodr.bil24.template.CompiledTemplate;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class TemplateBenchmarkData {
  @Param({"10", "100"})
  public int anchorCount;

  String source;
  CompiledTemplate compiledTemplate;

  @Setup(Level.Trial)
  public void setup() {
    if (anchorCount != 10 && anchorCount != 100) {
      throw new IllegalArgumentException("Unsupported anchor count: " + anchorCount);
    }
    source = createSource(anchorCount);
    compiledTemplate = CompiledTemplate.compile(source);
  }

  static String createSource(int anchorCount) {
    StringBuilder result = new StringBuilder(512_000);
    for (int i = 0; i < anchorCount; i++) {
      result.append("x".repeat(512_000 / anchorCount - 32));
      result.append("<anchor_").append(i).append('>');
    }
    result.append("x".repeat(512_000 - result.length()));
    return result.toString();
  }
}
