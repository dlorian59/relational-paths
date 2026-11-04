# relational-paths 0.3.0 — complete API reference

[← Usage guide](en.md) · [Versions](../index.md) · [README](../../README.md) · [Русский](reference.ru.md)

Every public type, accessor family and operation is listed below. The [complete runnable example](examples/Example.java) supplies `schema`, `planner`, plans and columns; the [Spring report](spring.en.md) adds constructor injection and configuration. Code expressions in the matrix are method-body usage, not standalone programs.

## Public API

| API and source | Example | Contract and edge cases |
| --- | --- | --- |
| [Table(id, name, columns)](../../src/main/java/io/github/alex59/relationalpaths/Table.java) | `new Table("orders", "sales.orders", Set.of("id"))` | Logical ID and columns use ASCII identifiers; physical name is table or schema.table. Nonempty allowlist is copied, sorted and unmodifiable. Multiple roles may share a physical name. |
| [ColumnPair(fromColumn, toColumn)](../../src/main/java/io/github/alex59/relationalpaths/ColumnPair.java) | `new ColumnPair("customer_id", "id")` | One equality pair. Relationship direction determines which table each name belongs to; endpoint/column existence is checked by Schema. |
| [Relationship(id, from, to, cost, columns)](../../src/main/java/io/github/alex59/relationalpaths/Relationship.java) | `new Relationship("order_customer", "orders", "customer", 1, pairs)` | Positive int cost; nonempty copied list with no duplicate pairs. Same logical from/to is rejected. Reverse traversal must be declared explicitly. |
| [Schema(tables, relationships)](../../src/main/java/io/github/alex59/relationalpaths/Schema.java) | `var schema = new Schema(tables, relationships)` | At least one table; unique IDs; every relationship endpoint and column must exist. Empty relationships are allowed. Collections are snapshotted. |
| [Schema.tables/relationships/table/relationship](../../src/main/java/io/github/alex59/relationalpaths/Schema.java) | `schema.table("orders")`; `schema.relationship("order_customer")` | Maps are unmodifiable and lexicographically ordered by ID. Unknown lookup ID throws IllegalArgumentException; null ID throws NullPointerException. No outgoing-edge method is public. |
| [SearchLimits and DEFAULT](../../src/main/java/io/github/alex59/relationalpaths/SearchLimits.java) | `new SearchLimits(32, 64, 64)` | Bounds for metadata size and one automatic search, not rows, SQL time or requested-target count. See defaults below. |
| [JoinPlanner(schema) / (schema, limits)](../../src/main/java/io/github/alex59/relationalpaths/JoinPlanner.java) | `var planner = new JoinPlanner(schema, limits)` | Default constructor uses SearchLimits.DEFAULT; both constructors reject oversized schemas immediately. Planner metadata is immutable. |
| [JoinPlanner.plan(root, targets)](../../src/main/java/io/github/alex59/relationalpaths/JoinPlanner.java) | `planner.plan("orders", List.of("customer", "region"))` | Minimum sum of positive semantic costs per target; equal cost is ambiguous even with different hop counts. Duplicate targets collapse; empty targets produce a root-only plan. |
| [JoinPlanner.plan(root, targets, explicitRoutes)](../../src/main/java/io/github/alex59/relationalpaths/JoinPlanner.java) | `planner.plan("orders", List.of("address"), Map.of("address", List.of("shipping_ref")))` | Overrides only requested targets. Route starts at root, follows connected edges, ends at target, never repeats a role. It may cost more than the automatic shortest path. |
| [JoinPlan.schema/root/joins](../../src/main/java/io/github/alex59/relationalpaths/JoinPlan.java) | `plan.schema()`; `plan.root()`; `plan.joins()` | Only the planner creates a validated plan. Joins are dependency-ordered; each destination role occurs once. Schema and result collections are immutable. |
| [JoinPlan.aliases/routes/explain](../../src/main/java/io/github/alex59/relationalpaths/JoinPlan.java) | `plan.aliases()`; `plan.routes()`; `plan.explain()` | Aliases start with root t0 in join order. Routes are sorted by requested target and contain relationship IDs; intermediate roles need not be map keys. Explain is diagnostic text, not SQL or a machine interchange format. |
| [ColumnRef(table, column)](../../src/main/java/io/github/alex59/relationalpaths/ColumnRef.java) | `new ColumnRef("customer", "name")` | Uses logical role IDs, not physical table names or aliases. Syntax is validated on construction; existence and plan membership at rendering time. |
| [Filter(column, operator, value) and Operator](../../src/main/java/io/github/alex59/relationalpaths/Filter.java) | `new Filter(new ColumnRef("customer", "name"), Filter.Operator.EQ, "Alice")` | Non-null column/operator; nullable value only for EQ and NE. Values retain their original reference/type. All filters are AND-combined; all six operators are listed below. |
| [SqlRenderer.select(plan, projection, filters)](../../src/main/java/io/github/alex59/relationalpaths/SqlRenderer.java) | `SqlRenderer.select(plan, projection, filters)` | Nonempty ordered projection; ordered filters may be empty. Every column must be declared and joined. Renders complete INNER JOIN plan, double-quoted identifiers, aliases c0/c1/… and JDBC ? placeholders. |
| [BoundSql(sql, bindings), sql(), bindings()](../../src/main/java/io/github/alex59/relationalpaths/BoundSql.java) | `query.sql()`; `query.bindings()` | Copies the list, not bound objects. SQL/list/elements must be non-null. Manual construction accepts arbitrary SQL and does not validate placeholder count or identifier safety; guarantees of SqlRenderer do not apply to hand-written SQL. |
| [PlanningException(message)](../../src/main/java/io/github/alex59/relationalpaths/PlanningException.java) | `catch (PlanningException failure)` | Extends IllegalArgumentException. A planning failure requires a change to metadata, targets, explicit choices or bounds; retrying the same input does not resolve ambiguity. |
| [AmbiguousPathException.target/candidates](../../src/main/java/io/github/alex59/relationalpaths/AmbiguousPathException.java) | `failure.target()`; `failure.candidates()` | Extends PlanningException. Two deterministic witness ID routes, not an exhaustive list; nested lists are immutable. Catch before the broader exception when offering route selection. |

