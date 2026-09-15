# BIL24 Template Renderer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Реализовать и объяснить подстановки в неизменяемые шаблоны и измерить стоимость переноса подготовки за пределы запроса.

**Architecture:** Подготовленный шаблон хранит неизменяемые текстовые части и ключи. Каждый запрос собирает собственную строку из своей Map. Отдельный benchmark сравнивает предварительную подготовку с подготовкой той же строки на каждом запросе.

**Tech Stack:** Java 17/21, Maven 3.9.9 через Maven Wrapper, JUnit Jupiter 5.11.4, JMH 1.37.

**Статус:** на ревью; реализация не выполнена. Зависимость: [план парсера](2026-09-15-seat-parser.md) предоставляет Maven reactor, профиль comparison и benchmarks jar. Этот план добавляет второй основной модуль и сохраняет результаты парсера.

## Global Constraints

- Java: исходный и целевой уровень 17; проверки на JDK 17 и 21; измерения на JDK 21.
- Сборка: Maven 3.9.9 через Maven Wrapper; версии зависимостей и плагинов фиксированы.
- Зависимости: основной код — только Java Standard Library; JUnit Jupiter 5.11.4 — тесты; JMH 1.37 — модуль benchmarks.
- Оформление: UTF-8, два пробела, K&R, без табуляций в Java-коде, осмысленные английские имена.
- Документация: русский язык, точные команды, фактические результаты и явно обозначенные ограничения.
- Эталон: SHA-256 архива 53be3446279e6312167e100cd86c66525801fcde78d3fac918f3ec1ed591a1b2; исходники эталона и бинарные сборки с ним не публикуются в Git.
- Процесс: реализация после ревью спецификации и планов; финальное ревью включает код, тесты и исходные результаты измерений.

Все пути от корня репозитория. Контракты T1–T8 — в [спецификации](../specs/2026-09-15-bil24-java-design.md), команды — в [методике](../../benchmark-methodology.md).

## Карта файлов

| Файл | Назначение |
|---|---|
| `template-renderer/pom.xml` | Второй основной модуль |
| `template-renderer/src/main/java/com/fedrbodr/bil24/template/CompiledTemplate.java` | Неизменяемое представление и рендер |
| `template-renderer/src/test/java/com/fedrbodr/bil24/template/CompiledTemplateTest.java` | Контракт синтаксиса и подстановки |
| `template-renderer/src/test/java/com/fedrbodr/bil24/template/TemplateConcurrencyTest.java` | Изоляция параллельных запросов |
| `template-renderer/src/test/java/com/fedrbodr/bil24/template/TemplateFilesTest.java` | Три UTF-8 файла и отсутствие повторного I/O |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/TemplateBenchmarkData.java` | Общий исходник и готовый шаблон для JMH |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/TemplateRequestState.java` | Набор Map и курсор каждого потока |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/TemplateRenderBenchmark.java` | Обе стратегии обработки запроса |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/TemplatePreparationBenchmark.java` | Разовая компиляция |
| `benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/TemplateBenchmarkContractTest.java` | Равенство измеряемых вариантов и контроль размера данных |
| `docs/template-algorithm.md` | Ответ на второе задание |

## Task 1: Подготовка шаблона и буквальные подстановки

**Files:** `pom.xml`, `template-renderer/pom.xml`, `CompiledTemplate.java`, `CompiledTemplateTest.java`.

**Interfaces:**

- Consumes: `String source`; на каждом рендере `Map<String,String>` с ключами без скобок.
- Produces: `CompiledTemplate.compile(String): CompiledTemplate`; `render(Map<String,String>): String` по T1–T8.

- [ ] **1.1. Добавить модуль и тесты контракта.** Parent перечисляет `template-renderer` после `seat-parser`. Модуль наследует release/кодировку/plugins и имеет JUnit только в test scope. Создать parameterized cases для всех правил §5.1. Первые тесты:

