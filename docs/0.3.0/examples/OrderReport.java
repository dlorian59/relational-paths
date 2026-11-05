package example;

import io.github.alex59.relationalpaths.*;

public final class OrderReport {
    private final RelationalQueries.Select report;

    public OrderReport() {
        // Declare only approved roles/columns; tenant equality is part of the business key.
        // Объявляйте разрешённые роли/колонки; равенство арендатора входит в бизнес-ключ.
        var schema = Schema.builder()
            .table("orders", "sales.orders", "id", "customer_id", "tenant_id")
            .table("customer", "sales.customer", "id", "name", "tenant_id")
            .relationship("customer", "orders", "customer",
                new ColumnPair("customer_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
            .build();
        report = new RelationalQueries(schema).from("orders").select("orders.id", "customer.name");
    }

    public PlannedQuery query(long authenticatedTenantId, String name) {
        // Per-request filters return a new query; the shared template stays unchanged.
        // Фильтры запроса создают новый объект; общий шаблон не изменяется.
        return report.whereEqual("orders.tenant_id", authenticatedTenantId)
            .whereEqual("customer.name", name).build();
    }

    public static void main(String[] args) {
        var report = new OrderReport();
        var query = report.query(42L, "Alice' OR 1=1 --");
        if (query.sql().contains("Alice")) throw new AssertionError("A value entered SQL");
        if (!query.bindings().equals(java.util.List.of(42L, "Alice' OR 1=1 --"))) throw new AssertionError("Bindings lost");
        if (!query.plan().routes().get("customer").equals(java.util.List.of("customer"))) throw new AssertionError("Route lost");
        System.out.println(query.explain());
        System.out.println(query.sql());
        System.out.println("Bindings: " + query.bindings());
    }
}
