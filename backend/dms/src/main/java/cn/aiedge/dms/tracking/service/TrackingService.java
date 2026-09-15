package cn.aiedge.dms.tracking.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.dms.common.enums.RiderTypeEnum;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.enums.TrackingSourceEnum;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.config.service.ConfigService;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.tracking.dto.RiderLocationQuery;
import cn.aiedge.dms.tracking.dto.RiderLocationVO;
import cn.aiedge.dms.tracking.dto.TrackingAlertVO;
import cn.aiedge.dms.tracking.dto.TrackingMileageVO;
import cn.aiedge.dms.tracking.dto.TrackingQueryDTO;
import cn.aiedge.dms.tracking.dto.TrackingStatVO;
import cn.aiedge.dms.tracking.dto.TrackingVO;
import cn.aiedge.dms.tracking.entity.DmsTracking;
import cn.aiedge.dms.tracking.mapper.DmsTrackingMapper;
import cn.aiedge.dms.verification.enums.AlertTypeEnum;
import cn.aiedge.dms.verification.service.VerificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 位置追踪服务（配送 → 配送跟踪 → 配送跟踪）
 *
 * <p>《配送跟踪开发文档》落地口径：</p>
 * <ul>
 *   <li>§3.4 补齐 <b>台账分页</b>（联查配送员姓名/电话 + 任务编号）与 <b>里程聚合</b>、<b>导出</b>；</li>
 *   <li>§3.5.2 抽稀：明细/轨迹查询限制返回条数（{@code maxPoints}），首尾点必留；</li>
 *   <li>§3.5.3 来源可溯：来源字典 {@link TrackingSourceEnum}（APP/后台补录/渠道回传）；</li>
 *   <li>§3.5.1 时序：明细查询走时间索引（迁移 V11.215.0 已建 rider+time / task+time / time 索引）。</li>
 * </ul>
 *
 * <p>与《实时跟踪》的分工：本页是**明细台账**（可查可导出可对账），实时跟踪是**可视化盯盘**；
 * 地图能力统一复用前端 `components/business/RouteMapCanvas`，不在本模块另造一套。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    /** 速度异常阈值（km/h）：用于页面标红，与《实名认证》预警口径解耦但一致 */
    private static final BigDecimal SPEED_ABNORMAL = BigDecimal.valueOf(60);

    /** 里程聚合一次最多扫描的轨迹点数（防止全表计算，§3.5.2） */
    private static final int MILEAGE_SCAN_LIMIT = 20000;

    /** 轨迹抽稀默认目标点数 */
    private static final int DEFAULT_MAX_POINTS = 500;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 在线判定阈值配置键（分钟） */
    public static final String CFG_ONLINE_MINUTES = "dms.tracking.online.minutes";
    /** 超速阈值配置键（km/h） */
    public static final String CFG_SPEED_LIMIT = "dms.tracking.speed.limit";
    /** 异常停留阈值配置键（分钟） */
    public static final String CFG_STOP_MINUTES = "dms.tracking.stop.minutes";
    /** 偏航阈值配置键（米）：当前位置偏离「起点→客户」配送基线的上限 */
    public static final String CFG_DEVIATION_METERS = "dms.tracking.deviation.meters";
    /** 位置采集时段配置键（`HH:mm-HH:mm`，支持跨天；缺失/空=全天采集） */
    public static final String CFG_COLLECT_HOURS = "dms.tracking.collect.hours";
    /** 轨迹保留天数配置键（0/缺失=不自动清理） */
    public static final String CFG_RETENTION_DAYS = "dms.tracking.retention.days";

    /** 「在途」任务状态：已分配 / 已接单 / 取货中 / 配送中（与任务状态机一致） */
    private static final List<Integer> IN_TRANSIT_STATUS = List.of(
            TaskStatusEnum.ASSIGNED.getValue(), TaskStatusEnum.ACCEPTED.getValue(),
            TaskStatusEnum.PICKING_UP.getValue(), TaskStatusEnum.DELIVERING.getValue());

    private static final int DEFAULT_ONLINE_MINUTES = 2;
    private static final int DEFAULT_SPEED_LIMIT = 60;
    private static final int DEFAULT_STOP_MINUTES = 15;
    private static final int DEFAULT_DEVIATION_METERS = 1000;
    /** 异常停留判定：位移小于该值(米)视为未移动 */
    private static final int STAY_MOVE_METERS = 50;
    /** 位置聚合的回溯窗口(天)：超过该窗口无上报视为「无位置数据」 */
    private static final int LOCATION_LOOKBACK_DAYS = 30;
    /** 预警一次返回上限 */
    private static final int ALERT_LIMIT = 200;

    private final DmsTrackingMapper trackingMapper;
    private final DmsRiderMapper riderMapper;
    private final DmsTaskMapper taskMapper;
    private final ConfigService configService;
    /**
     * 异常联动预警（§3.5.5）：复用《人员核验》预警体系。
     *
     * <p>用 {@link ObjectProvider} 延迟解析而非直接构造注入——{@code VerificationService} 反向依赖本类，
     * 构造注入会形成无法破解的构造器循环。</p>
     */
    private final ObjectProvider<VerificationService> verificationServiceProvider;

    // ==================== 台账 ====================

    public Page<TrackingVO> page(TrackingQueryDTO query) {
        TrackingQueryDTO q = query != null ? query : new TrackingQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<DmsTracking> entityPage = trackingMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(q));
        Page<TrackingVO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        // 台账带「段间里程」派生列（同页内同配送员相邻点直线距离）
        result.setRecords(toVOList(entityPage.getRecords(), true));
        return result;
    }

    /** 导出用全量列表 */
    public List<TrackingVO> list(TrackingQueryDTO query) {
        return toVOList(trackingMapper.selectList(buildWrapper(query != null ? query : new TrackingQueryDTO())), false);
    }

    /** 查询条件装配（§3.3） */
    private LambdaQueryWrapper<DmsTracking> buildWrapper(TrackingQueryDTO q) {
        LambdaQueryWrapper<DmsTracking> wrapper = new LambdaQueryWrapper<>();
        if (q.getRiderId() != null) {
            wrapper.eq(DmsTracking::getRiderId, q.getRiderId());
        }
        if (StringUtils.hasText(q.getRiderKeyword())) {
            String kw = q.getRiderKeyword().trim();
            Set<Long> ids = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                            .select(DmsRider::getId)
                            .and(w -> w.like(DmsRider::getRealName, kw).or().like(DmsRider::getPhone, kw)))
                    .stream().map(DmsRider::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            wrapper.in(DmsTracking::getRiderId, ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids));
        }
        if (q.getRiderStatus() != null) {
            Set<Long> ids = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                            .select(DmsRider::getId)
                            .eq(DmsRider::getStatus, q.getRiderStatus()))
                    .stream().map(DmsRider::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            wrapper.in(DmsTracking::getRiderId, ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids));
        }
        if (q.getOnline() != null) {
            // 「在线」口径 = 位置上报心跳（与《配送员管理》《实时跟踪》统一），而非人工状态 riderStatus
            LocalDateTime onlineSince = LocalDateTime.now()
                    .minusMinutes(cfgInt(CFG_ONLINE_MINUTES, DEFAULT_ONLINE_MINUTES));
            LambdaQueryWrapper<DmsRider> onlineWrapper = new LambdaQueryWrapper<DmsRider>()
                    .select(DmsRider::getId)
                    .ge(Boolean.TRUE.equals(q.getOnline()), DmsRider::getLastReportTime, onlineSince)
                    .and(Boolean.FALSE.equals(q.getOnline()), w -> w
                            .isNull(DmsRider::getLastReportTime)
                            .or().lt(DmsRider::getLastReportTime, onlineSince));
            Set<Long> ids = riderMapper.selectList(onlineWrapper).stream()
                    .map(DmsRider::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            wrapper.in(DmsTracking::getRiderId, ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids));
        }
        if (Boolean.TRUE.equals(q.getActiveToday())) {
            Set<Long> ids = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                            .select(DmsRider::getId)
                            .ge(DmsRider::getLastReportTime, LocalDate.now().atStartOfDay()))
                    .stream().map(DmsRider::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            wrapper.in(DmsTracking::getRiderId, ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids));
        }
        if (q.getTaskId() != null) {
            wrapper.eq(DmsTracking::getTaskId, q.getTaskId());
        }
        if (StringUtils.hasText(q.getTaskNo())) {
            Set<Long> ids = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                            .select(DmsTask::getId)
                            .like(DmsTask::getTaskNo, q.getTaskNo().trim()))
                    .stream().map(DmsTask::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            wrapper.in(DmsTracking::getTaskId, ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids));
        }
        LocalDateTime start = parseTime(q.getStartTime(), true);
        LocalDateTime end = parseTime(q.getEndTime(), false);
        wrapper.ge(start != null, DmsTracking::getReportTime, start);
        wrapper.le(end != null, DmsTracking::getReportTime, end);
        wrapper.in(q.getSources() != null && !q.getSources().isEmpty(), DmsTracking::getSource, q.getSources());
        wrapper.ge(q.getMinSpeed() != null, DmsTracking::getSpeed, q.getMinSpeed());
        if (Boolean.TRUE.equals(q.getOnlyWithTask())) {
            wrapper.isNotNull(DmsTracking::getTaskId);
        }
        applySort(wrapper, q);
        return wrapper;
    }

    private void applySort(LambdaQueryWrapper<DmsTracking> wrapper, TrackingQueryDTO q) {
        boolean asc = "asc".equalsIgnoreCase(q.getSortOrder());
        String field = q.getSortField() == null ? "" : q.getSortField();
        switch (field) {
            case "reportTime" -> wrapper.orderBy(true, asc, DmsTracking::getReportTime);
            case "speed" -> wrapper.orderBy(true, asc, DmsTracking::getSpeed);
            default -> wrapper.orderByDesc(DmsTracking::getReportTime);
        }
    }

    // ==================== 轨迹 ====================

    /** 按配送员取轨迹（含抽稀），供地图绘制/回放 */
    public List<TrackingVO> trackVO(Long riderId, String startTime, String endTime, Integer maxPoints) {
        if (riderId == null) {
            throw new DmsBusinessException("配送员ID不能为空");
        }
        LocalDateTime start = parseTime(startTime, true);
        LocalDateTime end = parseTime(endTime, false);
        LambdaQueryWrapper<DmsTracking> wrapper = new LambdaQueryWrapper<DmsTracking>()
                .eq(DmsTracking::getRiderId, riderId)
                .ge(start != null, DmsTracking::getReportTime, start)
                .le(end != null, DmsTracking::getReportTime, end)
                .orderByAsc(DmsTracking::getReportTime);
        return toVOList(decimate(trackingMapper.selectList(wrapper), maxPoints), true);
    }

    /** 按任务取轨迹（含抽稀） */
    public List<TrackingVO> trackByTaskVO(Long taskId, Integer maxPoints) {
        if (taskId == null) {
            throw new DmsBusinessException("任务ID不能为空");
        }
        List<DmsTracking> rows = trackingMapper.selectList(new LambdaQueryWrapper<DmsTracking>()
                .eq(DmsTracking::getTaskId, taskId)
                .orderByAsc(DmsTracking::getReportTime));
        return toVOList(decimate(rows, maxPoints), true);
    }

    /** 抽稀：超过 maxPoints 时按等间隔取样，首尾点必留（§3.5.2） */
    private List<DmsTracking> decimate(List<DmsTracking> rows, Integer maxPoints) {
        int limit = maxPoints == null || maxPoints < 2 ? DEFAULT_MAX_POINTS : maxPoints;
        if (rows == null || rows.size() <= limit) {
            return rows == null ? List.of() : rows;
        }
        int step = (int) Math.ceil((double) rows.size() / limit);
        List<DmsTracking> sampled = new ArrayList<>();
        for (int i = 0; i < rows.size(); i += step) {
            sampled.add(rows.get(i));
        }
        DmsTracking last = rows.get(rows.size() - 1);
        if (!sampled.get(sampled.size() - 1).getId().equals(last.getId())) {
            sampled.add(last);
        }
        log.debug("轨迹抽稀: {} → {} 点（step={}）", rows.size(), sampled.size(), step);
        return sampled;
    }

    // ==================== 里程聚合 ====================

    /**
     * 里程聚合（§3.4 `/mileage`）
     *
     * @param query   过滤条件（复用台账条件）
     * @param groupBy 聚合维度：rider（配送员，默认）/ task（任务）/ day（日）
     */
    public List<TrackingMileageVO> mileage(TrackingQueryDTO query, String groupBy) {
        TrackingQueryDTO q = query != null ? query : new TrackingQueryDTO();
        String dim = StringUtils.hasText(groupBy) ? groupBy.trim().toLowerCase() : "rider";
        if (!List.of("rider", "task", "day").contains(dim)) {
            throw new DmsBusinessException("不支持的聚合维度: " + groupBy + "（可选 rider / task / day）");
        }
        List<DmsTracking> rows = trackingMapper.selectList(buildWrapper(q).last("LIMIT " + MILEAGE_SCAN_LIMIT));
        if (rows.isEmpty()) {
            return List.of();
        }
        rows.sort(Comparator.comparing(DmsTracking::getReportTime, Comparator.nullsLast(Comparator.naturalOrder())));
        Map<String, List<DmsTracking>> grouped = rows.stream()
                .filter(r -> groupKey(r, dim) != null)
                .collect(Collectors.groupingBy(r -> groupKey(r, dim), LinkedHashMap::new, Collectors.toList()));

        List<TrackingMileageVO> result = new ArrayList<>();
        for (Map.Entry<String, List<DmsTracking>> e : grouped.entrySet()) {
            List<DmsTracking> points = e.getValue();
            points.sort(Comparator.comparing(DmsTracking::getReportTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            BigDecimal meters = BigDecimal.ZERO;
            BigDecimal speedSum = BigDecimal.ZERO;
            int speedCount = 0;
            for (int i = 0; i < points.size(); i++) {
                DmsTracking p = points.get(i);
                if (p.getSpeed() != null) {
                    speedSum = speedSum.add(p.getSpeed());
                    speedCount++;
                }
                if (i > 0) {
                    DmsTracking prev = points.get(i - 1);
                    if (prev.getLat() != null && prev.getLng() != null && p.getLat() != null && p.getLng() != null) {
                        meters = meters.add(BigDecimal.valueOf(haversine(prev.getLat(), prev.getLng(), p.getLat(), p.getLng())));
                    }
                }
            }
            TrackingMileageVO vo = new TrackingMileageVO();
            vo.setGroupKey(e.getKey());
            vo.setGroupName(groupName(e.getKey(), dim, points));
            vo.setPointCount(points.size());
            vo.setMileageMeters(meters.setScale(2, RoundingMode.HALF_UP));
            vo.setMileageKm(meters.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
            vo.setAvgSpeed(speedCount == 0 ? null
                    : speedSum.divide(BigDecimal.valueOf(speedCount), 2, RoundingMode.HALF_UP));
            vo.setFirstTime(points.get(0).getReportTime());
            vo.setLastTime(points.get(points.size() - 1).getReportTime());
            result.add(vo);
        }
        result.sort(Comparator.comparing(TrackingMileageVO::getMileageKm, Comparator.reverseOrder()));
        return result;
    }

    private String groupKey(DmsTracking row, String dim) {
        return switch (dim) {
            case "task" -> row.getTaskId() == null ? null : String.valueOf(row.getTaskId());
            case "day" -> row.getReportTime() == null ? null : row.getReportTime().toLocalDate().toString();
            default -> row.getRiderId() == null ? null : String.valueOf(row.getRiderId());
        };
    }

    private String groupName(String key, String dim, List<DmsTracking> points) {
        if ("day".equals(dim)) {
            return key;
        }
        if ("task".equals(dim)) {
            DmsTask task = taskMapper.selectById(Long.valueOf(key));
            return task == null ? key : task.getTaskNo();
        }
        DmsRider rider = riderMapper.selectById(Long.valueOf(key));
        return rider == null ? key : rider.getRealName();
    }

    // ==================== 上报 / 既有查询 ====================

    /**
     * 上报位置（兼容旧表单参数；新增 accuracy / address / source 可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportLocation(Long riderId, Long taskId, BigDecimal lat, BigDecimal lng,
                              BigDecimal speed, BigDecimal direction) {
        reportLocation(riderId, taskId, lat, lng, speed, direction, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reportLocation(Long riderId, Long taskId, BigDecimal lat, BigDecimal lng,
                              BigDecimal speed, BigDecimal direction,
                              BigDecimal accuracy, String address, Integer source) {
        if (riderId == null) {
            throw new DmsBusinessException("配送员ID不能为空");
        }
        if (lat == null || lng == null) {
            throw new DmsBusinessException("经纬度不能为空");
        }
        if (accuracy != null && accuracy.signum() < 0) {
            throw new DmsBusinessException("定位精度不能为负数");
        }
        assertWithinCollectWindow();
        // §3.5.5 异常停留判定需要「上一点」，必须在插入本点之前取
        DmsTracking previous = trackingMapper.selectOne(new LambdaQueryWrapper<DmsTracking>()
                .eq(DmsTracking::getRiderId, riderId)
                .orderByDesc(DmsTracking::getReportTime)
                .last("LIMIT 1"));

        DmsTracking record = new DmsTracking();
        record.setRiderId(riderId);
        record.setTaskId(taskId);
        record.setLat(lat);
        record.setLng(lng);
        record.setSpeed(speed);
        record.setDirection(direction);
        record.setAccuracy(accuracy);
        record.setAddress(StringUtils.hasText(address) ? address.trim() : null);
        record.setSource(source != null ? source : TrackingSourceEnum.APP.getValue());
        record.setReportTime(LocalDateTime.now());
        trackingMapper.insert(record);

        // 更新配送员当前位置信息
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider != null) {
            rider.setCurrentLat(lat);
            rider.setCurrentLng(lng);
            rider.setLastReportTime(LocalDateTime.now());
            riderMapper.updateById(rider);
        }

        raiseAbnormalAlerts(rider, taskId, lat, lng, speed, previous, record.getReportTime());
    }

    /**
     * §3.5.5 异常联动：超速 / 异常停留推送《人员核验》预警体系（复用 {@code dms_verification_alert}，口径统一）。
     *
     * <p>去重由 {@code VerificationService.raiseAlert} 负责（同配送员同类型仍有未处理预警时不重复产生）；
     * 预警链路异常只记日志，绝不阻断位置上报主链路。</p>
     */
    private void raiseAbnormalAlerts(DmsRider rider, Long taskId, BigDecimal lat, BigDecimal lng,
                                     BigDecimal speed, DmsTracking previous, LocalDateTime reportTime) {
        if (rider == null) {
            return;
        }
        try {
            Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
            VerificationService verificationService = verificationServiceProvider.getObject();

            int speedLimit = cfgInt(CFG_SPEED_LIMIT, DEFAULT_SPEED_LIMIT);
            if (speed != null && speed.compareTo(BigDecimal.valueOf(speedLimit)) > 0) {
                verificationService.raiseAlert(tenantId, rider.getId(), rider.getRealName(), taskId, lat, lng,
                        AlertTypeEnum.SPEED_ANOMALY.getValue(), 2,
                        "超速 " + speed.stripTrailingZeros().toPlainString() + " km/h（限 " + speedLimit + "）");
            }

            int stopMinutes = cfgInt(CFG_STOP_MINUTES, DEFAULT_STOP_MINUTES);
            if (previous != null && previous.getReportTime() != null && reportTime != null
                    && previous.getLat() != null && previous.getLng() != null && lat != null && lng != null) {
                long minutes = java.time.Duration.between(previous.getReportTime(), reportTime).toMinutes();
                double moved = haversine(previous.getLat(), previous.getLng(), lat, lng);
                if (minutes >= stopMinutes && moved < STAY_MOVE_METERS) {
                    verificationService.raiseAlert(tenantId, rider.getId(), rider.getRealName(), taskId, lat, lng,
                            AlertTypeEnum.ABNORMAL_STAY.getValue(), 2,
                            "停留 " + minutes + " 分钟未移动（位移 " + Math.round(moved) + " 米）");
                }
            }
        } catch (Exception e) {
            log.warn("轨迹异常联动预警失败（不影响位置上报）: riderId={}", rider.getId(), e);
        }
    }

    /** 某配送员最新位置（兼容旧接口） */
    public DmsTracking getLatestLocation(Long riderId) {
        return trackingMapper.selectOne(new LambdaQueryWrapper<DmsTracking>()
                .eq(DmsTracking::getRiderId, riderId)
                .orderByDesc(DmsTracking::getReportTime)
                .last("LIMIT 1"));
    }

    /** 配送员轨迹（兼容旧接口，按时间升序） */
    public List<DmsTracking> getTrack(Long riderId, LocalDateTime startTime, LocalDateTime endTime) {
        return trackingMapper.selectList(new LambdaQueryWrapper<DmsTracking>()
                .eq(DmsTracking::getRiderId, riderId)
                .ge(startTime != null, DmsTracking::getReportTime, startTime)
                .le(endTime != null, DmsTracking::getReportTime, endTime)
                .orderByAsc(DmsTracking::getReportTime));
    }

    /** 任务轨迹（兼容旧接口，按时间升序） */
    public List<DmsTracking> getTrackByTask(Long taskId) {
        return trackingMapper.selectList(new LambdaQueryWrapper<DmsTracking>()
                .eq(DmsTracking::getTaskId, taskId)
                .orderByAsc(DmsTracking::getReportTime));
    }

    /** 清理指定天数前的过期数据（§3.5.1 归档策略的执行入口） */
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredData(int retentionDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(retentionDays);
        return trackingMapper.delete(new LambdaQueryWrapper<DmsTracking>()
                .lt(DmsTracking::getCreateTime, deadline));
    }

    /** 合规：读取配置的保留天数（0/缺失 = 不做自动清理） */
    public int retentionDays() {
        return cfgInt(CFG_RETENTION_DAYS, 0);
    }

    /** 合规：按配置的保留天数清理历史轨迹（定时任务入口）；返回清理条数 */
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredByRetention() {
        int days = retentionDays();
        if (days <= 0) {
            log.debug("[实时跟踪] 未配置轨迹保留天数（{}），跳过清理", CFG_RETENTION_DAYS);
            return 0;
        }
        int deleted = cleanExpiredData(days);
        if (deleted > 0) {
            log.info("[实时跟踪] 按保留策略清理轨迹点 {} 条（保留 {} 天）", deleted, days);
        }
        return deleted;
    }

    /**
     * 合规：位置采集时段校验（§4 隐私与合规）
     *
     * <p>配置 {@code dms.tracking.collect.hours}（`HH:mm-HH:mm`，支持跨天）。缺失或格式非法时
     * **按全天放行**，不因配置问题阻断业务上报。</p>
     */
    private void assertWithinCollectWindow() {
        String hours = cfgStr(CFG_COLLECT_HOURS);
        if (!StringUtils.hasText(hours)) {
            return;
        }
        String[] parts = hours.trim().split("-");
        if (parts.length != 2) {
            log.warn("[实时跟踪] 采集时段配置格式非法（应为 HH:mm-HH:mm），按全天放行: {}", hours);
            return;
        }
        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            LocalTime now = LocalTime.now();
            boolean inWindow = start.equals(end)
                    || (start.isBefore(end)
                    ? (!now.isBefore(start) && !now.isAfter(end))
                    : (!now.isBefore(start) || !now.isAfter(end)));
            if (!inWindow) {
                throw new DmsBusinessException("当前不在位置采集时段（" + hours + "）内，已拒绝上报");
            }
        } catch (DmsBusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[实时跟踪] 采集时段配置解析失败，按全天放行: {}", hours);
        }
    }

    // ==================== 导出 ====================

    public void export(TrackingQueryDTO query, HttpServletResponse response) throws IOException {
        List<TrackingVO> rows = list(query);
        String fileName = "轨迹台账_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"上报时间", "配送员", "配送员电话", "任务编号", "客户", "经度", "纬度",
                "速度(km/h)", "方向", "来源", "定位精度(米)", "地址"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("轨迹台账");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (TrackingVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        timeText(vo.getReportTime()), nullSafe(vo.getRiderName()), nullSafe(vo.getRiderPhone()),
                        nullSafe(vo.getTaskNo()), nullSafe(vo.getCustomerName()),
                        num(vo.getLng()), num(vo.getLat()), num(vo.getSpeed()),
                        nullSafe(vo.getDirectionText()), nullSafe(vo.getSourceText()),
                        num(vo.getAccuracy()), nullSafe(vo.getAddress()),
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 16 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    // ==================== 私有辅助 ====================

    private List<TrackingVO> toVOList(List<DmsTracking> rows, boolean withSegment) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> riderIds = rows.stream().map(DmsTracking::getRiderId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DmsRider> riderMap = riderIds.isEmpty() ? Map.of()
                : riderMapper.selectBatchIds(riderIds).stream()
                .collect(Collectors.toMap(DmsRider::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
        Set<Long> taskIds = rows.stream().map(DmsTracking::getTaskId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DmsTask> taskMap = taskIds.isEmpty() ? Map.of()
                : taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(DmsTask::getId, t -> t, (a, b) -> a, LinkedHashMap::new));

        List<TrackingVO> list = rows.stream().map(row -> {
            DmsRider rider = row.getRiderId() == null ? null : riderMap.get(row.getRiderId());
            DmsTask task = row.getTaskId() == null ? null : taskMap.get(row.getTaskId());
            return toVO(row, rider, task);
        }).collect(Collectors.toList());

        if (withSegment) {
            fillSegment(list);
        }
        return list;
    }

    private TrackingVO toVO(DmsTracking row, DmsRider rider, DmsTask task) {
        TrackingVO vo = new TrackingVO();
        vo.setId(row.getId());
        vo.setRiderId(row.getRiderId());
        vo.setRiderName(rider == null ? null : rider.getRealName());
        vo.setRiderPhone(rider == null ? null : rider.getPhone());
        vo.setTaskId(row.getTaskId());
        vo.setTaskNo(task == null ? null : task.getTaskNo());
        vo.setCustomerName(task == null ? null : task.getCustomerName());
        vo.setLat(row.getLat());
        vo.setLng(row.getLng());
        vo.setSpeed(row.getSpeed());
        vo.setDirection(row.getDirection());
        vo.setDirectionText(directionText(row.getDirection()));
        vo.setSource(row.getSource());
        vo.setSourceText(TrackingSourceEnum.textOf(row.getSource()));
        vo.setAccuracy(row.getAccuracy());
        vo.setAddress(row.getAddress());
        vo.setReportTime(row.getReportTime());
        vo.setSpeedAbnormal(row.getSpeed() != null && row.getSpeed().compareTo(SPEED_ABNORMAL) > 0);
        return vo;
    }

    /**
     * 段间里程（米）：同配送员相邻两点的 Haversine 距离。
     *
     * <p>说明：明细台账按页返回，因此「上一条」以**当前返回集合内**同一配送员的时间前驱为准
     * （每组首条为 null）。跨页连续里程请用 `/mileage` 聚合接口，避免误导。</p>
     */
    private void fillSegment(List<TrackingVO> list) {
        Map<Long, TrackingVO> lastByRider = new LinkedHashMap<>();
        List<TrackingVO> ordered = new ArrayList<>(list);
        ordered.sort(Comparator.comparing(TrackingVO::getReportTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (TrackingVO vo : ordered) {
            TrackingVO prev = lastByRider.get(vo.getRiderId());
            if (prev != null && prev.getLat() != null && prev.getLng() != null
                    && vo.getLat() != null && vo.getLng() != null) {
                vo.setSegmentMeters(BigDecimal.valueOf(
                        haversine(prev.getLat(), prev.getLng(), vo.getLat(), vo.getLng())).setScale(2, RoundingMode.HALF_UP));
            }
            lastByRider.put(vo.getRiderId(), vo);
        }
    }

    /** 方向角度 → 八方位文案 */
    private String directionText(BigDecimal direction) {
        if (direction == null) {
            return null;
        }
        double deg = direction.doubleValue() % 360;
        if (deg < 0) {
            deg += 360;
        }
        String[] names = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        return names[(int) Math.round(deg / 45) % 8];
    }

    /** 时间解析：支持 `yyyy-MM-dd`（起始补 00:00:00，结束补 23:59:59）与 `yyyy-MM-dd HH:mm:ss` */
    private LocalDateTime parseTime(String text, boolean startOfDay) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() <= 10) {
                LocalDate date = LocalDate.parse(value);
                return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            }
            return LocalDateTime.parse(value, DATE_TIME_FMT);
        } catch (Exception e) {
            throw new DmsBusinessException("时间格式不正确: " + text + "（支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）");
        }
    }

    /** Haversine 距离（米） */
    private double haversine(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double dLat = radLat2 - radLat1;
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String timeText(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String num(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // ==================== 实时跟踪（配送员位置聚合 / 统计 / 预警） ====================

    /**
     * 配送员实时位置分页（§3.5 `/tracking/rider-page`）
     *
     * <p>替代改造前的 N+1：一次分页返回 配送员 + 最新位置 + 在线态 + 今日里程/点数 + 今日完成单量 + 在途负载。</p>
     */
    public Page<RiderLocationVO> riderPage(RiderLocationQuery q) {
        RiderLocationQuery query = q != null ? q : new RiderLocationQuery();
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        int onlineMinutes = cfgInt(CFG_ONLINE_MINUTES, DEFAULT_ONLINE_MINUTES);

        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(DmsRider::getRealName, kw).or().like(DmsRider::getPhone, kw));
        }
        wrapper.eq(query.getStatus() != null, DmsRider::getStatus, query.getStatus());
        if (Boolean.TRUE.equals(query.getOnlineOnly())) {
            wrapper.ge(DmsRider::getLastReportTime, LocalDateTime.now().minusMinutes(onlineMinutes));
        }
        wrapper.orderByDesc(DmsRider::getLastReportTime).orderByAsc(DmsRider::getId);

        Page<DmsRider> riderPage = riderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<RiderLocationVO> result = new Page<>(pageNum, pageSize, riderPage.getTotal());
        result.setRecords(buildRiderLocations(riderPage.getRecords(), onlineMinutes));
        return result;
    }

    private List<RiderLocationVO> buildRiderLocations(List<DmsRider> riders, int onlineMinutes) {
        if (riders == null || riders.isEmpty()) {
            return List.of();
        }
        Set<Long> riderIds = riders.stream().map(DmsRider::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime lookback = LocalDateTime.now().minusDays(LOCATION_LOOKBACK_DAYS);

        // 一次取这批配送员回溯窗口内的点（走 V11.215.0 的 tenant+rider+report_time 索引），内存里取最新 + 今日累计
        List<DmsTracking> points = trackingMapper.selectList(new LambdaQueryWrapper<DmsTracking>()
                .in(DmsTracking::getRiderId, riderIds)
                .ge(DmsTracking::getReportTime, lookback)
                .orderByAsc(DmsTracking::getRiderId)
                .orderByAsc(DmsTracking::getReportTime));
        Map<Long, List<DmsTracking>> byRider = points.stream()
                .collect(Collectors.groupingBy(DmsTracking::getRiderId, LinkedHashMap::new, Collectors.toList()));

        // 今日完成单量 / 在途负载：各一次批量查询
        Map<Long, Long> doneToday = countTasksByRider(riderIds, true);
        Map<Long, Long> activeTasks = countTasksByRider(riderIds, false);
        // 车辆快照：取这些配送员最近任务上的车牌
        Map<Long, String> vehicleByRider = new LinkedHashMap<>();
        for (DmsTask task : taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getRiderId, riderIds)
                .isNotNull(DmsTask::getVehicleName)
                .orderByDesc(DmsTask::getId))) {
            vehicleByRider.putIfAbsent(task.getRiderId(), task.getVehicleName());
        }

        List<RiderLocationVO> list = new ArrayList<>();
        for (DmsRider rider : riders) {
            RiderLocationVO vo = new RiderLocationVO();
            vo.setRiderId(rider.getId());
            vo.setRiderNo(rider.getRiderNo());
            vo.setRiderName(rider.getRealName());
            vo.setRiderPhone(rider.getPhone());
            vo.setRiderTypeText(RiderTypeEnum.fromValue(rider.getRiderType() == null ? 1 : rider.getRiderType())
                    .getDescription());
            vo.setStatus(rider.getStatus());
            vo.setStatusText(riderStatusText(rider.getStatus()));
            vo.setOnlineMinutes(onlineMinutes);
            LocalDateTime last = rider.getLastReportTime();
            vo.setLastReportTime(last);
            vo.setOnline(last != null && last.isAfter(LocalDateTime.now().minusMinutes(onlineMinutes)));
            vo.setLastReportAgoSeconds(last == null ? null : java.time.Duration.between(last, LocalDateTime.now()).getSeconds());
            vo.setTodayDoneTasks(doneToday.getOrDefault(rider.getId(), 0L).intValue());
            vo.setActiveTasks(activeTasks.getOrDefault(rider.getId(), 0L).intValue());
            vo.setVehicleName(vehicleByRider.get(rider.getId()));

            List<DmsTracking> rows = byRider.getOrDefault(rider.getId(), List.of());
            if (!rows.isEmpty()) {
                DmsTracking latest = rows.get(rows.size() - 1);
                vo.setLat(latest.getLat());
                vo.setLng(latest.getLng());
                vo.setSpeed(latest.getSpeed());
                vo.setDirection(latest.getDirection());
                vo.setDirectionText(directionText(latest.getDirection()));
                vo.setAccuracy(latest.getAccuracy());
                vo.setAddress(latest.getAddress());
                if (vo.getLastReportTime() == null) {
                    vo.setLastReportTime(latest.getReportTime());
                }
                BigDecimal meters = BigDecimal.ZERO;
                int todayCount = 0;
                List<DmsTracking> todayRows = rows.stream()
                        .filter(r -> r.getReportTime() != null && !r.getReportTime().isBefore(todayStart))
                        .toList();
                for (int i = 0; i < todayRows.size(); i++) {
                    todayCount++;
                    if (i > 0) {
                        DmsTracking prev = todayRows.get(i - 1);
                        DmsTracking cur = todayRows.get(i);
                        if (prev.getLat() != null && prev.getLng() != null && cur.getLat() != null && cur.getLng() != null) {
                            meters = meters.add(BigDecimal.valueOf(
                                    haversine(prev.getLat(), prev.getLng(), cur.getLat(), cur.getLng())));
                        }
                    }
                }
                vo.setTodayMileageKm(meters.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
                vo.setTodayPointCount(todayCount);
            } else {
                vo.setTodayMileageKm(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                vo.setTodayPointCount(0);
            }
            list.add(vo);
        }
        return list;
    }

    /** 按配送员统计「今日已完成」或「在途」任务数 */
    private Map<Long, Long> countTasksByRider(Set<Long> riderIds, boolean doneToday) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getRiderId, riderIds)
                .select(DmsTask::getRiderId, DmsTask::getId);
        if (doneToday) {
            wrapper.eq(DmsTask::getStatus, TaskStatusEnum.COMPLETED.getValue())
                    .ge(DmsTask::getCompletedTime, LocalDate.now().atStartOfDay());
        } else {
            wrapper.in(DmsTask::getStatus, IN_TRANSIT_STATUS);
        }
        Map<Long, Long> map = new LinkedHashMap<>();
        for (DmsTask t : taskMapper.selectList(wrapper)) {
            map.merge(t.getRiderId(), 1L, Long::sum);
        }
        return map;
    }

    /** 实时跟踪统计卡（§3.5 `/tracking/stat`，后端聚合） */
    public TrackingStatVO stat() {
        int onlineMinutes = cfgInt(CFG_ONLINE_MINUTES, DEFAULT_ONLINE_MINUTES);
        LocalDateTime onlineSince = LocalDateTime.now().minusMinutes(onlineMinutes);
        TrackingStatVO vo = new TrackingStatVO();
        vo.setOnlineMinutes(onlineMinutes);

        long riderTotal = riderMapper.selectCount(new LambdaQueryWrapper<>());
        vo.setRiderTotal(riderTotal);
        long online = riderMapper.selectCount(new LambdaQueryWrapper<DmsRider>()
                .ge(DmsRider::getLastReportTime, onlineSince));
        vo.setOnlineCount(online);
        vo.setOfflineCount(Math.max(riderTotal - online, 0));

        Set<Long> withLocation = trackingMapper.selectList(new LambdaQueryWrapper<DmsTracking>()
                        .select(DmsTracking::getRiderId)).stream()
                .map(DmsTracking::getRiderId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long ridersWithLoc = withLocation.size();
        vo.setWithLocationCount(ridersWithLoc);
        vo.setNoLocationCount(Math.max(riderTotal - ridersWithLoc, 0));

        vo.setActiveTaskCount(taskMapper.selectCount(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getStatus, IN_TRANSIT_STATUS)));
        vo.setOverdueTaskCount(taskMapper.selectCount(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getStatus, IN_TRANSIT_STATUS)
                .isNotNull(DmsTask::getDeadlineTime)
                .lt(DmsTask::getDeadlineTime, LocalDateTime.now())));
        vo.setTodayPointCount(trackingMapper.selectCount(new LambdaQueryWrapper<DmsTracking>()
                .ge(DmsTracking::getReportTime, LocalDate.now().atStartOfDay())));
        vo.setAlertCount(alerts(null).size());
        return vo;
    }

    /**
     * 异常预警（§4）：超速 / 异常停留 / 超时在途
     *
     * @param q 可选过滤（复用台账条件；为空则统计今日全量）
     */
    public List<TrackingAlertVO> alerts(TrackingQueryDTO q) {
        TrackingQueryDTO query = q != null ? q : new TrackingQueryDTO();
        if (!StringUtils.hasText(query.getStartTime())) {
            query.setStartTime(LocalDate.now().toString());
        }
        if (!StringUtils.hasText(query.getEndTime())) {
            query.setEndTime(LocalDate.now().toString());
        }
        int speedLimit = cfgInt(CFG_SPEED_LIMIT, DEFAULT_SPEED_LIMIT);
        int stopMinutes = cfgInt(CFG_STOP_MINUTES, DEFAULT_STOP_MINUTES);

        List<DmsTracking> rows = trackingMapper.selectList(buildWrapper(query).last("LIMIT " + MILEAGE_SCAN_LIMIT));
        rows.sort(Comparator.comparing(DmsTracking::getReportTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Map<Long, DmsRider> riderMap = loadRiderMap(rows.stream().map(DmsTracking::getRiderId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, DmsTask> taskMap = loadTaskMap(rows.stream().map(DmsTracking::getTaskId)
                .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)));

        List<TrackingAlertVO> alerts = new ArrayList<>();
        Map<Long, DmsTracking> prevByRider = new LinkedHashMap<>();
        for (DmsTracking row : rows) {
            if (row.getSpeed() != null && row.getSpeed().compareTo(BigDecimal.valueOf(speedLimit)) > 0) {
                alerts.add(buildAlert("OVERSPEED", "超速", "DANGER", row, riderMap, taskMap,
                        row.getSpeed(), "超速 " + row.getSpeed().stripTrailingZeros().toPlainString()
                                + " km/h（限 " + speedLimit + "）"));
            }
            DmsTracking prev = prevByRider.get(row.getRiderId());
            if (prev != null && prev.getReportTime() != null && row.getReportTime() != null
                    && prev.getLat() != null && prev.getLng() != null && row.getLat() != null && row.getLng() != null) {
                long minutes = java.time.Duration.between(prev.getReportTime(), row.getReportTime()).toMinutes();
                double moved = haversine(prev.getLat(), prev.getLng(), row.getLat(), row.getLng());
                if (minutes >= stopMinutes && moved < STAY_MOVE_METERS) {
                    alerts.add(buildAlert("STAY", "异常停留", "WARN", row, riderMap, taskMap,
                            BigDecimal.valueOf(minutes), "停留 " + minutes + " 分钟未移动（位移 "
                                    + Math.round(moved) + " 米）"));
                }
            }
            prevByRider.put(row.getRiderId(), row);
        }

        // 超时在途 / 偏航（与轨迹点无关，按在途任务表判定）
        List<DmsTask> transitTasks = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getStatus, IN_TRANSIT_STATUS)
                .last("LIMIT 200"));
        int deviationMeters = cfgInt(CFG_DEVIATION_METERS, DEFAULT_DEVIATION_METERS);
        Map<Long, DmsTracking> latestByRider = latestPointsByRider(transitTasks.stream()
                .map(DmsTask::getRiderId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        LocalDateTime now = LocalDateTime.now();
        for (DmsTask task : transitTasks) {
            if (task.getDeadlineTime() != null && task.getDeadlineTime().isBefore(now)) {
                TrackingAlertVO vo = new TrackingAlertVO();
                vo.setAlertType("OVERDUE");
                vo.setAlertTypeText("超时在途");
                vo.setLevel("DANGER");
                vo.setRiderId(task.getRiderId());
                vo.setRiderName(task.getRiderName());
                vo.setTaskId(task.getId());
                vo.setTaskNo(task.getTaskNo());
                vo.setLat(task.getCustomerLat());
                vo.setLng(task.getCustomerLng());
                vo.setEventTime(task.getDeadlineTime());
                long overdue = java.time.Duration.between(task.getDeadlineTime(), now).toMinutes();
                vo.setValue(BigDecimal.valueOf(overdue));
                vo.setAlertText("任务已超时 " + overdue + " 分钟（状态：" + taskStatusText(task.getStatus()) + "）");
                alerts.add(vo);
            }

            // 偏航：当前最新位置偏离「起点→客户」配送基线超过阈值（接入路线单规划点序列后可升级为折线口径）
            DmsTracking pos = task.getRiderId() == null ? null : latestByRider.get(task.getRiderId());
            if (pos != null && pos.getLat() != null && pos.getLng() != null
                    && task.getSourceLat() != null && task.getSourceLng() != null
                    && task.getCustomerLat() != null && task.getCustomerLng() != null) {
                double off = distanceToSegmentMeters(pos.getLat(), pos.getLng(),
                        task.getSourceLat(), task.getSourceLng(),
                        task.getCustomerLat(), task.getCustomerLng());
                if (off > deviationMeters) {
                    TrackingAlertVO vo = new TrackingAlertVO();
                    vo.setAlertType("DEVIATION");
                    vo.setAlertTypeText("偏航");
                    vo.setLevel("WARN");
                    vo.setRiderId(task.getRiderId());
                    vo.setRiderName(task.getRiderName());
                    vo.setTaskId(task.getId());
                    vo.setTaskNo(task.getTaskNo());
                    vo.setLat(pos.getLat());
                    vo.setLng(pos.getLng());
                    vo.setEventTime(pos.getReportTime());
                    vo.setValue(BigDecimal.valueOf(Math.round(off)));
                    vo.setAlertText("偏离配送基线 " + Math.round(off) + " 米（限 " + deviationMeters + " 米）");
                    alerts.add(vo);
                }
            }
        }

        alerts.sort(Comparator.comparing(TrackingAlertVO::getEventTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return alerts.size() > ALERT_LIMIT ? new ArrayList<>(alerts.subList(0, ALERT_LIMIT)) : alerts;
    }

    private TrackingAlertVO buildAlert(String type, String typeText, String level, DmsTracking row,
                                       Map<Long, DmsRider> riderMap, Map<Long, DmsTask> taskMap,
                                       BigDecimal value, String text) {
        TrackingAlertVO vo = new TrackingAlertVO();
        vo.setAlertType(type);
        vo.setAlertTypeText(typeText);
        vo.setLevel(level);
        vo.setRiderId(row.getRiderId());
        DmsRider rider = row.getRiderId() == null ? null : riderMap.get(row.getRiderId());
        vo.setRiderName(rider == null ? null : rider.getRealName());
        vo.setTaskId(row.getTaskId());
        DmsTask task = row.getTaskId() == null ? null : taskMap.get(row.getTaskId());
        vo.setTaskNo(task == null ? null : task.getTaskNo());
        vo.setLat(row.getLat());
        vo.setLng(row.getLng());
        vo.setEventTime(row.getReportTime());
        vo.setValue(value);
        vo.setAlertText(text);
        return vo;
    }

    private Map<Long, DmsRider> loadRiderMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return riderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DmsRider::getId, r -> r, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, DmsTask> loadTaskMap(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return taskMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DmsTask::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
    }

    private String taskStatusText(Integer status) {
        return TaskStatusEnum.fromValue(status == null ? 0 : status).getDescription();
    }

    private String riderStatusText(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "离线";
            case 1 -> "空闲";
            case 2 -> "忙碌";
            case 3 -> "休息";
            default -> "未知";
        };
    }

    /** 各配送员回溯窗口内的最新一个轨迹点（用于偏航判定；走 rider+report_time 索引） */
    private Map<Long, DmsTracking> latestPointsByRider(Set<Long> riderIds) {
        if (riderIds == null || riderIds.isEmpty()) {
            return Map.of();
        }
        List<DmsTracking> points = trackingMapper.selectList(new LambdaQueryWrapper<DmsTracking>()
                .in(DmsTracking::getRiderId, riderIds)
                .ge(DmsTracking::getReportTime, LocalDateTime.now().minusDays(LOCATION_LOOKBACK_DAYS))
                .orderByAsc(DmsTracking::getReportTime));
        Map<Long, DmsTracking> map = new LinkedHashMap<>();
        for (DmsTracking p : points) {
            map.put(p.getRiderId(), p);
        }
        return map;
    }

    /**
     * 点到线段（「起点 → 客户」配送基线）的最短距离（米）
     *
     * <p>小范围等距投影：经度按基线起点纬度收缩，纬度固定比例；对城市配送尺度误差可忽略。</p>
     */
    private double distanceToSegmentMeters(BigDecimal pLat, BigDecimal pLng,
                                           BigDecimal aLat, BigDecimal aLng,
                                           BigDecimal bLat, BigDecimal bLng) {
        double kx = 111320.0 * Math.cos(Math.toRadians(aLat.doubleValue()));
        double ky = 110540.0;
        double px = pLng.doubleValue() * kx;
        double py = pLat.doubleValue() * ky;
        double ax = aLng.doubleValue() * kx;
        double ay = aLat.doubleValue() * ky;
        double bx = bLng.doubleValue() * kx;
        double by = bLat.doubleValue() * ky;
        double dx = bx - ax;
        double dy = by - ay;
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    /** 读字符串配置（缺失/异常返回 null，不阻断主流程） */
    private String cfgStr(String key) {
        try {
            var cfg = configService.getConfig(MyBatisPlusConfig.getCurrentTenantIdValue(), key);
            return cfg == null ? null : cfg.getConfigValue();
        } catch (Exception e) {
            log.debug("配置未设置: {}", key);
            return null;
        }
    }

    /** 读整数配置（缺失/异常回落默认值，不阻断主流程） */
    private int cfgInt(String key, int defaultValue) {
        try {
            Integer value = configService.getInteger(MyBatisPlusConfig.getCurrentTenantIdValue(), key);
            return value != null && value > 0 ? value : defaultValue;
        } catch (Exception e) {
            log.debug("配置未设置，使用默认值: {}={}", key, defaultValue);
            return defaultValue;
        }
    }

    /** 供其它模块复用：批量取配送员姓名映射（与 RiderService.nameMap 同口径） */
    public Map<Long, String> riderNameMap(Set<Long> riderIds) {
        if (riderIds == null || riderIds.isEmpty()) {
            return Map.of();
        }
        return riderMapper.selectBatchIds(riderIds).stream()
                .collect(Collectors.toMap(DmsRider::getId, r -> r.getRealName() == null ? "" : r.getRealName(),
                        (a, b) -> a, LinkedHashMap::new));
    }
}
