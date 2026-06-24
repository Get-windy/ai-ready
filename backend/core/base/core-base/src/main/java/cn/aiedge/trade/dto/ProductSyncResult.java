package cn.aiedge.trade.dto;

import lombok.Data;

/**
 * 商品同步结果
 */
@Data
public class ProductSyncResult {

    /** SKU编码 */
    private String skuCode;

    /** 外部SKU ID */
    private String externalSkuId;

    /** 同步状态 */
    private Integer status; // 0失败, 1成功, 2部分成功

    /** 状态描述 */
    private String statusMsg;

    /** 同步时间戳 */
    private Long syncTimestamp;

    /** 平台返回数据 */
    private String response;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMsg;
}