package cn.aiedge.dms.dispatch.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.dispatch.dto.RiderOptionVO;
import cn.aiedge.dms.dispatch.dto.RouteOptionVO;
import cn.aiedge.dms.dispatch.dto.RouteRiderQuery;
import cn.aiedge.dms.dispatch.dto.RouteRiderSaveDTO;
import cn.aiedge.dms.dispatch.dto.RouteRiderVO;
import cn.aiedge.dms.dispatch.entity.DmsRouteRider;
import cn.aiedge.dms.dispatch.mapper.DmsRouteRiderMapper;
import cn.aiedge.dms.dispatch.mapper.RouteLookupMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 区域分包绑定服务（《智能调度开发文档》§3.4 派单策略「区域分包」）
 *
 * <p>维护「线路档案 × 配送员」绑定：一个线路可绑多名配送员（按 priority 排序），
 * 一名配送员可服务多条线路。`DispatchService` 在 AREA 策略下用 {@link #boundRiderIdsByRoute} 判定命中。</p>
 *
 * <p>红线：线路主数据在 `erp_route`（《资料 → 配送管理 → 线路》），本服务只读引用、只写绑定关系。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteRiderService {

    private final DmsRouteRiderMapper routeRiderMapper;
    private final RouteLookupMapper routeLookupMapper;
    private final DmsRiderMapper riderMapper;

    private static final int OPTION_LIMIT = 200;

    // ==================== 查询 ====================

    /** 绑定列表（分页） */
    public Page<RouteRiderVO> page(RouteRiderQuery query) {
        RouteRiderQuery q = query == null ? new RouteRiderQuery() : query;
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : Math.min(q.getPageSize(), 200);

        LambdaQueryWrapper<DmsRouteRider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(q.getRouteId() != null, DmsRouteRider::getRouteId, q.getRouteId());
        wrapper.eq(q.getRiderId() != null, DmsRouteRider::getRiderId, q.getRiderId());
        wrapper.eq(q.getStatus() != null, DmsRouteRider::getStatus, q.getStatus());
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(DmsRouteRider::getRouteName, kw)
                    .or().like(DmsRouteRider::getRouteCode, kw)
                    .or().like(DmsRouteRider::getRiderName, kw));
        }
        wrapper.orderByAsc(DmsRouteRider::getRouteName)
                .orderByAsc(DmsRouteRider::getPriority)
                .orderByDesc(DmsRouteRider::getId);

        Page<DmsRouteRider> rows = routeRiderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<RouteRiderVO> result = new Page<>(pageNum, pageSize, rows.getTotal());
        result.setRecords(toVOList(rows.getRecords()));
        return result;
    }

    /** 线路选择器（线路档案，只读） */
    public List<RouteOptionVO> routeOptions(String keyword) {
        return routeLookupMapper.searchRoutes(StringUtils.hasText(keyword) ? keyword.trim() : null, OPTION_LIMIT);
    }

    /** 配送员选择器（非休息且未删除） */
    public List<RiderOptionVO> riderOptions(String keyword) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(DmsRider::getRealName, kw)
                    .or().like(DmsRider::getPhone, kw)
                    .or().like(DmsRider::getRiderNo, kw));
        }
        wrapper.orderByAsc(DmsRider::getRealName).last("LIMIT " + OPTION_LIMIT);
        return riderMapper.selectList(wrapper).stream().map(r -> {
            RiderOptionVO vo = new RiderOptionVO();
            vo.setId(r.getId());
            vo.setRiderNo(r.getRiderNo());
            vo.setRealName(r.getRealName());
            vo.setPhone(r.getPhone());
            vo.setStatus(r.getStatus());
            vo.setVerifyStatus(r.getVerifyStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 单线路已绑定的配送员ID（仅启用绑定）
     *
     * @param routeId 线路档案ID
     * @return 启用的配送员ID集合（无绑定时为空集）
     */
    public Set<Long> boundRiderIds(Long routeId) {
        if (routeId == null) {
            return Set.of();
        }
        return boundRiderIdsByRoute(List.of(routeId)).getOrDefault(routeId, Set.of());
    }

    /**
     * 批量解析「线路 → 启用绑定的配送员」（一次查询，供预览/自动调度打分使用）
     *
     * @param routeIds 线路档案ID集合
     * @return routeId → riderId 集合（无绑定的线路不出现）
     */
    public Map<Long, Set<Long>> boundRiderIdsByRoute(Collection<Long> routeIds) {
        Map<Long, Set<Long>> map = new LinkedHashMap<>();
        if (routeIds == null || routeIds.isEmpty()) {
            return map;
        }
        Set<Long> ids = routeIds.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return map;
        }
        List<DmsRouteRider> rows = routeRiderMapper.selectList(new LambdaQueryWrapper<DmsRouteRider>()
                .in(DmsRouteRider::getRouteId, ids)
                .eq(DmsRouteRider::getStatus, DmsRouteRider.STATUS_ENABLED)
                .orderByAsc(DmsRouteRider::getPriority));
        for (DmsRouteRider row : rows) {
            map.computeIfAbsent(row.getRouteId(), k -> new LinkedHashSet<>()).add(row.getRiderId());
        }
        return map;
    }

    /** 线路名称（区域分包命中规则文案用；不可用时回落 null） */
    public Map<Long, String> routeNames(Collection<Long> routeIds) {
        Map<Long, String> map = new LinkedHashMap<>();
        if (routeIds == null) {
            return map;
        }
        for (Long id : routeIds.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))) {
            RouteOptionVO route = routeLookupMapper.findRoute(id);
            if (route != null) {
                map.put(id, route.getRouteName());
            }
        }
        return map;
    }

    // ==================== 写入 ====================

    @Transactional(rollbackFor = Exception.class)
    public RouteRiderVO create(RouteRiderSaveDTO dto) {
        DmsRouteRider entity = new DmsRouteRider();
        applyToEntity(entity, dto);
        assertNotDuplicated(entity.getRouteId(), entity.getRiderId(), null);
        entity.setTenantId(SecurityUtils.getCurrentTenantId());
        routeRiderMapper.insert(entity);
        log.info("区域分包绑定已创建: routeId={}, riderId={}", entity.getRouteId(), entity.getRiderId());
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public RouteRiderVO update(Long id, RouteRiderSaveDTO dto) {
        DmsRouteRider entity = require(id);
        applyToEntity(entity, dto);
        assertNotDuplicated(entity.getRouteId(), entity.getRiderId(), id);
        routeRiderMapper.updateById(entity);
        log.info("区域分包绑定已更新: id={}, routeId={}, riderId={}", id, entity.getRouteId(), entity.getRiderId());
        return toVO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != DmsRouteRider.STATUS_ENABLED && status != DmsRouteRider.STATUS_DISABLED)) {
            throw new DmsBusinessException("状态只能是 1-启用 或 0-停用");
        }
        DmsRouteRider entity = require(id);
        entity.setStatus(status);
        routeRiderMapper.updateById(entity);
    }

    /** 删除绑定（物理语义：逻辑删除，软删后同线路同配送员可重新绑定） */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        routeRiderMapper.deleteById(id);
        log.info("区域分包绑定已删除: id={}", id);
    }

    // ==================== 私有辅助 ====================

    private DmsRouteRider require(Long id) {
        DmsRouteRider entity = id == null ? null : routeRiderMapper.selectById(id);
        if (entity == null) {
            throw new DmsBusinessException("绑定关系不存在或已删除: " + id);
        }
        return entity;
    }

    /** 入参落库：线路/配送员必须来自档案（服务端回填名称快照，不信任页面传名） */
    private void applyToEntity(DmsRouteRider entity, RouteRiderSaveDTO dto) {
        if (dto == null || dto.getRouteId() == null) {
            throw new DmsBusinessException("请选择线路");
        }
        if (dto.getRiderId() == null) {
            throw new DmsBusinessException("请选择配送员");
        }
        RouteOptionVO route = routeLookupMapper.findRoute(dto.getRouteId());
        if (route == null) {
            throw new DmsBusinessException("线路档案不存在或已删除: " + dto.getRouteId());
        }
        DmsRider rider = riderMapper.selectById(dto.getRiderId());
        if (rider == null) {
            throw new DmsBusinessException("配送员不存在: " + dto.getRiderId());
        }
        entity.setRouteId(route.getId());
        entity.setRouteCode(route.getRouteCode());
        entity.setRouteName(route.getRouteName());
        entity.setRiderId(rider.getId());
        entity.setRiderName(rider.getRealName());
        entity.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        entity.setStatus(dto.getStatus() == null ? DmsRouteRider.STATUS_ENABLED : dto.getStatus());
        entity.setRemark(dto.getRemark());
    }

    private void assertNotDuplicated(Long routeId, Long riderId, Long excludeId) {
        List<DmsRouteRider> rows = routeRiderMapper.selectList(new LambdaQueryWrapper<DmsRouteRider>()
                .eq(DmsRouteRider::getRouteId, routeId)
                .eq(DmsRouteRider::getRiderId, riderId));
        boolean conflict = rows.stream().anyMatch(r -> excludeId == null || !excludeId.equals(r.getId()));
        if (conflict) {
            throw new DmsBusinessException("该配送员已绑定此线路，请勿重复添加");
        }
    }

    private List<RouteRiderVO> toVOList(List<DmsRouteRider> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> riderIds = rows.stream().map(DmsRouteRider::getRiderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, DmsRider> riderMap = new LinkedHashMap<>();
        for (DmsRider rider : riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                .in(DmsRider::getId, riderIds))) {
            riderMap.put(rider.getId(), rider);
        }
        List<RouteRiderVO> list = new ArrayList<>();
        for (DmsRouteRider row : rows) {
            RouteRiderVO vo = toVO(row);
            DmsRider rider = riderMap.get(row.getRiderId());
            vo.setRiderPhone(rider == null ? null : rider.getPhone());
            vo.setRiderStatus(rider == null ? null : rider.getStatus());
            list.add(vo);
        }
        return list;
    }

    private RouteRiderVO toVO(DmsRouteRider entity) {
        RouteRiderVO vo = new RouteRiderVO();
        vo.setId(entity.getId());
        vo.setRouteId(entity.getRouteId());
        vo.setRouteCode(entity.getRouteCode());
        vo.setRouteName(entity.getRouteName());
        vo.setRiderId(entity.getRiderId());
        vo.setRiderName(entity.getRiderName());
        vo.setPriority(entity.getPriority());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
