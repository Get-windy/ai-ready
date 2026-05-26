package cn.aiedge.erp.supplier.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分累计记录实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("erp_supplier_points_record")
@Entity
@Table(name = "erp_supplier_points_record", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_supplier_id", columnList = "supplier_id"),
    @Index(name = "idx_rule_code", columnList = "rule_code"),
    @Index(name = "idx_record_type", columnList = "record_type"),
    @Index(name = "idx_record_date", columnList = "record_date"),
    @Index(name = "idx_expire_time", columnList = "expire_time")
})
public class SupplierPointsRecordEntity {
    
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
     * 供应商ID
     */
    @TableField("supplier_id")
    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
    
    /**
     * 供应商编码
     */
    @TableField("supplier_code")
    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;
    
    /**
     * 规则编码
     */
    @TableField("rule_code")
    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;
    
    /**
     * 记录类型：ACCUMULATION-累计，CONSUMPTION-消耗，ADJUSTMENT-调整，EXPIRE-过期
     */
    @TableField("record_type")
    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;
    
    /**
     * 积分数量
     */
    @TableField("points_amount")
    @Column(name = "points_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal pointsAmount;
    
    /**
     * 记录描述
     */
    @TableField("record_description")
    @Column(name = "record_description", length = 500)
    private String recordDescription;
    
    /**
     * 关联业务类型：ORDER-订单，QUALITY-质量，DELIVERY-交付，SERVICE-服务
     */
    @TableField("related_business_type")
    @Column(name = "related_business_type", length = 20)
    private String relatedBusinessType;
    
    /**
     * 关联业务ID
     */
    @TableField("related_business_id")
    @Column(name = "related_business_id", length = 50)
    private String relatedBusinessId;
    
    /**
     * 关联业务编码
     */
    @TableField("related_business_code")
    @Column(name = "related_business_code", length = 50)
    private String relatedBusinessCode;
    
    /**
     * 业务发生时间
     */
    @TableField("business_time")
    @Column(name = "business_time")
    private LocalDateTime businessTime;
    
    /**
     * 记录时间
     */
    @TableField("record_date")
    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;
    
    /**
     * 生效时间
     */
    @TableField("effective_time")
    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;
    
    /**
     * 过期时间
     */
    @TableField("expire_time")
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
    
    /**
     * 当前余额
     */
    @TableField("current_balance")
    @Column(name = "current_balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentBalance;
    
    /**
     * 等级变化前等级
     */
    @TableField("before_level_code")
    @Column(name = "before_level_code", length = 20)
    private String beforeLevelCode;
    
    /**
     * 等级变化后等级
     */
    @TableField("after_level_code")
    @Column(name = "after_level_code", length = 20)
    private String afterLevelCode;
    
    /**
     * 操作人ID
     */
    @TableField("operator_id")
    @Column(name = "operator_id", length = 50)
    private String operatorId;
    
    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    @Column(name = "operator_name", length = 50)
    private String operatorName;
    
    /**
     * 审批状态：PENDING-待审批，APPROVED-已审批，REJECTED-已拒绝
     */
    @TableField("approval_status")
    @Column(name = "approval_status", length = 20)
    private String approvalStatus;
    
    /**
     * 审批人ID
     */
    @TableField("approver_id")
    @Column(name = "approver_id", length = 50)
    private String approverId;
    
    /**
     * 审批人姓名
     */
    @TableField("approver_name")
    @Column(name = "approver_name", length = 50)
    private String approverName;
    
    /**
     * 审批时间
     */
    @TableField("approval_time")
    @Column(name = "approval_time")
    private LocalDateTime approvalTime;
    
    /**
     * 审批意见
     */
    @TableField("approval_comment")
    @Column(name = "approval_comment", length = 500)
    private String approvalComment;
    
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