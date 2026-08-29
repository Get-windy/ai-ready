package cn.aiedge.erp.purchase.saledriven.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 以销定购-列表行（42列，对齐 ql361 对标文档的数据表列配置）
 * <p>
 * 数据源：erp_sale_order 主表（列字段即销售订单字段），订金账户来自
 * erp_sale_order_deposit 子表（取 sequence 前两条），是否采购来自 erp_purchase_order
 * 按 source_bill_no 反查。强制终止/记账状态/支付状态为推导列。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class SalesDrivenRowDTO {

    /** 销售订单ID */
    private Long id;

    /** 1 单据日期 */
    private LocalDateTime orderDate;

    /** 2 单据编号 */
    private String orderNo;

    /** 3 单据子类型（1正常/2样品/3促销） */
    private Integer saleType;

    /** 4 录入方式 */
    private String generationMethod;

    /** 5 单据状态 */
    private Integer status;

    /** 6 是否使用优惠券（couponAmount>0） */
    private Boolean useCoupon;

    /** 7 仓库 */
    private String warehouseName;

    /** 8 客户编号 */
    private String customerCode;

    /** 9 客户 */
    private String customerName;

    /** 10 记账状态（0未记账/1已记账） */
    private Integer bookkeepingStatus;

    /** 11 强制终止（系统未独立建模，恒 false） */
    private Boolean forcedTerminated;

    /** 12 是否采购（已生成采购单） */
    private Boolean hasPurchased;

    /** 13 发货商品数量 */
    private BigDecimal totalQuantity;

    /** 14 发货已发数量 */
    private BigDecimal shippedQuantity;

    /** 15 发货未发数量 */
    private BigDecimal unshippedQuantity;

    /** 16 重量（kg） */
    private BigDecimal totalWeight;

    /** 17 体积（m³） */
    private BigDecimal totalVolume;

    /** 18 订单金额 */
    private BigDecimal billAmount;

    /** 19 订金账户1 */
    private String depositAccount1;

    /** 20 订金金额1 */
    private BigDecimal depositAmount1;

    /** 21 订金账户2 */
    private String depositAccount2;

    /** 22 订金金额2 */
    private BigDecimal depositAmount2;

    /** 23 物流公司 */
    private String logisticsCompany;

    /** 24 运单号 */
    private String waybillNo;

    /** 25 运费 */
    private BigDecimal shippingFee;

    /** 26 优惠劵 */
    private BigDecimal couponAmount;

    /** 27 优惠金额 */
    private BigDecimal discountAmount;

    /** 28 收货人 */
    private String receiverName;

    /** 29 联系电话 */
    private String receiverPhone;

    /** 30 收货地址 */
    private String shippingAddress;

    /** 31 预计发货 */
    private LocalDateTime expectedShipTime;

    /** 32 经手人 */
    private String salesmanName;

    /** 33 部门 */
    private String deptName;

    /** 34 制单人 */
    private String creatorName;

    /** 35 提交人 */
    private String submitterName;

    /** 36 审核人 */
    private String auditorName;

    /** 37 卖家备注 */
    private String orderRemark;

    /** 38 买家备注 */
    private String buyerRemark;

    /** 39 附件 */
    private String attachment;

    /** 40 打印次数 */
    private Integer printCount;

    /** 41 提交时间 */
    private LocalDateTime submitTime;

    /** 42 制单时间 */
    private LocalDateTime bookkeepingTime;
}
