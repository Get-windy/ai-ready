package cn.aiedge.erp.finance.bankaccount.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行账户（资金账户）DTO
 *
 * 对标 ql361「资料 → 财务账户 → 银行账户」：
 *   列表列 = 科目编号(subjectCode) / 科目名称(accountName) / 账户类型(accountType) / 是否用于商城线下转账收款(mallTransferEnabled)
 *   表单字段 = 银行编号*(subjectCode) / 银行全称*(accountName) / 账户类型 / 助记码 / 银行简称 / 开户行 / 户主名 / 银行账号 / 二维码 / 是否用于商城线下转账收款
 *
 * 说明：本页与《支付账户》同源 finance_account（单一口径，严禁另建重复银行账户表）。
 */
@Data
public class BankAccountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 科目编号（银行编号） */
    private String subjectCode;

    /** 科目名称（银行全称） */
    private String accountName;

    /** 账户类型 1-银行账户 2-现金账户 3-内部账户 4-外部账户 */
    private Integer accountType;

    /** 开户行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 户主名 */
    private String accountHolder;

    /** 助记码 */
    private String easyCode;

    /** 银行简称 */
    private String briefName;

    /** 收款码地址 */
    private String qrcodeUrl;

    /** 是否用于商城线下转账收款 0-否 1-是 */
    private Integer mallTransferEnabled;

    /** 上级账户ID */
    private Long parentId;

    /** 账户余额 */
    private BigDecimal balance;

    /** 币种 */
    private String currency;

    /** 账户等级 1-基本账户 2-一般账户 3-专用账户 */
    private Integer accountLevel;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 是否系统预置 0-否 1-是 */
    private Integer isSystem;

    /** 同级排序号 */
    private Integer sortNo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ── 以下为树形展示派生字段（不入库） ──

    /** 层级（根=1），「显示层次结构」缩进用 */
    private Integer level;

    /** 是否有下级（行首文件夹图标 / 展开箭头） */
    private Boolean hasChildren;

    /** 上级账户名称（表单回显） */
    private String parentName;
}
