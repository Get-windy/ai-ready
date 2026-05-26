# 测试环境存储配置文档

## 1. 存储架构概述

### 1.1 存储层次结构
```
┌─────────────────────────────────────────────────────────┐
│                   测试环境存储架构                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │  性能层     │    │  容量层     │    │  备份层     │ │
│  │ (SSD/NVMe)  │    │ (HDD)       │    │ (对象存储)  │ │
│  │             │    │             │    │             │ │
│  │ • 数据库    │    │ • 日志存储  │    │ • 每日备份  │ │
│  │ • 缓存      │    │ • 文件存储  │    │ • 归档数据  │ │
│  │ • 索引      │    │ • 镜像仓库  │    │ • 冷数据    │ │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘ │
│         │                  │                  │        │
│  ┌──────┴──────────────────┴──────────────────┴──────┐ │
│  │                存储管理平台                        │ │
│  │ • NFS服务器                                      │ │
│  │ • SMB共享                                        │ │
│  │ • 存储监控                                       │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 1.2 存储容量规划
| 存储类型 | 总容量 | 可用容量 | RAID级别 | 用途 |
|----------|--------|----------|----------|------|
| SSD性能层 | 1TB | 800GB | RAID 10 | 数据库、缓存、索引 |
| HDD容量层 | 4TB | 3.2TB | RAID 5 | 日志、文件、镜像 |
| 备份存储 | 2TB | 1.6TB | 无 | 备份、归档 |

## 2. 共享存储配置

### 2.1 NFS服务器配置
```bash
# I:\AI-Ready\infra\storage\nfs-server-setup.sh
#!/bin/bash

# 安装NFS服务器
apt-get update
apt-get install -y nfs-kernel-server

# 创建共享目录
mkdir -p /srv/nfs/test-share
chown nobody:nogroup /srv/nfs/test-share
chmod 777 /srv/nfs/test-share

# 配置NFS导出
cat > /etc/exports << EOF
/srv/nfs/test-share 10.0.0.0/16(rw,sync,no_subtree_check,no_root_squash)
EOF

# 应用配置
exportfs -a
systemctl restart nfs-kernel-server
```

### 2.2 SMB共享配置
```ini
# I:\AI-Ready\infra\storage\smb.conf
[global]
   workgroup = TESTWORKGROUP
   server string = AI-Ready Test Samba Server
   security = user
   map to guest = bad user

[test-share]
   comment = AI-Ready Test Share
   path = /srv/smb/test-share
   browseable = yes
   read only = no
   guest ok = yes
   create mask = 0775
   directory mask = 0775
```

## 3. 数据库存储卷配置

### 3.1 PostgreSQL存储配置
```yaml
# I:\AI-Ready\infra\storage\postgres-storage.yml
version: '3.8'

services:
  postgres:
    image: postgres:15
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - postgres_backup:/backup
    environment:
      POSTGRES_DB: ai_ready_test
      POSTGRES_USER: test_user
      POSTGRES_PASSWORD: test_password123
    networks:
      - data-net

volumes:
  postgres_data:
    driver: local
    driver_opts:
      type: none
      device: /mnt/ssd/postgres
      o: bind
  postgres_backup:
    driver: local
    driver_opts:
      type: none
      device: /mnt/hdd/postgres-backup
      o: bind
```

### 3.2 Redis存储配置
```yaml
# I:\AI-Ready\infra\storage\redis-storage.yml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - data-net

volumes:
  redis_data:
    driver: local
    driver_opts:
      type: none
      device: /mnt/ssd/redis
      o: bind
```

## 4. 日志存储卷配置

### 4.1 集中式日志存储
```yaml
# I:\AI-Ready\infra\storage\log-storage.yml
version: '3.8'

services:
  elasticsearch:
    image: elasticsearch:8.11.0
    volumes:
      - es_data:/usr/share/elasticsearch/data
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    networks:
      - data-net

  kibana:
    image: kibana:8.11.0
    volumes:
      - kibana_data:/usr/share/kibana/data
    ports:
      - "5601:5601"
    networks:
      - data-net

volumes:
  es_data:
    driver: local
    driver_opts:
      type: none
      device: /mnt/hdd/elasticsearch
      o: bind
  kibana_data:
    driver: local
    driver_opts:
      type: none
      device: /mnt/hdd/kibana
      o: bind
```

### 4.2 日志轮转策略
```bash
# I:\AI-Ready\infra\storage\log-rotation.sh
#!/bin/bash

# 日志保留策略
# - 应用日志: 保留30天
# - 访问日志: 保留90天
# - 错误日志: 保留180天
# - 审计日志: 保留365天

cat > /etc/logrotate.d/ai-ready-test << EOF
/var/log/ai-ready/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 www-data adm
    sharedscripts
    postrotate
        systemctl reload nginx
    endscript
}
EOF
```

## 5. 备份存储卷配置

### 5.1 自动化备份策略
```bash
# I:\AI-Ready\infra\storage\backup-script.sh
#!/bin/bash

