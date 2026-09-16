# Методика измерений до и после

Статус: протокол одобрен 16.09.2026. Полный протокол выполнен: первоначальный оригинал — `docs/results/baseline-initial/`, итоговые пары — `docs/results/final/`, [отчёт](results/README.md).

## 1. Что сравниваем

### Парсер

Оригинальный `reference.ParserSeat` из архива BIL24 против оптимизированного `test.ParserSeat`. В эталоне заменено только объявление пакета. Оба метода вызываются напрямую, возвращаемый объект потребляется JMH. Дополнительных адаптеров или reflection внутри измерений нет.

Первый полный запуск оригинала выполняется до оптимизации. Основной итоговый вывод строится по повторному сравнению оригинала и новой реализации в одной окончательной версии benchmark-модуля на одном компьютере. Первичный запуск сохраняется как проверяемый этап работы; его числа не смешиваются с итоговой парой.

Пять независимых наборов, по 256 строк в каждом:

| `dataset` | Содержимое |
|---|---|
| `latin` | Имена секторов из латиницы и цифр, замены не нужны |
| `cyrillic` | Имена секторов с исходными 12 кириллическими буквами |
| `noSectorName` | Партер без отдельного имени сектора |
| `invalid` | Короткие несовпадающие строки и строки с отсутствующим разделителем |
| `repeatedMarkers` | Повторный маркер `Место`, сохраняющий особенности оригинала |

Строки создаются в `@Setup`, обходятся циклически. Счётчик, индекс входа и id принадлежат потоку. `null` проверяется тестами, а не этим benchmark. Корпуса синтетические: рабочее распределение данных BIL24 неизвестно.

### Шаблоны

Два способа обработки уже загруженной строки:

- `parseAndRender`: `CompiledTemplate.compile(source).render(replacementMap)` на каждом запросе.
- `render`: `compiledTemplate.render(replacementMap)` после подготовки в `@Setup`.

Первый вариант — наш контрольный способ организации обработки. BIL24 не предоставил исходный рендерер. Обе ветки используют одинаковые правила подстановки, исходник и таблицы; различие — момент подготовки.

Каждый исходник занимает ровно 512 000 байт в UTF-8. `anchorCount`: 10 и 100 вхождений, имена `anchor_0` … `anchor_9` либо `anchor_99`. Источник синтетический ASCII для точного размера; Unicode отдельно покрывается тестами. На поток подготовлено 256 неизменяемых таблиц с различными значениями. Генерация таблиц исключена из времени рендера в обеих ветках.

Подготовка измеряется дополнительно отдельным `TemplatePreparationBenchmark.compile`. В приложении эта подготовка выполняется при загрузке; JMH измеряет стоимость одного compile на прогретой JVM. Это не измерение холодного старта и не время обработки запроса. Чтение файлов в эти CPU/память-измерения не входит; эффект дискового кэша операционной системы не приписывается оптимизации Java.

## 2. Параметры и метрики

| Параметр | Полный запуск |
|---|---|
| JMH | 1.37 |
| JVM | JDK 21, точная сборка в окружении |
| Forks | 3 отдельных JVM |
| Прогрев | 5 итераций × 1 секунда |
| Измерения | 5 итераций × 1 секунда |
| Heap/GC | `-Xms512m -Xmx512m -XX:+UseG1GC` |
| Время операции | `avgt`, `ns/op`, 1 поток |
| Пропускная способность | `thrpt`, `ops/s`, 1 и 4 потока |
| Аллокации | `-prof gc`, основная метрика `gc.alloc.rate.norm`, `B/op` |
| Сохранение | JSON `-rf json -rff`, полный stdout и stderr |

`B/op` означает выделение памяти на операцию. Это не retained heap, не максимальная память процесса и не объём живых объектов. GC count/time можно добавить как диагностические данные. Среднее время операции не является p95/p99 задержки веб-запроса. Пропускная способность нескольких потоков — суммарная, а не на один поток.

Benchmark-сессии запускаются последовательно на свободной машине; не совмещаются с другими тестами, сборками или benchmark. Фиксируются питание от сети, режим энергопотребления, другие существенные нагрузки и доступные процессоры. Виртуализированная среда и фоновые процессы указываются как ограничения, если применимо.

## 3. Команды

Сначала проверка корректности:

```sh
python3 scripts/fetch_reference.py
./mvnw -Pcomparison clean verify
mkdir -p docs/results/baseline-initial docs/results/final
```

При первом запуске оригинала benchmark-класс содержит только метод `baseline`. Для корпуса из пяти наборов выполнить:

