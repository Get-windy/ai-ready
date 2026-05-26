package cn.aiedge.erp.finance.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务账户实体
 * 用于管理企业.GetAll财务账户信息
 */
@Data
@Entity
@Table(name = "finance_account")
@EqualsAndHashCode(callSuper = true)
public class FinanceAccount extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 账户名称
     */
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;
    
    /**
     * 账户类型
     * 1-银行账户 2-现金账户 3-内部账户 4-外部账户
     */
    @Column(name = "account_type", nullable = false)
    private Integer accountType;
    
    /**
     * 开户银行
     */
    @Column(name = "bank_name", length = 200)
    private String bankName;
    
    /**
     * 银行账号
     */
    @Column(name = "bank_account", length = 50)
    private String bankAccount;
    
    /**
     * 账户余额
     */
    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private Double balance = 0.0;
    
    /**
     * 账户状态
     * 0-停用 1-启用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
    
    /**
     * 账户币种
     */
    @Column(name = "currency", length = 10)
    private String currency = "CNY";
    
    /**
     * 账户等级
     * 1-基本账户 2-一般账户 3-专用账户
     */
    @Column(name = "account_level")
    private Integer accountLevel;
}
