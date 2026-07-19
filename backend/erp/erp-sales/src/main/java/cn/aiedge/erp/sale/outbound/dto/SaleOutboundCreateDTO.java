package cn.aiedge.erp.sale.outbound.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售出库单创建/更新DTO（前端 → 后端）
 * 包含完整的表头字段 + 明细列表
 */
@Data
public class SaleOutboundCreateDTO {

    // ═══ 基本信息 ═══
    private Long orderId;
    private String orderNo;
    private LocalDate outboundDate;
    private Integer outboundType;
    private Integer status;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
    private String generationMethod;
    private String summary;

    // ═══ 客户 ══
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

    // ═══ 金额/费用 ═══
    private BigDecimal promoDiscount;
    private BigDecimal couponAmount;
    private BigDecimal directDiscount;
    private BigDecimal otherFee;
    private BigDecimal roundingAmount;

    // ═══ 结算 ═══
    private String settlementMethod;
    private BigDecimal settledAmount;
    private String settlementStatus;

    // ═══ 审核信息 ═══
    @JsonAlias("approverId")
    private Long approvedBy;

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

    // ═══ 会员/积分 ═══
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

    // ═══ 明细列表 ═══
    private List<SaleOutboundItemDTO> items;
}
