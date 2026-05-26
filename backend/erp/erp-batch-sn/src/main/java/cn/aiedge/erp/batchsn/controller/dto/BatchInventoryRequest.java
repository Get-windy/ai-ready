package cn.aiedge.erp.batchsn.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批次盘点请求
 * 
 * @author team-member
 * @date 2026-05-05
 */
@Data
public class BatchInventoryRequest {
    
    /**
     * 批次ID
     */
    @NotNull(message = "批次ID不能为空")
    private Long batchId;
    
    /**
     * 实际盘点数量
     */
    @NotNull(message = "实际盘点数量不能为空")
    private BigDecimal physicalQuantity;
    
    /**
     * 调整数量（正数为增加，负数为减少）
     */
    private BigDecimal adjustmentQuantity;
    
    /**
     * 调整原因
     */
    @NotNull(message = "调整原因不能为空")
    private String adjustmentReason;
    
    /**
     * 操作人ID
     */
    @NotNull(message = "操作人ID不能为空")
    private String operatorId;
    
    /**
     * 操作人姓名
     */
    @NotNull(message = "操作人姓名不能为空")
    private String operatorName;
    
    /**
     * 验证请求参数
     */
    public void validate() {
        if (physicalQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("实际盘点数量不能为负数");
        }
        if (adjustmentQuantity != null) {
            if (adjustmentQuantity.compareTo(BigDecimal.ZERO) == 0) {
                adjustmentQuantity = null; // 调整量为0时设为null
            } else if (physicalQuantity.compareTo(BigDecimal.ZERO) == 0 && adjustmentQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("调整数量不能使盘点结果为负数");
            }
        }
    }
    
    /**
     * 计算调整数量（如果未提供，则根据物理数量和系统数量计算）
     */
    public BigDecimal calculateAdjustment(BigDecimal systemQuantity) {
        if (adjustmentQuantity != null) {
            return adjustmentQuantity;
        }
        return physicalQuantity.subtract(systemQuantity);
    }
}