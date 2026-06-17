package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.mapper.BackupRecordMapper;
import cn.aiedge.datasource.model.BackupRecord;
import cn.aiedge.datasource.service.BackupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份服务实现
 */
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private final BackupRecordMapper backupRecordMapper;

    @Override
    public List<BackupRecord> list(Long dataSourceId, Long tenantId) {
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<BackupRecord>()
                .eq(dataSourceId != null, BackupRecord::getDataSourceId, dataSourceId)
                .eq(tenantId != null, BackupRecord::getTenantId, tenantId)
                .orderByDesc(BackupRecord::getCreateTime);
        return backupRecordMapper.selectList(wrapper);
    }

    @Override
    public BackupRecord create(Long dataSourceId, String backupName, String backupType, Long tenantId, String createBy) {
        BackupRecord record = new BackupRecord();
        record.setDataSourceId(dataSourceId);
        record.setBackupName(backupName);
        record.setBackupType(backupType);
        record.setStatus("running");
        record.setStartTime(LocalDateTime.now());
        record.setTenantId(tenantId);
        record.setCreateTime(LocalDateTime.now());
        record.setCreateBy(createBy);
        backupRecordMapper.insert(record);
        return record;
    }

    @Override
    public boolean restore(Long id) {
        BackupRecord record = backupRecordMapper.selectById(id);
        if (record == null) return false;
        return "success".equals(record.getStatus());
    }

    @Override
    public boolean delete(Long id) {
        return backupRecordMapper.deleteById(id) > 0;
    }
}
