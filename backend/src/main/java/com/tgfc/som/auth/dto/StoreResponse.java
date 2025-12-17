package com.tgfc.som.auth.dto;

/**
 * 店別回應 (Constitution X: Java Record)
 *
 * @param storeId   店別ID
 * @param storeName 店別名稱
 */
public record StoreResponse(
    String storeId,
    String storeName
) {
}