All public records expose their named components and standard Java `equals/hashCode/toString`. For example, `Relationship.id/from/to/cost/columns`, `Table.id/name/columns`, `ColumnPair.fromColumn/toColumn`, `ColumnRef.table/column`, `Filter.column/operator/value` and `SearchLimits.maxTables/maxRelationships/maxEdgeVisits` are read-only accessors. They do not perform planning or execute SQL.

## Search configuration

| Setting | Default | Boundary and timing |
| --- | --- | --- |
| maxTables | 512 | Integer ≥ 1; all schema roles count, including unreachable ones. Checked when JoinPlanner is constructed. |
| maxRelationships | 8192 | Integer ≥ 0; all schema edges count. Checked at planner construction. |
| maxEdgeVisits | 8192 | Integer ≥ 0; examined outgoing edges in one automatic search. Unrelated reachable branches can consume the budget. |

A root-only plan or a request whose non-root targets all have explicit routes does not run automatic search, so it can work with maxEdgeVisits=0. Schema-size limits still apply. Explicit routes are always validated. There is no configuration file in the core; [the starter](spring.en.md) maps properties to this same SearchLimits type.

## Operators, projections and bindings

| Operator | Non-null value | null value |
| --- | --- | --- |
| EQ | `= ?` | `IS NULL`, no binding |
| NE | `<> ?` | `IS NOT NULL`, no binding |
| LT | `< ?` | rejected |
| LE | `<= ?` | rejected |
| GT | `> ?` | rejected |
| GE | `>= ?` | rejected |

Filters are AND-combined in input order; bindings contain only non-null filter values in placeholder order. JDBC uses one-based parameter indices. Empty filters omit WHERE; empty projection is invalid. Duplicate projected columns are allowed and get distinct cN aliases. The renderer performs no type coercion, LIKE/IN/OR expression parsing or database-driver binding. [Rendering/null examples](examples/Example.java) and [SQL contract tests](../../src/test/java/io/github/alex59/relationalpaths/SqlRendererTest.java) show the flow.

## Planning edge cases and lifecycle

