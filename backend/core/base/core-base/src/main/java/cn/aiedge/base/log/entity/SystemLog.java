package cn.aiedge.base.log.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统日志实体类
 * 记录系统运行过程中的各类事件，包括操作日志、登录日志、异常日志等
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_system_log")
public class SystemLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 日志类型
     * 1-操作日志, 2-登录日志, 3-异常日志, 4-系统日志, 5-安全日志
     */
    private Integer logType;

    /**
     * 日志级别
     * INFO, WARN, ERROR, DEBUG
     */
    private String logLevel;

    /**
     * 日志标题
     */
    private String title;

    /**
     * 操作类型
     * CREATE, UPDATE, DELETE, SELECT, LOGIN, LOGOUT, EXPORT, IMPORT等
     */
    private String operationType;

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
     * 请求方法
     */
    private String method;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方式 (GET, POST, PUT, DELETE)
     */
    private String requestMethod;

    /**
     * 请求参数 (JSON格式)
     */
    private String requestParams;

    /**
     * 响应结果 (JSON格式)
     */
    private String responseResult;

    /**
     * 执行时长 (毫秒)
     */
    private Long executionTime;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * IP地理位置
     */
    private String location;

    /**
     * 浏览器信息
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 设备类型 (PC, Mobile, Tablet)
     */
    private String deviceType;

    /**
     * 操作状态 (0-失败, 1-成功)
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 删除标志 (0-未删除, 1-已删除)
     */
    @TableLogic
    private Integer deleted;
}