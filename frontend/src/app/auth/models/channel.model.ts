/**
 * 通路/系統別回應
 */
export interface ChannelResponse {
  channelId: string;
  channelName: string;
  channelDesc: string;
}

/**
 * 系統別選擇請求
 */
export interface ChannelSelectionRequest {
  channelId: string;
}

/**
 * 可用系統別列表回應
 */
export interface AvailableChannelsResponse {
  channels: ChannelResponse[];
}
