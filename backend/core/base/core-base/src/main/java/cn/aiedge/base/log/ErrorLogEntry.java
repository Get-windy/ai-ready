package cn.aiedge.base.log;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * 错误日志条目 — JSONL 文件的每一行
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLogEntry {
    /** 唯一 ID */
    private String id;
    /** 时间戳 */
    private Date timestamp;
    /** 级别: ERROR / WARN / FATAL */
    private String level;
    /** 来源: backend / frontend / scheduler */
    private String source;
    /** 错误类型 (异常类名/错误分类) */
    private String type;
    /** 错误消息 */
    private String message;
    /** 堆栈跟踪 */
    private String stackTrace;
    /** 请求 URL (前端或后端) */
    private String url;
    /** 用户代理 (前端) */
    private String userAgent;
    /** 用户 ID */
    private Long userId;
    /** 租户 ID */
    private Long tenantId;
    /** 请求 IP */
    private String ip;
    /** 附加上下文 */
    private Map<String, Object> context;
}