| Case | Result / responsibility |
| --- | --- |
| Empty targets | Root alias t0, no joins and no route entries. Request the root explicitly to get its empty route list. |
| Duplicate targets / reordered metadata | Same deterministic plan; projection/filter order is intentionally retained. |
| Shared paths | Compatible prefixes are reused. This is not a globally minimum-cost join-tree/Steiner optimizer. |
| Positive-cost cycles | Automatic shortest-path search terminates without enumerating every route. Explicit routes cannot repeat a role. |
| More than two equal paths | Two witnesses are sufficient to reject ambiguity; candidates() is not a route catalogue. |
| Ownership | Metadata/plan collections are snapshots. Bound objects remain caller-owned references; use stable JDBC-compatible values. No background tasks, cache, connection or close hook. |
| Concurrent use | Immutable schema/planner/plan may be shared. Do not concurrently mutate call inputs or bound objects. Rebuild a planner after metadata changes. |
| Limits of safety | Allowed identifiers/columns do not authorize a user. Keep schema metadata trusted and enforce access, tenant filters and database permissions in the application. |
| SQL semantics | INNER JOIN may exclude rows or multiply to-many rows. No automatic cardinality inference, deduplication, outer join, grouping, pagination or transaction management. |

For a custom SQL dialect, consume the immutable JoinPlan directly. The built-in renderer uses ANSI double quotes and JDBC ?, and does not execute a live database query. See [architecture and bounds](../architecture.md) for trade-offs.

## Errors and troubleshooting

| Error/symptom | Cause | Action |
| --- | --- | --- |
| `Invalid identifier`, `Expected table or schema.table` | Punctuation, whitespace, leading digits, quoted/raw fragments or too many qualification parts. | Use declared ASCII identifiers matching `[A-Za-z_][A-Za-z0-9_]*`; quote generation belongs to the renderer. |
| `Duplicate table/relationship`, `Unknown endpoint/column` | Metadata is inconsistent before planning. | Correct IDs and include every equality column in both table allowlists. |
| `Self-joins need distinct logical table roles` | A relationship points from a role to itself. | Give each occurrence its own role ID while retaining the same physical name. |
| `Unknown table/relationship` | Unknown root/target or explicit relationship ID. | Use IDs from Schema, not physical names. |
| `No directed path from …` | The declared graph does not reach a requested target. | Check direction; add an approved reverse relationship only when semantically valid. |
| `Ambiguous path to …` | At least two routes have the same minimum total cost. | Inspect target()/candidates(), choose an explicit relationship-ID route, or separate roles. Do not auto-pick the first witness. |
| `Explicit route supplied for an unrequested target`, `Disconnected`, `repeats role`, `ends at`, `too long` | The supplied route is not a simple connected path from the root to a requested target. | Repair the whole route; maximum simple path length is number of schema roles minus one. |
| `Conflicting paths enter role …` | Requested routes disagree about the incoming edge of a shared role. | Choose consistent explicit routes or use separate roles; the planner does not silently rewrite the business choice. |
| `Invalid search bounds`, `Schema exceeds configured table/relationship limits`, `Search exceeded maxEdgeVisits=…` | Invalid limits, oversized metadata or exhausted automatic traversal. | Validate configuration; reduce approved metadata or deliberately raise the correct bound. No partial plan is returned. |
| `At least one projection is required`, `Undeclared column`, `Table is absent from plan` | Empty SELECT or a column outside the allowlist/join tree. | Declare the column and include its logical role as a target; keep projections explicit. |
| `Null supports only EQ and NE` | An ordered comparison received null. | Use EQ/NE for null checks, otherwise pass a typed non-null JDBC value. |
| `NullPointerException` / `UnsupportedOperationException` | Missing required structure / attempted mutation of an immutable container. | Fix null arguments; make an application-owned copy for edits. Filter null values are the documented exception. |

PlanningException is an IllegalArgumentException; AmbiguousPathException is more specific. Catch ambiguity first when translating errors into an application response. Message fragments above aid diagnosis; application logic should use exception types and structured target()/candidates() instead of parsing explain() or error text.

## Spring and AOP scope

The [starter integration](spring.en.md) supplies plain beans; it does not wrap planning in a proxy, transaction or retry. JoinPlanner is final and SqlRenderer.select is static, so intercept an application service boundary if you need an independent auditing/timing policy. Class-based Spring proxies cannot subclass final classes, and self-invocation bypasses advice; see [Spring proxy rules](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html). An aspect must not silently resolve ambiguous routes or change authorization decisions.

## Verification and version navigation

Run `./gradlew test example` on an installed JDK 17+; the [full plain-Java example](examples/Example.java) has [compile/run instructions](en.md#run-the-complete-example-and-troubleshoot). Existing [planner tests](../../src/test/java/io/github/alex59/relationalpaths/JoinPlannerTest.java) include a small-graph exhaustive oracle and determinism checks. [Java/version matrix](java.en.md) separates artifact baseline from runtime versions; source and bytecode stay at 17.

[← Usage guide](en.md)
