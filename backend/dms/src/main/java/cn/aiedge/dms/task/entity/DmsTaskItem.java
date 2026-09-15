package cn.aiedge.dms.task.entity;

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
 * 配送单商品明细（dms_task 的明细行）
 */
@Data
@TableName("dms_task_item")
public class DmsTaskItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 配送任务ID */
    private Long taskId;

    /** 行号（从 1 开始） */
    private Integer lineNo;

    private Long productId;

    private String productCode;

    private String productName;

    private String barcode;

    /** 规格 */
    private String spec;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    /** 金额 = 数量 × 单价 */
    private BigDecimal amount;

    /** 重量(kg) */
    private BigDecimal weight;

    /** 体积(m³) */
    private BigDecimal volume;

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
