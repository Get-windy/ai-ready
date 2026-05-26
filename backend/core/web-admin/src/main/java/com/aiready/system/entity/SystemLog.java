package com.aiready.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统日志实体类
 * 用于记录系统操作日志
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_log")
public class SystemLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 日志类型（1：操作日志 2：登录日志 3：异常日志 4：系统日志）
     */
    private Integer logType;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 操作类型（INSERT/UPDATE/DELETE/SELECT/LOGIN/LOGOUT/EXPORT/IMPORT等）
     */
    private String operationType;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方式（GET/POST/PUT/DELETE）
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseResult;

    /**
     * 执行时长（毫秒）
     */
    private Long executionTime;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人名称
     */
    private String username;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 操作地点
     */
    private String location;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 操作状态（0：失败 1：成功）
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 业务主键
     */
    private String businessKey;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
