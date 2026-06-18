package cn.aiedge.wms.putaway.service.impl;

import cn.aiedge.wms.entity.WmsPutawayTask;
import cn.aiedge.wms.entity.WmsPutawayDetail;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.exception.WmsBusinessException;
import cn.aiedge.wms.putaway.mapper.WmsPutawayTaskMapper;
import cn.aiedge.wms.putaway.mapper.WmsPutawayDetailMapper;
import cn.aiedge.wms.putaway.service.PutawayService;
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
public class PutawayServiceImpl implements PutawayService {

    private final WmsPutawayTaskMapper taskMapper;
    private final WmsPutawayDetailMapper detailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsPutawayTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsPutawayTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsPutawayTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsPutawayTask> pageTask(Page<WmsPutawayTask> page, WmsPutawayTask query) {
        LambdaQueryWrapper<WmsPutawayTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsPutawayTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsPutawayTask::getTaskNo, query.getTaskNo());
            }
            if (query.getSourceType() != null) {
                wrapper.eq(WmsPutawayTask::getSourceType, query.getSourceType());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsPutawayTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsPutawayTask::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(WmsPutawayTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetail(WmsPutawayDetail detail) {
        return detailMapper.insert(detail) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDetail(WmsPutawayDetail detail) {
        return detailMapper.updateById(detail) > 0;
    }

    @Override
    public List<WmsPutawayDetail> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsPutawayDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsPutawayDetail::getTaskId, taskId);
        wrapper.orderByAsc(WmsPutawayDetail::getLineNo);
        return detailMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startPutaway(Long taskId, Long userId, String userName) {
        WmsPutawayTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "上架");
        if (task.getStatus() != WmsTaskStatus.PENDING) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.PENDING);
        }
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("上架任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPutaway(Long taskId, Long userId, String userName) {
        WmsPutawayTask task = taskMapper.selectById(taskId);
        if (task == null) throw WmsBusinessException.taskNotFound(taskId, "上架");
        if (task.getStatus() != WmsTaskStatus.IN_PROGRESS) {
            throw WmsBusinessException.invalidStatus(task.getTaskNo(), task.getStatus(), WmsTaskStatus.IN_PROGRESS);
        }
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("上架任务完成: taskId={}, userId={}", taskId, userId);
    }
}
