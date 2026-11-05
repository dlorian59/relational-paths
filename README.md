# relational-paths

<details name="readme-language">
<summary><picture><source media="(prefers-color-scheme: dark)" srcset="assets/lang-en-dark.svg"><img src="assets/lang-en-light.svg" width="42" height="26" alt="EN"></picture></summary>

**0.3.0 · Java 17+ · MIT** · [EN/RU / Versions](docs/index.md) · [Java 17/21/25 + optional 27](docs/0.3.0/java.en.md)

Build report queries from the tables and relationships your application already knows. Describe approved columns once; select business fields and filters to get SQL with separate parameters and an explanation of every JOIN.

Useful for report APIs and query editors where different fields require different joins. When two routes have the same cost, the library asks for an explicit choice instead of returning the wrong business result.

## Get started in your project

Add the dependency to an existing Java 17+ project. These coordinates are not published to Maven Central yet; they resolve only when the artifact is supplied through your configured repository. Local artifact preparation is in CONTRIBUTING.

Maven:
```xml
<dependency>
  <groupId>io.github.59alex</groupId>
  <artifactId>relational-paths</artifactId>
  <version>0.3.0</version>
</dependency>
```

Gradle Kotlin:
```kotlin
dependencies { implementation("io.github.59alex:relational-paths:0.3.0") }
```

Declare the metadata at application startup. The following fragments belong inside a method with imports from io.github.alex59.relationalpaths; the complete class is linked below.

```java
var schema = Schema.builder()
    .table("orders", "sales.orders", "id", "customer_id", "tenant_id")
    .table("customer", "sales.customer", "id", "name", "tenant_id")
    .relationship("customer", "orders", "customer",
        new ColumnPair("customer_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
    .build();
var queries = new RelationalQueries(schema);
```

Build a query using the authenticated tenant from your server context:

```java
var query = queries.from("orders").select("orders.id", "customer.name")
    .whereEqual("orders.tenant_id", authenticatedTenantId)
    .whereEqual("customer.name", "Alice").build();
```

Result: a composite customer JOIN, tenant and name bound as [42, "Alice"], and column aliases c0/c1 in selection order. Pass query.sql() and query.bindings() to your JDBC layer; the library does not open connections.

[Complete service source](docs/0.3.0/examples/OrderReport.java) · [Step-by-step walkthrough](docs/0.3.0/integration.en.md) · [Spring Boot](docs/0.3.0/spring.en.md)

## Capabilities

- **Join only the roles needed by fields and filters**
  ```java
  queries.from("orders").select("orders.id").whereEqual("customer.name", "Alice").build();
  ```
  The filter-only customer role is joined automatically. See the complete service above.
- **Inspect why a JOIN was selected**
  ```java
  query.explain();
  ```
  Returns relationship IDs and equality pairs, including tenant keys.
- **Choose the correct business route**
  ```java
  queries.from("orders").select("address.city").via("address", "shipping").build();
  ```
  For metadata with billing/shipping relationships, chooses shipping explicitly. The full address example is in the Spring guide.
- **Bound metadata traversal**
  ```java
  var bounded = new RelationalQueries(new JoinPlanner(schema, SearchLimits.DEFAULT));
  ```
  Defaults allow 512 roles, 8192 relationships and 8192 edge visits. Hitting a limit throws; no partial plan is returned.

## What remains in your application

You own authorization, tenant policy, database execution, row limits and transactions. SQL is INNER JOIN with declared columns and AND comparisons, not an ORM or database optimizer. To-many joins can multiply rows; values must never be interpolated into SQL. Query templates are immutable, while bound mutable objects remain caller-owned.

[Guide](docs/0.3.0/integration.en.md) · [API](docs/0.3.0/reference.en.md) · [CONTRIBUTING](CONTRIBUTING.md) · [MIT](LICENSE)

</details>

<details name="readme-language" open>
<summary><picture><source media="(prefers-color-scheme: dark)" srcset="assets/lang-ru-dark.svg"><img src="assets/lang-ru-light.svg" width="42" height="26" alt="RU"></picture></summary>

