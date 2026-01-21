// src/main/java/com/rdc/admin/entity/AssetType.java
package com.rdc.admin.entity;

/**
 * Local copy of AssetType for the Admin Service.
 * Matches the File Nature logic used in the Asset Service vault.
 */
public enum AssetType {
    IMAGE,      // gallery images
    VIDEO,      // product videos
    GIF,        // animated previews
    MOCKUP,     // lifestyle mockups
    TIFF        // original paid file
}