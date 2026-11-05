package io.github.alex59.relationalpaths;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RelationalQueriesTest {
    private Schema schema() {
        return Schema.builder()
            .table("orders", "sales.orders", "id", "customer_id", "shipping_id", "billing_id", "tenant_id")
            .table("customer", "sales.customer", "id", "name", "tenant_id")
            .table("address", "sales.address", "id", "city", "tenant_id")
            .relationship("customer", "orders", "customer", new ColumnPair("customer_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
            .relationship("shipping", "orders", "address", new ColumnPair("shipping_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
            .relationship("billing", "orders", "address", new ColumnPair("billing_id", "id"), new ColumnPair("tenant_id", "tenant_id"))
            .build();
    }

    @Test void infersFilterOnlyTargetsAndKeepsValuesOutOfSqlWithoutMutatingTemplate() {
        var template = new RelationalQueries(schema()).from("orders").select("orders.id");
        var query = template.whereEqual("customer.name", "Alice' OR 1=1 --").whereEqual("orders.tenant_id", 42L).build();
        assertEquals(List.of("customer"), query.plan().routes().get("customer"));
        assertEquals(List.of("Alice' OR 1=1 --", 42L), query.bindings());
        assertFalse(query.sql().contains("Alice"));
        assertTrue(query.explain().contains("orders.tenant_id = customer.tenant_id"));
        assertEquals(List.of(), template.build().bindings());
        assertTrue(template.build().plan().joins().isEmpty());
    }

    @Test void ambiguityExplicitRouteAndLimitsStillUseTheSamePlanner() {
        var query = new RelationalQueries(schema()).from("orders").select("address.city");
        assertThrows(AmbiguousPathException.class, query::build);
        var shipping = query.via("address", "shipping");
        assertEquals(List.of("shipping"), shipping.build().plan().routes().get("address"));
        assertThrows(IllegalArgumentException.class, () -> shipping.via("address", "billing"));
        assertThrows(PlanningException.class, () -> new RelationalQueries(new JoinPlanner(schema(), new SearchLimits(3, 3, 0)))
            .from("orders").select("customer.name").build());
        assertThrows(PlanningException.class, () -> new RelationalQueries(schema()).from("customer").select("orders.id").build());
        assertThrows(PlanningException.class, () -> new RelationalQueries(schema()).from("orders")
            .select("orders.id").via("address", "shipping").build());
    }

    @Test void rejectsUndeclaredColumnsSqlFragmentsAndEmptyProjection() {
        var queries = new RelationalQueries(schema());
        for (String invalid : List.of("orders.id DESC", "sales.orders.id", "id", "orders.", ".id"))
            assertThrows(IllegalArgumentException.class, () -> queries.from("orders").select(invalid));
        assertThrows(IllegalArgumentException.class, () -> queries.from("orders").select("orders.secret").build());
        assertThrows(IllegalArgumentException.class, () -> queries.from("orders").build());
        var nullQuery = queries.from("orders").select("orders.id").whereEqual("customer.name", null).build();
        assertTrue(nullQuery.sql().endsWith("IS NULL"));
        assertTrue(nullQuery.bindings().isEmpty());
    }

    @Test void builderSnapshotsAndReusesMetadataValidation() {
        var builder = Schema.builder().table("a", "a", "id");
        var first = builder.build();
        builder.table("b", "b", "id").relationship("ab", "a", "b", 2, new ColumnPair("id", "id"));
        assertEquals(1, first.tables().size());
        assertEquals(2, builder.build().relationship("ab").cost());
        assertThrows(IllegalArgumentException.class, () -> Schema.builder().table("a", "a", "id")
            .relationship("bad", "a", "missing", new ColumnPair("id", "id")).build());
    }
}
