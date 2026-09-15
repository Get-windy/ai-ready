package cn.aiedge.dms.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道外部单台账（向外部运力平台下单的记录）
 *
 * <p>对应表 {@code dms_channel_order}（迁移 V11.350.0）。幂等键 {@code idem_key = taskNo:channelId}
 * 由部分唯一索引 `uk_dms_channel_order_idem` 保证「同一任务同一渠道只下一次单」。</p>
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_channel_order")
public class DmsChannelOrder {

    /** 单状态：待提交 */
    public static final int STATUS_PENDING = 0;
    /** 单状态：已提交 */
    public static final int STATUS_SUBMITTED = 1;
    /** 单状态：已接单 */
    public static final int STATUS_ACCEPTED = 2;
    /** 单状态：配送中 */
    public static final int STATUS_DELIVERING = 3;
    /** 单状态：已完成 */
    public static final int STATUS_COMPLETED = 4;
    /** 单状态：已取消 */
    public static final int STATUS_CANCELLED = 5;
    /** 单状态：提交失败 */
    public static final int STATUS_FAILED = 6;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long channelId;

    private String channelCode;

    private Long taskId;

    private String taskNo;

    /** 幂等键 = taskNo:channelId */
    private String idemKey;

    /** 外部平台单号（回调按此关联） */
    private String channelOrderNo;

    /** 0-待提交 1-已提交 2-已接单 3-配送中 4-已完成 5-已取消 6-提交失败 */
    private Integer orderStatus;

    /** 已尝试次数（含首次） */
    private Integer attempts;

    /** 最近一次失败原因 */
    private String lastError;

    private LocalDateTime submitTime;

    private LocalDateTime callbackTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}
