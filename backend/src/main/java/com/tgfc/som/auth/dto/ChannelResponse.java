package com.tgfc.som.auth.dto;

/**
 * 通路/系統別回應 (Constitution X: Java Record)
 *
 * @param channelId   通路ID
 * @param channelName 通路名稱
 */
public record ChannelResponse(
    String channelId,
    String channelName
) {
}
