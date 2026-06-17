package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.model.BackupRecord;
import cn.aiedge.datasource.service.BackupService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 备份服务实现
 */
@Service
public class BackupServiceImpl implements BackupService {

    private final List<BackupRecord> backupList = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @PostConstruct
    public void init() {
        BackupRecord br1 = new BackupRecord();
        br1.setId(idCounter.getAndIncrement());
        br1.setDataSourceId(1L);
        br1.setBackupName("本地MySQL全量备份_20260601");
        br1.setBackupType("full");
        br1.setFilePath("/data/backup/mysql/ai_ready_20260601.sql.gz");
        br1.setFileSize(524288000L);
        br1.setStatus("success");
        br1.setStartTime(LocalDateTime.now().minusDays(16));
        br1.setEndTime(LocalDateTime.now().minusDays(16).plusMinutes(35));
        br1.setTenantId(1L);
        br1.setCreateTime(LocalDateTime.now().minusDays(16));
        br1.setCreateBy("admin");
        backupList.add(br1);

        BackupRecord br2 = new BackupRecord();
        br2.setId(idCounter.getAndIncrement());
        br2.setDataSourceId(1L);
        br2.setBackupName("本地MySQL增量备份_20260602");
        br2.setBackupType("incremental");
        br2.setFilePath("/data/backup/mysql/ai_ready_inc_20260602.sql.gz");
        br2.setFileSize(25600000L);
        br2.setStatus("success");
        br2.setStartTime(LocalDateTime.now().minusDays(15));
        br2.setEndTime(LocalDateTime.now().minusDays(15).plusMinutes(8));
        br2.setTenantId(1L);
        br2.setCreateTime(LocalDateTime.now().minusDays(15));
        br2.setCreateBy("admin");
        backupList.add(br2);

        BackupRecord br3 = new BackupRecord();
        br3.setId(idCounter.getAndIncrement());
        br3.setDataSourceId(2L);
        br3.setBackupName("测试PostgreSQL全量备份_20260605");
        br3.setBackupType("full");
        br3.setFilePath("/data/backup/postgres/test_db_20260605.dump");
        br3.setFileSize(1073741824L);
        br3.setStatus("success");
        br3.setStartTime(LocalDateTime.now().minusDays(12));
        br3.setEndTime(LocalDateTime.now().minusDays(12).plusMinutes(52));
        br3.setTenantId(1L);
        br3.setCreateTime(LocalDateTime.now().minusDays(12));
        br3.setCreateBy("admin");
        backupList.add(br3);

        BackupRecord br4 = new BackupRecord();
        br4.setId(idCounter.getAndIncrement());
        br4.setDataSourceId(1L);
        br4.setBackupName("本地MySQL全量备份_20260610");
        br4.setBackupType("full");
        br4.setFilePath("/data/backup/mysql/ai_ready_20260610.sql.gz");
        br4.setFileSize(0L);
        br4.setStatus("failed");
        br4.setStartTime(LocalDateTime.now().minusDays(7));
        br4.setEndTime(LocalDateTime.now().minusDays(7).plusMinutes(3));
        br4.setErrorMessage("磁盘空间不足，备份失败");
        br4.setTenantId(1L);
        br4.setCreateTime(LocalDateTime.now().minusDays(7));
        br4.setCreateBy("admin");
        backupList.add(br4);

        BackupRecord br5 = new BackupRecord();
        br5.setId(idCounter.getAndIncrement());
        br5.setDataSourceId(1L);
        br5.setBackupName("本地MySQL全量备份_20260615");
        br5.setBackupType("full");
        br5.setFilePath("/data/backup/mysql/ai_ready_20260615.sql.gz");
        br5.setFileSize(0L);
        br5.setStatus("running");
        br5.setStartTime(LocalDateTime.now().minusMinutes(10));
        br5.setTenantId(1L);
        br5.setCreateTime(LocalDateTime.now().minusMinutes(10));
        br5.setCreateBy("admin");
        backupList.add(br5);
    }

    @Override
    public List<BackupRecord> list(Long dataSourceId, Long tenantId) {
        return backupList.stream()
                .filter(br -> dataSourceId == null || dataSourceId.equals(br.getDataSourceId()))
                .filter(br -> tenantId == null || tenantId.equals(br.getTenantId()))
                .collect(Collectors.toList());
    }

    @Override
    public BackupRecord create(Long dataSourceId, String backupName, String backupType, Long tenantId, String createBy) {
        BackupRecord record = new BackupRecord();
        record.setId(idCounter.getAndIncrement());
        record.setDataSourceId(dataSourceId);
        record.setBackupName(backupName);
        record.setBackupType(backupType);
        record.setStatus("running");
        record.setStartTime(LocalDateTime.now());
        record.setTenantId(tenantId);
        record.setCreateTime(LocalDateTime.now());
        record.setCreateBy(createBy);
        backupList.add(record);
        return record;
    }

    @Override
    public boolean restore(Long id) {
        BackupRecord record = backupList.stream()
                .filter(br -> id.equals(br.getId()))
                .findFirst()
                .orElse(null);
        if (record == null) {
            return false;
        }
        // Simulate restore validation
        return "success".equals(record.getStatus());
    }

    @Override
    public boolean delete(Long id) {
        return backupList.removeIf(br -> id.equals(br.getId()));
    }
}
