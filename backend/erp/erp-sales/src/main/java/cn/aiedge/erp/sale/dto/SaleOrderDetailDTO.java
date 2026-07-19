package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单详情DTO
 * 包含主表核心字段 + 子表数据
 */
@Data
public class SaleOrderDetailDTO {

    // ═══ 主表核心字段 ═══
    private String id;
    private String orderNo;
    private LocalDate orderDate;
    private Integer saleType;
    private Integer status;
    private String statusName;

    private Long customerId;
    private Long warehouseId;
    private Long salesmanId;
    private Long deptId;

    private BigDecimal productAmount;
    private BigDecimal discountAmount;
    private BigDecimal billAmount;
    private BigDecimal settledAmount;
    private BigDecimal receivedAmount;
    private BigDecimal totalQuantity;

    private LocalDateTime expectedShipTime;
    private String supplementType;
    private String generationMethod;
    private String sourceOrder;
    private Integer orderSource;
    private String remark;

    private Long originalOrderId;
    private String originalOrderNo;

    // ══ 冗余快照字段 ═══
    private String customerName;
    private String customerCode;
    private String customerLevel;
    private String customerRemark;
    private String customerTicket;
    private String warehouseName;
    private String salesmanName;
    private String deptName;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddress;
    private String promoterId;
    private String promoterName;
    private String contactName;
    private String contactPhone;
    private String pickupAddress;
    private String pickingWarehouse;
    private String collectionLocation;

    // ═══ 金额细分 ═══
    private BigDecimal promoDiscount;
    private BigDecimal couponAmount;
    private BigDecimal directDiscount;
    private BigDecimal otherFee;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;
    private BigDecimal shippedQuantity;
    private BigDecimal unshippedQuantity;
    private BigDecimal returnQuantity;
    private BigDecimal returnAmount;

    // ═══ 自定义/扩展 ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;
    private String footerExtText1;
    private String footerExtText2;
    private String summary;
    private String attachment;

    // ═══ 审核/制单 ═══
    private String creatorName;
    private String submitterName;
    private String auditorName;
    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private LocalDateTime bookkeepingTime;
    private Integer printCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ═══ 子表数据 ═══

    /** 往来单位快照 */
    private PartnerSnapshotDTO partnerSnapshot;

    /** 收货地址列表 */
    private List<DeliveryAddressDTO> deliveryAddresses;

    /** 结算信息 */
    private SettlementDTO settlement;

    /** 物流信息列表 */
    private List<LogisticsDTO> logisticsList;

    /** 订金账户列表 */
    private List<DepositDTO> deposits;

    /** 会员积分流水 */
    private PointsJournalDTO pointsJournal;

    /** 审核流水 */
    private List<AuditTrailDTO> auditTrails;

    /** 扩展信息 */
    private ExtInfoDTO extInfo;

    /** 订单明细 */
    private List<SaleOrderItemDTO> items;

    // ═══ 内部DTO类 ═══

    @Data
    public static class PartnerSnapshotDTO {
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
    public static class DeliveryAddressDTO {
        private String addressType;
        private String contactName;
        private String phone;
        private String address;
        private Boolean isDefault;
    }

    @Data
    public static class SettlementDTO {
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
    public static class LogisticsDTO {
        private String logisticsType;
        private String deliveryMethod;
        private String deliveryRoute;
        private Long deliveryRouteId;
        private Long driverId;
        private String driverName;
        private String deliveryVehicle;
        private String logisticsCompany;
        private String logisticsNo;
        private String waybillNo;
        private String freightPayer;
        private BigDecimal shippingFee;
        private BigDecimal codAmount;
    }

    @Data
    public static class DepositDTO {
        private String accountName;
        private BigDecimal amount;
        private Integer sequence;
    }

    @Data
    public static class PointsJournalDTO {
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
    public static class AuditTrailDTO {
        private String action;
        private Long operatorId;
        private String operatorName;
        private LocalDateTime actionTime;
        private String remark;
    }

    @Data
    public static class ExtInfoDTO {
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
