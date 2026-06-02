package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 财务账户实体
 * 用于管理企业财务账户信息
 */
@Data
@TableName("finance_account")
@EqualsAndHashCode(callSuper = true)
public class FinanceAccount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 账户名称
     */
    @TableField("account_name")
    private String accountName;

    /**
     * 账户类型
     * 1-银行账户 2-现金账户 3-内部账户 4-外部账户
     */
    @TableField("account_type")
    private Integer accountType;

    /**
     * 开户银行
     */
    @TableField("bank_name")
    private String bankName;

    /**
     * 银行账号
     */
    @TableField("bank_account")
    private String bankAccount;

    /**
     * 账户余额
     */
    @TableField("balance")
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * 账户状态
     * 0-停用 1-启用
     */
    @TableField("status")
    private Integer status = 1;

    /**
     * 账户币种
     */
    @TableField("currency")
    private String currency = "CNY";

    /**
     * 账户等级
     * 1-基本账户 2-一般账户 3-专用账户
     */
    @TableField("account_level")
    private Integer accountLevel;
}
