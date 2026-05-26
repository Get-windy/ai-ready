# 测试环境故障排查手册

## 手册概述

### 目的
本手册提供测试环境常见问题的诊断和解决方法，帮助团队成员快速定位和修复环境问题。

### 使用说明
1. **按症状查找**：根据问题现象查找对应章节
2. **按步骤操作**：按照诊断步骤逐一排查
3. **记录结果**：记录排查过程和解决方案
4. **反馈改进**：将新问题和解决方案反馈给团队

### 紧急联系方式
- **环境运维组**：env-ops@company.com
- **紧急响应**：+86-138-XXXX-XXXX
- **值班表**：见内部Wiki页面

## 第一章：环境访问问题

### 1.1 无法访问测试环境
**症状：** 无法通过浏览器或API访问测试环境

**诊断步骤：**
```bash
# 1. 检查网络连通性
ping ${ENVIRONMENT_HOST}

# 2. 检查端口访问
telnet ${ENVIRONMENT_HOST} ${PORT}
nc -zv ${ENVIRONMENT_HOST} ${PORT}

# 3. 检查DNS解析
nslookup ${ENVIRONMENT_HOST}
dig ${ENVIRONMENT_HOST}

# 4. 检查防火墙规则
iptables -L -n | grep ${PORT}
```

**常见原因和解决方案：**
| 原因 | 解决方案 | 验证方法 |
|------|----------|----------|
| 网络不通 | 检查网络配置和路由 | ping/traceroute |
| 端口未开放 | 检查防火墙和端口配置 | telnet/nc |
| DNS解析失败 | 检查DNS配置 | nslookup/dig |
| 服务未启动 | 检查服务状态 | systemctl status |
| 负载均衡问题 | 检查负载均衡器配置 | curl -v |

### 1.2 访问速度慢
**症状：** 访问测试环境响应时间过长

**诊断步骤：**
```bash
# 1. 测量响应时间
time curl -o /dev/null -s -w "%{time_total}\n" ${URL}

# 2. 检查网络延迟
mtr ${ENVIRONMENT_HOST}

# 3. 检查服务器负载
ssh ${SERVER} "uptime; free -h; df -h"

# 4. 检查应用性能
curl -H "X-Debug: true" ${URL}
```

**优化建议：**
1. **网络优化**：使用CDN或优化路由
2. **缓存优化**：增加缓存层，减少数据库查询
3. **代码优化**：优化慢查询和算法
4. **资源扩容**：增加服务器资源

## 第二章：服务启动问题

### 2.1 应用启动失败
**症状：** 应用无法正常启动，日志显示错误

**诊断步骤：**
```bash
# 1. 查看应用日志
kubectl logs -f ${POD_NAME} -n ${NAMESPACE}
journalctl -u ${SERVICE_NAME} -f

# 2. 检查应用配置
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}
kubectl get configmap ${CONFIGMAP_NAME} -n ${NAMESPACE} -o yaml

# 3. 检查依赖服务
kubectl get services -n ${NAMESPACE}
curl ${DEPENDENCY_SERVICE}:${PORT}/health

# 4. 检查资源限制
kubectl describe pod ${POD_NAME} -n ${NAMESPACE} | grep -A 5 "Limits"
```

**常见启动错误：**
```yaml
# 错误1：配置错误
Error: Configuration file not found or invalid

# 解决方案：
1. 检查ConfigMap配置
2. 验证环境变量
3. 检查配置文件权限

# 错误2：依赖服务不可用
Error: Connection refused to database:3306

# 解决方案：
1. 检查数据库服务状态
2. 验证网络连接
3. 检查防火墙规则

# 错误3：资源不足
Error: OOMKilled or CPU throttling

# 解决方案：
1. 增加资源限制
2. 优化应用内存使用
3. 检查内存泄漏
```

### 2.2 服务健康检查失败
**症状：** Kubernetes健康检查失败，Pod不断重启

**诊断步骤：**
```bash
# 1. 查看健康检查配置
kubectl describe pod ${POD_NAME} -n ${NAMESPACE} | grep -A 10 "Liveness\|Readiness"

# 2. 手动执行健康检查
curl http://${POD_IP}:${PORT}/health
curl http://${POD_IP}:${PORT}/ready

# 3. 检查应用状态
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- ps aux
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- netstat -tlnp

# 4. 查看事件日志
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
```

**健康检查配置示例：**
```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 30  # 应用启动时间
  periodSeconds: 10        # 检查间隔
  timeoutSeconds: 5        # 超时时间
  failureThreshold: 3      # 失败阈值

readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 1
```

