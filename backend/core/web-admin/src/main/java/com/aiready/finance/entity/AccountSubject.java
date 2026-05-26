package com.aiready.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会计科目实体类
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("fin_account_subject")
public class AccountSubject {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 科目编码
     */
    private String subjectCode;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
    /**
     * 科目类别（1：资产 2：负债 3：权益 4：成本 5：损益）
     */
    private Integer subjectType;
    
    /**
     * 科目级别（1-5级）
     */
    private Integer subjectLevel;
    
    /**
     * 上级科目ID
     */
    private Long parentId;
    
    /**
     * 借贷方向（1：借 2：贷）
     */
    private Integer balanceDirection;
    
    /**
     * 期初余额
     */
    private BigDecimal openingBalance;
    
    /**
     * 当前余额
     */
    private BigDecimal currentBalance;
    
    /**
     * 是否明细科目（0：否 1：是）
     */
    private Integer isDetail;
    
    /**
     * 辅助核算类型（1：客户 2：供应商 3：部门 4：项目 5：员工）
     */
    private String auxiliaryTypes;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标志（0：未删除 1：已删除）
     */
    @TableLogic
    private Integer deleted;
}
