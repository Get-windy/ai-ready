# AI-Ready 灾备方案设计文档

**项目**: 智企连·AI-Ready  
**版本**: v1.0  
**日期**: 2026-04-03  
**负责人**: devops-engineer

---

## 一、业务连续性需求

### 1.1 RTO/RPO定义

| 业务场景 | RTO (恢复时间目标) | RPO (数据丢失容忍) | 优先级 |
|----------|-------------------|-------------------|--------|
| 核心API服务 | 15分钟 | 5分钟 | P0 |
| 数据库服务 | 30分钟 | 0 (无丢失) | P0 |
| AI引擎服务 | 1小时 | 15分钟 | P1 |
| 文件存储 | 2小时 | 1小时 | P2 |
| 日志/监控 | 4小时 | 24小时 | P3 |

### 1.2 可用性目标

| 指标 | 目标值 | 年度允许停机 |
|------|--------|-------------|
| SLA | 99.9% | 8.76小时 |
| 核心服务SLA | 99.95% | 4.38小时 |

---

## 二、灾备架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      灾备架构 (两地三中心)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  主数据中心 (北京)                灾备中心 (上海)                │
│  ┌─────────────┐                 ┌─────────────┐               │
│  │ K8s Cluster │─────同步──────▶│ K8s Cluster │               │
│  │ 3 Master    │     (etcd)      │ 3 Master    │               │
│  │ 5 Worker    │                 │ 5 Worker    │               │
│  └──────┬──────┘                 └──────┬──────┘               │
│         │                               │                       │
│         ▼                               ▼                       │
│  ┌─────────────┐                 ┌─────────────┐               │
│  │ PostgreSQL  │─────流复制────▶│ PostgreSQL  │               │
│  │ Primary     │    (同步)       │ Standby     │               │
│  └──────┬──────┘                 └──────┬──────┘               │
│         │                               │                       │
│         ▼                               ▼                       │
│  ┌─────────────┐                 ┌─────────────┐               │
│  │   Redis     │─────复制──────▶│   Redis     │               │
│  │  Cluster    │                 │  Cluster    │               │
│  └─────────────┘                 └─────────────┘               │
│                                                                 │
│                      云存储 (异地归档)                           │
│                  ┌─────────────────────┐                       │
│                  │ OSS / S3 Glacier    │                       │
│                  └─────────────────────┘                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 组件冗余

| 组件 | 主中心 | 灾备中心 | 复制方式 |
|------|--------|----------|----------|
| K8s集群 | 3M+5W | 3M+5W | etcd同步 |
| PostgreSQL | 1主2从 | 1从 | 流复制(同步) |
| Redis | 3主3从 | 3从 | 异步复制 |
| 对象存储 | OSS | OSS跨区域 | 跨区域复制 |

---

## 三、灾备策略

### 3.1 故障分级

| 级别 | 故障类型 | 影响范围 | 响应时间 | 恢复方式 |
|------|----------|----------|----------|----------|
| **P0** | 机房断电/火灾 | 整个数据中心 | 立即 | 切换到灾备中心 |
| **P1** | 数据库主节点宕机 | 数据库服务 | 5分钟 | 自动故障转移 |
| **P2** | K8s节点故障 | 部分Pod | 1分钟 | 自动重调度 |
| **P3** | 单服务异常 | 单个服务 | 10分钟 | 重启/回滚 |

### 3.2 数据备份策略

| 数据类型 | 备份频率 | 保留周期 | 存储位置 |
|----------|----------|----------|----------|
| PostgreSQL | 每日全量+持续WAL | 30天 | 本地+远程+云 |
| Redis | 每小时RDB | 7天 | 本地+远程 |
| 文件存储 | 每日增量 | 90天 | OSS跨区域 |
| 配置文件 | 实时Git | 永久 | Git仓库 |

### 3.3 容灾切换流程

```
故障发生 → 故障确认 → 决策切换 → 执行切换 → 验证恢复 → 通知相关方
   │          │          │          │          │          │
  1分钟      5分钟      2分钟      10分钟     5分钟      2分钟
                                                     │
                                              总计: 25分钟
```

---

## 四、灾备演练方案

### 4.1 演练计划

| 演练类型 | 频率 | 参与人员 | 持续时间 | 验证目标 |
|----------|------|----------|----------|----------|
| 桌面演练 | 每月 | 运维团队 | 2小时 | 流程确认 |
| 数据库切换演练 | 每季 | DBA+运维 | 4小时 | 自动故障转移 |
| 机房切换演练 | 每半年 | 全员 | 8小时 | 完整灾备切换 |
| 全流程演练 | 每年 | 全员+业务 | 24小时 | 端到端验证 |

