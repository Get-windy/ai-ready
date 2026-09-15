package cn.aiedge.dms.settlement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送结算单明细（费用构成，用于「这 500 元由哪些单构成」的追溯与对账）
 */
@Data
@TableName("dms_settlement_item")
public class DmsSettlementItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long settlementId;

    private Long taskId;

    private String taskNo;

    /** 签收时间（计费触发点） */
    private LocalDateTime signTime;

    private BigDecimal distanceKm;

    private BigDecimal baseFee;

    private BigDecimal mileageFee;

    /** 重量费（按重量/组合计价时的每公斤费用） */
    private BigDecimal weightFee;

    private BigDecimal timeSurcharge;

    private BigDecimal urgentSurcharge;

    private BigDecimal totalFee;

    /** 签收类型：1-正常 2-部分签收 3-拒收（拒收不计费） */
    private Integer signType;

    /** 应签收数量（提交签收时的任务总量快照） */
    private BigDecimal plannedQuantity;

    /** 实际签收数量（部分签收按实际数量计费） */
    private BigDecimal actualQuantity;

    /** 计费系数（部分签收 = 实签收/应签收，作用于里程费/夜间/加急；起步价全收） */
    private BigDecimal billingRatio;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
