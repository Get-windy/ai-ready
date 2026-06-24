package cn.aiedge.trade.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 外部订单DTO - 统一的订单转换格式
 */
@Data
public class ExternalOrderDTO {

    /** 外部平台订单ID */
    private String externalOrderId;

    /** 外部平台订单号 */
    private String externalOrderNo;

    /** 渠道编码 */
    private String channelCode;

    /** 店铺ID */
    private String shopId;

    /** 店铺名称 */
    private String shopName;

    /** 订单状态 (平台原始状态) */
    private String externalStatus;

    /** 订单金额 */
    private BigDecimal orderAmount;

    /** 实付金额 */
    private BigDecimal paidAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 运费 */
    private BigDecimal shippingFee;

    /** 买家ID */
    private String buyerId;

    /** 买家昵称 */
    private String buyerName;

    /** 买家手机 */
    private String buyerPhone;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区县 */
    private String district;

    /** 下单时间 */
    private LocalDateTime orderTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 订单明细 */
    private List<ExternalOrderItemDTO> items;

    /** 订单备注 */
    private String buyerRemark;

    /** 平台扩展信息 */
    private Map<String, Object> extInfo;

    /** 原始数据(JSON) */
    private String rawJson;

    @Data
    public static class ExternalOrderItemDTO {
        /** 外部商品ID */
        private String externalProductId;
        /** 外部SKU ID */
        private String externalSkuId;
        /** 商品标题 */
        private String productTitle;
        /** SKU编码 */
        private String skuCode;
        /** 数量 */
        private Integer quantity;
        /** 单价 */
        private BigDecimal unitPrice;
        /** 实付单价 */
        private BigDecimal paidPrice;
        /** 商品图片 */
        private String productImage;
        /** 商品扩展 */
        private Map<String, Object> extInfo;
    }
}