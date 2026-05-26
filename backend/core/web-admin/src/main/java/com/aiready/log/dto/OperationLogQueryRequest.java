package com.aiready.log.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志查询请求
 */
@Data
public class OperationLogQueryRequest {
    
    /**
     * 当前页
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作类型列表
     */
    private List<String> operationTypes;
    
    /**
     * 操作人名称
     */
    private String operatorName;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作状态（0：失败 1：成功）
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 操作IP
     */
    private String operatorIp;
    
    /**
     * 关键字（模糊匹配操作描述）
     */
    private String keyword;
}
