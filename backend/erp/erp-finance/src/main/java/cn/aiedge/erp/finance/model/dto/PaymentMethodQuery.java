package cn.aiedge.erp.finance.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付方式查询条件
 *
 * 清单口径（本系统自定，ql361 无「支付方式」主数据页）：
 * 筛选条件（编码/名称）+ 类型 + 显示停用。
 */
@Data
public class PaymentMethodQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 筛选条件：支付方式编码/名称 模糊匹配 */
    private String keyword;

    /** 支付方式类型 CASH/BANK/WECHAT/ALIPAY/CHECK/OTHER（null=全部） */
    private String methodType;

    /** 状态 0-停用 1-启用（null=不限） */
    private Integer status;

    /** 显示停用 1=同时显示停用数据（与 status 互斥，status 优先） */
    private Integer showDisabled;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 20;
}
