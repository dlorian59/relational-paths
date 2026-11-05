package io.github.alex59.relationalpaths;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Shareable query facade that derives join targets from selected and filtered columns.
 * Delegates planning and SQL validation to JoinPlanner and SqlRenderer. Never executes SQL.
 */
public final class RelationalQueries {
    private final JoinPlanner planner;

    /** Uses the standard bounded planner.
     * @param schema approved application metadata
     */
    public RelationalQueries(Schema schema) { this(new JoinPlanner(schema)); }

    /** Uses an existing planner, preserving its search limits.
     * @param planner immutable planner
     */
    public RelationalQueries(JoinPlanner planner) { this.planner = Objects.requireNonNull(planner, "planner"); }

    /** Starts an immutable request/template rooted at a logical role.
     * @param root logical role
     * @return empty request; select at least one column before building
     */
    public Select from(String root) { return new Select(Names.identifier(root), List.of(), List.of(), Map.of()); }

    /** Immutable query template. Methods return new instances; keep their results.
     * Reuse a fixed projection/route template and add per-request values without sharing mutation.
     */
    public final class Select {
        private final String root;
        private final List<ColumnRef> projection;
        private final List<Filter> filters;
        private final Map<String, List<String>> routes;
        private Select(String root, List<ColumnRef> projection, List<Filter> filters, Map<String, List<String>> routes) {
            this.root = root; this.projection = List.copyOf(projection); this.filters = List.copyOf(filters);
            this.routes = Map.copyOf(routes);
        }

        /** Appends ordered projections using strict logical-role.column notation, not SQL expressions.
         * @param columns approved logical columns
         * @return new request
         */
        public Select select(String... columns) {
            var next = new ArrayList<>(projection);
            for (String column : columns) next.add(column(column));
            return new Select(root, next, filters, routes);
        }

        /** Adds an equality predicate; null uses IS NULL.
         * @param column logical-role.column
         * @param value bound value, never SQL text
         * @return new request
         */
        public Select whereEqual(String column, Object value) { return where(column, Filter.Operator.EQ, value); }

        /** Adds an AND-connected predicate using the core comparison operators.
         * @param column logical-role.column
         * @param operator comparison operator
         * @param value bound value; null allowed only for EQ/NE
         * @return new request
         */
        public Select where(String column, Filter.Operator operator, Object value) {
            var next = new ArrayList<>(filters);
            next.add(new Filter(column(column), operator, value));
            return new Select(root, projection, next, routes);
        }

        /** Chooses an explicit route for a role used in projection or filters.
         * Duplicate route assignments fail rather than silently replace business policy.
         * @param target target role
         * @param relationshipIds ordered edges from the root
         * @return new request
         */
        public Select via(String target, String... relationshipIds) {
            Names.identifier(target);
            var next = new LinkedHashMap<>(routes);
            if (next.putIfAbsent(target, List.of(relationshipIds)) != null)
                throw new IllegalArgumentException("Route already selected for " + target);
            return new Select(root, projection, filters, next);
        }

        /** Derives required roles and invokes the existing bounded planner and renderer.
         * @return explainable, parameterized query
         * @throws PlanningException for ambiguous, disconnected, conflicting or over-limit paths
         * @throws IllegalArgumentException for missing projections or undeclared/invalid columns
         */
        public PlannedQuery build() {
            if (projection.isEmpty()) throw new IllegalArgumentException("At least one projection is required");
            var targets = new TreeSet<String>();
            projection.forEach(column -> targets.add(column.table()));
            filters.forEach(filter -> targets.add(filter.column().table()));
            var plan = planner.plan(root, targets, routes);
            return new PlannedQuery(plan, SqlRenderer.select(plan, projection, filters));
        }
    }

    private static ColumnRef column(String value) {
        Objects.requireNonNull(value, "column");
        int dot = value.indexOf('.');
        if (dot < 1 || dot != value.lastIndexOf('.') || dot == value.length() - 1)
            throw new IllegalArgumentException("Expected logical-role.column: " + value);
        return new ColumnRef(value.substring(0, dot), value.substring(dot + 1));
    }
}
