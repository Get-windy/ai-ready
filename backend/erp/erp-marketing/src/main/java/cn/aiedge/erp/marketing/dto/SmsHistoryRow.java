package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短信历史行（对标 7 列）：接收人 / 手机号 / 发送时间 / 经手人 / 短信内容 / 发送状态 / 失败原因
 * 投递状态取自 sys_message（平台消息底座，单一真源）。
 */
@Data
public class SmsHistoryRow {

    private Long id;
    /** 接收人 */
    private String receiverName;
    /** 手机号 */
    private String mobile;
    /** 发送时间 */
    private LocalDateTime sendTime;
    /** 经手人 */
    private String handlerName;
    /** 短信内容（含签名前缀） */
    private String content;
    /** 发送状态：待发送 / 发送成功 / 发送失败 */
    private String sendStatus;
    /** 失败原因 */
    private String failReason;

    private String batchNo;
    private String smsType;
}
