package cn.aiedge.erp.finance.bankaccount.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 银行账户查询条件
 *
 * 对标 ql361 页面固定查询项：筛选条件（单文本框）+ 显示停用 + 显示层次结构。
 */
@Data
public class BankAccountQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 筛选条件：科目编号/科目名称/开户行/银行账号/助记码/银行简称 模糊匹配 */
    private String keyword;

    /** 账户类型 1-银行账户 2-现金账户 3-内部账户 4-外部账户 */
    private Integer accountType;

    /** 账户等级 1-基本账户 2-一般账户 3-专用账户（支付账户筛选） */
    private Integer accountLevel;

    /** 币种（如 CNY，支付账户筛选） */
    private String currency;

    /** 是否用于商城线下转账收款 0-否 1-是 */
    private Integer mallTransferEnabled;

    /** 状态 0-停用 1-启用（null=全部） */
    private Integer status;

    /** 显示停用（1=同时显示停用数据；与 status 互斥，优先 status） */
    private Integer showDisabled;

    /** 显示层次结构（1=按父子层级树形排序返回） */
    private Integer showTree;

    /** 上级账户ID（等于 0 表示仅顶级） */
    private Long parentId;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;
}
