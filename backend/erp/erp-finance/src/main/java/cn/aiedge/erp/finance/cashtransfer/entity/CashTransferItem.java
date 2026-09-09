package cn.aiedge.erp.finance.cashtransfer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提存-转入账户明细实体
 * 一单多转入账户：可填多行不同转入账户（现金/银行/内部账户等），本单金额=Σ转入金额+手续费。
 */
@Data
@Accessors(chain = true)
@TableName("erp_cash_transfer_item")
public class CashTransferItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 主表ID */
    private Long transferId;

    /** 行号 */
    private Integer lineNo;

    /** 转入账户ID */
    private Long toAccountId;

    /** 转入账户编号(账号) */
    private String toAccountNo;

    /** 转入账户名称 */
    private String toAccountName;

    /** 转入账户类型(1银行 2现金 3内部 4外部) */
    private Integer toAccountType;

    /** 转入账户科目编码(凭证用) */
    private String toSubjectCode;

    /** 转入金额 */
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
