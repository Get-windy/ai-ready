package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合作商等级定义实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_level")
@Entity
@Table(name = "erp_supplier_level", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_level_code", columnList = "level_code"),
    @Index(name = "idx_score_range", columnList = "min_score, max_score")
})
public class SupplierLevelEntity {
    
    /**
     * 主键ID
     */
    @Id
    @TableId(value = "id", type = IdType.AUTO)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 租户ID，用于多租户数据隔离
     */
    @TableField("tenant_id")
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;
    
    /**
     * 等级编码，唯一标识
     */
    @TableField("level_code")
    @Column(name = "level_code", nullable = false, unique = true, length = 20)
    private String levelCode;
    
    /**
     * 等级名称
     */
    @TableField("level_name")
    @Column(name = "level_name", nullable = false, length = 50)
    private String levelName;
    
    /**
     * 等级描述
     */
    @TableField("level_description")
    @Column(name = "level_description", length = 500)
    private String levelDescription;
    
    /**
     * 等级图标
     */
    @TableField("level_icon")
    @Column(name = "level_icon", length = 200)
    private String levelIcon;
    
    /**
     * 等级颜色
     */
    @TableField("level_color")
    @Column(name = "level_color", length = 20)
    private String levelColor;
    
    /**
     * 最低分数
     */
    @TableField("min_score")
    @Column(name = "min_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal minScore;
    
    /**
     * 最高分数
     */
    @TableField("max_score")
    @Column(name = "max_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxScore;
    
    /**
     * 评估周期：MONTHLY-月度，QUARTERLY-季度，YEARLY-年度
     */
    @TableField("evaluation_period")
    @Column(name = "evaluation_period", length = 20)
    private String evaluationPeriod;
    
    /**
     * 权益数量
     */
    @TableField("benefit_count")
    @Column(name = "benefit_count")
    private Integer benefitCount;
    
    /**
     * 最大折扣率
     */
    @TableField("max_discount_rate")
    @Column(name = "max_discount_rate", precision = 5, scale = 2)
    private BigDecimal maxDiscountRate;
    
    /**
     * 优先级
     */
    @TableField("priority_level")
    @Column(name = "priority_level")
    private Integer priorityLevel;
    
    /**
     * 是否启用
     */
    @TableField("is_active")
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * 排序序号
     */
    @TableField("sort_order")
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    /**
     * 创建人
     */
    @TableField("created_by")
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 创建时间
     */
    @TableField("created_time")
    @Column(name = "created_time")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 更新人
     */
    @TableField("updated_by")
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    /**
     * 更新时间
     */
    @TableField("updated_time")
    @Column(name = "updated_time")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
    
    /**
     * 版本号，用于乐观锁
     */
    @Version
    @TableField("version")
    @Column(name = "version")
    private Integer version;
    
    /**
     * 逻辑删除标识
     */
    @TableLogic
    @TableField("deleted")
    @Column(name = "deleted")
    private Integer deleted;
    
    /**
     * 扩展字段（JSON格式）
     */
    @TableField("extend_info")
    @Column(name = "extend_info", columnDefinition = "TEXT")
    private String extendInfo;
}