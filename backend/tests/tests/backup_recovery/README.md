# 企智连数据备份恢复测试套件

## 项目概述
本测试套件用于验证企智连系统的数据备份恢复功能，包括数据库备份、数据恢复、完整性验证、RTO/RPO测试。

## 技术栈
- **语言**: Java 11+
- **测试框架**: JUnit 5 (5.10.2)
- **构建工具**: Maven
- **数据库**: MySQL 8.0 / PostgreSQL 42.7.1

## 目录结构
```
backup_recovery/
├── pom.xml                         # Maven项目配置
├── README.md                       # 项目说明
├── BACKUP_RECOVERY_TEST_CASES.md   # 测试用例文档
├── backup_recovery_results.json    # 测试结果（Python）
├── test_backup_recovery.py         # Python测试脚本
└── src/
    └── test/
        └── java/
            └── com/
                └── qizhilian/
                    └── backup/
                        ├── BackupRecoveryTestSuite.java    # 测试套件
                        ├── DatabaseBackupTest.java         # 数据库备份测试
                        ├── DataRestoreTest.java            # 数据恢复测试
                        ├── DataIntegrityTest.java          # 数据完整性验证
                        ├── RTOTest.java                    # RTO测试
                        ├── RPOTest.java                    # RPO测试
                        ├── tool/
                        │   └── MockBackupTool.java         # Mock工具类
                        └── model/
                            ├── BackupResult.java           # 备份结果模型
                            ├── RestoreResult.java          # 恢复结果模型
                            └── IntegrityResult.java        # 完整性结果模型
```

## 测试用例汇总

| 类别 | 用例数 | Java类 | 覆盖范围 |
|------|--------|--------|----------|
| 数据库备份测试 | 5 | DatabaseBackupTest | TC-BR-001 至 TC-BR-005 |
| 数据恢复测试 | 3 | DataRestoreTest | TC-BR-006 至 TC-BR-008 |
| 数据完整性验证 | 4 | DataIntegrityTest | TC-BR-009 至 TC-BR-012 |
| RTO测试 | 3 | RTOTest | TC-BR-013 至 TC-BR-015 |
| RPO测试 | 4 | RPOTest | TC-BR-016 至 TC-BR-019 |
| **总计** | **19** | - | - |

## 运行测试

### Java测试 (Maven)

```bash
# 进入项目目录
cd I:\AI-Ready\tests\backup_recovery

# 运行所有测试
mvn test

# 运行特定类别测试
mvn test -P backup-tests     # 数据库备份测试
mvn test -P restore-tests    # 数据恢复测试
mvn test -P integrity-tests  # 数据完整性测试
mvn test -P rto-tests        # RTO测试
mvn test -P rpo-tests        # RPO测试

# 运行单个测试类
mvn test -Dtest=DatabaseBackupTest
mvn test -Dtest=RTOTest

# 生成测试报告
mvn test surefire-report:report
```

### Python测试 (pytest)

```bash
# 进入项目目录
cd I:\AI-Ready\tests\backup_recovery

# 运行所有Python测试
python -m pytest test_backup_recovery.py -v

# 运行特定类别测试
python -m pytest test_backup_recovery.py -v -m backup
python -m pytest test_backup_recovery.py -v -m restore
python -m pytest test_backup_recovery.py -v -m integrity
python -m pytest test_backup_recovery.py -v -m rto
python -m pytest test_backup_recovery.py -v -m rpo
```

## 测试详情

### 一、数据库备份测试 (DatabaseBackupTest)

| ID | 测试名称 | 目标 |
|----|----------|------|
| TC-BR-001 | MySQL全量备份测试 | 验证MySQL数据库全量备份功能 |
| TC-BR-002 | PostgreSQL全量备份测试 | 验证PostgreSQL数据库全量备份功能 |
| TC-BR-003 | 增量备份测试 | 验证数据库增量备份功能 |
| TC-BR-004 | 备份压缩测试 | 验证备份文件压缩功能 |
| TC-BR-005 | 定时备份测试 | 验证定时备份任务配置 |

### 二、数据恢复测试 (DataRestoreTest)

| ID | 测试名称 | 目标 |
|----|----------|------|
| TC-BR-006 | 全量恢复测试 | 验证从全量备份恢复数据 |
| TC-BR-007 | 增量恢复测试 | 验证从增量备份恢复数据 |
| TC-BR-008 | 时间点恢复测试 | 验证按时间点恢复数据 |

### 三、数据完整性验证 (DataIntegrityTest)

| ID | 测试名称 | 目标 |
|----|----------|------|
| TC-BR-009 | 备份文件校验和验证 | 验证备份文件完整性 |
| TC-BR-010 | 数据行数验证 | 验证恢复后数据行数正确 |
| TC-BR-011 | 表结构完整性验证 | 验证恢复后表结构完整 |
| TC-BR-012 | 外键约束完整性验证 | 验证恢复后外键约束完整 |

### 四、RTO测试 (RTOTest)

| ID | 测试名称 | RTO目标 |
|----|----------|---------|
| TC-BR-013 | 全量恢复RTO测试 | ≤ 3600秒 (1小时) |
| TC-BR-014 | 增量恢复RTO测试 | ≤ 600秒 (10分钟) |
| TC-BR-015 | 灾难恢复RTO测试 | ≤ 7200秒 (2小时) |

### 五、RPO测试 (RPOTest)

| ID | 测试名称 | RPO目标 |
|----|----------|---------|
| TC-BR-016 | 全量备份RPO测试 | ≤ 24小时 |
| TC-BR-017 | 增量备份RPO测试 | ≤ 1小时 |
| TC-BR-018 | 实时同步RPO测试 | ≤ 60秒 |
| TC-BR-019 | 灾难场景RPO测试 | ≤ 1小时 |

## 验收标准
- ✅ 完成数据备份恢复测试用例
- ✅ 覆盖全量备份和增量备份
- ✅ 包含恢复流程测试
- ✅ 提供自动化测试脚本
- ✅ 输出测试报告

## 输出文件
- `target/surefire-reports/` - Maven测试报告目录
- `backup_recovery_results.json` - Python测试结果JSON

## 环境要求
- JDK 11+
- Maven 3.8+
- Python 3.10+ (可选)
- pytest 9.0+ (可选)

## 作者
- 测试执行: test-agent-2
- 创建日期: 2026-04-15
- 版本: 1.0