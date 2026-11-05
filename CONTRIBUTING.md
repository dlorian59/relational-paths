# Development / Разработка

[README](README.md) · [Integration EN](docs/0.3.0/integration.en.md) · [Интеграция RU](docs/0.3.0/integration.ru.md)

Use a full JDK 17+; all library bytecode stays at release 17. LTS runtimes 17/21/25 and optional 27 use the same API. / Нужен полный JDK 17+; байткод библиотеки остаётся release 17. LTS 17/21/25 и optional 27 используют один API.

From this checkout / Из рабочей копии:

```bash
./gradlew build example publishToMavenLocal
```

To use the local artifact in a Gradle consumer, add mavenLocal() before mavenCentral() to repositories. Maven checks its local repository automatically. This is local artifact preparation; version 0.3.0 is not published to Maven Central. / Для локального артефакта в Gradle добавьте mavenLocal() перед mavenCentral() в repositories. Maven проверяет локальный репозиторий автоматически. Это локальная подготовка: версия 0.3.0 не опубликована в Maven Central.

The current-JDK toolchain is the default. To select another installed JDK / По умолчанию используется текущий JDK; выбор другого установленного JDK:

```bash
./gradlew build -PtestJavaVersion=17
```

The standalone service consumer uses only the packaged core / Отдельный пример сервиса использует только JAR:

```bash
mkdir -p build/service-consumer
javac --release 17 -cp build/libs/relational-paths-0.3.0.jar -d build/service-consumer docs/0.3.0/examples/OrderReport.java
java -cp build/service-consumer:build/libs/relational-paths-0.3.0.jar example.OrderReport
```

It asserts route and parameter isolation, then prints the plan and bindings. / Пример проверяет маршрут и изоляцию параметров, затем печатает план и параметры.

OSS-013 local verification used Java 17: core build/tests/Javadoc, the example, and a separate service compiled against the Maven Local artifact. The companion starter has a separate Maven consumer exercising an actual JDBC query and MVC controller. No registry publication or new JVM-wide matrix is claimed. / Проверка OSS-013 выполнена на Java 17: сборка/тесты/Javadoc, пример и отдельный сервис с Maven Local артефактом. У стартера есть отдельный Maven consumer с реальным JDBC-запросом и MVC-контроллером. Публикация и повторная полная матрица JVM не заявляются.
