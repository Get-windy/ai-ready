package cn.aiedge.dms.rider.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送员管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiderService {

    private final DmsRiderMapper riderMapper;

    /**
     * 分页查询配送员
     */
    public Page<DmsRider> page(Page<DmsRider> page, DmsRider query) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<>(query)
                .orderByDesc(DmsRider::getCreateTime);
        return riderMapper.selectPage(page, wrapper);
    }

    /**
     * 获取配送员详情
     */
    public DmsRider getById(Long id) {
        DmsRider rider = riderMapper.selectById(id);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        return rider;
    }

    /**
     * 新增配送员
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsRider create(DmsRider rider) {
        rider.setTenantId(0L);
        riderMapper.insert(rider);
        log.info("新增配送员: id={}, name={}, phone={}", rider.getId(), rider.getRealName(), rider.getPhone());
        return rider;
    }

    /**
     * 更新配送员
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsRider update(Long id, DmsRider dto) {
        DmsRider rider = getById(id);
        rider.setRealName(dto.getRealName());
        rider.setPhone(dto.getPhone());
        rider.setIdCard(dto.getIdCard());
        rider.setRiderType(dto.getRiderType());
        rider.setVehicleType(dto.getVehicleType());
        rider.setVehicleNo(dto.getVehicleNo());
        rider.setChannelId(dto.getChannelId());
        rider.setServiceRadius(dto.getServiceRadius());
        rider.setDepositAmount(dto.getDepositAmount());
        rider.setMaxConcurrent(dto.getMaxConcurrent());
        rider.setWorkHoursStart(dto.getWorkHoursStart());
        rider.setWorkHoursEnd(dto.getWorkHoursEnd());
        rider.setRemark(dto.getRemark());
        riderMapper.updateById(rider);
        return rider;
    }

    /**
     * 更新配送员状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        DmsRider rider = getById(id);
        rider.setStatus(status);
        riderMapper.updateById(rider);
        log.info("配送员状态更新: id={}, status={}", id, status);
    }

    /**
     * 更新配送员位置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLocation(Long id, BigDecimal lat, BigDecimal lng) {
        DmsRider rider = getById(id);
        rider.setCurrentLat(lat);
        rider.setCurrentLng(lng);
        rider.setLastReportTime(LocalDateTime.now());
        riderMapper.updateById(rider);
    }

    /**
     * 更新配送员审核状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateVerifyStatus(Long id, Integer verifyStatus) {
        DmsRider rider = getById(id);
        rider.setVerifyStatus(verifyStatus);
        riderMapper.updateById(rider);
        log.info("配送员审核: id={}, verifyStatus={}", id, verifyStatus);
    }

    /**
     * 删除配送员
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        riderMapper.deleteById(id);
        log.info("删除配送员: id={}", id);
    }

    /**
     * 获取服务范围内空闲配送员
     */
    public List<DmsRider> getAvailableRiders(BigDecimal lat, BigDecimal lng, BigDecimal radius) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<DmsRider>()
                .eq(DmsRider::getStatus, 1)          // 空闲
                .eq(DmsRider::getVerifyStatus, 1)    // 已通过
                .ge(DmsRider::getServiceRadius, radius)
                .orderByDesc(DmsRider::getRatingScore);
        return riderMapper.selectList(wrapper);
    }
}
