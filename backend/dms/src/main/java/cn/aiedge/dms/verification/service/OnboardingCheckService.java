package cn.aiedge.dms.verification.service;

import cn.aiedge.dms.common.enums.RiderTypeEnum;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.verification.entity.DmsVehicleInspection;
import cn.aiedge.dms.common.enums.VehicleStatusEnum;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.enums.BindingStatusEnum;
import cn.aiedge.dms.verification.enums.VerifyStatusEnum;
import cn.aiedge.dms.verification.mapper.DmsRiderVehicleBindingMapper;
import cn.aiedge.dms.verification.mapper.DmsVehicleInspectionMapper;
import cn.aiedge.dms.verification.vo.EligibilityVO;
import cn.aiedge.dms.verification.vo.OnboardingCheckVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 准入核验（人证 × 车证一屏）
 *
 * <p>横切视图：把「人的合规（KYC + 证照）」与「车的合规（当前绑定车辆状态 + 车证 + 最近出车检查）」
 * 汇成一张表，给出「可否接单 / 可否出车」结论与阻塞原因。**数据全部来自既有表，不新建表、不重复采集。**</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingCheckService {

    private final DmsRiderMapper riderMapper;
    private final DmsRiderVehicleBindingMapper bindingMapper;
    private final DmsVehicleMapper vehicleMapper;
    private final DmsVehicleInspectionMapper vehicleInspectionMapper;
    private final KycService kycService;

    /**
     * 准入核验分页（逐配送员）
     *
     * @param keyword  姓名/手机号模糊
     * @param onlyBlocked 只看存在阻塞的
     */
    public IPage<OnboardingCheckVO> page(int page, int size, String keyword, Boolean onlyBlocked) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<DmsRider>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(DmsRider::getRealName, keyword)
                        .or().like(DmsRider::getPhone, keyword))
                .orderByAsc(DmsRider::getId);
        IPage<DmsRider> riders = riderMapper.selectPage(new Page<>(page, size), wrapper);
        IPage<OnboardingCheckVO> result = riders.convert(this::check);
        if (Boolean.TRUE.equals(onlyBlocked)) {
            List<OnboardingCheckVO> filtered = new ArrayList<>();
            for (OnboardingCheckVO vo : result.getRecords()) {
                if (!Boolean.TRUE.equals(vo.getCanDrive())) {
                    filtered.add(vo);
                }
            }
            result.setRecords(filtered);
        }
        return result;
    }

    /** 单个配送员的准入核验 */
    public OnboardingCheckVO check(DmsRider rider) {
        OnboardingCheckVO vo = new OnboardingCheckVO();
        vo.setRiderId(rider.getId());
        vo.setRiderName(rider.getRealName());
        vo.setRiderPhone(rider.getPhone());
        vo.setRiderType(rider.getRiderType());
        vo.setRiderTypeText(rider.getRiderType() == null ? null
                : RiderTypeEnum.fromValue(rider.getRiderType()).getDescription());

        List<String> reasons = new ArrayList<>();

        // ── 人的合规（复用 KYC 资质判定，单一口径） ──
        EligibilityVO elig = kycService.eligibility(rider.getId());
        vo.setEligible(elig.getEligible());
        vo.setVerifyStatus(elig.getVerifyStatus());
        vo.setVerifyStatusText(elig.getVerifyStatusText());
        if (!Boolean.TRUE.equals(elig.getEligible())) {
            reasons.addAll(elig.getReasons());
        }

        // ── 车的合规：当前绑定车辆 ──
        DmsRiderVehicleBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getRiderId, rider.getId())
                        .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                        .last("LIMIT 1"));
        if (binding == null) {
            vo.setVehicleCertOk(false);
            reasons.add("未绑定车辆，无法出车");
        } else {
            vo.setVehicleId(binding.getVehicleId());
            vo.setPlateNo(binding.getPlateNo());
            DmsVehicle vehicle = vehicleMapper.selectById(binding.getVehicleId());
            if (vehicle == null) {
                vo.setVehicleCertOk(false);
                reasons.add("绑定车辆不存在（数据异常）");
            } else {
                vo.setVehicleStatusText(VehicleStatusEnum.fromValue(vehicle.getStatus()).getDescription());
                boolean statusOk = !Objects.equals(vehicle.getStatus(), VehicleStatusEnum.REPAIRING.getValue())
                        && !Objects.equals(vehicle.getStatus(), VehicleStatusEnum.SCRAPPED.getValue());
                if (!statusOk) {
                    reasons.add("车辆当前为「" + vo.getVehicleStatusText() + "」，不可出车");
                }
                LocalDate earliest = earliestCertDate(vehicle);
                vo.setVehicleCertEarliestExpire(earliest);
                boolean certOk = earliest == null || !earliest.isBefore(LocalDate.now());
                vo.setVehicleCertOk(certOk);
                if (!certOk) {
                    reasons.add("车辆证照已过期（最早到期 " + earliest + "），请先在《车辆管理》更新");
                }
            }

            // ── 最近一次出车检查 ──
            DmsVehicleInspection lastInspection = vehicleInspectionMapper.selectOne(
                    new LambdaQueryWrapper<DmsVehicleInspection>()
                            .eq(DmsVehicleInspection::getVehicleId, binding.getVehicleId())
                            .eq(DmsVehicleInspection::getInspectionType, 1)
                            .orderByDesc(DmsVehicleInspection::getInspectionTime)
                            .last("LIMIT 1"));
            if (lastInspection != null) {
                vo.setLastInspectionResult(lastInspection.getResult());
                vo.setLastInspectionTime(lastInspection.getInspectionTime() == null ? null
                        : lastInspection.getInspectionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                if (!Integer.valueOf(1).equals(lastInspection.getResult())) {
                    reasons.add("最近一次出车前检查不通过（" + vo.getLastInspectionTime() + "）");
                }
            }
        }

        vo.setCanDrive(reasons.isEmpty());
        vo.setBlockReasons(reasons.isEmpty() ? null : String.join("；", reasons));

        // 证照数量统计（供"到期预警"一眼可见）
        if (elig.getReasons() != null) {
            for (String r : elig.getReasons()) {
                if (r.startsWith("证照已过期")) {
                    vo.setExpiredCertCount(1);
                }
            }
        }
        return vo;
    }

    /** 车辆三证（保险/年检/营运证）中最早的到期日 */
    private LocalDate earliestCertDate(DmsVehicle vehicle) {
        LocalDate earliest = null;
        for (LocalDate d : new LocalDate[]{vehicle.getInsuranceExpireDate(),
                vehicle.getInspectionExpireDate(), vehicle.getOperatingPermitExpireDate()}) {
            if (d != null && (earliest == null || d.isBefore(earliest))) {
                earliest = d;
            }
        }
        return earliest;
    }
}
