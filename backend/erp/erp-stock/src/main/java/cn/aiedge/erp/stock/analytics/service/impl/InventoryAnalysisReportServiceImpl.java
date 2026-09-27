package cn.aiedge.erp.stock.analytics.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.analytics.dto.InventoryAnalysisQueryDTO;
import cn.aiedge.erp.stock.analytics.service.InventoryAnalysisReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进销存分析报表服务实现（按商品 / 仓库调拨分析 / 商品调拨分析）
 *
 * <p><b>取数架构</b>：本类自带一份「库存变动 UNION」（不依赖 {@code StockReportMapper.MOVEMENTS_CTE}，
 * 避免影响《库存明细》《采购准备》等页既有口径），把 10 类生效单据统一成
 * {@code (doc_type, product, warehouse, move_time, signed 小单位数量, signed 金额)}，
 * 再按维度聚合。数量与金额一律「入库为正、出库为负」，因此
 * {@code 期末 = 此前余额 + 入库合计 − 出库合计} 在数量与金额两口径同时成立。</p>
 *
 * <p><b>多租户</b>：全部走 {@link JdbcTemplate} 手写 SQL，MyBatis-Plus 多租户拦截器不生效，
 * 故每条 SQL 均显式带 {@code tenant_id = ?}（参数化，非字符串拼接）。</p>
 *
 * <p><b>单据生效状态</b>（与各业务域真实库存写入时点一致）：</p>
 * <ul>
 *   <li>采购入库 erp_purchase_inbound status IN (4..9)（已收货及以后；勾选「显示红冲」再并入 10 已取消）</li>
 *   <li>销售出库 erp_sale_outbound status IN (10,11)（已发货/已完成；「显示红冲」并入 12）</li>
 *   <li>调拨 erp_stock_transfer status = 5（已完成）</li>
 *   <li>报损/报溢 erp_stock_damage / erp_stock_overflow status = 3（已执行）</li>
 *   <li>其他入库/出库 erp_stock_in / erp_stock_out status = 3（已入库/已出库；「显示红冲」并入 5 已取消）</li>
 *   <li>盘点 erp_stock_check status = 7（已调整，diff_quantity 有符号）</li>
 *   <li>借进/借出 wms_borrow_order direction=1/2 status IN (2,3,4)（已记账及以后；「显示红冲」并入 5 已取消）</li>
 *   <li>销退入库 erp_sale_return_doc status IN (2,3)（已审核即回写库存，见 SaleReturnDocServiceImpl.approve）</li>
 *   <li>采退出库 erp_purchase_return status IN (2,4)（已审批/已完成）</li>
 * </ul>
 *
 * <p><b>缺口登记（确认无数据源 → 返回 null，前端显示 -，不返回 0 冒充）</b>：</p>
 * <ul>
 *   <li>商品档案「所属供应商」：全库无「商品↔供应商」关联表（已核 information_schema 的
 *       erp_product* 全部列名，无 supplier 字段）</li>
 *   <li>采购入库「采购费用分摊」：<code>erp_purchase_cost_sharing</code> 只有整单级金额，
 *       无行级/商品级分摊口径；本页不臆造分摊结果</li>
 *   <li>采购入库「小单位数量」：<code>erp_purchase_inbound_item</code> 无小单位列，
 *       优先按来源采购订单明细等比折算；无来源订单的直进行按「所记数量即基本单位数量」1:1 处理</li>
 *   <li>「显示红冲」仅对上述已确认「已取消/已红冲」状态码的单据类型生效，
 *       报损/报溢/调拨/盘点/销退/采退保持固定生效状态（无可靠取消码）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAnalysisReportServiceImpl implements InventoryAnalysisReportService {

    private final JdbcTemplate jdbcTemplate;

    /** 期间条件（起止均为 null 时视为不限） */
    private static final String IN_PERIOD =
        "mv.move_time >= COALESCE(rng.sd, DATE '0001-01-01')"
            + " AND mv.move_time < COALESCE(rng.ed, DATE '9999-12-31') + 1";
    /** 期初（此前余额）条件 */
    private static final String BEFORE_START = "mv.move_time < COALESCE(rng.sd, DATE '0001-01-01')";

    /**
     * 当前登录会话租户；**取不到时明确拒绝**，不把 null 传进 SQL。
     *
     * <p>⚠️ 2026-09-23 修复：原实现直接返回可能为 null 的租户，而 SQL 里的 {@code tenant_id = null}
     * 恒不成立 ⇒ 列表**静默返回 0 行**，"数据凭空少了"却没有任何报错。
     * 按本仓已确立口径（{@code DocQueryController#currentTenantId}），解析不出租户应当明确报「请重新登录」。</p>
     */
    private static Long tenantId() {
        Long t = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (t == null) {
            throw new cn.aiedge.common.exception.BusinessException(401, "无法确定当前租户，请重新登录");
        }
        return t;
    }

    // ════════════════════════════════════════════════════════════════
    //  库存变动 UNION（10 类生效单据；数量/金额入库为正、出库为负）
    // ════════════════════════════════════════════════════════════════

    /**
     * 单据状态条件（勾选「显示红冲」时并入已取消码）。
     * ⚠️ 必须整段带括号：调用处是 {@code ... AND <cond>}，若返回不带括号的 {@code a OR b}，
     * PostgreSQL 会按 {@code (前面所有 AND) OR b} 解析，从而把其它租户/其它条件的行一并带出。
     */
    private static String statusCond(boolean rev, String base, String reversed) {
        return rev ? "(" + base + " OR " + reversed + ")" : "(" + base + ")";
    }

    private String movementsCte(InventoryAnalysisQueryDTO q, List<Object> params) {
        boolean rev = Boolean.TRUE.equals(q.getShowReversed());
        Long tid = tenantId();
        StringBuilder sb = new StringBuilder("WITH mv AS (");
        // 1. 采购入库（小单位数量：优先来源采购订单明细等比折算，否则按基本单位 1:1）
        sb.append(" SELECT 'PURCHASE_IN' AS doc_type, i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.inbound_date AS timestamp) AS move_time,"
            + " COALESCE(CASE WHEN poi.small_unit_quantity IS NULL THEN NULL"
            + "   ELSE poi.small_unit_quantity * COALESCE(i.inbound_quantity,0)"
            + "        / NULLIF(COALESCE(NULLIF(poi.quantity,0), NULLIF(i.inbound_quantity,0)),0) END,"
            + "  COALESCE(i.inbound_quantity,0)) AS small_qty,"
            + " COALESCE(i.line_amount,0) AS amount"
            + " FROM erp_purchase_inbound h"
            + " JOIN erp_purchase_inbound_item i ON i.inbound_id = h.id AND i.deleted = 0"
            + " LEFT JOIN erp_purchase_order_item poi ON poi.id = i.order_item_id AND poi.tenant_id = i.tenant_id"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND " + statusCond(rev, "h.status IN (4,5,6,7,8,9)", "h.status = 10"));
        params.add(tid);
        // 2. 调拨入库
        sb.append(" UNION ALL SELECT 'TRANSFER_IN', i.product_id, i.product_code, i.product_name,"
            + " h.to_warehouse_id, CAST(h.execute_time AS timestamp),"
            + " COALESCE(i.small_unit_quantity, i.actual_quantity, i.quantity, 0),"
            + " COALESCE(NULLIF(i.cost_amount,0), i.line_amount, 0)"
            + " FROM erp_stock_transfer h"
            + " JOIN erp_stock_transfer_item i ON i.transfer_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status = 5");
        params.add(tid);
        // 3. 调拨出库（负数）
        sb.append(" UNION ALL SELECT 'TRANSFER_OUT', i.product_id, i.product_code, i.product_name,"
            + " h.from_warehouse_id, CAST(h.execute_time AS timestamp),"
            + " -COALESCE(i.small_unit_quantity, i.actual_quantity, i.quantity, 0),"
            + " -COALESCE(NULLIF(i.cost_amount,0), i.line_amount, 0)"
            + " FROM erp_stock_transfer h"
            + " JOIN erp_stock_transfer_item i ON i.transfer_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status = 5");
        params.add(tid);
        // 4. 其他入库：报溢
        sb.append(" UNION ALL SELECT 'OVERFLOW_IN', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.executed_time AS timestamp),"
            + " COALESCE(i.small_unit_quantity, i.quantity, 0), COALESCE(i.amount,0)"
            + " FROM erp_stock_overflow h"
            + " JOIN erp_stock_overflow_item i ON i.overflow_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status = 3");
        params.add(tid);
        // 5. 其他入库：其他入库单
        sb.append(" UNION ALL SELECT 'STOCK_IN', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.stock_in_date AS timestamp),"
            + " COALESCE(i.small_unit_quantity, i.quantity, 0), COALESCE(i.amount,0)"
            + " FROM erp_stock_in h"
            + " JOIN erp_stock_in_item i ON i.stock_in_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND " + statusCond(rev, "h.status = 3", "h.status = 5"));
        params.add(tid);
        // 6. 盘点调整（diff_quantity 有符号，金额按商品成本价估值；无小单位列，按基本单位处理）
        sb.append(" UNION ALL SELECT 'CHECK_ADJUST', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.adjusted_time AS timestamp),"
            + " COALESCE(i.diff_quantity,0), COALESCE(i.diff_quantity,0) * COALESCE(pr.cost_price,0)"
            + " FROM erp_stock_check h"
            + " JOIN erp_stock_check_item i ON i.check_id = h.id AND i.deleted = 0"
            + " LEFT JOIN erp_product pr ON pr.id = i.product_id AND pr.tenant_id = i.tenant_id AND pr.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status = 7 AND COALESCE(i.diff_quantity,0) <> 0");
        params.add(tid);
        // 7. 其他出库：报损
        sb.append(" UNION ALL SELECT 'DAMAGE_OUT', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.executed_time AS timestamp),"
            + " -COALESCE(i.small_unit_quantity, i.quantity, 0), -COALESCE(i.amount,0)"
            + " FROM erp_stock_damage h"
            + " JOIN erp_stock_damage_item i ON i.damage_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status = 3");
        params.add(tid);
        // 8. 其他出库：其他出库单
        sb.append(" UNION ALL SELECT 'STOCK_OUT', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.stock_out_date AS timestamp),"
            + " -COALESCE(i.small_unit_quantity, i.quantity, 0), -COALESCE(i.amount,0)"
            + " FROM erp_stock_out h"
            + " JOIN erp_stock_out_item i ON i.stock_out_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND " + statusCond(rev, "h.status = 3", "h.status = 5"));
        params.add(tid);
        // 9. 借进入库
        sb.append(" UNION ALL SELECT 'BORROW_IN', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.borrow_date AS timestamp),"
            + " COALESCE(i.small_unit_quantity, i.quantity, 0), COALESCE(i.amount,0)"
            + " FROM wms_borrow_order h"
            + " JOIN wms_borrow_order_item i ON i.order_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.direction = 1 AND "
            + statusCond(rev, "h.status IN (2,3,4)", "h.status = 5"));
        params.add(tid);
        // 10. 借出出库（负数）
        sb.append(" UNION ALL SELECT 'BORROW_OUT', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.borrow_date AS timestamp),"
            + " -COALESCE(i.small_unit_quantity, i.quantity, 0), -COALESCE(i.amount,0)"
            + " FROM wms_borrow_order h"
            + " JOIN wms_borrow_order_item i ON i.order_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.direction = 2 AND "
            + statusCond(rev, "h.status IN (2,3,4)", "h.status = 5"));
        params.add(tid);
        // 11. 销退入库（审核即回写库存）
        sb.append(" UNION ALL SELECT 'SALE_RETURN_IN', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.order_date AS timestamp),"
            + " COALESCE(i.small_unit_quantity, i.return_quantity, 0),"
            + " COALESCE(NULLIF(i.line_amount,0), i.ref_cost_amount, 0)"
            + " FROM erp_sale_return_doc h"
            + " JOIN erp_sale_return_doc_item i ON i.return_doc_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status IN (2,3)");
        params.add(tid);
        // 12. 销售出库（负数，金额取成本口径）
        sb.append(" UNION ALL SELECT 'SALE_OUT', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(COALESCE(h.shipped_time, CAST(h.outbound_date AS timestamp)) AS timestamp),"
            + " -COALESCE(i.small_unit_quantity, i.outbound_quantity, 0),"
            + " -COALESCE(NULLIF(i.cost_amount,0), i.line_amount, 0)"
            + " FROM erp_sale_outbound h"
            + " JOIN erp_sale_outbound_item i ON i.outbound_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND " + statusCond(rev, "h.status IN (10,11)", "h.status = 12"));
        params.add(tid);
        // 13. 采退出库（负数）
        sb.append(" UNION ALL SELECT 'PURCHASE_RETURN_OUT', i.product_id, i.product_code, i.product_name,"
            + " h.warehouse_id, CAST(h.return_date AS timestamp),"
            + " -COALESCE(i.small_unit_quantity, i.return_quantity, 0),"
            + " -COALESCE(NULLIF(i.cost_amount,0), i.line_amount, 0)"
            + " FROM erp_purchase_return h"
            + " JOIN erp_purchase_return_item i ON i.return_id = h.id AND i.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND h.status IN (2,4)");
        params.add(tid);
        sb.append(")");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════
    //  期间列生成（分组内 入库为正 / 出库取反为正）
    // ════════════════════════════════════════════════════════════════

    private static String sumIn(String alias, String docTypes) {
        return " COALESCE(SUM(CASE WHEN " + IN_PERIOD + " AND mv.doc_type IN (" + docTypes
            + ") AND mv.small_qty > 0 THEN mv.small_qty ELSE 0 END),0) AS \"" + alias + "Qty\","
            + " COALESCE(SUM(CASE WHEN " + IN_PERIOD + " AND mv.doc_type IN (" + docTypes
            + ") AND mv.small_qty > 0 THEN mv.amount ELSE 0 END),0) AS \"" + alias + "Amount\"";
    }

    private static String sumOut(String alias, String docTypes) {
        return " COALESCE(-SUM(CASE WHEN " + IN_PERIOD + " AND mv.doc_type IN (" + docTypes
            + ") AND mv.small_qty < 0 THEN mv.small_qty ELSE 0 END),0) AS \"" + alias + "Qty\","
            + " COALESCE(-SUM(CASE WHEN " + IN_PERIOD + " AND mv.doc_type IN (" + docTypes
            + ") AND mv.small_qty < 0 THEN mv.amount ELSE 0 END),0) AS \"" + alias + "Amount\"";
    }

    private static final String CAT_OTHER_IN = "'OVERFLOW_IN','STOCK_IN','CHECK_ADJUST'";
    private static final String CAT_OTHER_OUT = "'DAMAGE_OUT','STOCK_OUT','CHECK_ADJUST'";

    // ════════════════════════════════════════════════════════════════
    //  Tab1：按商品（进销存主账）
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> productTab(InventoryAnalysisQueryDTO q) {
        List<Object> params = new ArrayList<>();
        String cte = movementsCte(q, params);
        params.add(q.getStartDate());
        params.add(q.getEndDate());
        params.add(tenantId());
        StringBuilder where = new StringBuilder();
        // 出库/入库仓库：按商品口径下作为「变动仓库」过滤（两框取并集）
        if (StringUtils.hasText(q.getOutWarehouseName()) || StringUtils.hasText(q.getInWarehouseName())) {
            String out = StringUtils.hasText(q.getOutWarehouseName()) ? q.getOutWarehouseName().trim() : null;
            String in = StringUtils.hasText(q.getInWarehouseName()) ? q.getInWarehouseName().trim() : null;
            where.append(" AND mv.warehouse_id IN (SELECT w.id FROM erp_warehouse w WHERE w.deleted = 0"
                + " AND w.tenant_id = ? AND (");
            params.add(tenantId());
            if (out != null) {
                where.append("w.warehouse_name LIKE ?");
                params.add("%" + out + "%");
            }
            if (out != null && in != null) {
                where.append(" OR ");
            }
            if (in != null) {
                where.append("w.warehouse_name LIKE ?");
                params.add("%" + in + "%");
            }
            where.append("))");
        }
        if (StringUtils.hasText(q.getKeyword())) {
            where.append(" AND (p.product_name LIKE ? OR p.product_code LIKE ? OR p.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (StringUtils.hasText(q.getBrand())) {
            where.append(" AND p.brand LIKE ?");
            params.add("%" + q.getBrand().trim() + "%");
        }
        String sql = cte
            + ", rng AS (SELECT CAST(? AS DATE) AS sd, CAST(? AS DATE) AS ed)"
            + " SELECT COALESCE(mv.product_id::text, mv.product_code, mv.product_name) AS \"productId\","
            + " MAX(COALESCE(mv.product_name, p.product_name)) AS \"dimLabel\","
            + " MAX(COALESCE(mv.product_code, p.product_code)) AS \"productCode\","
            + " MAX(p.spec) AS \"spec\", MAX(p.model) AS \"model\", MAX(p.origin) AS \"origin\","
            + " MAX(p.barcode) AS \"barcode\", MAX(p.unit) AS \"unit\", MAX(p.brand) AS \"brand\","
            + " COALESCE(SUM(CASE WHEN " + BEFORE_START + " THEN mv.small_qty ELSE 0 END),0) AS \"openingSmallQty\","
            + " COALESCE(SUM(CASE WHEN " + BEFORE_START + " THEN mv.amount ELSE 0 END),0) AS \"openingAmount\","
            + sumIn("purchaseIn", "'PURCHASE_IN'") + ","
            + sumIn("transferIn", "'TRANSFER_IN'") + ","
            + sumIn("otherIn", CAT_OTHER_IN) + ","
            + sumIn("borrowIn", "'BORROW_IN'") + ","
            + sumIn("saleReturnIn", "'SALE_RETURN_IN'") + ","
            + sumIn("inTotal", "'PURCHASE_IN','TRANSFER_IN'," + CAT_OTHER_IN + ",'BORROW_IN','SALE_RETURN_IN'") + ","
            + sumOut("saleOut", "'SALE_OUT'") + ","
            + sumOut("transferOut", "'TRANSFER_OUT'") + ","
            + sumOut("otherOut", CAT_OTHER_OUT) + ","
            + sumOut("borrowOut", "'BORROW_OUT'") + ","
            + sumOut("purchaseReturnOut", "'PURCHASE_RETURN_OUT'") + ","
            + sumOut("outTotal", "'SALE_OUT','TRANSFER_OUT'," + CAT_OTHER_OUT + ",'BORROW_OUT','PURCHASE_RETURN_OUT'")
            + " FROM mv LEFT JOIN erp_product p ON p.id = mv.product_id AND p.tenant_id = ? AND p.deleted = 0"
            + " CROSS JOIN rng WHERE 1=1" + where
            + " GROUP BY 1";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : rows) {
            BigDecimal open = num(r, "openingSmallQty");
            BigDecimal openAmt = num(r, "openingAmount");
            r.put("closingSmallQty", open.add(num(r, "inTotalQty")).subtract(num(r, "outTotalQty")));
            r.put("closingAmount", openAmt.add(num(r, "inTotalAmount")).subtract(num(r, "outTotalAmount")));
            // 无数据源列：显式置 null（前端显示 -），不返回 0 冒充
            r.put("supplierName", null);
            r.put("purchaseInFeeShare", null);
            if (!r.containsKey("dimLabel") || r.get("dimLabel") == null) {
                r.put("dimLabel", r.get("productId"));
            }
            r.put("rowKey", String.valueOf(r.get("productId")));
        }
        sortBy(rows, "outTotalAmount", false);
        return pageResult(rows, q, "summary");
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab2：仓库调拨分析
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> transferWarehouseTab(InventoryAnalysisQueryDTO q) {
        List<Object> params = new ArrayList<>();
        Where w = new Where();
        w.dateFrom("h.execute_time", q.getStartDate());
        w.dateTo("h.execute_time", q.getEndDate());
        w.like("h.from_warehouse_name", q.getOutWarehouseName());
        w.like("h.to_warehouse_name", q.getInWarehouseName());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(i.product_name LIKE ? OR i.product_code LIKE ? OR i.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw, kw);
        }
        w.like("COALESCE(NULLIF(i.brand,''), p.brand)", q.getBrand());
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT COALESCE(h.from_warehouse_name,'未指定仓库') AS \"outWarehouseName\","
            + " COALESCE(h.to_warehouse_name,'未指定仓库') AS \"inWarehouseName\","
            + " COALESCE(SUM(COALESCE(i.actual_quantity, i.quantity, 0)),0) AS \"transferQty\","
            + " COALESCE(SUM(COALESCE(NULLIF(i.transfer_amount,0), i.line_amount, 0)),0) AS \"transferAmount\","
            + " COALESCE(SUM(COALESCE(NULLIF(i.cost_amount,0), i.line_amount, 0)),0) AS \"costAmount\""
            + " FROM erp_stock_transfer h"
            + " JOIN erp_stock_transfer_item i ON i.transfer_id = h.id AND i.deleted = 0"
            + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.tenant_id = i.tenant_id AND p.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND "
            + statusCond(Boolean.TRUE.equals(q.getShowReversed()), "h.status = 5", "h.status = 6") + w.sql()
            + " GROUP BY 1, 2";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : rows) {
            r.put("rowKey", r.get("outWarehouseName") + "->" + r.get("inWarehouseName"));
            // 差异金额 = 调拨金额 − 成本金额（对标实测等式）
            r.put("diffAmount", num(r, "transferAmount").subtract(num(r, "costAmount")));
        }
        sortBy(rows, "transferAmount", false);
        return pageResult(rows, q, "summary");
    }

    // ════════════════════════════════════════════════════════════════
    //  Tab3：商品调拨分析
    // ════════════════════════════════════════════════════════════════

    private Map<String, Object> transferProductTab(InventoryAnalysisQueryDTO q) {
        List<Object> params = new ArrayList<>();
        Where w = new Where();
        w.dateFrom("h.execute_time", q.getStartDate());
        w.dateTo("h.execute_time", q.getEndDate());
        w.like("h.from_warehouse_name", q.getOutWarehouseName());
        w.like("h.to_warehouse_name", q.getInWarehouseName());
        if (StringUtils.hasText(q.getKeyword())) {
            w.raw("(i.product_name LIKE ? OR i.product_code LIKE ? OR i.barcode LIKE ?)");
            String kw = "%" + q.getKeyword().trim() + "%";
            w.addParams(kw, kw, kw);
        }
        w.like("COALESCE(NULLIF(i.brand,''), p.brand)", q.getBrand());
        params.add(tenantId());
        params.addAll(w.list());
        String sql = "SELECT COALESCE(i.product_id::text, i.product_code, i.product_name) AS \"productId\","
            + " MAX(COALESCE(i.product_name, p.product_name)) AS \"dimLabel\","
            + " MAX(COALESCE(i.product_code, p.product_code)) AS \"productCode\","
            + " MAX(COALESCE(NULLIF(i.brand,''), p.brand)) AS \"brand\","
            + " MAX(i.product_unit) AS \"unit\","
            + " MAX(i.conversion_relation) AS \"conversionRelation\","
            + " MAX(i.conversion_result) AS \"conversionResult\","
            + " COALESCE(SUM(COALESCE(i.actual_quantity, i.quantity, 0)),0) AS \"transferQty\","
            + " COALESCE(SUM(COALESCE(NULLIF(i.transfer_amount,0), i.line_amount, 0)),0) AS \"transferAmount\","
            + " COALESCE(SUM(COALESCE(NULLIF(i.cost_amount,0), i.line_amount, 0)),0) AS \"costAmount\""
            + " FROM erp_stock_transfer h"
            + " JOIN erp_stock_transfer_item i ON i.transfer_id = h.id AND i.deleted = 0"
            + " LEFT JOIN erp_product p ON p.id = i.product_id AND p.tenant_id = i.tenant_id AND p.deleted = 0"
            + " WHERE h.deleted = 0 AND h.tenant_id = ? AND "
            + statusCond(Boolean.TRUE.equals(q.getShowReversed()), "h.status = 5", "h.status = 6") + w.sql()
            + " GROUP BY COALESCE(i.product_id::text, i.product_code, i.product_name)";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
        for (Map<String, Object> r : rows) {
            r.put("rowKey", String.valueOf(r.get("productId")));
            r.put("diffAmount", num(r, "transferAmount").subtract(num(r, "costAmount")));
            // 无「商品↔供应商」关联表，如实置 null（前端显示 -）
            r.put("supplierName", null);
            if (r.get("dimLabel") == null) {
                r.put("dimLabel", r.get("productId"));
            }
        }
        sortBy(rows, "transferAmount", false);
        return pageResult(rows, q, "summary");
    }

    // ════════════════════════════════════════════════════════════════
    //  条件 / 数值 / 分页工具
    // ════════════════════════════════════════════════════════════════

    private static final class Where {
        private final StringBuilder sb = new StringBuilder();
        private final List<Object> params = new ArrayList<>();

        void raw(String sql) {
            if (StringUtils.hasText(sql)) {
                sb.append(" AND ").append(sql);
            }
        }

        void like(String expr, String v) {
            if (expr != null && StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" LIKE ?");
                params.add("%" + v.trim() + "%");
            }
        }

        void dateFrom(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" >= ?::date");
                params.add(v.trim());
            }
        }

        void dateTo(String expr, String v) {
            if (StringUtils.hasText(v)) {
                sb.append(" AND ").append(expr).append(" < ?::date + 1");
                params.add(v.trim());
            }
        }

        void addParams(Object... values) {
            for (Object o : values) {
                params.add(o);
            }
        }

        String sql() {
            return sb.toString();
        }

        List<Object> list() {
            return params;
        }
    }

    private static BigDecimal toDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal b) {
            return b;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal num(Map<String, Object> m, String key) {
        return m == null ? BigDecimal.ZERO : toDecimal(m.get(key));
    }

    /** 合计行：数值列求和（比率类列本页无，全部为可加量额） */
    private static Map<String, Object> summary(List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            m.forEach((k, v) -> {
                if (v instanceof BigDecimal || v instanceof Number) {
                    s.merge(k, toDecimal(v), (a, b) -> toDecimal(a).add(toDecimal(b)));
                }
            });
        }
        s.remove("productId");
        return s;
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> all, InventoryAnalysisQueryDTO q, String summaryKey) {
        int page = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : q.getSize();
        int total = all.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("records", new ArrayList<>(all.subList(from, to)));
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        out.put("pages", (int) Math.ceil(total / (double) size));
        out.put(summaryKey, summary(all));
        return out;
    }

    private void sortBy(List<Map<String, Object>> rows, String field, boolean asc) {
        Comparator<Map<String, Object>> cmp = (a, b) -> {
            Object va = a.get(field);
            Object vb = b.get(field);
            if (va == null && vb == null) {
                return 0;
            }
            if (va == null) {
                return 1;
            }
            if (vb == null) {
                return -1;
            }
            if (va instanceof Number && vb instanceof Number) {
                return toDecimal(va).compareTo(toDecimal(vb));
            }
            return va.toString().compareTo(vb.toString());
        };
        rows.sort(asc ? cmp : cmp.reversed());
    }

    // ════════════════════════════════════════════════════════════════
    //  入口
    // ════════════════════════════════════════════════════════════════

    @Override
    public Map<String, Object> analysis(InventoryAnalysisQueryDTO query) {
        String tab = query.getTab() == null ? "product" : query.getTab();
        return switch (tab) {
            case "transferWarehouse" -> transferWarehouseTab(query);
            case "transferProduct" -> transferProductTab(query);
            default -> productTab(query);
        };
    }
}
