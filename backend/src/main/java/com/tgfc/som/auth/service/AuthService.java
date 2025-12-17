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

    public AuthService(UserMapper userMapper, UserDomainService userDomainService) {
        this.userMapper = userMapper;
        this.userDomainService = userDomainService;
    }

    /**
     * 驗證使用者
     *
     * @param empId 員工ID (從 JWT 取得)
     * @return UserValidationResponse 驗證結果
     */
    public UserValidationResponse validateUser(String empId) {
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

            return UserValidationResponse.success(
                user.getEmpName(),
                systemFlags
            );
        } else {
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
     */
    public void recordLogout(String empId) {
        logger.info("Recording logout event for user: {}", empId);
        // TODO: 可擴展至 AuditLogService 記錄到資料庫
    }
}
