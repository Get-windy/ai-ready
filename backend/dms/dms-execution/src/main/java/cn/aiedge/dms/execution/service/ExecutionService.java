package cn.aiedge.dms.execution.service;

import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.route.dto.RoutePlanRequest;
import cn.aiedge.dms.route.service.RouteService;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送执行服务
 *
 * 处理装车确认、取货确认、送达确认、催单和路线查询等配送执行逻辑。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final DmsTaskMapper taskMapper;
    private final RouteService routeService;

    /**
     * 确认装车
     *
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmLoad(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_ACCEPTED) {
            throw new DmsBusinessException("任务状态不允许确认装车，当前状态: " + task.getStatus());
        }

        task.setStatus(DmsConstants.TASK_PICKING_UP);
        task.setLoadTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("确认装车成功: taskId={}", taskId);
    }

    /**
     * 确认取货
     *
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPickup(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_PICKING_UP) {
            throw new DmsBusinessException("任务状态不允许确认取货，当前状态: " + task.getStatus());
        }

        task.setStatus(DmsConstants.TASK_DELIVERING);
        task.setPickupTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("确认取货成功: taskId={}", taskId);
    }

    /**
     * 确认送达
     *
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmArrive(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }
        if (task.getStatus() != DmsConstants.TASK_DELIVERING) {
            throw new DmsBusinessException("任务状态不允许确认送达，当前状态: " + task.getStatus());
        }

        task.setDeliveryTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("确认送达成功: taskId={}", taskId);
    }

    /**
     * 催单
     *
     * @param taskId 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void urge(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }

        task.setUrgeCount(task.getUrgeCount() == null ? 1 : task.getUrgeCount() + 1);
        task.setUrgeTime(LocalDateTime.now());
        taskMapper.updateById(task);

        log.info("催单成功: taskId={}, 当前催单次数={}", taskId, task.getUrgeCount());
    }

    /**
     * 获取优化路线
     *
     * @param taskId 任务ID
     * @return 路线规划结果
     */
    public Object getRoute(Long taskId) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }

        if (task.getSourceLat() == null || task.getSourceLng() == null
                || task.getCustomerLat() == null || task.getCustomerLng() == null) {
            throw new DmsBusinessException("任务坐标信息不完整，无法规划路线: taskId=" + taskId);
        }

        RoutePlanRequest.Coordinate origin = RoutePlanRequest.Coordinate.builder()
                .lat(task.getSourceLat().doubleValue())
                .lng(task.getSourceLng().doubleValue())
                .build();

        RoutePlanRequest.Coordinate dest = RoutePlanRequest.Coordinate.builder()
                .lat(task.getCustomerLat().doubleValue())
                .lng(task.getCustomerLng().doubleValue())
                .build();

        return routeService.planDeliveryRoute(origin, List.of(dest), 0);
    }
}
