package cn.aiedge.erp.stock.purchase;

/**
 * 「补货建议 → 采购订单」的建单出口（SPI，由采购模块实现）。
 *
 * <p><b>为什么是同步接口而不是事件</b>：本能力原先用 {@code ApplicationEventPublisher} 发布
 * {@code PurchaseOrderCreateEvent}，但采购侧**一直没有监听器**——调用方拿不到返回值，
 * 只能先写一个 {@code "PO-"+System.currentTimeMillis()} 的假单号并把建议置为已处理，
 * 于是「生成采购订单」按钮**返回成功但没有任何订单落库**，且建议无法重试（2026-09-22 审计 P0）。
 * 换成有返回值的同步接口后：拿到的是真实订单号，采购模块未装配时也能**显式失败**而不是假成功。</p>
 *
 * <p>方向说明：接口定义在 erp-stock、实现放在 erp-purchase，避免 erp-stock 反向依赖采购模块
 * （erp-purchase 本来就依赖 erp-stock，反向依赖会成环）。</p>
 */
public interface ReplenishmentOrderGateway {

    /**
     * 按补货建议创建采购订单并提交审批。
     *
     * @return 真实采购订单号（不是临时号）
     * @throws RuntimeException 建单失败（商品编码查不到、供应商缺失等），由调用方事务一并回滚
     */
    String createPurchaseOrder(ReplenishmentOrderRequest request);
}
