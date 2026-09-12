package cn.aiedge.erp.delivery.route.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.delivery.route.dto.RouteAreaDTO;
import cn.aiedge.erp.delivery.route.dto.RouteDTO;
import cn.aiedge.erp.delivery.route.dto.RouteQueryDTO;
import cn.aiedge.erp.delivery.route.entity.RouteArea;
import cn.aiedge.erp.delivery.route.entity.RouteMaster;
import cn.aiedge.erp.delivery.route.mapper.RegionLookupMapper;
import cn.aiedge.erp.delivery.route.mapper.RouteAreaMapper;
import cn.aiedge.erp.delivery.route.mapper.RouteMasterMapper;
import cn.aiedge.erp.delivery.route.service.RouteMasterService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 线路主数据 Service 实现
 *
 * 红线：本表为「线路档案」（资料 → 配送管理 → 线路），与《配送路线单》执行单据（erp_delivery_route）严格区分，
 *   不存配送员 / 进度 / 起讫时间，只被单据引用（销售订单/出库单表头「配送线路」、订单处理中心「按线路」维度）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteMasterServiceImpl implements RouteMasterService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    /** 线路编号自动生成前缀（对标资料模块手工编号习惯：XL001、XL002 …） */
    private static final String CODE_PREFIX = "XL";

    private final RouteMasterMapper routeMasterMapper;
    private final RouteAreaMapper routeAreaMapper;
    private final RegionLookupMapper regionLookupMapper;

    // ==================== 查询 ====================

    @Override
    public Page<RouteDTO> page(RouteQueryDTO query) {
        RouteQueryDTO q = query != null ? query : new RouteQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<RouteMaster> entityPage = routeMasterMapper.selectPage(
                new Page<>(pageNum, pageSize), buildWrapper(q));

        Page<RouteDTO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        result.setRecords(toDTOList(entityPage.getRecords()));
        return result;
    }

    @Override
    public List<RouteDTO> list(RouteQueryDTO query) {
        List<RouteMaster> rows = routeMasterMapper.selectList(buildWrapper(query != null ? query : new RouteQueryDTO()));
        return toDTOList(rows);
    }

    @Override
    public RouteDTO getById(Long id) {
        RouteMaster entity = routeMasterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("线路不存在: " + id);
        }
        RouteDTO dto = toDTO(entity);
        dto.setAreas(loadAreas(List.of(id)).getOrDefault(id, List.of()));
        dto.setAreaText(joinAreaText(dto.getAreas()));
        return dto;
    }

    @Override
    public List<RouteDTO> options() {
        RouteQueryDTO q = new RouteQueryDTO();
        q.setStatus(STATUS_ENABLED);
        List<RouteDTO> rows = toDTOList(routeMasterMapper.selectList(buildWrapper(q)));
        return rows;
    }

    @Override
    public String nextCode() {
        List<RouteMaster> rows = routeMasterMapper.selectList(
                new LambdaQueryWrapper<RouteMaster>()
                        .select(RouteMaster::getRouteCode)
                        .likeRight(RouteMaster::getRouteCode, CODE_PREFIX));
        Set<String> taken = rows.stream()
                .map(RouteMaster::getRouteCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (int seq = 1; seq <= 9999; seq++) {
            String candidate = CODE_PREFIX + String.format("%03d", seq);
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        throw BusinessException.badRequest("线路编号数量已达上限");
    }

    /** 查询条件装配（对标固定查询项：筛选条件 + 显示状态 + 线路类型） */
    private LambdaQueryWrapper<RouteMaster> buildWrapper(RouteQueryDTO q) {
        LambdaQueryWrapper<RouteMaster> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            // 「配送区域」模糊匹配：先按子表反查命中的线路ID，避免手写 inSql 丢租户条件
            List<RouteArea> hitAreas = routeAreaMapper.selectList(
                    new LambdaQueryWrapper<RouteArea>()
                            .select(RouteArea::getRouteId)
                            .and(w -> w.like(RouteArea::getAreaCode, kw).or().like(RouteArea::getAreaName, kw)));
            Set<Long> areaRouteIds = hitAreas.stream()
                    .map(RouteArea::getRouteId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            wrapper.and(w -> {
                w.like(RouteMaster::getRouteCode, kw).or().like(RouteMaster::getRouteName, kw);
                if (!areaRouteIds.isEmpty()) {
                    w.or().in(RouteMaster::getId, areaRouteIds);
                }
            });
        }
        if (StringUtils.hasText(q.getRouteType())) {
            if ("SELF".equalsIgnoreCase(q.getRouteType())) {
                wrapper.eq(RouteMaster::getRouteSelf, 1);
            } else if ("LOGISTICS".equalsIgnoreCase(q.getRouteType())) {
                wrapper.eq(RouteMaster::getRouteLogistics, 1);
            }
        }
        if (StringUtils.hasText(q.getStatus())) {
            wrapper.eq(RouteMaster::getStatus, q.getStatus().trim().toUpperCase());
        } else if (q.getShowDisabled() == null || q.getShowDisabled() != 1) {
            // 「显示状态」默认已启用（对标 ql361 默认行为）
            wrapper.eq(RouteMaster::getStatus, STATUS_ENABLED);
        }
        wrapper.orderByAsc(RouteMaster::getRouteCode);
        return wrapper;
    }

    // ==================== 写操作 ====================

    @Override
    @Transactional
    public RouteDTO create(RouteDTO dto) {
        String code = dto.getRouteCode() == null ? "" : dto.getRouteCode().trim();
        if (!StringUtils.hasText(code)) {
            code = nextCode();
        }
        if (!StringUtils.hasText(dto.getRouteName())) {
            throw BusinessException.badRequest("线路名称不能为空");
        }
        assertRouteTypeChosen(dto);
        assertCodeAvailable(code, null);

        RouteMaster entity = new RouteMaster();
        applyToEntity(entity, dto);
        entity.setRouteCode(code);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        routeMasterMapper.insert(entity);
        saveAreas(entity.getId(), dto.getAreas());
        log.info("新增线路: id={}, code={}, name={}", entity.getId(), entity.getRouteCode(), entity.getRouteName());
        return getById(entity.getId());
    }

    @Override
    @Transactional
    public RouteDTO update(Long id, RouteDTO dto) {
        RouteMaster entity = routeMasterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("线路不存在: " + id);
        }
        String newCode = dto.getRouteCode() == null ? null : dto.getRouteCode().trim();
        if (StringUtils.hasText(newCode) && !newCode.equals(entity.getRouteCode())) {
            assertCodeAvailable(newCode, id);
        }
        if (!StringUtils.hasText(dto.getRouteName()) && !StringUtils.hasText(entity.getRouteName())) {
            throw BusinessException.badRequest("线路名称不能为空");
        }
        if (dto.getRouteSelf() != null || dto.getRouteLogistics() != null) {
            // 只传一侧时另一侧补 0，保证「互斥单选」口径不被历史值破坏
            if (dto.getRouteSelf() == null) dto.setRouteSelf(0);
            if (dto.getRouteLogistics() == null) dto.setRouteLogistics(0);
            assertRouteTypeChosen(dto);
        }

        applyToEntity(entity, dto);
        if (StringUtils.hasText(newCode)) {
            entity.setRouteCode(newCode);
        }
        routeMasterMapper.updateById(entity);
        // 子表整体替换（对标子表「新增 / 删除」行为）
        if (dto.getAreas() != null) {
            deleteAreasByRouteId(id);
            saveAreas(id, dto.getAreas());
        }
        log.info("修改线路: id={}, code={}, name={}", id, entity.getRouteCode(), entity.getRouteName());
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RouteMaster entity = routeMasterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("线路不存在: " + id);
        }
        // ⚠️ @TableLogic 字段必须用 UpdateWrapper 显式 SET，updateById 会剔除逻辑删除列
        routeMasterMapper.update(null, new LambdaUpdateWrapper<RouteMaster>()
                .eq(RouteMaster::getId, id)
                .set(RouteMaster::getDeleted, 1));
        deleteAreasByRouteId(id);
        log.info("删除线路: id={}, code={}", id, entity.getRouteCode());
    }

    @Override
    @Transactional
    public RouteDTO updateStatus(Long id, String status) {
        RouteMaster entity = routeMasterMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("线路不存在: " + id);
        }
        String target = normalizeStatus(status);
        entity.setStatus(target);
        routeMasterMapper.updateById(entity);
        log.info("{}线路: id={}, code={}", STATUS_ENABLED.equals(target) ? "启用" : "停用", id, entity.getRouteCode());
        return getById(id);
    }

    @Override
    @Transactional
    public int batchStatus(List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请先选择要操作的线路");
        }
        String target = normalizeStatus(status);
        routeMasterMapper.update(null, new LambdaUpdateWrapper<RouteMaster>()
                .in(RouteMaster::getId, ids)
                .set(RouteMaster::getStatus, target));
        log.info("批量{}线路: count={}", STATUS_ENABLED.equals(target) ? "启用" : "停用", ids.size());
        return ids.size();
    }

    // ==================== 导入 ====================

    @Override
    @Transactional
    public Map<String, Object> importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要导入的 Excel 文件");
        }
        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headRow = sheet.getRow(sheet.getFirstRowNum());
            if (headRow == null) {
                return importResult(0, 0, List.of("模板缺少表头行"));
            }
            Map<Integer, String> headerMap = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headRow) {
                String field = importField(formatter.formatCellValue(cell));
                if (field != null) {
                    headerMap.put(cell.getColumnIndex(), field);
                }
            }
            if (!headerMap.containsValue("routeName")) {
                return importResult(0, 0, List.of("模板缺少「线路名称」列"));
            }

            for (int rowIdx = sheet.getFirstRowNum() + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                Map<String, String> values = new HashMap<>();
                headerMap.forEach((col, field) -> values.put(field, formatter.formatCellValue(row.getCell(col)).trim()));
                String name = values.get("routeName");
                if (!StringUtils.hasText(name)) {
                    continue; // 整行为空则跳过
                }
                total++;
                int rowNo = rowIdx + 1;
                try {
                    String code = values.get("routeCode");
                    if (!StringUtils.hasText(code)) {
                        code = nextCode();
                    }
                    if (routeMasterMapper.selectCount(new LambdaQueryWrapper<RouteMaster>()
                            .eq(RouteMaster::getRouteCode, code)) > 0) {
                        errors.add("第" + rowNo + "行：线路编号「" + code + "」已存在");
                        continue;
                    }
                    Integer[] types = parseRouteType(values.get("routeType"));
                    if (types[0] == 0 && types[1] == 0) {
                        errors.add("第" + rowNo + "行：线路类型必须为「自配」或「物流」");
                        continue;
                    }
                    if (types[0] == 1 && types[1] == 1) {
                        errors.add("第" + rowNo + "行：线路类型只能选择一项（自配 或 物流）");
                        continue;
                    }

                    RouteMaster entity = new RouteMaster();
                    entity.setRouteCode(code);
                    entity.setRouteName(name);
                    entity.setRouteSelf(types[0]);
                    entity.setRouteLogistics(types[1]);
                    entity.setExpressName(blankToNull(values.get("expressName")));
                    entity.setRemark(blankToNull(values.get("remark")));
                    entity.setStatus(STATUS_ENABLED);
                    routeMasterMapper.insert(entity);

                    List<RouteAreaDTO> areas = parseAreaCodes(values.get("areaCode"));
                    if (!areas.isEmpty()) {
                        saveAreas(entity.getId(), areas);
                    }
                    success++;
                } catch (Exception ex) {
                    log.warn("线路导入第{}行失败", rowNo, ex);
                    errors.add("第" + rowNo + "行：" + ex.getMessage());
                }
            }
        } catch (IOException e) {
            throw BusinessException.badRequest("Excel 解析失败：" + e.getMessage());
        }
        log.info("线路导入完成: total={}, success={}, failure={}", total, success, errors.size());
        return importResult(total, success, errors);
    }

    /** 模板表头 → 字段名 */
    private String importField(String header) {
        if (header == null) return null;
        String h = header.trim();
        if (h.isEmpty() || h.startsWith("导入结果")) return null;
        if (h.contains("线路编号") || h.contains("编号")) return "routeCode";
        if (h.contains("线路名称") || h.contains("名称")) return "routeName";
        if (h.contains("线路类型") || h.contains("类型")) return "routeType";
        if (h.contains("物流公司")) return "expressName";
        if (h.contains("配送区域")) return "areaCode";
        if (h.contains("备注")) return "remark";
        return null;
    }

    /** 线路类型文本 → [自配, 物流] */
    private Integer[] parseRouteType(String text) {
        int self = 0;
        int logistics = 0;
        if (StringUtils.hasText(text)) {
            String t = text.trim();
            if (t.contains("自配") || t.contains("自送")) self = 1;
            if (t.contains("物流") || t.contains("第三方")) logistics = 1;
        }
        return new Integer[]{self, logistics};
    }

    /** 配送区域编码串（英文逗号/中文逗号/分号分隔）→ 子表行 */
    private List<RouteAreaDTO> parseAreaCodes(String text) {
        List<RouteAreaDTO> areas = new ArrayList<>();
        if (!StringUtils.hasText(text)) {
            return areas;
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String part : text.split("[,，;；\\s]+")) {
            if (StringUtils.hasText(part)) {
                codes.add(part.trim());
            }
        }
        int sort = 0;
        for (String code : codes) {
            RegionLookupMapper.RegionRow region = regionLookupMapper.selectByCode(code);
            if (region == null) {
                throw BusinessException.badRequest("配送区域编码「" + code + "」不存在");
            }
            RouteAreaDTO dto = new RouteAreaDTO();
            dto.setAreaCode(region.getCode());
            dto.setAreaName(region.getName());
            dto.setAreaType(levelToType(region.getRegionLevel()));
            dto.setSortNo(sort++);
            areas.add(dto);
        }
        return areas;
    }

    // ==================== 内部工具 ====================

    private void applyToEntity(RouteMaster entity, RouteDTO dto) {
        if (dto.getRouteName() != null) entity.setRouteName(dto.getRouteName().trim());
        if (dto.getRouteSelf() != null) entity.setRouteSelf(dto.getRouteSelf());
        if (dto.getRouteLogistics() != null) entity.setRouteLogistics(dto.getRouteLogistics());
        if (dto.getExpressName() != null) entity.setExpressName(dto.getExpressName().trim());
        if (dto.getRemark() != null) entity.setRemark(dto.getRemark());
        if (StringUtils.hasText(dto.getStatus())) entity.setStatus(dto.getStatus().trim().toUpperCase());
        if (entity.getRouteSelf() == null) entity.setRouteSelf(0);
        if (entity.getRouteLogistics() == null) entity.setRouteLogistics(0);
    }

    /** 线路类型为互斥单选（对标实测：自配 / 物流 二选一），至少选一项且不可同时命中 */
    private void assertRouteTypeChosen(RouteDTO dto) {
        boolean self = dto.getRouteSelf() != null && dto.getRouteSelf() == 1;
        boolean logistics = dto.getRouteLogistics() != null && dto.getRouteLogistics() == 1;
        if (!self && !logistics) {
            throw BusinessException.badRequest("请选择线路类型（自配 / 物流）");
        }
        if (self && logistics) {
            throw BusinessException.badRequest("线路类型只能选择一项（自配 或 物流）");
        }
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        Long count = routeMasterMapper.selectCount(new LambdaQueryWrapper<RouteMaster>()
                .eq(RouteMaster::getRouteCode, code)
                .ne(excludeId != null, RouteMaster::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("线路编号「" + code + "」已存在");
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw BusinessException.badRequest("状态不能为空");
        }
        String s = status.trim().toUpperCase();
        if (!STATUS_ENABLED.equals(s) && !STATUS_DISABLED.equals(s)) {
            throw BusinessException.badRequest("状态取值非法，仅支持 ENABLED-启用 / DISABLED-停用");
        }
        return s;
    }

    /** 保存子表（先按份数去重，避免同一线路重复区域） */
    private void saveAreas(Long routeId, List<RouteAreaDTO> areas) {
        if (routeId == null || areas == null || areas.isEmpty()) {
            return;
        }
        Set<String> seen = new LinkedHashSet<>();
        int sort = 0;
        for (RouteAreaDTO dto : areas) {
            if (!StringUtils.hasText(dto.getAreaCode()) || !seen.add(dto.getAreaCode().trim())) {
                continue;
            }
            RouteArea entity = new RouteArea();
            entity.setRouteId(routeId);
            entity.setAreaCode(dto.getAreaCode().trim());
            entity.setAreaName(StringUtils.hasText(dto.getAreaName())
                    ? dto.getAreaName().trim() : lookupAreaName(dto.getAreaCode().trim()));
            entity.setAreaType(StringUtils.hasText(dto.getAreaType())
                    ? dto.getAreaType().trim().toUpperCase() : lookupAreaType(dto.getAreaCode().trim()));
            entity.setSortNo(sort++);
            routeAreaMapper.insert(entity);
        }
    }

    private String lookupAreaName(String code) {
        RegionLookupMapper.RegionRow region = regionLookupMapper.selectByCode(code);
        return region == null ? null : region.getName();
    }

    private String lookupAreaType(String code) {
        RegionLookupMapper.RegionRow region = regionLookupMapper.selectByCode(code);
        return region == null ? null : levelToType(region.getRegionLevel());
    }

    private void deleteAreasByRouteId(Long routeId) {
        routeAreaMapper.update(null, new LambdaUpdateWrapper<RouteArea>()
                .eq(RouteArea::getRouteId, routeId)
                .set(RouteArea::getDeleted, 1));
    }

    private Map<Long, List<RouteAreaDTO>> loadAreas(List<Long> routeIds) {
        if (routeIds == null || routeIds.isEmpty()) {
            return Map.of();
        }
        List<RouteArea> rows = routeAreaMapper.selectList(new LambdaQueryWrapper<RouteArea>()
                .in(RouteArea::getRouteId, routeIds)
                .orderByAsc(RouteArea::getSortNo)
                .orderByAsc(RouteArea::getId));
        Map<Long, List<RouteAreaDTO>> grouped = new HashMap<>();
        for (RouteArea row : rows) {
            RouteAreaDTO dto = new RouteAreaDTO();
            dto.setId(row.getId());
            dto.setRouteId(row.getRouteId());
            dto.setAreaType(row.getAreaType());
            dto.setAreaCode(row.getAreaCode());
            dto.setAreaName(row.getAreaName());
            dto.setSortNo(row.getSortNo());
            grouped.computeIfAbsent(row.getRouteId(), k -> new ArrayList<>()).add(dto);
        }
        return grouped;
    }

    private List<RouteDTO> toDTOList(List<RouteMaster> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = rows.stream().map(RouteMaster::getId).collect(Collectors.toList());
        Map<Long, List<RouteAreaDTO>> areaMap = loadAreas(ids);
        List<RouteDTO> list = new ArrayList<>(rows.size());
        for (RouteMaster row : rows) {
            RouteDTO dto = toDTO(row);
            List<RouteAreaDTO> areas = areaMap.getOrDefault(row.getId(), new ArrayList<>());
            dto.setAreas(areas);
            dto.setAreaText(joinAreaText(areas));
            list.add(dto);
        }
        list.sort(Comparator.comparing(d -> d.getRouteCode() == null ? "" : d.getRouteCode()));
        return list;
    }

    private RouteDTO toDTO(RouteMaster entity) {
        RouteDTO dto = new RouteDTO();
        dto.setId(entity.getId());
        dto.setRouteCode(entity.getRouteCode());
        dto.setRouteName(entity.getRouteName());
        dto.setRouteSelf(entity.getRouteSelf() == null ? 0 : entity.getRouteSelf());
        dto.setRouteLogistics(entity.getRouteLogistics() == null ? 0 : entity.getRouteLogistics());
        dto.setExpressName(entity.getExpressName());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        dto.setRouteTypeText(routeTypeText(dto.getRouteSelf(), dto.getRouteLogistics()));
        dto.setStatusText(STATUS_ENABLED.equalsIgnoreCase(entity.getStatus()) ? "已启用" : "已停用");
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private String routeTypeText(Integer self, Integer logistics) {
        List<String> parts = new ArrayList<>();
        if (self != null && self == 1) parts.add("自配");
        if (logistics != null && logistics == 1) parts.add("物流");
        return String.join("、", parts);
    }

    private String joinAreaText(List<RouteAreaDTO> areas) {
        if (areas == null || areas.isEmpty()) {
            return "";
        }
        return areas.stream()
                .map(a -> StringUtils.hasText(a.getAreaName()) ? a.getAreaName() : a.getAreaCode())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }

    private String levelToType(Integer level) {
        if (level == null) return null;
        return switch (level) {
            case 1 -> "PROVINCE";
            case 2 -> "CITY";
            case 3 -> "DISTRICT";
            default -> null;
        };
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> importResult(int total, int success, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failure", errors.size());
        result.put("errors", errors);
        return result;
    }
}
