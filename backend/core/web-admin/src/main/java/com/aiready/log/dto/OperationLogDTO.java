package com.aiready.log.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志DTO
 */
@Data
public class OperationLogDTO {
    
    private Long id;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作类型描述
     */
    private String operationTypeDesc;
    
    /**
     * 操作描述
     */
    private String operationDesc;
    
    /**
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
    
    /**
     * 操作人IP
     */
    private String operatorIp;
    
    /**
     * 操作地点
     */
    private String operatorLocation;
    
    /**
     * 操作状态（0：失败 1：成功）
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 执行时长（毫秒）
     */
    private Long executionTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
