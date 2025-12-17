package com.tgfc.som.auth.dto;

/**
 * 主店別回應 (Constitution X: Java Record)
 *
 * @param storeId   店別ID (null 表示全區)
 * @param storeName 店別名稱
 */
public record MastStoreResponse(
    String storeId,
    String storeName
) {
}
