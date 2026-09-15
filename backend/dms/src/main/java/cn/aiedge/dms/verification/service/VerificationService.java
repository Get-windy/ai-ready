package cn.aiedge.dms.verification.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.mapper.DmsConfigMapper;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.tracking.entity.DmsTracking;
import cn.aiedge.dms.tracking.service.TrackingService;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.dto.MaintenanceCreateDTO;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import cn.aiedge.dms.vehicle.service.VehicleMaintenanceService;
import cn.aiedge.dms.vehicle.service.VehicleService;
import cn.aiedge.dms.common.enums.MaintTypeEnum;
import cn.aiedge.dms.common.enums.VehicleStatusEnum;
import cn.aiedge.dms.verification.dto.BindingCreateDTO;
import cn.aiedge.dms.verification.dto.HandoverDTO;
import cn.aiedge.dms.verification.dto.VerificationQueryDTO;
import cn.aiedge.dms.verification.dto.VehicleInspectionCreateDTO;
import cn.aiedge.dms.verification.entity.DmsPositionVerification;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.entity.DmsVehicleInspection;
import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import cn.aiedge.dms.verification.enums.AlertTypeEnum;
import cn.aiedge.dms.verification.enums.BindingStatusEnum;
import cn.aiedge.dms.verification.mapper.DmsPositionVerificationMapper;
import cn.aiedge.dms.verification.mapper.DmsRiderVehicleBindingMapper;
import cn.aiedge.dms.verification.mapper.DmsVehicleInspectionMapper;
import cn.aiedge.dms.verification.mapper.DmsVerificationAlertMapper;
import cn.aiedge.dms.verification.vo.BindingDetailVO;
import cn.aiedge.dms.verification.vo.ScanResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 人车绑定与核验服务
 *
 * <p>覆盖：出车验车、人车绑定/交车、配送中位置核验、异常检测（人车分离/异常滞留/绑定超时）、
 * 预警处理与巡检审核。异常检测由 {@link VerificationScheduler} 真实调度，
 * 也可通过 {@code POST /batch-verification} 手动触发。</p>
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final DmsVehicleInspectionMapper vehicleInspectionMapper;
    private final DmsRiderVehicleBindingMapper bindingMapper;
    private final DmsPositionVerificationMapper positionVerificationMapper;
    private final DmsVerificationAlertMapper alertMapper;
    private final DmsVehicleMapper vehicleMapper;
    private final DmsRiderMapper riderMapper;
    private final DmsConfigMapper configMapper;
    private final TrackingService trackingService;
    /** 人车绑定/解绑的唯一实现（《车辆管理》页）；本模块只叠加「检查门控 + 凭证 + 异常闭环」 */
    private final VehicleService vehicleService;
    private final VehicleMaintenanceService vehicleMaintenanceService;
    /** 人车绑定/解绑的唯一实现（《车辆管理》页），本模块只做「检查门控 + 凭证 + 异常闭环」 */

    /** 地球半径（米） */
    private static final double EARTH_RADIUS = 6371000.0;

    /** 异常滞留判定：期间移动范围小于该值（米）视为滞留 */
    private static final double STAY_RANGE_METERS = 50.0;

    // 参数键（见 V11.200.0 预置，全部租户可配）
    public static final String PARAM_POSITION_THRESHOLD = "verification.position.threshold.meters";
    public static final String PARAM_BINDING_MAX_HOURS = "verification.binding.max.hours";
    public static final String PARAM_STAY_MINUTES = "verification.stay.threshold.minutes";
    public static final String PARAM_SEPARATION_CONSECUTIVE = "verification.separation.consecutive";
    public static final String PARAM_CERT_WARN_DAYS = "kyc.cert.expire.warn.days";
    public static final String PARAM_ELIGIBILITY_ENFORCE = "verification.eligibility.enforce";
    public static final String PARAM_DEPARTURE_INSPECTION_REQUIRED = "verification.departure.inspection.required";
    public static final String PARAM_RETURN_INSPECTION_REQUIRED = "verification.return.inspection.required";

    // ==================== 出车验车 / 收车检查 ====================

    /**
     * 创建巡检记录（出车前 / 收车后 / 随机抽检 / 定期检查）
     *
     * <p>自动评估：刹车/灯光/灭火器任一异常 → 不通过(2)，否则通过(1)。</p>
     */
    public Long createInspection(VehicleInspectionCreateDTO dto, Long riderId) {
        return saveInspection(dto, riderId, dto.getBindingId());
    }

    /**
     * 当前登录人对应的配送员（司机端自助场景）
     *
     * <p>司机端只知道自己登录的用户，不应前端传 riderId 去猜；服务端按 userId 反查。</p>
     */
    public DmsRider currentRider() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.unauthorized("未登录");
        }
        DmsRider rider = riderMapper.selectOne(new LambdaQueryWrapper<DmsRider>()
                .eq(DmsRider::getUserId, userId)
                .last("LIMIT 1"));
        if (rider == null) {
            throw BusinessException.notFound("当前账号未关联配送员档案，请联系管理员");
        }
        return rider;
    }

    /** 巡检记录详情 */
    public DmsVehicleInspection getInspection(Long id) {
        DmsVehicleInspection inspection = vehicleInspectionMapper.selectById(id);
        if (inspection == null) {
            throw BusinessException.notFound("巡检记录不存在: " + id);
        }
        return inspection;
    }

    /** 落一条巡检记录（出车检查由 bind 在绑定成功后回写 bindingId；收车检查由 handover 带入） */
    private Long saveInspection(VehicleInspectionCreateDTO dto, Long riderId, Long bindingId) {
        DmsVehicle vehicle = vehicleMapper.selectById(dto.getVehicleId());
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + dto.getVehicleId());
        }

        DmsVehicleInspection inspection = new DmsVehicleInspection();
        inspection.setVehicleId(dto.getVehicleId());
        inspection.setPlateNo(vehicle.getPlateNo());
        Long effectiveRiderId = riderId != null ? riderId : SecurityUtils.getCurrentUserId();
        inspection.setRiderId(effectiveRiderId);
        DmsRider rider = effectiveRiderId == null ? null : riderMapper.selectById(effectiveRiderId);
        inspection.setRiderName(rider != null ? rider.getRealName() : String.valueOf(effectiveRiderId));
        inspection.setInspectionType(dto.getInspectionType() != null ? dto.getInspectionType() : 1);
        inspection.setInspectionTime(LocalDateTime.now());
        inspection.setBindingId(bindingId);

        inspection.setExteriorStatus(dto.getExteriorStatus());
        inspection.setExteriorRemark(dto.getExteriorRemark());
        inspection.setExteriorPhotos(dto.getExteriorPhotos());
        inspection.setTireStatus(dto.getTireStatus());
        inspection.setTireRemark(dto.getTireRemark());
        inspection.setLightStatus(dto.getLightStatus());
        inspection.setLightRemark(dto.getLightRemark());
        inspection.setBrakeStatus(dto.getBrakeStatus());
        inspection.setBrakeRemark(dto.getBrakeRemark());
        inspection.setCleanlinessStatus(dto.getCleanlinessStatus());
        inspection.setCleanlinessRemark(dto.getCleanlinessRemark());
        inspection.setMileage(dto.getMileage());
        inspection.setFuelLevel(dto.getFuelLevel());
        inspection.setFireExtinguisher(dto.getFireExtinguisher());
        inspection.setWarningTriangle(dto.getWarningTriangle());
        inspection.setInspectionLocation(dto.getInspectionLocation());
        inspection.setRemark(dto.getRemark());
        inspection.setResult(evaluateResult(dto));

        vehicleInspectionMapper.insert(inspection);
        log.info("巡检记录已创建, id={}, vehicleId={}, type={}, result={}, bindingId={}",
                inspection.getId(), dto.getVehicleId(), inspection.getInspectionType(), inspection.getResult(), bindingId);
        return inspection.getId();
    }

    /** 自动评估：刹车、灯光、灭火器任一异常 → 不通过(2) */
    private int evaluateResult(VehicleInspectionCreateDTO dto) {
        boolean abnormal = Integer.valueOf(1).equals(dto.getBrakeStatus())
                || Integer.valueOf(1).equals(dto.getLightStatus())
                || Integer.valueOf(1).equals(dto.getFireExtinguisher());
        return abnormal ? 2 : 1;
    }

    /** 异常项摘要（用于拦截提示与《车辆维护》待办内容） */
    private String abnormalSummary(DmsVehicleInspection inspection) {
        List<String> items = new ArrayList<>();
        if (Integer.valueOf(1).equals(inspection.getBrakeStatus())) items.add("刹车");
        if (Integer.valueOf(1).equals(inspection.getLightStatus())) items.add("灯光");
        if (Integer.valueOf(1).equals(inspection.getFireExtinguisher())) items.add("灭火器");
        return items.isEmpty() ? "-" : String.join("、", items);
    }

    // ==================== 人车绑定（出车登记） ====================

    /**
     * 出车登记：出车前检查门控 → 绑定（复用《车辆管理》唯一实现）→ 回写检查归属
     *
     * <p>业界口径（DVIR / check-out）：检查不通过禁止出车；检查必须针对本次要绑定的车辆，且不可复用。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long bind(BindingCreateDTO dto) {
        boolean required = paramBool(null, PARAM_DEPARTURE_INSPECTION_REQUIRED, true);

        DmsVehicleInspection inspection = null;
        if (dto.getInspectionId() != null) {
            inspection = vehicleInspectionMapper.selectById(dto.getInspectionId());
            if (inspection == null) {
                throw BusinessException.notFound("巡检记录不存在: " + dto.getInspectionId());
            }
            if (!Objects.equals(inspection.getVehicleId(), dto.getVehicleId())) {
                throw BusinessException.badRequest("出车前检查与所选车辆不一致，请重新检查");
            }
            if (!Integer.valueOf(1).equals(inspection.getResult())) {
                throw BusinessException.badRequest("出车前检查不通过（异常项：" + abnormalSummary(inspection)
                        + "），禁止出车；请先转《车辆维护》处理");
            }
            if (inspection.getBindingId() != null) {
                throw BusinessException.badRequest("该出车前检查已用于另一次出车绑定，请重新检查");
            }
        } else if (required) {
            throw BusinessException.badRequest("请先完成出车前检查再出车绑定");
        }

        // 复用《车辆管理》页的绑定实现：一车一人唯一性 + 车辆状态（空闲 → 已出勤）+ 绑定流水
        vehicleService.bindRider(dto.getVehicleId(), dto.getRiderId(),
                inspection != null ? inspection.getMileage() : null, dto.getBindReason());

        DmsRiderVehicleBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getVehicleId, dto.getVehicleId())
                        .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                        .orderByDesc(DmsRiderVehicleBinding::getId)
                        .last("LIMIT 1"));
        if (binding == null) {
            throw BusinessException.internalError("出车绑定失败：未找到绑定流水");
        }

        if (inspection != null) {
            inspection.setBindingId(binding.getId());
            inspection.setRiderId(binding.getRiderId());
            inspection.setRiderName(binding.getRiderName());
            vehicleInspectionMapper.updateById(inspection);
        }
        log.info("出车登记成功, bindingId={}, riderId={}, vehicleId={}, inspectionId={}",
                binding.getId(), dto.getRiderId(), dto.getVehicleId(), dto.getInspectionId());
        return binding.getId();
    }

    // ==================== 交车（收车检查 + 解绑） ====================

    /**
     * 交车：收车检查门控 → 交车信息 → 解绑（复用《车辆管理》唯一实现）→ 异常闭环
     *
     * <p>业界口径（DVIR / check-in）：收车检查是必做单证；里程必须单调；
     * 检查异常时车辆不回到「空闲」而是置「维修中」，并自动生成《车辆维护》维修待办（defect → work order）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void handover(Long bindingId, HandoverDTO dto) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null) {
            throw BusinessException.notFound("绑定记录不存在: " + bindingId);
        }
        if (BindingStatusEnum.ACTIVE.getValue() != binding.getStatus()) {
            throw BusinessException.badRequest("当前绑定状态不是绑定中，无法交车");
        }

        HandoverDTO body = dto != null ? dto : new HandoverDTO();
        boolean required = paramBool(binding.getTenantId(), PARAM_RETURN_INSPECTION_REQUIRED, true);
        if (body.getInspection() == null && required) {
            throw BusinessException.badRequest("请先完成收车后检查再交车");
        }
        if (body.getHandoverMileage() != null && binding.getBindMileage() != null
                && body.getHandoverMileage() < binding.getBindMileage()) {
            throw BusinessException.badRequest("交车里程（" + body.getHandoverMileage()
                    + "）不得小于出车里程（" + binding.getBindMileage() + "）");
        }

        // 1) 收车检查（类型缺省 2-收车后检查；里程缺省取交车里程）
        DmsVehicleInspection inspection = null;
        if (body.getInspection() != null) {
            VehicleInspectionCreateDTO ins = body.getInspection();
            ins.setVehicleId(binding.getVehicleId());
            if (ins.getInspectionType() == null) {
                ins.setInspectionType(2);
            }
            if (ins.getMileage() == null) {
                ins.setMileage(body.getHandoverMileage());
            }
            Long insId = saveInspection(ins, binding.getRiderId(), binding.getId());
            inspection = vehicleInspectionMapper.selectById(insId);
        }

        // 2) 交车信息 + 解绑（车辆状态：使用中/已出勤 → 空闲）
        //    可重入保护：若车辆侧已释放（历史异常中断留下的脏数据），跳过重复解绑，
        //    仅关闭绑定流水——否则「有绑定流水但车辆已释放」的记录会永远交不掉。
        DmsVehicle boundVehicle = vehicleMapper.selectById(binding.getVehicleId());
        DmsRiderVehicleBinding fresh = bindingMapper.selectById(bindingId);
        if (boundVehicle != null && boundVehicle.getCurrentRiderId() != null) {
            vehicleService.unbindRider(binding.getVehicleId(), body.getHandoverMileage());
        } else {
            log.warn("交车可重入：车辆 {} 已无当前配送员，仅关闭绑定流水 bindingId={}",
                    binding.getVehicleId(), bindingId);
            fresh.setHandoverTime(LocalDateTime.now());
            fresh.setHandoverMileage(body.getHandoverMileage() != null
                    ? body.getHandoverMileage() : fresh.getBindMileage());
            fresh.setStatus(BindingStatusEnum.HANDED_OVER.getValue());
        }
        fresh = bindingMapper.selectById(bindingId);
        fresh.setHandoverLocation(body.getHandoverLocation());
        if (body.getHandoverLat() != null) {
            fresh.setHandoverLat(body.getHandoverLat().doubleValue());
        }
        if (body.getHandoverLng() != null) {
            fresh.setHandoverLng(body.getHandoverLng().doubleValue());
        }
        if (hasText(body.getRemark())) {
            fresh.setRemark(body.getRemark());
        }
        bindingMapper.updateById(fresh);

        // 3) 检查异常 → 车辆置「维修中」+ 自动生成《车辆维护》维修待办
        if (inspection != null && !Integer.valueOf(1).equals(inspection.getResult())) {
            DmsVehicle vehicle = vehicleMapper.selectById(binding.getVehicleId());
            if (vehicle != null && VehicleStatusEnum.SCRAPPED.getValue() != (vehicle.getStatus() == null ? -1 : vehicle.getStatus())) {
                vehicle.setStatus(VehicleStatusEnum.REPAIRING.getValue());
                vehicleMapper.updateById(vehicle);
            }
            MaintenanceCreateDTO maint = new MaintenanceCreateDTO();
            maint.setVehicleId(binding.getVehicleId());
            maint.setMaintType(MaintTypeEnum.REPAIR.getValue());
            maint.setMaintDate(LocalDate.now());
            maint.setMaintContent("收车检查异常（异常项：" + abnormalSummary(inspection) + "），巡检单 #" + inspection.getId());
            maint.setAfterMaintMileage(body.getHandoverMileage() != null ? body.getHandoverMileage() : inspection.getMileage());
            maint.setRemark("由「收车后检查」自动生成，请车管员核实处理");
            vehicleMaintenanceService.create(maint);
            log.warn("收车检查异常已转维修: vehicleId={}, inspectionId={}, 异常项={}",
                    binding.getVehicleId(), inspection.getId(), abnormalSummary(inspection));
        }

        // 4) 交车后自动关闭「绑定超时」待处理预警
        closePendingAlerts(bindingId, AlertTypeEnum.BINDING_TIMEOUT, "已交车，自动关闭");

        log.info("交车成功, bindingId={}, vehicleId={}, operator={}, inspectionId={}",
                bindingId, binding.getVehicleId(), currentOperator(), inspection == null ? null : inspection.getId());
    }

    // ==================== 位置核验 ====================

    /**
     * 执行位置核验
     *
     * 计算配送员手机GPS位置与车辆GPS位置之间的球面距离，判断是否超出阈值；
     * 核验后立即做一次人车分离判定，使预警可以"随核验自动产生"。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long verifyPosition(Long bindingId, BigDecimal riderLat, BigDecimal riderLng,
                               LocalDateTime riderReportTime, BigDecimal vehicleLat, BigDecimal vehicleLng,
                               LocalDateTime vehicleReportTime, BigDecimal threshold) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null) {
            throw BusinessException.notFound("绑定记录不存在: " + bindingId);
        }

        double distance = haversineDistance(
                riderLat.doubleValue(), riderLng.doubleValue(),
                vehicleLat.doubleValue(), vehicleLng.doubleValue()
        );

        BigDecimal thresholdMeters = threshold != null
                ? threshold
                : BigDecimal.valueOf(paramInt(binding.getTenantId(), PARAM_POSITION_THRESHOLD, 500));
        boolean isAbnormal = distance > thresholdMeters.doubleValue();

        DmsPositionVerification verification = new DmsPositionVerification();
        verification.setTenantId(binding.getTenantId());
        verification.setBindingId(bindingId);
        verification.setRiderId(binding.getRiderId());
        verification.setVehicleId(binding.getVehicleId());
        verification.setRiderLat(riderLat.doubleValue());
        verification.setRiderLng(riderLng.doubleValue());
        verification.setRiderReportTime(riderReportTime);
        verification.setVehicleLat(vehicleLat.doubleValue());
        verification.setVehicleLng(vehicleLng.doubleValue());
        verification.setVehicleReportTime(vehicleReportTime);
        verification.setDistanceMeters(distance);
        verification.setThresholdMeters(thresholdMeters.doubleValue());
        verification.setIsAbnormal(isAbnormal ? 1 : 0);
        verification.setVerifyTime(LocalDateTime.now());
        verification.setVerifyDesc(isAbnormal
                ? "人车距离 " + String.format("%.1f", distance) + " 米，超出阈值 " + thresholdMeters + " 米"
                : "位置正常，距离 " + String.format("%.1f", distance) + " 米");
        positionVerificationMapper.insert(verification);

        // 随核验自动判决人车分离
        int consecutive = paramInt(binding.getTenantId(), PARAM_SEPARATION_CONSECUTIVE, 3);
        detectSeparationAnomaly(bindingId, consecutive);

        log.info("位置核验完成, id={}, bindingId={}, distance={}m, abnormal={}",
                verification.getId(), bindingId, String.format("%.1f", distance), isAbnormal);
        return verification.getId();
    }

    // ==================== 异常检测 ====================

    /**
     * 检测人车分离异常：最近 N 条核验全部异常 → 产生预警（同一绑定未处理前不重复产生）
     *
     * @return 是否新产生预警
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean detectSeparationAnomaly(Long bindingId, int consecutiveThreshold) {
        List<DmsPositionVerification> recentRecords = positionVerificationMapper.selectList(
                new LambdaQueryWrapper<DmsPositionVerification>()
                        .eq(DmsPositionVerification::getBindingId, bindingId)
                        .orderByDesc(DmsPositionVerification::getCreateTime)
                        .last("LIMIT " + Math.max(1, consecutiveThreshold))
        );

        if (recentRecords.size() < consecutiveThreshold) {
            return false;
        }

        boolean allAbnormal = recentRecords.stream()
                .allMatch(r -> Integer.valueOf(1).equals(r.getIsAbnormal()));
        if (!allAbnormal) {
            return false;
        }

        DmsPositionVerification latest = recentRecords.get(0);
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null) {
            return false;
        }

        return createAlert(binding, AlertTypeEnum.POSITION_MISMATCH,
                "人车位置连续" + consecutiveThreshold + "次核验异常，最近距离" + String.format("%.1f", latest.getDistanceMeters()) + "米",
                latest.getRiderLat(), latest.getRiderLng(),
                latest.getVehicleLat(), latest.getVehicleLng(),
                latest.getDistanceMeters(), null);
    }

    /**
     * 检测异常滞留：最近 N 分钟内该配送员轨迹移动范围 &lt; 50 米 → 滞留。
     * 轨迹来自 {@code dms_tracking}（配送员端真实上报），无轨迹时不判定。
     *
     * @return 是否新产生预警
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean detectAbnormalStay(Long bindingId, int stayThresholdMinutes) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null || BindingStatusEnum.ACTIVE.getValue() != binding.getStatus()) {
            return false;
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(stayThresholdMinutes);
        List<DmsTracking> track = trackingService.getTrack(binding.getRiderId(), since, LocalDateTime.now());
        if (track.size() < 2) {
            return false;
        }

        double minLat = track.stream().map(DmsTracking::getLat).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).min().orElse(0);
        double maxLat = track.stream().map(DmsTracking::getLat).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().orElse(0);
        double minLng = track.stream().map(DmsTracking::getLng).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).min().orElse(0);
        double maxLng = track.stream().map(DmsTracking::getLng).filter(Objects::nonNull).mapToDouble(BigDecimal::doubleValue).max().orElse(0);

        double rangeDistance = haversineDistance(minLat, minLng, maxLat, maxLng);
        if (rangeDistance >= STAY_RANGE_METERS) {
            return false;
        }

        DmsTracking latest = track.get(track.size() - 1);
        return createAlert(binding, AlertTypeEnum.ABNORMAL_STAY,
                "配送员在" + stayThresholdMinutes + "分钟内位置移动范围仅" + String.format("%.1f", rangeDistance) + "米，存在异常滞留",
                latest.getLat() != null ? latest.getLat().doubleValue() : null,
                latest.getLng() != null ? latest.getLng().doubleValue() : null,
                null, null, null, (long) stayThresholdMinutes * 60);
    }

    /**
     * 检测绑定超时：绑定中且已超过最大时长 → 预警
     *
     * @return 是否新产生预警
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean detectBindingTimeout(Long bindingId, int maxDurationHours) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null || BindingStatusEnum.ACTIVE.getValue() != binding.getStatus()) {
            return false;
        }
        if (binding.getBindTime() == null) {
            return false;
        }

        long hoursBound = ChronoUnit.HOURS.between(binding.getBindTime(), LocalDateTime.now());
        if (hoursBound < maxDurationHours) {
            return false;
        }

        return createAlert(binding, AlertTypeEnum.BINDING_TIMEOUT,
                "配送员已连续工作" + hoursBound + "小时，超过最大时限" + maxDurationHours + "小时",
                binding.getBindLat(), binding.getBindLng(), null, null, null, null);
    }

    // ==================== 批量核验（真实实现） ====================

    /**
     * 批量核验所有活跃绑定
     *
     * <p>替换原空循环：对每条「绑定中」记录依次执行 绑定超时 / 异常滞留 / 人车分离 检测，
     * 参数取租户配置（{@code dms_config}），缺省回退内置默认值。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ScanResultVO batchVerification() {
        ScanResultVO result = new ScanResultVO();

        List<DmsRiderVehicleBinding> activeBindings = bindingMapper.selectList(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
        );
        result.setScannedBindings(activeBindings.size());
        log.info("批量核验活跃绑定, 数量={}", activeBindings.size());

        for (DmsRiderVehicleBinding binding : activeBindings) {
            try {
                Long tenantId = binding.getTenantId();
                int maxHours = paramInt(tenantId, PARAM_BINDING_MAX_HOURS, 12);
                int stayMinutes = paramInt(tenantId, PARAM_STAY_MINUTES, 60);
                int consecutive = paramInt(tenantId, PARAM_SEPARATION_CONSECUTIVE, 3);

                if (detectBindingTimeout(binding.getId(), maxHours)) {
                    result.incTimeout();
                }
                if (detectAbnormalStay(binding.getId(), stayMinutes)) {
                    result.incStay();
                }
                // 人车分离基于已落库的核验记录判定（配送员端上报时产生）
                if (detectSeparationAnomaly(binding.getId(), consecutive)) {
                    result.incSeparation();
                }
            } catch (Exception e) {
                log.error("批量核验异常, bindingId={}, error={}", binding.getId(), e.getMessage());
            }
        }

        result.setSkippedDuplicated(skippedCount.get());
        skippedCount.remove();
        return result;
    }

    // ==================== 分页查询 ====================

    /** 分页查询绑定记录 */
    public IPage<DmsRiderVehicleBinding> pageBindings(VerificationQueryDTO q) {
        Page<DmsRiderVehicleBinding> pageParam = new Page<>(q.getPage(), q.getSize());
        return bindingMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .like(hasText(q.getRiderName()), DmsRiderVehicleBinding::getRiderName, q.getRiderName())
                        .like(hasText(q.getPlateNo()), DmsRiderVehicleBinding::getPlateNo, q.getPlateNo())
                        .eq(q.getStatus() != null, DmsRiderVehicleBinding::getStatus, q.getStatus())
                        .ge(q.getStartDate() != null, DmsRiderVehicleBinding::getBindTime,
                                q.getStartDate() != null ? q.getStartDate().atStartOfDay() : null)
                        .le(q.getEndDate() != null, DmsRiderVehicleBinding::getBindTime,
                                q.getEndDate() != null ? LocalDateTime.of(q.getEndDate(), LocalTime.MAX) : null)
                        .orderByDesc(DmsRiderVehicleBinding::getBindTime)
        );
    }

    /** 绑定详情：绑定信息 + 核验历史 + 关联巡检 + 关联预警 */
    public BindingDetailVO getBindingDetail(Long id) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(id);
        if (binding == null) {
            throw BusinessException.notFound("绑定记录不存在: " + id);
        }

        List<DmsPositionVerification> verifications = positionVerificationMapper.selectList(
                new LambdaQueryWrapper<DmsPositionVerification>()
                        .eq(DmsPositionVerification::getBindingId, id)
                        .orderByDesc(DmsPositionVerification::getVerifyTime)
                        .last("LIMIT 100")
        );
        List<DmsVehicleInspection> inspections = vehicleInspectionMapper.selectList(
                new LambdaQueryWrapper<DmsVehicleInspection>()
                        .eq(DmsVehicleInspection::getBindingId, id)
                        .orderByDesc(DmsVehicleInspection::getInspectionTime)
        );
        List<DmsVerificationAlert> alerts = alertMapper.selectList(
                new LambdaQueryWrapper<DmsVerificationAlert>()
                        .eq(DmsVerificationAlert::getBindingId, id)
                        .orderByDesc(DmsVerificationAlert::getCreateTime)
        );

        BindingDetailVO vo = new BindingDetailVO();
        vo.setBinding(binding);
        vo.setVerifications(verifications);
        vo.setInspections(inspections);
        vo.setAlerts(alerts);
        vo.setVerifyCount(verifications.size());
        vo.setAbnormalCount((int) verifications.stream()
                .filter(v -> Integer.valueOf(1).equals(v.getIsAbnormal())).count());
        return vo;
    }

    /** 分页查询预警记录 */
    public IPage<DmsVerificationAlert> pageAlerts(VerificationQueryDTO q) {
        Page<DmsVerificationAlert> pageParam = new Page<>(q.getPage(), q.getSize());
        return alertMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsVerificationAlert>()
                        .eq(q.getAlertType() != null, DmsVerificationAlert::getAlertType, q.getAlertType())
                        .eq(q.getAlertLevel() != null, DmsVerificationAlert::getAlertLevel, q.getAlertLevel())
                        .eq(q.getHandleStatus() != null, DmsVerificationAlert::getHandleStatus, q.getHandleStatus())
                        .like(hasText(q.getRiderName()), DmsVerificationAlert::getRiderName, q.getRiderName())
                        .like(hasText(q.getPlateNo()), DmsVerificationAlert::getPlateNo, q.getPlateNo())
                        .ge(q.getStartDate() != null, DmsVerificationAlert::getCreateTime,
                                q.getStartDate() != null ? q.getStartDate().atStartOfDay() : null)
                        .le(q.getEndDate() != null, DmsVerificationAlert::getCreateTime,
                                q.getEndDate() != null ? LocalDateTime.of(q.getEndDate(), LocalTime.MAX) : null)
                        .orderByDesc(DmsVerificationAlert::getCreateTime)
        );
    }

    /** 分页查询巡检记录 */
    public IPage<DmsVehicleInspection> pageInspections(VerificationQueryDTO q) {
        Page<DmsVehicleInspection> pageParam = new Page<>(q.getPage(), q.getSize());
        return vehicleInspectionMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsVehicleInspection>()
                        .like(hasText(q.getRiderName()), DmsVehicleInspection::getRiderName, q.getRiderName())
                        .like(hasText(q.getPlateNo()), DmsVehicleInspection::getPlateNo, q.getPlateNo())
                        .eq(q.getInspectionType() != null, DmsVehicleInspection::getInspectionType, q.getInspectionType())
                        .eq(q.getResult() != null, DmsVehicleInspection::getResult, q.getResult())
                        .ge(q.getStartDate() != null, DmsVehicleInspection::getInspectionTime,
                                q.getStartDate() != null ? q.getStartDate().atStartOfDay() : null)
                        .le(q.getEndDate() != null, DmsVehicleInspection::getInspectionTime,
                                q.getEndDate() != null ? LocalDateTime.of(q.getEndDate(), LocalTime.MAX) : null)
                        .orderByDesc(DmsVehicleInspection::getInspectionTime)
        );
    }

    /** 配送员当前活跃绑定 */
    public DmsRiderVehicleBinding findActiveBindingByRider(Long riderId) {
        return bindingMapper.selectOne(new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                .eq(DmsRiderVehicleBinding::getRiderId, riderId)
                .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                .last("LIMIT 1"));
    }

    /** 车辆当前活跃绑定 */
    public DmsRiderVehicleBinding findActiveBindingByVehicle(Long vehicleId) {
        return bindingMapper.selectOne(new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                .eq(DmsRiderVehicleBinding::getVehicleId, vehicleId)
                .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                .last("LIMIT 1"));
    }

    /** 分页查询核验记录 */
    public IPage<DmsPositionVerification> pageVerifications(int page, int size, Long bindingId) {
        Page<DmsPositionVerification> pageParam = new Page<>(page, size);
        return positionVerificationMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsPositionVerification>()
                        .eq(bindingId != null, DmsPositionVerification::getBindingId, bindingId)
                        .orderByDesc(DmsPositionVerification::getCreateTime)
        );
    }

    // ==================== 预警处理 ====================

    /**
     * 处理预警
     *
     * @param id           预警ID
     * @param handleStatus 处理状态：1-已确认 2-已忽略 3-已处理
     * @param handler      处理人（为 null 时取登录态）
     * @param remark       处理备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(Long id, Integer handleStatus, String handler, String remark) {
        DmsVerificationAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw BusinessException.notFound("预警记录不存在: " + id);
        }
        if (handleStatus == null || handleStatus < 1 || handleStatus > 3) {
            throw BusinessException.badRequest("处理状态非法: " + handleStatus);
        }
        alert.setHandleStatus(handleStatus);
        alert.setHandler(hasText(handler) ? handler : currentOperator());
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(remark);
        alertMapper.updateById(alert);
        log.info("预警已处理, id={}, status={}, handler={}", id, handleStatus, alert.getHandler());
    }

    // ==================== 巡检审核 ====================

    /**
     * 审核巡检记录
     *
     * @param id       巡检记录ID
     * @param result   审核结果：1-通过 其他(0/2)-不通过（服务端归一化为 1/2）
     * @param reviewer 审核人（为 null 时取登录态）
     * @param remark   审核意见
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewInspection(Long id, Integer result, String reviewer, String remark) {
        DmsVehicleInspection inspection = vehicleInspectionMapper.selectById(id);
        if (inspection == null) {
            throw BusinessException.notFound("巡检记录不存在: " + id);
        }
        if (result == null) {
            throw BusinessException.badRequest("审核结果不能为空");
        }
        inspection.setResult(Integer.valueOf(1).equals(result) ? 1 : 2);
        inspection.setReviewer(hasText(reviewer) ? reviewer : currentOperator());
        inspection.setReviewTime(LocalDateTime.now());
        inspection.setReviewRemark(remark);
        vehicleInspectionMapper.updateById(inspection);
        log.info("巡检已审核, id={}, result={}, reviewer={}", id, inspection.getResult(), inspection.getReviewer());
    }

    // ==================== 供 KYC / 定时任务复用 ====================

    /**
     * 产生一条核验预警（带去重：同一对象同类型存在「未处理」预警时不再产生）
     *
     * @return 是否新建
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean raiseKycAlert(Long tenantId, Long riderId, String riderName, int alertType, int level, String content) {
        return raiseAlert(tenantId, riderId, riderName, null, null, null, alertType, level, content);
    }

    /**
     * 通用预警产生入口（带去重）：供 KYC / 轨迹跟踪等复用，避免各页各写一套去重与落库逻辑。
     *
     * <p>去重口径：同一配送员 + 同一类型 + 仍未处理（handleStatus=0）时不再重复产生，
     * 处理后才可能再次触发。</p>
     *
     * @param taskId   关联配送任务（可为空）
     * @param riderLat 预警时刻配送员纬度（可为空）
     * @param riderLng 预警时刻配送员经度（可为空）
     * @return 是否新建
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean raiseAlert(Long tenantId, Long riderId, String riderName, Long taskId,
                              BigDecimal riderLat, BigDecimal riderLng,
                              int alertType, int level, String content) {
        Long exists = alertMapper.selectCount(
                new LambdaQueryWrapper<DmsVerificationAlert>()
                        .eq(tenantId != null, DmsVerificationAlert::getTenantId, tenantId)
                        .eq(DmsVerificationAlert::getRiderId, riderId)
                        .eq(DmsVerificationAlert::getAlertType, alertType)
                        .eq(DmsVerificationAlert::getHandleStatus, 0)
        );
        if (exists != null && exists > 0) {
            return false;
        }
        DmsVerificationAlert alert = new DmsVerificationAlert();
        alert.setTenantId(tenantId);
        alert.setRiderId(riderId);
        alert.setRiderName(riderName);
        alert.setTaskId(taskId);
        alert.setRiderLat(riderLat == null ? null : riderLat.doubleValue());
        alert.setRiderLng(riderLng == null ? null : riderLng.doubleValue());
        alert.setAlertType(alertType);
        alert.setAlertLevel(level);
        alert.setAlertContent(content);
        alert.setHandleStatus(0);
        alertMapper.insert(alert);
        log.warn("创建核验预警, riderId={}, taskId={}, type={}, content={}", riderId, taskId, alertType, content);
        return true;
    }

    /** 读取租户整型参数（缺省回退 defaultVal，绝不抛异常） */
    public int paramInt(Long tenantId, String key, int defaultVal) {
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
            log.debug("读取 DMS 参数失败，回退默认值: key={}, default={}", key, defaultVal);
            return defaultVal;
        }
    }

    /** 读取租户布尔参数（缺省回退 defaultVal） */
    public boolean paramBool(Long tenantId, String key, boolean defaultVal) {
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
            return !"false".equalsIgnoreCase(config.getConfigValue().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /** 关闭某绑定下指定类型的未处理预警（如交车后关闭绑定超时） */
    @Transactional(rollbackFor = Exception.class)
    public void closePendingAlerts(Long bindingId, AlertTypeEnum type, String remark) {
        List<DmsVerificationAlert> alerts = alertMapper.selectList(
                new LambdaQueryWrapper<DmsVerificationAlert>()
                        .eq(DmsVerificationAlert::getBindingId, bindingId)
                        .eq(DmsVerificationAlert::getAlertType, type.getValue())
                        .eq(DmsVerificationAlert::getHandleStatus, 0)
        );
        for (DmsVerificationAlert alert : alerts) {
            alert.setHandleStatus(3);
            alert.setHandler(currentOperator());
            alert.setHandleTime(LocalDateTime.now());
            alert.setHandleRemark(remark);
            alertMapper.updateById(alert);
        }
    }

    // ==================== 辅助方法 ====================

    /** 当前操作人：登录用户名 → 用户ID → system */
    public static String currentOperator() {
        String username = SecurityUtils.getCurrentUsername();
        if (hasText(username)) {
            return username;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        return userId != null ? String.valueOf(userId) : "system";
    }

    /** 去重跳过的计数（每次 batchVerification 返回后清空） */
    private final ThreadLocal<Integer> skippedCount = ThreadLocal.withInitial(() -> 0);

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 创建告警记录（去重：同一绑定 + 同类型存在未处理告警时跳过）
     *
     * @return 是否新建
     */
    private boolean createAlert(DmsRiderVehicleBinding binding, AlertTypeEnum alertType,
                                String content, Double riderLat, Double riderLng,
                                Double vehicleLat, Double vehicleLng,
                                Double distanceMeters, Long stayDuration) {
        Long exists = alertMapper.selectCount(
                new LambdaQueryWrapper<DmsVerificationAlert>()
                        .eq(DmsVerificationAlert::getBindingId, binding.getId())
                        .eq(DmsVerificationAlert::getAlertType, alertType.getValue())
                        .eq(DmsVerificationAlert::getHandleStatus, 0)
        );
        if (exists != null && exists > 0) {
            skippedCount.set(skippedCount.get() + 1);
            return false;
        }

        DmsVerificationAlert alert = new DmsVerificationAlert();
        alert.setTenantId(binding.getTenantId());
        alert.setBindingId(binding.getId());
        alert.setAlertType(alertType.getValue());
        alert.setAlertLevel(defaultLevel(alertType));
        alert.setRiderId(binding.getRiderId());
        alert.setRiderName(binding.getRiderName());
        alert.setVehicleId(binding.getVehicleId());
        alert.setPlateNo(binding.getPlateNo());
        alert.setRiderLat(riderLat);
        alert.setRiderLng(riderLng);
        alert.setVehicleLat(vehicleLat);
        alert.setVehicleLng(vehicleLng);
        alert.setDistanceMeters(distanceMeters);
        alert.setStayDuration(stayDuration);
        alert.setAlertContent(content);
        alert.setHandleStatus(0);
        alertMapper.insert(alert);
        log.warn("创建核验告警, id={}, type={}, content={}", alert.getId(), alertType.getDescription(), content);
        return true;
    }

    /** 预警级别：人车分离/偏离路线=严重(3)，滞留/绑定超时=警告(2)，其余提示(1) */
    private int defaultLevel(AlertTypeEnum type) {
        return switch (type) {
            case POSITION_MISMATCH, OFF_ROUTE -> 3;
            case ABNORMAL_STAY, BINDING_TIMEOUT, VEHICLE_OFF_HOURS -> 2;
            case CERT_EXPIRING -> 2;
            default -> 1;
        };
    }

    /**
     * 计算GPS两点间的球面距离（Haversine公式）
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /** 供 KYC 服务复用的距离计算 */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
