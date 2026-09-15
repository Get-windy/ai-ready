package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城订单「按明细」分页行（管理端 /order/page-detail）
 *
 * <p>数据源：{@code erp_sale_order_item} INNER JOIN {@code erp_sale_order}（order_source IN (2,3)）。
 * 明细行字段 + 单头字段合并成一行，供前端「订单处理 / 商城订单」页的「按明细」Tab 直接消费。</p>
 *
 * <p>字段口径：明细侧仅取 {@code erp_sale_order_item} 实际存在的列（barcode/specification/unit/
 * batch_code/production_date/expiry_date/shipped_quantity 等）；
 * 单头侧取 {@code erp_sale_order} 现有列（含 {@code ErpSaleOrderMall} 已映射的金标准展示列）。
 * 库中确实无对应列的展示项（部门、活动商品、单据自定义字段等）不在本 DTO 中产出，
 * 前端对应列保持空值，不做假数据填充。</p>
 */
@Data
public class MallOrderItemPageDTO {

    // ==================== 明细行（erp_sale_order_item） ====================

    /** 明细主键（前端行键使用 itemId，避免与单头 id 重复） */
    private Long itemId;

    /** 所属订单ID */
    private Long orderId;

    /** 行号 */
    private Integer lineNo;

    /** 商品ID */
    private Long productId;

    /** 商品货号 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 条码 */
    private String barcode;

    /** 小单位条码 */
    private String smallUnitBarcode;

    /** 规格 */
    private String specification;

    /** 型号（明细 model） */
    private String modelNo;

    /** 产地（明细 origin） */
    private String originPlace;

    /** 品牌（明细 brand） */
    private String brand;

    /** 单位 */
    private String unit;

    /** 批次条码（明细 batch_code） */
    private String batchBarcode;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 到期日期 */
    private LocalDateTime expiryDate;

    /** 销售数量（前端「按明细」列 key：saleQuantity） */
    private BigDecimal saleQuantity;

    /** 大包装数量 */
    private BigDecimal bigPack;

    /** 中包装数量 */
    private BigDecimal midPack;

    /** 小包装数量 */
    private BigDecimal smallPack;

    /** 已发数量（前端「按明细」列 key：shippedItemQty） */
    private BigDecimal shippedItemQty;

    /** 小单位数量（明细 small_unit_quantity） */
    private BigDecimal smallUnitQuantity;

    /**
     * 未发数量 = 销售数量 - 已发数量（由 SQL 派生，非新增库字段）
     * 前端「按明细」列 key：unshippedItemQty。
     */
    private BigDecimal unshippedItemQty;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 小单位单价（明细 small_unit_price） */
    private BigDecimal smallUnitPrice;

    /** 金额 */
    private BigDecimal amount;

    /** 明细备注 */
    private String itemRemark;

    // ==================== 单头（erp_sale_order） ====================

    /** 单据编号 */
    private String orderNo;

    /** 单据日期 */
    private LocalDateTime orderDate;

    /** 订单状态: 0草稿,1待审批,2已审批,3部分出库,4完成,5交易完成,6已取消 */
    private Integer status;

    /** 订单来源: 2=企业客户商城 3=个人会员商城 */
    private Integer orderSource;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 已收款金额 */
    private BigDecimal receivedAmount;

    /** 支付方式: ALIPAY, WECHAT, UNIONPAY, BANK, CASH */
    private String paymentMethod;

    /** 支付状态: 0待支付,1支付中,2已支付,3部分支付,4已退款 */
    private Integer paymentStatus;

    /** 发货状态: 0待发货,1部分发货,2已发货,3已签收 */
    private Integer deliveryStatus;

    /** 收货人 */
    private String consignee;

    /** 收货人电话 */
    private String consigneePhone;

    /** 收货地址（冗余合并优先，回退收货详细地址） */
    private String consigneeAddress;

    /** 卖家备注 */
    private String orderRemark;

    /** 单据备注（erp_sale_order.remark，审核驳回原因等也落此列） */
    private String sellerRemark;

    /** 客户（买家）备注 */
    private String buyerRemark;

    /** 提货地址 */
    private String pickupAddress;

    /** 制单人（erp_sale_order.creator_name） */
    private String creatorName;

    /** 扩展信息 JSON（含 originalMallStatus） */
    private String extInfo;

    /** 创建时间（提交时间） */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ==================== 金标准「按明细」列所需单头字段（复用 ErpSaleOrderMall 既有列） ====================

    /** 运费（shipping_fee） */
    private BigDecimal freight;

    /** 运费承担方 */
    private String freightPayer;

    /** 配送方式 */
    private String deliveryMethod;

    /** 物流公司 */
    private String logisticsCompany;

    /** 运单号（waybill_no） */
    private String trackingNo;

    /** 代收金额 */
    private BigDecimal codAmount;

    /** 预计发货时间 */
    private LocalDateTime expectedShipTime;

    /** 商品数量（total_quantity） */
    private BigDecimal productQuantity;

    /** 仓库名称 */
    private String warehouseName;

    /**
     * 部门名称（erp_sale_order.dept_name，Tab2「部门」列数据源）
     *
     * <p>该列由 V9.49.0__Add_Missing_Sale_Order_Columns.sql 建于 erp_sale_order，
     * 非新造列；部门ID 为 erp_sale_order.dept_id（上游未落名称时本字段为 null）。</p>
     */
    private String departmentName;

    /** 经手人（salesman_name） */
    private String handlerName;

    /** 推广人 */
    private String promoterName;

    /** 打印次数 */
    private Integer printCount;

    /** 记账状态: 0未记账 1已记账 */
    private Integer bookkeepingStatus;

    /** 已结金额 */
    private BigDecimal settledAmount;

    /** 是否使用优惠券: 0否 1是 */
    private Integer couponUsed;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 其他费用 */
    private BigDecimal otherFee;

    /** 表头自定义字段1(数字) */
    private BigDecimal extNum1;

    /** 表头自定义字段2(数字) */
    private BigDecimal extNum2;

    /** 表头自定义字段3(文本) */
    private String extText3;

    /** 表头自定义字段4(文本) */
    private String extText4;

    /** 表头自定义字段5(文本) */
    private String extText5;
}
