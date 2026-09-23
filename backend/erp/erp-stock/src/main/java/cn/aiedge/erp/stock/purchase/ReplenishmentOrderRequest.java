package cn.aiedge.erp.stock.purchase;

import java.math.BigDecimal;

/**
 * 「补货建议 → 采购订单」的建单请求。
 *
 * <p>字段取自 {@code erp_stock_replenishment} 的建议行。注意该表**只有 product_code 没有 product_id**，
 * 所以实现方需要按编码回查商品主数据拿 id 与采购价。</p>
 *
 * @param suggestionId  补货建议 ID（写回创建结果时定位用）
 * @param supplierId    供应商（往来单位）ID
 * @param supplierName  供应商名称（写快照，可空）
 * @param productCode   商品编码
 * @param productName   商品名称
 * @param specification 规格（可空）
 * @param unit          单位（可空）
 * @param quantity      建议采购数量
 * @param warehouseId   建议入库仓库 ID
 * @param warehouseName 建议入库仓库名称（可空）
 */
public record ReplenishmentOrderRequest(
        Long suggestionId,
        Long supplierId,
        String supplierName,
        String productCode,
        String productName,
        String specification,
        String unit,
        BigDecimal quantity,
        Long warehouseId,
        String warehouseName) {
}
