package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购需求创建DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseDemandCreateDTO {
    
    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    /**
     * 需求标题
     */
    @NotBlank(message = "需求标题不能为空")
    private String title;
    
    /**
     * 需求描述
     */
    private String description;
    
    /**
     * 物料ID
     */
    @NotNull(message = "物料ID不能为空")
    private Long materialId;
    
    /**
     * 物料编码
     */
    @NotBlank(message = "物料编码不能为空")
    private String materialCode;
    
    /**
     * 物料名称
     */
    @NotBlank(message = "物料名称不能为空")
    private String materialName;
    
    /**
     * 需求数量
     */
    @NotNull(message = "需求数量不能为空")
    @Positive(message = "需求数量必须大于0")
    private BigDecimal demandQuantity;
    
    /**
     * 计量单位
     */
    @NotBlank(message = "计量单位不能为空")
    private String unit;
    
    /**
     * 需求日期
     */
    @NotNull(message = "需求日期不能为空")
    private LocalDateTime demandDate;
    
    /**
     * 期望到货日期
     */
    @NotNull(message = "期望到货日期不能为空")
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
    @NotNull(message = "需求部门ID不能为空")
    private Long departmentId;
    
    /**
     * 需求部门名称
     */
    @NotBlank(message = "需求部门名称不能为空")
    private String departmentName;
    
    /**
     * 优先级（1-低，2-中，3-高，4-紧急）
     */
    @NotNull(message = "优先级不能为空")
    private Integer priority;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 扩展字段（JSON格式存储额外信息）
     */
    private String extInfo;
}