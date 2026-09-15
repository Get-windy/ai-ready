package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 发货通知（ASN）外发台账
 *
 * <p>发货后按订单+运单号生成（幂等），经配置的回调地址推送；未配置回调地址时停「待发送」并记录原因，
 * 支持手工重试。对齐业界 ASN（EDI DESADV / X12 856）的「先通知、后到货」语义。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_shipment_notify")
public class ShipmentNotify {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    private Long orderId;
    private String orderNo;

    private Long carrierId;
    private String carrierName;
    private String waybillNo;

    /** 通知类型：ASN-发货通知 */
    private String notifyType;

    /** 报文（JSON） */
    private String payload;

    /** 目标回调地址（发送时快照） */
    private String targetUrl;

    /** 0-待发送 1-已发送 2-发送失败 */
    private Integer status;

    private Integer retryCount;
    private String lastError;
    private LocalDateTime sentTime;

    /** 幂等键：订单ID + 运单号 */
    private String idempotentKey;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;
    private Long updateBy;

    @Version
    private Integer version;
}
