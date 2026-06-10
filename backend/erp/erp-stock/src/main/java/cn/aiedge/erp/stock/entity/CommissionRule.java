package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("erp_commission_rule")
public class CommissionRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String ruleCode;
    private String ruleName;
    private String commissionType;
    private String calcBasis;
    private String calcMethod;
    private BigDecimal commissionValue;
    private BigDecimal maxCommission;
    private BigDecimal minOrderAmount;
    private String applicableProducts;
    private String applicablePartnerGrades;
    private String status;
    private String remark;
    @TableLogic
    private Integer deleted;
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
