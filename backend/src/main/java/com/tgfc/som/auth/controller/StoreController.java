package com.tgfc.som.auth.controller;

import com.tgfc.som.auth.dto.ApiResponse;
import com.tgfc.som.auth.dto.MastStoreResponse;
import com.tgfc.som.auth.dto.StoreResponse;
import com.tgfc.som.auth.dto.StoreSelectionRequest;
import com.tgfc.som.auth.service.StoreService;
import com.tgfc.som.common.util.JwtUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店別 Controller
 * 處理店別選擇相關 API
 */
@RestController
@RequestMapping("/stores")
public class StoreController {

    private static final Logger logger = LoggerFactory.getLogger(StoreController.class);

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    /**
     * GET /stores/mast
     * 取得使用者主店別
     *
     * @param jwt Keycloak JWT Token
     * @return 主店別 (可能為 null 表示全區)
     */
    @GetMapping("/mast")
    public ApiResponse<MastStoreResponse> getMastStore(@AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.extractUsername(jwt);
        logger.info("Getting mast store for user: {}", userId);

        MastStoreResponse response = storeService.getMastStore(userId);
        return ApiResponse.success(response);
    }

    /**
     * GET /stores/support
     * 取得使用者支援店別清單
     *
     * @param jwt Keycloak JWT Token
     * @return 支援店別清單
     */
    @GetMapping("/support")
    public ApiResponse<List<StoreResponse>> getSupportStores(@AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.extractUsername(jwt);
        logger.info("Getting support stores for user: {}", userId);

        List<StoreResponse> stores = storeService.getSupportStores(userId);
        return ApiResponse.success(stores);
    }

    /**
     * POST /stores/select
     * 記錄店別選擇
     *
     * @param jwt     Keycloak JWT Token
     * @param request 店別選擇請求
     * @return 操作結果
     */
    @PostMapping("/select")
    public ApiResponse<Void> selectStore(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StoreSelectionRequest request) {
        String userId = JwtUtils.extractUsername(jwt);
        logger.info("User {} selecting store: mast={}, support={}",
            userId, request.mastStoreId(), request.supportStoreIds());

        storeService.recordStoreSelection(userId, request);
        return ApiResponse.success("店別選擇成功", null);
    }
}
