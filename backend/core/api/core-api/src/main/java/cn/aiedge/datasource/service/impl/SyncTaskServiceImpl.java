package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.mapper.SyncTaskMapper;
import cn.aiedge.datasource.model.SyncTask;
import cn.aiedge.datasource.service.SyncTaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步任务服务实现
 */
@Service
@RequiredArgsConstructor
public class SyncTaskServiceImpl implements SyncTaskService {

    private final SyncTaskMapper syncTaskMapper;

    @Override
    public List<SyncTask> list(Long tenantId) {
        LambdaQueryWrapper<SyncTask> wrapper = new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getDeleted, false)
                .eq(tenantId != null, SyncTask::getTenantId, tenantId)
                .orderByDesc(SyncTask::getCreateTime);
        return syncTaskMapper.selectList(wrapper);
    }

    @Override
    public SyncTask create(SyncTask syncTask, Long tenantId, String createBy) {
        LocalDateTime now = LocalDateTime.now();
        syncTask.setTenantId(tenantId);
        syncTask.setCreateBy(createBy);
        syncTask.setUpdateBy(createBy);
        syncTask.setCreateTime(now);
        syncTask.setUpdateTime(now);
        syncTask.setDeleted(false);
        if (syncTask.getStatus() == null) syncTask.setStatus("stopped");
        syncTaskMapper.insert(syncTask);
        return syncTask;
    }

    @Override
    public SyncTask update(Long id, SyncTask syncTask, Long tenantId, String updateBy) {
        SyncTask existing = syncTaskMapper.selectById(id);
        if (existing == null || existing.getDeleted()) return null;
        if (syncTask.getSourceId() != null) existing.setSourceId(syncTask.getSourceId());
        if (syncTask.getTargetId() != null) existing.setTargetId(syncTask.getTargetId());
        if (syncTask.getTaskName() != null) existing.setTaskName(syncTask.getTaskName());
        if (syncTask.getSyncType() != null) existing.setSyncType(syncTask.getSyncType());
        if (syncTask.getCronExpression() != null) existing.setCronExpression(syncTask.getCronExpression());
        if (syncTask.getStatus() != null) existing.setStatus(syncTask.getStatus());
        if (syncTask.getDescription() != null) existing.setDescription(syncTask.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateBy(updateBy);
        if (tenantId != null) existing.setTenantId(tenantId);
        syncTaskMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        SyncTask existing = syncTaskMapper.selectById(id);
        if (existing == null || existing.getDeleted()) return false;
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        return syncTaskMapper.updateById(existing) > 0;
    }

    @Override
    public boolean execute(Long id) {
        SyncTask task = syncTaskMapper.selectById(id);
        if (task == null || task.getDeleted()) return false;
        task.setLastSyncTime(LocalDateTime.now());
        task.setStatus("running");
        task.setUpdateTime(LocalDateTime.now());
        return syncTaskMapper.updateById(task) > 0;
    }
}
