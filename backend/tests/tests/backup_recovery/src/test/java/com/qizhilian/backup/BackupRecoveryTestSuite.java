package com.qizhilian.backup;

import com.qizhilian.backup.DatabaseBackupTest;
import com.qizhilian.backup.DataRestoreTest;
import com.qizhilian.backup.DataIntegrityTest;
import com.qizhilian.backup.RTOTest;
import com.qizhilian.backup.RPOTest;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * 企智连数据备份恢复测试套件
 * 聚合所有测试类
 * 
 * 测试类别:
 * - 数据库备份测试 (TC-BR-001 至 TC-BR-005): DatabaseBackupTest
 * - 数据恢复测试 (TC-BR-006 至 TC-BR-008): DataRestoreTest
 * - 数据完整性验证 (TC-BR-009 至 TC-BR-012): DataIntegrityTest
 * - RTO测试 (TC-BR-013 至 TC-BR-015): RTOTest
 * - RPO测试 (TC-BR-016 至 TC-BR-019): RPOTest
 * 
 * 运行方式:
 * mvn test -Dtest=BackupRecoveryTestSuite
 */
@RunWith(JUnitPlatform.class)
@org.junit.jupiter.api.DisplayName("企智连数据备份恢复完整测试套件")
public class BackupRecoveryTestSuite {
    // 聚合测试类
    // DatabaseBackupTest - 5个备份测试
    // DataRestoreTest - 3个恢复测试
    // DataIntegrityTest - 4个完整性验证测试
    // RTOTest - 3个RTO测试
    // RPOTest - 4个RPO测试
    // 总计: 19个测试用例
}