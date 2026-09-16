# BIL24 Seat Parser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Сохранить поведение исходного парсера BIL24 и измерить эффект оптимизации относительно зафиксированного оригинала.

**Architecture:** Основная библиотека сохраняет API `test.ParserSeat` и `test.ResultParser`. Отдельный профиль сравнения получает проверенный оригинал в пакете `reference`, сравнивает результаты и запускает JMH. Эталон и измерительная инфраструктура не являются runtime-зависимостями библиотеки.

**Tech Stack:** Java 17/21, Maven 3.9.9 через Maven Wrapper, JUnit Jupiter 5.11.4, JMH 1.37, Python 3.9+ для получения эталона.

**Статус:** выполнен 16.09.2026; код и исходные измерения переданы в ветку для финального ревью. Фрагменты кода — конкретные указания для будущей реализации, не готовые исходные файлы.

## Global Constraints

- Java: исходный и целевой уровень 17; проверки на JDK 17 и 21; измерения на JDK 21.
- Сборка: Maven 3.9.9 через Maven Wrapper; версии зависимостей и плагинов фиксированы.
- Зависимости: основной код — только Java Standard Library; JUnit Jupiter 5.11.4 — тесты; JMH 1.37 — модуль benchmarks.
- Оформление: UTF-8, два пробела, K&R, без табуляций в Java-коде, осмысленные английские имена.
- Документация: русский язык, точные команды, фактические результаты и явно обозначенные ограничения.
- Эталон: SHA-256 архива 53be3446279e6312167e100cd86c66525801fcde78d3fac918f3ec1ed591a1b2; исходники эталона и бинарные сборки с ним не публикуются в Git.
- Процесс: реализация после ревью спецификации и планов; финальное ревью включает код, тесты и исходные результаты измерений.

Связанные документы: [спецификация](../specs/2026-09-15-bil24-java-design.md), [методика](../../benchmark-methodology.md). Все пути ниже — от корня самостоятельного репозитория.

## Карта файлов

| Файл | Ответственность |
|---|---|
| `pom.xml` | Reactor, общие версии, профиль comparison |
| `seat-parser/pom.xml` | Библиотека, JUnit только в test scope |
| `benchmarks/pom.xml` | Эталонные исходники, JMH processor, shaded executable jar |
| `scripts/fetch_reference.py` | Получение архива, проверка SHA, смена имени пакета |
| `scripts/test_fetch_reference.py` | Проверка ошибок checksum и преобразования |
| `seat-parser/src/main/java/test/ParserSeat.java` | Парсинг и нормализация имени сектора |
| `seat-parser/src/main/java/test/ResultParser.java` | Существующий контракт результата |
| `seat-parser/src/test/java/test/ParserSeatContractTest.java` | Фиксированные ожидания P1–P8 |
| `seat-parser/src/test/java/test/ResultParserTest.java` | Прямые setter/getter и getFull-контракты |
| `benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/ReferenceContractTest.java` | Характеризация оригинала до оптимизации |
| `benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/ParserSnapshot.java` | Полный снимок результата обеих реализаций |
| `benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/ParserDifferentialTest.java` | Фиксированные и сгенерированные сравнения |
| `benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/ParserConcurrencyTest.java` | Параллельные сравнения |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/ParserInputCorpus.java` | Детерминированные входы JMH |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/ParserBenchmarkState.java` | Локальное состояние потока JMH |
| `benchmarks/src/main/java/com/fedrbodr/bil24/benchmark/ParserBenchmark.java` | Прямые benchmark-вызовы |

## Task 1: Воспроизводимый эталон и первоначальные измерения

**Files:** все POM, Maven Wrapper, загрузчик и его тест, `ReferenceContractTest`, три класса parser benchmark, `docs/results/baseline-initial/`, `.github/workflows/verify.yml`.

**Interfaces:**

- Consumes: URL и SHA-256 из спецификации, два именованных Java-файла архива.
- Produces: `.cache/bil24/reference-src/reference/ParserSeat.java` и `ResultParser.java`; `reference.ParserSeat.parser(String,long)`; `ParserInputCorpus.create(String): String[]`; `benchmarks/target/benchmarks.jar` с методом `ParserBenchmark.baseline`.

- [x] **1.1. Написать тест загрузчика до реализации.** Python `unittest`: неправильный SHA отклоняется; для каждого исходного файла обратная замена `package reference;` даёт исходные байты; отсутствующий или повторный `package test;` отклоняется. Использовать маленький архив в памяти, без сетевого запроса в unit-тесте. Контракт функций:

```python
verify_archive(data: bytes, expected_sha256: str) -> None
relocate_source(data: bytes) -> bytes
```

