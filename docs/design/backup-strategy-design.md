# ERP系统数据备份策略设计

## 1. 概述

### 1.1 设计目标
基于备份需求分析，设计全面、可靠、高效的备份策略，确保ERP系统数据的安全性和可恢复性。

### 1.2 设计原则
1. **3-2-1原则**：3份备份，2种介质，1份异地
2. **分层备份**：按数据重要性设计不同的备份策略
3. **自动化执行**：减少人工干预，提高可靠性
4. **可验证性**：备份后自动验证数据完整性
5. **合规性**：满足法律法规和行业标准要求

## 2. 备份策略框架

### 2.1 整体备份架构
```
┌─────────────────────────────────────────────────────────┐
│                    ERP数据备份架构                        │
├─────────────────────────────────────────────────────────┤
│ 层级       │ 本地存储           │ 云存储            │ 异地存储  │
├────────────┼───────────────────┼───────────────────┼─────────┤
│ 实时备份   │ 增量备份(15分钟)   │ 实时同步          │ -        │
│ 日常备份   │ 差异备份(每日)     │ 差异备份(每日)    │ -        │
│ 周期备份   │ 全量备份(每周)     │ 全量备份(每周)    │ 全量(月) │
│ 长期归档   │ 月度归档           │ 年度归档          │ 年度归档 │
└────────────┴───────────────────┴───────────────────┴─────────┘
```

### 2.2 备份周期定义
| 备份类型 | 执行频率 | 执行时间 | 保留期限 | 数据范围 |
|----------|----------|----------|----------|----------|
| 增量备份 | 每15分钟 | 全天 | 30天 | P0+P1数据 |
| 差异备份 | 每日 | 02:00 | 90天 | 全部数据 |
| 全量备份 | 每周 | 周日 01:00 | 1年 | 全部数据 |
| 月度归档 | 每月 | 月初 01:00 | 5年 | 核心数据 |
| 年度归档 | 每年 | 年初 01:00 | 永久 | 审计数据 |

## 3. 详细备份策略

### 3.1 数据库备份策略

#### 3.1.1 PostgreSQL数据库备份
```yaml
database_backup:
  # 逻辑备份（pg_dump）
  logical_backup:
    frequency: "daily"
    time: "02:00"
    format: "custom"
    compression: "gzip"
    retention: "30 days"
    
  # 物理备份（pg_basebackup）
  physical_backup:
    frequency: "weekly"
    time: "Sunday 01:00"
    retention: "90 days"
    
  # WAL归档
  wal_archiving:
    enabled: true
    archive_command: "cp %p /backup/wal/%f"
    archive_timeout: "5min"
    retention: "15 days"
    
  # 流复制
  streaming_replication:
    enabled: true
    standby_count: 2
    sync_standby: 1
```

#### 3.1.2 备份文件命名规范
```
格式: {数据库}_{备份类型}_{时间戳}_{序号}.{扩展名}

示例:
- erp_prod_full_20260501_010000_001.tar.gz
- erp_prod_incr_20260501_021500_002.dump
- erp_prod_diff_20260501_020000_003.sql.gz
```

### 3.2 文件系统备份策略

#### 3.2.1 应用文件备份
```yaml
file_backup:
  # 配置文件
  config_files:
    source: "/opt/erp/config/"
    backup_type: "rsync"
    frequency: "hourly"
    retention: "90 days"
    
  # 上传文件
  upload_files:
    source: "/opt/erp/uploads/"
    backup_type: "tar + rsync"
    frequency: "daily"
    retention: "180 days"
    
  # 日志文件
  log_files:
    source: "/var/log/erp/"
    backup_type: "logrotate + rsync"
    frequency: "daily"
    retention: "30 days"
    compression: "gzip"
```

#### 3.2.2 备份目录结构
```
/backup/
├── database/
│   ├── full/           # 全量备份
│   ├── incremental/    # 增量备份
│   ├── differential/   # 差异备份
│   └── wal/           # WAL日志
├── files/
│   ├── config/        # 配置文件
│   ├── uploads/       # 上传文件
│   └── logs/         # 日志文件
├── metadata/          # 备份元数据
└── scripts/          # 备份脚本
```

### 3.3 云存储备份策略

