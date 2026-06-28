package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本分摊规则
 */
@Data
@TableName("erp_cost_allocation_rule")
public class CostAllocationRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;
    private String ruleCode;
    private String ruleName;
    private String allocationType; // DEPT, PRODUCT, ORDER
    private BigDecimal allocationRatio;
    private String targetAccount;
    private String description;
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
