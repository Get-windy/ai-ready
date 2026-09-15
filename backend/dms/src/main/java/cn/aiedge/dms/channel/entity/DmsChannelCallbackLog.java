package cn.aiedge.dms.channel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 渠道回调日志（外部平台状态/轨迹回传的接收留痕）
 *
 * <p>对应表 {@code dms_channel_callback_log}（迁移 V11.350.0）。
 * 记录验签结果、是否重放、处理结果与原始报文，支撑《渠道管理开发文档》§3.4「回调安全 + 可追溯」。
 * 同渠道同 nonce 唯一（部分唯一索引），重放请求只更新原行的 replayed 标记。</p>
 *
 * @author AI-Ready Team
 */
@Data
@TableName("dms_channel_callback_log")
public class DmsChannelCallbackLog {

    /** 处理结果：已处理 */
    public static final String RESULT_OK = "OK";
    /** 处理结果：重放（同 nonce 重复投递，已忽略） */
    public static final String RESULT_REPLAY = "REPLAY";
    /** 处理结果：拒绝（验签失败/时间戳过期/密钥缺失） */
    public static final String RESULT_REJECT = "REJECT";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long channelId;

    private String channelCode;

    private String channelOrderNo;

    private Long taskId;

    private String taskNo;

    /** 平台事件类型（如 rider_accepted / order_finished） */
    private String eventType;

    /** 平台侧状态原文 */
    private String externalStatus;

    /** 平台请求号（防重放） */
    private String nonce;

    /** 验签结果：0-失败 1-通过 */
    private Integer signOk;

    /** 是否重放：0-否 1-是 */
    private Integer replayed;

    /** 处理结果：OK / REPLAY / REJECT / UNMATCHED */
    private String processResult;

    private String processMessage;

    /** 原始报文（截断至 2000 字符） */
    private String payload;

    private LocalDateTime receiveTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
