# relational-paths 0.3.0 — from metadata to a report

[README](../../README.md) · [Versions](../index.md) · [English](integration.en.md) · [Русский](integration.ru.md) · [API](reference.en.md)

## The object you describe

A Schema is an approved map of the business relationships your reports may traverse. A role names one occurrence of a table: billing and shipping can use the same physical table through different roles. Relationships are directed equality pairs, including all parts of a composite key. This makes the report policy explicit and reusable.

Use it when selected report fields vary or several services repeat join rules. For a single fixed SQL statement, handwritten parameterized SQL is often simpler. The library does not infer authorization, physical foreign keys or row cardinality.

## 1. Connect to an existing application

Use the Maven or Gradle dependency in the README. No runtime dependencies or Spring configuration are required by core. Java 17 is the bytecode baseline; 21/25 and optional 27 keep the same API, without preview features. Artifact availability is separate from source readiness; Maven Central publication is pending.

```text
src/main/java/example/OrderReport.java
```

The complete [OrderReport.java](examples/OrderReport.java) can be copied into your service package. Its main method is only a finite verification entry point; the constructor and query method are the reusable application code.

## 2. Declare metadata once

```java
var schema = Schema.builder()
    .table("orders", "sales.orders", "id", "customer_id", "tenant_id")
    .table("customer", "sales.customer", "id", "name", "tenant_id")
    .relationship("customer", "orders", "customer",
        new ColumnPair("customer_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
    .build();
var queries = new RelationalQueries(schema);
```

table takes logical role, physical table/schema.table and approved columns. relationship defaults to cost 1; use the overload with a positive int before ColumnPair values to express a semantic preference. Schema.builder is mutable and startup-local; build snapshots it. Existing constructors remain supported.

The tenant equality in the example is a deliberate application rule. Omitting it can join equal customer IDs from different tenants. Declaring a column is an allowlist decision, not proof that a particular user may read it.

## 3. Reuse a template and add request values

```java
var template = queries.from("orders").select("orders.id", "customer.name");
var query = template.whereEqual("orders.tenant_id", 42L)
    .whereEqual("customer.name", "Alice").build();
```

Select is immutable. Each select/where/via call returns a new object; retain the result. Fields and filter-only roles become planner targets automatically. The original template can be shared between requests without accumulating another tenant’s filters.

Column strings must contain exactly one dot: logicalRole.column. Physical schema.table names belong only in metadata. Expressions, sort directions and additional dots are rejected. where supports the existing EQ/NE/LT/LE/GT/GE operators; EQ/NE null render IS NULL/IS NOT NULL, without a binding.

## 4. Inspect, bind and execute

```java
query.explain();
query.sql();
query.bindings();
```

The sample produces the customer route and bindings [42, "Alice"]. JDBC receives the SQL and ordered values separately. Output aliases c0, c1 follow projection order. See the Spring report for a complete JdbcTemplate service/controller; core also works with a caller-owned PreparedStatement.

Planning is synchronous in-memory work with no background resources. There is nothing to close or cancel in the planner; SQL cancellation, statement timeouts, connection pooling and transaction consistency belong to the execution layer.

## 5. Resolve business alternatives

Two equal-cost paths throw AmbiguousPathException with two witness routes. Do not choose the first candidate in generic glue: choose a business relationship. With address metadata, use via("address", "shipping"). Its target must appear in projection or filters; repeated via for the same target is rejected. Longer explicit routes are allowed but still validated.

For simultaneous billing and shipping, describe two roles for the same table. Multi-target plans share prefixes, reject conflicting incoming paths, and do not minimize the overall join-tree cost. Positive weights express business preference, not database latency.

## 6. Bounds, failures and verification

SearchLimits.DEFAULT caps 512 roles, 8192 relationships and 8192 edge visits per automatic search. All reachable edges can be visited even for a small target. Bounds fail closed rather than truncate results. Use new RelationalQueries(new JoinPlanner(schema, customLimits)) to preserve application bounds.

| Symptom | Action |
|---|---|
| AmbiguousPathException | Select via or split repeated table roles; inspect candidates. |
| PlanningException | Check direction, connectivity, explicit routes and configured bounds. |
| IllegalArgumentException | Check declared columns, role.column syntax and nonempty projection. |
| Unexpected duplicate/missing rows | Inspect INNER JOIN cardinality and data; the planner does not deduplicate. |

The downloadable service was compiled and executed against the installed JAR. New regression tests cover inferred filter targets, immutable templates, ambiguity/route selection, caps, injection strings, null predicates and metadata snapshots. Verification commands and publication limits are in CONTRIBUTING.

## What to retain

Metadata is business policy; the facade removes repeated plumbing, not policy decisions. Keep projection/routes fixed in services, bind untrusted values, enforce authorization before query construction, and apply execution limits in the database layer.

[Advanced core guide](en.md) · [API](reference.en.md) · [Spring](spring.en.md) · [CONTRIBUTING](../../CONTRIBUTING.md) · [README](../../README.md)
