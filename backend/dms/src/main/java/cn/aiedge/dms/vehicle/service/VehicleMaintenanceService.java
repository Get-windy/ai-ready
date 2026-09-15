package cn.aiedge.dms.vehicle.service;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.MaintTypeEnum;
import cn.aiedge.dms.vehicle.dto.MaintenanceCreateDTO;
import cn.aiedge.dms.vehicle.dto.MaintenanceQuery;
import cn.aiedge.dms.vehicle.dto.MaintenanceVO;
import cn.aiedge.dms.vehicle.dto.VendorOptionVO;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.entity.DmsVehicleMaintenance;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMaintenanceMapper;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import cn.aiedge.dms.vehicle.mapper.PartnerLookupMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 车辆维保记录服务（金标准）
 *
 * <p>职责：维保台账 CRUD + 车辆里程回写 + 下次到期推算 + 到期提醒 + 费用统计 + 真实 xlsx 导出。
 * 与《车辆管理》的分工：车辆档案存「当前状态/当前里程/证件到期日」，本服务存「维保事件流水」，
 * 并在维保后回写车辆里程、按维保类型回写车辆年检/保险到期日。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleMaintenanceService {

    /** 维保单号前缀（全租户唯一号段，同《线路》XL 的口径） */
    private static final String NO_PREFIX = "WBD";

    /** 保养默认间隔：车辆档案未配置 maintenanceIntervalKm 时兜底 5000 公里 */
    private static final int DEFAULT_UPKEEP_INTERVAL_KM = 5000;

    /** 保养默认周期：90 天（用于推算下次保养日期，可被表单显式值覆盖） */
    private static final int DEFAULT_UPKEEP_INTERVAL_DAYS = 90;

    /** 厂商选择器默认返回条数 / 上限 */
    private static final int VENDOR_OPTION_LIMIT = 20;
    private static final int VENDOR_OPTION_MAX_LIMIT = 50;

    /** 往来单位类型：仅「供应商 / 其他往来单位」可作为维保厂商（客户、物流公司不入选） */
    private static final Map<Integer, String> VENDOR_PARTY_TYPE_TEXT = Map.of(
            2, "供应商",
            4, "其他往来单位");

    private static final String[] EXPORT_HEADERS = {
            "维保单号", "车牌号", "品牌", "型号", "维保类型", "维保日期", "维保内容",
            "维保费用", "维保厂商", "维保前里程(km)", "维保后里程(km)",
            "下次维保日期", "下次维保里程(km)", "经办人", "备注"
    };

    private final DmsVehicleMaintenanceMapper maintenanceMapper;
    private final DmsVehicleMapper vehicleMapper;
    private final SysUserMapper sysUserMapper;
    private final PartnerLookupMapper partnerLookupMapper;

    // ==================== 查询 ====================

    /**
     * 多条件分页（车牌联查 / 类型多选 / 日期·费用区间 / 厂商 / 是否含附件）
     */
    public Page<MaintenanceVO> page(MaintenanceQuery query, int pageNum, int pageSize) {
        MaintenanceQuery q = query != null ? query : new MaintenanceQuery();
        List<Long> plateHitVehicleIds = resolvePlateVehicleIds(q);
        if (plateHitVehicleIds != null && plateHitVehicleIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }
        Page<DmsVehicleMaintenance> page = maintenanceMapper.selectPage(
                new Page<>(pageNum, pageSize), buildWrapper(q, plateHitVehicleIds));
        return toVoPage(page, q);
    }

    /**
     * 详情（含车辆快照与维保前里程）
     */
    public MaintenanceVO detail(Long id) {
        DmsVehicleMaintenance entity = maintenanceMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("维保记录不存在: " + id);
        }
        return toVoPage(singlePage(entity), new MaintenanceQuery()).getRecords().get(0);
    }

    /**
     * 下一个维保单号（WBD0001 递增，同租户内不重复）
     */
    public String nextNo() {
        List<DmsVehicleMaintenance> rows = maintenanceMapper.selectList(
                new LambdaQueryWrapper<DmsVehicleMaintenance>()
                        .select(DmsVehicleMaintenance::getMaintNo)
                        .likeRight(DmsVehicleMaintenance::getMaintNo, NO_PREFIX));
        Set<String> taken = rows.stream()
                .map(DmsVehicleMaintenance::getMaintNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (int seq = 1; seq <= 9999; seq++) {
            String candidate = NO_PREFIX + String.format("%04d", seq);
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        throw BusinessException.badRequest("维保单号数量已达上限");
    }

    /**
     * 厂商选择器数据源（往来单位：供应商 / 其他往来单位；支持名称·编码·助记码模糊）
     *
     * <p>与《资料 → 往来单位》同一主数据，本页只读引用；未建档厂商仍允许表单手工填写。</p>
     */
    public List<VendorOptionVO> vendorOptions(String keyword, Integer limit) {
        int size = limit == null || limit < 1 ? VENDOR_OPTION_LIMIT
                : Math.min(limit, VENDOR_OPTION_MAX_LIMIT);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<VendorOptionVO> rows = partnerLookupMapper.searchVendors(kw, size);
        rows.forEach(v -> v.setPartyTypeText(VENDOR_PARTY_TYPE_TEXT.get(v.getPartyType())));
        return rows;
    }

    /**
     * 费用统计（按类型 / 按月份 / 按车辆 / 按厂商），支撑单公里维保成本与服务商成本分析
     */
    public Map<String, Object> stat(MaintenanceQuery query) {
        MaintenanceQuery q = query != null ? query : new MaintenanceQuery();
        List<Long> plateHitVehicleIds = resolvePlateVehicleIds(q);
        Map<String, Object> result = new LinkedHashMap<>();
        if (plateHitVehicleIds != null && plateHitVehicleIds.isEmpty()) {
            result.put("recordCount", 0);
            result.put("totalCost", BigDecimal.ZERO);
            result.put("vehicleCount", 0);
            result.put("byType", Collections.emptyList());
            result.put("byMonth", Collections.emptyList());
            result.put("byVehicle", Collections.emptyList());
            result.put("byVendor", Collections.emptyList());
            return result;
        }
        List<DmsVehicleMaintenance> rows = maintenanceMapper.selectList(buildWrapper(q, plateHitVehicleIds));
        Map<Long, DmsVehicle> vehicleMap = loadVehicles(rows);

        Map<Integer, int[]> typeCount = new LinkedHashMap<>();
        Map<Integer, BigDecimal> typeCost = new LinkedHashMap<>();
        Map<String, int[]> monthCount = new TreeMap<>();
        Map<String, BigDecimal> monthCost = new TreeMap<>();
        Map<String, int[]> vehicleCount = new LinkedHashMap<>();
        Map<String, BigDecimal> vehicleCost = new LinkedHashMap<>();
        Map<String, int[]> vendorCount = new LinkedHashMap<>();
        Map<String, BigDecimal> vendorCost = new LinkedHashMap<>();
        Map<String, String> vendorLabel = new LinkedHashMap<>();
        Map<String, Boolean> vendorLinked = new LinkedHashMap<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        Set<Long> vehicles = new HashSet<>();

        for (DmsVehicleMaintenance row : rows) {
            BigDecimal cost = row.getMaintCost() != null ? row.getMaintCost() : BigDecimal.ZERO;
            totalCost = totalCost.add(cost);
            vehicles.add(row.getVehicleId());

            Integer type = row.getMaintType();
            typeCount.computeIfAbsent(type, k -> new int[1])[0]++;
            typeCost.merge(type, cost, BigDecimal::add);

            String month = row.getMaintDate() != null
                    ? row.getMaintDate().format(DateTimeFormatter.ofPattern("yyyy-MM")) : "未知";
            monthCount.computeIfAbsent(month, k -> new int[1])[0]++;
            monthCost.merge(month, cost, BigDecimal::add);

            DmsVehicle v = vehicleMap.get(row.getVehicleId());
            String plate = v != null && StringUtils.hasText(v.getPlateNo()) ? v.getPlateNo() : String.valueOf(row.getVehicleId());
            vehicleCount.computeIfAbsent(plate, k -> new int[1])[0]++;
            vehicleCost.merge(plate, cost, BigDecimal::add);

            // 按厂商：已建档按 vendorId 归并（改名后仍是同一厂商），未建档按名称归并
            String vendorKey = row.getVendorId() != null ? "id:" + row.getVendorId()
                    : "name:" + (StringUtils.hasText(row.getMaintVendor()) ? row.getMaintVendor().trim() : "未填写");
            vendorCount.computeIfAbsent(vendorKey, k -> new int[1])[0]++;
            vendorCost.merge(vendorKey, cost, BigDecimal::add);
            vendorLinked.putIfAbsent(vendorKey, row.getVendorId() != null);
            // 名称快照以最新一笔为准（rows 已按维保日期倒序）
            vendorLabel.putIfAbsent(vendorKey, StringUtils.hasText(row.getMaintVendor())
                    ? row.getMaintVendor() : "未填写");
        }

        List<Map<String, Object>> byType = new ArrayList<>();
        for (Map.Entry<Integer, int[]> e : typeCount.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", e.getKey());
            item.put("typeText", MaintTypeEnum.textOf(e.getKey()));
            item.put("count", e.getValue()[0]);
            item.put("cost", typeCost.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byType.add(item);
        }

        List<Map<String, Object>> byMonth = new ArrayList<>();
        for (Map.Entry<String, int[]> e : monthCount.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", e.getKey());
            item.put("count", e.getValue()[0]);
            item.put("cost", monthCost.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byMonth.add(item);
        }

        List<Map<String, Object>> byVehicle = new ArrayList<>();
        for (Map.Entry<String, int[]> e : vehicleCount.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("plateNo", e.getKey());
            item.put("count", e.getValue()[0]);
            item.put("cost", vehicleCost.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byVehicle.add(item);
        }
        byVehicle.sort((a, b) -> ((BigDecimal) b.get("cost")).compareTo((BigDecimal) a.get("cost")));

        // 按厂商（服务商成本分析；已建档厂商按 vendorId 归并，未建档按名称归并）
        List<Map<String, Object>> byVendor = new ArrayList<>();
        for (Map.Entry<String, int[]> e : vendorCount.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("vendorKey", e.getKey());
            item.put("vendorId", e.getKey().startsWith("id:") ? Long.valueOf(e.getKey().substring(3)) : null);
            item.put("vendorName", vendorLabel.getOrDefault(e.getKey(), "未填写"));
            item.put("linked", Boolean.TRUE.equals(vendorLinked.get(e.getKey())));
            item.put("count", e.getValue()[0]);
            item.put("cost", vendorCost.getOrDefault(e.getKey(), BigDecimal.ZERO));
            byVendor.add(item);
        }
        byVendor.sort((a, b) -> ((BigDecimal) b.get("cost")).compareTo((BigDecimal) a.get("cost")));

        result.put("recordCount", rows.size());
        result.put("totalCost", totalCost);
        result.put("vehicleCount", vehicles.size());
        result.put("byType", byType);
        result.put("byMonth", byMonth);
        result.put("byVehicle", byVehicle);
        result.put("byVendor", byVendor);
        return result;
    }

    /**
     * 到期提醒：每车每类型仅取最新一笔，按「下次维保日期」或「下次保养里程」筛选
     *
     * @param days   日期预警窗口（天）
     * @param warnKm 里程预警窗口（公里）
     */
    public List<MaintenanceVO> expiring(int days, int warnKm) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        List<DmsVehicleMaintenance> rows = maintenanceMapper.selectList(
                new LambdaQueryWrapper<DmsVehicleMaintenance>()
                        .and(w -> w.isNotNull(DmsVehicleMaintenance::getNextMaintDate)
                                .or().isNotNull(DmsVehicleMaintenance::getNextMaintMileage))
                        .orderByDesc(DmsVehicleMaintenance::getMaintDate)
                        .orderByDesc(DmsVehicleMaintenance::getId));

        // 每车每类型仅保留最新一笔（旧的保养计划已被新记录取代，不应重复提醒）
        Map<String, DmsVehicleMaintenance> latest = new LinkedHashMap<>();
        for (DmsVehicleMaintenance row : rows) {
            latest.putIfAbsent(row.getVehicleId() + "#" + row.getMaintType(), row);
        }
        List<MaintenanceVO> voList = toVoList(new ArrayList<>(latest.values()));
        return voList.stream()
                .filter(vo -> (vo.getRemainDays() != null && vo.getRemainDays() <= days)
                        || (vo.getRemainKm() != null && vo.getRemainKm() <= warnKm))
                .sorted(Comparator.comparing(MaintenanceVO::getRemainDays,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ==================== 写入 ====================

    /**
     * 新增维保记录：落库 + 里程回写 + 到期推算/回写
     */
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceVO create(MaintenanceCreateDTO dto) {
        DmsVehicle vehicle = requireVehicle(dto.getVehicleId());
        DmsVehicleMaintenance record = new DmsVehicleMaintenance();
        applyDto(record, dto, vehicle);
        record.setMaintNo(nextNo());
        Long userId = currentUserId();
        record.setCreateBy(userId);
        record.setUpdateBy(userId);
        maintenanceMapper.insert(record);
        writeBackVehicle(record, vehicle);
        log.info("新增维保记录: id={}, no={}, vehicleId={}, type={}",
                record.getId(), record.getMaintNo(), record.getVehicleId(), record.getMaintType());
        return detail(record.getId());
    }

    /**
     * 修改维保记录（原实现缺失 PUT，只能删了重建）
     */
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceVO update(Long id, MaintenanceCreateDTO dto) {
        DmsVehicleMaintenance record = maintenanceMapper.selectById(id);
        if (record == null) {
            throw BusinessException.notFound("维保记录不存在: " + id);
        }
        DmsVehicle vehicle = requireVehicle(dto.getVehicleId());
        applyDto(record, dto, vehicle);
        record.setUpdateBy(currentUserId());
        maintenanceMapper.updateById(record);
        writeBackVehicle(record, vehicle);
        log.info("修改维保记录: id={}, no={}", id, record.getMaintNo());
        return detail(id);
    }

    /**
     * 删除维保记录（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsVehicleMaintenance record = maintenanceMapper.selectById(id);
        if (record == null) {
            throw BusinessException.notFound("维保记录不存在: " + id);
        }
        maintenanceMapper.deleteById(id);
        log.info("删除维保记录: id={}, no={}", id, record.getMaintNo());
    }

    // ==================== 导出 ====================

    /**
     * 真实 xlsx 导出（当前筛选条件全集，最多 10000 行）
     */
    public void export(MaintenanceQuery query, HttpServletResponse response) throws IOException {
        MaintenanceQuery q = query != null ? query : new MaintenanceQuery();
        List<Long> plateHitVehicleIds = resolvePlateVehicleIds(q);
        List<MaintenanceVO> rows;
        if (plateHitVehicleIds != null && plateHitVehicleIds.isEmpty()) {
            rows = Collections.emptyList();
        } else {
            List<DmsVehicleMaintenance> entities =
                    maintenanceMapper.selectList(buildWrapper(q, plateHitVehicleIds));
            rows = toVoList(entities.size() > 10000 ? entities.subList(0, 10000) : entities);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("车辆维保");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowIdx = 1;
            for (MaintenanceVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(nvl(vo.getMaintNo()));
                row.createCell(c++).setCellValue(nvl(vo.getPlateNo()));
                row.createCell(c++).setCellValue(nvl(vo.getVehicleBrand()));
                row.createCell(c++).setCellValue(nvl(vo.getVehicleModel()));
                row.createCell(c++).setCellValue(nvl(vo.getMaintTypeText()));
                row.createCell(c++).setCellValue(vo.getMaintDate() == null ? "" : vo.getMaintDate().toString());
                row.createCell(c++).setCellValue(nvl(vo.getMaintContent()));
                row.createCell(c++).setCellValue(vo.getMaintCost() == null ? "" : vo.getMaintCost().toPlainString());
                row.createCell(c++).setCellValue(nvl(vo.getMaintVendor()));
                row.createCell(c++).setCellValue(vo.getBeforeMaintMileage() == null ? "" : String.valueOf(vo.getBeforeMaintMileage()));
                row.createCell(c++).setCellValue(vo.getAfterMaintMileage() == null ? "" : String.valueOf(vo.getAfterMaintMileage()));
                row.createCell(c++).setCellValue(vo.getNextMaintDate() == null ? "" : vo.getNextMaintDate().toString());
                row.createCell(c++).setCellValue(vo.getNextMaintMileage() == null ? "" : String.valueOf(vo.getNextMaintMileage()));
                row.createCell(c++).setCellValue(nvl(vo.getHandlerName()));
                row.createCell(c).setCellValue(nvl(vo.getRemark()));
            }

            String fileName = URLEncoder.encode("车辆维保_" + LocalDate.now(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");
            workbook.write(response.getOutputStream());
        }
    }

    // ==================== 内部实现 ====================

    private DmsVehicle requireVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw BusinessException.badRequest("车辆不能为空");
        }
        DmsVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + vehicleId);
        }
        return vehicle;
    }

    /** 车牌模糊条件 → 车辆ID集合；未按车牌过滤时返回 null */
    private List<Long> resolvePlateVehicleIds(MaintenanceQuery q) {
        if (q.getVehicleId() != null || !StringUtils.hasText(q.getPlateNo())) {
            return null;
        }
        return vehicleMapper.selectList(new LambdaQueryWrapper<DmsVehicle>()
                        .select(DmsVehicle::getId)
                        .like(DmsVehicle::getPlateNo, q.getPlateNo().trim()))
                .stream().map(DmsVehicle::getId).collect(Collectors.toList());
    }

    private LambdaQueryWrapper<DmsVehicleMaintenance> buildWrapper(MaintenanceQuery q, List<Long> plateHitVehicleIds) {
        LambdaQueryWrapper<DmsVehicleMaintenance> w = new LambdaQueryWrapper<>();
        if (q.getVehicleId() != null) {
            w.eq(DmsVehicleMaintenance::getVehicleId, q.getVehicleId());
        } else if (plateHitVehicleIds != null) {
            w.in(DmsVehicleMaintenance::getVehicleId, plateHitVehicleIds);
        }
        if (StringUtils.hasText(q.getMaintNo())) {
            w.like(DmsVehicleMaintenance::getMaintNo, q.getMaintNo().trim());
        }
        if (q.getMaintTypes() != null && !q.getMaintTypes().isEmpty()) {
            w.in(DmsVehicleMaintenance::getMaintType, q.getMaintTypes());
        }
        if (q.getDateFrom() != null) {
            w.ge(DmsVehicleMaintenance::getMaintDate, q.getDateFrom());
        }
        if (q.getDateTo() != null) {
            w.le(DmsVehicleMaintenance::getMaintDate, q.getDateTo());
        }
        if (q.getCostMin() != null) {
            w.ge(DmsVehicleMaintenance::getMaintCost, q.getCostMin());
        }
        if (q.getCostMax() != null) {
            w.le(DmsVehicleMaintenance::getMaintCost, q.getCostMax());
        }
        if (q.getVendorId() != null) {
            // 已建档厂商：按档案ID精确过滤（厂商改名不影响历史命中）
            w.eq(DmsVehicleMaintenance::getVendorId, q.getVendorId());
        } else if (StringUtils.hasText(q.getVendor())) {
            w.like(DmsVehicleMaintenance::getMaintVendor, q.getVendor().trim());
        }
        if (q.getHasAttachment() != null) {
            if (q.getHasAttachment() == 1) {
                w.isNotNull(DmsVehicleMaintenance::getAttachmentUrls)
                        .ne(DmsVehicleMaintenance::getAttachmentUrls, "");
            } else {
                w.and(x -> x.isNull(DmsVehicleMaintenance::getAttachmentUrls)
                        .or().eq(DmsVehicleMaintenance::getAttachmentUrls, ""));
            }
        }
        w.orderByDesc(DmsVehicleMaintenance::getMaintDate).orderByDesc(DmsVehicleMaintenance::getId);
        return w;
    }

    /** DTO → 实体（含下次到期推算：显式值优先） */
    private void applyDto(DmsVehicleMaintenance record, MaintenanceCreateDTO dto, DmsVehicle vehicle) {
        LocalDate maintDate = dto.getMaintDate() != null ? dto.getMaintDate() : LocalDate.now();
        record.setVehicleId(dto.getVehicleId());
        record.setMaintType(dto.getMaintType());
        record.setMaintDate(maintDate);
        record.setMaintContent(dto.getMaintContent());
        record.setMaintCost(dto.getMaintCost());
        applyVendor(record, dto);
        record.setMaintContact(dto.getMaintContact());
        record.setMaintPhone(dto.getMaintPhone());
        record.setAfterMaintMileage(dto.getAfterMaintMileage());

        LocalDate nextDate = dto.getNextMaintDate() != null
                ? dto.getNextMaintDate() : computeNextDate(dto.getMaintType(), maintDate);
        Integer nextKm = dto.getNextMaintMileage() != null
                ? dto.getNextMaintMileage() : computeNextMileage(dto.getMaintType(), dto.getAfterMaintMileage(), vehicle);
        record.setNextMaintDate(nextDate);
        record.setNextMaintMileage(nextKm);
        record.setAttachmentUrls(dto.getAttachmentUrls());
        record.setRemark(dto.getRemark());
    }

    /**
     * 厂商落库口径（生产级：档案引用优先，名称快照兜底）
     *
     * <ul>
     *   <li>选了往来单位（vendorId）：校验其确为「供应商 / 其他往来单位」，并以**档案名称**回填
     *       {@code maintVendor} 名称快照 —— 名称以档案为准，避免手输变体污染厂商维度；</li>
     *   <li>未选（vendorId 为空）：按 {@code maintVendor} 手工填写的名称记账（散户/路边快修），
     *       vendorId 留空据实标注「未建档」。</li>
     * </ul>
     */
    private void applyVendor(DmsVehicleMaintenance record, MaintenanceCreateDTO dto) {
        if (dto.getVendorId() == null) {
            record.setVendorId(null);
            record.setMaintVendor(dto.getMaintVendor());
            return;
        }
        VendorOptionVO vendor = partnerLookupMapper.findVendor(dto.getVendorId());
        if (vendor == null) {
            throw BusinessException.badRequest("维保厂商不存在或不是供应商/其他往来单位: " + dto.getVendorId());
        }
        record.setVendorId(vendor.getId());
        record.setMaintVendor(vendor.getName());
    }

    /** 下次维保日期：保养 +90 天；年检/保险 +1 年；维修/事故/其他 无周期 */
    private LocalDate computeNextDate(Integer maintType, LocalDate maintDate) {
        if (maintType == null || maintDate == null) {
            return null;
        }
        if (MaintTypeEnum.isUpkeep(maintType)) {
            return maintDate.plusDays(DEFAULT_UPKEEP_INTERVAL_DAYS);
        }
        if (MaintTypeEnum.isCertificate(maintType)) {
            return maintDate.plusYears(1);
        }
        return null;
    }

    /** 下次保养里程：维保后里程 + 车辆保养间隔（档案未配置时兜底 5000km），仅保养 */
    private Integer computeNextMileage(Integer maintType, Integer afterMileage, DmsVehicle vehicle) {
        if (!MaintTypeEnum.isUpkeep(maintType) || afterMileage == null) {
            return null;
        }
        Integer interval = vehicle != null ? vehicle.getMaintenanceIntervalKm() : null;
        int step = interval != null && interval > 0 ? interval : DEFAULT_UPKEEP_INTERVAL_KM;
        return afterMileage + step;
    }

    /** 维保成果回写车辆：里程、上次保养里程/日期、年检/保险到期日 */
    private void writeBackVehicle(DmsVehicleMaintenance record, DmsVehicle vehicle) {
        if (vehicle == null || vehicle.getId() == null) {
            return;
        }
        LambdaUpdateWrapper<DmsVehicle> uw = new LambdaUpdateWrapper<>();
        boolean dirty = false;
        Integer after = record.getAfterMaintMileage();
        Integer current = vehicle.getCurrentMileage();
        if (after != null && (current == null || after > current)) {
            uw.set(DmsVehicle::getCurrentMileage, after);
            dirty = true;
        }
        if (MaintTypeEnum.isUpkeep(record.getMaintType())) {
            if (after != null) {
                uw.set(DmsVehicle::getLastMaintenanceKm, after);
            }
            uw.set(DmsVehicle::getLastMaintenanceDate, record.getMaintDate());
            dirty = true;
        }
        if (record.getNextMaintDate() != null && record.getMaintType() != null) {
            if (record.getMaintType() == MaintTypeEnum.ANNUAL_INSPECTION.getValue()) {
                uw.set(DmsVehicle::getInspectionExpireDate, record.getNextMaintDate());
                dirty = true;
            } else if (record.getMaintType() == MaintTypeEnum.INSURANCE.getValue()) {
                uw.set(DmsVehicle::getInsuranceExpireDate, record.getNextMaintDate());
                dirty = true;
            }
        }
        if (dirty) {
            uw.eq(DmsVehicle::getId, vehicle.getId());
            vehicleMapper.update(null, uw);
            log.info("维保回写车辆: vehicleId={}, currentMileage={}, lastMaintenanceKm={}",
                    vehicle.getId(), after, record.getAfterMaintMileage());
        }
    }

    private Page<DmsVehicleMaintenance> singlePage(DmsVehicleMaintenance entity) {
        Page<DmsVehicleMaintenance> page = new Page<>(1, 1, false);
        page.setRecords(Collections.singletonList(entity));
        page.setTotal(1);
        return page;
    }

    private Page<MaintenanceVO> toVoPage(Page<DmsVehicleMaintenance> src, MaintenanceQuery q) {
        Page<MaintenanceVO> page = new Page<>(src.getCurrent(), src.getSize(), src.searchCount());
        page.setTotal(src.getTotal());
        page.setRecords(toVoList(src.getRecords()));
        return page;
    }

    private List<MaintenanceVO> toVoList(List<DmsVehicleMaintenance> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, DmsVehicle> vehicleMap = loadVehicles(rows);
        Map<Long, String> handlerNames = loadHandlerNames(rows);
        Map<Long, Integer> beforeMileage = loadBeforeMileage(rows);

        List<MaintenanceVO> list = new ArrayList<>(rows.size());
        for (DmsVehicleMaintenance row : rows) {
            MaintenanceVO vo = new MaintenanceVO();
            vo.setId(row.getId());
            vo.setMaintNo(row.getMaintNo());
            vo.setVehicleId(row.getVehicleId());
            DmsVehicle v = vehicleMap.get(row.getVehicleId());
            if (v != null) {
                vo.setPlateNo(v.getPlateNo());
                vo.setVehicleBrand(v.getBrand());
                vo.setVehicleModel(v.getModel());
                vo.setVehicleType(v.getVehicleType());
            }
            vo.setMaintType(row.getMaintType());
            vo.setMaintTypeText(MaintTypeEnum.textOf(row.getMaintType()));
            vo.setMaintDate(row.getMaintDate());
            vo.setMaintContent(row.getMaintContent());
            vo.setMaintCost(row.getMaintCost());
            vo.setMaintVendor(row.getMaintVendor());
            vo.setVendorId(row.getVendorId());
            vo.setMaintContact(row.getMaintContact());
            vo.setMaintPhone(row.getMaintPhone());
            vo.setBeforeMaintMileage(beforeMileage.get(row.getId()));
            vo.setAfterMaintMileage(row.getAfterMaintMileage());
            vo.setNextMaintDate(row.getNextMaintDate());
            vo.setNextMaintMileage(row.getNextMaintMileage());
            vo.setAttachmentUrls(row.getAttachmentUrls());
            vo.setRemark(row.getRemark());
            vo.setHandlerName(handlerNames.get(row.getCreateBy()));
            vo.setCreateTime(row.getCreateTime());
            vo.setUpdateTime(row.getUpdateTime());
            fillDue(vo, v);
            list.add(vo);
        }
        return list;
    }

    /** 到期口径：剩余天数 / 剩余公里 / 到期状态 */
    private void fillDue(MaintenanceVO vo, DmsVehicle vehicle) {
        Integer remainDays = null;
        Integer remainKm = null;
        if (vo.getNextMaintDate() != null) {
            remainDays = (int) (vo.getNextMaintDate().toEpochDay() - LocalDate.now().toEpochDay());
        }
        if (vo.getNextMaintMileage() != null && vehicle != null && vehicle.getCurrentMileage() != null) {
            remainKm = vo.getNextMaintMileage() - vehicle.getCurrentMileage();
        }
        vo.setRemainDays(remainDays);
        vo.setRemainKm(remainKm);
        if (remainDays == null && remainKm == null) {
            vo.setDueStatus("NONE");
        } else if ((remainDays != null && remainDays < 0) || (remainKm != null && remainKm <= 0)) {
            vo.setDueStatus("OVERDUE");
        } else if ((remainDays != null && remainDays <= 30) || (remainKm != null && remainKm <= 1000)) {
            vo.setDueStatus("DUE_SOON");
        } else {
            vo.setDueStatus("NORMAL");
        }
    }

    private Map<Long, DmsVehicle> loadVehicles(List<DmsVehicleMaintenance> rows) {
        Set<Long> ids = rows.stream().map(DmsVehicleMaintenance::getVehicleId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return vehicleMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DmsVehicle::getId, v -> v, (a, b) -> a));
    }

    private Map<Long, String> loadHandlerNames(List<DmsVehicleMaintenance> rows) {
        Set<Long> ids = rows.stream().map(DmsVehicleMaintenance::getCreateBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(ids)) {
            String name = StringUtils.hasText(user.getRealName()) ? user.getRealName()
                    : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            names.put(user.getId(), name);
        }
        return names;
    }

    /** 维保前里程 = 同车上一笔维保的「维保后里程」（首笔为空） */
    private Map<Long, Integer> loadBeforeMileage(List<DmsVehicleMaintenance> rows) {
        Set<Long> vehicleIds = rows.stream().map(DmsVehicleMaintenance::getVehicleId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DmsVehicleMaintenance> all = maintenanceMapper.selectList(
                new LambdaQueryWrapper<DmsVehicleMaintenance>()
                        .select(DmsVehicleMaintenance::getId, DmsVehicleMaintenance::getVehicleId,
                                DmsVehicleMaintenance::getMaintDate, DmsVehicleMaintenance::getAfterMaintMileage)
                        .in(DmsVehicleMaintenance::getVehicleId, vehicleIds)
                        .orderByAsc(DmsVehicleMaintenance::getMaintDate)
                        .orderByAsc(DmsVehicleMaintenance::getId));
        Map<Long, Integer> result = new HashMap<>();
        Map<Long, Integer> lastAfter = new HashMap<>();
        for (DmsVehicleMaintenance row : all) {
            Integer prev = lastAfter.get(row.getVehicleId());
            if (prev != null) {
                result.put(row.getId(), prev);
            }
            if (row.getAfterMaintMileage() != null) {
                lastAfter.put(row.getVehicleId(), row.getAfterMaintMileage());
            }
        }
        return result;
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
