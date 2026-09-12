package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单列表DTO - 完整版
 * 用于分页列表、统计卡片等列表展示场景
 * 包含列表页65+列所需的全部字段
 */
@Data
public class SaleOrderListDTO {

    private String id;
    private String orderNo;
    private LocalDate orderDate;
    private Integer saleType;
    private Integer status;
    private String statusName;

    // 客户信息(通过JOIN或冗余字段获取)
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private String customerRemark;
    private String customerTicket;

    // 经手人/部门(通过冗余字段获取)
    private Long salesmanId;
    private String salesmanName;
    private String deptName;

    // 仓库(通过冗余字段获取)
    private Long warehouseId;
    private String warehouseName;

    // 收货信息
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    // 推广人
    private Long promoterId;
    private String promoterName;

    // 金额
    private BigDecimal productAmount;
    private BigDecimal discountAmount;
    /** 优惠后金额 = 商品金额 − 促销优惠 − 优惠金额（= 本单金额 − 其他费用） */
    private BigDecimal favorableAmount;
    private BigDecimal billAmount;
    private BigDecimal settledAmount;
    private BigDecimal receivedAmount;
    private BigDecimal promoDiscount;
    private BigDecimal couponAmount;
    private BigDecimal directDiscount;
    private BigDecimal otherFee;

    // 结算状态（由 settledAmount 与 billAmount 计算得出）
    private String settlementStatus;

    // 运费
    private String freightPayer;
    private BigDecimal shippingFee;

    // 数量
    private BigDecimal totalQuantity;
    private BigDecimal shippedQuantity;
    private BigDecimal unshippedQuantity;
    private BigDecimal returnQuantity;
    private BigDecimal returnAmount;

    // 物理汇总
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    // 物流
    private String logisticsCompany;
    private String waybillNo;

    // 订金账户
    private String depositAccount1;
    private String depositAccount2;
    private String depositAccount3;
    private String depositAccount4;

    // 区域/销售类型/配送方式
    private String region;
    private String deliveryMethod;

    // 备注
    private String buyerRemark;
    private String orderRemark;
    private String summary;

    // 附件
    private String attachment;

    // 自定义字段(表头)
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;

    // 表尾自定义字段
    private String footerExtText1;
    private String footerExtText2;

    // 提交/审核/制单信息
    private LocalDateTime submitTime;
    private String generationMethod;
    private LocalDateTime bookkeepingTime;
    private String creatorName;
    private String submitterName;
    private String auditorName;
    private LocalDateTime auditTime;
    private Integer printCount;

    // 第三方/来源
    private String thirdPartyOrderNo;
    private String sourceOrder;

    // 结款方式
    private String settlementMethod;

    // 预计发货时间
    private LocalDateTime expectedShipTime;

    // 履约相关
    private String originalOrderNo;
    private String shippedOrderNo;
    private String supplementStatus;
    private BigDecimal originalAmount;
    private BigDecimal remainingUnshippedAmount;
    private BigDecimal originalDiscount;
    private Integer originalItemCount;
    private Integer unshippedItemCount;
    private BigDecimal originalQuantity;
    private BigDecimal unshippedQuantityItems;

    // 拣货/发货
    private String pickingWarehouse;
    private String collectionLocation;
    private String pickupAddress;
    /** 配送线路 */
    private String deliveryRoute;
    /** 配送司机 */
    private String driverName;
    /** 配送车辆 */
    private String deliveryVehicle;
    /** 已拣货数量（主表汇总，拣货作业回写） */
    private BigDecimal pickedQuantity;
    /** 未拣货数量（派生：订货数量 − 已拣货数量，不落库） */
    private BigDecimal unpickedQuantity;
    /** 商品行数（订单明细行数，查询时统计） */
    private Integer lineCount;
    /** 排序（拣货顺序序号） */
    private Integer sortOrder;
    /** 排序值（拣货顺序二级权重） */
    private Integer sortValue;
    /** 单据来源 */
    private Integer orderSource;

    // 时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 业务扩展字段
    private String productBrand;
    private String industryCategory;
    private String extText4;
    private String extText5;
}
