package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单DTO - 与实体保持一致，移除冗余合计字段
 */
@Data
public class SaleOrderDTO {

    private Long id;
    private Long tenantId;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private LocalDateTime orderDate;
    private LocalDateTime expectedShipDate;
    private Integer status;
    private String statusName;
    // 注意：移除冗余字段 totalAmount, taxAmount, totalAmountWithTax
    // 这些字段将通过明细实时计算，不存储在表头
    private BigDecimal receivedAmount;
    private Long salesmanId;
    private String salesmanName;
    private Long deptId;
    private Long warehouseId;
    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;
    private Integer saleType;
    private Long paymentAccountId;
    private String logisticsCompany;
    private String logisticsNo;
    private BigDecimal shippingFee;
    private String memberCardNo;
    private String memberName;
    private Integer memberDiscount;
    private String orderRemark;
    private String buyerRemark;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== 新增客户等级字段 =====
    private String customerGradeCode;
    private String customerGradeName;

    /**
     * 扩展信息
     */
    private String extInfo;

    /**
     * 订单明细列表
     */
    private List<SaleOrderItemDTO> items;
}