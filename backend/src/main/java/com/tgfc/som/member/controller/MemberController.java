package com.tgfc.som.member.controller;

import com.tgfc.som.member.dto.MemberResponse;
import com.tgfc.som.member.dto.TempMemberRequest;
import com.tgfc.som.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 會員控制器
 *
 * 提供會員查詢與臨時卡建立功能
 */
@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "Member", description = "會員管理 API")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "查詢會員", description = "根據會員卡號查詢會員資料")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功取得會員資料"),
        @ApiResponse(responseCode = "404", description = "會員不存在")
    })
    public ResponseEntity<MemberResponse> getMember(
            @PathVariable
            @Parameter(description = "會員卡號", required = true)
            String memberId
    ) {
        log.info("查詢會員請求: memberId={}", memberId);

        return memberService.getMember(memberId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/temp")
    @Operation(summary = "建立臨時卡", description = "建立臨時卡會員，供無會員卡客戶使用")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "臨時卡建立成功"),
        @ApiResponse(responseCode = "400", description = "請求參數錯誤")
    })
    public ResponseEntity<MemberResponse> createTempMember(
            @Valid @RequestBody TempMemberRequest request
    ) {
        log.info("建立臨時卡請求: name={}, cellPhone={}", request.name(), request.cellPhone());

        MemberResponse response = memberService.createTempMember(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
