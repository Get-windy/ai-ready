package cn.aiedge.dms.dispatch.service;

import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能调度服务
 *
 * 处理自动分配、手动指派、改派、候选骑手查询和围栏校验等调度逻辑。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private final DmsTaskMapper taskMapper;
    private final DmsRiderMapper riderMapper;

    /**
     * 自动调度 - 扫描待分配任务，查找空闲骑手并分配最佳匹配
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoDispatch() {
        // 查询所有待分配任务
        List<DmsTask> pendingTasks = taskMapper.selectList(
                new LambdaQueryWrapper<DmsTask>()
                        .eq(DmsTask::getStatus, DmsConstants.TASK_PENDING)
        );

        if (pendingTasks.isEmpty()) {
            log.info("没有待分配的任务，自动调度结束");
            return;
        }

        for (DmsTask task : pendingTasks) {
            try {
                // 查找空闲骑手
                List<DmsRider> idleRiders = riderMapper.selectList(
                        new LambdaQueryWrapper<DmsRider>()
                                .eq(DmsRider::getStatus, DmsConstants.RIDER_STATUS_IDLE)
                );

                if (idleRiders.isEmpty()) {
                    log.warn("任务 {} 无可用的空闲骑手", task.getId());
                    continue;
                }

                // 筛选地理围栏内的骑手并按照距离排序（最佳匹配策略）
                List<DmsRider> candidates = idleRiders.stream()
                        .filter(rider -> isWithinGeofence(task, rider))
                        .sorted((a, b) -> {
                            double distA = calculateDistance(task, a);
                            double distB = calculateDistance(task, b);
                            return Double.compare(distA, distB);
                        })
                        .collect(Collectors.toList());

                if (candidates.isEmpty()) {
                    log.warn("任务 {} 围栏内无可用骑手", task.getId());
                    continue;
                }

                // 分配最近骑手
                DmsRider bestRider = candidates.get(0);
                assignTask(task, bestRider);
                log.info("自动调度成功: taskId={}, riderId={}", task.getId(), bestRider.getId());

            } catch (Exception e) {
                log.error("自动调度任务 {} 失败: {}", task.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 手动指派骑手
     *
     * @param taskId  任务ID
     * @param riderId 骑手ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRider(Long taskId, Long riderId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_PENDING) {
            throw new DmsBusinessException("任务状态不允许分配，当前状态: " + task.getStatus());
        }

        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw new DmsBusinessException("骑手不存在: " + riderId);
        }
        if (rider.getStatus() != DmsConstants.RIDER_STATUS_IDLE) {
            throw new DmsBusinessException("骑手当前不可用，状态: " + rider.getStatus());
        }

        assignTask(task, rider);
        log.info("手动指派成功: taskId={}, riderId={}", taskId, riderId);
    }

    /**
     * 改派任务
     *
     * @param taskId      任务ID
     * @param fromRiderId 原骑手ID
     * @param toRiderId   新骑手ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void reassign(Long taskId, Long fromRiderId, Long toRiderId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_ASSIGNED && task.getStatus() != DmsConstants.TASK_ACCEPTED) {
            throw new DmsBusinessException("任务状态不允许改派，当前状态: " + task.getStatus());
        }

        // 释放原骑手
        DmsRider fromRider = riderMapper.selectById(fromRiderId);
        if (fromRider != null) {
            fromRider.setStatus(DmsConstants.RIDER_STATUS_IDLE);
            riderMapper.updateById(fromRider);
        }

        // 分配新骑手
        DmsRider toRider = riderMapper.selectById(toRiderId);
        if (toRider == null) {
            throw new DmsBusinessException("新骑手不存在: " + toRiderId);
        }
        if (toRider.getStatus() != DmsConstants.RIDER_STATUS_IDLE) {
            throw new DmsBusinessException("新骑手当前不可用，状态: " + toRider.getStatus());
        }

        toRider.setStatus(DmsConstants.RIDER_STATUS_BUSY);
        riderMapper.updateById(toRider);

        // 保持任务为已分配状态
        task.setStatus(DmsConstants.TASK_ASSIGNED);
        taskMapper.updateById(task);

        log.info("改派成功: taskId={}, fromRiderId={}, toRiderId={}", taskId, fromRiderId, toRiderId);
    }

    /**
     * 查询符合条件的地骑手候选列表
     *
     * @param taskId 任务ID
     * @return 候选骑手列表
     */
    public List<DmsRider> getCandidates(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }

        List<DmsRider> idleRiders = riderMapper.selectList(
                new LambdaQueryWrapper<DmsRider>()
                        .eq(DmsRider::getStatus, DmsConstants.RIDER_STATUS_IDLE)
        );

        return idleRiders.stream()
                .filter(rider -> isWithinGeofence(task, rider))
                .collect(Collectors.toList());
    }

    /**
     * 围栏校验 - 检查骑手是否在任务服务区域内
     *
     * @param taskId  任务ID
     * @param riderId 骑手ID
     * @return true 若在服务区域内, false 否则
     */
    public boolean fenceCheck(Long taskId, Long riderId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }

        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw new DmsBusinessException("骑手不存在: " + riderId);
        }

        boolean withinRange = isWithinGeofence(task, rider);
        log.info("围栏校验: taskId={}, riderId={}, 结果={}", taskId, riderId, withinRange);
        return withinRange;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 判断骑手是否在任务的地理围栏范围内
     */
    private boolean isWithinGeofence(DmsTask task, DmsRider rider) {
        if (rider.getCurrentLat() == null || rider.getCurrentLng() == null) {
            return false;
        }
        if (task.getSourceLat() == null || task.getSourceLng() == null) {
            return false;
        }
        // 简化的距离计算（实际应使用 Haversine 公式或调用地图服务）
        double distance = calculateDistance(task, rider);
        // 默认围栏半径 5000 米
        return distance <= 5000;
    }

    /**
     * 计算骑手与任务取货点的直线距离（米）
     */
    private double calculateDistance(DmsTask task, DmsRider rider) {
        double latDiff = rider.getCurrentLat().doubleValue() - task.getSourceLat().doubleValue();
        double lngDiff = rider.getCurrentLng().doubleValue() - task.getSourceLng().doubleValue();
        // 近似：1度约等于 111320 米
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111320;
    }

    /**
     * 执行任务分配（更新任务和骑手状态）
     */
    private void assignTask(DmsTask task, DmsRider rider) {
        task.setStatus(DmsConstants.TASK_ASSIGNED);
        task.setRiderId(rider.getId());
        task.setDispatchTime(LocalDateTime.now());
        taskMapper.updateById(task);

        rider.setStatus(DmsConstants.RIDER_STATUS_BUSY);
        riderMapper.updateById(rider);
    }
}
