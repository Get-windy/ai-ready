package cn.aiedge.erp.batchsn.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 序列号状态枚举
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Getter
@AllArgsConstructor
public enum SnStatusEnum {
    
    /**
     * 可用
     */
    AVAILABLE("AVAILABLE", "可用"),
    
    /**
     * 使用中
     */
    IN_USE("IN_USE", "使用中"),
    
    /**
     * 服务中
     */
    INSERVICE("INSERVICE", "服务中"),
    
    /**
     * 维修中
     */
    MAINTAINED("MAINTAINED", "维修中"),
    
    /**
     * 报废
     */
    SCRAP("SCRAP", "报废");
    
    private final String code;
    private final String description;
    
    /**
     * 根据状态码获取枚举
     */
    public static SnStatusEnum fromCode(String code) {
        for (SnStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SN status code: " + code);
    }
}
