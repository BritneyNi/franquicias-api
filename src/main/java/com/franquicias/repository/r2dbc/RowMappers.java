package com.franquicias.repository.r2dbc;

import com.franquicias.domain.Branch;
import com.franquicias.domain.Franchise;
import com.franquicias.domain.Product;
import com.franquicias.domain.TopStockProduct;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import java.util.function.BiFunction;

/** Pure row mappers shared by the R2DBC adapters. */
final class RowMappers {

    static final BiFunction<Row, RowMetadata, Franchise> FRANCHISE =
            (row, metadata) -> new Franchise(row.get("id", String.class), row.get("name", String.class));

    static final BiFunction<Row, RowMetadata, Branch> BRANCH = (row, metadata) -> new Branch(
            row.get("id", String.class),
            row.get("franchise_id", String.class),
            row.get("name", String.class));

    static final BiFunction<Row, RowMetadata, Product> PRODUCT = (row, metadata) -> new Product(
            row.get("id", String.class),
            row.get("branch_id", String.class),
            row.get("name", String.class),
            row.get("stock", Integer.class));

    static final BiFunction<Row, RowMetadata, TopStockProduct> TOP_STOCK = (row, metadata) -> new TopStockProduct(
            row.get("branch_id", String.class),
            row.get("branch_name", String.class),
            row.get("product_id", String.class),
            row.get("product_name", String.class),
            row.get("stock", Integer.class));

    private RowMappers() {
    }
}