Ожидание ошибки — `ValueError`. Пример содержательного теста:

```python
def test_package_relocation_preserves_the_rest(self):
    source = b"package test;\r\n\r\npublic class Example {}\r\n"
    actual = relocate_source(source)
    self.assertEqual(source, actual.replace(b"package reference;", b"package test;", 1))
```

- [x] **1.2. Выполнить `python3 -m unittest discover -s scripts -p 'test_*.py'`.** Сначала получить ошибку отсутствующей реализации. После добавления функций ниже ожидать успешное выполнение:

```python
def verify_archive(data: bytes, expected_sha256: str) -> None:
    import hashlib
    if hashlib.sha256(data).hexdigest() != expected_sha256:
        raise ValueError("BIL24 archive SHA-256 mismatch")


def relocate_source(data: bytes) -> bytes:
    old = b"package test;"
    if data.count(old) != 1:
        raise ValueError("Expected exactly one package declaration")
    return data.replace(old, b"package reference;", 1)
```

- [x] **1.3. Добавить `main` загрузчика.** Вычислить корень относительно `Path(__file__).resolve().parents[1]`; читать существующий `.cache/bil24/test_src.zip` либо загрузить URL через `urllib.request.urlopen(..., timeout=30)`; проверить SHA; только затем сохранять архив и два разрешённых имени. Использовать `ZipFile(BytesIO(data)).read(name)`, а не `extractall`. Записывать результат `relocate_source` в указанные пути. Любое исключение даёт ненулевой exit code. Сообщение успеха содержит SHA и пути, но не весь исходник. Проверить повторный запуск на локальном кэше и запуск с испорченной копией в отдельном `TemporaryDirectory`.

- [x] **1.4. Создать сборку, необходимую для эталона.** Parent coordinates: `com.fedrbodr.bil24:bil24-java-test:1.0-SNAPSHOT`, packaging `pom`. На этом шаге обычный модуль — `seat-parser`; профиль `comparison` добавляет `benchmarks`. Для Maven Wrapper выполнить:

```sh
mvn org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=3.9.9
```

Фиксировать compiler 3.13.0 (`release=17`), surefire 3.5.2 (`forkedProcessTimeoutInSeconds=120`), enforcer 3.5.0, build-helper 3.6.0 и shade 3.6.0. В `dependencyManagement` объявить JUnit 5.11.4; JMH 1.37 только в benchmarks. В Java source/target-кодировке использовать UTF-8.

Основные свойства:

```xml
<properties>
  <maven.compiler.release>17</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <junit.version>5.11.4</junit.version>
  <jmh.version>1.37</jmh.version>
</properties>
```

`benchmarks` зависит от `seat-parser`, `jmh-core`, JUnit в test scope. `jmh-generator-annprocess` на annotation processor path; generated JMH-код не редактировать. `build-helper:add-source` в generate-sources добавляет `${project.basedir}/../.cache/bil24/reference-src`. Enforcer в validate проверяет оба файла; сообщение содержит `python3 scripts/fetch_reference.py`. Shade задаёт main class `org.openjdk.jmh.Main` и итоговое имя `benchmarks.jar`.

- [x] **1.5. Зафиксировать поведение оригинала тестом.** Этот тест ещё не зависит от оптимизированной реализации:

```java
@Test
void preservesRepeatedSeatMarkerBehavior() {
  reference.ResultParser result = reference.ParserSeat.parser(
      "Сектор А Ряд 1 Место 2 Место 3", 42L);
  assertEquals("A", result.getSectorName());
  assertEquals("1 Место 2", result.getRowName());
  assertEquals("3", result.getSeatName());
  assertEquals(42L, result.getId());
}
```

Добавить проверки `null`, несовпадения и пустого ряда/места согласно таблице P1–P8. Запуск: `python3 scripts/fetch_reference.py`, затем `./mvnw -Pcomparison clean verify`. Ожидание: `BUILD SUCCESS`, выполняется `ReferenceContractTest`; отсутствие эталона не даёт успешную сборку профиля.

- [x] **1.6. Подготовить детерминированный корпус.** `ParserInputCorpus.create(dataset)` возвращает 256 строк, `i` от 0 до 255, по следующим точным правилам:

```java
String value = switch (dataset) {
  case "latin" -> "Сектор CETHYOPXABKM" + i + " Ряд 12 Место 34";
  case "cyrillic" -> "Сектор СЕТНУОРХАВКМ" + i + " Ряд 12 Место 34";
  case "noSectorName" -> "Партер Ряд " + i + " Место 34";
  case "invalid" -> switch (i % 4) {
    case 0 -> "неверный-билет-" + i;
    case 1 -> "Сектор A Ряд " + i;
    case 2 -> "Сектор A Место " + i;
    default -> "Сектор A ряд 1 Место " + i;
  };
  case "repeatedMarkers" -> "Сектор A" + i + " Ряд 1 Место 2 Место 3";
  default -> throw new IllegalArgumentException("Unknown dataset: " + dataset);
};
```

Добавить тест размера корпуса и успешности/неуспешности его категорий на оригинале. Запретить возвращать общий изменяемый массив между вызовами фабрики.

- [x] **1.7. Добавить JMH state и единственный исходный benchmark.** State — отдельный public-класс с `@State(Scope.Thread)`, полями `@Param({"latin", "cyrillic", "noSectorName", "invalid", "repeatedMarkers"}) public String dataset`, `String[] seatNameList`, `int cursor`, `long id`; в `@Setup(Level.Trial)` заполнить массив через фабрику и обнулить счётчики. Benchmark:

```java
@Benchmark
public reference.ResultParser baseline(ParserBenchmarkState state) {
  return reference.ParserSeat.parser(
      state.seatNameList[state.cursor++ & 255], ++state.id);
}
```

На этом этапе не добавлять метод optimized. Собрать профиль, выполнить parser smoke-команду из методики. Ожидание: один benchmark × один dataset, без ошибок.

- [x] **1.8. Добавить CI проверки JDK 17 и 21.** Workflow использует checkout и setup-java с проверенными фиксированными commit SHA официальных actions; distribution `temurin`, cache `maven`. Шаги: Python unit-тесты, явная загрузка эталона, `./mvnw -B -Pcomparison clean verify`. JDK 21 дополнительно запускает parser smoke. Timeout job — 15 минут, permissions — `contents: read`. Полные измерения в CI не запускаются.

- [x] **1.9. Проверить файлы для коммита, сделать содержательный commit инфраструктуры.** В Git входят инструменты и наши тесты, не `.cache`, `target` или исходники `reference`. Пример сообщения: `test: establish reproducible BIL24 reference and benchmark harness`.

- [x] **1.10. С чистой рабочей копией выполнить три первоначальные baseline-команды из методики.** Перед запуском записать окружение и commit SHA. Проверить 5 результатов в каждом JSON, одинаковые параметры и отсутствие ошибок. Сохранить файлы и отдельным коммитом `perf: record initial parser baseline`. Это первый проверяемый результат плана.

## Task 2: Совместимый оптимизированный парсер

**Files:** два основных Java-класса, `ParserSeatContractTest`, `ResultParserTest`, снимок и сравнительные тесты в benchmarks.

**Interfaces:**

- Consumes: эталон из Task 1 и требования P1–P8.
- Produces: `test.ParserSeat.parser(String,long): test.ResultParser`, прежний API результата; сравнительные тесты всех наблюдаемых полей.

- [x] **2.1. Написать тесты ожидаемого поведения до основного кода.** Включить каждую строку матрицы §4.4 спецификации. Использовать JUnit parameterized tests и отдельные проверки исключений. Пример:

```java
@Test
void replacesCyrillicLettersOnlyInSectorName() {
  ResultParser result = ParserSeat.parser("Сектор А Ряд А Место А", -7L);
  assertEquals("A", result.getSectorName());
  assertEquals("А", result.getRowName());
  assertEquals("А", result.getSeatName());
  assertEquals(-7L, result.getId());
}

@Test
void keepsNullInputFailure() {
  assertThrows(NullPointerException.class, () -> ParserSeat.parser(null, 0L));
}
```

- [x] **2.2. Выполнить `./mvnw -pl seat-parser test`.** На первом запуске ожидается ошибка отсутствующих `ParserSeat`/`ResultParser`. После создания API, если тесты падают, проверять фактическое расхождение с эталоном, не менять ожидания ради нового кода.

- [x] **2.3. Создать `ResultParser` с прежним публичным контрактом.** Поля: `long id`, шесть `String`: sector, sectorName, row, rowName, seat, seatName. Public no-arg constructor, getter/setter каждого поля. Смысл вычисляемых методов:

```java
public String getFullSector() {
  if (sector == null || sectorName == null) {
    return "";
  }
  return sector.equalsIgnoreCase("сектор") ? sectorName : sector + " " + sectorName;
}

public String getFullRow() {
  return row == null || rowName == null ? "" : row + " " + rowName;
}

public String getFullSeat() {
  return seat == null || seatName == null ? "" : seat + " " + seatName;
}
```