```java
@Test
void replacementsAreLiteralAndNotRecursive() {
  CompiledTemplate template = CompiledTemplate.compile("<user>/<area>/<user>");
  String result = template.render(Map.of("user", "<area>$\\", "area", "Зал"));
  assertEquals("<area>$\\/Зал/<area>$\\", result);
}

@Test
void missingValueRemainsAndEmptyValueRemovesAnchor() {
  CompiledTemplate template = CompiledTemplate.compile("<known>-<missing>-<empty>");
  assertEquals("Ира-<missing>-", template.render(Map.of("known", "Ира", "empty", "")));
}

@Test
void validInnerAnchorStillMatches() {
  assertEquals("<Ира>", CompiledTemplate.compile("<<name>>").render(Map.of("name", "Ира")));
}
```

Дополнительные входы: `""`, `"plain"`, `"<a><a>"`, `"<Name>/<name>"`, `"<1bad>"`, `"<bad-name>"`, `"<unfinished"`, `"<>"`, `"Привет, <_user2>!"`. Проверить точный ожидаемый текст, не только его длину.

- [ ] **1.2. Выполнить `./mvnw -pl template-renderer test`.** Ожидание первого запуска — отсутствующий класс `CompiledTemplate`. Зафиксировать этот результат до основного кода.

- [ ] **1.3. Реализовать хранение и compile.** Поля и алгоритм внутри `CompiledTemplate`:

```java
private static final Pattern ANCHOR_PATTERN = Pattern.compile("<([A-Za-z_][A-Za-z0-9_]*)>");
private final List<Segment> segmentList;
private final int sourceLength;

private CompiledTemplate(List<Segment> segmentList, int sourceLength) {
  this.segmentList = List.copyOf(segmentList);
  this.sourceLength = sourceLength;
}

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

private record Segment(String text, String key) {
}
```

Добавить конкретные imports из `java.util` и `java.util.regex`, без wildcard. `Segment` остаётся private; никаких публичных коллекций или метода изменения шаблона.

- [ ] **1.4. Реализовать render.** Значение `null` отличать от отсутствующего ключа; таблицу клиента не сохранять в объекте:

```java
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
```

Javadoc двух публичных методов фиксирует грамматику, null-ошибки, отсутствие рекурсивных замен и требование не менять Map во время render. Не добавлять HTML-экранирование, форматтеры, выражения или рекурсию.

- [ ] **1.5. Дополнить проверки ошибок.** Для null-значения использовать `HashMap`, поскольку `Map.of` его не допускает. Используемый ключ вызывает `IllegalArgumentException`, сообщение содержит ключ. `null` лишнего ключа игнорируется. `compile(null)` и `render(null)` вызывают `NullPointerException`. Передать неизменяемую Map и после вызова сравнить исходную изменяемую Map с заранее снятой копией, чтобы проверить T8.

- [ ] **1.6. Запустить `./mvnw -pl template-renderer test`.** Ожидание: все фиксированные cases проходят. Для проверки чувствительности тестов временно заменить отсутствующий якорь на пустую строку, увидеть падение `missingValueRemainsAndEmptyValueRemovesAnchor`, вернуть корректное поведение.

- [ ] **1.7. Commit:** `feat: add immutable template compilation and literal rendering`. Проверяемый результат — небольшой рендерер со всеми правилами подстановки.

## Task 2: Три файла и параллельные запросы

**Files:** `TemplateFilesTest.java`, `TemplateConcurrencyTest.java`, `docs/template-algorithm.md`.

**Interfaces:**

- Consumes: `CompiledTemplate.compile` и `render` из Task 1.
- Produces: проверки независимости от диска после подготовки и отсутствия смешивания данных клиентов; самодостаточное объяснение.

