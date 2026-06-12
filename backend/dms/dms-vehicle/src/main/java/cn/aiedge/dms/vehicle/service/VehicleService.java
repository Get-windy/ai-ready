package cn.aiedge.dms.vehicle.service;

import cn.aiedge.dms.vehicle.dto.VehicleCreateDTO;
import cn.aiedge.dms.vehicle.dto.MaintenanceCreateDTO;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.entity.DmsVehicleMaintenance;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMaintenanceMapper;
import cn.aiedge.dms.vehicle.mapper.DmsVehicleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 车辆管理服务
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final DmsVehicleMapper vehicleMapper;
    private final DmsVehicleMaintenanceMapper maintenanceMapper;

    /**
     * 分页查询车辆
     */
    public Page<DmsVehicle> page(Page<DmsVehicle> page, DmsVehicle query) {
        LambdaQueryWrapper<DmsVehicle> wrapper = new LambdaQueryWrapper<>(query)
                .orderByDesc(DmsVehicle::getCreateTime);
        return vehicleMapper.selectPage(page, wrapper);
    }

    /**
     * 获取车辆详情
     */
    public DmsVehicle getById(Long id) {
        return vehicleMapper.selectById(id);
    }

    /**
     * 新增车辆
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsVehicle create(VehicleCreateDTO dto) {
        DmsVehicle vehicle = new DmsVehicle();
        vehicle.setTenantId(0L); // 实际从上下文获取
        vehicle.setVehicleCode(generateVehicleCode());
        vehicle.setPlateNo(dto.getPlateNo());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setColor(dto.getColor());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setVin(dto.getVin());
        vehicle.setEngineNo(dto.getEngineNo());
        vehicle.setRatedLoad(dto.getRatedLoad());
        vehicle.setRatedPassenger(dto.getRatedPassenger());
        vehicle.setLengthCm(dto.getLengthCm());
        vehicle.setWidthCm(dto.getWidthCm());
        vehicle.setHeightCm(dto.getHeightCm());
        vehicle.setCargoVolume(dto.getCargoVolume());
        vehicle.setRegisterDate(dto.getRegisterDate());
        vehicle.setOperatingPermitNo(dto.getOperatingPermitNo());
        vehicle.setInsuranceExpireDate(dto.getInsuranceExpireDate());
        vehicle.setInspectionExpireDate(dto.getInspectionExpireDate());
        vehicle.setMaintenanceIntervalKm(dto.getMaintenanceIntervalKm());
        vehicle.setOwnershipType(dto.getOwnershipType());
        vehicle.setDepartment(dto.getDepartment());
        vehicle.setStatus(0); // 默认空闲
        vehicle.setRemark(dto.getRemark());
        vehicleMapper.insert(vehicle);
        log.info("新增车辆: id={}, plateNo={}", vehicle.getId(), vehicle.getPlateNo());
        return vehicle;
    }

    /**
     * 更新车辆
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsVehicle update(Long id, VehicleCreateDTO dto) {
        DmsVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }
        vehicle.setPlateNo(dto.getPlateNo());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setColor(dto.getColor());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicle.setRatedLoad(dto.getRatedLoad());
        vehicle.setCargoVolume(dto.getCargoVolume());
        vehicle.setInsuranceExpireDate(dto.getInsuranceExpireDate());
        vehicle.setInspectionExpireDate(dto.getInspectionExpireDate());
        vehicle.setOwnershipType(dto.getOwnershipType());
        vehicle.setDepartment(dto.getDepartment());
        vehicle.setRemark(dto.getRemark());
        vehicleMapper.updateById(vehicle);
        return vehicle;
    }

    /**
     * 更新车辆状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        DmsVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }
        vehicle.setStatus(status);
        vehicleMapper.updateById(vehicle);
        log.info("车辆状态更新: id={}, status={}", id, status);
    }

    /**
     * 绑定驾驶员
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindRider(Long vehicleId, Long riderId, String riderName) {
        DmsVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }
        vehicle.setCurrentRiderId(riderId);
        vehicle.setCurrentRiderName(riderName);
        vehicleMapper.updateById(vehicle);
        log.info("车辆绑定驾驶员: vehicleId={}, riderId={}", vehicleId, riderId);
    }

    /**
     * 更新里程
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMileage(Long id, Integer mileage) {
        DmsVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new RuntimeException("车辆不存在");
        }
        vehicle.setCurrentMileage(mileage);
        vehicleMapper.updateById(vehicle);
    }

    /**
     * 获取待保养提醒的车辆列表
     */
    public Page<DmsVehicle> pageDueForMaintenance(Page<DmsVehicle> page, Integer warnKm) {
        LambdaQueryWrapper<DmsVehicle> wrapper = new LambdaQueryWrapper<DmsVehicle>()
                .lt(DmsVehicle::getInsuranceExpireDate, LocalDate.now().plusDays(30))
                .or()
                .lt(DmsVehicle::getInspectionExpireDate, LocalDate.now().plusDays(30))
                .or()
                .apply("current_mileage - last_maintenance_km >= {0}", warnKm)
                .orderByDesc(DmsVehicle::getCreateTime);
        return vehicleMapper.selectPage(page, wrapper);
    }

    /**
     * 删除车辆
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        vehicleMapper.deleteById(id);
        log.info("删除车辆: id={}", id);
    }

    // ==================== 维保管理 ====================

    /**
     * 分页查询维保记录
     */
    public Page<DmsVehicleMaintenance> pageMaintenance(Page<DmsVehicleMaintenance> page, Long vehicleId) {
        LambdaQueryWrapper<DmsVehicleMaintenance> wrapper = new LambdaQueryWrapper<DmsVehicleMaintenance>()
                .eq(vehicleId != null, DmsVehicleMaintenance::getVehicleId, vehicleId)
                .orderByDesc(DmsVehicleMaintenance::getMaintDate);
        return maintenanceMapper.selectPage(page, wrapper);
    }

    /**
     * 新增维保记录
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsVehicleMaintenance createMaintenance(MaintenanceCreateDTO dto) {
        DmsVehicleMaintenance record = new DmsVehicleMaintenance();
        record.setTenantId(0L);
        record.setVehicleId(dto.getVehicleId());
        record.setMaintType(dto.getMaintType());
        record.setMaintNo(generateMaintNo());
        record.setMaintDate(LocalDate.now());
        record.setMaintContent(dto.getMaintContent());
        record.setMaintCost(dto.getMaintCost());
        record.setMaintVendor(dto.getMaintVendor());
        record.setMaintContact(dto.getMaintContact());
        record.setMaintPhone(dto.getMaintPhone());
        record.setAfterMaintMileage(dto.getAfterMaintMileage());
        record.setAttachmentUrls(dto.getAttachmentUrls());
        record.setRemark(dto.getRemark());
        maintenanceMapper.insert(record);

        // 更新车辆保养信息
        if (dto.getAfterMaintMileage() != null) {
            updateMileage(dto.getVehicleId(), dto.getAfterMaintMileage());
        }

        log.info("新增维保记录: id={}, vehicleId={}, type={}",
                record.getId(), dto.getVehicleId(), dto.getMaintType());
        return record;
    }

    /**
     * 删除维保记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaintenance(Long id) {
        maintenanceMapper.deleteById(id);
    }

    // ==================== 私有方法 ====================

    private String generateVehicleCode() {
        return "VH" + System.currentTimeMillis();
    }

    private String generateMaintNo() {
        return "MT" + System.currentTimeMillis();
    }
}
