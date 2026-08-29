package cn.aiedge.erp.purchase.saledriven.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 以销定购-采购成品/采购原料执行结果
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class SalesDrivenPurchaseResult {

    /** 销售订单编号 */
    private String orderNo;

    /** 采购模式（FINISHED 采购成品 / MATERIAL 采购原料） */
    private String mode;

    /** 生成的采购订单编号列表 */
    private List<String> purchaseOrderNos = new ArrayList<>();

    /** 生成的采购订单组数（按供应商分组） */
    private int orderCount;
}
