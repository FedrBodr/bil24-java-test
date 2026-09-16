# Проверка реализации

Дата: 16.09.2026. Реализация подготовлена для финального ревью владельца.

## Сборка

Измеренный код шаблонов и окончательная версия обоих модулей:
[`b9e9cc5`](https://github.com/FedrBodr/bil24-java-test/commit/b9e9cc55773ec182d5f71c831f5d3e6eb2605300).
[CI на JDK 17 и 21](https://github.com/FedrBodr/bil24-java-test/actions/runs/35141585409)
прошёл успешно: 111 Java-тестов и 5 Python-тестов. JMH smoke выполняется на JDK 21.

Из чистого локального Git-клона коммита
[`d9fa585`](https://github.com/FedrBodr/bil24-java-test/commit/d9fa585215284571df7fcf3db4d68fb0d06aad8d)
повторён следующий цикл на Corretto 21.0.6. Клон содержит только отслеживаемые файлы;
кэша эталона в начале нет. Код и сборочная конфигурация этого коммита совпадают с
`b9e9cc5`; последующие изменения относятся к отчёту и документации.

| Проверка | Результат |
|---|---|
| `python3 -m unittest discover -s scripts -p 'test_*.py'` | 5 тестов, успешно |
| `./mvnw -B -ntp clean verify` | 67 тестов основных библиотек, успешно без эталона |
| `./mvnw -B -ntp -Pcomparison verify` до загрузки эталона | Ожидаемый exit 1 и команда `python3 scripts/fetch_reference.py` в сообщении |
| `python3 scripts/fetch_reference.py` | Архив получен, SHA-256 проверен, пакет преобразован |
| `./mvnw -B -ntp -Pcomparison clean verify` | 111 тестов, 0 failures/errors/skips |
| Два smoke из [методики](benchmark-methodology.md) | Обе ветки каждого сравнения выполнились |
| `./mvnw -B -ntp -pl seat-parser,template-renderer dependency:tree -Dscope=runtime` | Сторонних runtime-зависимостей нет |

## Покрытие спецификации

| Контракт | Проверка |
|---|---|
| P1–P6: поля, имена, регулярка, null, пробелы, Unicode, id | [Контракт парсера и DTO](../seat-parser/src/test/java/test), [сравнение с оригиналом](../benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/ParserDifferentialTest.java) |
| P7–P8: независимость и параллельные вызовы | [8 потоков × 1 000 сравнений](../benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/ParserConcurrencyTest.java); по коду общий только неизменяемый Pattern |
| T1–T6, T8: синтаксис, буквальная подстановка, ошибки, сохранность Map | [CompiledTemplateTest](../template-renderer/src/test/java/com/fedrbodr/bil24/template/CompiledTemplateTest.java) |
| T7: изоляция запросов | [8 потоков × 100 рендеров](../template-renderer/src/test/java/com/fedrbodr/bil24/template/TemplateConcurrencyTest.java) |
| Три файла и отсутствие повторного I/O | [TemplateFilesTest](../template-renderer/src/test/java/com/fedrbodr/bil24/template/TemplateFilesTest.java), до и после удаления файлов |
| B1–B8: эквивалентность, методика и исходные измерения | [Benchmark-код](../benchmarks/src/main/java/com/fedrbodr/bil24/benchmark), [контракт шаблонов benchmark](../benchmarks/src/test/java/com/fedrbodr/bil24/benchmark/TemplateBenchmarkContractTest.java), [отчёт](results/README.md) |

Сгенерированный корпус парсера содержит 10 000 случаев с seed `240915`.
Проверена чувствительность тестов: неверная замена `А → Z` и удаление отсутствующего
якоря приводили к ожидаемым падениям; корректное поведение восстановлено перед
финальными проверками.

## Артефакты и ограничения

Проверены 59 записей JMH: 15 первоначальных и 44 итоговых. В каждой — три forks,
по пять измерительных итераций, конечные значения метрик и данные выделения памяти.
Полные логи завершены без ошибок. В 21 итоговой паре нет регрессий более 5%; интервалы
разделены в пользу нового варианта. Условия и ограничения приведены в отчёте.

`git diff --check` проходит. Проверены относительные ссылки Markdown, UTF-8,
отсутствие табуляций и wildcard imports в Java. В Git нет `.cache`, `target`, архива
оригинала, исходников пакета `reference` и бинарного JMH jar.

Проверки подтверждают заявленные случаи и устройство кода. Они не доказывают
поведение для всех возможных входов и расписаний потоков; измерения не описывают
производственный трафик BIL24.
