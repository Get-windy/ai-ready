package cn.aiedge.erp.purchase.inbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseInboundCreateDTO {

    private Long orderId;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private Long contractId;

    private String contractNo;

    private LocalDate inboundDate;

    private Integer inboundType;

    private Long warehouseId;

    private String warehouseName;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private String trackingNumber;

    private String logisticsCompany;

    private String remark;

    private String internalNote;

    private String summary;

    private BigDecimal extNum1;

    private BigDecimal extNum2;

    private String extText1;

    private String extText2;

    private String extText3;

    private BigDecimal discountAmount;

    private BigDecimal fee;

    private List<PurchaseInboundItemDTO> items;
}