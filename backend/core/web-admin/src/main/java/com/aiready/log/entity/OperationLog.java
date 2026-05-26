package com.aiready.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 记录系统中的所有操作行为
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_operation_log")
public class OperationLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 操作模块
     */
    private String module;
    
    /**
     * 操作类型（CREATE：创建 UPDATE：更新 DELETE：删除 QUERY：查询 EXPORT：导出 IMPORT：导入 LOGIN：登录 LOGOUT：登出 OTHER：其他）
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String operationDesc;
    
    /**
     * 请求方法（GET/POST/PUT/DELETE等）
     */
    private String requestMethod;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;
    
    /**
     * 响应结果（JSON格式）
     */
    private String responseData;
    
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
     * 用户代理（浏览器信息）
     */
    private String userAgent;
    
    /**
     * 操作状态（0：失败 1：成功）
     */
    private Integer status;
    
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
