package com.fedrbodr.bil24.benchmark;

import com.fedrbodr.bil24.template.CompiledTemplate;
import org.openjdk.jmh.annotations.Benchmark;

public class TemplateRenderBenchmark {
  @Benchmark
  public String parseAndRender(TemplateBenchmarkData data, TemplateRequestState state) {
    return CompiledTemplate.compile(data.source).render(state.nextReplacementMap());
  }

  @Benchmark
  public String render(TemplateBenchmarkData data, TemplateRequestState state) {
    return data.compiledTemplate.render(state.nextReplacementMap());
  }
}
