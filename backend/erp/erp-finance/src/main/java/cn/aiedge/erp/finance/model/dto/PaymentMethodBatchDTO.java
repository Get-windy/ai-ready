package cn.aiedge.erp.finance.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 支付方式批量启停入参
 */
@Data
public class PaymentMethodBatchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待操作的支付方式ID集合 */
    private List<Long> ids;

    /** 目标状态：0-停用 1-启用 */
    private Integer status;
}