- [ ] **2.1. Создать тест трёх файлов в `@TempDir`.** Для каждого файла использовать префикс `"Шаблон " + i + ": <user_name> / <area_name>\n"`; дополнить ASCII-символами `x` до ровно 512 000 UTF-8 байт. Расчёт padding по `prefix.getBytes(StandardCharsets.UTF_8).length`, а не по числу char. Проверить `Files.size(path) == 512_000`. Прочитать через `Files.readString`, скомпилировать, сравнить render с заранее построенной строкой из префикса, значений и padding.

```java
String prefix = "Шаблон " + i + ": <user_name> / <area_name>\n";
String padding = "x".repeat(512_000 - prefix.getBytes(StandardCharsets.UTF_8).length);
String source = prefix + padding;
Files.writeString(path, source, StandardCharsets.UTF_8);
CompiledTemplate template = CompiledTemplate.compile(Files.readString(path, StandardCharsets.UTF_8));
String expected = "Шаблон " + i + ": Ира / Зал\n" + padding;
assertEquals(expected, template.render(Map.of("user_name", "Ира", "area_name", "Зал")));
Files.delete(path);
assertEquals(expected, template.render(Map.of("user_name", "Ира", "area_name", "Зал")));
```

Переменные `i` и `path` принадлежат циклу по трём файлам внутри временного каталога. Тест не читает и не удаляет пользовательские файлы.

- [ ] **2.2. Создать тест 8 параллельных задач по 100 render.** Один общий подготовленный шаблон `"<user>/<area>/<user>"`; отдельная Map с user=`"u" + threadIndex + "-" + iteration`, area=`"a" + threadIndex`. `CountDownLatch` даёт общий старт. В каждой итерации точное ожидание `user + "/" + area + "/" + user`; результаты `Future.get` обязательно проверяются. Лимит 30 секунд; `shutdownNow`/`awaitTermination` в finally. Следующий однопоточный render проверяет, что предыдущие таблицы не сохранены в шаблоне.

Сначала отправить все задачи через `submit`, затем открыть latch; на ожидание всех `Future` использовать один общий deadline в 30 секунд.

- [ ] **2.3. Выполнить `./mvnw -pl template-renderer test`.** Ожидание: три файла проверены до и после удаления, 800 параллельных рендеров с точным совпадением. По коду отдельно убедиться, что matcher существует только в compile, а render использует только локальный builder.

- [ ] **2.4. Написать объяснение в `docs/template-algorithm.md`.** Разделы: условия BIL24; принятые правила; загрузка трёх файлов; подготовка; обработка запроса; конкурентность; затраты; ограничения. Включить рабочий фрагмент:

```java
Map<String, CompiledTemplate> templateMap = Map.of(
    "welcome", CompiledTemplate.compile(Files.readString(welcomePath, StandardCharsets.UTF_8)),
    "reminder", CompiledTemplate.compile(Files.readString(reminderPath, StandardCharsets.UTF_8)),
    "receipt", CompiledTemplate.compile(Files.readString(receiptPath, StandardCharsets.UTF_8)));
String response = templateMap.get("welcome")
    .render(Map.of("user_name", "Ира", "area_name", "Большой зал"));
```

В тексте определить три `Path` как конфигурацию приложения; I/O-ошибка загрузки прекращает запуск приложения с указанием файла, вместо обслуживания неполным набором шаблонов. Это пример интеграции, не добавление веб-сервера. Объяснить O(N + A) память подготовленного шаблона, O(A + R) обход и O(N + A + R) полную стоимость запроса с предварительным резервированием буфера; временная память — O(N + R), lookup Map считается обычной стоимости. Указать, что при небольшом трафике выигрыш может быть практически несущественен и важна простота.

- [ ] **2.5. Commit:** `test: verify cached templates with files and concurrent requests`.

## Task 3: Измерение подготовки и обработки запроса

**Files:** `benchmarks/pom.xml`, четыре template benchmark-класса, `TemplateBenchmarkContractTest.java`, workflow, `docs/results/final/template-*.json` и логи, `docs/results/final/environment.md`, `docs/results/README.md`.

**Interfaces:**

