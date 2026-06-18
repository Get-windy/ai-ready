package cn.aiedge.dms.task.service;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.common.enums.TaskStatusEnum;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配送任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DmsTaskMapper taskMapper;

    /**
     * 分页查询配送任务
     */
    public IPage<DmsTask> page(Page<DmsTask> page, LambdaQueryWrapper<DmsTask> wrapper) {
        return taskMapper.selectPage(page, wrapper);
    }

    /**
     * 根据 ID 获取配送任务，不存在则抛异常
     */
    public DmsTask getById(Long id) {
        DmsTask task = taskMapper.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("配送任务不存在");
        }
        return task;
    }

    /**
     * 创建配送任务，初始状态为待分配
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsTask create(DmsTask task) {
        task.setStatus(TaskStatusEnum.PENDING.getValue());
        taskMapper.insert(task);
        log.info("Task created: id={}, taskNo={}", task.getId(), task.getTaskNo());
        return task;
    }

    /**
     * 更新配送任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(DmsTask task) {
        getById(task.getId()); // 确保存在
        taskMapper.updateById(task);
        log.info("Task updated: id={}", task.getId());
    }

    /**
     * 更新任务状态，校验状态转换合法性
     *
     * @param id         任务 ID
     * @param fromStatus 期望的当前状态
     * @param toStatus   目标状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer fromStatus, Integer toStatus) {
        DmsTask task = getById(id);
        TaskStatusEnum current = TaskStatusEnum.fromValue(task.getStatus());
        TaskStatusEnum target = TaskStatusEnum.fromValue(toStatus);

        if (!task.getStatus().equals(fromStatus)) {
            throw new BusinessException(
                    String.format("任务状态已变更，当前状态：%s，期望状态：%s",
                            current.getDescription(),
                            TaskStatusEnum.fromValue(fromStatus).getDescription()));
        }

        if (!current.canTransitionTo(target)) {
            throw new BusinessException(
                    String.format("无效的状态转换：%s -> %s", current.getDescription(), target.getDescription()));
        }

        task.setStatus(toStatus);
        taskMapper.updateById(task);
        log.info("Task {} status updated: {} -> {}", id, current.getDescription(), target.getDescription());
    }

    /**
     * 取消任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        DmsTask task = getById(id);
        TaskStatusEnum current = TaskStatusEnum.fromValue(task.getStatus());
        if (!current.canTransitionTo(TaskStatusEnum.CANCELLED)) {
            throw new BusinessException("当前状态不可取消：" + current.getDescription());
        }
        task.setStatus(TaskStatusEnum.CANCELLED.getValue());
        taskMapper.updateById(task);
        log.info("Task {} cancelled", id);
    }

    /**
     * 标记任务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void markException(Long id) {
        DmsTask task = getById(id);
        TaskStatusEnum current = TaskStatusEnum.fromValue(task.getStatus());
        if (!current.canTransitionTo(TaskStatusEnum.EXCEPTION)) {
            throw new BusinessException("当前状态不可标记异常：" + current.getDescription());
        }
        task.setStatus(TaskStatusEnum.EXCEPTION.getValue());
        taskMapper.updateById(task);
        log.info("Task {} marked as exception", id);
    }

    /**
     * 根据骑手 ID 获取任务列表
     */
    public List<DmsTask> getByRiderId(Long riderId, Integer status) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsTask::getRiderId, riderId);
        if (status != null) {
            wrapper.eq(DmsTask::getStatus, status);
        }
        wrapper.orderByDesc(DmsTask::getCreateTime);
        return taskMapper.selectList(wrapper);
    }

    /**
     * 统计骑手活跃任务数量（已接单、取货中、配送中）
     */
    public long getActiveTaskCount(Long riderId) {
        LambdaQueryWrapper<DmsTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DmsTask::getRiderId, riderId)
                .in(DmsTask::getStatus,
                        TaskStatusEnum.ACCEPTED.getValue(),
                        TaskStatusEnum.PICKING_UP.getValue(),
                        TaskStatusEnum.DELIVERING.getValue());
        return taskMapper.selectCount(wrapper);
    }
}
