package cn.aiedge.erp.batchsn.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 批次状态枚举
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Getter
@AllArgsConstructor
public enum BatchStatusEnum {
    
    /**
     * 活跃中
     */
    ACTIVE("ACTIVE", "活跃中"),
    
    /**
     * 已过期
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 隔离中
     */
    QUARANTINED("QUARANTINED", "隔离中"),
    
    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消");
    
    private final String code;
    private final String description;
    
    /**
     * 根据状态码获取枚举
     */
    public static BatchStatusEnum fromCode(String code) {
        for (BatchStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown batch status code: " + code);
    }
}
