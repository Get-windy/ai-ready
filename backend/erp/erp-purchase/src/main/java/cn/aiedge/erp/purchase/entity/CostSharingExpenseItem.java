package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购费用分摊-费用单明细
 *
 * 对应表单页"费用单明细表"（9列：操作、费用单编号、往来单位、往来单位编码、
 * 结算单位编号、结算单位、费用项、费用金额、备注），数据来源于所选费用单。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_cost_sharing_expense_item")
public class CostSharingExpenseItem {

    /** 明细ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分摊单ID */
    private Long costSharingId;

    /** 费用单编号 */
    private String expenseNo;

    /** 往来单位ID */
    private Long partnerId;

    /** 往来单位名称 */
    private String partnerName;

    /** 往来单位编码 */
    private String partnerCode;

    /** 结算单位编号 */
    private String settleUnitId;

    /** 结算单位 */
    private String settleUnit;

    /** 费用项 */
    private String expenseType;

    /** 费用金额 */
    private BigDecimal expenseAmount;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
