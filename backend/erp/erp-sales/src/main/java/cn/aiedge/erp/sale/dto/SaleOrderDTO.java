package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单创建/更新DTO - 输入参数
 * 只包含用户输入字段，不包含ID/审计/快照字段
 */
@Data
public class SaleOrderDTO {

    private Long id;
    private Long tenantId;

    // ═══ 基本信息 ═══
    private String orderNo;
    private LocalDate orderDate;
    private Integer saleType;
    private String supplementType;
    private String generationMethod;
    private String sourceOrder;
    private Integer orderSource;
    private LocalDateTime expectedShipTime;

    // ═══ 外键 ═══
    private Long customerId;
    private Long warehouseId;
    private Long salesmanId;
    private Long deptId;

    // ═══ 金额 ═══
    private BigDecimal productAmount;
    private BigDecimal promoDiscount;
    private BigDecimal couponAmount;
    private BigDecimal directDiscount;
    private BigDecimal discountAmount;
    private BigDecimal otherFee;
    private BigDecimal billAmount;
    private BigDecimal shippingFee;

    // ═══ 数量 ═══
    private BigDecimal totalQuantity;

    // ═══ 履约 ═══
    private Long originalOrderId;
    private String originalOrderNo;

    // ═══ 备注 ═══
    private String remark;
    private String buyerRemark;
    private String orderRemark;

    // ═══ 收货信息 ═══
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;

    // ═══ 物流冗余 ═══
    private String logisticsCompany;
    private String freightPayer;
    private String waybillNo;
    private String deliveryMethod;
    private String deliveryRoute;
    private Long deliveryRouteId;
    private String settlementMethod;
    private String region;
    private String summary;
    private String attachment;

    // ═══ 自定义字段 ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;
    private String footerExtText1;
    private String footerExtText2;

    // ═══ 往来单位快照(创建时传入) ═══
    private PartnerInfo partnerInfo;

    // ═══ 收货地址 ═══
    private List<AddressInfo> deliveryAddresses;

    // ═══ 结算信息 ═══
    private SettlementInfo settlementInfo;

    // ═══ 物流信息 ═══
    private List<LogisticsInfo> logisticsInfoList;

    // ═══ 订金 ═══
    private List<DepositInfo> deposits;

    // ═══ 会员信息 ═══
    private MemberInfo memberInfo;

    // ═══ 扩展信息 ═══
    private ExtInfo extInfoData;

    // ═══ 订单明细 ═══
    private List<SaleOrderItemDTO> items;

    // ═══ 内部类 ═══

    @Data
    public static class PartnerInfo {
        private String customerName;
        private String customerCode;
        private String customerLevel;
        private String customerGradeCode;
        private String customerGradeName;
        private String customerTicket;
        private String customerRemark;
        private String bankName;
        private String bankAccount;
        private String taxNo;
        private String region;
    }

    @Data
    public static class AddressInfo {
        private String addressType;
        private String contactName;
        private String phone;
        private String address;
        private Boolean isDefault;
    }

    @Data
    public static class SettlementInfo {
        private String settlementMethod;
        private BigDecimal creditLimit;
        private BigDecimal availableCredit;
        private BigDecimal prevDebt;
        private LocalDate paymentDate;
        private LocalDate reconciliationDate;
        private Long paymentAccountId;
        private String paymentMethod;
        private Integer paymentStatus;
    }

    @Data
    public static class LogisticsInfo {
        private String logisticsType;
        private String deliveryMethod;
        private String deliveryRoute;
        private Long deliveryRouteId;
        private Long driverId;
        private String driverName;
        private String deliveryVehicle;
        private String logisticsCompany;
        /** 物流公司档案ID（biz_party.id，partnerType=LOGISTICS） */
        private Long logisticsCompanyId;
        private String logisticsNo;
        private String waybillNo;
        private String freightPayer;
        private BigDecimal shippingFee;
        private BigDecimal codAmount;
        // ── 包裹/运单层（一单多包：一条 = 一个包裹）──
        private String packageNo;
        private Integer packageCount;
        private BigDecimal packageWeight;
        private BigDecimal packageVolume;
        private Integer packageStatus;
        private String remark;
    }

    @Data
    public static class DepositInfo {
        private String accountName;
        private BigDecimal amount;
    }

    @Data
    public static class MemberInfo {
        private String memberCardNo;
        private String memberName;
        private Integer memberDiscount;
        private BigDecimal prevPoints;
        private BigDecimal salePoints;
        private BigDecimal returnPoints;
        private BigDecimal exchangePoints;
        private BigDecimal usedPoints;
        private BigDecimal currentPoints;
    }

    @Data
    public static class ExtInfo {
        private String summary;
        private String attachment;
        private BigDecimal extNum1;
        private BigDecimal extNum2;
        private String extText1;
        private String extText2;
        private String extText3;
        private String footerExtText1;
        private String footerExtText2;
    }
}
