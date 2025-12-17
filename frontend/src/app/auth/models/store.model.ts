/**
 * 主店別回應
 */
export interface MastStoreResponse {
  storeId: string | null; // null = 全區
  storeName: string;
}

/**
 * 支援店別回應
 */
export interface SupportStoreResponse {
  storeId: string;
  storeName: string;
}

/**
 * 店別選擇請求
 */
export interface StoreSelectionRequest {
  mastStoreId: string | null;
  supportStoreIds: string[];
}

/**
 * 店別列表回應
 */
export interface StoreListResponse {
  mastStore: MastStoreResponse | null;
  supportStores: SupportStoreResponse[];
}