Тестировать также объект, созданный напрямую: `null` и пустые строки, смешанный регистр `СеКтОр`, изменение полей через setters. Getter/setter-тесты должны подтверждать этот публичный контракт, не проверять приватные поля через reflection.

- [x] **2.4. Создать `ParserSeat` с исходным выражением в константе.** Публичный no-arg constructor сохранить. `parser` реализовать по следующему порядку:

```java
Matcher matcher = SEAT_PATTERN.matcher(seatName);
if (!matcher.find()) {
  return null;
}
ResultParser result = new ResultParser();
result.setId(id);
String sectorWithoutName = matcher.group("sectorWithoutName");
if (sectorWithoutName != null) {
  result.setSector(sectorWithoutName);
  result.setSectorName("");
} else {
  result.setSector(matcher.group("sector"));
  result.setSectorName(normalizeSectorName(matcher.group("sectorName")));
}
result.setRow(matcher.group("row"));
result.setRowName(matcher.group("rowName"));
result.setSeat(matcher.group("seat"));
result.setSeatName(matcher.group("seatName"));
return result;
```

Выражение брать побуквенно из верифицированного оригинала, не менять `.*`, пробелы, Unicode-класс, named groups и флаги. Удаление последующих проверок полей обосновать тем, что все обязательные группы участвуют в каждом успешном совпадении. Эта гипотеза проверяется сравнительными тестами.

- [x] **2.5. Добавить однопроходное преобразование.** Вызывать только для непустого имени сектора из совпавшей группы. Метод не зависит от shared mutable state:

```java
private static String normalizeSectorName(String value) {
  StringBuilder builder = null;
  for (int i = 0; i < value.length(); i++) {
    char current = value.charAt(i);
    char replacement = switch (current) {
      case 'С' -> 'C';
      case 'Е' -> 'E';
      case 'Т' -> 'T';
      case 'Н' -> 'H';
      case 'У' -> 'Y';
      case 'О' -> 'O';
      case 'Р' -> 'P';
      case 'Х' -> 'X';
      case 'А' -> 'A';
      case 'В' -> 'B';
      case 'К' -> 'K';
      case 'М' -> 'M';
      default -> current;
    };
    if (builder == null && current != replacement) {
      builder = new StringBuilder(value.length());
      builder.append(value, 0, i);
    }
    if (builder != null) {
      builder.append(replacement);
    }
  }
  return builder == null ? value : builder.toString();
}
```

Не дополнять преобразование `trim`, сменой регистра, нормализацией Unicode или буквами, которых нет в оригинале.

- [x] **2.6. Добавить снимок результата для сравнения.** `ParserSnapshot` — test-only record с 10 компонентами: id, sector, sectorName, row, rowName, seat, seatName, fullSector, fullRow, fullSeat. Две overload-фабрики `of(test.ResultParser)` и `of(reference.ResultParser)` возвращают `null` на `null` и копируют все 10 значений через публичные getters. Это снимает проблему разных типов результатов без вмешательства в основной код.

```java
assertEquals(
    ParserSnapshot.of(reference.ParserSeat.parser(input, id)),
    ParserSnapshot.of(test.ParserSeat.parser(input, id)),
    () -> "seed=240915, case=" + caseIndex + ", id=" + id + ", input=" + escapedInput);
```

- [x] **2.7. Добавить 10 000 сравнений с seed 240915.** Генератор в `ParserDifferentialTest`: структурированные строки варьируют сектор/имя/ряд/место; мутации выполняют ровно одно удаление, вставку или замену UTF-16 единицы в структурированной строке; произвольные строки используют буквы обеих письменностей, цифры, пробел, табуляцию, CR/LF, NBSP и surrogate-единицы. Длина произвольных строк — 0–96 включительно. Точный seed, диапазоны и алфавит записать рядом с генератором, чтобы контрпример воспроизводился. Для каждого вызова сохранять результат либо класс исключения; сравнивать одно с другим. Отдельные фиксированные cases проверяют `Long.MIN_VALUE`, `Long.MAX_VALUE` и корректный длинный вход 4096 символов.

- [x] **2.8. Запустить `./mvnw -Pcomparison clean verify`.** Ожидание: все тесты проходят, сравнительный тест сообщает выполненные 10 000 cases. Намеренно заменить только соответствие `А -> A` на `А -> Z`, убедиться, что тесты замечают регрессию, и восстановить правильное соответствие до коммита. Это проверяет способность набора ловить смысловую ошибку.

- [x] **2.9. Commit:** `perf: optimize seat parsing while preserving legacy behavior`. Проверяемый результат задачи — совместимая библиотека с тестами, ещё без заявления об ускорении.

