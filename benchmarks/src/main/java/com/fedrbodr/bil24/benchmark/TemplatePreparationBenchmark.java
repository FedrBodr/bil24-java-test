package com.fedrbodr.bil24.benchmark;

import com.fedrbodr.bil24.template.CompiledTemplate;
import org.openjdk.jmh.annotations.Benchmark;

public class TemplatePreparationBenchmark {
  @Benchmark
  public CompiledTemplate compile(TemplateBenchmarkData data) {
    return CompiledTemplate.compile(data.source);
  }
}
