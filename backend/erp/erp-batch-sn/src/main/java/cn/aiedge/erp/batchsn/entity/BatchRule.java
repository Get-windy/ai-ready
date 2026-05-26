package cn.aiedge.erp.batchsn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 批次规则配置表
 * 
 * @author devops-engineer
 * @date 2026-05-05
 */
@Data
@TableName("batch_rule")
public class BatchRule {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;
    
    /**
     * 规则编码
     */
    @TableField("rule_code")
    private String ruleCode;
    
    /**
     * 批次号前缀
     */
    @TableField("prefix")
    private String prefix;
    
    /**
     * 日期格式
     */
    @TableField("date_format")
    private String dateFormat;
    
    /**
     * 序号长度
     */
    @TableField("seq_length")
    private Integer seqLength;
    
    /**
     * 序号起始值
     */
    @TableField("seq_start")
    private Integer seqStart;
    
    /**
     * 默认有效期（天）
     */
    @TableField("default_expiry_days")
    private Integer defaultExpiryDays;
    
    /**
     * 自动过期
     */
    @TableField("auto_expiry")
    private Boolean autoExpiry;
    
    /**
     * 临期提醒天数
     */
    @TableField("expiry_warning_days")
    private Integer expiryWarningDays;
    
    /**
     * 需要质检
     */
    @TableField("require_quality_check")
    private Boolean requireQualityCheck;
    
    /**
     * 质检周期（天）
     */
    @TableField("quality_check_interval_days")
    private Integer qualityCheckIntervalDays;
    
    /**
     * 状态流转JSON配置
     */
    @TableField("status_flow")
    private String statusFlow;
    
    /**
     * 适用产品分类ID列表
     */
    @TableField("product_category_ids")
    private String productCategoryIds;
    
    /**
     * 是否启用
     */
    @TableField("enable")
    private Boolean enable;
    
    /**
     * 创建人ID
     */
    @TableField("created_by")
    private String createdBy;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新人ID
     */
    @TableField("updated_by")
    private String updatedBy;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 业务方法：判断是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enable);
    }
    
    /**
     * 业务方法：判断是否需要质检
     */
    public boolean requiresQualityCheck() {
        return Boolean.TRUE.equals(requireQualityCheck);
    }
    
    /**
     * 业务方法：判断是否自动过期
     */
    public boolean isAutoExpiryEnabled() {
        return Boolean.TRUE.equals(autoExpiry);
    }
}