```sh
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.baseline' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/baseline-initial/parser-time.json > docs/results/baseline-initial/parser-time.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.baseline' -bm thrpt -tu s -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/baseline-initial/parser-throughput-t1.json > docs/results/baseline-initial/parser-throughput-t1.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.baseline' -bm thrpt -tu s -t 4 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/baseline-initial/parser-throughput-t4.json > docs/results/baseline-initial/parser-throughput-t4.log 2>&1
```

После реализации и успешной проверки всех тестов:

```sh
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/parser-time.json > docs/results/final/parser-time.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -bm thrpt -tu s -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/parser-throughput-t1.json > docs/results/final/parser-throughput-t1.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -bm thrpt -tu s -t 4 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/parser-throughput-t4.json > docs/results/final/parser-throughput-t4.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-time.json > docs/results/final/template-time.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -bm thrpt -tu s -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-throughput-t1.json > docs/results/final/template-throughput-t1.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -bm thrpt -tu s -t 4 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-throughput-t4.json > docs/results/final/template-throughput-t4.log 2>&1
java -jar benchmarks/target/benchmarks.jar '.*TemplatePreparationBenchmark.*' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-compile.json > docs/results/final/template-compile.log 2>&1
```

Каждая команда запускается отдельно, проверяются её exit code, отсутствие `<failure>`/исключений в логе и полнота JSON. Отсутствующий сценарий — незавершённое измерение. Нельзя заменять неудачные результаты нулевыми значениями.

Для проверки работоспособности перед полным запуском:

```sh
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -p dataset=latin -bm avgt -tu ns -t 1 -f 1 -wi 1 -i 1 -w 100ms -r 100ms
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -p anchorCount=10 -bm avgt -tu ns -t 1 -f 1 -wi 1 -i 1 -w 100ms -r 100ms
```

Короткие запуски не включаются в доказательства ускорения. Чистое время полных измерительных итераций по этому протоколу — около 30 минут; старты JVM, тесты и анализ добавят время.

## 4. Окружение и воспроизводимость

Для `baseline-initial` и `final` создаются отдельные `environment.md`. До запуска фиксируются:

- Git commit SHA и чистота рабочей копии; SHA включает код и benchmark, результаты коммитятся отдельно после запуска.
- SHA-256 оригинального архива и единственное преобразование пакета.
- Полный `java -version` и `./mvnw -version`.
- OS, архитектура, CPU model, количество доступных логических процессоров и память.
- Все параметры JVM/JMH, дата и часовой пояс, питание/энергосбережение и существенная фоновая нагрузка.
- Параметры генерации: 256 строк в каждом parser dataset; 512 000 байт, 10/100 якорей и 256 Map на поток для шаблонов.
- Имена и SHA-256 файлов с входными корпусами либо детерминированный генератор с версией через commit SHA. В этой реализации используется второй вариант.

Парсер и шаблоны могут измеряться на разных commit, поскольку реализуются последовательно. Тогда для каждого набора результатов указывается его собственный SHA в `environment.md` и итоговом отчёте. Числа разных реализаций сравниваются только внутри одной пары и одного окружения.

## 5. Отчёт

`docs/results/README.md` содержит по сценарию:

| Сценарий | Потоки | Вариант | Среднее и интервал | Единица | B/op | Отношение |
|---|---|---|---|---|---|---|

Числа заполняются только из завершённых JSON. Для времени ускорение = время контрольного варианта / время нового; для throughput = новый / контрольный. Экономия выделений = `1 - new_B_per_op / baseline_B_per_op`; при нулевом знаменателе процент не вычисляется.

Сохраняются score, scoreError и scoreConfidence, которые выводит JMH. При пересечении доверительных интервалов вывод об ускорении считается неубедительным. Регрессии и ограничения видны в основном тексте отчёта. Результаты разных datasets, числа потоков и числа якорей не усредняются в один коэффициент.

Отдельно объясняются:

- вклад устранения повторной компиляции regex и строковых проходов как гипотезы; из двух конечных вариантов нельзя точно разложить общий эффект по изменениям;
- разовая стоимость подготовки шаблона и стоимость каждого ответа;
- отсутствие реального трафика BIL24, сетевой задержки и дискового I/O в этих измерениях;
- ограниченность проверки худших входов исходного регулярного выражения.

При сбое выполняется новый запуск только затронутого набора с теми же параметрами; исходный лог сохраняется. При смене настроек/корпуса/кода сравниваемая пара измеряется заново, причины указываются. Автоматических порогов скорости в CI нет.

## 6. Первичные источники методики

JMH рекомендует [потреблять результат вычисления](https://github.com/openjdk/jmh/blob/1.37/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_08_DeadCode.java) и использовать [отдельные JVM для изоляции профилей](https://github.com/openjdk/jmh/blob/1.37/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_12_Forking.java). Метрики выделения памяти описаны в [примере профилировщиков](https://github.com/openjdk/jmh/blob/1.37/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_35_Profilers.java).