## 第三章：配置问题

### 3.1 配置加载错误
**症状：** 应用启动时配置加载失败

**诊断步骤：**
```bash
# 1. 检查配置源
kubectl get configmap,secret -n ${NAMESPACE}

# 2. 查看配置内容
kubectl get configmap ${CONFIG_NAME} -n ${NAMESPACE} -o yaml
kubectl get secret ${SECRET_NAME} -n ${NAMESPACE} -o yaml

# 3. 验证配置格式
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- cat /etc/config/app.properties

# 4. 检查配置更新
kubectl rollout status deployment/${DEPLOYMENT_NAME} -n ${NAMESPACE}
```

**配置管理最佳实践：**
1. **配置版本化**：所有配置纳入版本控制
2. **配置验证**：部署前验证配置格式
3. **配置回滚**：支持快速配置回滚
4. **配置审计**：记录所有配置变更

### 3.2 环境变量错误
**症状：** 环境变量缺失或错误导致应用异常

**诊断步骤：**
```bash
# 1. 查看环境变量
kubectl describe pod ${POD_NAME} -n ${NAMESPACE} | grep -A 20 "Environment"

# 2. 检查环境变量值
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- env | grep ${VAR_NAME}

# 3. 验证环境变量来源
kubectl get deployment ${DEPLOYMENT_NAME} -n ${NAMESPACE} -o yaml | grep -A 5 "env"

# 4. 检查Secret引用
kubectl get secret ${SECRET_NAME} -n ${NAMESPACE} -o yaml | grep ${VAR_NAME}
```

**环境变量管理规范：**
```yaml
env:
  # 直接从值设置
  - name: ENVIRONMENT
    value: "test"
  
  # 从ConfigMap引用
  - name: CONFIG_VALUE
    valueFrom:
      configMapKeyRef:
        name: app-config
        key: config.key
  
  # 从Secret引用
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-secret
        key: password
```

## 第四章：资源问题

### 4.1 内存不足
**症状：** 应用频繁OOM（Out Of Memory）或被Kill

**诊断步骤：**
```bash
# 1. 查看内存使用
kubectl top pods -n ${NAMESPACE}
kubectl describe pod ${POD_NAME} -n ${NAMESPACE} | grep -A 5 "Memory"

# 2. 检查内存限制
kubectl get pod ${POD_NAME} -n ${NAMESPACE} -o yaml | grep -A 3 "resources"

# 3. 分析内存使用
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- ps aux --sort=-%mem
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- free -h

# 4. 查看GC日志
kubectl logs ${POD_NAME} -n ${NAMESPACE} | grep -i "gc\|memory\|heap"
```

**内存优化建议：**
1. **调整JVM参数**：优化堆内存设置
2. **内存分析工具**：使用jmap、jstat分析内存使用
3. **代码优化**：减少内存泄漏，优化数据结构
4. **资源调整**：适当增加内存限制

### 4.2 CPU资源不足
**症状：** 应用响应慢，CPU使用率高

**诊断步骤：**
```bash
# 1. 查看CPU使用
kubectl top pods -n ${NAMESPACE}
kubectl describe pod ${POD_NAME} -n ${NAMESPACE} | grep -A 5 "CPU"

# 2. 检查CPU限制
kubectl get pod ${POD_NAME} -n ${NAMESPACE} -o yaml | grep -A 3 "resources"

# 3. 分析CPU使用
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- top -b -n 1
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- mpstat -P ALL

# 4. 检查线程状态
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- jstack ${PID} > thread_dump.txt
```

**CPU优化建议：**
1. **代码优化**：优化算法，减少计算复杂度
2. **并发控制**：合理控制线程数
3. **缓存优化**：增加缓存，减少重复计算
4. **资源调整**：适当增加CPU限制

## 第五章：网络问题

### 5.1 服务间通信失败
**症状：** 微服务间调用失败

**诊断步骤：**
```bash
# 1. 检查服务发现
nslookup ${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local
dig ${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local

# 2. 检查网络策略
kubectl get networkpolicy -n ${NAMESPACE}
kubectl describe networkpolicy ${POLICY_NAME} -n ${NAMESPACE}

# 3. 测试网络连通性
kubectl exec ${SOURCE_POD} -n ${NAMESPACE} -- curl -v http://${TARGET_SERVICE}:${PORT}

# 4. 检查Ingress配置
kubectl get ingress -n ${NAMESPACE}
kubectl describe ingress ${INGRESS_NAME} -n ${NAMESPACE}
```

