package com.tgfc.som.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康檢查 Controller
 */
@RestController
public class HealthController {

    /**
     * GET /health
     * 健康檢查端點 (公開)
     *
     * @return 健康狀態
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString(),
            "application", "ddd-special-order-demo"
        );
    }
}
