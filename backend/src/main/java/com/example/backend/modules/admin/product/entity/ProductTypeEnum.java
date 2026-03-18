package com.example.backend.modules.admin.product.entity;

/**
 * Product Type Enum for the Product Master entity.
 * Display names are kept in French labels used by the UI.
 */
public enum ProductTypeEnum {
    MATIERE_PREMIERE("matiere premiere"),
    SEMI_FINI("semi-fini"),
    PRODUIT_FINI("produit fini");

    private final String displayName;

    ProductTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
