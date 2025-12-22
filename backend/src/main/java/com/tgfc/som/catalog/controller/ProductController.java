package com.tgfc.som.catalog.controller;

import com.tgfc.som.catalog.dto.EligibilityResponse;
import com.tgfc.som.catalog.service.ProductEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品控制器
 *
 * 提供商品銷售資格驗證功能
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "商品管理 API")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductEligibilityService productEligibilityService;

    public ProductController(ProductEligibilityService productEligibilityService) {
        this.productEligibilityService = productEligibilityService;
    }

    @GetMapping("/{skuNo}/eligibility")
    @Operation(summary = "檢查商品銷售資格", description = "執行 6-Layer 商品銷售資格驗證")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "驗證結果（包含通過或失敗資訊）")
    })
    public ResponseEntity<EligibilityResponse> checkEligibility(
            @PathVariable
            @Parameter(description = "商品編號", required = true)
            String skuNo,
            @RequestParam(required = false)
            @Parameter(description = "通路代號")
            String channelId,
            @RequestParam(required = false)
            @Parameter(description = "店別代號")
            String storeId
    ) {
        log.info("檢查商品銷售資格請求: skuNo={}, channelId={}, storeId={}",
                skuNo, channelId, storeId);

        EligibilityResponse response = productEligibilityService.checkEligibility(
                skuNo, channelId, storeId);

        return ResponseEntity.ok(response);
    }
}
