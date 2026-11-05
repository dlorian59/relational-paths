# 0.3.0 — build a Spring order report

## Existing application: service and controller

The starter now also injects RelationalQueries over the same planner. Use an immutable template to select fields and the shipping route; add tenant/name values per request. JOIN targets are derived from fields and filters. The existing low-level example below remains supported.

```java
var query = queries.from("orders")
    .select("orders.id", "customer.name", "address.city")
    .via("address", "shipping")
    .whereEqual("orders.tenant_id", authenticatedTenantId)
    .whereEqual("customer.name", customerName).build();
```

[Full Spring MVC/JDBC walkthrough](https://github.com/dlorian59/relational-paths-spring-boot-starter/blob/main/docs/0.1.0/integration.en.md) · [Source](https://github.com/dlorian59/relational-paths-spring-boot-starter/tree/main/examples/order-report).

[← Usage guide](en.md) · [Versions](../index.md) · [README](../../README.md) · [Русский](spring.ru.md)

[Complete API/configuration/error reference](reference.en.md)

## Situation

An order export needs the customer name and delivery city. The schema has two address
foreign keys, billing and shipping. The report must select shipping intentionally and
keep tenant keys in every relationship. The core models the allowed graph, explains the
chosen route and renders SQL with parameters; the host application executes it.

## Configure the application

The companion [relational-paths-spring-boot-starter](https://github.com/dlorian59/relational-paths-spring-boot-starter) 0.1.0 targets
Boot 4.1.2 and Java 17+. It installs an injectable `JoinPlanner` when the application
supplies one `Schema` candidate, or a primary candidate among multiple schemas.
No reflection or entity/database scanning is involved.

With both repositories checked out as sibling directories:

```sh
./gradlew publishToMavenLocal
cd ../relational-paths-spring-boot-starter
./gradlew build example publishToMavenLocal
```

These are local builds of the core and starter. In an existing Boot application:

```kotlin
repositories { mavenLocal(); mavenCentral() }
dependencies {
    implementation("io.github.59alex:relational-paths-spring-boot-starter:0.1.0")
}
```

Provide the approved metadata as a `Schema` bean, following
[ReportConfiguration.java](https://github.com/dlorian59/relational-paths-spring-boot-starter/blob/main/src/examples/java/example/reports/ReportConfiguration.java). It includes columns used
in projections, filters and composite joins. Set practical limits in application properties:

```properties
relational-paths.max-tables=32
relational-paths.max-relationships=64
relational-paths.max-edge-visits=64
```

A custom `JoinPlanner` or `SearchLimits` bean takes precedence; no schema means no
planner. Multiple schemas without `@Primary` do not cause an arbitrary choice.
`relational-paths.enabled=false` disables the starter. Invalid effective limits or an
oversized schema fail startup; exceeding edge visits fails the planning call.

## Annotated source and business choice

[OrderReport.java](https://github.com/dlorian59/relational-paths-spring-boot-starter/blob/main/src/examples/java/example/reports/OrderReport.java) receives the planner through its constructor,
selects `shipping_address` explicitly and retains the immutable plan for repeated requests.
Its query method filters `orders.tenant_id` and `customer.name`; the tenant value must come
from authenticated server context. Every example source comment is EN followed by RU.

```java
// Choose shipping when two address relationships have equal cost.
// Выбираем доставку, когда две связи с адресом имеют одинаковую стоимость.
var plan = planner.plan("orders", List.of("customer", "address"),
    Map.of("address", List.of("shipping_address")));
```

The complete [ReportApplication.java](https://github.com/dlorian59/relational-paths-spring-boot-starter/blob/main/src/examples/java/example/reports/ReportApplication.java) loads an actual Boot
context using normal starter discovery, proves the automatic address choice is ambiguous,
and requests the explicit report. No database is required.

## Outcome

Selected lines from `./gradlew example` in the starter:

```text
Address choices: [[billing_address], [shipping_address]]
Bindings: [42, Alice]
```

The printed SQL uses the shipping foreign key, composite tenant equality in both JOINs,
and placeholders for tenant and name. Replacing the name with `Alice' OR 1=1 --` leaves
the SQL structure intact; the value stays in the binding list. The starter tests check
that behavior, configuration overrides and ambiguity handling.

## Core capabilities without Spring

| Need | Runnable source | Guide |
| --- | --- | --- |
| A small customer report with a composite key | [ReportQuery](../../src/examples/java/io/github/alex59/relationalpaths/examples/ReportQuery.java), `./gradlew example` | [Metadata](en.md#describe-roles-and-relationships) |
| Cheaper paths and shared prefixes | `planningExample` in [Example.java](examples/Example.java) | [Planning](en.md#plan-weighted-routes-and-inspect-the-result) |
| Billing/shipping choice or two simultaneous roles | `ambiguityExample`, `rolesExample` in [Example.java](examples/Example.java) | [Ambiguity](en.md#choose-ambiguous-links-and-use-table-roles) |
| Parameter isolation and null semantics | `renderingExample`, `nullExample` in [Example.java](examples/Example.java) | [SQL](en.md#render-select-queries-with-bound-values) |
| Fail closed on limits and invalid columns | `boundariesExample` in [Example.java](examples/Example.java) | [Limits](en.md#limits-failures-and-guarantees) |

## Boundaries

`SqlRenderer` is a static core API and does not need a Spring bean. Neither library
executes SQL, manages transactions, introspects a schema or supplies row-level security.
Keep approved metadata server-side and enforce access before rendering/executing queries.
INNER JOIN can remove unmatched rows or multiply to-many rows. Search limits bound
metadata traversal, not database query duration. Recreate the planner after a schema
change; its metadata is immutable. This is a reusable application scenario, not evidence
of a production deployment or database execution.

[← Usage guide](en.md)
