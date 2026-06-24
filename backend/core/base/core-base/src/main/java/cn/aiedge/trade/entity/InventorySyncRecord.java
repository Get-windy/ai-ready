package cn.aiedge.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存同步记录
 */
@Data
@TableName("inventory_sync_record")
public class InventorySyncRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 渠道编码 */
    private String channelCode;

    /** 产品ID */
    private Long productId;

    /** SKU编码 */
    private String skuCode;

    /** 内部库存数量 */
    private Integer internalQty;

    /** 外部平台库存数量 */
    private Integer externalQty;

    /** 同步数量 */
    private Integer syncQty;

    /** 同步类型: PUSH, PULL, QUERY */
    private String syncType;

    /** 同步时间 */
    private LocalDateTime syncTime;

    /** 同步状态: 0待同步, 1成功, 2失败 */
    private Integer syncStatus;

    /** 错误信息 */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}