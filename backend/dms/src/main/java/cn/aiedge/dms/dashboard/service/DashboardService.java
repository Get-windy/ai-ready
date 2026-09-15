package cn.aiedge.dms.dashboard.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.dashboard.dto.CapacityAggVO;
import cn.aiedge.dms.dashboard.dto.DashboardStatsVO;
import cn.aiedge.dms.dashboard.dto.DistributionItemVO;
import cn.aiedge.dms.dashboard.dto.TaskAggVO;
import cn.aiedge.dms.dashboard.dto.TaskSummaryItemVO;
import cn.aiedge.dms.dashboard.dto.TopRiderVO;
import cn.aiedge.dms.dashboard.dto.TrendPointVO;
import cn.aiedge.dms.dashboard.mapper.DashboardMapper;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import cn.aiedge.dms.verification.enums.BindingStatusEnum;
import cn.aiedge.dms.verification.mapper.DmsRiderVehicleBindingMapper;
import cn.aiedge.dms.verification.mapper.DmsVerificationAlertMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送仪表盘聚合服务
 *
 * 所有指标均在数据库侧聚合，页面不做二次汇总；时间范围口径见 {@link RangeWindow}。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 自动刷新间隔（秒）配置键 */
    public static final String CFG_REFRESH_SECONDS = "dashboard.refresh.seconds";
    /** 在线配送员判定阈值（分钟）配置键 */
    public static final String CFG_ONLINE_MINUTES = "dashboard.online.minutes";

    private static final int DEFAULT_REFRESH_SECONDS = 30;
    private static final int DEFAULT_ONLINE_MINUTES = 2;
    private static final int TREND_MAX_DAYS = 90;
    private static final int TOP_RIDER_DEFAULT_LIMIT = 10;
    private static final int TOP_RIDER_MAX_LIMIT = 50;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 订单类型字典（与 dms_task.order_type 注释口径一致） */
    private static final Map<Integer, String> ORDER_TYPE_NAMES = Map.of(
            1, "销售配送",
            2, "调拨",
            3, "退货");

    private final DashboardMapper dashboardMapper;
    private final DmsRiderVehicleBindingMapper bindingMapper;
    private final DmsVerificationAlertMapper alertMapper;

    // ══════════════════════════════════════════════════════════
    // 时间范围解析（口径固化：左闭右开 [start, end)）
    // ══════════════════════════════════════════════════════════

    /**
     * 查询区间。
     *
     * @param rangeKey     范围标识（today/yesterday/last7/last30/month/custom）
     * @param rangeLabel   范围文案
     * @param start        起始时间（含）
     * @param endExclusive 截止时间（不含）
     * @param trendDays    趋势图天数（今日/昨日 → 近 7 日，近 30 日/本月 → 近 30 日）
     */
    public record RangeWindow(String rangeKey, String rangeLabel,
                              LocalDateTime start, LocalDateTime endExclusive, int trendDays) {
    }

    public RangeWindow resolveRange(String range, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        String key = (range == null || range.isBlank()) ? "today" : range.trim();
        LocalDate start;
        LocalDate endExclusive;
        String label;
        int trendDays;

        switch (key) {
            case "yesterday" -> {
                start = today.minusDays(1);
                endExclusive = today;
                label = "昨日";
                trendDays = 7;
            }
            case "last7" -> {
                start = today.minusDays(6);
                endExclusive = today.plusDays(1);
                label = "近 7 日";
                trendDays = 7;
            }
            case "last30" -> {
                start = today.minusDays(29);
                endExclusive = today.plusDays(1);
                label = "近 30 日";
                trendDays = 30;
            }
            case "month" -> {
                start = today.withDayOfMonth(1);
                endExclusive = today.plusDays(1);
                label = "本月";
                trendDays = 30;
            }
            case "custom" -> {
                if (startDate == null) {
                    throw new DmsBusinessException("自定义时间范围必须指定开始日期");
                }
                start = startDate;
                endExclusive = (endDate == null ? startDate : endDate).plusDays(1);
                if (endExclusive.isBefore(start.plusDays(1))) {
                    throw new DmsBusinessException("结束日期不能早于开始日期");
                }
                label = start.format(DATE_FMT) + " ~ " + endExclusive.minusDays(1).format(DATE_FMT);
                // 注意：Period.getDays() 只返回「天数部分」（跨月为 0），必须用 ChronoUnit 计算总天数
                long days = ChronoUnit.DAYS.between(start, endExclusive);
                trendDays = (int) Math.min(Math.max(days, 1), TREND_MAX_DAYS);
            }
            default -> {
                key = "today";
                start = today;
                endExclusive = today.plusDays(1);
                label = "今日";
                trendDays = 7;
            }
        }
        return new RangeWindow(key, label,
                start.atStartOfDay(), endExclusive.atStartOfDay(), trendDays);
    }

    // ══════════════════════════════════════════════════════════
    // KPI 聚合
    // ══════════════════════════════════════════════════════════

    public DashboardStatsVO getStats(String range, LocalDate startDate, LocalDate endDate,
                                     Long channelId, Integer orderType) {
        Long tenantId = currentTenantId();
        RangeWindow window = resolveRange(range, startDate, endDate);

        TaskAggVO task = dashboardMapper.aggregateTask(
                tenantId, window.start(), window.endExclusive(), channelId, orderType);
        CapacityAggVO rider = dashboardMapper.aggregateRider(tenantId, onlineSince(tenantId));
        CapacityAggVO vehicle = dashboardMapper.aggregateVehicle(tenantId);

        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setRange(window.rangeKey());
        vo.setRangeLabel(window.rangeLabel());
        vo.setStartTime(window.start().format(DATETIME_FMT));
        vo.setEndTime(window.endExclusive().minusSeconds(1).format(DATETIME_FMT));

        vo.setTotalRiders(rider == null ? 0L : nz(rider.getTotalRiders()));
        vo.setActiveRiders(rider == null ? 0L : nz(rider.getActiveRiders()));
        vo.setOnlineRiders(rider == null ? 0L : nz(rider.getOnlineRiders()));
        vo.setTotalVehicles(vehicle == null ? 0L : nz(vehicle.getTotalVehicles()));
        vo.setActiveVehicles(vehicle == null ? 0L : nz(vehicle.getActiveVehicles()));

        if (task != null) {
            vo.setOrderCount(nz(task.getOrderCount()));
            vo.setPendingOrders(nz(task.getPendingOrders()));
            vo.setAssignedOrders(nz(task.getAssignedOrders()));
            vo.setInTransitOrders(nz(task.getInTransitOrders()));
            vo.setCompletedOrders(nz(task.getCompletedOrders()));
            vo.setCancelledOrders(nz(task.getCancelledOrders()));
            vo.setExceptionOrders(nz(task.getExceptionOrders()));
            vo.setOverdueOrders(nz(task.getOverdueOrders()));
            vo.setOnTimeRate(rate(task.getOnTimeCount(), task.getOnTimeBase()));
            vo.setAvgDeliveryMinutes(scale(task.getAvgDeliveryMinutes()));
            vo.setDeliveryFee(amount(task.getDeliveryFee()));
            vo.setCollectOnDelivery(amount(task.getCollectOnDelivery()));
            vo.setGoodsAmount(amount(task.getGoodsAmount()));
        } else {
            vo.setOrderCount(0L);
            vo.setPendingOrders(0L);
            vo.setAssignedOrders(0L);
            vo.setInTransitOrders(0L);
            vo.setCompletedOrders(0L);
            vo.setCancelledOrders(0L);
            vo.setExceptionOrders(0L);
            vo.setOverdueOrders(0L);
            vo.setDeliveryFee(BigDecimal.ZERO);
            vo.setCollectOnDelivery(BigDecimal.ZERO);
            vo.setGoodsAmount(BigDecimal.ZERO);
        }

        vo.setActiveBindingCount(nz(dashboardMapper.countActiveBindings(tenantId)));
        vo.setPendingAlertCount(nz(dashboardMapper.countPendingAlerts(tenantId)));
        vo.setOnlineThresholdMinutes(readIntConfig(tenantId, CFG_ONLINE_MINUTES, DEFAULT_ONLINE_MINUTES, 1, 120));
        vo.setRefreshSeconds(readIntConfig(tenantId, CFG_REFRESH_SECONDS, DEFAULT_REFRESH_SECONDS, 5, 600));
        return vo;
    }

    /** 任务状态分布（补齐状态名与占比，区间内无数据的状态返回 0 便于图表稳定渲染） */
    public List<TaskSummaryItemVO> getTaskSummary(String range, LocalDate startDate, LocalDate endDate,
                                                  Long channelId, Integer orderType) {
        Long tenantId = currentTenantId();
        RangeWindow window = resolveRange(range, startDate, endDate);
        List<TaskSummaryItemVO> rows = dashboardMapper.selectStatusDistribution(
                tenantId, window.start(), window.endExclusive(), channelId, orderType);

        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (TaskStatusEnum status : TaskStatusEnum.values()) {
            counts.put(status.getValue(), 0L);
        }
        if (rows != null) {
            for (TaskSummaryItemVO row : rows) {
                if (row.getStatus() != null) {
                    counts.put(row.getStatus(), nz(row.getCount()));
                }
            }
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        List<TaskSummaryItemVO> result = new ArrayList<>();
        counts.forEach((status, count) -> {
            TaskSummaryItemVO item = new TaskSummaryItemVO();
            item.setStatus(status);
            item.setStatusName(TaskStatusEnum.fromValue(status).getDescription());
            item.setCount(count);
            item.setRatio(total > 0
                    ? BigDecimal.valueOf(count * 100.0 / total).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            result.add(item);
        });
        return result;
    }

    /**
     * 单量/时效趋势：缺失日期补零，保证折线连续。
     *
     * 区间取值规则（与 KPI 口径一致）：
     * · custom —— 直接使用自定义区间，避免「统计区间是历史月份、趋势却是近 30 天」的口径错位；
     * · 其它 —— 截止今日、向前取 N 天（N 由 days 显式指定或按 range 推导：今日/昨日=7，近 30 日/本月=30）；
     * · 两种情况均受 {@link #TREND_MAX_DAYS} 上限保护，超出时取区间尾部。
     */
    public List<TrendPointVO> getTrend(String range, LocalDate startDate, LocalDate endDate,
                                       Long channelId, Integer orderType, Integer days) {
        Long tenantId = currentTenantId();
        RangeWindow window = resolveRange(range, startDate, endDate);

        LocalDate today = LocalDate.now();
        LocalDate startDay;
        LocalDate endDay;
        if ("custom".equals(window.rangeKey()) && startDate != null) {
            startDay = startDate;
            endDay = endDate == null ? startDate : endDate;
        } else {
            int span = (days != null && days > 0) ? Math.min(days, TREND_MAX_DAYS) : window.trendDays();
            endDay = today;
            startDay = today.minusDays(span - 1L);
        }
        if (ChronoUnit.DAYS.between(startDay, endDay) + 1 > TREND_MAX_DAYS) {
            startDay = endDay.minusDays(TREND_MAX_DAYS - 1L);
        }
        int span = (int) ChronoUnit.DAYS.between(startDay, endDay) + 1;

        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime endExclusive = endDay.plusDays(1).atStartOfDay();

        List<TrendPointVO> rows = dashboardMapper.selectTrend(
                tenantId, start, endExclusive, channelId, orderType);
        Map<String, TrendPointVO> byDate = new LinkedHashMap<>();
        if (rows != null) {
            for (TrendPointVO row : rows) {
                byDate.put(row.getStatDate(), row);
            }
        }

        List<TrendPointVO> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            String date = startDay.plusDays(i).format(DATE_FMT);
            TrendPointVO row = byDate.get(date);
            if (row == null) {
                row = new TrendPointVO();
                row.setStatDate(date);
                row.setOrderCount(0L);
                row.setCompletedCount(0L);
                row.setOnTimeCount(0L);
                row.setOnTimeBase(0L);
                row.setDeliveryFee(BigDecimal.ZERO);
            } else {
                row.setOrderCount(nz(row.getOrderCount()));
                row.setCompletedCount(nz(row.getCompletedCount()));
                row.setOnTimeCount(nz(row.getOnTimeCount()));
                row.setOnTimeBase(nz(row.getOnTimeBase()));
                row.setDeliveryFee(amount(row.getDeliveryFee()));
            }
            row.setOnTimeRate(rate(row.getOnTimeCount(), row.getOnTimeBase()));
            row.setAvgMinutes(scale(row.getAvgMinutes()));
            result.add(row);
        }
        return result;
    }

    /** 配送员绩效 Top */
    public List<TopRiderVO> getTopRiders(String range, LocalDate startDate, LocalDate endDate,
                                         Long channelId, Integer orderType, Integer limit) {
        Long tenantId = currentTenantId();
        RangeWindow window = resolveRange(range, startDate, endDate);
        int size = (limit == null || limit <= 0) ? TOP_RIDER_DEFAULT_LIMIT : Math.min(limit, TOP_RIDER_MAX_LIMIT);

        List<TopRiderVO> rows = dashboardMapper.selectTopRiders(
                tenantId, window.start(), window.endExclusive(), channelId, orderType, size);
        if (rows == null) {
            return List.of();
        }
        rows.forEach(row -> {
            row.setOrderCount(nz(row.getOrderCount()));
            row.setCompletedCount(nz(row.getCompletedCount()));
            row.setOnTimeCount(nz(row.getOnTimeCount()));
            row.setOnTimeBase(nz(row.getOnTimeBase()));
            row.setOnTimeRate(rate(row.getOnTimeCount(), row.getOnTimeBase()));
            row.setAvgMinutes(scale(row.getAvgMinutes()));
            if (row.getRiderName() == null || row.getRiderName().isBlank()) {
                row.setRiderName("配送员#" + row.getRiderId());
            }
        });
        return rows;
    }

    /**
     * 任务分布
     *
     * @param by channel（按运力渠道）| orderType（按订单类型，默认）
     */
    public List<DistributionItemVO> getDistribution(String by, String range, LocalDate startDate,
                                                    LocalDate endDate, Long channelId, Integer orderType) {
        Long tenantId = currentTenantId();
        RangeWindow window = resolveRange(range, startDate, endDate);
        boolean byChannel = "channel".equalsIgnoreCase(by);
        List<DistributionItemVO> rows = byChannel
                ? dashboardMapper.selectChannelDistribution(
                        tenantId, window.start(), window.endExclusive(), channelId, orderType)
                : dashboardMapper.selectOrderTypeDistribution(
                        tenantId, window.start(), window.endExclusive(), channelId, orderType);
        if (rows == null) {
            return List.of();
        }
        rows.forEach(row -> {
            row.setOrderCount(nz(row.getOrderCount()));
            row.setAmount(amount(row.getAmount()));
            if (byChannel) {
                if (row.getItemName() == null || row.getItemName().isBlank()) {
                    row.setItemName("未知渠道");
                }
            } else {
                row.setItemName(orderTypeName(row.getItemKey()));
            }
        });
        return rows;
    }

    /**
     * 订单类型分布文案
     *
     * <p>口径：`-1` / 空 / 非数字 = **未指定**（历史或导入任务未填 order_type，SQL 侧已 COALESCE 成 -1），
     * 未登记的编码回落「其他」。**不可**直接用 {@code Map.of(...).getOrDefault(key, ...)}：
     * 不可变 Map 对 null key 取 hash 会抛 NPE（DistributionItemVO.itemKey 为 null 时整页 500）。</p>
     */
    private String orderTypeName(String key) {
        Integer code = parseInt(key);
        if (code == null || code < 0) {
            return "未指定";
        }
        return ORDER_TYPE_NAMES.getOrDefault(code, "其他");
    }

    // ══════════════════════════════════════════════════════════
    // 待办：活跃人车绑定 / 待处理预警
    // ══════════════════════════════════════════════════════════

    /** 活跃人车绑定（status = 0 绑定中），按绑定时间倒序 */
    public IPage<DmsRiderVehicleBinding> pageActiveBindings(int page, int size) {
        Page<DmsRiderVehicleBinding> p = new Page<>(page, size);
        return bindingMapper.selectPage(p, new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                .orderByDesc(DmsRiderVehicleBinding::getBindTime)
                .orderByDesc(DmsRiderVehicleBinding::getId));
    }

    /** 核验预警，默认仅未处理（handle_status = 0）；未处理优先、级别高的在前 */
    public IPage<DmsVerificationAlert> pagePendingAlerts(int page, int size, Integer handleStatus) {
        Page<DmsVerificationAlert> p = new Page<>(page, size);
        LambdaQueryWrapper<DmsVerificationAlert> wrapper = new LambdaQueryWrapper<>();
        if (handleStatus != null) {
            wrapper.eq(DmsVerificationAlert::getHandleStatus, handleStatus);
        } else {
            wrapper.eq(DmsVerificationAlert::getHandleStatus, 0);
        }
        wrapper.orderByDesc(DmsVerificationAlert::getAlertLevel)
                .orderByDesc(DmsVerificationAlert::getCreateTime)
                .orderByDesc(DmsVerificationAlert::getId);
        return alertMapper.selectPage(p, wrapper);
    }

    // ══════════════════════════════════════════════════════════
    // 内部工具
    // ══════════════════════════════════════════════════════════

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new DmsBusinessException("无法获取当前租户上下文");
        }
        return tenantId;
    }

    /** 在线判定阈值（分钟）→ 上报时间下界 */
    private LocalDateTime onlineSince(Long tenantId) {
        int minutes = readIntConfig(tenantId, CFG_ONLINE_MINUTES, DEFAULT_ONLINE_MINUTES, 1, 120);
        return LocalDateTime.now().minusMinutes(minutes);
    }

    private int readIntConfig(Long tenantId, String key, int defaultValue, int min, int max) {
        try {
            String raw = dashboardMapper.selectConfigValue(tenantId, key);
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }
            int value = Integer.parseInt(raw.trim());
            return Math.min(Math.max(value, min), max);
        } catch (Exception e) {
            log.debug("读取配送参数失败，使用默认值: key={}, tenantId={}", key, tenantId, e);
            return defaultValue;
        }
    }

    private Long nz(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }

    /** 百分比，保留 1 位小数；分母为 0 返回 null（前端显示「-」表示无有效样本） */
    private BigDecimal rate(Long numerator, Long denominator) {
        if (denominator == null || denominator == 0L) {
            return null;
        }
        long top = numerator == null ? 0L : numerator;
        return BigDecimal.valueOf(top * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP);
    }

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
