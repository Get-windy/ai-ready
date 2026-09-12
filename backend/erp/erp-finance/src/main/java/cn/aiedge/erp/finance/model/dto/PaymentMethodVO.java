package cn.aiedge.erp.finance.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付方式出参
 *
 * accountName 由 account_id 回填 finance_account.account_name，
 * 供列表「默认入账账户」列直接展示，避免前端二次请求。
 */
@Data
public class PaymentMethodVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    /** 支付方式编码 */
    private String methodCode;

    /** 支付方式名称 */
    private String methodName;

    /** 支付方式类型 CASH/BANK/WECHAT/ALIPAY/CHECK/OTHER */
    private String methodType;

    /** 默认入账账户ID → finance_account.id */
    private Long accountId;

    /** 默认入账账户名称（回填） */
    private String accountName;

    /** 手续费率（小数，0.006=0.6%） */
    private BigDecimal feeRate;

    /** 是否默认：0-否 1-是 */
    private Integer isDefault;

    /** 排序号 */
    private Integer sort;

    /** 状态：0-停用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
