package com.tgfc.som.auth.controller;

import com.tgfc.som.auth.dto.ApiResponse;
import com.tgfc.som.auth.dto.UserValidationResponse;
import com.tgfc.som.auth.service.AuthService;
import com.tgfc.som.common.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認證 Controller
 * 處理使用者驗證相關 API
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/validate
     * 驗證使用者 (6-checkpoint 驗證)
     *
     * @param jwt Keycloak JWT Token
     * @param request HTTP 請求 (用於取得 IP 和 User-Agent)
     * @return UserValidationResponse 驗證結果
     */
    @PostMapping("/validate")
    public ApiResponse<UserValidationResponse> validateUser(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        String username = JwtUtils.extractUsername(jwt);
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        logger.info("Validating user: {} from IP: {}", username, ipAddress);

        UserValidationResponse response = authService.validateUser(username, ipAddress, userAgent);

        if (response.success()) {
            logger.info("User {} validation successful", username);
            return ApiResponse.success("驗證成功", response);
        } else {
            logger.warn("User {} validation failed: {} - {}",
                username, response.errorCode(), response.errorMessage());
            return ApiResponse.fail(response.errorMessage());
        }
    }

    /**
     * POST /auth/logout
     * 登出審計事件 (記錄登出時間)
     *
     * @param jwt Keycloak JWT Token
     * @param request HTTP 請求 (用於取得 IP 和 User-Agent)
     * @return 登出結果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        String username = JwtUtils.extractUsername(jwt);
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        logger.info("User {} logout from IP: {}", username, ipAddress);

        authService.recordLogout(username, ipAddress, userAgent);

        return ApiResponse.success("登出成功", null);
    }

    /**
     * 取得客戶端 IP (支援 proxy)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
