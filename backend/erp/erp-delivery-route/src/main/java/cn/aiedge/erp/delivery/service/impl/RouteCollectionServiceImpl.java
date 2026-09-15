package cn.aiedge.erp.delivery.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.route.dto.FenceCheckRequest;
import cn.aiedge.dms.route.dto.FenceCheckResponse;
import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.entity.RoutePoint;
import cn.aiedge.erp.delivery.enums.RouteStatus;
import cn.aiedge.erp.delivery.mapper.CustomerGeoMapper;
import cn.aiedge.erp.delivery.mapper.DeliveryDemandMapper;
import cn.aiedge.erp.delivery.mapper.DeliveryRouteMapper;
import cn.aiedge.erp.delivery.mapper.RoutePointMapper;
import cn.aiedge.erp.delivery.service.RouteCollectionService;
import cn.aiedge.erp.delivery.service.RoutePlanningService;
import cn.aiedge.erp.delivery.service.RouteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送需求归集（围栏自动归集 + 手动添加）
 *
 * 规则（对应需求①②）：
 *   · 自动：销售出库单（已发货待配送）/ 销售订单（待发货·部分发货）的**客户配送坐标**
 *     落入路线单绑定的**电子围栏**内 → 自动追加为该路线单点位（已入线的不重复归集）；
 *   · 手动：不受围栏限制，运营可把围栏外订单**人工补进**指定路线单。
 *
 * 判定口径复用 dms {@code GeoFenceService}（圆形/多边形单一口径，与《路线规划》围栏档案一致），
 * 不对围栏几何做第二次实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCollectionServiceImpl implements RouteCollectionService {

    private static final String RESULT_ADDED = "ADDED";
    private static final String RESULT_PREVIEW = "PREVIEW";
    private static final String RESULT_ALREADY = "ALREADY_ON_ROUTE";
    private static final String RESULT_NO_COORD = "NO_COORD";
    private static final String RESULT_OUTSIDE = "OUTSIDE";

    private final DeliveryRouteMapper routeMapper;
    private final RoutePointMapper pointMapper;
    private final DeliveryDemandMapper demandMapper;
    private final CustomerGeoMapper customerGeoMapper;
    private final RouteService routeService;
    private final RoutePlanningService planningService;

    /** dms 围栏校验（全系统单一口径） */
    private final cn.aiedge.dms.route.service.GeoFenceService geoFenceService;

    // ==================== 围栏自动归集 ====================

    @Override
    @Transactional
    public AutoCollectResultVO autoCollect(AutoCollectQueryDTO query) {
        AutoCollectQueryDTO q = query != null ? query : new AutoCollectQueryDTO();
        boolean dryRun = Boolean.TRUE.equals(q.getDryRun());
        int limit = q.getLimit() == null || q.getLimit() < 1 ? 200 : Math.min(q.getLimit(), 1000);

        AutoCollectResultVO result = new AutoCollectResultVO();
        result.setDryRun(dryRun);

        List<DeliveryRoute> targets = resolveTargets(q.getRouteId());
        if (targets.isEmpty()) {
            result.getMessages().add(q.getRouteId() != null
                    ? "该路线单未绑定围栏，或状态不是「规划中/待出发」"
                    : "没有「规划中/待出发 + 自动归集开启 + 已绑定围栏」的路线单");
            return result;
        }

        List<DeliveryDemandVO> demands = loadDemands(q);
        result.setScanned(demands.size());
        result.getMessages().add("扫描到 " + demands.size() + " 条未入线的配送需求；归集目标路线单 "
                + targets.size() + " 条");

        for (DeliveryDemandVO demand : demands) {
            if (demand.getLatitude() == null || demand.getLongitude() == null) {
                result.setMissingCoordinate(result.getMissingCoordinate() + 1);
                result.getItems().add(item(null, null, demand, RESULT_NO_COORD, "客户未维护配送坐标，无法围栏判定"));
                continue;
            }
            DeliveryRoute hit = matchFence(demand, targets);
            if (hit == null) {
                result.setOutsideFence(result.getOutsideFence() + 1);
                result.getItems().add(item(null, null, demand, RESULT_OUTSIDE, "不在任何目标路线单的围栏内"));
                continue;
            }
            result.setMatched(result.getMatched() + 1);
            if (dryRun) {
                result.setAdded(result.getAdded() + 1);
                result.getItems().add(item(hit, hit.getFenceName(), demand, RESULT_PREVIEW, "命中围栏（预览，未落库）"));
                continue;
            }
            try {
                routeService.appendPoints(hit.getId(), List.of(toPoint(demand)));
                result.setAdded(result.getAdded() + 1);
                result.getItems().add(item(hit, hit.getFenceName(), demand, RESULT_ADDED, "已加入路线单"));
            } catch (Exception e) {
                log.warn("围栏归集单条失败: bill={}", demand.getBillNo(), e);
                result.getItems().add(item(hit, hit.getFenceName(), demand, RESULT_OUTSIDE,
                        "入线失败：" + e.getMessage()));
            }
        }
        log.info("围栏自动归集: dryRun={}, scanned={}, matched={}, added={}", dryRun,
                result.getScanned(), result.getMatched(), result.getAdded());
        return result;
    }

    /** 归集目标：指定路线单，或全部「规划中/待出发 + autoCollect=1 + 有围栏」的路线单 */
    private List<DeliveryRoute> resolveTargets(Long routeId) {
        if (routeId != null) {
            DeliveryRoute route = routeMapper.selectById(routeId);
            if (route == null) {
                throw BusinessException.notFound("配送路线不存在: " + routeId);
            }
            if (route.getFenceId() == null) {
                return List.of();
            }
            if (!isCollectable(route)) {
                return List.of();
            }
            return List.of(route);
        }
        return routeMapper.selectList(new LambdaQueryWrapper<DeliveryRoute>()
                .isNotNull(DeliveryRoute::getFenceId)
                .eq(DeliveryRoute::getAutoCollect, 1)
                .in(DeliveryRoute::getStatus, List.of(RouteStatus.PLANNING.getCode(), RouteStatus.READY.getCode()))
                .orderByAsc(DeliveryRoute::getId));
    }

    private boolean isCollectable(DeliveryRoute route) {
        return RouteStatus.PLANNING.getCode().equals(route.getStatus())
                || RouteStatus.READY.getCode().equals(route.getStatus());
    }

    /** 逐条判定是否落在某个目标路线单的围栏内 */
    private DeliveryRoute matchFence(DeliveryDemandVO demand, List<DeliveryRoute> targets) {
        for (DeliveryRoute target : targets) {
            if (target.getFenceId() == null) {
                continue;
            }
            FenceCheckResponse check = geoFenceService.check(FenceCheckRequest.builder()
                    .lat(demand.getLatitude().doubleValue())
                    .lng(demand.getLongitude().doubleValue())
                    .fenceId(target.getFenceId())
                    .build());
            demand.setInsideFence(Boolean.TRUE.equals(check.getInside()));
            demand.setDistanceMeters(check.getDistanceMeters());
            if (Boolean.TRUE.equals(check.getInside())) {
                return target;
            }
        }
        return null;
    }

    private List<DeliveryDemandVO> loadDemands(AutoCollectQueryDTO q) {
        String source = q.getSource() == null ? "BOTH" : q.getSource().trim().toUpperCase();
        int limit = q.getLimit() == null || q.getLimit() < 1 ? 200 : Math.min(q.getLimit(), 1000);
        String kw = StringUtils.hasText(q.getKeyword()) ? q.getKeyword().trim() : null;
        List<DeliveryDemandVO> list = new ArrayList<>();
        if (!"SO".equals(source)) {
            list.addAll(demandMapper.selectOutboundDemands(currentTenantId(), kw, limit));
        }
        if (!"OUT".equals(source)) {
            list.addAll(demandMapper.selectOrderDemands(currentTenantId(), kw, limit));
        }
        return list;
    }

    // ==================== 手动添加（不受围栏限制） ====================

    @Override
    @Transactional
    public Map<String, Object> addPoints(Long routeId, AddRoutePointsDTO dto) {
        if (dto == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw BusinessException.badRequest("请选择要添加的配送单据");
        }
        List<RoutePointSaveDTO> payload = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (AddRoutePointsDTO.Item it : dto.getItems()) {
            DeliveryDemandVO demand = loadDemand(it.getSourceType(), it.getSourceId());
            if (demand == null) {
                errors.add(safeType(it.getSourceType()) + " 单据不存在：" + it.getSourceId());
                continue;
            }
            if (routeContainsDemand(routeId, demand)) {
                errors.add("单据 " + demand.getBillNo() + " 已在本路线单中");
                continue;
            }
            RoutePointSaveDTO p = toPoint(demand);
            if (StringUtils.hasText(it.getAddress())) {
                p.setAddress(it.getAddress().trim());
            }
            if (StringUtils.hasText(it.getReceiverName())) {
                p.setCustomerName(it.getReceiverName().trim());
            }
            if (StringUtils.hasText(it.getReceiverPhone())) {
                p.setCustomerPhone(it.getReceiverPhone().trim());
            }
            payload.add(p);
        }
        if (payload.isEmpty()) {
            throw BusinessException.badRequest("没有可添加的点位：" + String.join("；", errors));
        }

        routeService.appendPoints(routeId, payload);

        boolean replan = dto.getReplan() != null && dto.getReplan();
        Map<String, Object> planResult = null;
        if (replan) {
            // 点位若缺坐标，规划接口会明确报错并回滚不了已追加的点位（追加已完成），此处捕获后降级提示
            try {
                planResult = planningService.planOrder(routeId);
            } catch (Exception e) {
                log.warn("手动添加后自动规划失败: routeId={}", routeId, e);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routeId", routeId);
        result.put("added", payload.size());
        result.put("errors", errors);
        result.put("replanned", planResult != null);
        result.put("plan", planResult);
        result.put("message", "已添加 " + payload.size() + " 个配送点位"
                + (payload.size() > 0 && planResult == null && replan ? "（自动规划未执行：点位缺坐标）" : ""));
        log.info("手动添加配送点位: routeId={}, added={}", routeId, payload.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> saveCustomerGeo(Long customerId, java.math.BigDecimal latitude, java.math.BigDecimal longitude) {
        if (customerId == null) {
            throw BusinessException.badRequest("客户ID不能为空");
        }
        if (latitude == null || longitude == null) {
            throw BusinessException.badRequest("经纬度不能为空");
        }
        if (latitude.doubleValue() < -90 || latitude.doubleValue() > 90
                || longitude.doubleValue() < -180 || longitude.doubleValue() > 180) {
            throw BusinessException.badRequest("经纬度超出合法范围（纬度 -90~90，经度 -180~180）");
        }
        int updated = customerGeoMapper.updateGeo(customerId, currentTenantId(), latitude, longitude);
        if (updated == 0) {
            throw BusinessException.notFound("客户不存在: " + customerId);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerId", customerId);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("message", "客户配送坐标已保存，可重新执行围栏归集");
        log.info("客户配送坐标已补录: customerId={}, lat={}, lng={}", customerId, latitude, longitude);
        return result;
    }

    @Override
    public List<Map<String, Object>> listCustomerGeos(Integer limit) {
        return customerGeoMapper.selectCustomerGeos(currentTenantId(), limit == null || limit < 1 ? 200 : Math.min(limit, 1000));
    }

    @Override
    public List<DeliveryDemandVO> listDemands(String source, String keyword, Integer limit) {
        AutoCollectQueryDTO q = new AutoCollectQueryDTO();
        q.setSource(source);
        q.setKeyword(keyword);
        q.setLimit(limit == null ? 200 : limit);
        return loadDemands(q);
    }

    // ==================== 工具 ====================

    /** 当前租户ID（缺失时回落 1，与系统租户默认口径一致） */
    private Long currentTenantId() {
        Long tid = SecurityUtils.getCurrentTenantId();
        return tid == null ? 1L : tid;
    }

    private DeliveryDemandVO loadDemand(String sourceType, Long sourceId) {
        if (sourceId == null) {
            return null;
        }
        return "SO".equalsIgnoreCase(safeType(sourceType))
                ? demandMapper.selectOrderDemand(currentTenantId(), sourceId)
                : demandMapper.selectOutboundDemand(currentTenantId(), sourceId);
    }

    private String safeType(String sourceType) {
        return sourceType == null ? "OUT" : sourceType.trim().toUpperCase();
    }

    /** 同路线单内是否已存在该来源单据（点位来源键 order_id = SO:{id} / OUT:{id}） */
    private boolean routeContainsDemand(Long routeId, DeliveryDemandVO demand) {
        String key = demand.getSourceType() + ":" + demand.getSourceId();
        Long count = pointMapper.selectCount(new LambdaQueryWrapper<RoutePoint>()
                .eq(RoutePoint::getRouteId, routeId)
                .eq(RoutePoint::getOrderId, key));
        return count != null && count > 0;
    }

    /** 配送需求 → 点位入参（坐标来自客户主数据，供围栏判定与地图规划使用） */
    private RoutePointSaveDTO toPoint(DeliveryDemandVO demand) {
        RoutePointSaveDTO p = new RoutePointSaveDTO();
        p.setSourceType(demand.getSourceType());
        p.setSourceId(demand.getSourceId());
        p.setOrderNo(demand.getBillNo());
        p.setCustomerName(StringUtils.hasText(demand.getReceiverName())
                ? demand.getReceiverName() : demand.getCustomerName());
        p.setCustomerPhone(demand.getReceiverPhone());
        p.setAddress(demand.getAddress());
        p.setLatitude(demand.getLatitude() == null ? null : demand.getLatitude().toPlainString());
        p.setLongitude(demand.getLongitude() == null ? null : demand.getLongitude().toPlainString());
        p.setRemark("来源：" + demand.getBillNo() + (StringUtils.hasText(demand.getCustomerName())
                ? "（" + demand.getCustomerName() + "）" : ""));
        return p;
    }

    private AutoCollectResultVO.Item item(DeliveryRoute route, String fenceName,
                                          DeliveryDemandVO demand, String result, String reason) {
        AutoCollectResultVO.Item it = new AutoCollectResultVO.Item();
        it.setRouteId(route == null ? null : route.getId());
        it.setRouteCode(route == null ? null : route.getRouteCode());
        it.setFenceName(fenceName);
        it.setSourceType(demand.getSourceType());
        it.setSourceId(demand.getSourceId());
        it.setBillNo(demand.getBillNo());
        it.setCustomerName(demand.getCustomerName());
        it.setAddress(demand.getAddress());
        it.setResult(result);
        it.setReason(reason);
        return it;
    }
}
