package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.model.SyncTask;
import cn.aiedge.datasource.service.SyncTaskService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 同步任务服务实现
 */
@Service
public class SyncTaskServiceImpl implements SyncTaskService {

    private final List<SyncTask> syncTaskList = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @PostConstruct
    public void init() {
        SyncTask st1 = new SyncTask();
        st1.setId(idCounter.getAndIncrement());
        st1.setSourceId(1L);
        st1.setTargetId(2L);
        st1.setTaskName("MySQL同步到PostgreSQL");
        st1.setSyncType("full");
        st1.setCronExpression("0 0 3 * * ?");
        st1.setStatus("running");
        st1.setLastSyncTime(LocalDateTime.now().minusDays(1).withHour(3).withMinute(0));
        st1.setNextSyncTime(LocalDateTime.now().plusDays(1).withHour(3).withMinute(0));
        st1.setDescription("每日凌晨3点将MySQL数据全量同步到PostgreSQL");
        st1.setTenantId(1L);
        st1.setCreateTime(LocalDateTime.now().minusDays(30));
        st1.setUpdateTime(LocalDateTime.now().minusDays(1));
        st1.setCreateBy("admin");
        st1.setUpdateBy("admin");
        st1.setDeleted(false);
        syncTaskList.add(st1);

        SyncTask st2 = new SyncTask();
        st2.setId(idCounter.getAndIncrement());
        st2.setSourceId(1L);
        st2.setTargetId(3L);
        st2.setTaskName("MySQL增量同步到Oracle");
        st2.setSyncType("incremental");
        st2.setCronExpression("0 */30 * * * ?");
        st2.setStatus("paused");
        st2.setLastSyncTime(LocalDateTime.now().minusDays(5));
        st2.setNextSyncTime(null);
        st2.setDescription("每半小时增量同步MySQL数据到Oracle（已暂停）");
        st2.setTenantId(1L);
        st2.setCreateTime(LocalDateTime.now().minusDays(20));
        st2.setUpdateTime(LocalDateTime.now().minusDays(5));
        st2.setCreateBy("admin");
        st2.setUpdateBy("admin");
        st2.setDeleted(false);
        syncTaskList.add(st2);

        SyncTask st3 = new SyncTask();
        st3.setId(idCounter.getAndIncrement());
        st3.setSourceId(2L);
        st3.setTargetId(1L);
        st3.setTaskName("PostgreSQL回同步到MySQL");
        st3.setSyncType("incremental");
        st3.setCronExpression("0 0 6 * * ?");
        st3.setStatus("stopped");
        st3.setLastSyncTime(LocalDateTime.now().minusDays(10));
        st3.setNextSyncTime(null);
        st3.setDescription("每日早上6点将PostgreSQL分析结果回同步到MySQL（已停止）");
        st3.setTenantId(1L);
        st3.setCreateTime(LocalDateTime.now().minusDays(15));
        st3.setUpdateTime(LocalDateTime.now().minusDays(10));
        st3.setCreateBy("analyst");
        st3.setUpdateBy("admin");
        st3.setDeleted(false);
        syncTaskList.add(st3);

        SyncTask st4 = new SyncTask();
        st4.setId(idCounter.getAndIncrement());
        st4.setSourceId(1L);
        st4.setTargetId(4L);
        st4.setTaskName("MySQL同步到SQLServer分析库");
        st4.setSyncType("full");
        st4.setCronExpression("0 0 2 * * ?");
        st4.setStatus("running");
        st4.setLastSyncTime(LocalDateTime.now().minusDays(1).withHour(2).withMinute(0));
        st4.setNextSyncTime(LocalDateTime.now().plusDays(1).withHour(2).withMinute(0));
        st4.setDescription("每日凌晨2点同步业务数据到分析库");
        st4.setTenantId(2L);
        st4.setCreateTime(LocalDateTime.now().minusDays(7));
        st4.setUpdateTime(LocalDateTime.now().minusDays(1));
        st4.setCreateBy("analyst");
        st4.setUpdateBy("analyst");
        st4.setDeleted(false);
        syncTaskList.add(st4);
    }

    @Override
    public List<SyncTask> list(Long tenantId) {
        return syncTaskList.stream()
                .filter(st -> !st.getDeleted())
                .filter(st -> tenantId == null || tenantId.equals(st.getTenantId()))
                .collect(Collectors.toList());
    }

    @Override
    public SyncTask create(SyncTask syncTask, Long tenantId, String createBy) {
        syncTask.setId(idCounter.getAndIncrement());
        syncTask.setTenantId(tenantId);
        syncTask.setCreateBy(createBy);
        syncTask.setUpdateBy(createBy);
        syncTask.setCreateTime(LocalDateTime.now());
        syncTask.setUpdateTime(LocalDateTime.now());
        syncTask.setDeleted(false);
        if (syncTask.getStatus() == null) {
            syncTask.setStatus("stopped");
        }
        syncTaskList.add(syncTask);
        return syncTask;
    }

    @Override
    public SyncTask update(Long id, SyncTask syncTask, Long tenantId, String updateBy) {
        SyncTask existing = syncTaskList.stream()
                .filter(st -> id.equals(st.getId()) && !st.getDeleted())
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return null;
        }
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
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        SyncTask existing = syncTaskList.stream()
                .filter(st -> id.equals(st.getId()) && !st.getDeleted())
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return false;
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean execute(Long id) {
        SyncTask task = syncTaskList.stream()
                .filter(st -> id.equals(st.getId()) && !st.getDeleted())
                .findFirst()
                .orElse(null);
        if (task == null) {
            return false;
        }
        task.setLastSyncTime(LocalDateTime.now());
        task.setStatus("running");
        task.setUpdateTime(LocalDateTime.now());
        return true;
    }
}