- Consumes: общий JMH jar из плана 1, API `CompiledTemplate`.
- Produces: `TemplateRenderBenchmark.parseAndRender`, `TemplateRenderBenchmark.render`, `TemplatePreparationBenchmark.compile`; исходные результаты для 10/100 якорей.

- [ ] **3.1. Добавить `template-renderer:1.0-SNAPSHOT` как dependency benchmarks.** Runtime-зависимости двух основных модулей не меняются.

- [ ] **3.2. Создать `TemplateBenchmarkData` с `@State(Scope.Benchmark)`.** Поля `@Param({"10", "100"}) public int anchorCount`, `String source`, `CompiledTemplate compiledTemplate`. `@Setup(Level.Trial)` генерирует строку с равномерно разнесёнными именами `anchor_0` … `anchor_(anchorCount-1)`, затем делает compile. Конкретный генератор:

```java
static String createSource(int anchorCount) {
  StringBuilder result = new StringBuilder(512_000);
  for (int i = 0; i < anchorCount; i++) {
    result.append("x".repeat(512_000 / anchorCount - 32));
    result.append("<anchor_").append(i).append('>');
  }
  result.append("x".repeat(512_000 - result.length()));
  return result.toString();
}
```

Генератор предназначен только для параметров 10 и 100; проверить это в вызывающем setup с `IllegalArgumentException` на другое значение. Для этих параметров формула даёт неотрицательное число повторений и точный размер ASCII/UTF-8.

- [ ] **3.3. Создать `TemplateRequestState` с `@State(Scope.Thread)`.** Поля `List<Map<String,String>> replacementMapList`, `int cursor`. Метод setup принимает `TemplateBenchmarkData data` как JMH state dependency; создаёт 256 `HashMap`, в каждой ключи `anchor_j`, значения `"request-" + requestIndex + "-value-" + j`, сохраняет `Map.copyOf` и `List.copyOf`. Каждому потоку принадлежит собственный список и курсор; общий compiledTemplate читается всеми. Метод:

```java
Map<String, String> nextReplacementMap() {
  return replacementMapList.get(cursor++ & 255);
}
```

Генерация и копирование таблиц выполняются только в setup. В тестах шаблонов, в отличие от benchmark, user включает номер потока для обнаружения смешивания.

- [ ] **3.4. Реализовать две ветки запроса и отдельную подготовку.** В `TemplateRenderBenchmark`:

```java
@Benchmark
public String parseAndRender(TemplateBenchmarkData data, TemplateRequestState state) {
  return CompiledTemplate.compile(data.source).render(state.nextReplacementMap());
}

@Benchmark
public String render(TemplateBenchmarkData data, TemplateRequestState state) {
  return data.compiledTemplate.render(state.nextReplacementMap());
}
```

В отдельном `TemplatePreparationBenchmark`:

```java
@Benchmark
public CompiledTemplate compile(TemplateBenchmarkData data) {
  return CompiledTemplate.compile(data.source);
}
```

Измеряемые методы не включают I/O, asserts, цикл по всем запросам или ручной замер `nanoTime`.

- [ ] **3.5. До benchmark добавить `TemplateBenchmarkContractTest`.** Для каждого `anchorCount` вызвать setup данных, проверить размер `source.getBytes(UTF_8).length == 512_000`, точное количество совпадений с грамматикой и 256 карт. Для всех карт сравнить строку `compile(source).render(map)` со строкой `compiledTemplate.render(map)` и независимо проверить наличие значений первого/последнего якоря и отсутствие исходных использованных якорей. Явные ожидаемые тесты Task 1 предотвращают ситуацию, когда обе ветки ошибаются одинаково.

- [ ] **3.6. Выполнить `./mvnw -Pcomparison clean verify` и template smoke-команду из методики.** Ожидание: корректность обеих библиотек и эталонные тесты парсера подтверждены; smoke показывает обе ветки при 10 якорях. Добавить этот короткий smoke в JDK 21 job после parser smoke; проверить обе JDK в CI.

