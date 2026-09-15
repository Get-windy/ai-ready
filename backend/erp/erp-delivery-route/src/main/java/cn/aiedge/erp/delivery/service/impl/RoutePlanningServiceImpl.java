package cn.aiedge.erp.delivery.service.impl;

import cn.aiedge.base.entity.SysMessage;
import cn.aiedge.base.mapper.SysMessageMapper;
import cn.aiedge.base.service.MessageService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.route.dto.RoutePlanRequest;
import cn.aiedge.dms.route.dto.RoutePlanResponse;
import cn.aiedge.erp.delivery.dto.DeliveryEtaNotifyQueryDTO;
import cn.aiedge.erp.delivery.dto.ExpeditePointDTO;
import cn.aiedge.erp.delivery.dto.RouteEtaVO;
import cn.aiedge.erp.delivery.entity.DeliveryEtaNotify;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.entity.RoutePoint;
import cn.aiedge.erp.delivery.enums.PointStatus;
import cn.aiedge.erp.delivery.enums.RouteStatus;
import cn.aiedge.erp.delivery.mapper.DeliveryEtaNotifyMapper;
import cn.aiedge.erp.delivery.mapper.DeliveryRouteMapper;
import cn.aiedge.erp.delivery.mapper.RoutePointMapper;
import cn.aiedge.erp.delivery.service.RoutePlanningService;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 配送路线规划 / 催单调整 / ETA 预估 服务
 *
 * 边界（研判结论）：地图能力**只在 dms-delivery 实现一次**，本服务只消费
 * {@link cn.aiedge.dms.route.service.RouteService}（含「未配置地图 Key 自动降级直线估算」），
 * 不在本模块自建地图调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutePlanningServiceImpl implements RoutePlanningService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DeliveryRouteMapper routeMapper;
    private final RoutePointMapper pointMapper;
    private final DeliveryEtaNotifyMapper etaNotifyMapper;

    /** DMS 地理能力（全系统单一口径，含降级） */
    private final cn.aiedge.dms.route.service.RouteService dmsRouteService;

    /** 消息底座（短信通道统一走此处，不另建发送通道） */
    private final MessageService messageService;
    private final SysMessageMapper sysMessageMapper;

    // ==================== 路线规划 ====================

    @Override
    @Transactional
    public Map<String, Object> planOrder(Long routeId) {
        DeliveryRoute route = requireRoute(routeId);
        if (RouteStatus.COMPLETED.getCode().equals(route.getStatus())
                || RouteStatus.CANCELLED.getCode().equals(route.getStatus())) {
            throw BusinessException.badRequest("路线已" + route.getStatus() + "，不可再规划");
        }
        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        List<RoutePoint> located = points.stream().filter(p -> hasCoord(p)).toList();
        List<RoutePoint> unlocated = points.stream().filter(p -> !hasCoord(p)).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("totalPoints", points.size());
        result.put("locatedPoints", located.size());
        result.put("unlocatedPoints", unlocated.size());
        if (located.isEmpty()) {
            throw BusinessException.badRequest("没有带坐标的点位，无法规划（请先维护客户配送坐标）");
        }

        RoutePlanRequest.Coordinate origin = resolveOrigin(route, located);
        RoutePlanRequest request = RoutePlanRequest.builder()
                .origin(origin)
                .destinations(located.stream().map(this::toCoordinate).toList())
                .strategy(0)
                .optimizeOrder(true)
                .build();

        RoutePlanResponse response = dmsRouteService.planDeliveryRoute(request);
        if (!response.isSuccess()) {
            throw BusinessException.badRequest("路线规划失败：" + response.getMessage());
        }

        // 按地图返回的访问顺序重排点位（未带坐标的点位顺延保留在末尾）
        List<Integer> order = response.getOptimizedOrder();
        List<RoutePoint> ordered = new ArrayList<>();
        if (order != null && order.size() == located.size()) {
            order.forEach(i -> ordered.add(located.get(i)));
        } else {
            ordered.addAll(located);
        }
        ordered.addAll(unlocated);

        // 参与规划（带坐标）的点位顺序，legs 只与其对齐；未带坐标的点位顺延在末尾且无分段数据
        List<RoutePoint> orderedLocated = new ArrayList<>(ordered.subList(0, located.size()));
        int seq = 1;
        for (RoutePoint p : ordered) {
            p.setPointOrder(seq++);
            if (!hasCoord(p)) {
                p.setDistanceFromPrev(null);
                p.setDurationFromPrev(null);
            }
        }
        // 分段距离/时长：stops[0] 为起点，stops[k+1] 对应访问顺序第 k 个点位
        List<RoutePlanResponse.RouteStop> stops = response.getStops();
        if (stops != null && stops.size() == orderedLocated.size() + 1) {
            for (int k = 0; k < orderedLocated.size(); k++) {
                RoutePlanResponse.RouteStop leg = stops.get(k + 1);
                RoutePoint p = orderedLocated.get(k);
                p.setDistanceFromPrev(leg.getDistanceFromPrev() == null ? null : leg.getDistanceFromPrev().doubleValue());
                p.setDurationFromPrev(leg.getDurationFromPrev() == null ? null : leg.getDurationFromPrev().intValue());
            }
        }
        ordered.forEach(pointMapper::updateById);

        route.setTotalDistance(response.getTotalDistance() == null ? null : response.getTotalDistance().doubleValue());
        route.setTotalDuration(response.getTotalDuration() == null ? null : (int) (response.getTotalDuration() / 60));
        RoutePoint last = ordered.get(ordered.size() - 1);
        route.setEndPoint(last.getAddress());
        route.setEndLatitude(last.getLatitude());
        route.setEndLongitude(last.getLongitude());
        routeMapper.updateById(route);

        result.put("provider", response.getProvider());
        result.put("degraded", response.isDegraded());
        result.put("totalDistance", route.getTotalDistance());
        result.put("totalDuration", route.getTotalDuration());
        result.put("message", response.isDegraded()
                ? "未配置地图 Key，已按直线距离估算并完成顺序优化"
                : "已按地图能力完成路线规划");
        log.info("配送路线规划完成: routeId={}, provider={}, degraded={}", routeId, response.getProvider(), response.isDegraded());
        return result;
    }

    // ==================== 催单：提前某客户 ====================

    @Override
    @Transactional
    public Map<String, Object> expeditePoint(Long routeId, Long pointId, ExpeditePointDTO dto) {
        DeliveryRoute route = requireRoute(routeId);
        if (RouteStatus.COMPLETED.getCode().equals(route.getStatus())
                || RouteStatus.CANCELLED.getCode().equals(route.getStatus())) {
            throw BusinessException.badRequest("路线已" + route.getStatus() + "，不可调整顺序");
        }
        List<RoutePoint> points = new ArrayList<>(pointMapper.selectByRouteId(routeId));
        RoutePoint target = points.stream()
                .filter(p -> p.getId().equals(pointId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("配送点位不存在: " + pointId));
        if (PointStatus.DELIVERED.getCode().equals(target.getStatus())
                || PointStatus.FAILED.getCode().equals(target.getStatus())
                || PointStatus.SKIPPED.getCode().equals(target.getStatus())) {
            throw BusinessException.badRequest("该点位已处理完毕，无需催单");
        }

        int targetSeq = dto == null || dto.getTargetSeq() == null ? 1 : Math.max(1, dto.getTargetSeq());
        targetSeq = Math.min(targetSeq, points.size());

        // 其余点位保持原相对顺序，目标点位插到 targetSeq
        points.sort(Comparator.comparing(p -> p.getPointOrder() == null ? 0 : p.getPointOrder()));
        points.remove(target);
        points.add(targetSeq - 1, target);

        int seq = 1;
        for (RoutePoint p : points) {
            p.setPointOrder(seq++);
        }
        target.setExpedited(1);
        if (dto != null && StringUtils.hasText(dto.getReason())) {
            String old = target.getRemark() == null ? "" : target.getRemark();
            target.setRemark(("【催单】" + dto.getReason().trim() + (old.isEmpty() ? "" : "；" + old)));
        }

        // 可选：对目标点位之后的剩余点位重新做地图规划（催单点固定不动）
        Map<String, Object> result = new HashMap<>();
        result.put("expeditedPointId", pointId);
        result.put("targetSeq", targetSeq);
        result.put("replanned", false);

        boolean replanRest = dto != null && Boolean.TRUE.equals(dto.getReplanRest());
        List<RoutePoint> rest = points.size() > targetSeq ? new ArrayList<>(points.subList(targetSeq, points.size())) : List.of();
        List<RoutePoint> restLocated = rest.stream().filter(this::hasCoord)
                .filter(p -> !PointStatus.DELIVERED.getCode().equals(p.getStatus())
                        && !PointStatus.FAILED.getCode().equals(p.getStatus())
                        && !PointStatus.SKIPPED.getCode().equals(p.getStatus()))
                .toList();
        if (replanRest && restLocated.size() > 1) {
            RoutePlanRequest.Coordinate origin = hasCoord(target) ? toCoordinate(target) : resolveOrigin(route, points);
            RoutePlanResponse response = dmsRouteService.planDeliveryRoute(RoutePlanRequest.builder()
                    .origin(origin)
                    .destinations(restLocated.stream().map(this::toCoordinate).toList())
                    .strategy(0)
                    .optimizeOrder(true)
                    .build());
            if (response.isSuccess() && response.getOptimizedOrder() != null
                    && response.getOptimizedOrder().size() == restLocated.size()) {
                List<RoutePoint> reordered = response.getOptimizedOrder().stream().map(restLocated::get).toList();
                int cursor = targetSeq;
                for (RoutePoint p : reordered) {
                    points.set(cursor++, p);
                }
                int s = 1;
                for (RoutePoint p : points) {
                    p.setPointOrder(s++);
                }
                result.put("replanned", true);
                result.put("degraded", response.isDegraded());
            }
        }

        points.forEach(pointMapper::updateById);
        log.info("催单调整: routeId={}, pointId={}, targetSeq={}, replanned={}", routeId, pointId, targetSeq, result.get("replanned"));
        result.put("message", "已将该点位调整到第 " + targetSeq + " 位"
                + (Boolean.TRUE.equals(result.get("replanned")) ? "，后续点位已按地图能力重新排序" : ""));
        return result;
    }

    // ==================== ETA 预估 ====================

    @Override
    @Transactional
    public RouteEtaVO calcEta(Long routeId) {
        DeliveryRoute route = requireRoute(routeId);
        List<RoutePoint> all = pointMapper.selectByRouteId(routeId);
        List<RoutePoint> remaining = all.stream()
                .filter(p -> PointStatus.PENDING.getCode().equals(p.getStatus())
                        || PointStatus.IN_ROUTE.getCode().equals(p.getStatus()))
                .toList();

        RouteEtaVO vo = new RouteEtaVO();
        vo.setRouteId(routeId);
        vo.setRouteCode(route.getRouteCode());
        vo.setBaseTime(LocalDateTime.now());
        vo.setRemainingPoints(remaining.size());

        List<RoutePoint> located = remaining.stream().filter(this::hasCoord).toList();
        if (located.isEmpty()) {
            vo.setProvider("none");
            vo.setDegraded(true);
            return vo;
        }

        // 当前位置：优先「已送达/在途」的最后一个点位，其次路线起点
        RoutePlanRequest.Coordinate origin = resolveOrigin(route, all);
        // optimizeOrder=false：ETA 必须沿既定顺序累计，不能重排
        RoutePlanResponse response = dmsRouteService.planDeliveryRoute(RoutePlanRequest.builder()
                .origin(origin)
                .destinations(located.stream().map(this::toCoordinate).toList())
                .strategy(0)
                .optimizeOrder(false)
                .build());
        if (!response.isSuccess()) {
            throw BusinessException.badRequest("ETA 计算失败：" + response.getMessage());
        }
        vo.setProvider(response.getProvider());
        vo.setDegraded(response.isDegraded());

        long cumulativeSeconds = 0L;
        List<RoutePlanResponse.RouteStop> stops = response.getStops();
        LocalDateTime now = vo.getBaseTime();
        boolean legAligned = stops != null && stops.size() == located.size() + 1;
        List<RouteEtaVO.Item> items = new ArrayList<>();
        int locatedIdx = 0;
        // 逐点按既定顺序累计；缺坐标的点位不参与累计，ETA 留空（前端提示「缺坐标」）
        for (RoutePoint p : remaining) {
            Integer legSeconds = null;
            Double legMeters = null;
            LocalDateTime eta = null;
            if (hasCoord(p)) {
                if (legAligned) {
                    RoutePlanResponse.RouteStop leg = stops.get(locatedIdx + 1);
                    legSeconds = leg.getDurationFromPrev() == null ? null : leg.getDurationFromPrev().intValue();
                    legMeters = leg.getDistanceFromPrev() == null ? null : leg.getDistanceFromPrev().doubleValue();
                }
                cumulativeSeconds += legSeconds == null ? 0 : legSeconds;
                eta = now.plusSeconds(cumulativeSeconds);
                p.setEtaTime(eta);
                pointMapper.updateById(p);
                locatedIdx++;
            }

            RouteEtaVO.Item item = new RouteEtaVO.Item();
            item.setPointId(p.getId());
            item.setPointOrder(p.getPointOrder());
            item.setCustomerName(p.getCustomerName());
            item.setCustomerPhone(p.getCustomerPhone());
            item.setAddress(p.getAddress());
            item.setStatus(p.getStatus());
            item.setEtaTime(eta);
            item.setDistanceFromPrev(legMeters);
            item.setDurationFromPrev(legSeconds);
            items.add(item);
        }
        vo.setPoints(items);
        if (located.size() < remaining.size()) {
            vo.setMessage((remaining.size() - located.size()) + " 个点位未维护坐标，未参与 ETA 估算");
        }
        log.info("ETA 预估完成: routeId={}, remaining={}, degraded={}", routeId, located.size(), response.isDegraded());
        return vo;
    }

    // ==================== ETA 通知（通道未接入，先落库保留能力） ====================

    @Override
    @Transactional
    public Map<String, Object> notifyEta(Long routeId, List<Long> pointIds, String channel) {
        DeliveryRoute route = requireRoute(routeId);
        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        String ch = StringUtils.hasText(channel) ? channel.trim().toUpperCase() : "SMS";
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (RoutePoint p : points) {
            if (pointIds != null && !pointIds.isEmpty() && !pointIds.contains(p.getId())) {
                continue;
            }
            if (p.getEtaTime() == null) {
                skipped++;
                errors.add("第 " + p.getPointOrder() + " 点位无 ETA，请先执行 ETA 预估");
                continue;
            }
            if (!StringUtils.hasText(p.getCustomerPhone())) {
                skipped++;
                errors.add("第 " + p.getPointOrder() + " 点位缺联系电话，无法通知");
                continue;
            }
            DeliveryEtaNotify notify = new DeliveryEtaNotify();
            notify.setRouteId(routeId);
            notify.setRouteCode(route.getRouteCode());
            notify.setPointId(p.getId());
            notify.setPointSeq(p.getPointOrder());
            notify.setCustomerName(p.getCustomerName());
            notify.setCustomerPhone(p.getCustomerPhone());
            notify.setAddress(p.getAddress());
            notify.setEtaTime(p.getEtaTime());
            notify.setChannel(ch);
            notify.setContent(buildContent(route, p));
            notify.setStatus("PENDING");
            notify.setRetryCount(0);
            notify.setCreateByName(SecurityUtils.getCurrentUsername());
            etaNotifyMapper.insert(notify);
            // 投递到消息底座放在事务外（controller 调用 dispatchPending），
            // 避免消息底座异常把业务事务打成 rollback-only
            created++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("routeId", routeId);
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        result.put("channel", ch);
        result.put("channelReady", false);
        result.put("message", "已生成 " + created + " 条待发送通知，已投递到消息中心；"
                + "是否真正发出取决于短信通道配置（sms.enabled / sms.endpoint），未配置时保持「待发送」并按策略重试。");
        log.info("ETA 通知落库: routeId={}, created={}, skipped={}", routeId, created, skipped);
        return result;
    }

    // ==================== 通知台账（通道未接入下的运营闭环） ====================

    private static final List<String> NOTIFY_STATUS = List.of("PENDING", "SENT", "FAILED", "CANCELLED");

    @Override
    public Page<DeliveryEtaNotify> pageNotify(DeliveryEtaNotifyQueryDTO query) {
        DeliveryEtaNotifyQueryDTO q = query != null ? query : new DeliveryEtaNotifyQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();
        LambdaQueryWrapper<DeliveryEtaNotify> w = new LambdaQueryWrapper<>();
        w.eq(q.getRouteId() != null, DeliveryEtaNotify::getRouteId, q.getRouteId());
        if (StringUtils.hasText(q.getStatus())) {
            w.eq(DeliveryEtaNotify::getStatus, q.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(DeliveryEtaNotify::getRouteCode, kw)
                    .or().like(DeliveryEtaNotify::getCustomerName, kw)
                    .or().like(DeliveryEtaNotify::getCustomerPhone, kw));
        }
        w.orderByDesc(DeliveryEtaNotify::getId);
        Page<DeliveryEtaNotify> page = etaNotifyMapper.selectPage(new Page<>(pageNum, pageSize), w);
        fillMessageStatus(page.getRecords());
        return page;
    }

    /** 关联消息底座，回填「发送结果」（0待发送 1发送中 2成功 3失败） */
    private void fillMessageStatus(List<DeliveryEtaNotify> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> ids = rows.stream().map(DeliveryEtaNotify::getMessageId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, Integer> statusMap = sysMessageMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysMessage::getId, m -> m.getSendStatus() == null ? 0 : m.getSendStatus(), (a, b) -> a));
        for (DeliveryEtaNotify row : rows) {
            row.setErrorMsg(row.getErrorMsg());
            if (row.getMessageId() != null) {
                Integer st = statusMap.get(row.getMessageId());
                row.setChannelStatus(st);
            }
        }
    }

    /** 注意：**不加 @Transactional** —— 逐条独立提交，单条投递失败不影响其余记录与业务事务 */
    @Override
    public Map<String, Object> dispatchPending(Long routeId) {
        LambdaQueryWrapper<DeliveryEtaNotify> w = new LambdaQueryWrapper<DeliveryEtaNotify>()
                .eq(DeliveryEtaNotify::getStatus, "PENDING")
                .isNull(DeliveryEtaNotify::getMessageId);
        if (routeId != null) {
            w.eq(DeliveryEtaNotify::getRouteId, routeId);
        }
        List<DeliveryEtaNotify> list = etaNotifyMapper.selectList(w);
        int dispatched = 0;
        for (DeliveryEtaNotify n : list) {
            if (!StringUtils.hasText(n.getCustomerPhone())) {
                continue;
            }
            try {
                Long messageId = messageService.sendSms(null, n.getCustomerPhone(),
                        "配送预计到达通知", n.getContent(), "DELIVERY_ETA", n.getRouteId());
                n.setMessageId(messageId);
                etaNotifyMapper.updateById(n);
                dispatched++;
            } catch (Exception e) {
                log.warn("补投递失败: notifyId={}", n.getId(), e);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", list.size());
        result.put("dispatched", dispatched);
        result.put("message", "已投递 " + dispatched + " 条到消息中心（共扫描 " + list.size() + " 条待发送）；"
                + "是否真正发出取决于短信通道配置（sms.enabled / sms.endpoint）");
        return result;
    }

    @Override
    @Transactional
    public int updateNotifyStatus(List<Long> ids, String status, String errorMsg) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请先选择要操作的通知");
        }
        String target = status == null ? "" : status.trim().toUpperCase();
        if (!NOTIFY_STATUS.contains(target) || "PENDING".equals(target)) {
            throw BusinessException.badRequest("状态仅支持 SENT-已发送 / FAILED-失败 / CANCELLED-作废");
        }
        LambdaUpdateWrapper<DeliveryEtaNotify> w = new LambdaUpdateWrapper<DeliveryEtaNotify>()
                .in(DeliveryEtaNotify::getId, ids)
                .set(DeliveryEtaNotify::getStatus, target)
                .set(DeliveryEtaNotify::getErrorMsg, errorMsg);
        if ("SENT".equals(target)) {
            w.set(DeliveryEtaNotify::getSentTime, LocalDateTime.now());
        }
        int rows = etaNotifyMapper.update(null, w);
        log.info("ETA 通知状态回写: ids={}, status={}, rows={}", ids.size(), target, rows);
        return rows;
    }

    @Override
    public Map<String, Object> sendNotify(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请先选择要发送的通知");
        }
        List<DeliveryEtaNotify> list = etaNotifyMapper.selectBatchIds(ids);
        List<String> skipped = new ArrayList<>();
        for (DeliveryEtaNotify n : list) {
            if (!"PENDING".equals(n.getStatus())) {
                skipped.add("通知 " + n.getId() + " 当前状态为 " + n.getStatus() + "，跳过");
            } else if (!StringUtils.hasText(n.getCustomerPhone())) {
                skipped.add("通知 " + n.getId() + " 缺客户手机号，跳过");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selected", ids.size());
        result.put("sent", 0);
        result.put("skipped", skipped.size());
        result.put("errors", skipped);
        result.put("channelReady", false);
        result.put("message", "短信/微信/APP 发送通道尚未接入：本次未发送任何通知，记录仍为「待发送」。"
                + "接入通道后由发送任务消费本台账；期间可在线下发送后用「标记已发送」回写状态。");
        log.info("ETA 通知发送尝试（通道未接入）: ids={}", ids.size());
        return result;
    }

    private String buildContent(DeliveryRoute route, RoutePoint point) {
        return "【配送通知】尊敬的客户，您的配送单" + String.valueOf(route.getRouteCode() == null ? "" : "（" + route.getRouteCode() + "）")
                + "预计 " + point.getEtaTime().format(TIME_FMT) + " 送达"
                + (StringUtils.hasText(route.getDeliveryPersonName()) ? "，配送员 " + route.getDeliveryPersonName() : "")
                + "，请保持电话畅通。";
    }

    // ==================== 工具 ====================

    private DeliveryRoute requireRoute(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw BusinessException.notFound("配送路线不存在: " + routeId);
        }
        return route;
    }

    private boolean hasCoord(RoutePoint p) {
        return StringUtils.hasText(p.getLatitude()) && StringUtils.hasText(p.getLongitude());
    }

    private RoutePlanRequest.Coordinate toCoordinate(RoutePoint p) {
        return RoutePlanRequest.Coordinate.builder()
                .lat(Double.parseDouble(p.getLatitude()))
                .lng(Double.parseDouble(p.getLongitude()))
                .address(p.getAddress())
                .build();
    }

    /** 起点：优先路线起点坐标；其次最近已处理点位；再次首个待配送点位 */
    private RoutePlanRequest.Coordinate resolveOrigin(DeliveryRoute route, List<RoutePoint> points) {
        if (StringUtils.hasText(route.getStartLatitude()) && StringUtils.hasText(route.getStartLongitude())) {
            return RoutePlanRequest.Coordinate.builder()
                    .lat(Double.parseDouble(route.getStartLatitude()))
                    .lng(Double.parseDouble(route.getStartLongitude()))
                    .address(route.getStartPoint())
                    .build();
        }
        RoutePoint visited = points.stream()
                .filter(p -> PointStatus.DELIVERED.getCode().equals(p.getStatus()) && hasCoord(p))
                .reduce((a, b) -> b)
                .orElse(null);
        if (visited != null) {
            return toCoordinate(visited);
        }
        RoutePoint first = points.stream().filter(this::hasCoord).findFirst()
                .orElseThrow(() -> BusinessException.badRequest("路线缺少起点坐标，且点位无坐标，无法规划"));
        return toCoordinate(first);
    }
}
