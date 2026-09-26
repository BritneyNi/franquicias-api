package com.franquicias.api.dto;

import com.franquicias.domain.Branch;
import com.franquicias.domain.Franchise;
import com.franquicias.domain.FranchiseDetail;
import com.franquicias.domain.Product;
import com.franquicias.domain.TopStockProduct;

import java.util.List;

/** Response payloads: immutable records mapped from the domain with pure {@code from} factories. */
public final class ApiMapper {

    private ApiMapper() {
    }

    public record FranchiseResponse(String id, String nombre) {
        public static FranchiseResponse from(Franchise franchise) {
            return new FranchiseResponse(franchise.id(), franchise.name());
        }
    }

    public record BranchResponse(String id, String idFranquicia, String nombre) {
        public static BranchResponse from(Branch branch) {
            return new BranchResponse(branch.id(), branch.franchiseId(), branch.name());
        }
    }

    public record ProductResponse(String id, String idSucursal, String nombre, int stock) {
        public static ProductResponse from(Product product) {
            return new ProductResponse(product.id(), product.branchId(), product.name(), product.stock());
        }
    }

    /**
     * Winner of a branch. Includes the branch it belongs to, as required by the acceptance
     * criteria of the "product with the most stock" endpoint.
     */
    public record TopStockProductResponse(
            String idSucursal,
            String sucursal,
            String idProducto,
            String producto,
            int stock
    ) {
        public static TopStockProductResponse from(TopStockProduct top) {
            return new TopStockProductResponse(
                    top.branchId(), top.branchName(), top.productId(), top.productName(), top.stock());
        }
    }

    public record ProductDetailResponse(String id, String nombre, List<ProductResponse> productos) {
        public static ProductDetailResponse from(FranchiseDetail.BranchWithProducts branch) {
            return new ProductDetailResponse(
                    branch.branch().id(),
                    branch.branch().name(),
                    branch.products().stream().map(ProductResponse::from).toList());
        }
    }

    public record FranchiseDetailResponse(
            String id,
            String nombre,
            List<ProductDetailResponse> sucursales
    ) {
        public static FranchiseDetailResponse from(FranchiseDetail detail) {
            return new FranchiseDetailResponse(
                    detail.franchise().id(),
                    detail.franchise().name(),
                    detail.branches().stream().map(ProductDetailResponse::from).toList());
        }
    }
}
