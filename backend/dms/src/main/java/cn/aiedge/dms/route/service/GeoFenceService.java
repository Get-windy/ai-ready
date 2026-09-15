package cn.aiedge.dms.route.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.util.GeoUtils;
import cn.aiedge.dms.route.dto.*;
import cn.aiedge.dms.route.entity.GeoFence;
import cn.aiedge.dms.route.mapper.GeoFenceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 电子围栏服务
 *
 * 统一承载围栏档案 CRUD 与围栏校验（圆形 + 多边形），
 * 是全系统围栏判定的单一口径（《路线规划》围栏管理、《调度任务》到点校验均走此处）。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoFenceService {

    private static final String TYPE_CIRCLE = "CIRCLE";
    private static final String TYPE_POLYGON = "POLYGON";
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String CODE_PREFIX = "WL";

    /** 边界容差（米）：落在边界上的点按「在围栏内」处理，规避射线法临界抖动 */
    private static final double BOUNDARY_TOLERANCE_METERS = 0.01d;

    private final GeoFenceMapper geoFenceMapper;

    // ==================== 查询 ====================

    public Page<GeoFenceDTO> page(GeoFenceQueryDTO query) {
        GeoFenceQueryDTO q = query != null ? query : new GeoFenceQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<GeoFence> entityPage = geoFenceMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(q));
        Page<GeoFenceDTO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toDTO).toList());
        return result;
    }

    public GeoFenceDTO detail(Long id) {
        GeoFence entity = requireFence(id);
        return toDTO(entity);
    }

    /** 启用围栏下拉（供《配送路线单》《渠道管理》等绑定选择） */
    public List<GeoFenceDTO> options() {
        return geoFenceMapper.selectList(new LambdaQueryWrapper<GeoFence>()
                        .eq(GeoFence::getStatus, STATUS_ENABLED)
                        .orderByAsc(GeoFence::getFenceCode))
                .stream().map(this::toDTO).toList();
    }

    /** 自动生成围栏编码：WL001、WL002 …（同租户内不重号） */
    public String nextCode() {
        Set<String> taken = geoFenceMapper.selectList(new LambdaQueryWrapper<GeoFence>()
                        .select(GeoFence::getFenceCode)
                        .likeRight(GeoFence::getFenceCode, CODE_PREFIX))
                .stream().map(GeoFence::getFenceCode).filter(Objects::nonNull).collect(Collectors.toSet());
        for (int seq = 1; seq <= 9999; seq++) {
            String candidate = CODE_PREFIX + String.format("%03d", seq);
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        throw BusinessException.badRequest("围栏编码数量已达上限");
    }

    /**
     * 按绑定业务查询启用围栏（供《调度任务》按线路档案取围栏判定）
     */
    public List<GeoFence> listEnabledByBiz(String bizType, String bizId) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizId)) {
            return List.of();
        }
        return geoFenceMapper.selectList(new LambdaQueryWrapper<GeoFence>()
                .eq(GeoFence::getBizType, bizType.trim().toUpperCase())
                .eq(GeoFence::getBizId, bizId.trim())
                .eq(GeoFence::getStatus, STATUS_ENABLED));
    }

    /**
     * 单点是否落在指定围栏内（调度 / 签收链路复用，不再重复实现几何判定）
     */
    public boolean isInside(GeoFence fence, double lat, double lng) {
        if (fence == null) {
            return false;
        }
        if (TYPE_POLYGON.equals(fence.getFenceType())) {
            List<double[]> polygon = GeoUtils.parsePolygon(fence.getPolygonPoints());
            if (polygon.size() < 3) {
                return false;
            }
            return judge(TYPE_POLYGON, polygon, null, null, null, lat, lng).inside();
        }
        if (fence.getCenterLat() == null || fence.getCenterLng() == null || fence.getRadiusMeters() == null) {
            return false;
        }
        return judge(TYPE_CIRCLE, null,
                fence.getCenterLat().doubleValue(), fence.getCenterLng().doubleValue(),
                fence.getRadiusMeters().doubleValue(), lat, lng).inside();
    }

    // ==================== 维护 ====================

    @Transactional(rollbackFor = Exception.class)
    public GeoFenceDTO create(GeoFenceDTO dto) {
        GeoFence entity = new GeoFence();
        applyToEntity(entity, dto, true);
        entity.setId(null);
        geoFenceMapper.insert(entity);
        log.info("电子围栏已创建: id={}, code={}, type={}", entity.getId(), entity.getFenceCode(), entity.getFenceType());
        return toDTO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public GeoFenceDTO update(Long id, GeoFenceDTO dto) {
        GeoFence entity = requireFence(id);
        applyToEntity(entity, dto, false);
        entity.setId(id);
        geoFenceMapper.updateById(entity);
        return toDTO(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        requireFence(id);
        geoFenceMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public GeoFenceDTO updateStatus(Long id, String status) {
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw BusinessException.badRequest("状态仅支持 ENABLED / DISABLED");
        }
        GeoFence entity = requireFence(id);
        entity.setStatus(status);
        geoFenceMapper.updateById(entity);
        return toDTO(entity);
    }

    // ==================== 围栏校验（全系统单一口径） ====================

    /**
     * 围栏校验：支持「围栏档案 / 内联圆形 / 内联多边形」三种来源，支持单点与批量点位
     */
    public FenceCheckResponse check(FenceCheckRequest request) {
        if (request == null) {
            throw BusinessException.badRequest("围栏校验参数不能为空");
        }

        // 1) 解析围栏来源
        Long fenceId = request.getFenceId();
        String fenceName = null;
        String fenceType;
        Double centerLat = request.getCenterLat();
        Double centerLng = request.getCenterLng();
        Double radiusMeters = request.getRadiusMeters();
        List<double[]> polygon = GeoUtils.parsePolygon(request.getPolygon());

        if (fenceId != null) {
            GeoFence fence = requireFence(fenceId);
            fenceName = fence.getFenceName();
            fenceType = fence.getFenceType();
            if (TYPE_POLYGON.equals(fence.getFenceType())) {
                polygon = GeoUtils.parsePolygon(fence.getPolygonPoints());
            } else {
                centerLat = fence.getCenterLat() != null ? fence.getCenterLat().doubleValue() : null;
                centerLng = fence.getCenterLng() != null ? fence.getCenterLng().doubleValue() : null;
                radiusMeters = fence.getRadiusMeters() != null ? fence.getRadiusMeters().doubleValue() : null;
            }
        } else if (TYPE_POLYGON.equalsIgnoreCase(String.valueOf(request.getFenceType()))) {
            fenceType = TYPE_POLYGON;
        } else {
            fenceType = TYPE_CIRCLE;
        }

        // 2) 校验围栏几何合法性
        if (TYPE_POLYGON.equals(fenceType)) {
            if (polygon.size() < 3) {
                throw BusinessException.badRequest("多边形围栏至少需要 3 个顶点");
            }
        } else if (centerLat == null || centerLng == null || radiusMeters == null || radiusMeters <= 0) {
            throw BusinessException.badRequest("圆形围栏需要中心点坐标与大于 0 的半径");
        }

        // 3) 组装待校验点位
        List<FenceCheckRequest.Point> points = new ArrayList<>();
        if (request.getPoints() != null && !request.getPoints().isEmpty()) {
            points.addAll(request.getPoints());
        } else if (request.getLat() != null && request.getLng() != null) {
            points.add(FenceCheckRequest.Point.builder().lat(request.getLat()).lng(request.getLng()).build());
        }
        if (points.isEmpty()) {
            throw BusinessException.badRequest("缺少待校验点位（lat/lng 或 points）");
        }

        // 4) 逐点判定
        List<FenceCheckResponse.Item> results = new ArrayList<>();
        for (FenceCheckRequest.Point p : points) {
            double lat = p.getLat();
            double lng = p.getLng();
            Judgement judgement = judge(fenceType, polygon, centerLat, centerLng, radiusMeters, lat, lng);
            results.add(FenceCheckResponse.Item.builder()
                    .lat(lat).lng(lng).address(p.getAddress())
                    .inside(judgement.inside())
                    .distanceMeters(round(judgement.distance()))
                    .build());
        }

        FenceCheckResponse.Item first = results.get(0);
        return FenceCheckResponse.builder()
                .success(true)
                .inside(first.getInside())
                .distanceMeters(first.getDistanceMeters())
                .fenceId(fenceId)
                .fenceName(fenceName)
                .fenceType(fenceType)
                .results(results)
                .message(Boolean.TRUE.equals(first.getInside()) ? "在围栏内" : "在围栏外")
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 判定结果：是否在围栏内 + 到中心/边界的距离（米）
     */
    private record Judgement(boolean inside, double distance) {
    }

    /**
     * 纯几何判定（圆形 / 多边形），供围栏校验与调度到点校验共用（单一口径）
     */
    private Judgement judge(String fenceType, List<double[]> polygon,
                            Double centerLat, Double centerLng, Double radiusMeters,
                            double lat, double lng) {
        if (TYPE_POLYGON.equals(fenceType)) {
            boolean inside = GeoUtils.inPolygon(lat, lng, polygon);
            double distance = GeoUtils.distanceToPolygonMeters(lat, lng, polygon);
            // 落在边界线上的点按「在围栏内」处理（射线法对边界点判定不确定，避免临界抖动）
            if (!inside && distance <= BOUNDARY_TOLERANCE_METERS) {
                inside = true;
            }
            return new Judgement(inside, distance);
        }
        double distance = GeoUtils.distanceMeters(lat, lng, centerLat, centerLng);
        return new Judgement(distance <= radiusMeters, distance);
    }

    private LambdaQueryWrapper<GeoFence> buildWrapper(GeoFenceQueryDTO q) {
        LambdaQueryWrapper<GeoFence> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(GeoFence::getFenceCode, kw)
                    .or().like(GeoFence::getFenceName, kw)
                    .or().like(GeoFence::getBizName, kw));
        }
        if (StringUtils.hasText(q.getFenceType())) {
            wrapper.eq(GeoFence::getFenceType, q.getFenceType().trim().toUpperCase());
        }
        if (StringUtils.hasText(q.getStatus())) {
            wrapper.eq(GeoFence::getStatus, q.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(q.getBizType())) {
            wrapper.eq(GeoFence::getBizType, q.getBizType().trim().toUpperCase());
        }
        wrapper.orderByDesc(GeoFence::getCreateTime).orderByDesc(GeoFence::getId);
        return wrapper;
    }

    private GeoFence requireFence(Long id) {
        if (id == null) {
            throw BusinessException.badRequest("围栏ID不能为空");
        }
        GeoFence entity = geoFenceMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.badRequest("围栏不存在: " + id);
        }
        return entity;
    }

    /** 保存入参 → 实体（含几何与编码合法性校验） */
    private void applyToEntity(GeoFence entity, GeoFenceDTO dto, boolean isCreate) {
        if (dto == null) {
            throw BusinessException.badRequest("围栏数据不能为空");
        }
        if (!StringUtils.hasText(dto.getFenceName())) {
            throw BusinessException.badRequest("围栏名称不能为空");
        }
        String type = StringUtils.hasText(dto.getFenceType()) ? dto.getFenceType().trim().toUpperCase() : TYPE_CIRCLE;
        if (!TYPE_CIRCLE.equals(type) && !TYPE_POLYGON.equals(type)) {
            throw BusinessException.badRequest("围栏类型仅支持 CIRCLE / POLYGON");
        }

        entity.setFenceName(dto.getFenceName().trim());
        entity.setFenceType(type);
        entity.setBizType(StringUtils.hasText(dto.getBizType()) ? dto.getBizType().trim().toUpperCase() : null);
        entity.setBizId(dto.getBizId());
        entity.setBizName(dto.getBizName());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : STATUS_ENABLED);
        entity.setRemark(dto.getRemark());

        if (TYPE_CIRCLE.equals(type)) {
            if (dto.getCenterLat() == null || dto.getCenterLng() == null
                    || dto.getRadiusMeters() == null || dto.getRadiusMeters().compareTo(BigDecimal.ZERO) <= 0) {
                throw BusinessException.badRequest("圆形围栏需要中心点坐标与大于 0 的半径");
            }
            if (!GeoUtils.validCoord(dto.getCenterLat().doubleValue(), dto.getCenterLng().doubleValue())) {
                throw BusinessException.badRequest("圆形围栏中心点坐标不合法");
            }
            entity.setCenterLat(dto.getCenterLat());
            entity.setCenterLng(dto.getCenterLng());
            entity.setRadiusMeters(dto.getRadiusMeters());
            entity.setPolygonPoints(null);
        } else {
            List<double[]> polygon = GeoUtils.parsePolygon(dto.getPolygonPoints());
            if (polygon.size() < 3) {
                throw BusinessException.badRequest("多边形围栏至少需要 3 个顶点");
            }
            for (double[] p : polygon) {
                if (!GeoUtils.validCoord(p[1], p[0])) {
                    throw BusinessException.badRequest("多边形围栏存在非法顶点: " + p[0] + "," + p[1]);
                }
            }
            entity.setPolygonPoints(GeoUtils.formatPolygon(polygon));
            entity.setCenterLat(null);
            entity.setCenterLng(null);
            entity.setRadiusMeters(null);
        }

        // 编码：留空自动生成；同租户内唯一
        String code = StringUtils.hasText(dto.getFenceCode()) ? dto.getFenceCode().trim() : null;
        if (code == null) {
            code = isCreate && entity.getId() == null ? nextCode() : entity.getFenceCode();
            if (!StringUtils.hasText(code)) {
                code = nextCode();
            }
        }
        assertCodeAvailable(code, entity.getId());
        entity.setFenceCode(code);
        if (isCreate) {
            entity.setTenantId(SecurityUtils.getCurrentTenantId());
        }
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        List<GeoFence> rows = geoFenceMapper.selectList(new LambdaQueryWrapper<GeoFence>()
                .eq(GeoFence::getFenceCode, code));
        boolean conflict = rows.stream().anyMatch(r -> excludeId == null || !excludeId.equals(r.getId()));
        if (conflict) {
            throw BusinessException.badRequest("围栏编码已存在: " + code);
        }
    }

    private GeoFenceDTO toDTO(GeoFence entity) {
        GeoFenceDTO dto = new GeoFenceDTO();
        dto.setId(entity.getId());
        dto.setFenceCode(entity.getFenceCode());
        dto.setFenceName(entity.getFenceName());
        dto.setFenceType(entity.getFenceType());
        dto.setCenterLat(entity.getCenterLat());
        dto.setCenterLng(entity.getCenterLng());
        dto.setRadiusMeters(entity.getRadiusMeters());
        dto.setPolygonPoints(entity.getPolygonPoints());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
        dto.setBizName(entity.getBizName());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
