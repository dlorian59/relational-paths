package io.github.alex59.relationalpaths;

import java.util.*;

/** Immutable runtime metadata with validated relationship endpoints and columns. */
public final class Schema {
    private final Map<String, Table> tables;
    private final Map<String, Relationship> relationships;
    private final Map<String, List<Relationship>> outgoing;

    /** Starts a metadata builder; only application-approved columns should be declared.
     * @return a new mutable, non-thread-safe builder
     */
    public static Builder builder() { return new Builder(); }

    /** Convenience assembly; build delegates all validation to the existing metadata types. */
    public static final class Builder {
        private final List<Table> tables = new ArrayList<>();
        private final List<Relationship> relationships = new ArrayList<>();
        private Builder() { }

        /** Adds a logical role and its column allowlist.
         * @param role logical role
         * @param physicalName physical table or schema.table
         * @param columns approved columns
         * @return this builder
         */
        public Builder table(String role, String physicalName, String... columns) {
            tables.add(new Table(role, physicalName, Set.of(columns)));
            return this;
        }

        /** Adds a directed relationship with unit semantic cost.
         * @param id relationship ID
         * @param from source role
         * @param to destination role
         * @param columns equality pairs, including tenant keys where required
         * @return this builder
         */
        public Builder relationship(String id, String from, String to, ColumnPair... columns) {
            return relationship(id, from, to, 1, columns);
        }

        /** Adds a directed relationship with explicit semantic cost.
         * @param id relationship ID
         * @param from source role
         * @param to destination role
         * @param cost positive preference weight
         * @param columns equality pairs
         * @return this builder
         */
        public Builder relationship(String id, String from, String to, int cost, ColumnPair... columns) {
            relationships.add(new Relationship(id, from, to, cost, List.of(columns)));
            return this;
        }

        /** Validates and snapshots this metadata. Later additions do not change earlier schemas.
         * @return immutable metadata
         */
        public Schema build() { return new Schema(tables, relationships); }
    }

    /**
     * Creates a schema. IDs must be unique and every referenced column must be declared.
     * @param tables logical table roles
     * @param relationships directed join edges
     */
    public Schema(Collection<Table> tables, Collection<Relationship> relationships) {
        TreeMap<String, Table> ts = new TreeMap<>();
        for (Table table : tables) {
            Objects.requireNonNull(table, "table");
            if (ts.putIfAbsent(table.id(), table) != null) throw new IllegalArgumentException("Duplicate table: " + table.id());
        }
        if (ts.isEmpty()) throw new IllegalArgumentException("Schema needs a table");
        TreeMap<String, Relationship> rs = new TreeMap<>();
        TreeMap<String, List<Relationship>> edges = new TreeMap<>();
        for (String table : ts.keySet()) edges.put(table, new ArrayList<>());
        for (Relationship relation : relationships) {
            Objects.requireNonNull(relation, "relationship");
            if (rs.putIfAbsent(relation.id(), relation) != null) throw new IllegalArgumentException("Duplicate relationship: " + relation.id());
            Table from = ts.get(relation.from()), to = ts.get(relation.to());
            if (from == null || to == null) throw new IllegalArgumentException("Unknown endpoint: " + relation.id());
            for (ColumnPair pair : relation.columns()) {
                if (!from.columns().contains(pair.fromColumn()) || !to.columns().contains(pair.toColumn())) {
                    throw new IllegalArgumentException("Unknown column in relationship: " + relation.id());
                }
            }
            edges.get(relation.from()).add(relation);
        }
        edges.replaceAll((id, list) -> list.stream().sorted(Comparator.comparing(Relationship::id)).toList());
        this.tables = Collections.unmodifiableMap(ts);
        this.relationships = Collections.unmodifiableMap(rs);
        this.outgoing = Collections.unmodifiableMap(edges);
    }

    /** Returns the described immutable value.
     * @return tables in lexicographic role order
     */
    public Map<String, Table> tables() { return tables; }
    /** Returns the described immutable value.
     * @return relationships in lexicographic ID order
     */
    public Map<String, Relationship> relationships() { return relationships; }
    /** Creates or resolves the described metadata value.
     * @param id logical role
     * @return matching table
     * @throws IllegalArgumentException if unknown
     */
    public Table table(String id) {
        Table value = tables.get(Objects.requireNonNull(id, "table id"));
        if (value == null) throw new IllegalArgumentException("Unknown table: " + id);
        return value;
    }
    /** Creates or resolves the described metadata value.
     * @param id relationship ID
     * @return matching edge
     * @throws IllegalArgumentException if unknown
     */
    public Relationship relationship(String id) {
        Relationship value = relationships.get(Objects.requireNonNull(id, "relationship id"));
        if (value == null) throw new IllegalArgumentException("Unknown relationship: " + id);
        return value;
    }
    List<Relationship> outgoing(String id) { return outgoing.get(id); }
}
