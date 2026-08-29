package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购费用分摊单主表
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_cost_sharing")
public class CostSharing {

    /** 分摊单ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 分摊单号 */
    private String sharingNo;

    /** 分摊日期 */
    private LocalDate sharingDate;

    /** 分摊方式（amount/quantity/weight） */
    private String allocationMethod;

    /** 费用类型 */
    private String expenseType;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 经手人ID */
    private Long handlerId;

    /** 经手人名称 */
    private String handlerName;

    /** 部门ID */
    private Long departmentId;

    /** 部门名称 */
    private String departmentName;

    /** 摘要 */
    private String summary;

    /** 附件 */
    private Integer attachment;

    /** 单据状态（0-草稿 1-已完成 2-已取消） */
    private Integer status;

    /** 记账人ID */
    private Long bookkeeperId;

    /** 记账人名称 */
    private String bookkeeperName;

    /** 记账时间 */
    private LocalDateTime accountTime;

    /** 分摊总金额 */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 制单人名称 */
    private String createByName;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
