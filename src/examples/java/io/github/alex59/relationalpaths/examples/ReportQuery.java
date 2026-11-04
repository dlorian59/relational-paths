package io.github.alex59.relationalpaths.examples;

import io.github.alex59.relationalpaths.*;
import java.util.List;
import java.util.Set;

// Build an order report without opening a database; run with ./gradlew example.
// Строим отчёт по заказам без подключения к БД; запуск: ./gradlew example.
public final class ReportQuery {
    private ReportQuery() { }

    public static void main(String[] args) {
        // Declare approved columns and both parts of the tenant-aware customer key.
        // Описываем разрешённые колонки и обе части ключа клиента с учётом арендатора.
        Schema schema = new Schema(List.of(
                new Table("orders", "sales.orders", Set.of("id", "customer_id", "tenant_id")),
                new Table("customer", "sales.customer", Set.of("id", "tenant_id", "name"))
        ), List.of(new Relationship("order_customer", "orders", "customer", 1,
                List.of(new ColumnPair("customer_id", "id"), new ColumnPair("tenant_id", "tenant_id")))));

        // Request the customer role, then render a parameterized filter from that plan.
        // Запрашиваем роль клиента и строим параметризованный фильтр по этому плану.
        JoinPlan plan = new JoinPlanner(schema).plan("orders", List.of("customer"));
        BoundSql query = SqlRenderer.select(plan,
                List.of(new ColumnRef("orders", "id"), new ColumnRef("customer", "name")),
                List.of(new Filter(new ColumnRef("customer", "name"), Filter.Operator.EQ, "Alice")));

        // SQL and bindings stay separate; the application controls authorization and JDBC execution.
        // SQL и параметры остаются раздельными; доступом и выполнением JDBC управляет приложение.
        System.out.println(plan.explain());
        System.out.println(query.sql());
        System.out.println("Bindings: " + query.bindings());
    }
}
