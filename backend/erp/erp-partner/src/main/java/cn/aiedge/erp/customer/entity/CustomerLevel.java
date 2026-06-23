package cn.aiedge.erp.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 客户等级实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_customer_level")
public class CustomerLevel {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;
    
    /**
     * 等级名称
     */
    @TableField("level_name")
    private String levelName;
    
    /**
     * 等级代码
     */
    @TableField("level_code")
    private String levelCode;
    
    /**
     * 等级描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 最小交易金额阈值
     */
    @TableField("min_amount")
    private Double minAmount;
    
    /**
     * 最大交易金额阈值（null表示无上限）
     */
    @TableField("max_amount")
    private Double maxAmount;
    
    /**
     * 最小交易频次阈值
     */
    @TableField("min_frequency")
    private Integer minFrequency;
    
    /**
     * 最小回款及时率阈值（0-100）
     */
    @TableField("min_payment_rate")
    private Integer minPaymentRate;
    
    /**
     * 折扣率（0-100，表示百分比）
     */
    @TableField("discount_rate")
    private Integer discountRate;
    
    /**
     * 账期天数
     */
    @TableField("credit_days")
    private Integer creditDays;
    
    /**
     * 排序权重
     */
    @TableField("sort_weight")
    private Integer sortWeight;
    
    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}