## Task 3: Параллельность и итоговые измерения парсера

**Files:** `ParserConcurrencyTest.java`, `ParserBenchmark.java`, `docs/results/final/parser-*.json`, соответствующие логи, `docs/results/final/environment.md`, `docs/results/README.md`, README.

**Interfaces:**

- Consumes: обе реализации и снимки из Task 2, корпуса из Task 1.
- Produces: проверенная параллельная совместимость; raw-результаты benchmark и интерпретация.

- [x] **3.1. Добавить проверку параллельных вызовов.** `Executors.newFixedThreadPool(8)`, `CountDownLatch` для общего старта; 8 задач по 1 000 входов. Использовать по очереди все пять benchmark-корпусов; id = номер потока × 1000 + индекс. Сравнивать снимок нового результата с оригиналом, хранить `Future`, обязательно вызвать `get`, чтобы ошибка рабочего потока не потерялась. Лимит ожидания — 30 секунд; `shutdownNow` и `awaitTermination` в finally. Проверить, что изменение результата одного вызова не меняет результат другого и результат следующего вызова.

Сначала отправить все 8 задач через `submit`, затем открыть latch, затем ждать `Future` с общим deadline на 30 секунд. Не ждать завершения задач перед открытием latch.

```java
test.ResultParser first = test.ParserSeat.parser("Сектор А Ряд 1 Место 2", 1L);
test.ResultParser second = test.ParserSeat.parser("Сектор А Ряд 1 Место 2", 2L);
assertNotSame(first, second);
first.setSectorName("changed");
assertEquals("A", second.getSectorName());
assertEquals("A", test.ParserSeat.parser("Сектор А Ряд 1 Место 2", 3L).getSectorName());
```

- [x] **3.2. Выполнить `./mvnw -Pcomparison clean verify` и проверку на обоих JDK в CI.** Ожидание: 8 000 параллельных сравнений завершены, исключения не проглочены, CI зелёный. Проверить по коду отсутствие shared Matcher и изменяемых общих буферов; успешный stress-test сам по себе не является доказательством для всех возможных расписаний потоков.

- [x] **3.3. Добавить второй метод JMH без изменения эталона и корпуса.**

```java
@Benchmark
public test.ResultParser optimized(ParserBenchmarkState state) {
  return test.ParserSeat.parser(
      state.seatNameList[state.cursor++ & 255], ++state.id);
}
```

Повторить parser smoke; ожидание — две реализации одного dataset. Сначала commit кода и проверок (`test: verify concurrent parser use and paired benchmarks`), затем запись SHA окружения и полный запуск. Рабочая копия кода во время запуска чистая.

- [x] **3.4. Выполнить три итоговые parser-команды из методики последовательно.** В каждом JSON ожидается 10 строк: 5 datasets × 2 реализации, со всеми fork-данными и метрикой `gc.alloc.rate.norm`. Проверить отсутствие ошибок в логах. На JDK/параметрах, отличных от первоначального этапа, явно отметить различие и использовать только итоговую пару для коэффициентов.

- [x] **3.5. Заполнить parser-раздел `docs/results/README.md`.** Для каждой пары показать время/throughput, интервалы, B/op, отношение. Нельзя заявлять ускорение при пересекающихся интервалах. Разобрать регрессии по критерию спецификации; если требуется менять код, после исправления повторить затронутые тесты и соответствующие пары измерений.

- [x] **3.6. Обновить README командами сборки и ссылками на результаты.** Указать синтетический характер данных и сохранённые особенности API. Проверить `./mvnw -pl seat-parser dependency:tree`: runtime-зависимостей кроме JDK нет. В документации основной команды `verify` указать, что полная совместимость с оригиналом проверяется профилем `comparison`.

- [x] **3.7. Commit результатов:** `docs: publish parser benchmark evidence and compatibility notes`. Передать к следующему плану рабочую сборку и исходные измерения; статус всего проекта остаётся «реализация», пока задание 2 не выполнено.

## Проверка покрытия перед исполнением

| Требования | Задача |
|---|---|
| Эталон и фиксированный SHA | 1.1–1.5 |
| P1–P7 | 2.1–2.8 |
| P8 | 3.1–3.2 |
| B1, B3–B8 для парсера | 1.6–1.10, 3.3–3.7 |
| JDK 17/21, стандарт оформления, отсутствие runtime-библиотек | 1.4, 1.8, 2.3–2.5, 3.6 |

Самопроверка плана: файлы и интерфейсы определены, эталон не меняется при оптимизации, функциональные проверки предшествуют заявлениям о скорости. Числа производительности заполняются при исполнении из JMH, а не на этапе планирования.
