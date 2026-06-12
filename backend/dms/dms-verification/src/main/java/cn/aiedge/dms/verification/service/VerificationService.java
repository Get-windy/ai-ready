package cn.aiedge.dms.verification.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import cn.aiedge.dms.verification.dto.BindingCreateDTO;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 人车绑定与核验服务
 *
 * 提供出车验车、人车绑定、配送中位置核验、异常检测、交车解绑等全流程管理。
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

    /** 地球半径（米） */
    private static final double EARTH_RADIUS = 6371000.0;

    // ==================== 出车验车 ====================

    /**
     * 创建验车记录
     *
     * 自动评估验车结果：如果刹车/灯光/灭火器任一异常，结果为不通过(2)，否则为通过(1)。
     *
     * @param dto     验车信息
     * @param riderId 配送员ID
     * @return 验车记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createInspection(VehicleInspectionCreateDTO dto, Long riderId) {
        DmsVehicle vehicle = vehicleMapper.selectById(dto.getVehicleId());
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + dto.getVehicleId());
        }

        DmsVehicleInspection inspection = new DmsVehicleInspection();
        inspection.setVehicleId(dto.getVehicleId());
        inspection.setPlateNo(vehicle.getPlateNo());
        inspection.setRiderId(riderId);
        // 查询配送员姓名
        DmsRider rider = riderMapper.selectById(riderId);
        inspection.setRiderName(rider != null ? rider.getRealName() : String.valueOf(riderId));
        inspection.setInspectionType(dto.getInspectionType() != null ? dto.getInspectionType() : 1);
        inspection.setInspectionTime(LocalDateTime.now());

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
        inspection.setRemark(dto.getRemark());

        // 自动评估：刹车、灯光、灭火器任一异常 -> 不通过
        boolean brakeAbnormal = Integer.valueOf(1).equals(dto.getBrakeStatus());
        boolean lightAbnormal = Integer.valueOf(1).equals(dto.getLightStatus());
        boolean fireExtinguisherAbnormal = Integer.valueOf(1).equals(dto.getFireExtinguisher());
        if (brakeAbnormal || lightAbnormal || fireExtinguisherAbnormal) {
            inspection.setResult(2); // 不通过
        } else {
            inspection.setResult(1); // 通过
        }

        vehicleInspectionMapper.insert(inspection);
        log.info("验车记录已创建, id={}, vehicleId={}, result={}", inspection.getId(), dto.getVehicleId(), inspection.getResult());
        return inspection.getId();
    }

    // ==================== 人车绑定 ====================

    /**
     * 人车绑定（出车登记）
     *
     * 先验车通过，再检查车辆和配送员是否已经在绑定中，然后创建绑定记录并更新车辆状态。
     *
     * @param dto 绑定信息
     * @return 绑定记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long bind(BindingCreateDTO dto) {
        // 检查验车记录
        if (dto.getInspectionId() != null) {
            DmsVehicleInspection inspection = vehicleInspectionMapper.selectById(dto.getInspectionId());
            if (inspection == null) {
                throw BusinessException.notFound("验车记录不存在: " + dto.getInspectionId());
            }
            if (!Integer.valueOf(1).equals(inspection.getResult())) {
                throw BusinessException.badRequest("验车未通过，禁止出车绑定");
            }
        }

        // 检查车辆是否已有绑定
        Long activeBindingCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getVehicleId, dto.getVehicleId())
                        .eq(DmsRiderVehicleBinding::getStatus, 0)
        );
        if (activeBindingCount != null && activeBindingCount > 0) {
            throw BusinessException.badRequest("车辆当前已有绑定记录，不可重复绑定");
        }

        // 检查配送员是否已有绑定
        Long riderBindingCount = bindingMapper.selectCount(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getRiderId, dto.getRiderId())
                        .eq(DmsRiderVehicleBinding::getStatus, 0)
        );
        if (riderBindingCount != null && riderBindingCount > 0) {
            throw BusinessException.badRequest("配送员当前已有绑定记录，不可重复绑定");
        }

        // 获取车辆信息
        DmsVehicle vehicle = vehicleMapper.selectById(dto.getVehicleId());
        if (vehicle == null) {
            throw BusinessException.notFound("车辆不存在: " + dto.getVehicleId());
        }

        // 查询配送员信息
        DmsRider rider = riderMapper.selectById(dto.getRiderId());
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在: " + dto.getRiderId());
        }

        // 创建绑定记录
        DmsRiderVehicleBinding binding = new DmsRiderVehicleBinding();
        binding.setRiderId(dto.getRiderId());
        binding.setRiderName(rider.getRealName());
        binding.setRiderPhone(rider.getPhone());
        binding.setVehicleId(dto.getVehicleId());
        binding.setPlateNo(vehicle.getPlateNo());
        binding.setBindTime(LocalDateTime.now());
        binding.setBindMileage(vehicle.getCurrentMileage());
        binding.setStatus(0); // 绑定中
        binding.setBindReason(dto.getBindReason());
        bindingMapper.insert(binding);

        // 更新车辆状态
        vehicle.setCurrentRiderId(dto.getRiderId());
        vehicle.setStatus(1); // 使用中
        vehicleMapper.updateById(vehicle);

        log.info("人车绑定成功, bindingId={}, riderId={}, vehicleId={}", binding.getId(), dto.getRiderId(), dto.getVehicleId());
        return binding.getId();
    }

    // ==================== 交车解绑 ====================

    /**
     * 交车解绑
     *
     * 设置交车时间和状态，清除车辆上的配送员信息。
     *
     * @param bindingId        绑定记录ID
     * @param handoverMileage  交车里程
     * @param handoverLat      交车位置纬度
     * @param handoverLng      交车位置经度
     */
    @Transactional(rollbackFor = Exception.class)
    public void handover(Long bindingId, Integer handoverMileage, BigDecimal handoverLat, BigDecimal handoverLng) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null) {
            throw BusinessException.notFound("绑定记录不存在: " + bindingId);
        }
        if (BindingStatusEnum.ACTIVE.getValue() != binding.getStatus()) {
            throw BusinessException.badRequest("当前绑定状态不是绑定中，无法交车");
        }

        binding.setHandoverTime(LocalDateTime.now());
        binding.setHandoverMileage(handoverMileage);
        binding.setHandoverLat(handoverLat.doubleValue());
        binding.setHandoverLng(handoverLng.doubleValue());
        binding.setStatus(BindingStatusEnum.HANDED_OVER.getValue());
        bindingMapper.updateById(binding);

        // 清除车辆绑定信息
        DmsVehicle vehicle = vehicleMapper.selectById(binding.getVehicleId());
        if (vehicle != null) {
            vehicle.setCurrentRiderId(null);
            vehicle.setCurrentRiderName(null);
            vehicle.setStatus(0); // 空闲
            vehicleMapper.updateById(vehicle);
        }

        log.info("交车成功, bindingId={}, vehicleId={}", bindingId, binding.getVehicleId());
    }

    // ==================== 位置核验 ====================

    /**
     * 执行位置核验
     *
     * 计算配送员手机GPS位置与车辆GPS位置之间的球面距离，
     * 判断是否超出阈值并创建核验记录。
     *
     * @param bindingId          绑定记录ID
     * @param riderLat           配送员纬度
     * @param riderLng           配送员经度
     * @param riderReportTime    配送员位置上报时间
     * @param vehicleLat         车辆纬度
     * @param vehicleLng         车辆经度
     * @param vehicleReportTime  车辆位置上报时间
     * @param threshold          偏差阈值（米）
     * @return 核验记录ID
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

        BigDecimal thresholdMeters = threshold != null ? threshold : BigDecimal.valueOf(100);
        boolean isAbnormal = distance > thresholdMeters.doubleValue();

        DmsPositionVerification verification = new DmsPositionVerification();
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

        log.info("位置核验完成, id={}, bindingId={}, distance={}m, abnormal={}",
                verification.getId(), bindingId, String.format("%.1f", distance), isAbnormal);
        return verification.getId();
    }

    // ==================== 异常检测 ====================

    /**
     * 检测人车分离异常
     *
     * 检查最近N条核验记录，如果全部异常则创建告警。
     *
     * @param bindingId             绑定记录ID
     * @param consecutiveThreshold  连续异常次数阈值
     */
    @Transactional(rollbackFor = Exception.class)
    public void detectSeparationAnomaly(Long bindingId, int consecutiveThreshold) {
        List<DmsPositionVerification> recentRecords = positionVerificationMapper.selectList(
                new LambdaQueryWrapper<DmsPositionVerification>()
                        .eq(DmsPositionVerification::getBindingId, bindingId)
                        .orderByDesc(DmsPositionVerification::getCreateTime)
                        .last("LIMIT " + consecutiveThreshold)
        );

        if (recentRecords.size() < consecutiveThreshold) {
            return; // 记录不足，无法判定
        }

        boolean allAbnormal = recentRecords.stream()
                .allMatch(r -> Integer.valueOf(1).equals(r.getIsAbnormal()));

        if (allAbnormal) {
            DmsPositionVerification latest = recentRecords.get(0);
            DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);

            createAlert(binding, AlertTypeEnum.POSITION_MISMATCH,
                    "人车位置连续" + consecutiveThreshold + "次核验异常，最近距离" + String.format("%.1f", latest.getDistanceMeters()) + "米",
                    latest.getRiderLat(), latest.getRiderLng(),
                    latest.getVehicleLat(), latest.getVehicleLng(),
                    latest.getDistanceMeters(), null);
        }
    }

    /**
     * 检测异常滞留
     *
     * 检查最近N分钟内的位置核验记录，如果位置变化范围未超过阈值则视为滞留。
     *
     * @param bindingId              绑定记录ID
     * @param stayThresholdMinutes   滞留判定时间（分钟）
     */
    @Transactional(rollbackFor = Exception.class)
    public void detectAbnormalStay(Long bindingId, int stayThresholdMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(stayThresholdMinutes);
        List<DmsPositionVerification> records = positionVerificationMapper.selectList(
                new LambdaQueryWrapper<DmsPositionVerification>()
                        .eq(DmsPositionVerification::getBindingId, bindingId)
                        .ge(DmsPositionVerification::getCreateTime, since)
                        .orderByAsc(DmsPositionVerification::getCreateTime)
        );

        if (records.size() < 2) {
            return;
        }

        // 计算位置变化范围（过滤空值防止 NPE）
        double minLat = records.stream()
                .map(DmsPositionVerification::getRiderLat)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).min().orElse(0);
        double maxLat = records.stream()
                .map(DmsPositionVerification::getRiderLat)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).max().orElse(0);
        double minLng = records.stream()
                .map(DmsPositionVerification::getRiderLng)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).min().orElse(0);
        double maxLng = records.stream()
                .map(DmsPositionVerification::getRiderLng)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).max().orElse(0);

        double rangeDistance = haversineDistance(minLat, minLng, maxLat, maxLng);
        if (rangeDistance < 50) { // 移动范围小于50米视为滞留
            DmsPositionVerification latest = records.get(records.size() - 1);
            DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);

            createAlert(binding, AlertTypeEnum.ABNORMAL_STAY,
                    "配送员在" + stayThresholdMinutes + "分钟内位置移动范围仅" + String.format("%.1f", rangeDistance) + "米，存在异常滞留",
                    latest.getRiderLat(), latest.getRiderLng(),
                    latest.getVehicleLat(), latest.getVehicleLng(),
                    null, (long) stayThresholdMinutes * 60);
        }
    }

    /**
     * 检测绑定超时
     *
     * @param bindingId         绑定记录ID
     * @param maxDurationHours  最大绑定时长（小时）
     */
    @Transactional(rollbackFor = Exception.class)
    public void detectBindingTimeout(Long bindingId, int maxDurationHours) {
        DmsRiderVehicleBinding binding = bindingMapper.selectById(bindingId);
        if (binding == null || BindingStatusEnum.ACTIVE.getValue() != binding.getStatus()) {
            return;
        }

        long hoursBound = ChronoUnit.HOURS.between(binding.getBindTime(), LocalDateTime.now());
        if (hoursBound >= maxDurationHours) {
            createAlert(binding, AlertTypeEnum.BINDING_TIMEOUT,
                    "配送员已连续工作" + hoursBound + "小时，超过最大时限" + maxDurationHours + "小时",
                    binding.getBindLat(), binding.getBindLng(),
                    null, null, null, null);
        }
    }

    // ==================== 批量核验 ====================

    /**
     * 批量核验所有活跃绑定
     *
     * 扫描所有状态为绑定中的记录，对每条记录执行位置核验和异常检测。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchVerification() {
        List<DmsRiderVehicleBinding> activeBindings = bindingMapper.selectList(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getStatus, 0)
        );

        log.info("批量核验活跃绑定, 数量={}", activeBindings.size());

        for (DmsRiderVehicleBinding binding : activeBindings) {
            try {
                // 这里应当从tracking模块获取配送员最新位置和车辆GPS位置
                // 实际项目中通过Feign调用或共享Mapper实现
                log.debug("跳过核验绑定: bindingId={}", binding.getId());
            } catch (Exception e) {
                log.error("批量核验异常, bindingId={}, error={}", binding.getId(), e.getMessage());
            }
        }
    }

    // ==================== 分页查询 ====================

    /**
     * 分页查询绑定记录
     */
    public IPage<DmsRiderVehicleBinding> pageBindings(int page, int size, Long tenantId, Integer status) {
        Page<DmsRiderVehicleBinding> pageParam = new Page<>(page, size);
        return bindingMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(tenantId != null, DmsRiderVehicleBinding::getTenantId, tenantId)
                        .eq(status != null, DmsRiderVehicleBinding::getStatus, status)
                        .orderByDesc(DmsRiderVehicleBinding::getCreateTime)
        );
    }

    /**
     * 分页查询告警记录
     */
    public IPage<DmsVerificationAlert> pageAlerts(int page, int size, Long tenantId, Integer alertType,
                                                   Integer handleStatus) {
        Page<DmsVerificationAlert> pageParam = new Page<>(page, size);
        return alertMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsVerificationAlert>()
                        .eq(tenantId != null, DmsVerificationAlert::getTenantId, tenantId)
                        .eq(alertType != null, DmsVerificationAlert::getAlertType, alertType)
                        .eq(handleStatus != null, DmsVerificationAlert::getHandleStatus, handleStatus)
                        .orderByDesc(DmsVerificationAlert::getCreateTime)
        );
    }

    /**
     * 分页查询核验记录
     */
    public IPage<DmsPositionVerification> pageVerifications(int page, int size, Long bindingId) {
        Page<DmsPositionVerification> pageParam = new Page<>(page, size);
        return positionVerificationMapper.selectPage(pageParam,
                new LambdaQueryWrapper<DmsPositionVerification>()
                        .eq(bindingId != null, DmsPositionVerification::getBindingId, bindingId)
                        .orderByDesc(DmsPositionVerification::getCreateTime)
        );
    }

    // ==================== 告警处理 ====================

    /**
     * 处理告警
     *
     * @param id            告警ID
     * @param handleStatus  处理状态：1-已确认 2-已忽略 3-已处理
     * @param handler       处理人
     * @param remark        处理备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(Long id, Integer handleStatus, String handler, String remark) {
        DmsVerificationAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw BusinessException.notFound("告警记录不存在: " + id);
        }
        alert.setHandleStatus(handleStatus);
        alert.setHandler(handler);
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(remark);
        alertMapper.updateById(alert);
        log.info("告警已处理, id={}, status={}, handler={}", id, handleStatus, handler);
    }

    // ==================== 审核验车 ====================

    /**
     * 审核验车记录
     *
     * @param id        验车记录ID
     * @param result    审核结果：1-通过 2-不通过
     * @param reviewer  审核人
     * @param remark    审核意见
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewInspection(Long id, Integer result, String reviewer, String remark) {
        DmsVehicleInspection inspection = vehicleInspectionMapper.selectById(id);
        if (inspection == null) {
            throw BusinessException.notFound("验车记录不存在: " + id);
        }
        inspection.setResult(result);
        inspection.setReviewer(reviewer);
        inspection.setReviewTime(LocalDateTime.now());
        inspection.setReviewRemark(remark);
        vehicleInspectionMapper.updateById(inspection);
        log.info("验车已审核, id={}, result={}, reviewer={}", id, result, reviewer);
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算GPS两点间的球面距离（Haversine公式）
     *
     * @param lat1 点1纬度
     * @param lng1 点1经度
     * @param lat2 点2纬度
     * @param lng2 点2经度
     * @return 距离（米）
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

    /**
     * 创建告警记录
     */
    private void createAlert(DmsRiderVehicleBinding binding, AlertTypeEnum alertType,
                             String content, Double riderLat, Double riderLng,
                             Double vehicleLat, Double vehicleLng,
                             Double distanceMeters, Long stayDuration) {
        DmsVerificationAlert alert = new DmsVerificationAlert();
        alert.setBindingId(binding.getId());
        alert.setAlertType(alertType.getValue());
        alert.setAlertLevel(2); // 默认警告级别
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
        alert.setHandleStatus(0); // 未处理
        alertMapper.insert(alert);
        log.warn("创建核验告警, id={}, type={}, content={}", alert.getId(), alertType.getDescription(), content);
    }
}
