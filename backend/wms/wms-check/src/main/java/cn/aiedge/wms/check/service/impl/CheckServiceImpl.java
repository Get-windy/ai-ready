package cn.aiedge.wms.check.service.impl;

import cn.aiedge.wms.entity.WmsCheckTask;
import cn.aiedge.wms.entity.WmsCheckResult;
import cn.aiedge.wms.enums.WmsTaskStatus;
import cn.aiedge.wms.check.mapper.WmsCheckTaskMapper;
import cn.aiedge.wms.check.mapper.WmsCheckResultMapper;
import cn.aiedge.wms.check.service.CheckService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckServiceImpl implements CheckService {

    private final WmsCheckTaskMapper taskMapper;
    private final WmsCheckResultMapper resultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTask(WmsCheckTask task) {
        return taskMapper.insert(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(WmsCheckTask task) {
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public WmsCheckTask getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Page<WmsCheckTask> pageTask(Page<WmsCheckTask> page, WmsCheckTask query) {
        LambdaQueryWrapper<WmsCheckTask> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getId() != null) {
                wrapper.eq(WmsCheckTask::getId, query.getId());
            }
            if (query.getTaskNo() != null) {
                wrapper.like(WmsCheckTask::getTaskNo, query.getTaskNo());
            }
            if (query.getWarehouseId() != null) {
                wrapper.eq(WmsCheckTask::getWarehouseId, query.getWarehouseId());
            }
            if (query.getCheckType() != null) {
                wrapper.eq(WmsCheckTask::getCheckType, query.getCheckType());
            }
            if (query.getScopeType() != null) {
                wrapper.eq(WmsCheckTask::getScopeType, query.getScopeType());
            }
            if (query.getStatus() != null) {
                wrapper.eq(WmsCheckTask::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(WmsCheckTask::getId);
        return taskMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        return taskMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveResult(WmsCheckResult result) {
        return resultMapper.insert(result) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateResult(WmsCheckResult result) {
        return resultMapper.updateById(result) > 0;
    }

    @Override
    public List<WmsCheckResult> listByTaskId(Long taskId) {
        LambdaQueryWrapper<WmsCheckResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsCheckResult::getTaskId, taskId);
        wrapper.orderByAsc(WmsCheckResult::getLineNo);
        return resultMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startCheck(Long taskId, Long userId, String userName) {
        WmsCheckTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != WmsTaskStatus.PENDING) return;
        task.setStatus(WmsTaskStatus.IN_PROGRESS);
        task.setAssigneeId(userId);
        task.setAssigneeName(userName);
        taskMapper.updateById(task);
        log.info("盘点任务开始: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitResult(Long taskId, Long userId, String userName) {
        WmsCheckTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != WmsTaskStatus.IN_PROGRESS) return;
        task.setStatus(WmsTaskStatus.COMPLETED);
        task.setCheckerId(userId);
        task.setCheckerName(userName);
        taskMapper.updateById(task);
        log.info("盘点任务提交: taskId={}, userId={}", taskId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCheck(Long taskId, Long userId) {
        WmsCheckTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != WmsTaskStatus.COMPLETED) return;
        task.setStatus(3); // 已审核 — WmsTaskStatus has no APPROVED, so keep as 3
        task.setApprovedBy(userId);
        task.setApprovedTime(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("盘点任务审核: taskId={}, userId={}", taskId, userId);
    }
}
