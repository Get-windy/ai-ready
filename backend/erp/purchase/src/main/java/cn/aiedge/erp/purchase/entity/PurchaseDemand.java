package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购需求实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("purchase_demand")
public class PurchaseDemand {
    
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 需求编号
     */
    private String demandNo;
    
    /**
     * 需求标题
     */
    private String title;
    
    /**
     * 需求描述
     */
    private String description;
    
    /**
     * 物料ID
     */
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
     * 需求数量
     */
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
     * 需求人ID
     */
    private Long demandUserId;
    
    /**
     * 需求人姓名
     */
    private String demandUserName;
    
    /**
     * 优先级（1-低，2-中，3-高，4-紧急）
     */
    private Integer priority;
    
    /**
     * 需求状态（1-草稿，2-已提交，3-已审批，4-已转为询价，5-已取消）
     */
    private Integer status;
    
    /**
     * 审批状态（1-待审批，2-审批中，3-已批准，4-已拒绝）
     */
    private Integer approvalStatus;
    
    /**
     * 审批人ID
     */
    private Long approvedBy;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvedTime;
    
    /**
     * 审批意见
     */
    private String approvalComment;
    
    /**
     * 关联的询价单ID
     */
    private Long inquiryId;
    
    /**
     * 关联的采购订单ID
     */
    private Long orderId;
    
    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer deleted;
    
    /**
     * 版本号（乐观锁）
     */
    @Version
    private Integer version;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 扩展字段（JSON格式存储额外信息）
     */
    private String extInfo;
}