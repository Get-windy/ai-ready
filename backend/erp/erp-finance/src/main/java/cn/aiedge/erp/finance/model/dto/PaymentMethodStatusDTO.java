package cn.aiedge.erp.finance.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 支付方式启用/停用入参
 */
@Data
public class PaymentMethodStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标状态：0-停用 1-启用 */
    private Integer status;
}
