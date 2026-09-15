package cn.aiedge.dms.task.entity;

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
 * 配送单内上游单据关联（配送单 1:N 上游单据）
 *
 * <p>建模口径（对齐 SAP TM 的 Freight Order ← Freight Unit ← Delivery 分层）：
 * 配送单是**运输执行单**，只承载「运什么（汇总装载量）+ 怎么运（承运资源/路线/时间）」；
 * 货权、库存与金额口径归上游单据（销售出库单等），本表保存其快照与引用，
 * 配送单表头的数量/金额/重量/体积一律由本表聚合，保证单一数据源。</p>
 */
@Data
@TableName("dms_task_doc")
public class DmsTaskDoc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 配送任务ID */
    private Long taskId;

    /** 1-销售出库单 2-销售退货单 3-调拨单 */
    private Integer docType;

    /** 上游单据主键（用于穿透查看明细） */
    private Long docId;

    /** 上游单据编号（XSCKD- / XSCK…） */
    private String docNo;

    private LocalDate docDate;

    private String customerName;

    /** 该单据发货数量快照 */
    private BigDecimal quantity;

    /** 该单据发货金额快照（货值口径归上游，配送单不重算） */
    private BigDecimal amount;

    private BigDecimal weight;

    private BigDecimal volume;

    /** 该单据装箱数量快照 */
    private Integer boxCount;

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
}
