package com.tgfc.som.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 建立臨時卡請求
 *
 * @param name 姓名
 * @param cellPhone 手機號碼
 * @param address 地址
 * @param zipCode 郵遞區號
 */
public record TempMemberRequest(
    @NotBlank(message = "姓名不可為空")
    @Size(max = 50, message = "姓名長度不可超過 50 字")
    String name,

    @NotBlank(message = "手機號碼不可為空")
    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式錯誤，須為 09 開頭共 10 碼")
    String cellPhone,

    @NotBlank(message = "地址不可為空")
    @Size(max = 200, message = "地址長度不可超過 200 字")
    String address,

    @NotBlank(message = "郵遞區號不可為空")
    @Pattern(regexp = "^\\d{3,5}$", message = "郵遞區號格式錯誤，須為 3-5 碼數字")
    String zipCode
) {}
