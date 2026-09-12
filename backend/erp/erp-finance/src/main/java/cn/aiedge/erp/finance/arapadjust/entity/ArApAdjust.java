package cn.aiedge.erp.finance.arapadjust.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收应付调整主表实体
 * 不动资金账户的往来余额调整（应收增加/应收减少/应付增加/应付减少）。
 * 单号前缀 YSKZJ-。状态 0-草稿 1-已记账 2-已取消。
 * 方向 1应收增加 2应收减少 3应付增加 4应付减少；应收方向结算单位=客户，应付方向=供应商。
 */
@Data
@Accessors(chain = true)
@TableName("erp_ar_ap_adjust")
public class ArApAdjust {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 单号 YSKZJ-YYYYMMDD-序号 */
    private String docNo;

    /** 单据日期 */
    private LocalDate docDate;

    /** 调整方向 1应收增加 2应收减少 3应付增加 4应付减少 */
    private Integer direction;

    /** 调整方向显示名（应收增加等） */
    private String directionName;

    /** 结算单位类型 customer-客户 supplier-供应商 */
    private String partnerType;

    /** 结算单位ID */
    private Long partnerId;

    /** 结算单位编号 */
    private String partnerCode;

    /** 结算单位名称 */
    private String partnerName;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    /** 部门 */
    private Long deptId;

    private String deptName;

    /** 本单金额 = Σ明细金额 */
    private BigDecimal totalAmount;

    /** 状态 0-草稿 1-已记账 2-已取消 */
    private Integer status;

    /** 制单人 */
    private String creatorName;

    /** 记账人 */
    private Long bookkeeperId;

    private String bookkeeperName;

    /** 记账时间 */
    private LocalDateTime bookkeepingTime;

    /** 摘要 */
    private String summary;

    /** 附件 */
    private String attachment;

    /** 单据备注 */
    private String remark;

    /** 打印次数 */
    private Integer printCount;

    /** 红冲标记 */
    private Integer redFlag;

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
