package cn.aiedge.erp.sale.outbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售出库单VO（后端 → 前端）
 * 包含列表页和表单页所需的全部字段
 */
@Data
public class SaleOutboundVO {

    private Long id;

    // ═══ 基本信息 ═══
    private String outboundNo;
    private Long orderId;
    private String orderNo;
    private LocalDate outboundDate;
    private Integer outboundType;
    private String outboundTypeDesc;
    private Integer status;
    private String statusDesc;
    private String generationMethod;
    private String summary;

    // ═══ 客户快照 ═══
    private Long customerId;
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private Long contactId;
    private String contactName;
    private String customerRemark;

    // ═══ 银行/税务 ═══
    private String bankName;
    private String bankAccount;
    private String taxNo;

    // ═══ 仓库/经手人 ═══
    private Long warehouseId;
    private String warehouseName;
    private Long salesPersonId;
    private String salesPersonName;
    private Long departmentId;
    private String departmentName;
    private String location;
    private String region;

    // ═══ 收货 ═══
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    // ═══ 数量/金额 ═══
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal promoDiscount;
    private BigDecimal couponAmount;
    private BigDecimal directDiscount;
    private BigDecimal otherFee;
    private BigDecimal roundingAmount;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;
    private BigDecimal returnQuantity;
    private BigDecimal returnAmount;
    private Integer boxCount;

    // ═══ 结算 ═══
    private String settlementMethod;
    private BigDecimal settledAmount;
    private String settlementStatus;

    // ═══ 收款账户 ═══
    private String paymentAccount1;
    private String paymentAccount2;
    private String paymentAccount3;
    private String paymentAccount4;

    // ═══ 物流 ═══
    private String deliveryMethod;
    private String logisticsCompany;
    private String logisticsBranch;
    private String freightPayer;
    private BigDecimal freight;
    private String trackingNumber;
    private String waybillNo;
    private BigDecimal codAmount;
    private String deliveryDriver;

    // ═══ 流程时间/人员 ═══
    private LocalDateTime expectedShipTime;
    private LocalDateTime actualShipTime;
    private Long pickingBy;
    private LocalDateTime pickingTime;
    private Long packingBy;
    private LocalDateTime packingTime;
    private Long shippedBy;
    private LocalDateTime shippedTime;
    private Long approvedBy;
    private LocalDateTime approvedTime;
    private String approvedNote;
    private Long completedBy;
    private LocalDateTime completedTime;

    // ═══ 会员/积分 ══
    private String memberCardNo;
    private BigDecimal prevPoints;
    private BigDecimal memberGeneratedPoints;
    private BigDecimal memberExchangePoints;
    private BigDecimal memberUsedPoints;
    private BigDecimal currentPoints;

    // ═══ 收款日/对账日 ═══
    private LocalDate paymentDate;
    private LocalDate reconciliationDate;

    // ═══ 备注 ═══
    private String remark;
    private String internalNote;
    private String buyerRemark;

    // ═══ 列表显示字段 ═══
    private String bookkeeperName;
    private String creatorName;
    private String auditorName;
    private Integer printCount;
    private LocalDateTime bookkeepingTime;
    private LocalDateTime printTime;

    // ═══ 表头自定义字段 ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private BigDecimal extNum3;
    private BigDecimal extNum4;
    private BigDecimal extNum5;
    private String extText1;
    private String extText2;
    private String extText3;
    private String extText4;
    private String extText5;
    private Long extPartner;
    private Long extStaff;
    private Long extDept;

    // ═══ 表尾自定义字段 ═══
    private String footerExtText1;
    private String footerExtText2;

    // ═══ 系统字段 ═══
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;

    // ═══ 明细列表 ═══
    private List<?> items;
}
