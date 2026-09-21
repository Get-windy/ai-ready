package cn.aiedge.erp.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 采购订单统计结果。
 *
 * <p>由 {@code PurchaseOrderService#getPurchaseStatistics} 产出，两个消费方：
 * 首页看板的「今日采购」KPI（{@code DashboardController}）
 * 与前台采购分析的订单统计（{@code GET /api/erp/purchase/order/statistics}）。</p>
 *
 * <p>本类从旧的 {@code cn.aiedge.erp.order.dto.PurchaseOrderStatisticsDTO} 收敛而来：
 * 旧类声明了 totalItems / taxAmount / topSuppliers 等十余个字段，但旧实现**只填充了下面这几个**，
 * 其余恒为 null —— 这里只保留真正有值的字段，避免继续保留"看起来有、实际永远为空"的假字段。
 * 若后续要补齐其余统计维度，在这里加字段并同步前端
 * （frontend/apps/pc-admin/src/api/analytics.ts 的 PurchaseOrderStatistics）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class PurchaseOrderStatisticsDTO {

    /** 统计区间起（含） */
    private LocalDateTime startDate;

    /** 统计区间止（含） */
    private LocalDateTime endDate;

    /** 区间内订单总数 */
    private Integer totalOrders;

    /** 区间内订单总额（合计 {@code erp_purchase_order.total_amount}） */
    private BigDecimal totalAmount;

    /** 按订单状态分组的单数，key 为状态码字符串 */
    private Map<String, Integer> ordersByStatus;

    /** 按采购类型分组的单数，key 为类型码字符串 */
    private Map<String, Integer> ordersByPurchaseType;
}