**网络问题排查工具：**
```bash
# 网络诊断工具集
# 1. 基础连通性测试
ping ${HOST}
traceroute ${HOST}

# 2. 端口扫描
nmap -p ${PORT} ${HOST}

# 3. 网络抓包
tcpdump -i any port ${PORT} -w capture.pcap

# 4. 连接测试
nc -zv ${HOST} ${PORT}
telnet ${HOST} ${PORT}
```

### 5.2 DNS解析问题
**症状：** 域名解析失败或解析错误

**诊断步骤：**
```bash
# 1. 检查DNS配置
cat /etc/resolv.conf
kubectl get pod ${POD_NAME} -n ${NAMESPACE} -o yaml | grep -A 5 "dnsConfig"

# 2. 测试DNS解析
nslookup ${DOMAIN}
dig ${DOMAIN} @${DNS_SERVER}

# 3. 检查CoreDNS状态
kubectl get pods -n kube-system -l k8s-app=kube-dns
kubectl logs -n kube-system ${COREDNS_POD}

# 4. 检查DNS缓存
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- nscd -g
```

**DNS配置示例：**
```yaml
dnsConfig:
  nameservers:
    - 8.8.8.8
    - 8.8.4.4
  searches:
    - ${NAMESPACE}.svc.cluster.local
    - svc.cluster.local
    - cluster.local
  options:
    - name: ndots
      value: "2"
    - name: timeout
      value: "2"
```

## 第六章：存储问题

### 6.1 存储卷挂载失败
**症状：** Pod启动失败，存储卷无法挂载

**诊断步骤：**
```bash
# 1. 检查PVC状态
kubectl get pvc -n ${NAMESPACE}
kubectl describe pvc ${PVC_NAME} -n ${NAMESPACE}

# 2. 检查PV状态
kubectl get pv
kubectl describe pv ${PV_NAME}

# 3. 检查存储类
kubectl get storageclass
kubectl describe storageclass ${STORAGE_CLASS}

# 4. 查看Pod事件
kubectl describe pod ${POD_NAME} -n ${NAMESPACE} | grep -A 10 "Events"
```

**存储配置示例：**
```yaml
# PVC定义
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-data
  namespace: ${NAMESPACE}
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: standard
  resources:
    requests:
      storage: 10Gi

# Pod中使用
volumes:
  - name: app-storage
    persistentVolumeClaim:
      claimName: app-data
```

### 6.2 磁盘空间不足
**症状：** 应用无法写入，磁盘空间告警

**诊断步骤：**
```bash
# 1. 检查磁盘使用
df -h
du -sh /path/to/directory

# 2. 检查Pod磁盘使用
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- df -h
kubectl exec ${POD_NAME} -n ${NAMESPACE} -- du -sh /data

# 3. 查找大文件
find /path -type f -size +100M -exec ls -lh {} \;
find /path -type f -name "*.log" -size +50M

# 4. 清理旧文件
find /path -type f -name "*.log" -mtime +7 -delete
```

**磁盘管理策略：**
1. **日志轮转**：配置日志轮转策略
2. **临时文件清理**：定期清理临时文件
3. **存储扩容**：动态调整存储大小
4. **监控告警**：设置磁盘使用率告警

## 第七章：安全与权限

### 7.1 权限不足
**症状：** 操作被拒绝，权限错误

**诊断步骤：**
```bash
# 1. 检查RBAC配置
kubectl get role,rolebinding -n ${NAMESPACE}
kubectl describe role ${ROLE_NAME} -n ${NAMESPACE}

# 2. 检查ServiceAccount
kubectl get serviceaccount -n ${NAMESPACE}
kubectl describe serviceaccount ${SA_NAME} -n ${NAMESPACE}

# 3. 检查Pod使用的SA
kubectl get pod ${POD_NAME} -n ${NAMESPACE} -o yaml | grep serviceAccount

# 4. 测试权限
kubectl auth can-i create pod -n ${NAMESPACE} --as=system:serviceaccount:${NAMESPACE}:${SA_NAME}
```