### 4.2 演练检查清单

**演练前准备**：
- [ ] 通知所有相关人员
- [ ] 确认备份完整性
- [ ] 准备回滚方案
- [ ] 设置监控告警

**演练执行**：
- [ ] 记录开始时间
- [ ] 按步骤执行切换
- [ ] 验证服务可用性
- [ ] 验证数据一致性
- [ ] 记录结束时间

**演练后总结**：
- [ ] 记录实际RTO/RPO
- [ ] 分析问题与改进点
- [ ] 更新灾备文档
- [ ] 发送演练报告

### 4.3 演练报告模板

```markdown
# AI-Ready 灾备演练报告

**演练日期**: YYYY-MM-DD
**演练类型**: 数据库故障转移 / 机房切换
**参与人员**: xxx

## 演练结果
- 实际RTO: XX分钟
- 实际RPO: XX分钟
- 是否达标: 是/否

## 发现的问题
1. ...
2. ...

## 改进措施
1. ...
2. ...
```

---

## 五、故障恢复手册

### 5.1 数据库故障恢复

```bash
# 1. 确认主库状态
kubectl exec -n ai-ready postgres-0 -- pg_isready

# 2. 检查复制状态
kubectl exec -n ai-ready postgres-0 -- psql -c "SELECT * FROM pg_stat_replication;"

# 3. 手动故障转移 (如果自动失败)
kubectl exec -n ai-ready postgres-1 -- pg_ctl promote

# 4. 更新服务指向
kubectl patch service postgres -n ai-ready -p '{"spec":{"selector":{"statefulset.kubernetes.io/pod-name":"postgres-1"}}}'

# 5. 验证连接
psql -h postgres-service -U postgres -c "SELECT version();"
```

### 5.2 K8s集群故障恢复

```bash
# 1. 检查节点状态
kubectl get nodes
kubectl describe node <problem-node>

# 2. 驱逐Pod
kubectl drain <problem-node> --ignore-daemonsets --delete-emptydir-data

# 3. 恢复节点
kubectl uncordon <problem-node>

# 4. 验证Pod分布
kubectl get pods -o wide -n ai-ready
```

### 5.3 机房级故障切换

```bash
# 1. 确认主中心不可用
ping prod-cluster.ai-ready.cn
curl https://prod-cluster.ai-ready.cn/health

# 2. 激活灾备中心
# 更新DNS指向灾备中心
aws route53 change-resource-record-sets --hosted-zone-id XXX \
  --change-batch '{"Changes":[{"Action":"UPSERT","ResourceRecordSet":{"Name":"api.ai-ready.cn","Type":"CNAME","TTL":60,"ResourceRecords":[{"Value":"dr-cluster.ai-ready.cn"}]}}]}'

# 3. 提升灾备数据库
kubectl exec -n ai-ready dr-postgres-0 -- pg_ctl promote

# 4. 扩容灾备服务
kubectl scale deployment ai-ready-api --replicas=5 -n ai-ready

# 5. 验证服务
curl https://api.ai-ready.cn/actuator/health
```

---

## 六、监控与告警

### 6.1 灾备监控指标

| 指标 | 告警阈值 | 响应动作 |
|------|----------|----------|
| 数据库复制延迟 | >5秒 | 通知DBA |
| 集群节点不可用 | >2个 | 自动告警 |
| 服务可用性 | <99% | 升级处理 |
| 备份失败 | 1次 | 立即处理 |

### 6.2 告警通知

```
告警级别：
- Critical: 电话 + 钉钉 + 邮件 (5分钟内响应)
- Warning: 钉钉 + 邮件 (30分钟内响应)
- Info: 邮件 (24小时内响应)
```

---

## 七、附录

### 7.1 联系人列表

| 角色 | 姓名 | 电话 | 邮箱 |
|------|------|------|------|
| 运维负责人 | xxx | 138xxxx | ops@ai-ready.cn |
| DBA | xxx | 139xxxx | dba@ai-ready.cn |
| 业务负责人 | xxx | 137xxxx | biz@ai-ready.cn |

### 7.2 相关文档

- 备份恢复操作手册: `docs/database-backup-optimization-report.md`
- 监控配置文档: `docs/monitoring-guide.md`
- 自动扩缩容配置: `docs/autoscaling-guide.md`

---

**文档生成**: devops-engineer  
**版本**: v1.0
