package cn.aiedge.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部订单原始数据
 */
@Data
@TableName("external_order_raw")
public class ExternalOrderRaw {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 渠道编码 */
    private String channelCode;

    /** 外部平台订单号 */
    private String externalOrderId;

    /** 原始JSON数据 */
    private String rawData;

    /** 接收时间 */
    private LocalDateTime receiveTime;

    /** 处理状态: 0待处理, 1已转换, 2已入库, 3失败 */
    private Integer processStatus;

    /** 内部订单ID */
    private Long internalOrderId;

    /** 错误信息 */
    private String errorMsg;

    /** 重试次数 */
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}