**RBAC配置示例：**
```yaml
# Role定义
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: ${NAMESPACE}
  name: pod-reader
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "watch", "list"]

# RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  namespace: ${NAMESPACE}
  name: read-pods
subjects:
- kind: ServiceAccount
  name: default
  namespace: ${NAMESPACE}
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

### 7.2 安全策略限制
**症状：** 网络访问被拒绝，Pod无法启动

**诊断步骤：**
```bash
# 1. 检查NetworkPolicy
kubectl get networkpolicy -n ${NAMESPACE}
kubectl describe networkpolicy ${POLICY_NAME} -n ${NAMESPACE}

# 2. 检查PodSecurityPolicy
kubectl get psp
kubectl describe psp ${PSP_NAME}

# 3. 检查SecurityContext
kubectl get pod ${POD_NAME} -n ${NAMESPACE} -o yaml | grep -A 10 "securityContext"

# 4. 查看安全事件
kubectl get events -n ${NAMESPACE} --field-selector involvedObject.kind=Pod | grep -i "forbidden\|denied"
```

## 第八章：监控与日志

### 8.1 监控指标异常
**症状：** 监控告警，指标异常

**诊断步骤：**
```bash
# 1. 查看监控指标
kubectl top pods -n ${NAMESPACE}
kubectl top nodes

# 2. 检查Prometheus指标
curl http://${PROMETHEUS_URL}/api/v1/query?query=${METRIC_NAME}

# 3. 查看Grafana仪表板
# 访问Grafana查看相关仪表板

# 4. 分析指标趋势
# 使用PromQL分析指标变化趋势
```

**关键监控指标：**
- **CPU使用率**：`rate(container_cpu_usage_seconds_total[5m])`
- **内存使用率**：`container_memory_working_set_bytes`
- **网络流量**：`rate(container_network_receive_bytes_total[5m])`
- **磁盘IO**：`rate(container_fs_reads_bytes_total[5m])`

### 8.2 日志分析
**症状：** 需要分析应用日志排查问题

**诊断步骤：**
```bash
# 1. 查看实时日志
kubectl logs -f ${POD_NAME} -n ${NAMESPACE}

# 2. 查看历史日志
kubectl logs --since=1h ${POD_NAME} -n ${NAMESPACE}

# 3. 日志搜索
kubectl logs ${POD_NAME} -n ${NAMESPACE} | grep -i "error\|exception\|failed"

# 4. 多容器日志
kubectl logs ${POD_NAME} -c ${CONTAINER_NAME} -n ${NAMESPACE}
```

**日志分析技巧：**
1. **时间过滤**：`--since`, `--tail`
2. **关键词搜索**：`grep -i "error\|warn"`
3. **JSON日志解析**：`jq`工具
4. **日志聚合**：ELK Stack, Loki

## 第九章：自动化诊断脚本

### 9.1 环境健康检查脚本
```bash
#!/bin/bash
# 环境健康检查脚本

set -e

NAMESPACE=${1:-default}
TIMEOUT=30

echo "=== 开始环境健康检查 ==="
echo "命名空间: $NAMESPACE"
echo "检查时间: $(date)"
echo ""

# 1. 检查命名空间是否存在
echo "1. 检查命名空间..."
if kubectl get namespace $NAMESPACE &>/dev/null; then
    echo "✓ 命名空间 $NAMESPACE 存在"
else
    echo "✗ 命名空间 $NAMESPACE 不存在"
    exit 1
fi

# 2. 检查Pod状态
echo "2. 检查Pod状态..."
PODS=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
if [ $PODS -eq 0 ]; then
    echo "⚠ 命名空间 $NAMESPACE 中没有Pod"
else
    echo "发现 $PODS 个Pod"
    kubectl get pods -n $NAMESPACE
fi

