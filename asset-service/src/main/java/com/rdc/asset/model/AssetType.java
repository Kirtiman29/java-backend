package com.rdc.asset.model;

public enum AssetType {
    IMAGE,
    VIDEO,
    GIF,
    MOCKUP,
    TIFF // Add this back so Hibernate doesn't crash on old data
}