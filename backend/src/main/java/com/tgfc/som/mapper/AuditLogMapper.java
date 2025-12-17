package com.tgfc.som.mapper;

import com.tgfc.som.entity.AuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 稽核日誌 Mapper
 */
@Mapper
public interface AuditLogMapper {

    @Insert("""
        INSERT INTO TBL_AUDIT_LOG (
            EMP_ID, EMP_NAME, ACTION_TYPE, ACTION_DETAIL,
            STORE_ID, CHANNEL_ID, IP_ADDRESS, USER_AGENT,
            RESULT, ERROR_MESSAGE, CREATE_DATE
        ) VALUES (
            #{empId}, #{empName}, #{actionType}, #{actionDetail},
            #{storeId}, #{channelId}, #{ipAddress}, #{userAgent},
            #{result}, #{errorMessage}, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "logId")
    int insert(AuditLog auditLog);
}
