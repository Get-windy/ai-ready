package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.BackupRecord;
import java.util.List;

/**
 * 备份服务接口
 */
public interface BackupService {

    List<BackupRecord> list(Long dataSourceId, Long tenantId);

    BackupRecord create(Long dataSourceId, String backupName, String backupType, Long tenantId, String createBy);

    boolean restore(Long id);

    boolean delete(Long id);
}
