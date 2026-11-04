# 0.3.0 — отчёт по заказам в Spring

[← Руководство](ru.md) · [Версии](../index.md) · [README](../../README.md) · [English](spring.en.md)

## Ситуация

Для выгрузки заказов нужны имя клиента и город доставки. В схеме есть две ссылки
на адрес: платёжный и доставки. Отчёт должен осознанно выбрать доставку и сохранить
ключ арендатора в каждой связи. Основная библиотека описывает разрешённый граф,
объясняет маршрут и формирует SQL с параметрами; выполняет запрос приложение.

## Настройка приложения

Отдельный [relational-paths-spring-boot-starter](https://github.com/59Alex/relational-paths-spring-boot-starter) версии 0.1.0 рассчитан
на Boot 4.1.2 и Java 17+. Он создаёт внедряемый `JoinPlanner`, когда приложение
предоставляет один bean `Schema` либо основной кандидат среди нескольких схем.
Рефлексии и сканирования сущностей или таблиц нет.

Если оба репозитория расположены рядом:

```sh
./gradlew publishToMavenLocal
cd ../relational-paths-spring-boot-starter
./gradlew build example publishToMavenLocal
```

Это локальные сборки основной библиотеки и стартера. В существующем Boot-приложении:

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    implementation("io.github.59alex:relational-paths-spring-boot-starter:0.1.0")
}
```

Опишите разрешённые метаданные как bean `Schema` по примеру
[ReportConfiguration.java](https://github.com/59Alex/relational-paths-spring-boot-starter/blob/main/src/examples/java/example/reports/ReportConfiguration.java). В нём объявлены колонки
для проекций, фильтров и составных JOIN. Задайте лимиты в application properties:

```properties
relational-paths.max-tables=32
relational-paths.max-relationships=64
relational-paths.max-edge-visits=64
```

Собственный `JoinPlanner` или `SearchLimits` имеет приоритет. Без схемы планировщик
не создаётся; несколько схем без `@Primary` не приводят к произвольному выбору.
`relational-paths.enabled=false` отключает стартер. Некорректные действующие лимиты
или слишком большая схема останавливают запуск; превышение лимита рёбер завершает
вызов планирования ошибкой.

## Комментированный исходник и бизнес-выбор

[OrderReport.java](https://github.com/59Alex/relational-paths-spring-boot-starter/blob/main/src/examples/java/example/reports/OrderReport.java) принимает планировщик через конструктор,
явно выбирает `shipping_address` и сохраняет неизменяемый план для повторных запросов.
Метод запроса фильтрует `orders.tenant_id` и `customer.name`; ID арендатора нужно получить
из проверенного серверного контекста. Комментарии в примерах идут парами: EN, затем RU.

```java
// Choose shipping when two address relationships have equal cost.
// Выбираем доставку, когда две связи с адресом имеют одинаковую стоимость.
var plan = planner.plan("orders", List.of("customer", "address"),
    Map.of("address", List.of("shipping_address")));
```

Полный [ReportApplication.java](https://github.com/59Alex/relational-paths-spring-boot-starter/blob/main/src/examples/java/example/reports/ReportApplication.java) создаёт настоящий
контекст Boot со штатным обнаружением стартера, проверяет неоднозначность автоматического
выбора адреса и получает отчёт с явным маршрутом. База данных не нужна.

## Результат

Часть вывода `./gradlew example` из каталога стартера:

```text
Address choices: [[billing_address], [shipping_address]]
Bindings: [42, Alice]
```

SQL использует ключ адреса доставки, равенство арендатора в обоих JOIN и параметры
для арендатора и имени. Замена имени на `Alice' OR 1=1 --` не меняет структуру SQL:
значение остаётся в списке параметров. Проверки стартера контролируют это поведение,
переопределение конфигурации и обработку неоднозначности.

## Возможности основной библиотеки без Spring

| Задача | Запускаемый исходник | Руководство |
| --- | --- | --- |
| Небольшой отчёт по клиентам с составным ключом | [ReportQuery](../../src/examples/java/io/github/alex59/relationalpaths/examples/ReportQuery.java), `./gradlew example` | [Метаданные](ru.md#описание-ролей-и-связей) |
| Более дешёвые пути и общие участки | `planningExample` в [Example.java](examples/Example.java) | [Планирование](ru.md#планирование-маршрутов-и-объяснение-результата) |
| Выбор платёжного адреса/доставки или две роли одновременно | `ambiguityExample`, `rolesExample` в [Example.java](examples/Example.java) | [Неоднозначность](ru.md#выбор-неоднозначных-связей-и-роли-таблиц) |
| Отделение параметров и семантика null | `renderingExample`, `nullExample` в [Example.java](examples/Example.java) | [SQL](ru.md#формирование-select-с-параметрами) |
| Явная ошибка при превышении лимитов или неверной колонке | `boundariesExample` в [Example.java](examples/Example.java) | [Лимиты](ru.md#лимиты-ошибки-и-гарантии) |

## Границы применения

`SqlRenderer` — статический API, которому не нужен Spring bean. Библиотеки не выполняют
SQL, не управляют транзакциями, не читают схему БД и не обеспечивают защиту строк.
Храните разрешённые метаданные на сервере и проверяйте права до построения и выполнения
запроса. `INNER JOIN` может исключать строки и размножать их при связях один-ко-многим.
Лимиты ограничивают обход метаданных, а не время SQL. При изменении схемы создавайте
новый планировщик: метаданные неизменяемы. Это переиспользуемый сценарий приложения,
без заявления о production-внедрении или выполнении запросов к базе.

[← Руководство](ru.md)
