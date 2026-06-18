package cn.aiedge.erp.purchase.purchasereturn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseReturnDTO {

    private Long id;

    private Long purchaseOrderId;

    private String purchaseOrderNo;

    private Long supplierId;

    private String supplierName;

    private Integer returnType;

    private String reason;

    private String remark;

    private List<PurchaseReturnItemDTO> items;
}