package com.rdc.admin.entity;

public enum CategoryScope {
    DESIGN,
    FABRIC,
    BOTH;

    public boolean supportsDesign() {
        return this == DESIGN || this == BOTH;
    }

    public boolean supportsFabric() {
        return this == FABRIC || this == BOTH;
    }
}
