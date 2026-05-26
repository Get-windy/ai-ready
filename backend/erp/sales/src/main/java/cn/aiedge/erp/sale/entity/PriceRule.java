package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格规则实体
 * 定义具体的价格计算规则，可关联到价格策略
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_pricing_rule")
public class PriceRule {

    /**
     * 规则ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 所属策略ID
     */
    private Long strategyId;

    /**
     * 规则类型：quantity_discount-数量折扣/customer_discount-客户折扣/
     * region_factor-区域系数/time_factor-时间系数/additional_charge-附加费用
     */
    private String ruleType;

    /**
     * 条件表达式（如：quantity >= 100）
     */
    private String conditionExpression;

    /**
     * 计算表达式（如：basePrice * 0.95）
     */
    private String calculationExpression;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 价格系数
     */
    private BigDecimal priceFactor;

    /**
     * 固定金额
     */
    private BigDecimal fixedAmount;

    /**
     * 最小数量
     */
    private Integer minQuantity;

    /**
     * 最大数量
     */
    private Integer maxQuantity;

    /**
     * 最小金额
     */
    private BigDecimal minAmount;

    /**
     * 最大金额
     */
    private BigDecimal maxAmount;

    /**
     * 排序号（规则执行顺序）
     */
    private Integer sortOrder;

    /**
     * 状态：active-生效/inactive-失效
     */
    private String status;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
