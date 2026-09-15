package cn.aiedge.dms.vehicle.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.VehicleStatusEnum;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import cn.aiedge.dms.vehicle.dto.VehicleCertExpiryVO;
import cn.aiedge.dms.vehicle.dto.VehicleDTO;
import cn.aiedge.dms.vehicle.dto.VehicleQueryDTO;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.enums.BindingStatusEnum;
import cn.aiedge.dms.verification.mapper.DmsRiderVehicleBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
 * 车辆档案服务（配送 → 人车管理 → 车辆管理）
 *
 * 业务口径见《车辆管理开发文档》：
 *   · §3.2 状态机：空闲(0) ⇄ 使用中(1)；使用中 → 维修中(2) → 空闲(0)；任意 → 已报废(3)（终态）；4-已出勤=出车未接单。
 *   · §3.3 人车一对一：绑定/解绑记流水（复用 `dms_rider_vehicle_binding`，不另建表）。
 *   · §3.5 证件到期强提醒：保险 / 年检 / 营运证 三证统一到期视图。
 *
 * 注意：本服务不直接修改 `deleted` 之外的逻辑列；逻辑删除必须走 UpdateWrapper（`updateById` 会剔除 @TableLogic 列）。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    /** 车辆编码号段前缀（VH001、VH002 …） */
    private static final String CODE_PREFIX = "VH";

    /** 「在途任务」状态：已分配 / 已接单 / 取货中 / 配送中 */
    private static final List<Integer> IN_TRANSIT_TASK_STATUS = List.of(1, 2, 3, 4);

    private static final Map<Integer, String> VEHICLE_TYPE_TEXT = Map.of(
            1, "电动车", 2, "小货车", 3, "面包车", 4, "厢式货车", 5, "冷藏车", 6, "三轮车");

    private static final Map<Integer, String> OWNERSHIP_TEXT = Map.of(
            1, "公司自有", 2, "个人自带", 3, "租赁");

    private static final Map<String, String> CERT_TYPE_TEXT = Map.of(
            "INSURANCE", "保险", "INSPECTION", "年检", "PERMIT", "营运证");

    private final DmsVehicleMapper vehicleMapper;
    private final DmsRiderVehicleBindingMapper bindingMapper;
    private final DmsRiderMapper riderMapper;
    private final DmsTaskMapper taskMapper;

    // ==================== 查询 ====================

    public Page<VehicleDTO> page(VehicleQueryDTO query) {
        VehicleQueryDTO q = query != null ? query : new VehicleQueryDTO();
        int pageNum = q.getPageNum() == null || q.getPageNum() < 1 ? 1 : q.getPageNum();
        int pageSize = q.getPageSize() == null || q.getPageSize() < 1 ? 20 : q.getPageSize();

        Page<DmsVehicle> entityPage = vehicleMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(q));
        Page<VehicleDTO> result = new Page<>(pageNum, pageSize, entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toDTO).toList());
        return result;
    }

    /** 导出用全量列表（不分页） */
    public List<VehicleDTO> list(VehicleQueryDTO query) {
        return vehicleMapper.selectList(buildWrapper(query != null ? query : new VehicleQueryDTO()))
                .stream().map(this::toDTO).toList();
    }

    public VehicleDTO getById(Long id) {
        DmsVehicle entity = vehicleMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("车辆不存在: " + id);
        }
        return toDTO(entity);
    }

    /** 选择器数据源（调度指派 / 人车绑定 / 线路共用）：排除已报废车辆 */
    public List<VehicleDTO> options() {
        return vehicleMapper.selectList(new LambdaQueryWrapper<DmsVehicle>()
                        .ne(DmsVehicle::getStatus, VehicleStatusEnum.SCRAPPED.getValue())
                        .orderByAsc(DmsVehicle::getPlateNo))
                .stream().map(this::toDTO).toList();
    }

    /** 生成下一个车辆编码（VH 号段，同租户内不重复） */
    public String nextCode() {
        List<DmsVehicle> rows = vehicleMapper.selectList(new LambdaQueryWrapper<DmsVehicle>()
                .select(DmsVehicle::getVehicleCode)
                .likeRight(DmsVehicle::getVehicleCode, CODE_PREFIX));
        Set<String> taken = rows.stream().map(DmsVehicle::getVehicleCode)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        for (int seq = 1; seq <= 9999; seq++) {
            String candidate = CODE_PREFIX + String.format("%03d", seq);
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
        throw BusinessException.badRequest("车辆编码数量已达上限");
    }

    /**
     * 证件到期清单（保险 / 年检 / 营运证三证统一视图）
     *
     * @param days     到期窗口天数（含已过期），缺省 30
     * @param certType INSURANCE / INSPECTION / PERMIT，缺省=全部证件
     */
    public List<VehicleCertExpiryVO> expiring(Integer days, String certType) {
        int window = days == null || days < 0 ? 30 : days;
        String type = StringUtils.hasText(certType) ? certType.trim().toUpperCase() : null;
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(window);

        List<DmsVehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<DmsVehicle>()
                .ne(DmsVehicle::getStatus, VehicleStatusEnum.SCRAPPED.getValue()));

        List<VehicleCertExpiryVO> rows = new ArrayList<>();
        for (DmsVehicle v : vehicles) {
            addCertRow(rows, v, "INSURANCE", v.getInsuranceExpireDate(), type, limit, today);
            addCertRow(rows, v, "INSPECTION", v.getInspectionExpireDate(), type, limit, today);
            addCertRow(rows, v, "PERMIT", v.getOperatingPermitExpireDate(), type, limit, today);
        }
        rows.sort(Comparator.comparing(VehicleCertExpiryVO::getDaysLeft)
                .thenComparing(VehicleCertExpiryVO::getPlateNo, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private void addCertRow(List<VehicleCertExpiryVO> rows, DmsVehicle v, String certType, LocalDate certDate,
                            String filterType, LocalDate limit, LocalDate today) {
        if (certDate == null || (filterType != null && !filterType.equals(certType)) || certDate.isAfter(limit)) {
            return;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, certDate);
        VehicleCertExpiryVO row = new VehicleCertExpiryVO();
        row.setVehicleId(v.getId());
        row.setVehicleCode(v.getVehicleCode());
        row.setPlateNo(v.getPlateNo());
        row.setCertType(certType);
        row.setCertTypeText(CERT_TYPE_TEXT.getOrDefault(certType, certType));
        row.setCertDate(certDate);
        row.setDaysLeft(daysLeft);
        row.setWarnLevel(daysLeft < 0 ? "EXPIRED" : daysLeft <= 7 ? "WARNING" : "NORMAL");
        row.setStatus(v.getStatus());
        row.setStatusText(statusText(v.getStatus()));
        row.setCurrentRiderName(v.getCurrentRiderName());
        row.setOwnershipTypeText(textOf(OWNERSHIP_TEXT, v.getOwnershipType()));
        row.setDepartment(v.getDepartment());
        rows.add(row);
    }

    /** 查询条件装配（对标金标准查询项：关键词/类型/归属/状态/证件到期/配送员/部门/注册日期） */
    private LambdaQueryWrapper<DmsVehicle> buildWrapper(VehicleQueryDTO q) {
        LambdaQueryWrapper<DmsVehicle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(DmsVehicle::getPlateNo, kw)
                    .or().like(DmsVehicle::getVehicleCode, kw)
                    .or().like(DmsVehicle::getBrand, kw)
                    .or().like(DmsVehicle::getModel, kw)
                    .or().like(DmsVehicle::getCurrentRiderName, kw));
        }
        wrapper.eq(q.getVehicleType() != null, DmsVehicle::getVehicleType, q.getVehicleType());
        wrapper.eq(q.getOwnershipType() != null, DmsVehicle::getOwnershipType, q.getOwnershipType());
        wrapper.eq(q.getStatus() != null, DmsVehicle::getStatus, q.getStatus());
        wrapper.like(StringUtils.hasText(q.getCurrentRiderName()), DmsVehicle::getCurrentRiderName,
                q.getCurrentRiderName() == null ? null : q.getCurrentRiderName().trim());
        wrapper.like(StringUtils.hasText(q.getDepartment()), DmsVehicle::getDepartment,
                q.getDepartment() == null ? null : q.getDepartment().trim());
        wrapper.ge(q.getRegisterDateStart() != null, DmsVehicle::getRegisterDate, q.getRegisterDateStart());
        wrapper.le(q.getRegisterDateEnd() != null, DmsVehicle::getRegisterDate, q.getRegisterDateEnd());

        if (q.getExpiringDays() != null) {
            LocalDate limit = LocalDate.now().plusDays(Math.max(q.getExpiringDays(), 0));
            String certType = StringUtils.hasText(q.getCertType()) ? q.getCertType().trim().toUpperCase() : null;
            wrapper.and(w -> {
                if (certType == null || "INSURANCE".equals(certType)) {
                    w.le(DmsVehicle::getInsuranceExpireDate, limit);
                }
                if (certType == null || "INSPECTION".equals(certType)) {
                    if (certType == null) {
                        w.or().le(DmsVehicle::getInspectionExpireDate, limit);
                    } else {
                        w.le(DmsVehicle::getInspectionExpireDate, limit);
                    }
                }
                if (certType == null || "PERMIT".equals(certType)) {
                    if (certType == null) {
                        w.or().le(DmsVehicle::getOperatingPermitExpireDate, limit);
                    } else {
                        w.le(DmsVehicle::getOperatingPermitExpireDate, limit);
                    }
                }
            });
        }
        applySort(wrapper, q);
        return wrapper;
    }

    /** 服务端排序（白名单列，避免任意字段注入） */
    private void applySort(LambdaQueryWrapper<DmsVehicle> wrapper, VehicleQueryDTO q) {
        boolean asc = "asc".equalsIgnoreCase(q.getSortOrder());
        String field = q.getSortField() == null ? "" : q.getSortField();
        switch (field) {
            case "plateNo" -> wrapper.orderBy(true, asc, DmsVehicle::getPlateNo);
            case "vehicleCode" -> wrapper.orderBy(true, asc, DmsVehicle::getVehicleCode);
            case "registerDate" -> wrapper.orderBy(true, asc, DmsVehicle::getRegisterDate);
            case "insuranceExpireDate" -> wrapper.orderBy(true, asc, DmsVehicle::getInsuranceExpireDate);
            case "currentMileage" -> wrapper.orderBy(true, asc, DmsVehicle::getCurrentMileage);
            case "status" -> wrapper.orderBy(true, asc, DmsVehicle::getStatus);
            case "createTime" -> wrapper.orderBy(true, asc, DmsVehicle::getCreateTime);
            default -> wrapper.orderByDesc(DmsVehicle::getCreateTime);
        }
    }

    // ==================== 写操作 ====================

    @Transactional(rollbackFor = Exception.class)
    public VehicleDTO create(VehicleDTO dto) {
        String plateNo = dto.getPlateNo() == null ? "" : dto.getPlateNo().trim();
        assertPlateNoAvailable(plateNo, null);

        String code = dto.getVehicleCode() == null ? "" : dto.getVehicleCode().trim();
        if (!StringUtils.hasText(code)) {
            code = nextCode();
        } else {
            assertCodeAvailable(code, null);
        }

        DmsVehicle entity = new DmsVehicle();
        applyToEntity(entity, dto);
        entity.setPlateNo(plateNo);
        entity.setVehicleCode(code);
        entity.setStatus(VehicleStatusEnum.IDLE.getValue());
        entity.setCurrentMileage(0);
        vehicleMapper.insert(entity);
        log.info("新增车辆: id={}, code={}, plateNo={}", entity.getId(), entity.getVehicleCode(), entity.getPlateNo());
        return getById(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public VehicleDTO update(Long id, VehicleDTO dto) {
        DmsVehicle entity = vehicleMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("车辆不存在: " + id);
        }
        if (VehicleStatusEnum.SCRAPPED.getValue() == (entity.getStatus() == null ? -1 : entity.getStatus())) {
            throw BusinessException.badRequest("已报废车辆不可编辑，请先变更状态");
        }
        String plateNo = dto.getPlateNo() == null ? "" : dto.getPlateNo().trim();
        if (!plateNo.equals(entity.getPlateNo())) {
            assertPlateNoAvailable(plateNo, id);
        }
        // ⚠️ 必须走 UpdateWrapper 显式 SET：MyBatis-Plus 默认 NOT_NULL 策略会丢弃 null，
        //    否则「清空车主/备注/到期日」等操作静默不落库（表单回显仍为旧值）。
        vehicleMapper.update(null, buildUpdateWrapper(id, plateNo, dto));
        log.info("修改车辆: id={}, plateNo={}", id, plateNo);
        return getById(id);
    }

    /** 全字段显式更新（含 null，用于支持「清空」语义） */
    private LambdaUpdateWrapper<DmsVehicle> buildUpdateWrapper(Long id, String plateNo, VehicleDTO dto) {
        return new LambdaUpdateWrapper<DmsVehicle>()
                .eq(DmsVehicle::getId, id)
                .set(DmsVehicle::getPlateNo, plateNo)
                .set(DmsVehicle::getVehicleType, dto.getVehicleType())
                .set(DmsVehicle::getBrand, dto.getBrand())
                .set(DmsVehicle::getModel, dto.getModel())
                .set(DmsVehicle::getColor, dto.getColor())
                .set(DmsVehicle::getVin, blankToNull(dto.getVin()))
                .set(DmsVehicle::getEngineNo, dto.getEngineNo())
                .set(DmsVehicle::getOwnershipType, dto.getOwnershipType())
                .set(DmsVehicle::getOwnerName, dto.getOwnerName())
                .set(DmsVehicle::getOwnerPhone, dto.getOwnerPhone())
                .set(DmsVehicle::getRatedLoad, dto.getRatedLoad())
                .set(DmsVehicle::getRatedPassenger, dto.getRatedPassenger())
                .set(DmsVehicle::getCargoVolume, dto.getCargoVolume())
                .set(DmsVehicle::getRegisterDate, dto.getRegisterDate())
                .set(DmsVehicle::getMaintenanceIntervalKm, dto.getMaintenanceIntervalKm())
                .set(DmsVehicle::getOperatingPermitNo, blankToNull(dto.getOperatingPermitNo()))
                .set(DmsVehicle::getOperatingPermitExpireDate, dto.getOperatingPermitExpireDate())
                .set(DmsVehicle::getInsuranceExpireDate, dto.getInsuranceExpireDate())
                .set(DmsVehicle::getInspectionExpireDate, dto.getInspectionExpireDate())
                .set(DmsVehicle::getDepartment, dto.getDepartment())
                .set(DmsVehicle::getRemark, dto.getRemark());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsVehicle entity = vehicleMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("车辆不存在: " + id);
        }
        assertDeletable(entity);
        // ⚠️ @TableLogic 列必须用 UpdateWrapper 显式 SET（updateById 会剔除逻辑删除列）
        vehicleMapper.update(null, new LambdaUpdateWrapper<DmsVehicle>()
                .eq(DmsVehicle::getId, id)
                .set(DmsVehicle::getDeleted, 1));
        log.info("删除车辆: id={}, plateNo={}", id, entity.getPlateNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        List<Long> targets = normalizeIds(ids);
        for (Long id : targets) {
            DmsVehicle entity = vehicleMapper.selectById(id);
            if (entity == null) {
                throw BusinessException.notFound("车辆不存在: " + id);
            }
            assertDeletable(entity);
        }
        vehicleMapper.update(null, new LambdaUpdateWrapper<DmsVehicle>()
                .in(DmsVehicle::getId, targets)
                .set(DmsVehicle::getDeleted, 1));
        log.info("批量删除车辆: count={}", targets.size());
        return targets.size();
    }

    /**
     * 状态变更（§3.2 状态机校验 + 在途任务/绑定约束）
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleDTO updateStatus(Long id, Integer status) {
        DmsVehicle entity = vehicleMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("车辆不存在: " + id);
        }
        VehicleStatusEnum target = VehicleStatusEnum.fromValue(status);
        if (target == null) {
            throw BusinessException.badRequest("非法车辆状态: " + status);
        }
        assertTransitionAllowed(entity, target);
        Integer from = entity.getStatus();
        entity.setStatus(target.getValue());
        vehicleMapper.updateById(entity);
        log.info("车辆状态变更: id={}, {} → {}", id, from, target.getDescription());
        return getById(id);
    }

    /** 批量状态变更（逐车校验状态机，任一不合法则整体拒绝） */
    @Transactional(rollbackFor = Exception.class)
    public int batchStatus(List<Long> ids, Integer status) {
        List<Long> targets = normalizeIds(ids);
        VehicleStatusEnum target = VehicleStatusEnum.fromValue(status);
        if (target == null) {
            throw BusinessException.badRequest("非法车辆状态: " + status);
        }
        for (Long id : targets) {
            DmsVehicle entity = vehicleMapper.selectById(id);
            if (entity == null) {
                throw BusinessException.notFound("车辆不存在: " + id);
            }
            assertTransitionAllowed(entity, target);
        }
        vehicleMapper.update(null, new LambdaUpdateWrapper<DmsVehicle>()
                .in(DmsVehicle::getId, targets)
                .set(DmsVehicle::getStatus, target.getValue()));
        log.info("批量车辆状态变更: count={}, target={}", targets.size(), target.getDescription());
        return targets.size();
    }

    /**
     * 绑定配送员（人车一对一：车只绑一人、人只绑一车；写绑定流水）
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleDTO bindRider(Long vehicleId, Long riderId, Integer mileage, String remark) {
        DmsVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + vehicleId);
        }
        if (VehicleStatusEnum.SCRAPPED.getValue() == (vehicle.getStatus() == null ? -1 : vehicle.getStatus())) {
            throw BusinessException.badRequest("已报废车辆不可绑定配送员");
        }
        if (VehicleStatusEnum.REPAIRING.getValue() == (vehicle.getStatus() == null ? -1 : vehicle.getStatus())) {
            throw BusinessException.badRequest("维修中车辆不可绑定配送员");
        }
        if (vehicle.getCurrentRiderId() != null) {
            throw BusinessException.badRequest("该车辆已绑定配送员「" + vehicle.getCurrentRiderName() + "」，请先解绑");
        }
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw BusinessException.badRequest("配送员不存在: " + riderId);
        }
        DmsRiderVehicleBinding riderActive = findActiveBindingByRider(riderId);
        if (riderActive != null) {
            throw BusinessException.badRequest("配送员「" + rider.getRealName() + "」已绑定车辆「"
                    + riderActive.getPlateNo() + "」，请先解绑");
        }

        DmsRiderVehicleBinding binding = new DmsRiderVehicleBinding();
        binding.setRiderId(riderId);
        binding.setRiderName(rider.getRealName());
        binding.setRiderPhone(rider.getPhone());
        binding.setVehicleId(vehicleId);
        binding.setPlateNo(vehicle.getPlateNo());
        binding.setBindTime(LocalDateTime.now());
        binding.setBindMileage(mileage != null ? mileage : vehicle.getCurrentMileage());
        binding.setBindReason(remark);
        binding.setStatus(BindingStatusEnum.ACTIVE.getValue());
        bindingMapper.insert(binding);

        vehicle.setCurrentRiderId(riderId);
        vehicle.setCurrentRiderName(rider.getRealName());
        if (mileage != null) {
            vehicle.setCurrentMileage(mileage);
        }
        // 出车绑定：空闲 → 已出勤（出车但未接单）；使用中保持不变
        if (VehicleStatusEnum.IDLE.getValue() == (vehicle.getStatus() == null ? -1 : vehicle.getStatus())) {
            vehicle.setStatus(VehicleStatusEnum.ON_DUTY.getValue());
        }
        vehicleMapper.updateById(vehicle);
        log.info("车辆绑定配送员: vehicleId={}, riderId={}, 流水号={}", vehicleId, riderId, binding.getId());
        return getById(vehicleId);
    }

    /**
     * 解绑配送员（结对流水：写入交车时间与交车里程）
     */
    @Transactional(rollbackFor = Exception.class)
    public VehicleDTO unbindRider(Long vehicleId, Integer mileage) {
        DmsVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + vehicleId);
        }
        if (vehicle.getCurrentRiderId() == null) {
            throw BusinessException.badRequest("该车辆当前未绑定配送员");
        }
        DmsRiderVehicleBinding binding = findActiveBindingByVehicle(vehicleId);
        if (binding != null) {
            binding.setHandoverTime(LocalDateTime.now());
            binding.setHandoverMileage(mileage != null ? mileage : vehicle.getCurrentMileage());
            binding.setStatus(BindingStatusEnum.HANDED_OVER.getValue());
            bindingMapper.updateById(binding);
        }
        // 交车：使用中 / 已出勤 → 空闲
        Integer st = vehicle.getStatus();
        boolean backToIdle = st != null
                && (st == VehicleStatusEnum.IN_USE.getValue() || st == VehicleStatusEnum.ON_DUTY.getValue());
        // ⚠️ 清空 currentRider* 必须显式 SET：updateById 的 NOT_NULL 策略会跳过 null 字段，
        //    导致「解绑后当前配送员仍显示」的假解绑。
        vehicleMapper.update(null, new LambdaUpdateWrapper<DmsVehicle>()
                .eq(DmsVehicle::getId, vehicleId)
                .set(DmsVehicle::getCurrentRiderId, null)
                .set(DmsVehicle::getCurrentRiderName, null)
                .set(mileage != null, DmsVehicle::getCurrentMileage, mileage)
                .set(backToIdle, DmsVehicle::getStatus, VehicleStatusEnum.IDLE.getValue()));
        log.info("车辆解绑配送员: vehicleId={}", vehicleId);
        return getById(vehicleId);
    }

    /** 绑定/解绑流水（按时间倒序，最多 100 条） */
    public List<DmsRiderVehicleBinding> bindingHistory(Long vehicleId) {
        return bindingMapper.selectList(new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                .eq(DmsRiderVehicleBinding::getVehicleId, vehicleId)
                .orderByDesc(DmsRiderVehicleBinding::getBindTime)
                .last("LIMIT 100"));
    }

    /** 更新当前里程（人工校正） */
    @Transactional(rollbackFor = Exception.class)
    public void updateMileage(Long id, Integer mileage) {
        DmsVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + id);
        }
        if (mileage == null || mileage < 0) {
            throw BusinessException.badRequest("里程数不能为负");
        }
        vehicle.setCurrentMileage(mileage);
        vehicleMapper.updateById(vehicle);
    }

    /**
     * 待保养提醒列表（保险 / 年检 / 营运证近 warnDays 天到期，或里程距上次保养超过 warnKm）
     */
    public Page<VehicleDTO> pageDueForMaintenance(Page<VehicleDTO> page, Integer warnKm) {
        int km = warnKm == null || warnKm < 0 ? 5000 : warnKm;
        LocalDate limit = LocalDate.now().plusDays(30);
        LambdaQueryWrapper<DmsVehicle> wrapper = new LambdaQueryWrapper<DmsVehicle>()
                .ne(DmsVehicle::getStatus, VehicleStatusEnum.SCRAPPED.getValue())
                .and(w -> w.le(DmsVehicle::getInsuranceExpireDate, limit)
                        .or().le(DmsVehicle::getInspectionExpireDate, limit)
                        .or().le(DmsVehicle::getOperatingPermitExpireDate, limit)
                        .or().apply("COALESCE(current_mileage,0) - COALESCE(last_maintenance_km,0) >= {0}", km))
                .orderByAsc(DmsVehicle::getInsuranceExpireDate);

        Page<DmsVehicle> entityPage = vehicleMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        Page<VehicleDTO> result = new Page<>(page.getCurrent(), page.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toDTO).toList());
        return result;
    }

    // ==================== 私有方法 ====================

    private void applyToEntity(DmsVehicle entity, VehicleDTO dto) {
        entity.setPlateNo(dto.getPlateNo() == null ? null : dto.getPlateNo().trim());
        entity.setVehicleType(dto.getVehicleType());
        entity.setBrand(dto.getBrand());
        entity.setModel(dto.getModel());
        entity.setColor(dto.getColor());
        entity.setVin(blankToNull(dto.getVin()));
        entity.setEngineNo(dto.getEngineNo());
        entity.setOwnershipType(dto.getOwnershipType());
        entity.setOwnerName(dto.getOwnerName());
        entity.setOwnerPhone(dto.getOwnerPhone());
        entity.setRatedLoad(dto.getRatedLoad());
        entity.setRatedPassenger(dto.getRatedPassenger());
        entity.setCargoVolume(dto.getCargoVolume());
        entity.setRegisterDate(dto.getRegisterDate());
        entity.setMaintenanceIntervalKm(dto.getMaintenanceIntervalKm());
        entity.setOperatingPermitNo(blankToNull(dto.getOperatingPermitNo()));
        entity.setOperatingPermitExpireDate(dto.getOperatingPermitExpireDate());
        entity.setInsuranceExpireDate(dto.getInsuranceExpireDate());
        entity.setInspectionExpireDate(dto.getInspectionExpireDate());
        entity.setDepartment(dto.getDepartment());
        entity.setRemark(dto.getRemark());
    }

    private void assertPlateNoAvailable(String plateNo, Long excludeId) {
        if (!StringUtils.hasText(plateNo)) {
            throw BusinessException.badRequest("车牌号不能为空");
        }
        Long count = vehicleMapper.selectCount(new LambdaQueryWrapper<DmsVehicle>()
                .eq(DmsVehicle::getPlateNo, plateNo)
                .ne(excludeId != null, DmsVehicle::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("车牌号已存在: " + plateNo);
        }
    }

    private void assertCodeAvailable(String code, Long excludeId) {
        Long count = vehicleMapper.selectCount(new LambdaQueryWrapper<DmsVehicle>()
                .eq(DmsVehicle::getVehicleCode, code)
                .ne(excludeId != null, DmsVehicle::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("车辆编码已存在: " + code);
        }
    }

    private void assertDeletable(DmsVehicle entity) {
        if (entity.getCurrentRiderId() != null) {
            throw BusinessException.badRequest("车辆「" + entity.getPlateNo() + "」绑定配送员中，请先解绑再删除");
        }
        if (hasInTransitTask(entity.getId())) {
            throw BusinessException.badRequest("车辆「" + entity.getPlateNo() + "」存在在途配送任务，不可删除");
        }
    }

    private void assertTransitionAllowed(DmsVehicle entity, VehicleStatusEnum target) {
        VehicleStatusEnum current = VehicleStatusEnum.fromValue(entity.getStatus());
        if (current == null) {
            throw BusinessException.badRequest("车辆当前状态非法: " + entity.getStatus());
        }
        if (!current.canTransitionTo(target)) {
            throw BusinessException.badRequest("车辆「" + entity.getPlateNo() + "」不允许从「"
                    + current.getDescription() + "」变更为「" + target.getDescription() + "」");
        }
        if (target == VehicleStatusEnum.REPAIRING || target == VehicleStatusEnum.SCRAPPED) {
            if (hasInTransitTask(entity.getId())) {
                throw BusinessException.badRequest("车辆「" + entity.getPlateNo() + "」存在在途配送任务，不可变更为「"
                        + target.getDescription() + "」");
            }
        }
        if (target == VehicleStatusEnum.SCRAPPED && entity.getCurrentRiderId() != null) {
            throw BusinessException.badRequest("车辆「" + entity.getPlateNo() + "」绑定配送员中，请先解绑再报废");
        }
    }

    private boolean hasInTransitTask(Long vehicleId) {
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<DmsTask>()
                .eq(DmsTask::getVehicleId, vehicleId)
                .in(DmsTask::getStatus, IN_TRANSIT_TASK_STATUS));
        return count != null && count > 0;
    }

    private DmsRiderVehicleBinding findActiveBindingByVehicle(Long vehicleId) {
        return bindingMapper.selectOne(new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                .eq(DmsRiderVehicleBinding::getVehicleId, vehicleId)
                .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                .orderByDesc(DmsRiderVehicleBinding::getBindTime)
                .last("LIMIT 1"));
    }

    private DmsRiderVehicleBinding findActiveBindingByRider(Long riderId) {
        return bindingMapper.selectOne(new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                .eq(DmsRiderVehicleBinding::getRiderId, riderId)
                .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                .orderByDesc(DmsRiderVehicleBinding::getBindTime)
                .last("LIMIT 1"));
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("请先选择要操作的车辆");
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private VehicleDTO toDTO(DmsVehicle entity) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(entity.getId());
        dto.setVehicleCode(entity.getVehicleCode());
        dto.setPlateNo(entity.getPlateNo());
        dto.setVehicleType(entity.getVehicleType());
        dto.setVehicleTypeText(textOf(VEHICLE_TYPE_TEXT, entity.getVehicleType()));
        dto.setBrand(entity.getBrand());
        dto.setModel(entity.getModel());
        dto.setColor(entity.getColor());
        dto.setVin(entity.getVin());
        dto.setEngineNo(entity.getEngineNo());
        dto.setOwnershipType(entity.getOwnershipType());
        dto.setOwnershipTypeText(textOf(OWNERSHIP_TEXT, entity.getOwnershipType()));
        dto.setOwnerName(entity.getOwnerName());
        dto.setOwnerPhone(entity.getOwnerPhone());
        dto.setRatedLoad(entity.getRatedLoad());
        dto.setRatedPassenger(entity.getRatedPassenger());
        dto.setCargoVolume(entity.getCargoVolume());
        dto.setRegisterDate(entity.getRegisterDate());
        dto.setMaintenanceIntervalKm(entity.getMaintenanceIntervalKm());
        dto.setOperatingPermitNo(entity.getOperatingPermitNo());
        dto.setOperatingPermitExpireDate(entity.getOperatingPermitExpireDate());
        dto.setInsuranceExpireDate(entity.getInsuranceExpireDate());
        dto.setInspectionExpireDate(entity.getInspectionExpireDate());
        dto.setDepartment(entity.getDepartment());
        dto.setRemark(entity.getRemark());
        dto.setStatus(entity.getStatus());
        dto.setStatusText(statusText(entity.getStatus()));
        dto.setVehicleManagerId(entity.getVehicleManagerId());
        dto.setVehicleManagerName(entity.getVehicleManagerName());
        dto.setVehicleManagerPhone(entity.getVehicleManagerPhone());
        dto.setCurrentRiderId(entity.getCurrentRiderId());
        dto.setCurrentRiderName(entity.getCurrentRiderName());
        dto.setCurrentMileage(entity.getCurrentMileage());
        dto.setLastMaintenanceKm(entity.getLastMaintenanceKm());
        dto.setLastMaintenanceDate(entity.getLastMaintenanceDate());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        fillCertInfo(dto);
        return dto;
    }

    /** 证件到期提示：取保险 / 年检 / 营运证中最近的一证 */
    private void fillCertInfo(VehicleDTO dto) {
        Map<String, LocalDate> certs = new HashMap<>();
        certs.put("保险", dto.getInsuranceExpireDate());
        certs.put("年检", dto.getInspectionExpireDate());
        certs.put("营运证", dto.getOperatingPermitExpireDate());
        Map.Entry<String, LocalDate> nearest = certs.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .min(Comparator.comparing(Map.Entry::getValue))
                .orElse(null);
        if (nearest == null) {
            return;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), nearest.getValue());
        dto.setCertNearestExpireDate(nearest.getValue());
        dto.setCertDaysLeft(days);
        dto.setCertWarnText(days < 0 ? nearest.getKey() + "已过期 " + (-days) + " 天"
                : days == 0 ? nearest.getKey() + "今日到期"
                : nearest.getKey() + " " + days + " 天后到期");
    }

    private String statusText(Integer status) {
        VehicleStatusEnum e = VehicleStatusEnum.fromValue(status);
        return e == null ? null : e.getDescription();
    }

    /**
     * 空值安全的字典查表（{@code Map.of} 的 get/getOrDefault 对 null key 会抛 NPE，
     * 车辆档案的车辆类型/归属类型均可为空，故统一走此方法）
     */
    private static String textOf(Map<Integer, String> dict, Integer key) {
        return key == null ? null : dict.get(key);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
