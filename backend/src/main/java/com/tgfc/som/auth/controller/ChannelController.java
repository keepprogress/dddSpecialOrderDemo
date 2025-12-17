package com.tgfc.som.auth.controller;

import com.tgfc.som.auth.dto.ApiResponse;
import com.tgfc.som.auth.dto.ChannelResponse;
import com.tgfc.som.auth.dto.ChannelSelectionRequest;
import com.tgfc.som.auth.service.ChannelService;
import com.tgfc.som.common.util.JwtUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通路/系統別 Controller
 * 處理系統別選擇相關 API
 */
@RestController
@RequestMapping("/channels")
public class ChannelController {

    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    /**
     * GET /channels
     * 取得使用者可用的系統別清單
     *
     * @param jwt Keycloak JWT Token
     * @return 系統別清單
     */
    @GetMapping
    public ApiResponse<List<ChannelResponse>> getAvailableChannels(@AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.extractUsername(jwt);
        logger.info("Getting available channels for user: {}", userId);

        List<ChannelResponse> channels = channelService.getAvailableChannels(userId);
        return ApiResponse.success(channels);
    }

    /**
     * POST /channels/select
     * 選擇系統別
     *
     * @param jwt     Keycloak JWT Token
     * @param request 系統別選擇請求
     * @return 操作結果
     */
    @PostMapping("/select")
    public ApiResponse<Void> selectChannel(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChannelSelectionRequest request) {
        String userId = JwtUtils.extractUsername(jwt);
        logger.info("User {} selecting channel: {}", userId, request.channelId());

        channelService.recordChannelSelection(userId, request.channelId());
        return ApiResponse.success("系統別選擇成功", null);
    }
}