#### 3.3.1 云存储配置
```yaml
cloud_storage:
  # 主云存储（阿里云OSS）
  primary:
    provider: "alicloud"
    bucket: "erp-backup-primary"
    region: "cn-hangzhou"
    storage_class: "Standard"
    lifecycle:
      - transition_to_ia: "30 days"
      - transition_to_archive: "90 days"
      - expiration: "365 days"
      
  # 备份云存储（腾讯云COS）
  secondary:
    provider: "tencent"
    bucket: "erp-backup-secondary"
    region: "ap-shanghai"
    storage_class: "Standard"
    encryption: "SSE-KMS"
```

#### 3.3.2 云备份同步策略
| 备份类型 | 同步时机 | 同步方式 | 网络优化 |
|----------|----------|----------|----------|
| 增量备份 | 实时 | 直接上传 | 压缩+分块 |
| 差异备份 | 备份后1小时内 | 异步上传 | 带宽限制 |
| 全量备份 | 备份后6小时内 | 分段上传 | 断点续传 |
| 归档备份 | 备份后24小时内 | 批量上传 | 夜间传输 |

## 4. 存储与保留策略

### 4.1 多级存储策略
| 存储层级 | 介质类型 | 访问速度 | 成本 | 适用数据 |
|----------|----------|----------|------|----------|
| L1（热存储） | SSD/NVMe | 毫秒级 | 高 | 最近30天备份 |
| L2（温存储） | HDD/云标准 | 秒级 | 中 | 31-90天备份 |
| L3（冷存储） | 磁带/云归档 | 分钟级 | 低 | 91天-1年备份 |
| L4（归档存储） | 光盘/云深度归档 | 小时级 | 极低 | 1年以上备份 |

### 4.2 数据保留策略
| 数据类别 | 在线保留 | 近线保留 | 离线保留 | 合规保留 |
|----------|----------|----------|----------|----------|
| 交易数据 | 30天 | 90天 | 1年 | 7年（财务） |
| 用户数据 | 90天 | 1年 | 3年 | 永久（审计） |
| 配置数据 | 永久 | - | - | 永久 |
| 日志数据 | 30天 | 90天 | 180天 | 6个月（法律） |
| 审计数据 | 1年 | 3年 | 7年 | 永久 |

## 5. 加密与安全策略

### 5.1 加密方案
```yaml
encryption:
  # 传输加密
  transport:
    protocol: "TLS 1.3"
    ciphers: "ECDHE-RSA-AES256-GCM-SHA384"
    
  # 静态加密
  at_rest:
    algorithm: "AES-256-GCM"
    key_management: "HSM/KMS"
    
  # 密钥管理
  key_management:
    rotation: "90 days"
    backup: "异地HSM"
    access_control: "RBAC"
```

### 5.2 访问控制策略
| 角色 | 权限 | 访问控制 | 审计要求 |
|------|------|----------|----------|
| 备份管理员 | 完全访问 | MFA+IP白名单 | 详细日志 |
| 系统管理员 | 读/执行 | MFA | 操作日志 |
| 监控人员 | 只读 | IP限制 | 查询日志 |
| 审计人员 | 只读（归档） | 时间限制 | 访问日志 |

## 6. 备份窗口优化

### 6.1 时间窗口分配
| 时间段 | 备份类型 | 资源占用 | 业务影响 |
|--------|----------|----------|----------|
| 00:00-02:00 | 差异备份 | 中（50%） | 低 |
| 02:00-04:00 | 文件备份 | 低（30%） | 无 |
| 04:00-06:00 | 云同步 | 低（20%） | 无 |
| 全天 | 增量备份 | 低（15%） | 无 |
| 周日01:00-06:00 | 全量备份 | 高（80%） | 计划内维护 |

### 6.2 资源调配策略
```yaml
resource_allocation:
  cpu:
    backup_limit: "60%"
    normal_priority: "20%"
    
  memory:
    buffer_cache: "4GB"
    working_memory: "2GB"
    
  disk_io:
    read_limit: "100MB/s"
    write_limit: "50MB/s"
    
  network:
    bandwidth_limit: "80%"
    qos_priority: "medium"
```

## 7. 监控与告警策略

### 7.1 监控指标
| 指标类别 | 监控指标 | 告警阈值 | 检查频率 |
|----------|----------|----------|----------|
| 备份状态 | 备份成功率 | < 95% | 实时 |
| 备份时效 | 备份延迟 | > 30分钟 | 每5分钟 |
| 存储空间 | 使用率 | > 80% | 每小时 |
| 数据完整性 | 校验失败率 | > 0.1% | 每次备份 |
| 性能影响 | IO等待时间 | > 100ms | 实时 |

