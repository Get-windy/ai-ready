package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付渠道实体
 * 对应表 md_payment_channel（V11.29.0 新增）
 */
@Data
@TableName("md_payment_channel")
@EqualsAndHashCode(callSuper = true)
public class PaymentChannel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 渠道编码
     */
    @TableField("channel_code")
    private String channelCode;

    /**
     * 渠道名称
     */
    @TableField("channel_name")
    private String channelName;

    /**
     * 关联支付方式ID
     */
    @TableField("method_id")
    private Long methodId;

    /**
     * 商户号
     */
    @TableField("merchant_no")
    private String merchantNo;

    /**
     * 渠道配置JSON
     */
    @TableField("config_json")
    private String configJson;

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
