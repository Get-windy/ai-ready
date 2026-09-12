package cn.aiedge.erp.finance.arapadjust.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应收应付调整-科目明细实体
 * 明细"科目编号/科目名称"：调整的对转科目（差额去向，如营业外收入/营业外支出等），金额可多行拆分。
 */
@Data
@Accessors(chain = true)
@TableName("erp_ar_ap_adjust_item")
public class ArApAdjustItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 主表ID（关联 erp_ar_ap_adjust.id） */
    private Long adjustId;

    /** 行号 */
    private Integer lineNo;

    /** 科目编号（实现对转科目） */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 金额 */
    private BigDecimal amount;

    /** 备注 */
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

    @Version
    private Integer versionNo;
}
