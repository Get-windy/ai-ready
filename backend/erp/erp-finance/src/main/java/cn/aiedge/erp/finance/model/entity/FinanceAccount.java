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

    /**
     * 科目编号（资金账户即科目，与 finance_account_subject.subject_code 对齐）
     * 对标 ql361 银行账户列表「科目编号」列
     */
    @TableField("subject_code")
    private String subjectCode;

    /**
     * 上级账户ID（同级之上挂父账户，「显示层次结构」树形展示依据）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 是否用于商城线下转账收款
     * 0-否 1-是（对标 ql361 银行账户列表「是否用于商城线下转账收款」列）
     */
    @TableField("mall_transfer_enabled")
    private Integer mallTransferEnabled = 0;

    /**
     * 助记码
     */
    @TableField("easy_code")
    private String easyCode;

    /**
     * 银行简称
     */
    @TableField("brief_name")
    private String briefName;

    /**
     * 户主名
     */
    @TableField("account_holder")
    private String accountHolder;

    /**
     * 收款码图片地址（对标 ql361 银行信息表单「二维码/上传收款码」）
     */
    @TableField("qrcode_url")
    private String qrcodeUrl;

    /**
     * 是否系统预置
     * 0-否 1-是（预置账户禁止改编号/删除）
     */
    @TableField("is_system")
    private Integer isSystem = 0;

    /**
     * 同级排序号
     */
    @TableField("sort_no")
    private Integer sortNo = 0;
}
