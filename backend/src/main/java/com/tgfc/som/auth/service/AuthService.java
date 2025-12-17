package com.tgfc.som.auth.service;

import com.tgfc.som.auth.domain.UserDomainService;
import com.tgfc.som.auth.domain.ValidationResult;
import com.tgfc.som.auth.dto.UserValidationResponse;
import com.tgfc.som.entity.User;
import com.tgfc.som.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 認證服務
 * 協調驗證流程
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final UserDomainService userDomainService;
    private final AuditLogService auditLogService;

    public AuthService(UserMapper userMapper, UserDomainService userDomainService, AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.userDomainService = userDomainService;
        this.auditLogService = auditLogService;
    }

    /**
     * 驗證使用者
     *
     * @param empId 員工ID (從 JWT 取得)
     * @param ipAddress 客戶端 IP
     * @param userAgent User-Agent
     * @return UserValidationResponse 驗證結果
     */
    public UserValidationResponse validateUser(String empId, String ipAddress, String userAgent) {
        logger.debug("Starting user validation for: {}", empId);

        // 從資料庫查詢使用者 (UAT 主鍵為 EMP_ID)
        User user = userMapper.selectByPrimaryKey(empId);

        // 執行 6-checkpoint 驗證
        ValidationResult result = userDomainService.validateUser(user);

        if (result.success()) {
            // 解析系統權限
            List<String> systemFlags = Arrays.asList(
                userDomainService.parseSystemFlags(result.systemFlag())
            );

            // 記錄驗證成功
            auditLogService.logValidateSuccess(empId, user.getEmpName(), ipAddress, userAgent);

            return UserValidationResponse.success(
                user.getEmpName(),
                systemFlags
            );
        } else {
            // 記錄驗證失敗
            auditLogService.logValidateFail(empId, ipAddress, userAgent, result.errorCode(), result.errorMessage());

            return UserValidationResponse.fail(
                result.errorCode(),
                result.errorMessage()
            );
        }
    }

    /**
     * 記錄登出事件
     *
     * @param empId 員工ID
     * @param ipAddress 客戶端 IP
     * @param userAgent User-Agent
     */
    public void recordLogout(String empId, String ipAddress, String userAgent) {
        logger.info("Recording logout event for user: {}", empId);
        auditLogService.logLogout(empId, null, ipAddress, userAgent);
    }
}