**0.3.0 · Java 17+ · MIT** · [EN/RU / Versions](docs/index.md) · [Java 17/21/25 + optional 27](docs/0.3.0/java.ru.md)

Стройте запросы отчётов по таблицам и связям, которые уже известны приложению. Один раз опишите разрешённые колонки; затем выбирайте бизнес-поля и фильтры, чтобы получить SQL с отдельными параметрами и объяснением каждого JOIN.

Подходит для API отчётов и редакторов запросов, где разным полям нужны разные соединения. Если два маршрута имеют одинаковую стоимость, библиотека требует явного выбора вместо неверного бизнес-результата.

## Подключение в свой проект

Добавьте зависимость в существующий проект Java 17+. Эти координаты пока не опубликованы в Maven Central: артефакт должен быть доступен через настроенный репозиторий. Подготовка локальных артефактов вынесена в CONTRIBUTING.

Maven:
```xml
<dependency>
  <groupId>io.github.59alex</groupId>
  <artifactId>relational-paths</artifactId>
  <version>0.3.0</version>
</dependency>
```

Gradle Kotlin:
```kotlin
dependencies { implementation("io.github.59alex:relational-paths:0.3.0") }
```

Объявите метаданные при запуске приложения. Фрагменты ниже находятся внутри метода с импортами из io.github.alex59.relationalpaths; полная версия класса связана ниже.

```java
var schema = Schema.builder()
    .table("orders", "sales.orders", "id", "customer_id", "tenant_id")
    .table("customer", "sales.customer", "id", "name", "tenant_id")
    .relationship("customer", "orders", "customer",
        new ColumnPair("customer_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
    .build();
var queries = new RelationalQueries(schema);
```

Создайте запрос, передав арендатора из проверенного серверного контекста:

```java
var query = queries.from("orders").select("orders.id", "customer.name")
    .whereEqual("orders.tenant_id", authenticatedTenantId)
    .whereEqual("customer.name", "Alice").build();
```

Результат: составной JOIN клиента, арендатор и имя в параметрах [42, "Alice"], алиасы c0/c1 в порядке выбора. Передайте query.sql() и query.bindings() вашему JDBC-слою; библиотека не открывает соединения.

[Полный исходник сервиса](docs/0.3.0/examples/OrderReport.java) · [Пошаговый разбор](docs/0.3.0/integration.ru.md) · [Spring Boot](docs/0.3.0/spring.ru.md)

## Возможности

- **Соединить роли, нужные полям и фильтрам**
  ```java
  queries.from("orders").select("orders.id").whereEqual("customer.name", "Alice").build();
  ```
  Роль customer, нужная только фильтру, добавляется автоматически. Полный сервис приведён выше.
- **Понять причину выбора JOIN**
  ```java
  query.explain();
  ```
  Возвращает ID связей и пары равенств, включая ключ арендатора.
- **Выбрать правильный бизнес-маршрут**
  ```java
  queries.from("orders").select("address.city").via("address", "shipping").build();
  ```
  Для схемы с платёжным адресом и доставкой явно выбирает shipping. Полный пример адресов — в руководстве Spring.
- **Ограничить обход метаданных**
  ```java
  var bounded = new RelationalQueries(new JoinPlanner(schema, SearchLimits.DEFAULT));
  ```
  По умолчанию: 512 ролей, 8192 связи и 8192 просмотренных ребра. Превышение вызывает ошибку, частичный план не возвращается.

## Что остаётся приложению

Вы управляете авторизацией, правилами арендатора, выполнением SQL, лимитом строк и транзакциями. SQL использует INNER JOIN, объявленные колонки и AND-сравнения; это не ORM и не оптимизатор БД. Связи один-ко-многим могут размножать строки; не вставляйте значения в SQL. Шаблоны запросов неизменяемы, но изменяемые объекты параметров остаются ответственностью вызывающего кода.

[Руководство](docs/0.3.0/integration.ru.md) · [API](docs/0.3.0/reference.ru.md) · [CONTRIBUTING](CONTRIBUTING.md) · [MIT](LICENSE)

</details>