- [ ] **3.7. Commit кода benchmark и тестов:** `perf: compare template preparation and request rendering`. На чистом коде записать его SHA отдельно от SHA ранее измеренного парсера в environment; не менять поля его окружения задним числом.

- [ ] **3.8. Выполнить четыре template-команды полного протокола.** В каждом render JSON — 4 результата: 2 стратегии × 2 размера числа якорей. В compile JSON — 2 результата. Каждый результат содержит все forks и allocation metric; ошибки и пропуски не допускаются. Ожидание по направлению эффекта — меньше работы при повторном render, но вывод формулируется только по фактическим данным.

- [ ] **3.9. Дополнить общий отчёт.** Показать запросы отдельно от компиляции, `ns/op`, `ops/s`, `B/op` и интервалы. Обозначить контрольную ветку как нашу, а не код BIL24. Отдельно объяснить, что размер итоговой строки создаёт существенную общую стоимость даже после подготовки. Разобрать регрессии и неопределённые различия по правилам методики.

- [ ] **3.10. Commit результатов:** `docs: publish template measurements and algorithm explanation`.

## Task 4: Финальная проверка и передача кода на ревью

**Files:** README, оба плана (фактические отметки), спецификация (критерии готовности), `docs/results/README.md`.

**Interfaces:**

- Consumes: оба задания, полная CI-проверка, исходные результаты измерений.
- Produces: воспроизводимый репозиторий и ссылки для финального ревью.

- [ ] **4.1. Проверить весь цикл из чистого клона в отдельном временном каталоге.** Команды: Python unit-тесты, `./mvnw clean verify`, получение reference, `./mvnw -Pcomparison clean verify`, оба smoke. До загрузки reference обычная сборка проходит, профиль comparison сообщает понятную ошибку; после загрузки проходит полный набор. Полные замеры повторно без причины не запускать.
- [ ] **4.2. Проверить CI JDK 17/21, `git diff --check` и runtime dependency tree двух основных модулей.** Просмотреть Java-файлы на отступы, imports, API и объяснения нетривиальных решений по стандарту BIL24. Шумные тесты скорости не добавлять в обязательные проверки.
- [ ] **4.3. Проверить Git-состав.** `git ls-files` не должен содержать `.cache`, `target`, архив BIL24, `reference/*.java` или бинарный benchmark jar. Проверить ссылки в Markdown. JSON и logs допустимы только как фактические результаты измерений. Никаких токенов или локальных конфигураций аккаунтов.
- [ ] **4.4. Сверить P1–P8, T1–T8 и B1–B8 с кодом, тестами и отчётом.** Отмечать checkbox только при наличии проверки. Не утверждать устранение худшего случая исходной регулярки или улучшение реального веб-сервера.
- [ ] **4.5. Обновить README текущим статусом, командами и результатами.** Вынести 2–3 подтверждённых наблюдения со ссылками на полные таблицы. Если скорость статистически не отличается, сообщить это вместо рекламного коэффициента.
- [ ] **4.6. Сделать финальный документирующий commit и push.** Передать владельцу GitHub-ссылки на основные классы, тесты, отчёт, raw JSON и commit SHA. Отдельно указать ограничения и результаты CI. Завершение реализации не означает, что владелец уже одобрил финальный код.

## Проверка покрытия перед исполнением

| Требования | Задача |
|---|---|
| T1–T6 | 1.1–1.6 |
| T7–T8 | 1.5, 2.1–2.3 |
| Три файла, объяснение и затраты | 2.1, 2.4 |
| B2–B8 для шаблонов | 3.1–3.10 |
| Чистая сборка, CI, документация, финальное ревью | 4.1–4.6 |

Самопроверка плана: обе стратегии имеют одинаковую семантику, I/O исключён из обеих веток, Map не создаётся внутри измерения, одноразовая подготовка измеряется отдельно. API и имена benchmark совпадают с методикой и спецификацией.
