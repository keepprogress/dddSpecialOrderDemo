package com.tgfc.som.auth.service;

import com.tgfc.som.entity.AuditLog;
import com.tgfc.som.mapper.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 稽核日誌服務
 * 記錄登入/登出/選擇等事件
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * Action Types
     */
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_VALIDATE = "VALIDATE";
    public static final String ACTION_SELECT_STORE = "SELECT_STORE";
    public static final String ACTION_SELECT_CHANNEL = "SELECT_CHANNEL";

    /**
     * Results
     */
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAIL = "FAIL";

    /**
     * 記錄登入成功
     */
    public void logLoginSuccess(String empId, String empName, String ipAddress, String userAgent) {
        log(empId, empName, ACTION_LOGIN, "使用者登入成功", null, null, ipAddress, userAgent, RESULT_SUCCESS, null);
    }

    /**
     * 記錄登入失敗
     */
    public void logLoginFail(String empId, String ipAddress, String userAgent, String errorMessage) {
        log(empId, null, ACTION_LOGIN, "使用者登入失敗", null, null, ipAddress, userAgent, RESULT_FAIL, errorMessage);
    }

    /**
     * 記錄驗證成功
     */
    public void logValidateSuccess(String empId, String empName, String ipAddress, String userAgent) {
        log(empId, empName, ACTION_VALIDATE, "6-checkpoint 驗證成功", null, null, ipAddress, userAgent, RESULT_SUCCESS, null);
    }

    /**
     * 記錄驗證失敗
     */
    public void logValidateFail(String empId, String ipAddress, String userAgent, String errorCode, String errorMessage) {
        String detail = String.format("6-checkpoint 驗證失敗: %s", errorCode);
        log(empId, null, ACTION_VALIDATE, detail, null, null, ipAddress, userAgent, RESULT_FAIL, errorMessage);
    }

    /**
     * 記錄登出
     */
    public void logLogout(String empId, String empName, String ipAddress, String userAgent) {
        log(empId, empName, ACTION_LOGOUT, "使用者登出", null, null, ipAddress, userAgent, RESULT_SUCCESS, null);
    }

    /**
     * 記錄選擇店別
     */
    public void logSelectStore(String empId, String empName, String storeId, String channelId, String ipAddress, String userAgent) {
        String detail = String.format("選擇店別: %s", storeId);
        log(empId, empName, ACTION_SELECT_STORE, detail, storeId, channelId, ipAddress, userAgent, RESULT_SUCCESS, null);
    }

    /**
     * 記錄選擇系統別
     */
    public void logSelectChannel(String empId, String empName, String channelId, String ipAddress, String userAgent) {
        String detail = String.format("選擇系統別: %s", channelId);
        log(empId, empName, ACTION_SELECT_CHANNEL, detail, null, channelId, ipAddress, userAgent, RESULT_SUCCESS, null);
    }

    /**
     * 通用日誌記錄方法
     */
    private void log(String empId, String empName, String actionType, String actionDetail,
                     String storeId, String channelId, String ipAddress, String userAgent,
                     String result, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setEmpId(empId);
            auditLog.setEmpName(empName);
            auditLog.setActionType(actionType);
            auditLog.setActionDetail(actionDetail);
            auditLog.setStoreId(storeId);
            auditLog.setChannelId(channelId);
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(userAgent);
            auditLog.setResult(result);
            auditLog.setErrorMessage(errorMessage);

            auditLogMapper.insert(auditLog);
            logger.debug("Audit log recorded: {} - {} - {}", empId, actionType, result);
        } catch (Exception e) {
            // 稽核日誌失敗不應影響主流程
            logger.error("Failed to record audit log: {} - {} - {}", empId, actionType, result, e);
        }
    }
}
