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
 * 配送结算单（按周期 + 结算对象锁定已签收任务的计费结果）
 *
 * <p>状态流转：0 草稿 → 1 已确认（锁定金额）→ 2 已推送（终态，推送幂等）；
 * 计费规则以生成时的 `ruleSnapshot` 留痕，配置化调价后可回溯历史。</p>
 */
@Data
@TableName("dms_settlement")
public class DmsSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 结算单号 JSD-YYYYMMDD-序号 */
    private String settlementNo;

    /** 1-配送员 2-渠道 */
    private Integer targetType;

    private Long targetId;

    private String targetName;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    /** 单量 */
    private Integer taskCount;

    /** 结算金额 */
    private BigDecimal totalAmount;

    /** 0-草稿 1-已确认 2-已推送 */
    private Integer status;

    private LocalDateTime pushTime;

    private Integer pushCount;

    private String pushTraceId;

    /** 计费规则快照 */
    private String ruleSnapshot;

    /** 生成时命中的计费规则ID（NULL = 用全局缺省费率） */
    private Long ruleId;

    /** 推送 ERP 生成的记账凭证号（KJPZ-…，幂等凭据） */
    private String erpVoucherNo;

    /** 推送 ERP 生成的应付单ID（外部运力/渠道结算；对账查核销状态用） */
    private Long erpPayableId;

    /** 推送 ERP 生成的应付单号 */
    private String erpPayableNo;

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
