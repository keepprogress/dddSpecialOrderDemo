package com.tgfc.som.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 系統別選擇請求 (Constitution X: Java Record)
 *
 * @param channelId 系統別ID
 */
public record ChannelSelectionRequest(
    @NotBlank(message = "系統別ID不可為空")
    String channelId
) {
}
