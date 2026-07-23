package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.dto.InvSummaryVO;
import cn.aiedge.erp.stock.dto.PurchasePrepAnalysisVO;
import cn.aiedge.erp.stock.dto.StockFlowVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 库存分析报表Mapper（进销存汇总 / 库存变动流水 / 采购准备分析）
 * <p>
 * 变动来源：各业务单据表 UNION 推导（系统无独立的库存流水表）。
 * 单据生效状态口径：
 * 采购入库 erp_purchase_inbound status IN (4,5,6,7,8,9)（已收货及以后，排除已取消10）；
 * 销售出库 erp_sale_outbound status IN (10,11)（已发货/已完成，发货时扣减库存）；
 * 调拨 erp_stock_transfer status = 5（已完成，执行时移库）；
 * 报损/报溢 erp_stock_damage / erp_stock_overflow status = 3（已执行）；
 * 盘点 erp_stock_check status = 7（已调整，diff_quantity = 实盘 - 账面）。
 * <p>
 * 注意1：erp_purchase_order_item / erp_stock_damage_item / erp_stock_overflow_item
 * 已由 V11.16.0 迁移补齐 tenant_id 列（按主表回填），全局租户拦截器可直接生效；
 * SQL 内保留对主表 tenant_id 的手工过滤（tenantId 为 null 时不过滤，
 * 与全局拦截器"未登录不注入"行为一致）。
 * <p>
 * 注意2：所有可空参数（tenantId/warehouseId/productId/keyword/docType/startDate/endDate）
 * 在SQL中一律以 CAST(#{x} AS 类型) 引用。JDBC 以 null 传参时 PostgreSQL 无法推断
 * 裸参数类型（报"无法确定参数 $N 的数据类型"），显式 CAST 后类型恒可推断。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface StockReportMapper {

    /** 库存变动 UNION（7类生效单据），供进销存汇总与变动流水共用 */
    String MOVEMENTS_CTE =
        "WITH movements AS ("
        // 采购入库（已收货及以后）
        + " SELECT i.id AS line_id, CAST(h.inbound_date AS timestamp) AS move_time, 'PURCHASE_IN' AS doc_type, h.inbound_no AS doc_no,"
        + " i.product_id, i.product_code, i.product_name, h.warehouse_id, h.warehouse_name,"
        + " i.inbound_quantity AS qty, COALESCE(i.unit_cost, i.unit_price, p.cost_price, 0) AS unit_cost, h.create_by AS operator_id"
        + " FROM erp_purchase_inbound h"
        + " JOIN erp_purchase_inbound_item i ON i.inbound_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status IN (4,5,6,7,8,9)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        // 销售出库（已发货/已完成）
        + " SELECT i.id, COALESCE(h.shipped_time, CAST(h.outbound_date AS timestamp)), 'SALE_OUT', h.outbound_no,"
        + " i.product_id, i.product_code, i.product_name, h.warehouse_id, h.warehouse_name,"
        + " -i.outbound_quantity, COALESCE(i.cost_price, p.cost_price, 0), COALESCE(h.shipped_by, h.create_by)"
        + " FROM erp_sale_outbound h"
        + " JOIN erp_sale_outbound_item i ON i.outbound_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status IN (10,11)"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        // 调拨出库（已完成，从调出仓扣减）
        + " SELECT i.id, h.execute_time, 'TRANSFER_OUT', h.transfer_no,"
        + " i.product_id, i.product_code, i.product_name, h.from_warehouse_id, h.from_warehouse_name,"
        + " -COALESCE(i.actual_quantity, i.quantity), COALESCE(i.unit_cost, i.unit_price, p.cost_price, 0), h.execute_by"
        + " FROM erp_stock_transfer h"
        + " JOIN erp_stock_transfer_item i ON i.transfer_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status = 5"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        // 调拨入库（已完成，调入仓增加）
        + " SELECT i.id, h.execute_time, 'TRANSFER_IN', h.transfer_no,"
        + " i.product_id, i.product_code, i.product_name, h.to_warehouse_id, h.to_warehouse_name,"
        + " COALESCE(i.actual_quantity, i.quantity), COALESCE(i.unit_cost, i.unit_price, p.cost_price, 0), h.execute_by"
        + " FROM erp_stock_transfer h"
        + " JOIN erp_stock_transfer_item i ON i.transfer_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status = 5"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        // 报损出库（已执行）
        + " SELECT i.id, h.executed_time, 'DAMAGE_OUT', h.damage_no,"
        + " i.product_id, i.product_code, i.product_name, h.warehouse_id, h.warehouse_name,"
        + " -i.quantity, COALESCE(i.unit_cost, p.cost_price, 0), h.executed_by"
        + " FROM erp_stock_damage h"
        + " JOIN erp_stock_damage_item i ON i.damage_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status = 3"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        // 报溢入库（已执行）
        + " SELECT i.id, h.executed_time, 'OVERFLOW_IN', h.overflow_no,"
        + " i.product_id, i.product_code, i.product_name, h.warehouse_id, h.warehouse_name,"
        + " i.quantity, COALESCE(i.unit_cost, p.cost_price, 0), h.executed_by"
        + " FROM erp_stock_overflow h"
        + " JOIN erp_stock_overflow_item i ON i.overflow_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status = 3"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " UNION ALL"
        // 盘点调整（已调整，diff_quantity = 实盘 - 账面，可正可负）
        + " SELECT i.id, h.adjusted_time, 'CHECK_ADJUST', h.check_no,"
        + " i.product_id, i.product_code, i.product_name, h.warehouse_id, h.warehouse_name,"
        + " i.diff_quantity, COALESCE(i.unit_cost, p.cost_price, 0), h.adjusted_by"
        + " FROM erp_stock_check h"
        + " JOIN erp_stock_check_item i ON i.check_id = h.id AND i.deleted = 0"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status = 7 AND i.diff_quantity <> 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + ") ";

    /**
     * 进销存汇总分页（每商品+仓库一行）
     */
    @Select(MOVEMENTS_CTE
        + "SELECT t.*,"
        + " t.opening_qty + t.in_qty - t.out_qty AS closing_qty,"
        + " t.opening_amt + t.in_amt - t.out_amt AS closing_amt"
        + " FROM ("
        + " SELECT m.product_id, MAX(m.product_code) AS product_code, MAX(m.product_name) AS product_name,"
        + " m.warehouse_id, MAX(m.warehouse_name) AS warehouse_name,"
        + " COALESCE(SUM(CASE WHEN m.move_time < CAST(#{startDate} AS DATE) THEN m.qty ELSE 0 END), 0) AS opening_qty,"
        + " COALESCE(SUM(CASE WHEN m.move_time < CAST(#{startDate} AS DATE) THEN m.qty * m.unit_cost ELSE 0 END), 0) AS opening_amt,"
        + " COALESCE(SUM(CASE WHEN m.qty > 0"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR m.move_time >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR m.move_time < CAST(#{endDate} AS DATE) + 1)"
        + "   THEN m.qty ELSE 0 END), 0) AS in_qty,"
        + " COALESCE(SUM(CASE WHEN m.qty > 0"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR m.move_time >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR m.move_time < CAST(#{endDate} AS DATE) + 1)"
        + "   THEN m.qty * m.unit_cost ELSE 0 END), 0) AS in_amt,"
        + " COALESCE(-SUM(CASE WHEN m.qty < 0"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR m.move_time >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR m.move_time < CAST(#{endDate} AS DATE) + 1)"
        + "   THEN m.qty ELSE 0 END), 0) AS out_qty,"
        + " COALESCE(-SUM(CASE WHEN m.qty < 0"
        + "   AND (CAST(#{startDate} AS DATE) IS NULL OR m.move_time >= CAST(#{startDate} AS DATE))"
        + "   AND (CAST(#{endDate} AS DATE) IS NULL OR m.move_time < CAST(#{endDate} AS DATE) + 1)"
        + "   THEN m.qty * m.unit_cost ELSE 0 END), 0) AS out_amt"
        + " FROM movements m"
        + " WHERE (CAST(#{warehouseId} AS BIGINT) IS NULL OR m.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + " AND (CAST(#{keyword} AS VARCHAR) IS NULL OR CAST(#{keyword} AS VARCHAR) = ''"
        + "   OR m.product_name ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%'"
        + "   OR m.product_code ILIKE '%' || CAST(#{keyword} AS VARCHAR) || '%')"
        + " GROUP BY m.product_id, m.warehouse_id"
        + ") t"
        + " ORDER BY t.product_id, t.warehouse_id")
    IPage<InvSummaryVO> selectInvSummaryPage(Page<InvSummaryVO> page,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("warehouseId") Long warehouseId,
                                             @Param("keyword") String keyword,
                                             @Param("tenantId") Long tenantId);

    /**
     * 库存变动流水分页
     * <p>
     * balance_after 在内层 bal 中按 product+warehouse 全量历史累计（不受日期/单据类型过滤影响），
     * 保证变动后结存的时序正确；外层再做日期与单据类型过滤。
     */
    @Select(MOVEMENTS_CTE
        + ", bal AS ("
        + " SELECT m.*, SUM(m.qty) OVER (PARTITION BY m.product_id, m.warehouse_id"
        + "   ORDER BY m.move_time, m.doc_type, m.line_id) AS balance_after"
        + " FROM movements m"
        + " WHERE (CAST(#{productId} AS BIGINT) IS NULL OR m.product_id = CAST(#{productId} AS BIGINT))"
        + " AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR m.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + ") "
        + "SELECT b.move_time, b.doc_type, b.doc_no, b.product_id, b.product_code, b.product_name,"
        + " b.warehouse_id, b.warehouse_name, b.qty, b.unit_cost, b.qty * b.unit_cost AS amount,"
        + " b.balance_after, b.operator_id, COALESCE(u.real_name, u.nickname, u.username) AS operator_name"
        + " FROM bal b"
        + " LEFT JOIN sys_user u ON u.id = b.operator_id AND u.deleted = 0"
        + " WHERE (CAST(#{docType} AS VARCHAR) IS NULL OR CAST(#{docType} AS VARCHAR) = '' OR b.doc_type = CAST(#{docType} AS VARCHAR))"
        + " AND (CAST(#{startDate} AS DATE) IS NULL OR b.move_time >= CAST(#{startDate} AS DATE))"
        + " AND (CAST(#{endDate} AS DATE) IS NULL OR b.move_time < CAST(#{endDate} AS DATE) + 1)"
        + " ORDER BY b.move_time DESC, b.doc_type, b.line_id DESC")
    IPage<StockFlowVO> selectStockFlowPage(Page<StockFlowVO> page,
                                           @Param("productId") Long productId,
                                           @Param("warehouseId") Long warehouseId,
                                           @Param("docType") String docType,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("tenantId") Long tenantId);

    /**
     * 采购准备分析汇总
     * <p>
     * 安全库存取值优先级：预警配置(active=1，仓库精确匹配优先于全局配置) > erp_stock.safety_stock > erp_stock.min_stock。
     * 在途采购：erp_purchase_order status IN (2已审批,3已下达,5履行中) 且未关闭未取消，
     * 明细行 quantity > received_quantity 的部分为在途。
     */
    @Select("WITH stock_scope AS ("
        + " SELECT s.product_id, s.warehouse_id, s.quantity,"
        + " COALESCE((SELECT COALESCE(c.safety_stock, c.min_stock, c.min_quantity)"
        + "   FROM erp_stock_alert_config c"
        + "   WHERE c.deleted = 0 AND c.active = 1 AND c.product_id = s.product_id"
        + "     AND (c.warehouse_id = s.warehouse_id OR c.warehouse_id IS NULL OR c.warehouse_id = 0)"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR c.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   ORDER BY CASE WHEN c.warehouse_id = s.warehouse_id THEN 0 ELSE 1 END"
        + "   LIMIT 1),"
        + "   s.safety_stock, s.min_stock) AS eff_safety"
        + " FROM erp_stock s"
        + " WHERE s.deleted = 0"
        + " AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR s.warehouse_id = CAST(#{warehouseId} AS BIGINT))"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR s.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "), in_transit AS ("
        + " SELECT h.id AS order_id,"
        + " i.quantity - COALESCE(i.received_quantity, 0) AS qty,"
        + " (i.quantity - COALESCE(i.received_quantity, 0)) * COALESCE(i.cost_price, i.unit_price, p.cost_price, 0) AS amt"
        + " FROM erp_purchase_order h"
        + " JOIN erp_purchase_order_item i ON i.order_id = h.id"
        + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.deleted = 0"
        + "   AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + " WHERE h.deleted = 0 AND h.status IN (2,3,5)"
        + " AND COALESCE(h.closed_flag, 0) = 0 AND COALESCE(h.cancellation_flag, 0) = 0"
        + " AND i.quantity > COALESCE(i.received_quantity, 0)"
        + " AND (CAST(#{warehouseId} AS BIGINT) IS NULL OR COALESCE(i.warehouse_id, h.warehouse_id) = CAST(#{warehouseId} AS BIGINT))"
        + " AND (CAST(#{tenantId} AS BIGINT) IS NULL OR h.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + ") "
        + "SELECT"
        + " (SELECT COUNT(DISTINCT product_id) FROM stock_scope"
        + "   WHERE eff_safety IS NOT NULL AND eff_safety > 0 AND quantity <= eff_safety) AS alert_product_count,"
        + " (SELECT COUNT(DISTINCT product_id) FROM stock_scope WHERE quantity <= 0) AS out_of_stock_sku_count,"
        + " (SELECT COUNT(DISTINCT order_id) FROM in_transit) AS in_transit_order_count,"
        + " (SELECT COALESCE(SUM(qty), 0) FROM in_transit) AS in_transit_quantity,"
        + " (SELECT COALESCE(SUM(amt), 0) FROM in_transit) AS in_transit_amount,"
        + " (SELECT COALESCE(SUM(GREATEST(ss.eff_safety - ss.quantity, 0) * COALESCE(p.cost_price, 0)), 0)"
        + "   FROM stock_scope ss"
        + "   LEFT JOIN erp_product p ON p.id = ss.product_id AND p.deleted = 0"
        + "     AND (CAST(#{tenantId} AS BIGINT) IS NULL OR p.tenant_id = CAST(#{tenantId} AS BIGINT))"
        + "   WHERE ss.eff_safety IS NOT NULL AND ss.eff_safety > 0 AND ss.quantity < ss.eff_safety) AS suggest_replenish_amount")
    PurchasePrepAnalysisVO selectPurchasePrepAnalysis(@Param("warehouseId") Long warehouseId,
                                                      @Param("tenantId") Long tenantId);
}
