package cn.aiedge.wms.move.service.impl;

import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.entity.WmsMoveDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.move.mapper.WmsMoveTaskMapper;
import cn.aiedge.wms.move.mapper.WmsMoveDetailMapper;
import cn.aiedge.wms.move.service.MoveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoveServiceImpl implements MoveService {

    private final WmsMoveTaskMapper taskMapper;
    private final WmsMoveDetailMapper detailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsMoveTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsMoveTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsMoveTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsMoveTask> pageTask(Page<WmsMoveTask> page, WmsMoveTask query) {
        LambdaQueryWrapper<WmsMoveTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsMoveTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsMoveTask::getTaskNo, query.getTaskNo());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsMoveTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsMoveTask::getStatus, query.getStatus());
            }
            if (query.getMoveType() != null) {
                wrapper.eq(WmsMoveTask::getMoveType, query.getMoveType());
            }
        }
        wrapper.orderByDesc(WmsMoveTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsMoveDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsMoveDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public List<WmsMoveDetail> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsMoveDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsMoveDetail::getTaskId, taskId);
        wrapper.orderByAsc(WmsMoveDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startMove(Long taskId, Long userId, String userName) {
        WmsMoveTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "移库");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("移库任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeMove(Long taskId, Long userId, String userName) {
        WmsMoveTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "移库");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("移库任务完成: taskId={}, userId={}", taskId, userId);
    }
}
