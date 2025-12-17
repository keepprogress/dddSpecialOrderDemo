package com.tgfc.som.auth.domain;

import com.tgfc.som.entity.User;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 使用者領域服務
 * 負責 6-checkpoint 驗證邏輯
 */
@Service
public class UserDomainService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 執行 6-checkpoint 驗證
     *
     * @param user 使用者實體 (如果為 null 則視為 Checkpoint 1 失敗)
     * @return ValidationResult 驗證結果
     */
    public ValidationResult validateUser(User user) {
        // Checkpoint 1: 使用者存在
        if (user == null) {
            return ValidationResult.error(
                "USER_NOT_FOUND",
                "使用者不存在於系統中，請聯繫管理員"
            );
        }

        // Checkpoint 2: SYSTEM_FLAG 不為 null
        if (user.getSystemFlag() == null || user.getSystemFlag().isEmpty()) {
            return ValidationResult.error(
                "SYSTEM_FLAG_NULL",
                "使用者未被授權任何系統別，請聯繫管理員"
            );
        }

        // Checkpoint 3: 未被停用
        if ("Y".equals(user.getDisabledFlag())) {
            return ValidationResult.error(
                "USER_DISABLED",
                "帳號已被停用，請聯繫管理員"
            );
        }

        // Checkpoint 4: 啟用日期與停用日期已設定
        if (user.getStartDate() == null || user.getEndDate() == null) {
            return ValidationResult.error(
                "DATES_NOT_SET",
                "帳號啟用/停用日期未設定，請聯繫管理員"
            );
        }

        // Checkpoint 5: 在有效使用期間內
        Date today = new Date();
        if (today.before(user.getStartDate())) {
            return ValidationResult.error(
                "NOT_YET_ENABLED",
                "帳號尚未啟用，啟用日期：" + DATE_FORMAT.format(user.getStartDate())
            );
        }
        if (today.after(user.getEndDate())) {
            return ValidationResult.error(
                "ALREADY_EXPIRED",
                "帳號已過期，過期日期：" + DATE_FORMAT.format(user.getEndDate())
            );
        }

        // Checkpoint 6: 擁有功能權限 (在此服務中僅驗證 SYSTEM_FLAG 有值)
        // 實際的功能權限檢查在各功能模組中進行

        return ValidationResult.ok(user.getSystemFlag());
    }

    /**
     * 解析 SYSTEM_FLAG 為系統別陣列
     *
     * @param systemFlag 逗號分隔的系統別字串 (如: "SO,TTS,APP")
     * @return 系統別陣列
     */
    public String[] parseSystemFlags(String systemFlag) {
        if (systemFlag == null || systemFlag.isEmpty()) {
            return new String[0];
        }
        return systemFlag.split(",");
    }

    /**
     * 檢查使用者是否擁有指定系統別權限
     *
     * @param user     使用者實體
     * @param channelId 系統別 ID
     * @return 是否擁有權限
     */
    public boolean hasChannelPermission(User user, String channelId) {
        if (user == null || user.getSystemFlag() == null || channelId == null) {
            return false;
        }
        String[] flags = parseSystemFlags(user.getSystemFlag());
        for (String flag : flags) {
            if (flag.trim().equals(channelId)) {
                return true;
            }
        }
        return false;
    }
}