BACKUP_DIR="/mnt/backup"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# 数据库备份
pg_dump -U test_user -h 10.0.3.10 ai_ready_test > $BACKUP_DIR/db_backup_$DATE.sql
gzip $BACKUP_DIR/db_backup_$DATE.sql

# Redis备份
redis-cli -h 10.0.3.20 SAVE
cp /mnt/ssd/redis/dump.rdb $BACKUP_DIR/redis_backup_$DATE.rdb
gzip $BACKUP_DIR/redis_backup_$DATE.rdb

# 配置文件备份
tar czf $BACKUP_DIR/config_backup_$DATE.tar.gz /etc/ai-ready/

# 清理旧备份
find $BACKUP_DIR -type f -name "*.gz" -mtime +$RETENTION_DAYS -delete
```

### 5.2 备份监控
```yaml
# I:\AI-Ready\infra\storage\backup-monitoring.yml
version: '3.8'

services:
  backup-monitor:
    image: alpine:latest
    volumes:
      - backup_data:/backup
      - ./backup-check.sh:/backup-check.sh
    command: sh -c "while true; do /backup-check.sh; sleep 3600; done"
    networks:
      - mgmt-net

volumes:
  backup_data:
    driver: local
    driver_opts:
      type: none
      device: /mnt/backup
      o: bind
```

## 6. 存储性能测试

### 6.1 IO性能测试脚本
```bash
# I:\AI-Ready\infra\storage\io-performance-test.sh
#!/bin/bash

echo "=== 存储IO性能测试 ==="

# 测试目录
TEST_DIR="/mnt/ssd/test"
mkdir -p $TEST_DIR

# 顺序写入测试
echo "1. 顺序写入测试 (1GB文件)..."
dd if=/dev/zero of=$TEST_DIR/seq_write bs=1M count=1024 oflag=direct 2>&1 | tail -1

# 顺序读取测试
echo "2. 顺序读取测试..."
dd if=$TEST_DIR/seq_write of=/dev/null bs=1M count=1024 iflag=direct 2>&1 | tail -1

# 随机写入测试
echo "3. 随机写入测试..."
fio --name=randwrite --ioengine=libaio --rw=randwrite --bs=4k --size=1G --numjobs=4 --runtime=60 --time_based --group_reporting

# 随机读取测试
echo "4. 随机读取测试..."
fio --name=randread --ioengine=libaio --rw=randread --bs=4k --size=1G --numjobs=4 --runtime=60 --time_based --group_reporting

# 清理
rm -f $TEST_DIR/seq_write
echo "=== 测试完成 ==="
```

### 6.2 性能验收标准
| 测试项目 | 预期性能 | 实际性能 | 是否达标 |
|----------|----------|----------|----------|
| 顺序写入 | ≥ 500 MB/s | - | - |
| 顺序读取 | ≥ 600 MB/s | - | - |
| 随机写入 | ≥ 50,000 IOPS | - | - |
| 随机读取 | ≥ 80,000 IOPS | - | - |
| 延迟 | ≤ 5ms | - | - |

## 7. 存储监控配置

### 7.1 Prometheus存储监控
```yaml
# I:\AI-Ready\infra\storage\storage-monitoring.yml
scrape_configs:
  - job_name: 'storage'
    static_configs:
      - targets: ['10.0.3.100:9100']  # node_exporter
    
  - job_name: 'nfs'
    static_configs:
      - targets: ['10.0.3.101:9117']  # nfs_exporter
    
  - job_name: 'samba'
    static_configs:
      - targets: ['10.0.3.102:9911']  # samba_exporter
```

### 7.2 Grafana存储仪表盘
```json
# I:\AI-Ready\infra\storage\storage-dashboard.json
{
  "dashboard": {
    "title": "测试环境存储监控",
    "panels": [
      {
        "title": "磁盘使用率",
        "type": "stat",
        "targets": [
          {
            "expr": "100 - (node_filesystem_avail_bytes{mountpoint=\"/mnt/ssd\"} / node_filesystem_size_bytes{mountpoint=\"/mnt/ssd\"} * 100)"
          }
        ]
      }
    ]
  }
}
```

## 8. 部署说明

### 8.1 部署步骤
1. 准备存储硬件并分区
2. 配置NFS/SMB共享
3. 部署数据库存储卷
4. 配置日志存储
5. 设置备份策略
6. 部署存储监控
7. 运行性能测试

### 8.2 验证步骤
1. 验证共享存储可访问
2. 验证数据库存储可读写
3. 验证日志存储正常
4. 验证备份策略执行
5. 验证监控数据采集

---

**文档版本**: 1.0  
**创建时间**: 2026-04-27  
**最后更新**: 2026-04-27  
**负责人**: team-member