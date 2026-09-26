package cn.aiedge.erp.monitor.service.impl;

import cn.aiedge.erp.monitor.entity.BusinessMetric;
import cn.aiedge.erp.monitor.enums.MetricPeriod;
import cn.aiedge.erp.monitor.enums.MetricStatus;
import cn.aiedge.erp.monitor.enums.MetricType;
import cn.aiedge.erp.monitor.mapper.MonitorBusinessMetricMapper;
import cn.aiedge.erp.monitor.service.MetricCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 指标采集服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricCollectorServiceImpl implements MetricCollectorService {

    private final MonitorBusinessMetricMapper businessMetricMapper;
    /** 用于财务指标的**真实取数**（本模块只依赖 core-base，故直查财务表；同模块 MetricsCalculationServiceImpl 亦是此做法）。 */
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    @Override
    public List<BusinessMetric> collectOrderMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> metrics = new ArrayList<>();
        
        // 今日订单数
        metrics.add(createMetric(tenantId, "order_today_count", "今日订单数", 
            MetricType.ORDER.getCode(), generateRandomValue(100, 200), "笔", 
            "realtime", statTime));
        
        // 今日订单金额
        metrics.add(createMetric(tenantId, "order_today_amount", "今日订单金额", 
            MetricType.ORDER.getCode(), generateRandomValue(30000, 60000), "元", 
            "realtime", statTime));
        
        // 待处理订单数
        metrics.add(createMetric(tenantId, "order_pending_count", "待处理订单数", 
            MetricType.ORDER.getCode(), generateRandomValue(10, 50), "笔", 
            "realtime", statTime));
        
        // 已完成订单数
        metrics.add(createMetric(tenantId, "order_completed_count", "已完成订单数", 
            MetricType.ORDER.getCode(), generateRandomValue(80, 180), "笔", 
            "realtime", statTime));
        
        // 订单取消率
        metrics.add(createMetric(tenantId, "order_cancel_rate", "订单取消率", 
            MetricType.ORDER.getCode(), generateRandomValue(0, 5), "%", 
            "realtime", statTime));
        
        // 平均订单金额
        metrics.add(createMetric(tenantId, "order_avg_amount", "平均订单金额", 
            MetricType.ORDER.getCode(), generateRandomValue(200, 400), "元", 
            "realtime", statTime));
        
        return metrics;
    }

    @Override
    public List<BusinessMetric> collectInventoryMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> metrics = new ArrayList<>();
        
        // 库存总量
        metrics.add(createMetric(tenantId, "inventory_total_quantity", "库存总量", 
            MetricType.INVENTORY.getCode(), generateRandomValue(10000, 15000), "件", 
            "realtime", statTime));
        
        // 库存总价值
        metrics.add(createMetric(tenantId, "inventory_total_value", "库存总价值", 
            MetricType.INVENTORY.getCode(), generateRandomValue(400000, 700000), "元", 
            "realtime", statTime));
        
        // SKU数量
        metrics.add(createMetric(tenantId, "inventory_sku_count", "SKU数量", 
            MetricType.INVENTORY.getCode(), generateRandomValue(300, 400), "个", 
            "realtime", statTime));
        
        // 低库存商品数
        metrics.add(createMetric(tenantId, "inventory_low_stock_count", "低库存商品数", 
            MetricType.INVENTORY.getCode(), generateRandomValue(5, 20), "个", 
            "realtime", statTime));
        
        // 缺货商品数
        metrics.add(createMetric(tenantId, "inventory_out_of_stock_count", "缺货商品数", 
            MetricType.INVENTORY.getCode(), generateRandomValue(0, 5), "个", 
            "realtime", statTime));
        
        // 库存周转率
        metrics.add(createMetric(tenantId, "inventory_turnover_rate", "库存周转率", 
            MetricType.INVENTORY.getCode(), generateRandomValue(3, 6), "次/年", 
            "realtime", statTime));
        
        return metrics;
    }

    @Override
    public List<BusinessMetric> collectUserMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> metrics = new ArrayList<>();
        
        // 用户总数
        metrics.add(createMetric(tenantId, "user_total_count", "用户总数", 
            MetricType.USER.getCode(), generateRandomValue(2000, 3000), "人", 
            "realtime", statTime));
        
        // 今日新增用户
        metrics.add(createMetric(tenantId, "user_new_today", "今日新增用户", 
            MetricType.USER.getCode(), generateRandomValue(5, 20), "人", 
            "realtime", statTime));
        
        // 今日活跃用户
        metrics.add(createMetric(tenantId, "user_active_today", "今日活跃用户", 
            MetricType.USER.getCode(), generateRandomValue(150, 250), "人", 
            "realtime", statTime));
        
        // 在线用户数
        metrics.add(createMetric(tenantId, "user_online_count", "在线用户数", 
            MetricType.USER.getCode(), generateRandomValue(30, 60), "人", 
            "realtime", statTime));
        
        // 用户日活跃率
        metrics.add(createMetric(tenantId, "user_daily_active_rate", "用户日活跃率", 
            MetricType.USER.getCode(), generateRandomValue(5, 15), "%", 
            "realtime", statTime));
        
        return metrics;
    }

    @Override
    public List<BusinessMetric> collectSalesMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> metrics = new ArrayList<>();
        
        // 今日销售额
        metrics.add(createMetric(tenantId, "sales_today_amount", "今日销售额", 
            MetricType.SALES.getCode(), generateRandomValue(30000, 60000), "元", 
            "realtime", statTime));
        
        // 本月销售额
        metrics.add(createMetric(tenantId, "sales_month_amount", "本月销售额", 
            MetricType.SALES.getCode(), generateRandomValue(1000000, 1500000), "元", 
            "realtime", statTime));
        
        // 本年销售额
        metrics.add(createMetric(tenantId, "sales_year_amount", "本年销售额", 
            MetricType.SALES.getCode(), generateRandomValue(10000000, 15000000), "元", 
            "realtime", statTime));
        
        // 销售目标完成率
        metrics.add(createMetric(tenantId, "sales_target_completion", "销售目标完成率", 
            MetricType.SALES.getCode(), generateRandomValue(70, 95), "%", 
            "realtime", statTime));
        
        // 客单价
        metrics.add(createMetric(tenantId, "sales_avg_order_value", "客单价", 
            MetricType.SALES.getCode(), generateRandomValue(250, 350), "元", 
            "realtime", statTime));
        
        return metrics;
    }

    @Override
    public List<BusinessMetric> collectPurchaseMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> metrics = new ArrayList<>();
        
        // 今日采购金额
        metrics.add(createMetric(tenantId, "purchase_today_amount", "今日采购金额", 
            MetricType.PURCHASE.getCode(), generateRandomValue(10000, 30000), "元", 
            "realtime", statTime));
        
        // 本月采购金额
        metrics.add(createMetric(tenantId, "purchase_month_amount", "本月采购金额", 
            MetricType.PURCHASE.getCode(), generateRandomValue(300000, 500000), "元", 
            "realtime", statTime));
        
        // 待入库采购单数
        metrics.add(createMetric(tenantId, "purchase_pending_count", "待入库采购单数", 
            MetricType.PURCHASE.getCode(), generateRandomValue(5, 20), "笔", 
            "realtime", statTime));
        
        return metrics;
    }

    /**
     * 财务指标采集（**真实取数**）。
     *
     * <p><b>2026-09-26 修复</b>：本方法原先 5 个指标**全部是 {@code generateRandomValue(...)} 随机数**
     * （见 FINANCE_MODULE_AUDIT §3.11）——经营看板/监控大屏上显示的「今日收入/支出/应收/应付/毛利率」
     * 与实际账务毫无关系，会误导经营决策。现改为按真实数据取数。</p>
     *
     * <p><b>取数口径</b>（按会计科目属性判定，不用硬编码科目编码前缀）：</p>
     * <ul>
     *   <li>收入类科目 = {@code finance_account_subject.subject_type = 5 AND direction = 2}（损益类·贷方）</li>
     *   <li>费用类科目 = {@code subject_type = 5 AND direction = 1}（损益类·借方）</li>
     *   <li>成本类科目 = {@code subject_type = 4}</li>
     *   <li>今日收入/支出 = 凭证日期为当日的**已过账**凭证分录净额（收入取贷方净额、支出取借方净额）</li>
     *   <li>应收/应付账款 = {@code finance_receivable} / {@code finance_payable} 的
     *       {@code remaining_amount} 合计（未结清部分，排除已取消 status='cancelled'）</li>
     *   <li>毛利率 = (本年累计收入 − 本年累计成本) ÷ 本年累计收入 × 100，收入为 0 时置 0</li>
     * </ul>
     * <p>⚠️ 口径如需调整（例如「今日」改按记账日期而非凭证日期），改本方法的 SQL 即可；
     * 各指标取数失败时降级为 0 并记日志，不阻断其它指标。</p>
     */
    @Override
    public List<BusinessMetric> collectFinanceMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> metrics = new ArrayList<>();
        java.time.LocalDate today = statTime.toLocalDate();
        int year = today.getYear();

        // 今日收入：当日已过账凭证中，收入类科目的贷方净额
        BigDecimal todayRevenue = queryDecimal(
                "SELECT COALESCE(SUM(i.credit_amount - i.debit_amount), 0) " +
                "FROM finance_voucher_item i " +
                "JOIN finance_voucher v ON v.id = i.voucher_id " +
                "JOIN finance_account_subject s ON s.id = i.subject_id " +
                "WHERE v.status = 'posted' AND v.deleted_flag = 0 AND i.deleted_flag = 0 " +
                "  AND s.subject_type = 5 AND s.direction = 2 " +
                "  AND v.voucher_date = ? AND v.tenant_id = ?", today, tenantId);

        // 今日支出：当日已过账凭证中，费用类科目的借方净额
        BigDecimal todayExpense = queryDecimal(
                "SELECT COALESCE(SUM(i.debit_amount - i.credit_amount), 0) " +
                "FROM finance_voucher_item i " +
                "JOIN finance_voucher v ON v.id = i.voucher_id " +
                "JOIN finance_account_subject s ON s.id = i.subject_id " +
                "WHERE v.status = 'posted' AND v.deleted_flag = 0 AND i.deleted_flag = 0 " +
                "  AND s.subject_type = 5 AND s.direction = 1 " +
                "  AND v.voucher_date = ? AND v.tenant_id = ?", today, tenantId);

        // 应收账款余额（未结清）
        BigDecimal receivable = queryDecimal(
                "SELECT COALESCE(SUM(remaining_amount), 0) FROM finance_receivable " +
                "WHERE deleted_flag = 0 AND tenant_id = ? AND COALESCE(status, 'normal') <> 'cancelled'", tenantId);

        // 应付账款余额（未结清）
        BigDecimal payable = queryDecimal(
                "SELECT COALESCE(SUM(remaining_amount), 0) FROM finance_payable " +
                "WHERE deleted_flag = 0 AND tenant_id = ? AND COALESCE(status, 'normal') <> 'cancelled'", tenantId);

        // 毛利率：本年累计（收入 − 成本）÷ 收入 × 100
        BigDecimal yearRevenue = queryDecimal(
                "SELECT COALESCE(SUM(i.credit_amount - i.debit_amount), 0) " +
                "FROM finance_voucher_item i " +
                "JOIN finance_voucher v ON v.id = i.voucher_id " +
                "JOIN finance_account_subject s ON s.id = i.subject_id " +
                "WHERE v.status = 'posted' AND v.deleted_flag = 0 AND i.deleted_flag = 0 " +
                "  AND s.subject_type = 5 AND s.direction = 2 " +
                "  AND v.fiscal_year = ? AND v.tenant_id = ?", year, tenantId);
        BigDecimal yearCost = queryDecimal(
                "SELECT COALESCE(SUM(i.debit_amount - i.credit_amount), 0) " +
                "FROM finance_voucher_item i " +
                "JOIN finance_voucher v ON v.id = i.voucher_id " +
                "JOIN finance_account_subject s ON s.id = i.subject_id " +
                "WHERE v.status = 'posted' AND v.deleted_flag = 0 AND i.deleted_flag = 0 " +
                "  AND s.subject_type = 4 " +
                "  AND v.fiscal_year = ? AND v.tenant_id = ?", year, tenantId);
        BigDecimal grossMargin = yearRevenue.signum() == 0
                ? BigDecimal.ZERO
                : yearRevenue.subtract(yearCost)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(yearRevenue, 2, RoundingMode.HALF_UP);

        metrics.add(createMetric(tenantId, "finance_today_revenue", "今日收入",
                MetricType.FINANCE.getCode(), todayRevenue, "元", "realtime", statTime));
        metrics.add(createMetric(tenantId, "finance_today_expense", "今日支出",
                MetricType.FINANCE.getCode(), todayExpense, "元", "realtime", statTime));
        metrics.add(createMetric(tenantId, "finance_receivable", "应收账款",
                MetricType.FINANCE.getCode(), receivable, "元", "realtime", statTime));
        metrics.add(createMetric(tenantId, "finance_payable", "应付账款",
                MetricType.FINANCE.getCode(), payable, "元", "realtime", statTime));
        metrics.add(createMetric(tenantId, "finance_gross_margin", "毛利率",
                MetricType.FINANCE.getCode(), grossMargin, "%", "realtime", statTime));

        return metrics;
    }

    /**
     * 财务指标取数兜底：查询异常时记日志并返回 0，避免单个指标取数失败影响其它指标。
     * （注意：返回 0 与「真实值为 0」不可区分，故异常必须留日志。）
     */
    private BigDecimal queryDecimal(String sql, Object... args) {
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return value != null ? value : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("[财务指标] 取数失败，本次按 0 处理: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    public List<BusinessMetric> collectAllMetrics(Long tenantId, LocalDateTime statTime) {
        List<BusinessMetric> allMetrics = new ArrayList<>();
        
        allMetrics.addAll(collectOrderMetrics(tenantId, statTime));
        allMetrics.addAll(collectInventoryMetrics(tenantId, statTime));
        allMetrics.addAll(collectUserMetrics(tenantId, statTime));
        allMetrics.addAll(collectSalesMetrics(tenantId, statTime));
        allMetrics.addAll(collectPurchaseMetrics(tenantId, statTime));
        allMetrics.addAll(collectFinanceMetrics(tenantId, statTime));
        
        return allMetrics;
    }

    @Override
    public BusinessMetric calculateRealTimeMetric(String metricCode, Long tenantId) {
        // 实时计算特定指标
        BusinessMetric metric = new BusinessMetric();
        metric.setTenantId(tenantId);
        metric.setMetricCode(metricCode);
        metric.setMetricValue(generateRandomValue(100, 1000));
        metric.setPeriod("realtime");
        metric.setStatTime(LocalDateTime.now());
        metric.setStatus(MetricStatus.NORMAL.getCode());
        return metric;
    }

    @Override
    @Transactional
    public int batchCollectAndSave(Long tenantId, List<String> metricTypes) {
        LocalDateTime statTime = LocalDateTime.now();
        List<BusinessMetric> allMetrics = new ArrayList<>();
        
        for (String type : metricTypes) {
            switch (type) {
                case "order":
                    allMetrics.addAll(collectOrderMetrics(tenantId, statTime));
                    break;
                case "inventory":
                    allMetrics.addAll(collectInventoryMetrics(tenantId, statTime));
                    break;
                case "user":
                    allMetrics.addAll(collectUserMetrics(tenantId, statTime));
                    break;
                case "sales":
                    allMetrics.addAll(collectSalesMetrics(tenantId, statTime));
                    break;
                case "purchase":
                    allMetrics.addAll(collectPurchaseMetrics(tenantId, statTime));
                    break;
                case "finance":
                    allMetrics.addAll(collectFinanceMetrics(tenantId, statTime));
                    break;
                default:
                    log.warn("Unknown metric type: {}", type);
            }
        }
        
        // 批量保存
        if (!allMetrics.isEmpty()) {
            return businessMetricMapper.batchInsertOrUpdate(allMetrics);
        }
        
        return 0;
    }
    
    private BusinessMetric createMetric(Long tenantId, String code, String name, 
                                         String type, BigDecimal value, String unit, 
                                         String period, LocalDateTime statTime) {
        BusinessMetric metric = new BusinessMetric();
        metric.setTenantId(tenantId);
        metric.setMetricCode(code);
        metric.setMetricName(name);
        metric.setMetricType(type);
        metric.setMetricValue(value);
        metric.setUnit(unit);
        metric.setPeriod(period);
        metric.setStatTime(statTime);
        metric.setStatus(MetricStatus.NORMAL.getCode());
        return metric;
    }
    
    private BigDecimal generateRandomValue(int min, int max) {
        int value = min + random.nextInt(max - min);
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
}