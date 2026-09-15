package cn.aiedge.dms.settlement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 配送计费规则（按 结算对象 + 渠道/线路 + 生效期 差异化计价）
 *
 * <p>算费时按「启用 + 生效期内 + 适用范围匹配」筛选，取优先级最小（更具体者优先）的一条**整体**计费；
 * 未命中任何规则时回落《配送参数》的全局缺省费率（`dms.settlement.*`），保证规则表为空时仍可算费。</p>
 */
@Data
@TableName("dms_settlement_rule")
public class DmsSettlementRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 规则编码（租户内唯一） */
    private String ruleCode;

    private String ruleName;

    /** 适用结算对象：1-配送员 2-渠道 */
    private Integer targetType;

    /** 适用渠道ID（dms_channel.id，NULL=不限） */
    private Long channelId;

    /** 适用线路ID（erp_route.id，NULL=不限） */
    private Long routeId;

    /** 计价方式：1-按单 2-按距离 3-按重量 4-组合（距离+重量） */
    private Integer billingType;

    /** 起步价（元） */
    private BigDecimal baseFee;

    /** 免费里程（公里） */
    private BigDecimal freeDistanceKm;

    /** 每公里单价（元/公里） */
    private BigDecimal perKmRate;

    /** 每公斤单价（元/公斤，按重量计价时使用） */
    private BigDecimal perKgRate;

    /** 夜间时段附加系数（22:00-06:00，按起步价倍数） */
    private BigDecimal timeSurchargeRate;

    /** 加急附加费（优先级≥2，元） */
    private BigDecimal urgentSurcharge;

    /** 结算周期：1-日结 2-周结 3-月结 */
    private Integer settleCycle;

    /** 生效开始日期 */
    private LocalDate effectiveStart;

    /** 生效结束日期 */
    private LocalDate effectiveEnd;

    /** 优先级（数字越小越优先） */
    private Integer priority;

    /** 0-停用 1-启用 */
    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    private Integer version;
}