# 3. 检查服务状态
echo "3. 检查服务状态..."
SERVICES=$(kubectl get services -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
if [ $SERVICES -eq 0 ]; then
    echo "⚠ 命名空间 $NAMESPACE 中没有服务"
else
    echo "发现 $SERVICES 个服务"
    kubectl get services -n $NAMESPACE
fi

# 4. 检查ConfigMap和Secret
echo "4. 检查配置..."
CONFIGMAPS=$(kubectl get configmaps -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
SECRETS=$(kubectl get secrets -n $NAMESPACE --no-headers 2>/dev/null | wc -l)
echo "发现 $CONFIGMAPS 个ConfigMap, $SECRETS 个Secret"

# 5. 检查事件
echo "5. 检查最近事件..."
kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp' --field-selector type!=Normal --tail=10 2>/dev/null || true

# 6. 检查资源使用
echo "6. 检查资源使用..."
if command -v kubectl-top &>/dev/null; then
    kubectl top pods -n $NAMESPACE 2>/dev/null || echo "无法获取资源使用信息"
else
    echo "kubectl top 命令不可用"
fi

echo ""
echo "=== 环境健康检查完成 ==="
echo "总结:"
echo "- 命名空间: $NAMESPACE"
echo "- Pod数量: $PODS"
echo "- 服务数量: $SERVICES"
echo "- 配置数量: $((CONFIGMAPS + SECRETS))"
```

### 9.2 快速诊断脚本
```bash
#!/bin/bash
# 快速诊断脚本

diagnose_pod() {
    local POD=$1
    local NAMESPACE=${2:-default}
    
    echo "=== 诊断Pod: $POD ==="
    
    # 检查Pod状态
    echo "1. Pod状态:"
    kubectl get pod $POD -n $NAMESPACE -o wide
    
    # 检查Pod事件
    echo -e "\n2. Pod事件:"
    kubectl describe pod $POD -n $NAMESPACE | grep -A 20 "Events"
    
    # 检查容器状态
    echo -e "\n3. 容器状态:"
    kubectl describe pod $POD -n $NAMESPACE | grep -A 10 "Containers"
    
    # 检查日志
    echo -e "\n4. 最近日志:"
    kubectl logs $POD -n $NAMESPACE --tail=20 2>/dev/null || echo "无法获取日志"
    
    # 检查资源限制
    echo -e "\n5. 资源限制:"
    kubectl describe pod $POD -n $NAMESPACE | grep -A 5 "Limits\|Requests"
}

# 使用示例
# diagnose_pod my-pod my-namespace
```

## 第十章：问题上报流程

### 10.1 问题分类
| 问题等级 | 响应时间 | 处理时限 | 上报路径 |
|----------|----------|----------|----------|
| P0-紧急 | 15分钟 | 2小时 | 直接电话+工单 |
| P1-高 | 30分钟 | 4小时 | 工单+邮件 |
| P2-中 | 2小时 | 1天 | 工单 |
| P3-低 | 4小时 | 3天 | 工单 |

### 10.2 问题报告模板
```markdown
## 问题报告

### 基本信息
- **报告人**：
- **报告时间**：
- **环境**：
- **问题等级**：

### 问题描述
**现象**：
**发生时间**：
**影响范围**：
**复现步骤**：

### 诊断信息
**已尝试的解决方案**：
**相关日志**：
**监控指标**：
**环境配置**：

### 紧急程度评估
- [ ] 影响生产功能
- [ ] 影响测试进度
- [ ] 影响多个团队
- [ ] 有变通方案

### 附件
- 日志文件
- 截图
- 配置信息
```

### 10.3 问题跟踪
1. **创建工单**：在问题跟踪系统创建工单
2. **分配负责人**：根据问题类型分配负责人
3. **跟踪进度**：定期更新处理进度
4. **问题关闭**：问题解决后关闭工单
5. **经验总结**：将解决方案加入知识库

## 附录

### A. 常用命令速查
```bash
# Pod管理
kubectl get pods
kubectl describe pod <pod-name>
kubectl logs <pod-name>
kubectl exec -it <pod-name> -- /bin/bash

# 部署管理
kubectl get deployments
kubectl describe deployment <deployment-name>
kubectl rollout status deployment/<deployment-name>

# 服务管理
kubectl get services
kubectl describe service <service-name>

# 配置管理
kubectl get configmaps
kubectl get secrets
```

### B. 监控指标说明
| 指标 | 正常范围 | 告警阈值 | 说明 |
|------|----------|----------|------|
| CPU使用率 | <70% | >85% | 持续5分钟超过阈值 |
| 内存使用率 | <80% | >90% | 持续5分钟超过阈值 |
| 磁盘使用率 | <85% | >95% | 持续10分钟超过阈值 |
| 网络错误率 | <0.1% | >1% | 持续2分钟超过阈值 |
| 请求延迟 | <200ms | >500ms | P95延迟超过阈值 |

### C. 紧急恢复步骤
1. **确定影响范围**：评估问题影响
2. **启用备用方案**：如有备用方案立即启用
3. **收集诊断信息**：收集日志和监控数据
4. **实施临时修复**：实施临时解决方案
5. **根本原因分析**：分析问题根本原因
6. **实施永久修复**：实施永久解决方案
7. **复盘总结**：总结问题原因和改进措施

---

**手册版本**：1.0  
**最后更新**：2026年5月5日  
**维护团队**：测试环境运维组  
**联系方式**：env-support@company.com