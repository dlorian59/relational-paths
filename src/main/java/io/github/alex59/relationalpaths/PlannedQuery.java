package io.github.alex59.relationalpaths;

import java.util.List;
import java.util.Objects;

/** An explainable plan and its parameterized SELECT, without a connection or execution lifecycle.
 * @param plan validated join plan
 * @param statement rendered SQL with separate bindings
 */
public record PlannedQuery(JoinPlan plan, BoundSql statement) {
    /** Requires a plan and rendered statement.
     * @param plan join plan
     * @param statement parameterized SQL
     */
    public PlannedQuery { Objects.requireNonNull(plan, "plan"); Objects.requireNonNull(statement, "statement"); }
    /** Returns the JDBC SQL, never interpolating values.
     * @return SQL with question-mark placeholders
     */
    public String sql() { return statement.sql(); }
    /** Returns ordered values; mutable values remain caller-owned.
     * @return positional bindings
     */
    public List<Object> bindings() { return statement.bindings(); }
    /** Explains the chosen relationships and equality predicates.
     * @return deterministic plan explanation
     */
    public String explain() { return plan.explain(); }
}
