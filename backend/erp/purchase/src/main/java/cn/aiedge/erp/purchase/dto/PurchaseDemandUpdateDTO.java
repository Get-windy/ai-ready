package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购需求更新DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDemandUpdateDTO {
    
    /**
     * 需求ID
     */
    @NotNull(message = "需求ID不能为空")
    private Long id;
    
    /**
     * 需求标题
     */
    private String title;
    
    /**
     * 需求描述
     */
    private String description;
    
    /**
     * 需求数量
     */
    @Positive(message = "需求数量必须大于0")
    private BigDecimal demandQuantity;
    
    /**
     * 计量单位
     */
    private String unit;
    
    /**
     * 需求日期
     */
    private LocalDateTime demandDate;
    
    /**
     * 期望到货日期
     */
    private LocalDateTime expectedDeliveryDate;
    
    /**
     * 建议单价
     */
    private BigDecimal suggestedUnitPrice;
    
    /**
     * 预算金额
     */
    private BigDecimal budgetAmount;
    
    /**
     * 需求部门ID
     */
    private Long departmentId;
    
    /**
     * 需求部门名称
     */
    private String departmentName;
    
    /**
     * 优先级（1-低，2-中，3-高，4-紧急）
     */
    private Integer priority;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 扩展字段（JSON格式存储额外信息）
     */
    private String extInfo;
    
    /**
     * 版本号（乐观锁）
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;
}