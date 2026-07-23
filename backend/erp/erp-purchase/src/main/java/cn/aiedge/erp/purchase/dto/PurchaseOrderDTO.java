package cn.aiedge.erp.purchase.dto;

import cn.aiedge.erp.purchase.entity.*;
import lombok.Data;

import java.util.List;

/**
 * 采购订单完整DTO（含子表）
 * 用于创建/更新/详情返回
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseOrderDTO {

    /** 主表 */
    private PurchaseOrder order;

    /** 供应商快照 (1:1) */
    private PurchaseOrderPartnerSnapshot partnerSnapshot;

    /** 结算信息 (1:1) */
    private PurchaseOrderSettlement settlement;

    /** 物流信息 (1:N) */
    private List<PurchaseOrderLogistics> logisticsList;

    /** 订金账户 (1:N) */
    private List<PurchaseOrderDeposit> deposits;

    /** 扩展信息 (1:1) */
    private PurchaseOrderExtInfo extInfo;

    /** 明细列表 (1:N) */
    private List<PurchaseOrderItem> items;
}
