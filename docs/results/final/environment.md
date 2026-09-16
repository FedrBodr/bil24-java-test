
## parser — 2026-09-16T20:43:54.284698+02:00

- Code and benchmark commit: `79372b10bfd370b35b428fd6188a9748ad75523c`; code clean at launch.
- Reference archive SHA-256: `53be3446279e6312167e100cd86c66525801fcde78d3fac918f3ec1ed591a1b2`; package declaration only changed to `reference`.
- JMH 1.37; 3 forks; 5 × 1 s warmup; 5 × 1 s measurement; GC profiler.
- JVM: `-Xms512m -Xmx512m -XX:+UseG1GC`.
- Maven 3.9.9; generator reproducibility is tied to the code commit above.
- Parser: five datasets × 256 strings. Template: 512000 UTF-8 bytes, 10/100 anchors, 256 maps per thread.
- Shared desktop machine; project builds/tests and other JMH sessions are paused. OS and unrelated applications remain uncontrolled.

### Java

```text
openjdk version "21.0.6" 2025-01-21 LTS
OpenJDK Runtime Environment Corretto-21.0.6.7.1 (build 21.0.6+7-LTS)
OpenJDK 64-Bit Server VM Corretto-21.0.6.7.1 (build 21.0.6+7-LTS, mixed mode, sharing)
```

### OS

```text
ProductName:		macOS
ProductVersion:		14.4
BuildVersion:		23E214
```

### Architecture

```text
arm64
```

### CPU

```text
Apple M3 Max
```

### Logical processors / bytes of RAM

```text
14
38654705664
```

### Power at launch

```text
Now drawing from 'Battery Power'
 -InternalBattery-0 (id=20578403)	38%; discharging; 0:59 remaining present: true
```

### Power settings

```text
Battery Power:
 Sleep On Power Button 1
 powermode            0
 standby              1
 ttyskeepawake        1
 hibernatemode        3
 powernap             1
 hibernatefile        /var/vm/sleepimage
 displaysleep         10
 womp                 0
 networkoversleep     0
 sleep                1
 lessbright           1
 tcpkeepalive         1
 disksleep            10
AC Power:
 Sleep On Power Button 1
 powermode            0
 standby              1
 ttyskeepawake        1
 hibernatemode        3
 powernap             1
 hibernatefile        /var/vm/sleepimage
 displaysleep         0
 womp                 1
 networkoversleep     0
 sleep                1
 tcpkeepalive         1
 disksleep            10
```

### Load average at launch

```text
{ 8.04 7.66 7.59 }
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/parser-time.json > docs/results/final/parser-time.log 2>&1
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -bm thrpt -tu s -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/parser-throughput-t1.json > docs/results/final/parser-throughput-t1.log 2>&1
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*ParserBenchmark.*' -bm thrpt -tu s -t 4 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/parser-throughput-t4.json > docs/results/final/parser-throughput-t4.log 2>&1
```

## template — 2026-09-16T21:36:31.372812+02:00

- Code and benchmark commit: `b9e9cc55773ec182d5f71c831f5d3e6eb2605300`; code clean at launch.
- Reference archive SHA-256: `53be3446279e6312167e100cd86c66525801fcde78d3fac918f3ec1ed591a1b2`; package declaration only changed to `reference`.
- JMH 1.37; 3 forks; 5 × 1 s warmup; 5 × 1 s measurement; GC profiler.
- JVM: `-Xms512m -Xmx512m -XX:+UseG1GC`.
- Maven 3.9.9; generator reproducibility is tied to the code commit above.
- Parser: five datasets × 256 strings. Template: 512000 UTF-8 bytes, 10/100 anchors, 256 maps per thread.
- Shared desktop machine; project builds/tests and other JMH sessions are paused. OS and unrelated applications remain uncontrolled.

### Java

```text
openjdk version "21.0.6" 2025-01-21 LTS
OpenJDK Runtime Environment Corretto-21.0.6.7.1 (build 21.0.6+7-LTS)
OpenJDK 64-Bit Server VM Corretto-21.0.6.7.1 (build 21.0.6+7-LTS, mixed mode, sharing)
```

### OS

```text
ProductName:		macOS
ProductVersion:		14.4
BuildVersion:		23E214
```

### Architecture

```text
arm64
```

### CPU

```text
Apple M3 Max
```

### Logical processors / bytes of RAM

```text
14
38654705664
```

### Power at launch

```text
Now drawing from 'AC Power'
 -InternalBattery-0 (id=20578403)	74%; charging; 0:58 remaining present: true
```

### Power settings

```text
Battery Power:
 Sleep On Power Button 1
 powermode            0
 standby              1
 ttyskeepawake        1
 hibernatemode        3
 powernap             1
 hibernatefile        /var/vm/sleepimage
 displaysleep         10
 womp                 0
 networkoversleep     0
 sleep                1
 lessbright           1
 tcpkeepalive         1
 disksleep            10
AC Power:
 Sleep On Power Button 1
 powermode            0
 standby              1
 ttyskeepawake        1
 hibernatemode        3
 powernap             1
 hibernatefile        /var/vm/sleepimage
 displaysleep         0
 womp                 1
 networkoversleep     0
 sleep                1
 tcpkeepalive         1
 disksleep            10
```

### Load average at launch

```text
{ 8.93 7.78 7.71 }
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-time.json > docs/results/final/template-time.log 2>&1
```

### Повторная проверка Maven Wrapper, 16.09.2026

Вывод `./mvnw -version` проверен после измерений парсера и перед измерениями шаблонов; версия Wrapper и Maven не менялась. Ранее зафиксированная версия — 3.9.9.

```text
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /private/tmp/bil24-tools/maven-home/wrapper/dists/apache-maven-3.9.9/3477a4f1
Java version: 21.0.6, vendor: Amazon.com Inc., runtime: /Users/d.fedorenko/Library/Java/JavaVirtualMachines/corretto-21.0.6/Contents/Home
Default locale: en_RU, platform encoding: UTF-8
OS name: "mac os x", version: "14.4", arch: "aarch64", family: "mac"
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -bm thrpt -tu s -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-throughput-t1.json > docs/results/final/template-throughput-t1.log 2>&1
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*TemplateRenderBenchmark.*' -bm thrpt -tu s -t 4 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-throughput-t4.json > docs/results/final/template-throughput-t4.log 2>&1
```

```sh
java -jar benchmarks/target/benchmarks.jar '.*TemplatePreparationBenchmark.*' -bm avgt -tu ns -t 1 -f 3 -wi 5 -i 5 -w 1s -r 1s -prof gc -jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC' -rf json -rff docs/results/final/template-compile.json > docs/results/final/template-compile.log 2>&1
```
