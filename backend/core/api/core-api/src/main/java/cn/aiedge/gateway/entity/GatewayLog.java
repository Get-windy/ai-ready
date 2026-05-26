package cn.aiedge.gateway.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 网关日志实体
 * 记录通过API网关的所有请求信息
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("gateway_log")
public class GatewayLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 日志类型 (ACCESS-访问日志, ERROR-错误日志, SECURITY-安全日志)
     */
    private String logType;

    /**
     * HTTP方法 (GET, POST, PUT, DELETE等)
     */
    private String method;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 查询字符串
     */
    private String queryString;

    /**
     * 事件类型 (安全相关)
     */
    private String eventType;

    /**
     * 客户端IP地址
     */
    private String remoteAddr;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 引用页面
     */
    private String referer;

    /**
     * 响应状态码
     */
    private Integer statusCode;

    /**
     * 请求耗时(毫秒)
     */
    private Long duration;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 详细信息
     */
    private String details;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}
