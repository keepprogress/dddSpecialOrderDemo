/**
 * 使用者驗證回應
 */
export interface UserValidationResponse {
  success: boolean;
  userName: string;
  systemFlags: string[];
  errorCode?: string;
  errorMessage?: string;
}

/**
 * 驗證錯誤回應
 */
export interface ValidationErrorResponse {
  errorCode: string;
  errorMessage: string;
}

/**
 * 登入上下文 (存儲於 LocalStorage)
 */
export interface LoginContext {
  userId: string;
  userName: string;
  systemFlags: string[];
  selectedStore: StoreInfo | null;
  supportStores: StoreInfo[];
  selectedChannel: ChannelInfo | null;
  loginTime: string;
}

/**
 * 店別資訊
 */
export interface StoreInfo {
  storeId: string | null; // null = 全區
  storeName: string;
}

/**
 * 通路/系統別資訊
 */
export interface ChannelInfo {
  channelId: string;
  channelName: string;
  channelDesc?: string;
}
