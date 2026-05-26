package cn.aiedge.erp.order.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单创建DTO
 */
@Data
public class PurchaseOrderCreateDTO {
    
    private Long id;
    
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;
    
    private String supplierName;
    
    @NotNull(message = "采购员ID不能为空")
    private Long buyerId;
    
    private String buyerName;
    
    @NotNull(message = "采购类型不能为空")
    private Integer purchaseType;
    
    @Size(max = 100, message = "订单备注不能超过100个字符")
    private String remark;
    
    private BigDecimal totalAmount;
    
    private BigDecimal taxAmount;
    
    private BigDecimal discountAmount;
    
    private BigDecimal finalAmount;
    
    private String currency;
    
    private String deliveryAddress;
    
    private LocalDateTime expectedDeliveryDate;
    
    private LocalDateTime paymentDueDate;
    
    private Integer paymentTerms;
    
    private String incoterms;
    
    private String shippingMethod;
    
    // 采购订单项列表
    private List<PurchaseOrderItemDTO> items;
    
    // 附件信息
    private List<AttachmentDTO> attachments;
    
    /**
     * 采购订单项DTO
     */
    @Data
    public static class PurchaseOrderItemDTO {
        
        @NotNull(message = "产品ID不能为空")
        private Long productId;
        
        private String productCode;
        
        private String productName;
        
        private String productSpec;
        
        @NotNull(message = "采购数量不能为空")
        private Integer quantity;
        
        @NotNull(message = "单价不能为空")
        private BigDecimal unitPrice;
        
        private BigDecimal itemAmount;
        
        private String unit;
        
        private BigDecimal taxRate;
        
        private BigDecimal taxAmount;
        
        private BigDecimal discountRate;
        
        private BigDecimal discountAmount;
        
        private String remark;
        
        private Long warehouseId;
        
        private String expectedDeliveryDate;
    }
    
    /**
     * 附件DTO
     */
    @Data
    public static class AttachmentDTO {
        
        private String fileName;
        
        private String fileUrl;
        
        private String fileType;
        
        private Long fileSize;
        
        private String remark;
    }
}