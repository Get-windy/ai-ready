package cn.aiedge.trade.dto;

import lombok.Data;

/**
 * 库存查询结果
 */
@Data
public class InventoryQueryResult {

    /** SKU编码 */
    private String skuCode;

    /** 外部SKU ID */
    private String externalSkuId;

    /** 平台库存数量 */
    private Integer quantity;

    /** 可售数量 */
    private Integer availableQuantity;

    /** 占用数量 */
    private Integer lockedQuantity;

    /** 查询时间戳 */
    private Long queryTimestamp;

    /** 是否成功 */
    private boolean success;

    /** 错误信息 */
    private String errorMsg;
}