package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.SyncTask;
import java.util.List;

/**
 * 同步任务服务接口
 */
public interface SyncTaskService {

    List<SyncTask> list(Long tenantId);

    SyncTask create(SyncTask syncTask, Long tenantId, String createBy);

    SyncTask update(Long id, SyncTask syncTask, Long tenantId, String updateBy);

    boolean delete(Long id);

    boolean execute(Long id);
}
