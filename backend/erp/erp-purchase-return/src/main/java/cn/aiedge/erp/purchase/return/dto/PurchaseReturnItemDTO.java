package cn.aiedge.erp.purchase.return.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购换货单明细数据传输对象
 * 
 * 功能: 用于前端与后端的采购换货明细数据交互
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PurchaseReturnItemDTO {
    
    /**
     * 换货单明细ID (更新时使用)
     */
    private Long id;
    
    /**
     * 原采购订单明细ID
     */
    @NotNull(message = "原采购订单明细ID不能为空")
    private Long purchaseOrderItemId;
    
    /**
     * 物料ID
     */
    @NotNull(message = "物料ID不能为空")
    private Long materialId;
    
    /**
     * 物料编码
     */
    private String materialCode;
    
    /**
     * 物料名称
     */
    private String materialName;
    
    /**
     * 物料规格
     */
    private String materialSpecification;
    
    /**
     * 物料型号
     */
    private String materialModel;
    
    /**
     * 物料单位
     */
    private String materialUnit;
    
    /**
     * 原采购数量
     */
    @NotNull(message = "原采购数量不能为空")
    @DecimalMin(value = "0.0001", message = "原采购数量必须大于0")
    private BigDecimal originalPurchaseQuantity;
    
    /**
     * 原采购单价
     */
    @NotNull(message = "原采购单价不能为空")
    @DecimalMin(value = "0.0001", message = "原采购单价必须大于0")
    private BigDecimal originalPurchasePrice;
    
    /**
     * 原采购金额
     */
    private BigDecimal originalPurchaseAmount;
    
    /**
     * 已收货数量
     */
    @NotNull(message = "已收货数量不能为空")
    @DecimalMin(value = "0.00", message = "已收货数量不能为负数")
    private BigDecimal receivedQuantity;
    
    /**
     * 待退货数量
     */
    @NotNull(message = "待退货数量不能为空")
    @DecimalMin(value = "0.00", message = "待退货数量不能为负数")
    @DecimalMax(value = "1000000.00", message = "待退货数量不能超过1000000")
    private BigDecimal returnQuantity;
    
    /**
     * 待换货数量
     */
    @NotNull(message = "待换货数量不能为空")
    @DecimalMin(value = "0.00", message = "待换货数量不能为负数")
    @DecimalMax(value = "1000000.00", message = "待换货数量不能超过1000000")
    private BigDecimal replacementQuantity;
    
    /**
     * 换货单价 (如果与原来不同)
     */
    @DecimalMin(value = "0.00", message = "换货单价不能为负数")
    private BigDecimal replacementPrice;
    
    /**
     * 换货金额
     */
    @DecimalMin(value = "0.00", message = "换货金额不能为负数")
    private BigDecimal replacementAmount;
    
    /**
     * 批次号
     */
    @Size(max = 100, message = "批次号不能超过100字符")
    private String batchNumber;
    
    /**
     * 生产日期
     */
    private LocalDateTime productionDate;
    
    /**
     * 有效期至
     */
    private LocalDateTime expiryDate;
    
    /**
     * 库存位置 (仓库/库位)
     */
    @Size(max = 200, message = "库存位置不能超过200字符")
    private String inventoryLocation;
    
    /**
     * 质量问题类型编码 (仅质量换货时使用)
     */
    private String qualityIssueCode;
    
    /**
     * 质量问题描述 (仅质量换货时使用)
     */
    @Size(max = 500, message = "质量问题描述不能超过500字符")
    private String qualityIssueDescription;
    
    /**
     * 问题等级
     */
    @Pattern(regexp = "minor|moderate|severe|critical", message = "问题等级必须是minor(轻微)、moderate(中等)、severe(严重)或critical(致命)")
    private String issueLevel;
    
    /**
     * 质量检测报告路径 (针对此物料)
     */
    private String qualityReportPath;
    
    /**
     * 换货原因 (物料级具体原因)
     */
    @NotBlank(message = "物料换货原因不能为空")
    @Size(max = 500, message = "物料换货原因不能超过500字符")
    private String itemReturnReason;
    
    /**
     * 供应商确认状态
     */
    private String supplierConfirmationStatus = "pending";
    
    /**
     * 供应商确认备注 (物料级)
     */
    @Size(max = 500, message = "供应商确认备注不能超过500字符")
    private String supplierConfirmationNote;
    
    /**
     * 退货状态
     */
    private String returnStatus = "pending";
    
    /**
     * 换货状态
     */
    private String replacementStatus = "pending";
    
    /**
     * 退货物流单号 (物料级)
     */
    @Size(max = 100, message = "退货物流单号不能超过100字符")
    private String returnTrackingNumber;
    
    /**
     * 换货物流单号 (物料级)
     */
    @Size(max = 100, message = "换货物流单号不能超过100字符")
    private String replacementTrackingNumber;
    
    /**
     * 预计退货日期
     */
    @Future(message = "预计退货日期必须是未来日期")
    private LocalDateTime expectedReturnDate;
    
    /**
     * 预计换货收货日期
     */
    @Future(message = "预计换货收货日期必须是未来日期")
    private LocalDateTime expectedReplacementDate;
    
    /**
     * 检验人ID
     */
    private Long inspectorId;
    
    /**
     * 检验人姓名
     */
    private String inspectorName;
    
    /**
     * 检验日期
     */
    private LocalDateTime inspectionDate;
    
    /**
     * 检验结果
     */
    @Pattern(regexp = "qualified|unqualified|conditional", message = "检验结果必须是qualified(合格)、unqualified(不合格)或conditional(有条件接受)")
    private String inspectionResult;
    
    /**
     * 检验备注
     */
    @Size(max = 500, message = "检验备注不能超过500字符")
    private String inspectionNote;
    
    /**
     * 备注
     */
    @Size(max = 1000, message = "备注不能超过1000字符")
    private String remark;
    
    /**
     * 验证方法：确保换货数量合理性
     */
    public void validateQuantities() {
        if (returnQuantity.compareTo(receivedQuantity) > 0) {
            throw new IllegalArgumentException("待退货数量不能超过已收货数量");
        }
        if (replacementQuantity.compareTo(receivedQuantity) > 0) {
            throw new IllegalArgumentException("待换货数量不能超过已收货数量");
        }
    }
    
    /**
     * 计算换货金额
     */
    public BigDecimal calculateReplacementAmount() {
        if (replacementPrice != null && replacementPrice.compareTo(BigDecimal.ZERO) > 0) {
            return replacementPrice.multiply(replacementQuantity);
        } else {
            return originalPurchasePrice.multiply(replacementQuantity);
        }
    }
    
    /**
     * 验证换货单价合理性
     */
    public void validateReplacementPrice() {
        if (replacementPrice != null && replacementPrice.compareTo(BigDecimal.ZERO) > 0) {
            // 换货单价不能超过原单价的200%
            BigDecimal maxPrice = originalPurchasePrice.multiply(new BigDecimal("2.0"));
            if (replacementPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException("换货单价不能超过原单价的200%");
            }
        }
    }
}