package cn.aiedge.erp.delivery.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.entity.RoutePoint;
import cn.aiedge.erp.delivery.enums.PointStatus;
import cn.aiedge.erp.delivery.enums.RouteStatus;
import cn.aiedge.erp.delivery.mapper.DeliveryRouteMapper;
import cn.aiedge.erp.delivery.mapper.RoutePointMapper;
import cn.aiedge.dms.route.dto.GeoFenceDTO;
import cn.aiedge.erp.delivery.route.entity.RouteMaster;
import cn.aiedge.erp.delivery.service.RouteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 配送路线单（执行单）服务实现
 *
 * 红线：本表 {@code erp_delivery_route} 是**执行单**（谁跑、跑到哪、开始/完成/取消），
 *      线路档案主数据见 {@code erp_route}（资料 → 配送管理 → 线路），二者严格分表、不互相替代。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    /** 路线编号号段前缀：PSXL-YYYYMMDD-0001 */
    private static final String NO_PREFIX = "PSXL-";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 允许的状态流转集合 */
    private static final List<String> STARTABLE = List.of(RouteStatus.PLANNING.getCode(), RouteStatus.READY.getCode());
    private static final List<String> TERMINAL = List.of(RouteStatus.COMPLETED.getCode(), RouteStatus.CANCELLED.getCode());

    private final DeliveryRouteMapper routeMapper;
    private final RoutePointMapper pointMapper;
    /** 围栏档案（dms_geo_fence）——全系统单一口径，本模块只读引用 */
    private final cn.aiedge.dms.route.service.GeoFenceService geoFenceService;

    // ==================== 台账 ====================

    @Override
    public Page<DeliveryRouteVO> pageRoutes(DeliveryRouteQueryDTO query) {
        DeliveryRouteQueryDTO q = query != null ? query : new DeliveryRouteQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<DeliveryRoute> entityPage = routeMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(q));
        Page<DeliveryRouteVO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<DeliveryRouteVO> listRoutes(DeliveryRouteQueryDTO query) {
        List<DeliveryRoute> rows = routeMapper.selectList(buildWrapper(query != null ? query : new DeliveryRouteQueryDTO()));
        return rows.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public DeliveryRouteVO getRouteVO(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw BusinessException.notFound("配送路线不存在: " + routeId);
        }
        DeliveryRouteVO vo = toVO(route);
        vo.setPoints(pointMapper.selectByRouteId(routeId).stream().map(this::toPointResult).collect(Collectors.toList()));
        return vo;
    }

    /** 多条件装配（对标《配送路线单开发文档》§3.3 查询条件） */
    private LambdaQueryWrapper<DeliveryRoute> buildWrapper(DeliveryRouteQueryDTO q) {
        LambdaQueryWrapper<DeliveryRoute> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            w.and(x -> x.like(DeliveryRoute::getRouteCode, kw)
                    .or().like(DeliveryRoute::getRouteName, kw)
                    .or().like(DeliveryRoute::getDeliveryPersonName, kw)
                    .or().like(DeliveryRoute::getVehicleNo, kw)
                    .or().like(DeliveryRoute::getStartPoint, kw)
                    .or().like(DeliveryRoute::getEndPoint, kw));
        }
        if (q.getRouteId() != null) {
            w.eq(DeliveryRoute::getRouteId, q.getRouteId());
        }
        if (StringUtils.hasText(q.getDeliveryPersonId())) {
            w.eq(DeliveryRoute::getDeliveryPersonId, q.getDeliveryPersonId().trim());
        }
        if (q.getVehicleId() != null) {
            w.eq(DeliveryRoute::getVehicleId, q.getVehicleId());
        }
        if (StringUtils.hasText(q.getStatus())) {
            List<String> statusList = Arrays.stream(q.getStatus().split(","))
                    .map(String::trim).filter(StringUtils::hasText)
                    .filter(this::isValidStatus)
                    .collect(Collectors.toList());
            if (!statusList.isEmpty()) {
                w.in(DeliveryRoute::getStatus, statusList);
            }
        } else if (q.getShowCancelled() == null || q.getShowCancelled() != 1) {
            // 「显示已取消」默认不勾选 → 台账默认不展示已取消路线
            w.ne(DeliveryRoute::getStatus, RouteStatus.CANCELLED.getCode());
        }
        if (q.getPlanDateStart() != null) {
            w.ge(DeliveryRoute::getPlanDate, q.getPlanDateStart());
        }
        if (q.getPlanDateEnd() != null) {
            w.le(DeliveryRoute::getPlanDate, q.getPlanDateEnd());
        }
        if (q.getCreateTimeStart() != null) {
            w.ge(DeliveryRoute::getCreateTime, q.getCreateTimeStart());
        }
        if (q.getCreateTimeEnd() != null) {
            w.le(DeliveryRoute::getCreateTime, q.getCreateTimeEnd());
        }
        applySort(w, q);
        return w;
    }

    /** 服务端排序（白名单列生效，其余回落默认口径：创建时间倒序） */
    private void applySort(LambdaQueryWrapper<DeliveryRoute> w, DeliveryRouteQueryDTO q) {
        boolean asc = !"desc".equalsIgnoreCase(q.getSortOrder());
        String field = q.getSortField();
        if (field == null) {
            w.orderByDesc(DeliveryRoute::getCreateTime);
            return;
        }
        switch (field) {
            case "routeCode" -> w.orderBy(true, asc, DeliveryRoute::getRouteCode);
            case "planDate" -> w.orderBy(true, asc, DeliveryRoute::getPlanDate);
            case "status" -> w.orderBy(true, asc, DeliveryRoute::getStatus);
            case "totalDistance" -> w.orderBy(true, asc, DeliveryRoute::getTotalDistance);
            case "totalPoints" -> w.orderBy(true, asc, DeliveryRoute::getTotalPoints);
            case "completedPoints" -> w.orderBy(true, asc, DeliveryRoute::getCompletedPoints);
            case "createTime" -> w.orderBy(true, asc, DeliveryRoute::getCreateTime);
            case "startTime" -> w.orderBy(true, asc, DeliveryRoute::getStartTime);
            case "completeTime" -> w.orderBy(true, asc, DeliveryRoute::getCompleteTime);
            default -> w.orderByDesc(DeliveryRoute::getCreateTime);
        }
        w.orderByDesc(DeliveryRoute::getId);
    }

    private boolean isValidStatus(String status) {
        return Arrays.stream(RouteStatus.values()).anyMatch(s -> s.getCode().equals(status));
    }

    // ==================== 建单 / 改单 ====================

    @Override
    @Transactional
    public String nextNo() {
        String prefix = NO_PREFIX + LocalDate.now().format(DAY_FMT) + "-";
        int max = 0;
        for (String code : routeMapper.selectCodesByPrefix(prefix + "%")) {
            if (code == null || code.length() <= prefix.length()) continue;
            try {
                max = Math.max(max, Integer.parseInt(code.substring(prefix.length())));
            } catch (NumberFormatException ignore) {
                // 非号段编号忽略
            }
        }
        return prefix + String.format("%04d", max + 1);
    }

    @Override
    @Transactional
    public DeliveryRouteVO createRoute(DeliveryRouteSaveDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDeliveryPersonId())) {
            throw BusinessException.badRequest("请选择配送员");
        }
        List<RoutePointSaveDTO> pointDtos = normalizePoints(dto.getPoints());

        DeliveryRoute route = new DeliveryRoute();
        String code = StringUtils.hasText(dto.getRouteCode()) ? dto.getRouteCode().trim() : nextNo();
        assertCodeAvailable(code, null);
        route.setRouteCode(code);
        applyMasterSnapshot(route, dto.getRouteId());
        route.setDeliveryPersonId(dto.getDeliveryPersonId().trim());
        route.setDeliveryPersonName(dto.getDeliveryPersonName());
        route.setVehicleId(dto.getVehicleId());
        route.setVehicleNo(dto.getVehicleNo());
        applyFenceBinding(route, dto);
        route.setPlanDate(dto.getPlanDate() != null ? dto.getPlanDate() : LocalDate.now());
        route.setStartPoint(StringUtils.hasText(dto.getStartPoint()) ? dto.getStartPoint().trim() : pointDtos.get(0).getAddress());
        route.setStartLatitude(dto.getStartLatitude());
        route.setStartLongitude(dto.getStartLongitude());
        route.setEndPoint(pointDtos.get(pointDtos.size() - 1).getAddress());
        route.setTotalPoints(pointDtos.size());
        route.setCompletedPoints(0);
        route.setFailedPoints(0);
        route.setStatus(RouteStatus.PLANNING.getCode());
        route.setRemark(dto.getRemark());
        route.setCreateByName(SecurityUtils.getCurrentUsername());
        routeMapper.insert(route);

        savePoints(route.getId(), pointDtos);
        log.info("新增配送路线单: id={}, code={}, points={}", route.getId(), route.getRouteCode(), pointDtos.size());
        return getRouteVO(route.getId());
    }

    @Override
    @Transactional
    public DeliveryRouteVO updateRoute(Long routeId, DeliveryRouteSaveDTO dto) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw BusinessException.notFound("配送路线不存在: " + routeId);
        }
        if (!STARTABLE.contains(route.getStatus())) {
            throw BusinessException.badRequest("仅「规划中 / 待出发」的路线可修改，当前状态："
                    + statusText(route.getStatus()));
        }
        if (dto == null) {
            throw BusinessException.badRequest("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDeliveryPersonId())) {
            throw BusinessException.badRequest("请选择配送员");
        }
        List<RoutePointSaveDTO> pointDtos = normalizePoints(dto.getPoints());

        if (StringUtils.hasText(dto.getRouteCode()) && !dto.getRouteCode().trim().equals(route.getRouteCode())) {
            assertCodeAvailable(dto.getRouteCode().trim(), routeId);
            route.setRouteCode(dto.getRouteCode().trim());
        }
        if (dto.getRouteId() != null) {
            applyMasterSnapshot(route, dto.getRouteId());
        }
        route.setDeliveryPersonId(dto.getDeliveryPersonId().trim());
        route.setDeliveryPersonName(dto.getDeliveryPersonName());
        route.setVehicleId(dto.getVehicleId());
        route.setVehicleNo(dto.getVehicleNo());
        applyFenceBinding(route, dto);
        route.setPlanDate(dto.getPlanDate() != null ? dto.getPlanDate() : route.getPlanDate());
        route.setStartPoint(StringUtils.hasText(dto.getStartPoint()) ? dto.getStartPoint().trim() : pointDtos.get(0).getAddress());
        route.setStartLatitude(dto.getStartLatitude());
        route.setStartLongitude(dto.getStartLongitude());
        route.setEndPoint(pointDtos.get(pointDtos.size() - 1).getAddress());
        route.setRemark(dto.getRemark());
        routeMapper.updateById(route);

        // 点位整体替换（与线路档案子表同口径）
        deletePointsByRoute(routeId);
        savePoints(routeId, pointDtos);
        refreshRouteStats(routeId);
        log.info("修改配送路线单: id={}, code={}, points={}", routeId, route.getRouteCode(), pointDtos.size());
        return getRouteVO(routeId);
    }

    /** 点位入参规范化：地址必填、顺序按数组重排 */
    private List<RoutePointSaveDTO> normalizePoints(List<RoutePointSaveDTO> points) {
        if (points == null || points.isEmpty()) {
            throw BusinessException.badRequest("请至少添加 1 个配送点位");
        }
        List<RoutePointSaveDTO> list = new ArrayList<>();
        int seq = 1;
        for (RoutePointSaveDTO p : points) {
            if (p == null || !StringUtils.hasText(p.getAddress())) {
                throw BusinessException.badRequest("第 " + seq + " 个点位的地址不能为空");
            }
            p.setAddress(p.getAddress().trim());
            p.setPointOrder(seq++);
            list.add(p);
        }
        return list;
    }

    private void savePoints(Long routeId, List<RoutePointSaveDTO> points) {
        for (RoutePointSaveDTO dto : points) {
            RoutePoint point = new RoutePoint();
            point.setRouteId(routeId);
            point.setPointOrder(dto.getPointOrder());
            point.setOrderId(resolveOrderId(dto));
            point.setOrderNo(dto.getOrderNo());
            point.setCustomerName(dto.getCustomerName());
            point.setCustomerPhone(dto.getCustomerPhone());
            point.setAddress(dto.getAddress());
            point.setLatitude(dto.getLatitude());
            point.setLongitude(dto.getLongitude());
            point.setRemark(dto.getRemark());
            point.setStatus(PointStatus.PENDING.getCode());
            pointMapper.insert(point);
        }
    }

    /**
     * 来源单据命名键：`SO:{订单ID}` / `OUT:{出库单ID}`，用于「已入线」去重；
     * 未指明来源时回落到调用方传入的 orderId（保持历史兼容）。
     */
    private String resolveOrderId(RoutePointSaveDTO dto) {
        if (StringUtils.hasText(dto.getSourceType()) && dto.getSourceId() != null) {
            return dto.getSourceType().trim().toUpperCase() + ":" + dto.getSourceId();
        }
        return dto.getOrderId();
    }

    @Override
    @Transactional
    public List<RoutePointResult> appendPoints(Long routeId, List<RoutePointSaveDTO> points) {
        DeliveryRoute route = requireRoute(routeId);
        if (TERMINAL.contains(route.getStatus())) {
            throw BusinessException.badRequest("路线已" + statusText(route.getStatus()) + "，不可再追加点位");
        }
        if (points == null || points.isEmpty()) {
            throw BusinessException.badRequest("没有可追加的点位");
        }
        List<RoutePoint> existing = pointMapper.selectByRouteId(routeId);
        int next = existing.stream()
                .map(p -> p.getPointOrder() == null ? 0 : p.getPointOrder())
                .max(Integer::compareTo).orElse(0) + 1;
        for (RoutePointSaveDTO dto : points) {
            if (!StringUtils.hasText(dto.getAddress())) {
                throw BusinessException.badRequest("点位地址不能为空");
            }
            RoutePoint point = new RoutePoint();
            point.setRouteId(routeId);
            point.setPointOrder(next++);
            point.setOrderId(resolveOrderId(dto));
            point.setOrderNo(dto.getOrderNo());
            point.setCustomerName(dto.getCustomerName());
            point.setCustomerPhone(dto.getCustomerPhone());
            point.setAddress(dto.getAddress().trim());
            point.setLatitude(dto.getLatitude());
            point.setLongitude(dto.getLongitude());
            point.setRemark(dto.getRemark());
            point.setStatus(PointStatus.PENDING.getCode());
            pointMapper.insert(point);
        }
        // 末点位变更 → 同步终点（统计口径同 refreshRouteStats）
        DeliveryRoute updated = routeMapper.selectById(routeId);
        if (updated != null) {
            updated.setEndPoint(pointMapper.selectByRouteId(routeId)
                    .stream().reduce((a, b) -> b).map(RoutePoint::getAddress).orElse(updated.getEndPoint()));
            routeMapper.updateById(updated);
        }
        refreshRouteStats(routeId);
        log.info("追加配送点位: routeId={}, count={}", routeId, points.size());
        return pointMapper.selectByRouteId(routeId).stream().map(this::toPointResult).collect(Collectors.toList());
    }

    private void deletePointsByRoute(Long routeId) {
        pointMapper.delete(new LambdaQueryWrapper<RoutePoint>().eq(RoutePoint::getRouteId, routeId));
    }

    /**
     * 围栏绑定：只落围栏ID + 名称快照。
     * 围栏**档案**由《路线规划》页维护（dms_geo_fence），本页只做绑定引用（研判边界）。
     */
    private void applyFenceBinding(DeliveryRoute route, DeliveryRouteSaveDTO dto) {
        if (dto.getFenceId() == null) {
            if (Boolean.TRUE.equals(dto.getClearFence())) {
                route.setFenceId(null);
                route.setFenceName(null);
            }
            return;
        }
        GeoFenceDTO fence = geoFenceService.detail(dto.getFenceId());
        route.setFenceId(fence.getId());
        route.setFenceName(fence.getFenceName());
        route.setAutoCollect(dto.getAutoCollect() == null ? 1 : dto.getAutoCollect());
    }

    /** 引用线路档案（erp_route）回填名称/类型快照 —— 只读引用，不复制档案本体 */
    private void applyMasterSnapshot(DeliveryRoute route, Long masterRouteId) {
        if (masterRouteId == null) {
            return;
        }
        RouteMaster master = routeMapper.selectRouteMasterById(masterRouteId);
        if (master == null) {
            throw BusinessException.badRequest("线路不存在: " + masterRouteId);
        }
        route.setRouteId(master.getId());
        route.setRouteName(master.getRouteName());
        route.setRouteType(master.getRouteLogistics() != null && master.getRouteLogistics() == 1 ? "LOGISTICS" : "SELF");
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        Long count = routeMapper.selectCount(new LambdaQueryWrapper<DeliveryRoute>()
                .eq(DeliveryRoute::getRouteCode, code)
                .ne(excludeId != null, DeliveryRoute::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("路线编号「" + code + "」已存在");
        }
    }

    // ==================== 状态流转 ====================

    @Override
    @Transactional
    public void startRoute(Long routeId) {
        DeliveryRoute route = requireRoute(routeId);
        if (!STARTABLE.contains(route.getStatus())) {
            throw BusinessException.badRequest("仅「规划中 / 待出发」的路线可开始配送，当前状态："
                    + statusText(route.getStatus()));
        }
        route.setStatus(RouteStatus.IN_PROGRESS.getCode());
        route.setStartTime(LocalDateTime.now());
        routeMapper.updateById(route);

        // 首个未处理点位置为「在途中」
        List<RoutePoint> points = pointMapper.selectByRouteId(routeId);
        points.stream()
                .filter(p -> PointStatus.PENDING.getCode().equals(p.getStatus()))
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus(PointStatus.IN_ROUTE.getCode());
                    pointMapper.updateById(p);
                });
        log.info("开始配送: id={}, code={}", routeId, route.getRouteCode());
    }

    @Override
    @Transactional
    public void completeRoute(Long routeId, String reason) {
        DeliveryRoute route = requireRoute(routeId);
        if (!RouteStatus.IN_PROGRESS.getCode().equals(route.getStatus())) {
            throw BusinessException.badRequest("仅「配送中」的路线可完成，当前状态：" + statusText(route.getStatus()));
        }
        LocalDateTime now = LocalDateTime.now();
        route.setStatus(RouteStatus.COMPLETED.getCode());
        route.setCompleteTime(now);
        if (route.getStartTime() != null) {
            route.setActualDuration((int) Duration.between(route.getStartTime(), now).toMinutes());
        }
        routeMapper.updateById(route);

        // 收口：仍未处理的点位标记为「已跳过」，保证完成进度口径自洽
        markUnfinishedPointsSkipped(routeId);
        refreshRouteStats(routeId);
        log.info("完成配送: id={}, code={}, actualDuration={}min", routeId, route.getRouteCode(), route.getActualDuration());
    }

    @Override
    @Transactional
    public void cancelRoute(Long routeId, String reason) {
        DeliveryRoute route = requireRoute(routeId);
        if (TERMINAL.contains(route.getStatus())) {
            throw BusinessException.badRequest("已完成 / 已取消的路线不可再取消，当前状态：" + statusText(route.getStatus()));
        }
        route.setStatus(RouteStatus.CANCELLED.getCode());
        route.setCancelTime(LocalDateTime.now());
        route.setCancelReason(reason);
        routeMapper.updateById(route);

        markUnfinishedPointsSkipped(routeId);
        refreshRouteStats(routeId);
        log.info("取消配送: id={}, code={}, reason={}", routeId, route.getRouteCode(), reason);
    }

    @Override
    @Transactional
    public Map<String, Object> batchStatus(List<Long> ids, String action, String reason) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请先选择要操作的配送路线");
        }
        String act = action == null ? "" : action.trim().toLowerCase();
        if (!List.of("start", "complete", "cancel").contains(act)) {
            throw BusinessException.badRequest("不支持的操作：" + action);
        }
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (Long id : ids) {
            try {
                switch (act) {
                    case "start" -> startRoute(id);
                    case "complete" -> completeRoute(id, reason);
                    default -> cancelRoute(id, reason);
                }
                success++;
            } catch (Exception e) {
                log.warn("批量操作失败: action={}, id={}", act, id, e);
                errors.add("路线 " + id + "：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", ids.size());
        result.put("success", success);
        result.put("failure", errors.size());
        result.put("errors", errors);
        return result;
    }

    /** 未处理点位（待配送 / 在途中 / 已到达）→ 已跳过 */
    private void markUnfinishedPointsSkipped(Long routeId) {
        pointMapper.update(null, new LambdaUpdateWrapper<RoutePoint>()
                .eq(RoutePoint::getRouteId, routeId)
                .in(RoutePoint::getStatus, List.of(PointStatus.PENDING.getCode(),
                        PointStatus.IN_ROUTE.getCode(), PointStatus.ARRIVED.getCode()))
                .set(RoutePoint::getStatus, PointStatus.SKIPPED.getCode())
                .set(RoutePoint::getUpdateTime, LocalDateTime.now()));
    }

    // ==================== 点位签收 ====================

    @Override
    @Transactional
    public void signPoint(Long routeId, Long pointId, RoutePointSignDTO dto) {
        DeliveryRoute route = requireRoute(routeId);
        if (TERMINAL.contains(route.getStatus())) {
            throw BusinessException.badRequest("路线已" + statusText(route.getStatus()) + "，不可再操作点位");
        }
        RoutePoint point = pointMapper.selectById(pointId);
        if (point == null || !Objects.equals(point.getRouteId(), routeId)) {
            throw BusinessException.notFound("配送点位不存在: " + pointId);
        }
        String status = dto == null || !StringUtils.hasText(dto.getStatus())
                ? PointStatus.DELIVERED.getCode() : dto.getStatus().trim().toUpperCase();
        boolean valid = Arrays.stream(PointStatus.values()).anyMatch(s -> s.getCode().equals(status))
                && !PointStatus.SKIPPED.getCode().equals(status);
        if (!valid) {
            throw BusinessException.badRequest("点位状态取值非法：" + status);
        }
        if (PointStatus.FAILED.getCode().equals(status) && !StringUtils.hasText(dto.getFailReason())) {
            throw BusinessException.badRequest("配送失败必须填写失败原因");
        }
        if (PointStatus.DELIVERED.getCode().equals(status) && !StringUtils.hasText(dto.getSignee())) {
            throw BusinessException.badRequest("送达签收必须填写签收人");
        }

        LocalDateTime now = LocalDateTime.now();
        point.setStatus(status);
        switch (status) {
            case "ARRIVED" -> point.setArriveTime(now);
            case "DELIVERED" -> {
                point.setSignTime(now);
                point.setLeaveTime(now);
                if (StringUtils.hasText(dto.getSignee())) {
                    point.setSignee(dto.getSignee().trim());
                }
                point.setFailReason(null);
            }
            case "FAILED" -> {
                point.setFailReason(dto.getFailReason().trim());
                point.setLeaveTime(now);
            }
            default -> { /* IN_ROUTE：仅改状态 */ }
        }
        if (dto != null && dto.getRemark() != null) {
            point.setRemark(dto.getRemark());
        }
        pointMapper.updateById(point);
        refreshRouteStats(routeId);
        log.info("点位签收: routeId={}, pointId={}, status={}", routeId, pointId, status);
    }

    @Override
    public DeliveryRoute getActiveRouteByPerson(String deliveryPersonId) {
        return routeMapper.selectActiveRouteByPerson(deliveryPersonId);
    }

    // ==================== 统计口径 ====================

    /** 重算总点位 / 已送达 / 失败数（完成进度 = 已送达 ÷ 总点位） */
    private void refreshRouteStats(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            return;
        }
        route.setTotalPoints(pointMapper.countByRoute(routeId));
        route.setCompletedPoints(pointMapper.countDeliveredByRoute(routeId));
        route.setFailedPoints(pointMapper.countFailedByRoute(routeId));
        routeMapper.updateById(route);
    }

    private DeliveryRoute requireRoute(Long routeId) {
        DeliveryRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw BusinessException.notFound("配送路线不存在: " + routeId);
        }
        return route;
    }

    // ==================== 映射 ====================

    private DeliveryRouteVO toVO(DeliveryRoute e) {
        DeliveryRouteVO vo = new DeliveryRouteVO();
        vo.setId(e.getId());
        vo.setRouteCode(e.getRouteCode());
        vo.setRouteId(e.getRouteId());
        vo.setRouteName(e.getRouteName());
        vo.setRouteType(e.getRouteType());
        vo.setRouteTypeText(routeTypeText(e.getRouteType()));
        vo.setDeliveryPersonId(e.getDeliveryPersonId());
        vo.setDeliveryPersonName(e.getDeliveryPersonName());
        vo.setVehicleId(e.getVehicleId());
        vo.setVehicleNo(e.getVehicleNo());
        vo.setFenceId(e.getFenceId());
        vo.setFenceName(e.getFenceName());
        vo.setAutoCollect(e.getAutoCollect() == null ? 0 : e.getAutoCollect());
        vo.setPlanDate(e.getPlanDate());
        vo.setTotalPoints(e.getTotalPoints());
        vo.setCompletedPoints(e.getCompletedPoints());
        vo.setFailedPoints(e.getFailedPoints());
        vo.setProgress((e.getCompletedPoints() == null ? 0 : e.getCompletedPoints())
                + " / " + (e.getTotalPoints() == null ? 0 : e.getTotalPoints()));
        vo.setStartPoint(e.getStartPoint());
        vo.setEndPoint(e.getEndPoint());
        vo.setTotalDistance(e.getTotalDistance());
        vo.setTotalDuration(e.getTotalDuration());
        vo.setActualDuration(e.getActualDuration());
        vo.setStatus(e.getStatus());
        vo.setStatusText(statusText(e.getStatus()));
        vo.setStartTime(e.getStartTime());
        vo.setCompleteTime(e.getCompleteTime());
        vo.setCancelTime(e.getCancelTime());
        vo.setCancelReason(e.getCancelReason());
        vo.setRemark(e.getRemark());
        vo.setCreateBy(e.getCreateBy());
        vo.setCreateByName(e.getCreateByName());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private RoutePointResult toPointResult(RoutePoint p) {
        RoutePointResult r = new RoutePointResult();
        r.setPointId(p.getId());
        r.setPointOrder(p.getPointOrder());
        r.setOrderId(p.getOrderId());
        r.setOrderNo(p.getOrderNo());
        r.setCustomerName(p.getCustomerName());
        r.setCustomerPhone(p.getCustomerPhone());
        r.setAddress(p.getAddress());
        r.setLatitude(p.getLatitude());
        r.setLongitude(p.getLongitude());
        r.setDistanceFromPrev(p.getDistanceFromPrev());
        r.setDurationFromPrev(p.getDurationFromPrev());
        r.setStatus(p.getStatus());
        r.setArriveTime(p.getArriveTime());
        r.setLeaveTime(p.getLeaveTime());
        r.setSignee(p.getSignee());
        r.setSignTime(p.getSignTime());
        r.setFailReason(p.getFailReason());
        r.setEtaTime(p.getEtaTime());
        r.setExpedited(p.getExpedited() == null ? 0 : p.getExpedited());
        r.setSourceType(sourceTypeOf(p.getOrderId()));
        r.setSourceId(sourceIdOf(p.getOrderId()));
        r.setRemark(p.getRemark());
        return r;
    }

    /** 来源单据命名键 → 来源单据ID（解析失败返回 null） */
    private Long sourceIdOf(String orderId) {
        if (orderId == null || !orderId.contains(":")) {
            return null;
        }
        try {
            return Long.valueOf(orderId.substring(orderId.indexOf(':') + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 来源单据命名键 → 来源类型：SO:{id} / OUT:{id} / 其它视为手工 */
    private String sourceTypeOf(String orderId) {
        if (orderId == null) {
            return "MANUAL";
        }
        if (orderId.startsWith("SO:")) return "SO";
        if (orderId.startsWith("OUT:")) return "OUT";
        return "MANUAL";
    }

    private String routeTypeText(String type) {
        if ("SELF".equalsIgnoreCase(type)) return "自配";
        if ("LOGISTICS".equalsIgnoreCase(type)) return "物流";
        return "";
    }

    private String statusText(String status) {
        return Arrays.stream(RouteStatus.values())
                .filter(s -> s.getCode().equals(status))
                .map(RouteStatus::getName)
                .findFirst()
                .orElse(status == null ? "-" : status);
    }
}
