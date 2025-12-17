package com.tgfc.som.auth.dto;

import java.util.List;

/**
 * 店別選擇請求 (Constitution X: Java Record)
 *
 * @param mastStoreId     主店別ID (null 表示全區)
 * @param supportStoreIds 支援店別ID清單
 */
public record StoreSelectionRequest(
    String mastStoreId,
    List<String> supportStoreIds
) {
}