### 7.2 告警级别
| 级别 | 条件 | 通知方式 | 响应时间 |
|------|------|----------|----------|
| P0（紧急） | 备份连续失败3次 | 电话+短信+邮件 | 15分钟 |
| P1（严重） | 存储空间>90% | 短信+邮件 | 1小时 |
| P2（警告） | 备份延迟>1小时 | 邮件 | 4小时 |
| P3（提示） | 校验失败 | 邮件 | 8小时 |

## 8. 策略验证与测试

### 8.1 验证方法
| 验证类型 | 频率 | 验证内容 | 验收标准 |
|----------|------|----------|----------|
| 完整性验证 | 每次备份 | 数据一致性 | 100%一致 |
| 可恢复性验证 | 每月 | 恢复测试 | RTO/RPO达标 |
| 性能验证 | 每季度 | 备份性能 | 满足SLA |
| 安全验证 | 每半年 | 安全审计 | 无高危漏洞 |

### 8.2 测试计划
```yaml
testing_schedule:
  monthly_test:
    type: "partial_restore"
    scope: "critical_tables"
    schedule: "第一个周末"
    
  quarterly_test:
    type: "full_restore"
    scope: "full_database"
    schedule: "季度末"
    
  annual_test:
    type: "disaster_recovery"
    scope: "complete_system"
    schedule: "年度演练"
```

## 9. 策略维护与优化

### 9.1 维护计划
| 维护项目 | 频率 | 负责人 | 产出物 |
|----------|------|--------|--------|
| 策略评审 | 每季度 | 备份架构师 | 评审报告 |
| 性能优化 | 每半年 | 系统工程师 | 优化方案 |
| 合规检查 | 每年 | 合规专员 | 合规报告 |
| 技术升级 | 按需 | 技术专家 | 升级方案 |

### 9.2 持续改进
1. **指标收集**：收集备份成功率、恢复时间等指标
2. **问题分析**：分析备份失败原因，优化策略
3. **技术跟进**：关注新技术，评估引入价值
4. **需求响应**：根据业务变化调整备份策略

## 10. 附录

### 10.1 备份策略检查清单
- [ ] 是否满足3-2-1原则
- [ ] 是否覆盖所有重要数据
- [ ] 是否满足RTO/RPO要求
- [ ] 是否包含加密和安全控制
- [ ] 是否有监控和告警机制
- [ ] 是否有定期测试计划
- [ ] 是否有文档和培训材料

### 10.2 相关配置文件示例
```bash
# 备份策略配置文件示例
BACKUP_STRATEGY_CONFIG="/etc/erp/backup/strategy.conf"

# 数据库备份配置
DB_FULL_BACKUP_DAY="Sunday"
DB_FULL_BACKUP_TIME="01:00"
DB_INCREMENTAL_INTERVAL="15"
DB_RETENTION_DAYS="30"

# 文件备份配置
FILE_BACKUP_SOURCES="/opt/erp/config /opt/erp/uploads"
FILE_BACKUP_TIME="02:00"
FILE_RETENTION_DAYS="90"

# 云存储配置
CLOUD_ENABLED="true"
CLOUD_PROVIDER="alicloud"
CLOUD_BUCKET="erp-backup"
CLOUD_REGION="cn-hangzhou"
```

### 10.3 策略实施路线图
| 阶段 | 时间 | 主要任务 | 完成标准 |
|------|------|----------|----------|
| 一期 | 第1个月 | 基础备份实施 | P0数据备份就绪 |
| 二期 | 第2个月 | 云备份集成 | 混合备份运行 |
| 三期 | 第3个月 | 监控告警完善 | 自动化监控上线 |
| 四期 | 第6个月 | 容灾备份建设 | 异地备份就绪 |
| 五期 | 第12个月 | 全面优化 | 全策略稳定运行 |

---

**文档版本**: v1.0  
**创建日期**: 2026-05-01  
**更新日期**: 2026-05-01  
**负责人**: qa-lead  
**审核人**: [待审核]  
**批准人**: [待批准]  

**相关文档**:
- [备份需求分析文档](./backup-requirements-analysis.md)
- [备份技术实现方案](./backup-implementation-plan.md)
- [恢复策略设计文档](./recovery-strategy-design.md)
- [自动化监控设计](./automation-monitoring-design.md)