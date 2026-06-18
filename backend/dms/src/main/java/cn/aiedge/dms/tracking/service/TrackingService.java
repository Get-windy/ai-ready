package cn.aiedge.dms.tracking.service;

import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.tracking.entity.DmsTracking;
import cn.aiedge.dms.tracking.mapper.DmsTrackingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 位置追踪服务
 *
 * 处理配送员GPS位置上报、轨迹查询、历史数据清理等业务逻辑。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final DmsTrackingMapper trackingMapper;
    private final DmsRiderMapper riderMapper;

    /**
     * 上报位置
     *
     * 插入追踪记录，并更新配送员的当前经纬度和最后上报时间。
     *
     * @param riderId   配送员ID
     * @param taskId    任务ID（可为null）
     * @param lat       纬度
     * @param lng       经度
     * @param speed     速度（km/h）
     * @param direction 方向角度
     */
    @Transactional(rollbackFor = Exception.class)
    public void reportLocation(Long riderId, Long taskId, BigDecimal lat, BigDecimal lng,
                               BigDecimal speed, BigDecimal direction) {
        // 插入追踪记录
        DmsTracking record = new DmsTracking();
        record.setRiderId(riderId);
        record.setTaskId(taskId);
        record.setLat(lat);
        record.setLng(lng);
        record.setSpeed(speed);
        record.setDirection(direction);
        record.setReportTime(LocalDateTime.now());
        record.setSource(1);
        trackingMapper.insert(record);

        // 更新配送员当前位置信息
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider != null) {
            rider.setCurrentLat(lat);
            rider.setCurrentLng(lng);
            rider.setLastReportTime(LocalDateTime.now());
            riderMapper.updateById(rider);
        }
    }

    /**
     * 获取某配送员的最新位置
     *
     * @param riderId 配送员ID
     * @return 最新追踪记录，无记录时返回null
     */
    public DmsTracking getLatestLocation(Long riderId) {
        return trackingMapper.selectOne(
                new LambdaQueryWrapper<DmsTracking>()
                        .eq(DmsTracking::getRiderId, riderId)
                        .orderByDesc(DmsTracking::getReportTime)
                        .last("LIMIT 1")
        );
    }

    /**
     * 获取配送员在指定时间范围内的轨迹
     *
     * @param riderId   配送员ID
     * @param startTime 起始时间
     * @param endTime   结束时间
     * @return 追踪记录列表（按时间升序）
     */
    public List<DmsTracking> getTrack(Long riderId, LocalDateTime startTime, LocalDateTime endTime) {
        return trackingMapper.selectList(
                new LambdaQueryWrapper<DmsTracking>()
                        .eq(DmsTracking::getRiderId, riderId)
                        .ge(startTime != null, DmsTracking::getReportTime, startTime)
                        .le(endTime != null, DmsTracking::getReportTime, endTime)
                        .orderByAsc(DmsTracking::getReportTime)
        );
    }

    /**
     * 获取某任务的全部追踪记录
     *
     * @param taskId 任务ID
     * @return 追踪记录列表（按时间升序）
     */
    public List<DmsTracking> getTrackByTask(Long taskId) {
        return trackingMapper.selectList(
                new LambdaQueryWrapper<DmsTracking>()
                        .eq(DmsTracking::getTaskId, taskId)
                        .orderByAsc(DmsTracking::getReportTime)
        );
    }

    /**
     * 清理指定天数前的过期数据
     *
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanExpiredData(int retentionDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(retentionDays);
        return trackingMapper.delete(
                new LambdaQueryWrapper<DmsTracking>()
                        .lt(DmsTracking::getCreateTime, deadline)
        );
    }
}
