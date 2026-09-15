package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 承运商运费规则
 *
 * <p>按「承运商 × 区域 × 重量区间」取首重/续重计费，用于发货时试算运费与事后对账
 * （对齐用友 U8「运费维护/计费规则」与 TMS 的承运商合同价）。</p>
 *
 * <p>匹配优先级：承运商+区域 &gt; 承运商 &gt; 区域 &gt; 通用；同级按 priority 降序、区间取包含重量的那条。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_freight_rule")
public class FreightRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 承运商（物流公司档案ID；为空=通用规则） */
    private Long carrierId;
    private String carrierName;

    /** 区域/线路（为空=通用） */
    private String area;

    /** 重量区间（含下界、不含上界；max 为空视为无穷） */
    private BigDecimal minWeight;
    private BigDecimal maxWeight;

    /** 首重(kg) / 首重价(元) */
    private BigDecimal firstWeight;
    private BigDecimal firstPrice;

    /** 续重步长(kg) / 每步长单价(元) */
    private BigDecimal addStep;
    private BigDecimal addPrice;

    /** 基础费(元)：在首重价之上叠加 */
    private BigDecimal baseFee;

    private Integer enabled;
    private Integer priority;
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long createBy;
    private Long updateBy;

    @Version
    private Integer version;
}
