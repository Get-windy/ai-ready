package cn.aiedge.erp.metrics.service.impl;

import cn.aiedge.erp.metrics.entity.BusinessMetric;
import cn.aiedge.erp.metrics.entity.MetricData;
import cn.aiedge.erp.metrics.enums.MetricType;
import cn.aiedge.erp.metrics.repository.BusinessMetricRepository;
import cn.aiedge.erp.metrics.repository.MetricDataRepository;
import cn.aiedge.erp.metrics.service.MetricsCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标计算服务实现
 *
 * 事务说明：所有 calculate* 只读查询方法标注
 * {@code @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)}，
 * 调用方（如 MetricsServiceImpl.refreshMetrics）开启的写事务会被挂起，
 * 计算查询在非事务上下文（自动提交）中执行。
 * 这样任何一条计算 SQL 失败都不会毒化外层写事务（避免 PostgreSQL 25P02
 * 导致 erp_metric_data 级联写入失败），单个指标失败不影响其余指标的采集写入。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsCalculationServiceImpl implements MetricsCalculationService {

    /**
     * 无租户上下文时的回落租户（与 monitor 包 MetricCollectorConfig 保持一致）。
     *
     * <p>正常路径不会用到它：定时任务由 {@code MetricsScheduler} 逐租户
     * {@code setTempTenantId} 后调用，请求触发时取会话租户。
     * <b>2026-09-20 修复</b>：此前本类所有指标 SQL 都把租户**写死成 1**
     * （字符串拼接 `" AND tenant_id = " + currentTenantId()`），
     * 导致非平台租户的管理员在经营看板上看到的是**租户 1 的数据** —— 既不准也属跨租户泄露。</p>
     */
    private static final long FALLBACK_TENANT_ID = 1L;

    /** 本次计算所属租户：调度/会话上下文优先，都取不到才回落 */
    private long currentTenantId() {
        Long tid = cn.aiedge.base.config.MyBatisPlusConfig.getCurrentTenantIdValue();
        return tid != null ? tid : FALLBACK_TENANT_ID;
    }

    private final BusinessMetricRepository metricRepository;
    private final MetricDataRepository metricDataRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public BigDecimal calculateMetric(BusinessMetric metric, LocalDateTime calculationTime) {
        log.debug("Calculating metric: {} at {}", metric.getMetricCode(), calculationTime);

        try {
            switch (metric.getMetricType()) {
                case ORDER:
                    return calculateOrderMetric(metric.getMetricCode(), calculationTime);
                case INVENTORY:
                    return calculateInventoryMetric(metric.getMetricCode(), calculationTime);
                case USER:
                    return calculateUserMetric(metric.getMetricCode(), calculationTime);
                case SALES:
                    return calculateSalesMetric(metric.getMetricCode(), calculationTime);
                case FINANCE:
                    return calculateFinanceMetric(metric.getMetricCode(), calculationTime);
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("Error calculating metric {}: {}", metric.getMetricCode(), e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public Map<String, BigDecimal> calculateMetricsByType(MetricType type, LocalDateTime calculationTime) {
        log.info("Calculating all metrics for type: {} at {}", type, calculationTime);

        List<BusinessMetric> metrics = metricRepository.findActiveMetricsByType(type);
        Map<String, BigDecimal> results = new HashMap<>();

        for (BusinessMetric metric : metrics) {
            BigDecimal value = calculateMetric(metric, calculationTime);
            results.put(metric.getMetricCode(), value);
        }

        // 如果没有配置指标，使用默认指标
        if (results.isEmpty()) {
            results.putAll(calculateDefaultMetrics(type, calculationTime));
        }

        return results;
    }

    private Map<String, BigDecimal> calculateDefaultMetrics(MetricType type, LocalDateTime calculationTime) {
        Map<String, BigDecimal> defaults = new HashMap<>();

        switch (type) {
            case ORDER:
                defaults.put("order_total_today", calculateOrderTotalToday(calculationTime));
                defaults.put("order_amount_today", calculateOrderAmountToday(calculationTime));
                defaults.put("order_pending_count", calculateOrderPendingCount(calculationTime));
                defaults.put("order_completed_today", calculateOrderCompletedToday(calculationTime));
                break;
            case INVENTORY:
                defaults.put("inventory_total_value", calculateInventoryTotalValue(calculationTime));
                defaults.put("inventory_low_stock_count", calculateInventoryLowStockCount(calculationTime));
                defaults.put("inventory_out_of_stock_count", calculateInventoryOutOfStockCount(calculationTime));
                break;
            case USER:
                defaults.put("user_active_today", calculateUserActiveToday(calculationTime));
                defaults.put("user_new_today", calculateUserNewToday(calculationTime));
                defaults.put("user_total_count", calculateUserTotalCount(calculationTime));
                break;
            case SALES:
                defaults.put("sales_revenue_today", calculateSalesRevenueToday(calculationTime));
                defaults.put("sales_order_count_today", calculateSalesOrderCountToday(calculationTime));
                defaults.put("sales_avg_order_value", calculateSalesAvgOrderValue(calculationTime));
                break;
            case FINANCE:
                defaults.put("finance_revenue_month", calculateFinanceRevenueMonth(calculationTime));
                defaults.put("finance_profit_margin", calculateFinanceProfitMargin(calculationTime));
                break;
        }

        return defaults;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public BigDecimal calculateOrderMetric(String metricCode, LocalDateTime calculationTime) {
        switch (metricCode) {
            case "order_total_today":
                return calculateOrderTotalToday(calculationTime);
            case "order_amount_today":
                return calculateOrderAmountToday(calculationTime);
            case "order_pending_count":
                return calculateOrderPendingCount(calculationTime);
            case "order_completed_today":
                return calculateOrderCompletedToday(calculationTime);
            case "order_cancelled_today":
                return calculateOrderCancelledToday(calculationTime);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 今日订单总数：erp_sale_order（销售订单），按 create_time 当天过滤
     */
    private BigDecimal calculateOrderTotalToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COUNT(*) FROM erp_sale_order " +
                    "WHERE create_time::date = ? AND deleted = 0 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, today);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate order_total_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日订单金额：erp_sale_order.total_amount 求和，排除已取消（status=6）
     */
    private BigDecimal calculateOrderAmountToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM erp_sale_order " +
                    "WHERE create_time::date = ? AND status <> 6 AND deleted = 0 AND tenant_id = " + currentTenantId();
            BigDecimal amount = jdbcTemplate.queryForObject(sql, BigDecimal.class, today);
            return amount != null ? amount : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate order_amount_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 待处理订单数：erp_sale_order 状态为 待审核(1)/待发货(2)/部分发货(3)
     */
    private BigDecimal calculateOrderPendingCount(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COUNT(*) FROM erp_sale_order " +
                    "WHERE status IN (1, 2, 3) AND deleted = 0 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate order_pending_count from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日完成订单数：erp_sale_order 状态为 交易完成(5)，按 update_time 当天过滤
     */
    private BigDecimal calculateOrderCompletedToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COUNT(*) FROM erp_sale_order " +
                    "WHERE update_time::date = ? AND status = 5 AND deleted = 0 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, today);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate order_completed_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日取消订单数：erp_sale_order 状态为 已取消(6)，按 update_time 当天过滤
     */
    private BigDecimal calculateOrderCancelledToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COUNT(*) FROM erp_sale_order " +
                    "WHERE update_time::date = ? AND status = 6 AND deleted = 0 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, today);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate order_cancelled_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public BigDecimal calculateInventoryMetric(String metricCode, LocalDateTime calculationTime) {
        switch (metricCode) {
            case "inventory_total_value":
                return calculateInventoryTotalValue(calculationTime);
            case "inventory_low_stock_count":
                return calculateInventoryLowStockCount(calculationTime);
            case "inventory_out_of_stock_count":
                return calculateInventoryOutOfStockCount(calculationTime);
            case "inventory_total_sku":
                return calculateInventoryTotalSku(calculationTime);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 库存总价值：erp_stock.quantity × erp_product.cost_price（库存表无单价列，关联商品成本价）
     */
    private BigDecimal calculateInventoryTotalValue(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COALESCE(SUM(s.quantity * COALESCE(p.cost_price, 0)), 0) " +
                    "FROM erp_stock s JOIN erp_product p ON p.id = s.product_id AND p.deleted = 0 " +
                    "WHERE s.deleted = 0 AND s.tenant_id = " + currentTenantId();
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            return value != null ? value : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate inventory_total_value from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 低库存商品数：erp_stock 中库存数量不高于安全库存的记录数
     */
    private BigDecimal calculateInventoryLowStockCount(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COUNT(*) FROM erp_stock " +
                    "WHERE deleted = 0 AND tenant_id = " + currentTenantId() +
                    " AND safety_stock IS NOT NULL AND quantity <= safety_stock";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate inventory_low_stock_count from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 缺货商品数：erp_stock 中库存数量小于等于 0 的记录数
     */
    private BigDecimal calculateInventoryOutOfStockCount(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COUNT(*) FROM erp_stock " +
                    "WHERE deleted = 0 AND tenant_id = " + currentTenantId() + " AND quantity <= 0";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate inventory_out_of_stock_count from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 在售SKU总数：erp_product 中启用状态（status='ENABLED'）的商品数
     */
    private BigDecimal calculateInventoryTotalSku(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COUNT(*) FROM erp_product " +
                    "WHERE deleted = 0 AND tenant_id = " + currentTenantId() + " AND status = 'ENABLED'";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate inventory_total_sku from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public BigDecimal calculateUserMetric(String metricCode, LocalDateTime calculationTime) {
        switch (metricCode) {
            case "user_active_today":
                return calculateUserActiveToday(calculationTime);
            case "user_new_today":
                return calculateUserNewToday(calculationTime);
            case "user_total_count":
                return calculateUserTotalCount(calculationTime);
            case "user_online_now":
                return calculateUserOnlineNow(calculationTime);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 今日活跃用户：sys_login_log 中当天登录成功（login_result=0）的去重用户数
     */
    private BigDecimal calculateUserActiveToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COUNT(DISTINCT user_id) FROM sys_login_log " +
                    "WHERE login_time::date = ? AND login_result = 0";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, today);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate user_active_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日新增用户：sys_user 按 create_time 当天过滤
     */
    private BigDecimal calculateUserNewToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COUNT(*) FROM sys_user " +
                    "WHERE create_time::date = ? AND deleted = 0 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, today);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate user_new_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 用户总数：sys_user 中启用状态（status=1）的用户数
     */
    private BigDecimal calculateUserTotalCount(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COUNT(*) FROM sys_user " +
                    "WHERE deleted = 0 AND status = 1 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate user_total_count from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 当前在线用户：无独立会话表，以 sys_login_log 最近15分钟内登录成功的去重用户数近似
     */
    private BigDecimal calculateUserOnlineNow(LocalDateTime calculationTime) {
        try {
            // 假设15分钟内活跃的用户为在线用户
            LocalDateTime fifteenMinutesAgo = calculationTime.minusMinutes(15);
            String sql = "SELECT COUNT(DISTINCT user_id) FROM sys_login_log " +
                    "WHERE login_time >= ? AND login_result = 0";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, fifteenMinutesAgo);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate user_online_now from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public BigDecimal calculateSalesMetric(String metricCode, LocalDateTime calculationTime) {
        switch (metricCode) {
            case "sales_revenue_today":
                return calculateSalesRevenueToday(calculationTime);
            case "sales_order_count_today":
                return calculateSalesOrderCountToday(calculationTime);
            case "sales_avg_order_value":
                return calculateSalesAvgOrderValue(calculationTime);
            case "sales_conversion_rate":
                return calculateSalesConversionRate(calculationTime);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 今日销售收入：erp_sale_order 中 交易完成(5) 订单的 total_amount 求和
     */
    private BigDecimal calculateSalesRevenueToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM erp_sale_order " +
                    "WHERE create_time::date = ? AND status = 5 AND deleted = 0 AND tenant_id = " + currentTenantId();
            BigDecimal amount = jdbcTemplate.queryForObject(sql, BigDecimal.class, today);
            return amount != null ? amount : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate sales_revenue_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日销售订单数：erp_sale_order 中 交易完成(5) 的订单数
     */
    private BigDecimal calculateSalesOrderCountToday(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COUNT(*) FROM erp_sale_order " +
                    "WHERE create_time::date = ? AND status = 5 AND deleted = 0 AND tenant_id = " + currentTenantId();
            Long count = jdbcTemplate.queryForObject(sql, Long.class, today);
            return count != null ? BigDecimal.valueOf(count) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate sales_order_count_today from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日平均订单金额：erp_sale_order 中 交易完成(5) 订单的 total_amount 平均值
     */
    private BigDecimal calculateSalesAvgOrderValue(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COALESCE(AVG(total_amount), 0) FROM erp_sale_order " +
                    "WHERE create_time::date = ? AND status = 5 AND deleted = 0 AND tenant_id = " + currentTenantId();
            BigDecimal avg = jdbcTemplate.queryForObject(sql, BigDecimal.class, today);
            return avg != null ? avg : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate sales_avg_order_value from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 今日销售转化率：交易完成(5)订单数 / 当日订单总数 × 100
     */
    private BigDecimal calculateSalesConversionRate(LocalDateTime calculationTime) {
        try {
            LocalDate today = calculationTime.toLocalDate();
            String sql = "SELECT COALESCE(COUNT(*) FILTER (WHERE status = 5) * 100.0 / NULLIF(COUNT(*), 0), 0) " +
                    "FROM erp_sale_order WHERE create_time::date = ? AND deleted = 0 AND tenant_id = " + currentTenantId();
            BigDecimal rate = jdbcTemplate.queryForObject(sql, BigDecimal.class, today);
            return rate != null ? rate : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate sales_conversion_rate from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateFinanceMetric(String metricCode, LocalDateTime calculationTime) {
        switch (metricCode) {
            case "finance_revenue_month":
                return calculateFinanceRevenueMonth(calculationTime);
            case "finance_profit_margin":
                return calculateFinanceProfitMargin(calculationTime);
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * 本月销售收入：erp_sale_order 中本月 交易完成(5) 订单的 total_amount 求和
     */
    private BigDecimal calculateFinanceRevenueMonth(LocalDateTime calculationTime) {
        try {
            String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM erp_sale_order " +
                    "WHERE status = 5 AND deleted = 0 AND tenant_id = " + currentTenantId() +
                    " AND create_time >= date_trunc('month', CURRENT_DATE)";
            BigDecimal amount = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            return amount != null ? amount : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Could not calculate finance_revenue_month from database: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 利润率：数据源缺失（erp_sale_order 无成本列、fin_profit_statement 等利润表无数据），
     * 不编造数值，返回 0。后续接入成本核算模块后再实现。
     */
    private BigDecimal calculateFinanceProfitMargin(LocalDateTime calculationTime) {
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void saveMetricData(MetricData metricData) {
        metricDataRepository.save(metricData);
    }

    @Override
    @Transactional
    public void batchSaveMetricData(List<MetricData> metricDataList) {
        metricDataRepository.saveAll(metricDataList);
    }
}
