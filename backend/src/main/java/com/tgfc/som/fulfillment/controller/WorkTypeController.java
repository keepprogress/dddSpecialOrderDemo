package com.tgfc.som.fulfillment.controller;

import com.tgfc.som.fulfillment.domain.WorkCategory;
import com.tgfc.som.fulfillment.dto.WorkType;
import com.tgfc.som.fulfillment.dto.WorkTypeResponse;
import com.tgfc.som.fulfillment.service.WorkTypeService;
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

import java.util.List;

/**
 * 工種控制器
 *
 * 提供工種查詢 API
 */
@RestController
@RequestMapping("/api/v1/work-types")
@Tag(name = "WorkType", description = "工種管理 API")
public class WorkTypeController {

    private static final Logger log = LoggerFactory.getLogger(WorkTypeController.class);

    private final WorkTypeService workTypeService;

    public WorkTypeController(WorkTypeService workTypeService) {
        this.workTypeService = workTypeService;
    }

    @GetMapping
    @Operation(summary = "取得所有工種", description = "取得系統中所有可用的工種清單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得工種清單")
    })
    public ResponseEntity<List<WorkTypeResponse>> getAllWorkTypes() {
        log.info("取得所有工種請求");

        List<WorkTypeResponse> responses = workTypeService.getAllWorkTypes().stream()
                .map(WorkTypeResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{workTypeId}")
    @Operation(summary = "取得工種詳情", description = "根據工種代碼取得工種詳細資訊")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得工種詳情"),
        @ApiResponse(responseCode = "404", description = "工種不存在")
    })
    public ResponseEntity<WorkTypeResponse> getWorkType(
            @PathVariable
            @Parameter(description = "工種代碼", required = true)
            String workTypeId
    ) {
        log.info("取得工種詳情請求: workTypeId={}", workTypeId);

        return workTypeService.getWorkType(workTypeId)
                .map(WorkTypeResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "根據類別取得工種", description = "取得指定類別的工種清單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得工種清單"),
        @ApiResponse(responseCode = "400", description = "無效的類別代碼")
    })
    public ResponseEntity<List<WorkTypeResponse>> getWorkTypesByCategory(
            @PathVariable
            @Parameter(description = "工種類別 (INSTALLATION, DELIVERY, HOME_DELIVERY, PURE_DELIVERY)", required = true)
            String category
    ) {
        log.info("根據類別取得工種請求: category={}", category);

        try {
            WorkCategory workCategory = WorkCategory.valueOf(category.toUpperCase());
            List<WorkTypeResponse> responses = workTypeService.getWorkTypesByCategory(workCategory).stream()
                    .map(WorkTypeResponse::from)
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            log.warn("無效的類別代碼: {}", category);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/installation")
    @Operation(summary = "取得安裝工種", description = "取得所有安裝類工種清單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得安裝工種清單")
    })
    public ResponseEntity<List<WorkTypeResponse>> getInstallationWorkTypes() {
        log.info("取得安裝工種請求");

        List<WorkTypeResponse> responses = workTypeService.getInstallationWorkTypes().stream()
                .map(WorkTypeResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/delivery")
    @Operation(summary = "取得運送工種", description = "取得所有運送類工種清單（含純運）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得運送工種清單")
    })
    public ResponseEntity<List<WorkTypeResponse>> getDeliveryWorkTypes() {
        log.info("取得運送工種請求");

        List<WorkTypeResponse> responses = workTypeService.getDeliveryWorkTypes().stream()
                .map(WorkTypeResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/home-delivery")
    @Operation(summary = "取得宅配工種", description = "取得所有宅配類工種清單")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得宅配工種清單")
    })
    public ResponseEntity<List<WorkTypeResponse>> getHomeDeliveryWorkTypes() {
        log.info("取得宅配工種請求");

        List<WorkTypeResponse> responses = workTypeService.getHomeDeliveryWorkTypes().stream()
                .map(WorkTypeResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/recommend")
    @Operation(summary = "推薦工種", description = "根據商品類別推薦適合的工種")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功推薦工種")
    })
    public ResponseEntity<WorkTypeResponse> recommendWorkType(
            @RequestParam
            @Parameter(description = "商品類別 (AC, APPLIANCE, FURNITURE, 3C)", required = true)
            String productCategory
    ) {
        log.info("推薦工種請求: productCategory={}", productCategory);

        WorkType workType = workTypeService.recommendWorkType(productCategory);

        return ResponseEntity.ok(WorkTypeResponse.from(workType));
    }
}
