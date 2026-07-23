package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加价购规则
 */
@Data
@Accessors(chain = true)
@TableName("mkt_addon_rule")
public class AddonRule {

    /** 状态: 0=停用 1=启用 */
    public static final int STATUS_DISABLED = 0;
    public static final int STATUS_ENABLED = 1;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 规则名称 */
    private String ruleName;

    /** 主商品 */
    private Long mainProductId;
    private String mainProductName;

    /** 加价购商品 */
    private Long addonProductId;
    private String addonProductName;

    /** 加价金额 */
    private BigDecimal addonPrice;
    /** 每单限制数量 */
    private Integer maxPerOrder;

    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;

    /** 状态: 0=停用 1=启用 */
    private Integer status;
    private Integer sort;
    private String remark;

    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
