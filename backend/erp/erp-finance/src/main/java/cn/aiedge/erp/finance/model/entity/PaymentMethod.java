package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付方式实体
 * 对应表 md_payment_method（V11.29.0 新增）
 */
@Data
@TableName("md_payment_method")
@EqualsAndHashCode(callSuper = true)
public class PaymentMethod extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 支付方式编码
     */
    @TableField("method_code")
    private String methodCode;

    /**
     * 支付方式名称
     */
    @TableField("method_name")
    private String methodName;

    /**
     * 支付方式类型
     * CASH=现金 BANK=银行转账 WECHAT=微信 ALIPAY=支付宝 CHECK=支票 OTHER=其他
     */
    @TableField("method_type")
    private String methodType;

    /**
     * 默认入账账户ID
     */
    @TableField("account_id")
    private Long accountId;

    /**
     * 手续费率
     */
    @TableField("fee_rate")
    private BigDecimal feeRate = BigDecimal.ZERO;

    /**
     * 是否默认
     */
    @TableField("is_default")
    private Integer isDefault = 0;

    /**
     * 排序号
     */
    @TableField("sort")
    private Integer sort = 0;

    /**
     * 状态：0-停用 1-启用
     */
    @TableField("status")
    private Integer status = 1;
}
