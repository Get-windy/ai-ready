package cn.aiedge.dms.vehicle.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.EnergyPayModeEnum;
import cn.aiedge.dms.common.enums.EnergyTypeEnum;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.mapper.DmsConfigMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.vehicle.dto.EnergyLogCreateDTO;
import cn.aiedge.dms.vehicle.dto.EnergyLogQuery;
import cn.aiedge.dms.vehicle.dto.EnergyLogVO;
import cn.aiedge.dms.vehicle.dto.EnergyStatsVO;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.entity.DmsVehicleEnergyCard;
import cn.aiedge.dms.vehicle.entity.DmsVehicleEnergyLog;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleEnergyLogMapper;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 补能流水服务（加油 / 充电 / 加气 / 换电）
 *
 * <p>核心口径（业界 fuel log 的通用做法）：补能记录必须与**里程**绑定，才能算出
 * 「区间里程」与「每公里成本」，进而横向比较燃油车与电动车的真实使用成本。</p>
 *
 * <ul>
 *   <li>{@code mileageSinceLast} = 本次仪表里程 − 上次同主体补能里程（自动）</li>
 *   <li>{@code unitCost} = 金额 ÷ 区间里程（自动）</li>
 *   <li>异常判定：里程倒挂 / 百公里油耗超阈值 / 每公里成本超阈值 / 金额非法（阈值租户可配）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleEnergyLogService {

    private final DmsVehicleEnergyLogMapper energyLogMapper;
    private final DmsVehicleMapper vehicleMapper;
    private final DmsRiderMapper riderMapper;
    private final DmsConfigMapper configMapper;
    /** 补能卡稽核（一卡一车一人 / 过期卡 / 停用卡 / 额度） */
    private final VehicleEnergyCardService energyCardService;

    /** 百公里油耗阈值参数键 */
    public static final String PARAM_MAX_PER_100KM = "energy.consumption.max.per100km";
    /** 每公里成本阈值参数键 */
    public static final String PARAM_MAX_UNIT_COST = "energy.unit.cost.max";
    /** 合规补能时段参数键（HH:mm-HH:mm） */
    public static final String PARAM_WORK_HOURS = "energy.workhours";

    private static final int EXPORT_LIMIT = 5000;

    // ==================== 查询 ====================

    public IPage<EnergyLogVO> page(EnergyLogQuery query) {
        Page<DmsVehicleEnergyLog> pageParam = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 20 : query.getSize());

        LambdaQueryWrapper<DmsVehicleEnergyLog> wrapper = new LambdaQueryWrapper<DmsVehicleEnergyLog>()
                .eq(query.getVehicleId() != null, DmsVehicleEnergyLog::getVehicleId, query.getVehicleId())
                .eq(query.getRiderId() != null, DmsVehicleEnergyLog::getRiderId, query.getRiderId())
                .eq(query.getEnergyType() != null, DmsVehicleEnergyLog::getEnergyType, query.getEnergyType())
                .eq(query.getPayMode() != null, DmsVehicleEnergyLog::getPayMode, query.getPayMode())
                .eq(Boolean.TRUE.equals(query.getAbnormalOnly()), DmsVehicleEnergyLog::getAbnormalFlag, 1)
                .ge(query.getStartDate() != null, DmsVehicleEnergyLog::getOccurredAt,
                        query.getStartDate() != null ? query.getStartDate().atStartOfDay() : null)
                .le(query.getEndDate() != null, DmsVehicleEnergyLog::getOccurredAt,
                        query.getEndDate() != null ? LocalDateTime.of(query.getEndDate(), java.time.LocalTime.MAX) : null)
                .orderByDesc(DmsVehicleEnergyLog::getOccurredAt)
                .orderByDesc(DmsVehicleEnergyLog::getId);

        if ("VEHICLE".equalsIgnoreCase(query.getSubjectType())) {
            wrapper.isNotNull(DmsVehicleEnergyLog::getVehicleId);
        } else if ("RIDER".equalsIgnoreCase(query.getSubjectType())) {
            wrapper.isNotNull(DmsVehicleEnergyLog::getRiderId);
        }

        // 关键字：车牌/姓名/站点/卡号 —— 车与人均需回查，故先解析出主体ID集合再过滤
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            List<Long> vehicleIds = vehicleMapper.selectList(new LambdaQueryWrapper<DmsVehicle>()
                            .like(DmsVehicle::getPlateNo, kw))
                    .stream().map(DmsVehicle::getId).collect(Collectors.toList());
            List<Long> riderIds = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                            .like(DmsRider::getRealName, kw))
                    .stream().map(DmsRider::getId).collect(Collectors.toList());
            wrapper.and(w -> {
                w.like(DmsVehicleEnergyLog::getStation, kw)
                        .or().like(DmsVehicleEnergyLog::getCardNo, kw);
                if (!vehicleIds.isEmpty()) {
                    w.or().in(DmsVehicleEnergyLog::getVehicleId, vehicleIds);
                }
                if (!riderIds.isEmpty()) {
                    w.or().in(DmsVehicleEnergyLog::getRiderId, riderIds);
                }
            });
        }

        IPage<DmsVehicleEnergyLog> raw = energyLogMapper.selectPage(pageParam, wrapper);
        return raw.convert(this::toVo);
    }

    public EnergyLogVO detail(Long id) {
        DmsVehicleEnergyLog entity = energyLogMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("补能记录不存在: " + id);
        }
        return toVo(entity);
    }

    // ==================== 新增 / 修改 / 删除 ====================

    @Transactional(rollbackFor = Exception.class)
    public EnergyLogVO create(EnergyLogCreateDTO dto) {
        DmsVehicleEnergyLog entity = new DmsVehicleEnergyLog();
        applyDto(entity, dto);
        if (entity.getOccurredAt() == null) {
            entity.setOccurredAt(LocalDateTime.now());
        }
        computeDerived(entity, null);
        energyLogMapper.insert(entity);
        energyCardService.recomputeUsedQuota(entity.getCardNo());
        log.info("新增补能记录: id={}, 主体={}, 类型={}, 金额={}, 区间里程={}, 每公里成本={}",
                entity.getId(), subjectText(entity), entity.getEnergyType(), entity.getAmountYuan(),
                entity.getMileageSinceLast(), entity.getUnitCost());
        return toVo(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public EnergyLogVO update(Long id, EnergyLogCreateDTO dto) {
        DmsVehicleEnergyLog entity = energyLogMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("补能记录不存在: " + id);
        }
        applyDto(entity, dto);
        String oldCardNo = entity.getCardNo();
        computeDerived(entity, id);
        energyLogMapper.updateById(entity);
        energyCardService.recomputeUsedQuota(oldCardNo);
        if (!java.util.Objects.equals(oldCardNo, entity.getCardNo())) {
            energyCardService.recomputeUsedQuota(entity.getCardNo());
        }
        return toVo(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsVehicleEnergyLog entity = energyLogMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("补能记录不存在: " + id);
        }
        energyLogMapper.deleteById(id);
        energyCardService.recomputeUsedQuota(entity.getCardNo());
        log.info("删除补能记录: id={}", id);
    }

    /** 批量删除 */
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return energyLogMapper.delete(new LambdaQueryWrapper<DmsVehicleEnergyLog>()
                .in(DmsVehicleEnergyLog::getId, ids));
    }

    // ==================== 导出（真实 xlsx） ====================

    public void export(EnergyLogQuery query, HttpServletResponse response) throws IOException {
        EnergyLogQuery q = query == null ? new EnergyLogQuery() : query;
        q.setPage(1);
        q.setSize(EXPORT_LIMIT);
        List<EnergyLogVO> rows = page(q).getRecords();

        String fileName = "车辆补能_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"补能时间", "主体类型", "车牌号/配送员", "补能类型", "支付方式", "数量", "单价",
                "金额(元)", "仪表里程(km)", "区间里程(km)", "每公里成本(元)", "站点/商户", "卡号/套餐",
                "是否异常", "异常原因", "备注"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("车辆补能");
            CellStyle headStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 16 * 256);
            }
            int rowIdx = 1;
            for (EnergyLogVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(str(vo.getOccurredAt()));
                row.createCell(c++).setCellValue("RIDER".equals(vo.getSubjectType()) ? "骑手两轮车" : "四轮车");
                row.createCell(c++).setCellValue(str(vo.getSubjectName()));
                row.createCell(c++).setCellValue(str(vo.getEnergyTypeText()));
                row.createCell(c++).setCellValue(str(vo.getPayModeText()));
                row.createCell(c++).setCellValue(num(vo.getQuantity()));
                row.createCell(c++).setCellValue(num(vo.getUnitPrice()));
                row.createCell(c++).setCellValue(num(vo.getAmountYuan()));
                setCell(row, c++, vo.getOdometer());
                setCell(row, c++, vo.getMileageSinceLast());
                row.createCell(c++).setCellValue(num(vo.getUnitCost()));
                row.createCell(c++).setCellValue(str(vo.getStation()));
                row.createCell(c++).setCellValue(str(vo.getCardNo()));
                row.createCell(c++).setCellValue(Integer.valueOf(1).equals(vo.getAbnormalFlag()) ? "异常" : "正常");
                row.createCell(c++).setCellValue(str(vo.getAbnormalReason()));
                row.createCell(c++).setCellValue(str(vo.getRemark()));
            }
            workbook.write(response.getOutputStream());
        }
    }

    // ==================== 能耗报表（P2） ====================

    /** CO₂ 排放因子（kg/单位）：汽油 L、柴油 L、电 kWh、加气 L（近似） */
    private static final BigDecimal CO2_GASOLINE = new BigDecimal("2.30");
    private static final BigDecimal CO2_DIESEL = new BigDecimal("2.63");
    private static final BigDecimal CO2_GAS = new BigDecimal("2.16");
    private static final BigDecimal CO2_POWER = new BigDecimal("0.581");

    /**
     * 能耗报表：汇总 + 油电对比 + 按主体/能源类型分组
     *
     * @param query 复用补能查询条件（日期/主体/车辆/骑手/类型）
     */
    public EnergyStatsVO stats(EnergyLogQuery query) {
        EnergyLogQuery q = query == null ? new EnergyLogQuery() : query;
        q.setPage(1);
        q.setSize(EXPORT_LIMIT);
        List<EnergyLogVO> rows = page(q).getRecords();

        EnergyStatsVO vo = new EnergyStatsVO();
        vo.setLogCount(rows.size());
        vo.setTotalAmount(rows.stream().map(EnergyLogVO::getAmountYuan).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setTotalQuantity(rows.stream().map(EnergyLogVO::getQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        int mileage = rows.stream().map(EnergyLogVO::getMileageSinceLast).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        vo.setTotalMileage(mileage);
        vo.setAvgUnitCost(mileage > 0
                ? vo.getTotalAmount().divide(BigDecimal.valueOf(mileage), 4, RoundingMode.HALF_UP)
                : null);
        vo.setTotalCo2Kg(rows.stream().map(this::co2Of).reduce(BigDecimal.ZERO, BigDecimal::add));

        vo.setFuelGroup(group(rows, "FUEL", "燃油/燃气", r -> EnergyTypeEnum.isFuel(r.getEnergyType())));
        vo.setPowerGroup(group(rows, "POWER", "电动（充电/换电）", r -> !EnergyTypeEnum.isFuel(r.getEnergyType())));
        vo.setByEnergyType(rows.stream()
                .collect(Collectors.groupingBy(r -> String.valueOf(r.getEnergyType())))
                .entrySet().stream()
                .map(e -> buildGroup(e.getKey(), EnergyTypeEnum.textOf(Integer.valueOf(e.getKey())), e.getValue()))
                .sorted((a, b2) -> b2.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList()));
        vo.setBySubject(rows.stream()
                .collect(Collectors.groupingBy(r -> "RIDER".equals(r.getSubjectType())
                        ? "R" + r.getRiderId() : "V" + r.getVehicleId()))
                .entrySet().stream()
                .map(e -> buildGroup(e.getKey(), e.getValue().get(0).getSubjectName(), e.getValue()))
                .sorted((a, b2) -> b2.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList()));
        return vo;
    }

    private EnergyStatsVO.EnergyGroup group(List<EnergyLogVO> rows, String key, String label,
                                            java.util.function.Predicate<EnergyLogVO> filter) {
        return buildGroup(key, label, rows.stream().filter(filter).collect(Collectors.toList()));
    }

    private EnergyStatsVO.EnergyGroup buildGroup(String key, String label, List<EnergyLogVO> rows) {
        EnergyStatsVO.EnergyGroup g = new EnergyStatsVO.EnergyGroup();
        g.setKey(key);
        g.setLabel(label);
        g.setLogCount(rows.size());
        g.setAmount(rows.stream().map(EnergyLogVO::getAmountYuan).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        g.setQuantity(rows.stream().map(EnergyLogVO::getQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        int span = rows.stream().map(EnergyLogVO::getMileageSinceLast).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        g.setMileage(span);
        g.setUnitCost(span > 0 ? g.getAmount().divide(BigDecimal.valueOf(span), 4, RoundingMode.HALF_UP) : null);
        g.setCo2Kg(rows.stream().map(this::co2Of).reduce(BigDecimal.ZERO, BigDecimal::add));
        return g;
    }

    /** 单条记录的 CO₂ 折算（数量 × 因子） */
    private BigDecimal co2Of(EnergyLogVO row) {
        if (row.getQuantity() == null || row.getEnergyType() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal factor = switch (EnergyTypeEnum.fromValue(row.getEnergyType())) {
            case GASOLINE -> CO2_GASOLINE;
            case DIESEL -> CO2_DIESEL;
            case GAS -> CO2_GAS;
            default -> CO2_POWER;
        };
        return row.getQuantity().multiply(factor);
    }

    // ==================== 派生计算（核心口径） ====================

    /**
     * 计算区间里程 / 每公里成本 / 异常标记
     *
     * @param entity    待保存记录
     * @param excludeId 修改场景需排除自身，避免把自己当成"上次记录"
     */
    private void computeDerived(DmsVehicleEnergyLog entity, Long excludeId) {
        validateSubject(entity);

        Integer odometer = entity.getOdometer();
        if (odometer != null) {
            DmsVehicleEnergyLog prev = findPrevious(entity, excludeId);
            if (prev != null && prev.getOdometer() != null) {
                int diff = odometer - prev.getOdometer();
                entity.setMileageSinceLast(diff > 0 ? diff : null);
                if (diff < 0) {
                    markAbnormal(entity, "本次里程(" + odometer + "km)小于上次补能里程(" + prev.getOdometer() + "km)");
                } else if (diff == 0) {
                    markAbnormal(entity, "本次里程与上次补能里程相同(" + odometer + "km)，无法核算区间成本");
                }
            } else {
                entity.setMileageSinceLast(null);
            }
        } else {
            entity.setMileageSinceLast(null);
        }

        // 每公里成本 = 金额 ÷ 区间里程
        BigDecimal amount = entity.getAmountYuan();
        Integer span = entity.getMileageSinceLast();
        if (amount != null && span != null && span > 0) {
            entity.setUnitCost(amount.divide(BigDecimal.valueOf(span), 4, RoundingMode.HALF_UP));
        } else {
            // 无里程无法分摊时，退回「按数量算单价」的展示口径，不臆造每公里成本
            entity.setUnitCost(null);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            markAbnormal(entity, "补能金额非法（" + (amount == null ? "空" : amount + "元") + "）");
        }

        // 燃油车百公里油耗（L/100km）
        if (EnergyTypeEnum.isFuel(entity.getEnergyType()) && entity.getQuantity() != null && span != null && span > 0) {
            BigDecimal per100 = fuelPer100Km(entity.getQuantity(), span);
            int max = paramInt(entity.getTenantId(), PARAM_MAX_PER_100KM, 30);
            if (per100.compareTo(BigDecimal.valueOf(max)) > 0) {
                markAbnormal(entity, "百公里油耗 " + per100 + "L 超过阈值 " + max + "L");
            }
        }

        // 每公里成本上限
        if (entity.getUnitCost() != null) {
            BigDecimal max = BigDecimal.valueOf(paramInt(entity.getTenantId(), PARAM_MAX_UNIT_COST, 5));
            if (entity.getUnitCost().compareTo(max) > 0) {
                markAbnormal(entity, "每公里成本 " + entity.getUnitCost() + "元 超过阈值 " + max + "元");
            }
        }

        // ── 用卡稽核（一卡一车一人 / 卡状态 / 有效期 / 额度）──
        auditCard(entity, excludeId);

        // ── 非合规时段补能 ──
        auditWorkHours(entity);

        if (entity.getAbnormalFlag() == null) {
            entity.setAbnormalFlag(0);
        }
    }

    /**
     * 用卡稽核（业界 fuel card 管理的核心风控）
     *
     * <ul>
     *   <li>卡号未建档 → 提示（不阻断，允许路边快修先用卡号记账）</li>
     *   <li><b>一卡一车一人</b>：用卡主体 ≠ 卡绑定主体 → 异常</li>
     *   <li>卡停用 / 已过期 → 异常</li>
     *   <li>套餐额度超限 → 异常</li>
     * </ul>
     */
    private void auditCard(DmsVehicleEnergyLog entity, Long excludeId) {
        if (!StringUtils.hasText(entity.getCardNo())) {
            return;
        }
        DmsVehicleEnergyCard card = energyCardService.findByCardNo(entity.getCardNo());
        if (card == null) {
            markAbnormal(entity, "卡号「" + entity.getCardNo() + "」未建档，无法稽核持卡主体与额度");
            return;
        }
        if (Integer.valueOf(0).equals(card.getStatus())) {
            markAbnormal(entity, "使用已停用卡「" + card.getCardNo() + "」");
        }
        LocalDate occurDate = entity.getOccurredAt() == null ? LocalDate.now() : entity.getOccurredAt().toLocalDate();
        if (card.getStartDate() != null && occurDate.isBefore(card.getStartDate())) {
            markAbnormal(entity, "补能日期早于卡生效日期 " + card.getStartDate());
        }
        if (card.getExpireDate() != null && occurDate.isAfter(card.getExpireDate())) {
            markAbnormal(entity, "使用已过期卡（有效期至 " + card.getExpireDate() + "）");
        }
        boolean sameSubject = (card.getVehicleId() != null && card.getVehicleId().equals(entity.getVehicleId()))
                || (card.getRiderId() != null && card.getRiderId().equals(entity.getRiderId()));
        if (!sameSubject) {
            markAbnormal(entity, "一卡一车一人校验失败：卡绑定主体与本次补能主体不一致");
        }
        if (card.getQuota() != null && card.getQuota().compareTo(BigDecimal.ZERO) > 0) {
            // 额度按「除本笔以外的已用 + 本笔」判定（本笔之后是否超限），否则新增永远漏判
            BigDecimal used = energyCardService.sumUsedQuota(card, excludeId)
                    .add(entity.getQuantity() == null ? BigDecimal.ZERO : entity.getQuantity());
            if (used.compareTo(card.getQuota()) > 0) {
                markAbnormal(entity, "套餐额度超限：已用 " + used.stripTrailingZeros().toPlainString()
                        + " / 额度 " + card.getQuota().stripTrailingZeros().toPlainString());
            }
        }
    }

    /** 非合规时段补能（默认 06:00-23:00，租户可配） */
    private void auditWorkHours(DmsVehicleEnergyLog entity) {
        if (entity.getOccurredAt() == null) {
            return;
        }
        String window = paramString(entity.getTenantId(), PARAM_WORK_HOURS, "06:00-23:00");
        try {
            String[] parts = window.split("-");
            if (parts.length != 2) {
                return;
            }
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            LocalTime at = entity.getOccurredAt().toLocalTime();
            boolean inWindow = start.isBefore(end)
                    ? (!at.isBefore(start) && !at.isAfter(end))
                    : (!at.isBefore(start) || !at.isAfter(end));
            if (!inWindow) {
                markAbnormal(entity, "非合规时段补能（" + at.format(DateTimeFormatter.ofPattern("HH:mm"))
                        + "，允许时段 " + window + "）");
            }
        } catch (Exception e) {
            log.debug("补能时段参数解析失败，跳过该规则: {}", window);
        }
    }

    /** 读取租户字符串参数 */
    private String paramString(Long tenantId, String key, String defaultVal) {
        try {
            LambdaQueryWrapper<DmsConfig> wrapper = new LambdaQueryWrapper<DmsConfig>()
                    .eq(DmsConfig::getConfigKey, key)
                    .orderByAsc(DmsConfig::getTenantId)
                    .last("LIMIT 1");
            if (tenantId != null) {
                wrapper = new LambdaQueryWrapper<DmsConfig>()
                        .eq(DmsConfig::getConfigKey, key)
                        .eq(DmsConfig::getTenantId, tenantId)
                        .last("LIMIT 1");
            }
            DmsConfig config = configMapper.selectOne(wrapper);
            return config == null || config.getConfigValue() == null ? defaultVal : config.getConfigValue().trim();
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /** 上次同主体补能记录（按补能时间倒序；时间相同则按 id 倒序） */
    private DmsVehicleEnergyLog findPrevious(DmsVehicleEnergyLog entity, Long excludeId) {
        LambdaQueryWrapper<DmsVehicleEnergyLog> wrapper = new LambdaQueryWrapper<DmsVehicleEnergyLog>()
                .isNotNull(DmsVehicleEnergyLog::getOdometer)
                .ne(excludeId != null, DmsVehicleEnergyLog::getId, excludeId)
                .le(entity.getOccurredAt() != null, DmsVehicleEnergyLog::getOccurredAt, entity.getOccurredAt())
                .lt(entity.getOccurredAt() == null, DmsVehicleEnergyLog::getId, Long.MAX_VALUE);
        if (entity.getVehicleId() != null) {
            wrapper.eq(DmsVehicleEnergyLog::getVehicleId, entity.getVehicleId());
        } else {
            wrapper.eq(DmsVehicleEnergyLog::getRiderId, entity.getRiderId());
        }
        wrapper.orderByDesc(DmsVehicleEnergyLog::getOccurredAt)
                .orderByDesc(DmsVehicleEnergyLog::getId)
                .last("LIMIT 1");
        return energyLogMapper.selectOne(wrapper);
    }

    private void markAbnormal(DmsVehicleEnergyLog entity, String reason) {
        entity.setAbnormalFlag(1);
        String old = entity.getAbnormalReason();
        entity.setAbnormalReason(StringUtils.hasText(old) ? old + "；" + reason : reason);
    }

    /** 归属校验：车/人二选一，且主体必须存在 */
    private void validateSubject(DmsVehicleEnergyLog entity) {
        if (entity.getVehicleId() == null && entity.getRiderId() == null) {
            throw BusinessException.badRequest("请选择补能主体：四轮车或配送员");
        }
        if (entity.getVehicleId() != null && entity.getRiderId() != null) {
            throw BusinessException.badRequest("补能主体只能二选一：四轮车 或 配送员");
        }
        if (entity.getVehicleId() != null && vehicleMapper.selectById(entity.getVehicleId()) == null) {
            throw BusinessException.notFound("车辆不存在: " + entity.getVehicleId());
        }
        if (entity.getRiderId() != null && riderMapper.selectById(entity.getRiderId()) == null) {
            throw BusinessException.notFound("配送员不存在: " + entity.getRiderId());
        }
    }

    private void applyDto(DmsVehicleEnergyLog entity, EnergyLogCreateDTO dto) {
        entity.setVehicleId(dto.getVehicleId());
        entity.setRiderId(dto.getRiderId());
        entity.setEnergyType(dto.getEnergyType());
        entity.setPayMode(dto.getPayMode() == null ? EnergyPayModeEnum.CASH.getValue() : dto.getPayMode());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setAmountYuan(dto.getAmountYuan());
        entity.setOdometer(dto.getOdometer());
        entity.setStation(dto.getStation());
        entity.setOccurredAt(dto.getOccurredAt());
        entity.setCardNo(dto.getCardNo());
        entity.setVoucherUrl(dto.getVoucherUrl());
        entity.setRemark(dto.getRemark());
        // 派生字段每次重算，避免旧值残留
        entity.setAbnormalFlag(0);
        entity.setAbnormalReason(null);
    }

    private EnergyLogVO toVo(DmsVehicleEnergyLog entity) {
        EnergyLogVO vo = new EnergyLogVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setEnergyTypeText(EnergyTypeEnum.textOf(entity.getEnergyType()));
        vo.setPayModeText(EnergyPayModeEnum.textOf(entity.getPayMode()));
        // 百公里油耗是派生值（可由数量与区间里程还原），不落库
        vo.setFuelPer100Km(fuelPer100Km(entity.getQuantity(), entity.getMileageSinceLast()));
        if (entity.getVehicleId() != null) {
            vo.setSubjectType("VEHICLE");
            DmsVehicle vehicle = vehicleMapper.selectById(entity.getVehicleId());
            vo.setPlateNo(vehicle == null ? null : vehicle.getPlateNo());
            vo.setSubjectName(vehicle == null ? null : vehicle.getPlateNo());
        } else {
            vo.setSubjectType("RIDER");
            DmsRider rider = riderMapper.selectById(entity.getRiderId());
            vo.setRiderName(rider == null ? null : rider.getRealName());
            vo.setSubjectName(rider == null ? null : rider.getRealName());
        }
        return vo;
    }

    private String subjectText(DmsVehicleEnergyLog entity) {
        return entity.getVehicleId() != null ? "vehicle:" + entity.getVehicleId() : "rider:" + entity.getRiderId();
    }

    /** 读取租户整型参数（缺省回退 defaultVal，绝不抛异常） */
    private int paramInt(Long tenantId, String key, int defaultVal) {
        try {
            LambdaQueryWrapper<DmsConfig> wrapper = new LambdaQueryWrapper<DmsConfig>()
                    .eq(DmsConfig::getConfigKey, key)
                    .orderByAsc(DmsConfig::getTenantId)
                    .last("LIMIT 1");
            if (tenantId != null) {
                wrapper = new LambdaQueryWrapper<DmsConfig>()
                        .eq(DmsConfig::getConfigKey, key)
                        .eq(DmsConfig::getTenantId, tenantId)
                        .last("LIMIT 1");
            }
            DmsConfig config = configMapper.selectOne(wrapper);
            if (config == null || config.getConfigValue() == null) {
                return defaultVal;
            }
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /** 百公里油耗( L/100km )：派生值，不落库 */
    private static BigDecimal fuelPer100Km(BigDecimal quantity, Integer spanKm) {
        if (quantity == null || spanKm == null || spanKm <= 0) {
            return null;
        }
        return quantity.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(spanKm), 2, RoundingMode.HALF_UP);
    }

    private static String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return String.valueOf(value);
    }

    private static String num(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static void setCell(Row row, int idx, Integer value) {
        if (value == null) {
            row.createCell(idx).setCellValue("");
        } else {
            row.createCell(idx).setCellValue(value);
        }
    }
}
