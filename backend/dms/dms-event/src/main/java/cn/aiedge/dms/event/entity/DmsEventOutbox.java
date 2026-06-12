package cn.aiedge.dms.event.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件发件箱
 *
 * 基于发件箱模式(Outbox Pattern)实现可靠的事件发布。
 * 系统内部产生事件时先写入此表，再由定时任务异步发送到目标系统。
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_event_outbox")
public class DmsEventOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 追踪ID（全局唯一，用于去重和链路追踪） */
    private String traceId;

    /** 事件类型（如：SETTLEMENT_PUSH_ERP, ORDER_SYNC_WMS） */
    private String eventType;

    /** 事件来源（默认 DMS） */
    private String source;

    /** 目标系统（如：ERP, WMS, OMS） */
    private String target;

    /** 事件载荷（JSON字符串） */
    private String payload;

    /** 发送状态：0-待发送 1-已发送 2-失败 */
    private Integer status;

    /** 重试次数 */
    private Integer retryCount;

    /** 最后一次错误信息 */
    private String lastError;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;
}
