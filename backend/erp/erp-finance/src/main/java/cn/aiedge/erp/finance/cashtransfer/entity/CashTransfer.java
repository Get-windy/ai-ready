package cn.aiedge.erp.finance.cashtransfer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 提存（提存现金转账）主表实体
 * 资金在企业账户间移动（银行提现、现金存行、账户互转），不涉及往来单位。
 * 单号前缀 YHZKD-。状态 0-草稿 1-已记账 2-已取消。
 */
@Data
@Accessors(chain = true)
@TableName("erp_cash_transfer")
public class CashTransfer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 单号 YHZKD-YYYYMMDD-序号 */
    private String docNo;

    /** 单据日期 */
    private LocalDate docDate;

    /** 转出账户ID */
    private Long fromAccountId;

    /** 转出账户名称 */
    private String fromAccountName;

    /** 转出账户类型(1银行 2现金 3内部 4外部) */
    private Integer fromAccountType;

    /** 转出账户科目编码(凭证用) */
    private String fromSubjectCode;

    /** 转出金额 = Σ转入金额 + 手续费 */
    private BigDecimal fromAmount;

    /** 手续费 */
    private BigDecimal fee;

    /** 转入金额合计 = Σ转入账户明细金额 */
    private BigDecimal toAmount;

    /** 本单金额 = 转出金额 = Σ转入 + 手续费 */
    private BigDecimal totalAmount;

    /** 经手人 */
    private Long handlerId;

    private String handlerName;

    /** 部门 */
    private Long deptId;

    private String deptName;

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
