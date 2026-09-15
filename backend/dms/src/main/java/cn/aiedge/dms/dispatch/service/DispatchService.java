package cn.aiedge.dms.dispatch.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.service.ChannelOrderService;
import cn.aiedge.dms.channel.service.ChannelService;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.enums.DispatchTypeEnum;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.common.util.GeoUtils;
import cn.aiedge.dms.config.service.ConfigService;
import cn.aiedge.dms.dispatch.dto.DispatchCandidateVO;
import cn.aiedge.dms.dispatch.dto.DispatchPreviewVO;
import cn.aiedge.dms.dispatch.dto.DispatchStatVO;
import cn.aiedge.dms.dispatch.dto.DispatchStrategyDTO;
import cn.aiedge.dms.event.service.EventService;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.route.entity.GeoFence;
import cn.aiedge.dms.route.service.GeoFenceService;
import cn.aiedge.dms.task.dto.BatchResultVO;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.entity.DmsTaskLog;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.task.service.TaskLogService;
import cn.aiedge.dms.task.service.TaskService;
import cn.aiedge.dms.tracking.service.TrackingService;
import cn.aiedge.dms.verification.service.KycService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
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
 * 智能调度服务
 *
 * 处理自动分配、手动指派、改派、候选骑手查询和围栏校验等调度逻辑。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final DmsTaskMapper taskMapper;
    private final DmsRiderMapper riderMapper;
    private final KycService kycService;
    private final GeoFenceService geoFenceService;
    private final ConfigService configService;
    private final EventService eventService;
    private final TaskLogService taskLogService;
    /** 区域分包（AREA）绑定：线路档案 × 配送员 */
    private final RouteRiderService routeRiderService;
    /** 运力渠道（任务指定 channelId 时走外部平台适配器下单，见《渠道管理开发文档》§7.1） */
    private final ChannelService channelService;
    private final ChannelOrderService channelOrderService;
    /** 任务归属变更后的运力释放（单一口径：名下无在途单才回置空闲） */
    private final TaskService taskService;

    // ==================== 派单策略配置键（配置中心 dms_config，见迁移 V11.218.0 / V11.321.0） ====================
    private static final String CFG_STRATEGY = "dms.dispatch.strategy";
    private static final String CFG_W_DISTANCE = "dms.dispatch.weight.distance";
    private static final String CFG_W_LOAD = "dms.dispatch.weight.load";
    private static final String CFG_W_SCORE = "dms.dispatch.weight.score";
    private static final String CFG_MAX_CONCURRENT = "dms.dispatch.max.concurrent";
    private static final String CFG_REQUIRE_ONLINE = "dms.dispatch.require.online";
    private static final String CFG_MAX_LOAD_KG = "dms.dispatch.max.load.kg";
    private static final String CFG_MAX_VOLUME_M3 = "dms.dispatch.max.volume.m3";
    private static final String CFG_ESCALATE_MINUTES = "dms.dispatch.timeout.escalate.minutes";
    private static final String CFG_AREA_STRICT = "dms.dispatch.area.strict";

    /** 在线判定阈值键（与《实时跟踪》《配送跟踪》《配送员管理》同一口径：位置上报心跳） */
    private static final String CFG_ONLINE_MINUTES = TrackingService.CFG_ONLINE_MINUTES;
    private static final int DEFAULT_ONLINE_MINUTES = 2;

    /** 派单策略：最近可用 / 负载均衡 / 评分优先 / 区域分包 */
    private static final List<String> STRATEGIES = List.of("NEAREST", "BALANCED", "SCORE", "AREA");

    /** 区域分包加权：命中线路绑定的配送员固定加此分，保证「绑定者优先」不被距离分反超 */
    private static final BigDecimal AREA_BOUND_BONUS = BigDecimal.valueOf(100_000);

    /** 距离不可判定时的哨兵值（米）：用于「缺定位/缺取货点坐标」场景 */
    private static final double UNKNOWN_DISTANCE_METERS = 999_999d;

    /** 在途任务状态（已分配/已接单/取货中/配送中） */
    private static final List<Integer> ACTIVE_STATUS = List.of(
            TaskStatusEnum.ASSIGNED.getValue(), TaskStatusEnum.ACCEPTED.getValue(),
            TaskStatusEnum.PICKING_UP.getValue(), TaskStatusEnum.DELIVERING.getValue());

    /** 围栏档案绑定的业务类型：线路档案（erp_route） */
    private static final String BIZ_TYPE_ROUTE = "ROUTE";

    /** 无绑定围栏时的兜底半径（米） */
    private static final double DEFAULT_FENCE_RADIUS_METERS = 5000d;

    /**
     * 手动指派骑手
     *
     * @param taskId  任务ID
     * @param riderId 骑手ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRider(Long taskId, Long riderId) {
        assignRider(taskId, riderId, null);
    }

    /**
     * 手动指派配送员（带原因，写调度审计）
     *
     * <p>《调度任务开发文档》§3.6 工程约束 1「指派前校验配送员负载上限」：在途并接数 / 载重 / 容积
     * 任一超限、配送员休息中、资质不满足 → 拒绝并给出可读原因。离线与无定位不做硬门控
     * （人工指派是调度员的显式决策，距离/围栏属自动派单的优化约束）。</p>
     *
     * @param reason 指派原因（可空，仅留痕）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRider(Long taskId, Long riderId, String reason) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (!Integer.valueOf(DmsConstants.TASK_PENDING).equals(task.getStatus())) {
            throw new DmsBusinessException("任务状态不允许分配，当前状态: "
                    + TaskStatusEnum.fromValue(task.getStatus()).getDescription());
        }
        assertNotChannelTask(task);

        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw new DmsBusinessException("配送员不存在: " + riderId);
        }
        assertAssignable(task, rider);

        assignTask(task, rider, DispatchTypeEnum.MANUAL.getValue());
        taskLogService.record(taskId, task.getTaskNo(), DmsTaskLog.ACTION_ASSIGN,
                null, riderId, reason);
        log.info("手动指派成功: taskId={}, riderId={}", taskId, riderId);
    }

    /**
     * 改派任务
     *
     * @param taskId      任务ID
     * @param fromRiderId 原骑手ID
     * @param toRiderId   新骑手ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long taskId, Long fromRiderId, Long toRiderId) {
        reassign(taskId, fromRiderId, toRiderId, null);
    }

    /**
     * 改派任务（带原因，写调度审计）
     *
     * <p>改派同样走负载上限校验（新配送员超限则拒绝），并释放原配送员占用。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long taskId, Long fromRiderId, Long toRiderId, String reason) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_ASSIGNED && task.getStatus() != DmsConstants.TASK_ACCEPTED) {
            throw new DmsBusinessException("任务状态不允许改派，当前状态: "
                    + TaskStatusEnum.fromValue(task.getStatus()).getDescription());
        }
        if (Objects.equals(fromRiderId, toRiderId)) {
            throw new DmsBusinessException("新配送员与原配送员相同，无需改派");
        }
        assertNotChannelTask(task);

        DmsRider toRider = riderMapper.selectById(toRiderId);
        if (toRider == null) {
            throw new DmsBusinessException("新配送员不存在: " + toRiderId);
        }
        // 原配送员先从负载中移除，避免「改派给同一人所在运力池」时的自阻塞
        assertAssignable(task, toRider);

        toRider.setStatus(DmsConstants.RIDER_STATUS_BUSY);
        riderMapper.updateById(toRider);

        // 保持任务为已分配状态，并刷新配送员快照
        task.setStatus(DmsConstants.TASK_ASSIGNED);
        task.setRiderId(toRiderId);
        task.setRiderName(toRider.getRealName());
        task.setDispatchType(DispatchTypeEnum.MANUAL.getValue());
        task.setDispatchTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 归属切换已完成：原配送员名下若无其它在途单则回置空闲（还有单据在途时保持忙碌）
        taskService.releaseRiderIfIdle(fromRiderId);

        taskLogService.record(taskId, task.getTaskNo(), DmsTaskLog.ACTION_REASSIGN,
                fromRiderId, toRiderId, reason);
        log.info("改派成功: taskId={}, fromRiderId={}, toRiderId={}", taskId, fromRiderId, toRiderId);
    }

    /**
     * 批量指派（《调度任务开发文档》§3.4 功能按钮：批量指派）
     *
     * <p>逐单执行、逐单反馈：状态不符 / 超限的单据只记失败原因，不阻塞其余单据。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO batchAssign(List<Long> taskIds, Long riderId, String reason) {
        BatchResultVO result = new BatchResultVO();
        if (taskIds == null || taskIds.isEmpty()) {
            return result;
        }
        result.setTotal(taskIds.size());
        if (riderId == null) {
            throw new DmsBusinessException("请选择要指派的配送员");
        }
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw new DmsBusinessException("配送员不存在: " + riderId);
        }
        for (Long taskId : taskIds) {
            DmsTask task = null;
            try {
                task = taskMapper.selectById(taskId);
                if (task == null) {
                    result.markFailed(taskId, null, "任务不存在或已删除");
                    continue;
                }
                assignRider(taskId, riderId, StringUtils.hasText(reason) ? reason : "批量指派");
                result.markSuccess();
            } catch (Exception e) {
                result.markFailed(taskId, task == null ? null : task.getTaskNo(), e.getMessage());
            }
        }
        log.info("批量指派完成: riderId={}, 成功 {}/{}", riderId, result.getSuccess(), result.getTotal());
        return result;
    }

    /**
     * 超时升级扫描（《调度任务开发文档》§3.6 工程约束 2：任务超时未接单/未完成 → 自动重新指派或升级告警）
     *
     * <p>口径：在途任务（已分配/已接单/取货中/配送中）的 {@code deadline_time} 已过、
     * 或「已分配」超过配置阈值 {@code dms.dispatch.timeout.escalate.minutes} 仍未接单 → 进入超时。
     * 处理：**已分配未接单**的任务重新走策略派单（写 ESCALATE 审计）；其余只记超时审计与告警日志，等待人工介入。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO escalateOverdue() {
        return escalateOverdue(false);
    }

    /**
     * 超时升级扫描（可演练）
     *
     * @param dryRun {@code true} 只统计与解释（**不落库**：不改任务归属、不写审计、不动配送员、不迭代已重派）
     *               —— 供定时任务「演练」与验收断言使用，避免在共享/生产环境误改数据
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchResultVO escalateOverdue(boolean dryRun) {
        DispatchStrategyDTO cfg = strategy();
        int thresholdMinutes = cfg.getTimeoutEscalateMinutes() == null ? 30 : cfg.getTimeoutEscalateMinutes();
        LocalDateTime now = LocalDateTime.now();
        List<DmsTask> active = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getStatus, ACTIVE_STATUS));

        // 在途配送员池：与人工指派同口径（负载上限校验在 assertAssignable 内）
        List<DmsRider> pool = riderMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Integer> activeByRider = activeTaskCounts(pool.stream().map(DmsRider::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        AreaCtx area = loadAreaCtx(cfg, active);

        BatchResultVO result = new BatchResultVO();
        for (DmsTask task : active) {
            if (!isTimedOut(task, now, thresholdMinutes)) {
                continue;
            }
            result.setTotal(result.getTotal() + 1);
            // 外部渠道任务：指派权在平台侧（本系统只做台账与回写），不在本系统内换人重派
            ChannelPlan escalatedPlan = channelPlan(task);
            if (escalatedPlan.applicable()) {
                if (!dryRun) {
                    taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_ESCALATE,
                            task.getRiderId(), task.getRiderId(),
                            "外部渠道任务（" + escalatedPlan.channel().getChannelName() + "）超时，等待平台侧处理或人工介入");
                }
                result.markFailed(task.getId(), task.getTaskNo(),
                        "外部渠道任务超时（" + escalatedPlan.channel().getChannelName() + "，等待平台侧处理）");
                continue;
            }
            boolean pendingAccept = Integer.valueOf(DmsConstants.TASK_ASSIGNED).equals(task.getStatus())
                    && (task.getDispatchTime() == null
                        || task.getDispatchTime().plusMinutes(thresholdMinutes).isBefore(now));
            if (!pendingAccept) {
                if (!dryRun) {
                    taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_ESCALATE,
                            task.getRiderId(), task.getRiderId(),
                            "任务已超时（超过要求送达时间），已告警待人工介入");
                }
                result.markFailed(task.getId(), task.getTaskNo(), "已超时告警（配送中/取货中，需人工介入）");
                continue;
            }
            Long fromRiderId = task.getRiderId();
            // 优先换人：超时未接单说明原配送员已不合适；若确无他人可派，退回原配送员重新派单（重新计时）
            List<DmsRider> altPool = pool.stream()
                    .filter(r -> !Objects.equals(r.getId(), fromRiderId))
                    .collect(Collectors.toList());
            DispatchCandidateVO best = pickBest(task, altPool, activeByRider, cfg, area);
            boolean sameRider = false;
            if (best == null) {
                best = pickBest(task, pool, activeByRider, cfg, area);
                sameRider = best != null;
            }
            if (best == null) {
                String reason = failReason(task, pool, activeByRider, cfg, area);
                if (!dryRun) {
                    taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_ESCALATE,
                            fromRiderId, null, "超时未接单且无人可派：" + reason);
                }
                result.markFailed(task.getId(), task.getTaskNo(), "超时未接单且无人可派：" + reason);
                continue;
            }
            try {
                DmsRider rider = riderMapper.selectById(best.getRiderId());
                if (rider == null) {
                    result.markFailed(task.getId(), task.getTaskNo(), "候选配送员已不存在");
                    continue;
                }
                // 先校验（负载/载重/容积/资质），通过后再改任务与配送员状态，避免半成品状态
                assertAssignable(task, rider);
                if (dryRun) {
                    // 演练：只统计「本可重派」，不改任务归属、不动配送员、不写审计
                    result.markSuccess();
                    continue;
                }
                assignTask(task, rider, DispatchTypeEnum.AUTO.getValue());
                // 归属切换后释放原配送员（重新派回原人时其在途含本单，不会误置空闲）
                taskService.releaseRiderIfIdle(fromRiderId);
                taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_ESCALATE,
                        fromRiderId, rider.getId(),
                        "超时未接单（阈值 " + thresholdMinutes + " 分钟），"
                                + (sameRider ? "无其他可用运力，重新派单给原配送员（重新计时）" : "自动重派"));
                activeByRider.merge(rider.getId(), 1, Integer::sum);
                result.markSuccess();
            } catch (Exception e) {
                result.markFailed(task.getId(), task.getTaskNo(), "超时重派失败: " + e.getMessage());
                log.warn("超时升级重派失败: taskId={}", task.getId(), e);
            }
        }
        if (dryRun) {
            result.setNote("演练模式：命中 " + result.getTotal() + " 单、可重派 " + result.getSuccess()
                    + " 单；未修改任务归属、未变更配送员状态、未写调度审计");
        }
        log.info("超时升级扫描完成: 命中 {} 单，重派成功 {}{}", result.getTotal(), result.getSuccess(),
                dryRun ? "（演练未落库）" : "");
        return result;
    }

    /** 超时口径：已过要求送达时间；或「已分配」超过阈值仍未接单 */
    private boolean isTimedOut(DmsTask task, LocalDateTime now, int thresholdMinutes) {
        if (task.getDeadlineTime() != null && task.getDeadlineTime().isBefore(now)) {
            return true;
        }
        return Integer.valueOf(DmsConstants.TASK_ASSIGNED).equals(task.getStatus())
                && task.getDispatchTime() != null
                && task.getDispatchTime().plusMinutes(thresholdMinutes).isBefore(now);
    }

    /**
     * 指派前约束校验（负载上限 / 载重 / 容积 / 休息中 / 资质），不满足即抛业务异常
     *
     * <p>与自动派单的评分约束不同：这里只做**硬门控**，距离/围栏/定位不参与人工指派判定。</p>
     */
    public void assertAssignable(DmsTask task, DmsRider rider) {
        if (Integer.valueOf(DmsConstants.RIDER_STATUS_REST).equals(rider.getStatus())) {
            throw new DmsBusinessException("配送员「" + rider.getRealName() + "」正在休息，不可指派");
        }
        DispatchStrategyDTO cfg = strategy();
        // 区域分包严格模式：人工指派同样受「线路绑定」约束（与候选列表的「不可指派」口径一致）
        if ("AREA".equals(cfg.getStrategy()) && Boolean.TRUE.equals(cfg.getAreaStrict())
                && task.getRouteId() != null
                && !routeRiderService.boundRiderIds(task.getRouteId()).contains(rider.getId())) {
            throw new DmsBusinessException("区域分包严格模式：配送员「" + rider.getRealName()
                    + "」未绑定任务线路（routeId=" + task.getRouteId() + "），不可指派");
        }
        int active = activeTaskCounts(Set.of(rider.getId())).getOrDefault(rider.getId(), 0);
        Integer maxConcurrent = cfg.getMaxConcurrent();
        if (maxConcurrent != null && maxConcurrent > 0 && active >= maxConcurrent) {
            throw new DmsBusinessException("配送员「" + rider.getRealName() + "」已达并接上限（在途 "
                    + active + " / 上限 " + maxConcurrent + "），不可指派");
        }
        BigDecimal maxLoad = cfg.getMaxLoadKg();
        if (maxLoad != null && maxLoad.signum() > 0 && task.getTotalWeight() != null
                && task.getTotalWeight().compareTo(maxLoad) > 0) {
            throw new DmsBusinessException("任务超出该配送员载重上限（"
                    + task.getTotalWeight().stripTrailingZeros().toPlainString() + "kg > "
                    + maxLoad.stripTrailingZeros().toPlainString() + "kg）");
        }
        BigDecimal maxVolume = cfg.getMaxVolumeM3();
        if (maxVolume != null && maxVolume.signum() > 0 && task.getTotalVolume() != null
                && task.getTotalVolume().compareTo(maxVolume) > 0) {
            throw new DmsBusinessException("任务超出该配送员容积上限（"
                    + task.getTotalVolume().stripTrailingZeros().toPlainString() + "m³ > "
                    + maxVolume.stripTrailingZeros().toPlainString() + "m³）");
        }
        // 「无资质不接单」：实名认证未通过 / 证照过期一律拒绝（受租户参数开关控制）
        kycService.assertEligible(rider.getId());
    }

    /**
     * 围栏校验 - 检查骑手是否在任务服务区域内
     *
     * @param taskId  任务ID
     * @param riderId 骑手ID
     * @return true 若在服务区域内, false 否则
     */
    public boolean fenceCheck(Long taskId, Long riderId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }

        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw new DmsBusinessException("骑手不存在: " + riderId);
        }

        boolean withinRange = isWithinGeofence(task, rider);
        log.info("围栏校验: taskId={}, riderId={}, 结果={}", taskId, riderId, withinRange);
        return withinRange;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 判断配送员是否在任务的地理围栏范围内
     *
     * 判定顺序（单一口径，复用《路线规划》的围栏档案与几何判定）：
     * 1. 任务所属**线路档案**若绑定了启用围栏（`dms_geo_fence.biz_type=ROUTE`）→ 用该围栏判定（支持多边形）；
     * 2. 未绑定围栏 → 回退「取货点为圆心 + 默认半径」的球面距离判定。
     */
    private boolean isWithinGeofence(DmsTask task, DmsRider rider) {
        if (rider.getCurrentLat() == null || rider.getCurrentLng() == null) {
            return false;
        }
        double riderLat = rider.getCurrentLat().doubleValue();
        double riderLng = rider.getCurrentLng().doubleValue();

        // 1) 线路档案绑定的围栏（命中任一即视为在范围内）
        if (task.getRouteId() != null) {
            List<GeoFence> fences = geoFenceService.listEnabledByBiz(BIZ_TYPE_ROUTE, String.valueOf(task.getRouteId()));
            if (!fences.isEmpty()) {
                for (GeoFence fence : fences) {
                    if (geoFenceService.isInside(fence, riderLat, riderLng)) {
                        log.debug("围栏判定命中: taskId={}, routeId={}, fenceId={}",
                                task.getId(), task.getRouteId(), fence.getId());
                        return true;
                    }
                }
                log.debug("围栏判定未命中: taskId={}, routeId={}, fences={}", task.getId(), task.getRouteId(), fences.size());
                return false;
            }
        }

        // 2) 回退：以取货点为圆心的默认半径判定
        if (task.getSourceLat() == null || task.getSourceLng() == null) {
            return false;
        }
        return calculateDistance(task, rider) <= DEFAULT_FENCE_RADIUS_METERS;
    }

    /**
     * 计算配送员与任务取货点的球面距离（米，Haversine）
     *
     * <p>⚠️ 任一侧缺经纬度时返回哨兵值 {@link #UNKNOWN_DISTANCE_METERS}（不可比距离），
     * 由 {@code constraintReason} 提前给出「无定位数据 / 缺取货点坐标」的明确拒绝原因，
     * 避免历史数据（配送员未上报过位置）直接 NPE 打挂整个候选/预览接口。</p>
     */
    private double calculateDistance(DmsTask task, DmsRider rider) {
        if (rider.getCurrentLat() == null || rider.getCurrentLng() == null
                || task.getSourceLat() == null || task.getSourceLng() == null) {
            return UNKNOWN_DISTANCE_METERS;
        }
        return GeoUtils.distanceMeters(
                rider.getCurrentLat().doubleValue(), rider.getCurrentLng().doubleValue(),
                task.getSourceLat().doubleValue(), task.getSourceLng().doubleValue());
    }

    /**
     * 执行任务分配（更新任务和骑手状态）
     */
    private void assignTask(DmsTask task, DmsRider rider) {
        assignTask(task, rider, 1);
    }

    /**
     * 执行任务分配（更新任务与配送员状态）
     *
     * @param dispatchType 分配方式：1-自动调度 2-手工指派（用于效果复盘的自动占比口径）
     */
    private void assignTask(DmsTask task, DmsRider rider, int dispatchType) {
        // 「无资质不接单」：实名认证未通过 / 证照过期一律拒绝指派
        kycService.assertEligible(rider.getId());

        task.setStatus(DmsConstants.TASK_ASSIGNED);
        task.setRiderId(rider.getId());
        // 名称快照：列表/详情/导出直接可用，免联查
        task.setRiderName(rider.getRealName());
        task.setDispatchType(dispatchType);
        task.setDispatchTime(LocalDateTime.now());
        taskMapper.updateById(task);

        rider.setStatus(DmsConstants.RIDER_STATUS_BUSY);
        riderMapper.updateById(rider);
    }

    // ==================== 策略配置（§3.2 策略配置 Tab） ====================

    /** 读取当前策略与约束（缺失回落代码默认值，不强制预置） */
    public DispatchStrategyDTO strategy() {
        DispatchStrategyDTO dto = new DispatchStrategyDTO();
        String strategy = cfgString(CFG_STRATEGY, "NEAREST").toUpperCase();
        dto.setStrategy(STRATEGIES.contains(strategy) ? strategy : "NEAREST");
        dto.setWeightDistance(cfgDecimal(CFG_W_DISTANCE, BigDecimal.ONE));
        dto.setWeightLoad(cfgDecimal(CFG_W_LOAD, BigDecimal.ONE));
        dto.setWeightScore(cfgDecimal(CFG_W_SCORE, BigDecimal.ONE));
        dto.setMaxConcurrent(cfgInt(CFG_MAX_CONCURRENT, 5));
        dto.setRequireOnline(cfgBool(CFG_REQUIRE_ONLINE, true));
        dto.setMaxLoadKg(cfgDecimal(CFG_MAX_LOAD_KG, BigDecimal.valueOf(2000)));
        dto.setMaxVolumeM3(cfgDecimal(CFG_MAX_VOLUME_M3, BigDecimal.TEN));
        dto.setTimeoutEscalateMinutes(cfgInt(CFG_ESCALATE_MINUTES, 30));
        dto.setAreaStrict(cfgBool(CFG_AREA_STRICT, false));
        return dto;
    }

    /** 保存策略与约束（走配置中心，保存即热生效） */
    @Transactional(rollbackFor = Exception.class)
    public DispatchStrategyDTO saveStrategy(DispatchStrategyDTO dto) {
        if (dto == null) {
            throw new DmsBusinessException("策略参数不能为空");
        }
        String strategy = dto.getStrategy() == null ? "NEAREST" : dto.getStrategy().trim().toUpperCase();
        if (!STRATEGIES.contains(strategy)) {
            throw new DmsBusinessException("不支持的派单策略: " + dto.getStrategy()
                    + "（可选 NEAREST / BALANCED / SCORE / AREA）");
        }
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        put(tenantId, CFG_STRATEGY, strategy);
        put(tenantId, CFG_W_DISTANCE, str(dto.getWeightDistance(), BigDecimal.ONE));
        put(tenantId, CFG_W_LOAD, str(dto.getWeightLoad(), BigDecimal.ONE));
        put(tenantId, CFG_W_SCORE, str(dto.getWeightScore(), BigDecimal.ONE));
        put(tenantId, CFG_MAX_CONCURRENT, String.valueOf(dto.getMaxConcurrent() == null ? 5 : dto.getMaxConcurrent()));
        put(tenantId, CFG_REQUIRE_ONLINE, String.valueOf(dto.getRequireOnline() == null || dto.getRequireOnline()));
        put(tenantId, CFG_MAX_LOAD_KG, str(dto.getMaxLoadKg(), BigDecimal.valueOf(2000)));
        put(tenantId, CFG_MAX_VOLUME_M3, str(dto.getMaxVolumeM3(), BigDecimal.TEN));
        put(tenantId, CFG_ESCALATE_MINUTES,
                String.valueOf(dto.getTimeoutEscalateMinutes() == null ? 30 : dto.getTimeoutEscalateMinutes()));
        put(tenantId, CFG_AREA_STRICT, String.valueOf(dto.getAreaStrict() != null && dto.getAreaStrict()));
        log.info("派单策略已保存: strategy={}, areaStrict={}", strategy, dto.getAreaStrict());
        return strategy();
    }

    private void put(Long tenantId, String key, String value) {
        try {
            configService.updateConfig(tenantId, key, value);
        } catch (Exception e) {
            throw new DmsBusinessException("保存派单策略失败(" + key + "): " + e.getMessage());
        }
    }

    // ==================== 调度预览 / 执行（§3.3） ====================

    /** 待分配任务的指派预览（**不落库**） */
    public DispatchPreviewVO preview(Integer maxTasks) {
        return buildPreview(normalizeLimit(maxTasks));
    }

    /** 执行自动调度；dryRun=true 仅预览不落库。返回逐单命中理由与失败原因。 */
    @Transactional(rollbackFor = Exception.class)
    public DispatchPreviewVO autoDispatchResult(boolean dryRun, Integer maxTasks) {
        DispatchPreviewVO preview = buildPreview(normalizeLimit(maxTasks));
        if (dryRun) {
            log.info("自动调度预览: 待分配 {} 单，可指派 {} 单", preview.getTaskCount(), preview.getAssignableCount());
            return preview;
        }
        int success = 0;
        for (DispatchPreviewVO.Row row : preview.getRows()) {
            if (!Boolean.TRUE.equals(row.getAssignable())) {
                continue;
            }
            // 渠道派单：走外部平台适配器下单（幂等 + 重试；失败可降级回自有运力）
            if (Boolean.TRUE.equals(row.getViaChannel())) {
                if (executeChannelRow(row, preview.getStrategy())) {
                    success++;
                }
                continue;
            }
            if (row.getRiderId() == null) {
                continue;
            }
            try {
                DmsTask task = taskMapper.selectById(row.getTaskId());
                DmsRider rider = riderMapper.selectById(row.getRiderId());
                if (task == null || rider == null) {
                    row.setAssignable(false);
                    row.setFailReason("任务或配送员已被并发修改");
                    continue;
                }
                if (task.getStatus() == null || task.getStatus() != DmsConstants.TASK_PENDING) {
                    row.setAssignable(false);
                    row.setFailReason("任务状态已变化（当前 " + task.getStatus() + "），跳过");
                    continue;
                }
                assignTask(task, rider, DispatchTypeEnum.AUTO.getValue());
                row.setRuleHit((row.getRuleHit() == null ? "" : row.getRuleHit() + "；") + "已指派");
                taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_AUTO_ASSIGN,
                        null, rider.getId(),
                        "自动调度（策略 " + preview.getStrategyText() + "）");
                publishDispatchEvent(task, rider, preview.getStrategy());
                success++;
            } catch (Exception e) {
                row.setAssignable(false);
                row.setFailReason("指派失败: " + e.getMessage());
                log.warn("自动调度指派失败: taskId={}, riderId={}", row.getTaskId(), row.getRiderId(), e);
            }
        }
        preview.setAssignableCount(success);
        log.info("自动调度完成: 策略={}, 成功 {} / 待分配 {}", preview.getStrategy(), success, preview.getTaskCount());
        return preview;
    }

    private int normalizeLimit(Integer maxTasks) {
        return maxTasks == null || maxTasks < 1 ? 50 : Math.min(maxTasks, 200);
    }

    private DispatchPreviewVO buildPreview(int maxTasks) {
        DispatchStrategyDTO cfg = strategy();
        DispatchPreviewVO result = new DispatchPreviewVO();
        result.setStrategy(cfg.getStrategy());
        result.setStrategyText(strategyText(cfg.getStrategy()));

        List<DmsTask> pendingTasks = taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .eq(DmsTask::getStatus, DmsConstants.TASK_PENDING)
                .orderByAsc(DmsTask::getCreateTime)
                .last("LIMIT " + maxTasks));
        result.setTaskCount(pendingTasks.size());

        List<DmsRider> pool = loadRiderPool(cfg);
        Map<Long, Integer> activeByRider = activeTaskCounts(pool.stream().map(DmsRider::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        AreaCtx area = loadAreaCtx(cfg, pendingTasks);
        // 任务指定的运力渠道（一次查全量，避免逐单 N+1）
        Map<Long, DmsChannel> channelMap = channelService.mapByIds(pendingTasks.stream()
                .map(DmsTask::getChannelId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        List<DispatchPreviewVO.Row> rows = new ArrayList<>();
        int assignable = 0;
        for (DmsTask task : pendingTasks) {
            DispatchPreviewVO.Row row = new DispatchPreviewVO.Row();
            row.setTaskId(task.getId());
            row.setTaskNo(task.getTaskNo());
            row.setCustomerName(task.getCustomerName());
            row.setStatus(task.getStatus());
            row.setTotalWeight(task.getTotalWeight());
            row.setTotalVolume(task.getTotalVolume());

            // ① 渠道派单优先：任务指定了可用渠道 → 交外部平台，不占用本系统配送员
            ChannelPlan plan = channelPlan(task, channelMap);
            if (plan.applicable()) {
                row.setAssignable(true);
                row.setViaChannel(true);
                row.setChannelId(plan.channel().getId());
                row.setChannelName(plan.channel().getChannelName());
                row.setRuleHit("渠道派单：" + plan.channel().getChannelName()
                        + "（外部平台运力，不占用本系统配送员）");
                assignable++;
                rows.add(row);
                continue;
            }
            // ② 渠道不可用（或未指定渠道）→ 按自有运力派单，并在文案里说明降级原因
            String channelHint = task.getChannelId() == null ? ""
                    : "（渠道不可用：" + plan.reason() + "，按自有运力派单）";
            DispatchCandidateVO best = pickBest(task, pool, activeByRider, cfg, area);
            if (best == null) {
                row.setAssignable(false);
                row.setFailReason(failReason(task, pool, activeByRider, cfg, area) + channelHint);
            } else {
                row.setAssignable(true);
                row.setRiderId(best.getRiderId());
                row.setRiderName(best.getRiderName());
                row.setRiderPhone(best.getRiderPhone());
                row.setDistanceMeters(best.getDistanceMeters());
                assignable++;
                row.setRuleHit(ruleText(cfg, area, task, best) + channelHint);
            }
            rows.add(row);
        }
        result.setAssignableCount(assignable);
        result.setUnassignableCount(rows.size() - assignable);
        result.setRows(rows);
        return result;
    }

    // ==================== 渠道派单（§7.1 派单接线） ====================

    /**
     * 执行渠道派单：成功后任务流转「已分配」并写调度审计（外部单号见外部单台账）；
     * 失败且允许降级时回退自有运力派单，避免派单中断。
     */
    private boolean executeChannelRow(DispatchPreviewVO.Row row, String strategy) {
        DmsTask task = taskMapper.selectById(row.getTaskId());
        if (task == null) {
            row.setAssignable(false);
            row.setFailReason("任务已被并发删除");
            return false;
        }
        if (!Integer.valueOf(DmsConstants.TASK_PENDING).equals(task.getStatus())) {
            row.setAssignable(false);
            row.setFailReason("任务状态已变化（当前 "
                    + TaskStatusEnum.fromValue(nzStatus(task.getStatus())).getDescription() + "），跳过");
            return false;
        }
        try {
            ChannelOrderService.PushResult push = channelOrderService.pushOrder(task.getId(), row.getChannelId(),
                    DispatchTypeEnum.AUTO.getValue(), "自动调度（策略 " + strategy + "，渠道派单）");
            if (push.success()) {
                row.setChannelOrderNo(push.channelOrderNo());
                row.setRuleHit(append(row.getRuleHit(), "已下单 " + push.channelOrderNo()
                        + (push.reused() ? "（幂等复用台账）" : "")));
                return true;
            }
            if (!channelOrderService.fallbackEnabled()) {
                row.setAssignable(false);
                row.setFailReason(push.message() + "（未开启降级：dms.channel.push.fallback=false）");
                return false;
            }
            return fallbackToRider(row, task, push.message());
        } catch (Exception e) {
            row.setAssignable(false);
            row.setFailReason("渠道派单失败: " + e.getMessage());
            log.warn("渠道派单异常: taskId={}, channelId={}", row.getTaskId(), row.getChannelId(), e);
            return false;
        }
    }

    /** 渠道不可用时的降级：按当前策略在自有运力中选人派单（人工指派的硬门控同样生效） */
    private boolean fallbackToRider(DispatchPreviewVO.Row row, DmsTask task, String channelError) {
        DispatchStrategyDTO cfg = strategy();
        List<DmsRider> pool = loadRiderPool(cfg);
        Map<Long, Integer> activeByRider = activeTaskCounts(pool.stream().map(DmsRider::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        AreaCtx area = loadAreaCtx(cfg, List.of(task));
        DispatchCandidateVO best = pickBest(task, pool, activeByRider, cfg, area);
        if (best == null) {
            row.setAssignable(false);
            row.setFailReason("渠道不可用（" + channelError + "）且无自有运力可派："
                    + failReason(task, pool, activeByRider, cfg, area));
            return false;
        }
        try {
            DmsRider rider = riderMapper.selectById(best.getRiderId());
            if (rider == null) {
                row.setAssignable(false);
                row.setFailReason("渠道不可用，且候选配送员已不存在（并发变化）");
                return false;
            }
            assertAssignable(task, rider);
            assignTask(task, rider, DispatchTypeEnum.AUTO.getValue());
            taskLogService.record(task.getId(), task.getTaskNo(), DmsTaskLog.ACTION_AUTO_ASSIGN,
                    null, rider.getId(), "渠道不可用（" + channelError + "），降级回自有运力派单");
            publishDispatchEvent(task, rider, cfg.getStrategy());
            row.setViaChannel(false);
            row.setRiderId(rider.getId());
            row.setRiderName(rider.getRealName());
            row.setRiderPhone(rider.getPhone());
            row.setRuleHit(append(row.getRuleHit(),
                    "渠道不可用（" + channelError + "）→ 降级自有运力：命中 " + rider.getRealName()));
            return true;
        } catch (Exception e) {
            row.setAssignable(false);
            row.setFailReason("渠道不可用且降级失败: " + e.getMessage());
            log.warn("渠道派单降级失败: taskId={}", task.getId(), e);
            return false;
        }
    }

    /** 任务指定渠道的可用性判定（渠道存在 + 启用 + 适配器已装配） */
    private ChannelPlan channelPlan(DmsTask task) {
        if (task.getChannelId() == null) {
            return ChannelPlan.none();
        }
        return channelPlan(task, channelService.mapByIds(List.of(task.getChannelId())));
    }

    private ChannelPlan channelPlan(DmsTask task, Map<Long, DmsChannel> channelMap) {
        if (task.getChannelId() == null) {
            return ChannelPlan.none();
        }
        DmsChannel channel = channelMap == null ? null : channelMap.get(task.getChannelId());
        if (channel == null) {
            return new ChannelPlan(false, null, "渠道不存在或已删除");
        }
        if (!Integer.valueOf(1).equals(channel.getStatus())) {
            return new ChannelPlan(false, channel, "渠道已停用");
        }
        if (channelService.resolveAdapter(channel) == null) {
            String bean = channel.getAdapterBean();
            return new ChannelPlan(false, channel, "适配器未装配（"
                    + (bean == null || bean.isBlank() ? "未配置适配器 Bean" : bean + " 未启用") + "）");
        }
        return new ChannelPlan(true, channel, null);
    }

    /**
     * 人工指派/改派前：任务已指定**可用**外部渠道时拒绝指派本系统配送员。
     *
     * <p>口径：渠道任务的指派权在平台侧；如需改用自有运力，请先清空任务的运力渠道。
     * 渠道不可用（停用/适配器未装配）时不拦截——此时任务本就回落到自有运力派单。</p>
     */
    private void assertNotChannelTask(DmsTask task) {
        ChannelPlan plan = channelPlan(task);
        if (plan.applicable()) {
            throw new DmsBusinessException("任务已指定外部运力渠道「" + plan.channel().getChannelName()
                    + "」，不可指派本系统配送员；如需自有运力请先清空任务的运力渠道");
        }
    }

    /** 渠道可用性判定结果（applicable=false 时 reason 给出可读原因，用于降级说明） */
    private record ChannelPlan(boolean applicable, DmsChannel channel, String reason) {

        static ChannelPlan none() {
            return new ChannelPlan(false, null, "任务未指定运力渠道");
        }
    }

    private String append(String base, String extra) {
        return (base == null || base.isBlank() ? "" : base + "；") + extra;
    }

    private int nzStatus(Integer status) {
        return status == null ? DmsConstants.TASK_PENDING : status;
    }

    /**
     * 命中规则文案（逐单可读：谁被命中、距离/在途/评分、区域分包与在线口径）
     */
    private String ruleText(DispatchStrategyDTO cfg, AreaCtx area, DmsTask task, DispatchCandidateVO best) {
        long meters = best.getDistanceMeters() == null ? 0 : Math.round(best.getDistanceMeters().doubleValue());
        StringBuilder rule = new StringBuilder(strategyText(cfg.getStrategy()))
                .append("：命中 ").append(best.getRiderName())
                .append("，").append(meters).append(" 米、在途 ").append(best.getActiveTasks()).append(" 单");
        if (best.getRatingScore() != null) {
            rule.append("、评分 ").append(best.getRatingScore());
        }
        if (area.active()) {
            if (task.getRouteId() == null) {
                rule.append("（区域分包降级：任务未指定配送线路）");
            } else if (area.bound(task.getRouteId()).contains(best.getRiderId())) {
                rule.append("（区域分包：线路「").append(area.routeName(task.getRouteId()))
                        .append("」绑定）");
            } else {
                rule.append("（区域分包：未绑定线路「").append(area.routeName(task.getRouteId()))
                        .append("」，备用运力）");
            }
        }
        if (Boolean.TRUE.equals(cfg.getRequireOnline()) && Boolean.TRUE.equals(best.getOnline())) {
            rule.append("、在线");
        }
        return rule.toString();
    }

    /** 候选池：按配置决定是否仅取「空闲」配送员 */
    private List<DmsRider> loadRiderPool(DispatchStrategyDTO cfg) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(cfg.getRequireOnline())) {
            wrapper.eq(DmsRider::getStatus, DmsConstants.RIDER_STATUS_IDLE);
        } else {
            wrapper.ne(DmsRider::getStatus, 3);
        }
        return riderMapper.selectList(wrapper);
    }

    /**
     * 区域分包上下文：线路 → 启用绑定的配送员 + 线路名称
     *
     * <p>仅 AREA 策略下产生查询（其它策略零开销）；一次批量解析，避免逐单 N+1。</p>
     */
    private AreaCtx loadAreaCtx(DispatchStrategyDTO cfg, List<DmsTask> tasks) {
        if (!"AREA".equals(cfg.getStrategy())) {
            return AreaCtx.none();
        }
        Set<Long> routeIds = tasks.stream().map(DmsTask::getRouteId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new AreaCtx(true, routeRiderService.boundRiderIdsByRoute(routeIds),
                routeRiderService.routeNames(routeIds));
    }

    private DispatchCandidateVO pickBest(DmsTask task, List<DmsRider> pool,
                                        Map<Long, Integer> activeByRider, DispatchStrategyDTO cfg, AreaCtx area) {
        return evaluate(task, pool, activeByRider, cfg, area).stream()
                .filter(c -> Boolean.TRUE.equals(c.getEligible()))
                .max(Comparator.comparing(DispatchCandidateVO::getScore))
                .orElse(null);
    }

    /** 无人可派时的原因：优先展示「最近一个候选」的拒绝原因，便于运维定位是约束太紧还是运力不足 */
    private String failReason(DmsTask task, List<DmsRider> pool,
                              Map<Long, Integer> activeByRider, DispatchStrategyDTO cfg, AreaCtx area) {
        if (pool.isEmpty()) {
            return "无人可派（无可用配送员）";
        }
        return evaluate(task, pool, activeByRider, cfg, area).stream()
                .filter(c -> c.getReason() != null)
                .min(Comparator.comparing(DispatchCandidateVO::getDistanceMeters))
                .map(c -> "无人可派：" + c.getRiderName() + "（" + c.getReason() + "）")
                .orElse("无人可派（候选均不满足约束）");
    }

    /** 逐候选评分 + 约束判定（不可派的也返回，便于解释「为什么没人可派」） */
    private List<DispatchCandidateVO> evaluate(DmsTask task, List<DmsRider> pool,
                                              Map<Long, Integer> activeByRider, DispatchStrategyDTO cfg, AreaCtx area) {
        List<DispatchCandidateVO> list = new ArrayList<>();
        for (DmsRider rider : pool) {
            DispatchCandidateVO vo = new DispatchCandidateVO();
            vo.setRiderId(rider.getId());
            vo.setRiderName(rider.getRealName());
            vo.setRiderPhone(rider.getPhone());
            vo.setVehicleNo(rider.getVehicleNo());
            vo.setCurrentLat(rider.getCurrentLat());
            vo.setCurrentLng(rider.getCurrentLng());
            vo.setStatus(rider.getStatus());
            vo.setTotalOrders(rider.getTotalOrders());
            vo.setRatingScore(rider.getRatingScore());
            vo.setOnline(isOnline(rider));
            vo.setLastReportTime(rider.getLastReportTime());
            boolean routeBound = area.active() && task.getRouteId() != null
                    && area.bound(task.getRouteId()).contains(rider.getId());
            vo.setRouteBound(routeBound);
            int active = activeByRider.getOrDefault(rider.getId(), 0);
            vo.setActiveTasks(active);
            double distance = calculateDistance(task, rider);
            vo.setDistanceMeters(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));
            String reason = constraintReason(task, rider, active, cfg, area);
            vo.setEligible(reason == null);
            vo.setReason(reason);
            vo.setScore(reason == null ? score(task, distance, active, rider, cfg, area) : BigDecimal.valueOf(-1));
            list.add(vo);
        }
        return list;
    }

    /** 约束校验：通过返回 null，否则返回拒绝原因 */
    private String constraintReason(DmsTask task, DmsRider rider, int active,
                                    DispatchStrategyDTO cfg, AreaCtx area) {
        if (Integer.valueOf(3).equals(rider.getStatus())) {
            return "配送员休息中";
        }
        if (rider.getCurrentLat() == null || rider.getCurrentLng() == null) {
            return "配送员无定位数据（未上报过位置）";
        }
        if (task.getSourceLat() == null || task.getSourceLng() == null) {
            return "任务缺少取货点坐标（无法计算距离与围栏）";
        }
        // 区域分包（严格模式）：任务线路未绑定该配送员 → 直接不可派
        if (area.active() && task.getRouteId() != null && Boolean.TRUE.equals(cfg.getAreaStrict())
                && !area.bound(task.getRouteId()).contains(rider.getId())) {
            return "区域分包：未绑定线路「" + area.routeName(task.getRouteId()) + "」";
        }
        // 在线口径（与《实时跟踪》《配送跟踪》《配送员管理》统一）：位置上报心跳在阈值内才视为在线
        if (Boolean.TRUE.equals(cfg.getRequireOnline()) && !isOnline(rider)) {
            return "配送员离线（" + onlineAgoText(rider) + "）";
        }
        Integer maxConcurrent = cfg.getMaxConcurrent();
        if (maxConcurrent != null && maxConcurrent > 0 && active >= maxConcurrent) {
            return "已达并接上限（在途 " + active + " / 上限 " + maxConcurrent + "）";
        }
        BigDecimal maxLoad = cfg.getMaxLoadKg();
        if (maxLoad != null && maxLoad.signum() > 0 && task.getTotalWeight() != null
                && task.getTotalWeight().compareTo(maxLoad) > 0) {
            return "超出载重上限（" + task.getTotalWeight().stripTrailingZeros().toPlainString()
                    + "kg > " + maxLoad.stripTrailingZeros().toPlainString() + "kg）";
        }
        BigDecimal maxVolume = cfg.getMaxVolumeM3();
        if (maxVolume != null && maxVolume.signum() > 0 && task.getTotalVolume() != null
                && task.getTotalVolume().compareTo(maxVolume) > 0) {
            return "超出容积上限（" + task.getTotalVolume().stripTrailingZeros().toPlainString()
                    + "m³ > " + maxVolume.stripTrailingZeros().toPlainString() + "m³）";
        }
        // 资质门控①：实名认证未通过直接拒绝（KycService 只管证照有效期，未提交 KYC 时若无此判会放行）
        if (!Integer.valueOf(1).equals(rider.getVerifyStatus())) {
            return "资质不满足：实名认证未通过（verifyStatus=" + rider.getVerifyStatus() + "）";
        }
        // 资质门控②：证照有效期（实名认证域的统一口径）
        try {
            kycService.assertEligible(rider.getId());
        } catch (Exception e) {
            return "资质不满足：" + e.getMessage();
        }
        if (!isWithinGeofence(task, rider)) {
            return "超出服务围栏";
        }
        return null;
    }

    /** 在线判定：最近位置上报在 `dms.tracking.online.minutes` 内（null=从未上报 → 离线） */
    private boolean isOnline(DmsRider rider) {
        if (rider.getLastReportTime() == null) {
            return false;
        }
        int minutes = cfgInt(CFG_ONLINE_MINUTES, DEFAULT_ONLINE_MINUTES);
        return rider.getLastReportTime().isAfter(LocalDateTime.now().minusMinutes(Math.max(minutes, 1)));
    }

    private String onlineAgoText(DmsRider rider) {
        if (rider.getLastReportTime() == null) {
            return "从未上报位置";
        }
        long minutes = Duration.between(rider.getLastReportTime(), LocalDateTime.now()).toMinutes();
        return "最近上报 " + Math.max(minutes, 0) + " 分钟前";
    }

    /** 综合得分：距离越近/在途越少/评分越高 → 得分越高（权重来自策略配置；区域分包命中的绑定者加权） */
    private BigDecimal score(DmsTask task, double distanceMeters, int activeTasks,
                             DmsRider rider, DispatchStrategyDTO cfg, AreaCtx area) {
        BigDecimal wDistance = nvl(cfg.getWeightDistance(), BigDecimal.ONE);
        BigDecimal wLoad = nvl(cfg.getWeightLoad(), BigDecimal.ONE);
        BigDecimal wScore = nvl(cfg.getWeightScore(), BigDecimal.ONE);
        BigDecimal distanceScore = BigDecimal.valueOf(1000)
                .divide(BigDecimal.valueOf(Math.max(distanceMeters, 1)), 4, RoundingMode.HALF_UP);
        BigDecimal loadScore = BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(Math.max(activeTasks, 1)), 4, RoundingMode.HALF_UP);
        BigDecimal rating = rider.getRatingScore() == null ? BigDecimal.valueOf(4) : rider.getRatingScore();
        BigDecimal score = distanceScore.multiply(wDistance)
                .add(loadScore.multiply(wLoad))
                .add(rating.multiply(wScore));
        if ("BALANCED".equals(cfg.getStrategy())) {
            score = score.add(loadScore.multiply(BigDecimal.valueOf(2)));
        } else if ("SCORE".equals(cfg.getStrategy())) {
            score = score.add(rating.multiply(BigDecimal.valueOf(2)));
        } else if (area.active() && task.getRouteId() != null
                && area.bound(task.getRouteId()).contains(rider.getId())) {
            // 区域分包：线路绑定的配送员固定加权，保证「绑定者优先」不被距离分反超
            score = score.add(AREA_BOUND_BONUS);
        }
        return score.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 区域分包上下文（线路 → 启用绑定的配送员 + 线路名称）
     *
     * @param active         是否处于区域分包策略（false 时后续判定与查询全部跳过）
     * @param boundByRoute   线路ID → 该线路绑定的配送员ID集合
     * @param routeNames     线路ID → 线路名称（命中规则文案用）
     */
    private record AreaCtx(boolean active, Map<Long, Set<Long>> boundByRoute, Map<Long, String> routeNames) {

        static AreaCtx none() {
            return new AreaCtx(false, Map.of(), Map.of());
        }

        Set<Long> bound(Long routeId) {
            return routeId == null ? Set.of() : boundByRoute.getOrDefault(routeId, Set.of());
        }

        String routeName(Long routeId) {
            String name = routeId == null ? null : routeNames.get(routeId);
            return name == null ? "#" + routeId : name;
        }
    }

    /** 候选配送员（含评分与约束说明，供「手动指派」选人） */
    public List<DispatchCandidateVO> candidateVO(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        DispatchStrategyDTO cfg = strategy();
        // 候选池不做状态预过滤：「休息中」「资质不满足」等要作为**明确拒绝原因**展示出来
        List<DmsRider> pool = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>());
        Map<Long, Integer> activeByRider = activeTaskCounts(pool.stream().map(DmsRider::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        AreaCtx area = loadAreaCtx(cfg, List.of(task));
        return evaluate(task, pool, activeByRider, cfg, area).stream()
                .sorted(Comparator.comparing(DispatchCandidateVO::getEligible, Comparator.reverseOrder())
                        .thenComparing(DispatchCandidateVO::getScore, Comparator.reverseOrder()))
                .toList();
    }

    /** 调度效果复盘（§3.3 `/dispatch/stat`） */
    public DispatchStatVO stat() {
        DispatchStatVO vo = new DispatchStatVO();
        List<DmsTask> all = taskMapper.selectList(new LambdaQueryWrapper<>());
        vo.setTaskTotal(all.size());
        vo.setPendingCount(all.stream().filter(t -> Objects.equals(t.getStatus(), DmsConstants.TASK_PENDING)).count());
        List<DmsTask> assigned = all.stream().filter(t -> t.getDispatchTime() != null).toList();
        vo.setAssignedCount(assigned.size());
        vo.setAutoCount(all.stream().filter(t -> Integer.valueOf(DispatchTypeEnum.AUTO.getValue())
                .equals(t.getDispatchType())).count());
        vo.setManualCount(all.stream().filter(t -> Integer.valueOf(DispatchTypeEnum.MANUAL.getValue())
                .equals(t.getDispatchType())).count());
        // 抢单 / 竞价归口《订单池》，本页只做占比统计（避免两页重复实现抢单能力）
        vo.setGrabCount(all.stream().filter(t -> Integer.valueOf(DispatchTypeEnum.GRAB.getValue())
                .equals(t.getDispatchType())).count());
        vo.setBidCount(all.stream().filter(t -> Integer.valueOf(DispatchTypeEnum.BID.getValue())
                .equals(t.getDispatchType())).count());
        vo.setOtherCount(vo.getTaskTotal() - vo.getAutoCount() - vo.getManualCount()
                - vo.getGrabCount() - vo.getBidCount());
        vo.setAutoRate(rate(vo.getAutoCount(), vo.getAutoCount() + vo.getManualCount()));
        long seconds = 0;
        int counted = 0;
        for (DmsTask t : assigned) {
            if (t.getCreateTime() != null && t.getDispatchTime() != null) {
                seconds += Math.max(Duration.between(t.getCreateTime(), t.getDispatchTime()).getSeconds(), 0);
                counted++;
            }
        }
        vo.setAvgDispatchSeconds(counted == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(seconds * 1.0 / counted).setScale(2, RoundingMode.HALF_UP));
        long active = all.stream().filter(t -> t.getStatus() != null && ACTIVE_STATUS.contains(t.getStatus())).count();
        long overdue = all.stream().filter(t -> t.getStatus() != null && ACTIVE_STATUS.contains(t.getStatus())
                && t.getDeadlineTime() != null && t.getDeadlineTime().isBefore(LocalDateTime.now())).count();
        vo.setActiveCount(active);
        vo.setOverdueCount(overdue);
        vo.setOverdueRate(rate(overdue, active));
        vo.setUnassignedCount(vo.getPendingCount());
        return vo;
    }

    // ==================== 私有辅助（策略/统计） ====================

    private Map<Long, Integer> activeTaskCounts(Set<Long> riderIds) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        if (riderIds.isEmpty()) {
            return map;
        }
        for (DmsTask t : taskMapper.selectList(new LambdaQueryWrapper<DmsTask>()
                .in(DmsTask::getRiderId, riderIds)
                .in(DmsTask::getStatus, ACTIVE_STATUS)
                .select(DmsTask::getRiderId, DmsTask::getId))) {
            map.merge(t.getRiderId(), 1, Integer::sum);
        }
        return map;
    }

    /** 派单审计：经 dms_event_outbox 外发（消费方可做调度复盘/通知） */
    private void publishDispatchEvent(DmsTask task, DmsRider rider, String strategy) {
        try {
            String payload = "{\"taskId\":" + task.getId()
                    + ",\"taskNo\":\"" + esc(task.getTaskNo()) + "\""
                    + ",\"riderId\":" + rider.getId()
                    + ",\"riderName\":\"" + esc(rider.getRealName()) + "\""
                    + ",\"strategy\":\"" + esc(strategy) + "\"}";
            eventService.publishEvent("DISPATCH_ASSIGNED", "dms:dispatch", payload);
        } catch (Exception e) {
            log.warn("派单事件外发失败: taskId={}", task.getId(), e);
        }
    }

    private String esc(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }

    private String strategyText(String strategy) {
        if (strategy == null) {
            return "最近可用";
        }
        return switch (strategy) {
            case "BALANCED" -> "负载均衡";
            case "SCORE" -> "评分优先";
            case "AREA" -> "区域分包";
            default -> "最近可用";
        };
    }

    private BigDecimal rate(long numerator, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String str(BigDecimal value, BigDecimal defaultValue) {
        return nvl(value, defaultValue).stripTrailingZeros().toPlainString();
    }

    private String cfgString(String key, String defaultValue) {
        try {
            String value = configService.getString(MyBatisPlusConfig.getCurrentTenantIdValue(), key);
            return value == null || value.isBlank() ? defaultValue : value.trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer cfgInt(String key, int defaultValue) {
        try {
            Integer value = configService.getInteger(MyBatisPlusConfig.getCurrentTenantIdValue(), key);
            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private BigDecimal cfgDecimal(String key, BigDecimal defaultValue) {
        String value = cfgString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Boolean cfgBool(String key, boolean defaultValue) {
        String value = cfgString(key, null);
        return value == null ? defaultValue : ("true".equalsIgnoreCase(value) || "1".equals(value));
    }
}
