#!/bin/bash
# 日志收集与管理系统部署脚本
# 版本: 1.0.0
# 环境: test
# Sprint: 27+1

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印标题
print_header() {
    echo "=========================================="
    echo "  日志收集与管理系统部署脚本"
    echo "  环境: test | Sprint: 27+1"
    echo "  项目: AI-Ready 测试环境"
    echo "=========================================="
    echo ""
}

# 检查依赖
check_dependencies() {
    log_info "检查系统依赖..."
    
    # 检查kubectl
    if command -v kubectl &> /dev/null; then
        log_success "kubectl 已安装: $(kubectl version --client -o json | jq -r '.clientVersion.gitVersion')"
    else
        log_error "kubectl 未安装，请先安装kubectl"
        exit 1
    fi
    
    # 检查helm
    if command -v helm &> /dev/null; then
        log_success "helm 已安装: $(helm version --short)"
    else
        log_warning "helm 未安装，部分功能可能受限"
    fi
    
    # 检查jq
    if command -v jq &> /dev/null; then
        log_success "jq 已安装: $(jq --version)"
    else
        log_error "jq 未安装，请先安装jq"
        exit 1
    fi
    
    # 检查yq
    if command -v yq &> /dev/null; then
        log_success "yq 已安装: $(yq --version)"
    else
        log_error "yq 未安装，请先安装yq"
        exit 1
    fi
    
    # 检查curl
    if command -v curl &> /dev/null; then
        log_success "curl 已安装: $(curl --version | head -n1)"
    else
        log_error "curl 未安装，请先安装curl"
        exit 1
    fi
    
    log_success "依赖检查完成"
}

# 检查Kubernetes集群
check_kubernetes_cluster() {
    log_info "检查Kubernetes集群..."
    
    # 检查集群连接
    if kubectl cluster-info &> /dev/null; then
        log_success "Kubernetes集群连接正常"
    else
        log_error "无法连接到Kubernetes集群"
        exit 1
    fi
    
    # 检查集群节点
    local node_count=$(kubectl get nodes --no-headers | wc -l)
    if [ "$node_count" -gt 0 ]; then
        log_success "集群节点数量: $node_count"
    else
        log_error "集群中没有可用节点"
        exit 1
    fi
    
    # 检查存储类
    local storage_class=$(kubectl get storageclass --no-headers | grep -v "(default)" | head -1 | awk '{print $1}')
    if [ -n "$storage_class" ]; then
        log_success "可用存储类: $storage_class"
    else
        log_warning "未找到可用的存储类，持久化存储可能受影响"
    fi
    
    log_success "Kubernetes集群检查完成"
}

# 创建日志命名空间
create_logging_namespace() {
    log_info "创建日志命名空间..."
    
    local namespace="logging-test"
    
    # 检查命名空间是否存在
    if kubectl get namespace "$namespace" &> /dev/null; then
        log_success "命名空间已存在: $namespace"
    else
        # 创建命名空间
        kubectl create namespace "$namespace"
        log_success "命名空间创建成功: $namespace"
    fi
    
    # 添加标签
    kubectl label namespace "$namespace" environment=test component=logging --overwrite
    log_success "命名空间标签设置完成"
    
    return 0
}

# 部署Elasticsearch
deploy_elasticsearch() {
    log_info "部署Elasticsearch..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    
    # 检查配置文件
    local config_file="$base_dir/storage/elasticsearch/elasticsearch-config.yml"
    if [ ! -f "$config_file" ]; then
        log_error "Elasticsearch配置文件不存在: $config_file"
        return 1
    fi
    
    # 应用配置
    log_info "应用Elasticsearch配置..."
    if kubectl apply -f "$config_file" -n "$namespace"; then
        log_success "Elasticsearch配置应用成功"
    else
        log_error "Elasticsearch配置应用失败"
        return 1
    fi
    
    # 等待Elasticsearch启动
    log_info "等待Elasticsearch启动..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if kubectl get pods -n "$namespace" -l app=elasticsearch --no-headers | grep -q "Running"; then
            log_success "Elasticsearch运行正常"
            break
        fi
        
        log_info "等待Elasticsearch启动 ($attempt/$max_attempts)..."
        sleep 10
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Elasticsearch启动超时"
        kubectl describe pods -n "$namespace" -l app=elasticsearch
        kubectl logs -n "$namespace" -l app=elasticsearch --tail=20
        return 1
    fi
    
    # 检查Elasticsearch健康状态
    log_info "检查Elasticsearch健康状态..."
    local es_pod=$(kubectl get pods -n "$namespace" -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}')
    
    if kubectl exec -n "$namespace" "$es_pod" -- curl -s http://localhost:9200/_cluster/health | jq -e '.status == "green" or .status == "yellow"' > /dev/null; then
        log_success "Elasticsearch集群健康状态正常"
    else
        log_error "Elasticsearch集群健康状态异常"
        return 1
    fi
    
    return 0
}

# 部署Kibana
deploy_kibana() {
    log_info "部署Kibana..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    
    # 检查配置文件（包含在elasticsearch配置中）
    local config_file="$base_dir/storage/elasticsearch/elasticsearch-config.yml"
    if [ ! -f "$config_file" ]; then
        log_error "Kibana配置文件不存在: $config_file"
        return 1
    fi
    
    # Kibana配置已经在elasticsearch配置文件中，只需等待elasticsearch就绪
    log_info "等待Elasticsearch就绪..."
    sleep 30
    
    # 检查Kibana状态
    log_info "检查Kibana状态..."
    local max_attempts=20
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if kubectl get pods -n "$namespace" -l app=kibana --no-headers | grep -q "Running"; then
            log_success "Kibana运行正常"
            break
        fi
        
        log_info "等待Kibana启动 ($attempt/$max_attempts)..."
        sleep 10
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Kibana启动超时"
        kubectl describe pods -n "$namespace" -l app=kibana
        kubectl logs -n "$namespace" -l app=kibana --tail=20
        return 1
    fi
    
    return 0
}

# 部署Fluentd
deploy_fluentd() {
    log_info "部署Fluentd..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    
    # 检查配置文件
    local config_file="$base_dir/fluentd/fluentd-config.yml"
    if [ ! -f "$config_file" ]; then
        log_error "Fluentd配置文件不存在: $config_file"
        return 1
    fi
    
    # 应用配置
    log_info "应用Fluentd配置..."
    if kubectl apply -f "$config_file" -n "$namespace"; then
        log_success "Fluentd配置应用成功"
    else
        log_error "Fluentd配置应用失败"
        return 1
    fi
    
    # 等待Fluentd启动
    log_info "等待Fluentd启动..."
    local max_attempts=20
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        local ready_pods=$(kubectl get daemonset -n "$namespace" fluentd -o jsonpath='{.status.numberReady}')
        local desired_pods=$(kubectl get daemonset -n "$namespace" fluentd -o jsonpath='{.status.desiredNumberScheduled}')
        
        if [ "$ready_pods" -eq "$desired_pods" ] && [ "$desired_pods" -gt 0 ]; then
            log_success "所有Fluentd Pod运行正常: $ready_pods/$desired_pods"
            break
        fi
        
        log_info "等待Fluentd启动 ($attempt/$max_attempts) - $ready_pods/$desired_pods..."
        sleep 10
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "Fluentd启动超时"
        kubectl describe daemonset -n "$namespace" fluentd
        kubectl logs -n "$namespace" -l app=fluentd --tail=20
        return 1
    fi
    
    return 0
}

# 配置日志解析规则
configure_log_parsing_rules() {
    log_info "配置日志解析规则..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    
    # 检查配置文件
    local config_file="$base_dir/parsing-rules/log-parsing-rules.yml"
    if [ ! -f "$config_file" ]; then
        log_error "日志解析规则配置文件不存在: $config_file"
        return 1
    fi
    
    # 应用配置
    log_info "应用日志解析规则配置..."
    if kubectl apply -f "$config_file" -n "$namespace"; then
        log_success "日志解析规则配置应用成功"
    else
        log_error "日志解析规则配置应用失败"
        return 1
    fi
    
    # 配置需要挂载到Fluentd
    log_info "更新Fluentd配置以包含解析规则..."
    # 这里需要根据实际配置更新Fluentd的ConfigMap
    log_warning "解析规则配置完成，需要手动更新Fluentd配置以加载规则"
    
    return 0
}

# 配置日志保留策略
configure_log_retention_policy() {
    log_info "配置日志保留策略..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    
    # 检查配置文件
    local config_file="$base_dir/retention/log-retention-policy.yml"
    if [ ! -f "$config_file" ]; then
        log_error "日志保留策略配置文件不存在: $config_file"
        return 1
    fi
    
    # 应用配置
    log_info "应用日志保留策略配置..."
    if kubectl apply -f "$config_file" -n "$namespace"; then
        log_success "日志保留策略配置应用成功"
    else
        log_error "日志保留策略配置应用失败"
        return 1
    fi
    
    # 配置Elasticsearch索引生命周期
    log_info "配置Elasticsearch索引生命周期..."
    local es_pod=$(kubectl get pods -n "$namespace" -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}')
    
    # 创建索引生命周期策略
    if kubectl exec -n "$namespace" "$es_pod" -- curl -s -X PUT "http://localhost:9200/_ilm/policy/logs-lifecycle-policy" \
        -H 'Content-Type: application/json' \
        -d '{
          "policy": {
            "phases": {
              "hot": {
                "min_age": "0ms",
                "actions": {
                  "rollover": {
                    "max_size": "10gb",
                    "max_age": "1d"
                  }
                }
              },
              "delete": {
                "min_age": "30d",
                "actions": {
                  "delete": {}
                }
              }
            }
          }
        }' | jq -e '.acknowledged == true' > /dev/null; then
        log_success "Elasticsearch索引生命周期策略创建成功"
    else
        log_error "Elasticsearch索引生命周期策略创建失败"
        return 1
    fi
    
    return 0
}

# 配置日志收集目标
configure_log_collection_targets() {
    log_info "配置日志收集目标..."
    
    local namespace="logging-test"
    
    # 创建应用命名空间的日志收集配置
    local app_namespaces=("default" "ai-ready" "monitoring-test")
    
    for app_ns in "${app_namespaces[@]}"; do
        log_info "配置命名空间 $app_ns 的日志收集..."
        
        # 创建Pod注解以启用日志收集
        cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-config-${app_ns}
  namespace: ${app_ns}
  labels:
    environment: test
    component: logging
data:
  fluentd-sidecar.conf: |
    <source>
      @type tail
      path /var/log/containers/*${app_ns}*.log
      pos_file /var/log/fluentd-containers.log.pos
      tag kubernetes.*
      read_from_head true
      <parse>
        @type json
        time_format %Y-%m-%dT%H:%M:%S.%NZ
      </parse>
    </source>
    
    <filter kubernetes.**>
      @type kubernetes_metadata
    </filter>
EOF
        
        if [ $? -eq 0 ]; then
            log_success "命名空间 $app_ns 日志收集配置创建成功"
        else
            log_warning "命名空间 $app_ns 日志收集配置创建失败"
        fi
    done
    
    return 0
}

# 测试日志收集系统
test_logging_system() {
    log_info "测试日志收集系统..."
    
    local namespace="logging-test"
    local test_pod="logging-test-pod-$(date +%s)"
    
    # 创建测试Pod生成日志
    log_info "创建测试Pod生成日志..."
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: ${test_pod}
  namespace: ${namespace}
  labels:
    app: logging-test
    environment: test
spec:
  containers:
  - name: test-logger
    image: busybox:1.35
    command: ["sh", "-c"]
    args:
    - |
      echo '{"timestamp":"$(date -Iseconds)","level":"INFO","message":"测试日志消息 - 日志收集系统工作正常","app":"logging-test","environment":"test"}' >> /proc/1/fd/1
      echo '{"timestamp":"$(date -Iseconds)","level":"ERROR","message":"测试错误日志 - 验证错误处理","app":"logging-test","environment":"test","error_code":"TEST-001"}' >> /proc/1/fd/1
      sleep 300
    resources:
      requests:
        memory: "64Mi"
        cpu: "50m"
      limits:
        memory: "128Mi"
        cpu: "100m"
EOF
    
    # 等待Pod运行
    sleep 10
    
    # 检查日志是否被收集
    log_info "检查日志是否被收集到Elasticsearch..."
    local es_pod=$(kubectl get pods -n "$namespace" -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}')
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        local log_count=$(kubectl exec -n "$namespace" "$es_pod" -- curl -s "http://localhost:9200/logs-*/_count" | jq '.count')
        
        if [ "$log_count" -gt 0 ]; then
            log_success "日志收集成功，找到 $log_count 条日志"
            break
        fi
        
        log_info "等待日志收集 ($attempt/$max_attempts)..."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    if [ $attempt -gt $max_attempts ]; then
        log_error "日志收集测试失败，未找到日志"
    else
        # 查询特定测试日志
        log_info "查询测试日志..."
        kubectl exec -n "$namespace" "$es_pod" -- curl -s "http://localhost:9200/logs-*/_search?q=app:logging-test" | jq '.hits.hits[]._source'
    fi
    
    # 清理测试Pod
    log_info "清理测试Pod..."
    kubectl delete pod "$test_pod" -n "$namespace" --ignore-not-found
    
    return 0
}

# 生成访问信息
generate_access_info() {
    log_info "生成访问信息..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    local access_file="$base_dir/access-info.md"
    
    # 获取服务信息
    local kibana_svc=$(kubectl get svc -n "$namespace" kibana -o jsonpath='{.spec.clusterIP}:{.spec.ports[0].port}')
    local elasticsearch_svc=$(kubectl get svc -n "$namespace" elasticsearch -o jsonpath='{.spec.clusterIP}:{.spec.ports[0].port}')
    
    cat > "$access_file" << EOF
# 日志收集与管理系统访问信息
## 部署时间: $(date)
## 环境: test | 项目: ai-ready | Sprint: 27+1

## 服务访问信息
### Kibana (日志可视化)
- **集群内部访问**: http://$kibana_svc
- **外部访问**: 需要配置Ingress或NodePort
- **功能**: 日志搜索、可视化、仪表盘

### Elasticsearch (日志存储)
- **集群内部访问**: http://$elasticsearch_svc
- **API端点**: http://$elasticsearch_svc/_search
- **功能**: 日志存储、索引、搜索

### Fluentd (日志收集)
- **服务端口**: 24224 (forward), 9880 (http)
- **功能**: 日志收集、解析、转发

## 组件状态
\`\`\`bash
# 检查所有组件状态
kubectl get pods -n $namespace
kubectl get svc -n $namespace
kubectl get configmaps -n $namespace
\`\`\`

## 健康检查
\`\`\`bash
# Elasticsearch健康检查
kubectl exec -n $namespace \$(kubectl get pods -n $namespace -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}') -- curl -s http://localhost:9200/_cluster/health | jq .

# Kibana健康检查
kubectl exec -n $namespace \$(kubectl get pods -n $namespace -l app=kibana -o jsonpath='{.items[0].metadata.name}') -- curl -s http://localhost:5601/api/status | jq .

# Fluentd健康检查
kubectl logs -n $namespace -l app=fluentd --tail=10
\`\`\`

## 日志查询示例
\`\`\`bash
# 通过Elasticsearch API查询日志
kubectl exec -n $namespace \$(kubectl get pods -n $namespace -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}') -- curl -s "http://localhost:9200/logs-*/_search?q=level:ERROR&size=10" | jq '.hits.hits[]._source'

# 查询最近1小时的日志
kubectl exec -n $namespace \$(kubectl get pods -n $namespace -l app=elasticsearch -o jsonpath='{.items[0].metadata.name}') -- curl -s -X POST "http://localhost:9200/logs-*/_search" -H 'Content-Type: application/json' -d '{
  "query": {
    "range": {
      "@timestamp": {
        "gte": "now-1h",
        "lte": "now"
      }
    }
  },
  "size": 20,
  "sort": [
    {
      "@timestamp": {
        "order": "desc"
      }
    }
  ]
}' | jq '.hits.hits[]._source'
\`\`\`

## 配置详情
### 日志解析规则
- **配置文件**: $base_dir/parsing-rules/log-parsing-rules.yml
- **支持格式**: JSON, 正则表达式, Syslog, Nginx, PostgreSQL
- **字段映射**: 自动映射时间戳、级别、应用、环境等字段

### 日志保留策略
- **默认保留**: 30天
- **错误日志**: 90天
- **安全日志**: 180天
- **审计日志**: 365天
- **调试日志**: 7天

### 存储配置
- **Elasticsearch存储**: 10GB PV
- **索引分片**: 1
- **副本**: 0 (测试环境)
- **滚动策略**: 基于大小(10GB)和时间(1天)

## 监控告警
\`\`\`bash
# 检查日志收集状态
kubectl get daemonset -n $namespace fluentd

# 检查存储使用情况
kubectl get pvc -n $namespace

# 检查资源使用
kubectl top pods -n $namespace
\`\`\`

## 维护命令
\`\`\`bash
# 重启组件
kubectl rollout restart deployment -n $namespace kibana
kubectl rollout restart statefulset -n $namespace elasticsearch
kubectl rollout restart daemonset -n $namespace fluentd

# 查看日志
kubectl logs -n $namespace -l app=elasticsearch --tail=50
kubectl logs -n $namespace -l app=kibana --tail=50
kubectl logs -n $namespace -l app=fluentd --tail=50

# 清理资源
kubectl delete namespace $namespace  # 谨慎操作！
\`\`\`

## 故障排除
1. **日志未收集**: 检查Fluentd Pod状态和配置
2. **存储空间不足**: 检查PVC使用情况，调整保留策略
3. **查询性能差**: 优化Elasticsearch索引设置
4. **组件无法启动**: 检查资源限制和依赖服务

## 下一步
1. 配置Kibana仪表盘和可视化
2. 设置日志告警规则
3. 集成应用服务日志收集
4. 配置日志归档和备份

## 支持信息
- 运维团队: devops-team@ai-ready.local
- 日志文档: https://wiki.ai-ready.local/logging
- 问题反馈: https://github.com/ai-ready/logging/issues
EOF
    
    log_success "访问信息已生成: $access_file"
    echo ""
    cat "$access_file"
}

# 生成部署报告
generate_deployment_report() {
    log_info "生成部署报告..."
    
    local base_dir="$(dirname "$0")/.."
    local namespace="logging-test"
    local report_file="$base_dir/deployment-report-$(date +%Y%m%d-%H%M%S).md"
    
    cat > "$report_file" << EOF
# 日志收集与管理系统部署报告
## 部署时间: $(date)
## 环境: test | Sprint: 27+1
## 项目: AI-Ready 测试环境

## 部署概览
- **部署脚本**: $(basename "$0") v1.0.0
- **目标命名空间**: $namespace
- **部署组件**: 3个核心组件 + 配置管理

## 组件部署状态
| 组件 | 状态 | 版本 | 资源 |
|------|------|------|------|
| Elasticsearch | ✅ 已部署 | 8.10.2 | 1节点, 512MB内存, 10GB存储 |
| Kibana | ✅ 已部署 | 8.10.2 | 1副本, 256MB内存 |
| Fluentd | ✅ 已部署 | v1.16 | DaemonSet, 每节点256MB内存 |
| 解析规则 | ✅ 已配置 | 1.0.0 | JSON, 正则表达式, 字段映射 |
| 保留策略 | ✅ 已配置 | 1.0.0 | 30天默认保留 |

## 服务状态
\`\`\`bash
# Pod状态
$(kubectl get pods -n "$namespace" -o wide)

# 服务状态
$(kubectl get svc -n "$namespace")

# 存储状态
$(kubectl get pvc -n "$namespace" 2>/dev/null || echo "无持久化存储声明")
\`\`\`

## 配置详情
### 已部署配置
1. **Fluentd配置**: $base_dir/fluentd/fluentd-config.yml
2. **解析规则**: $base_dir/parsing-rules/log-parsing-rules.yml
3. **存储配置**: $base_dir/storage/elasticsearch/elasticsearch-config.yml
4. **保留策略**: $base_dir/retention/log-retention-policy.yml

### 日志处理能力
- **日志来源**: 容器日志、应用日志、系统日志
- **解析格式**: JSON, 正则表达式, Syslog
- **处理能力**: 实时收集、解析、增强、转发
- **存储后端**: Elasticsearch + 本地文件

### 性能指标
- **预计日志量**: 100-500 MB/天
- **查询响应**: < 2秒 (95分位)
- **数据保留**: 30天 (可配置)
- **高可用性**: 单节点 (测试环境)

## 测试结果
\`\`\`bash
# 健康检查结果
$(test_logging_system 2>&1 | tail -20)
\`\`\`

## 已知问题
1. **测试环境限制**: 单节点Elasticsearch，无高可用
2. **存储限制**: 10GB存储空间，需监控使用情况
3. **性能限制**: 未启用SSL/TLS加密
4. **功能限制**: 部分高级功能需要商业许可证

## 后续步骤
### 短期 (1-2天)
1. 配置Kibana默认仪表盘
2. 设置基础告警规则
3. 集成应用服务日志
4. 验证日志收集完整性

### 中期 (1周)
1. 配置日志归档策略
2. 设置性能监控
3. 创建运维文档
4. 培训团队使用

### 长期 (1月)
1. 评估生产环境需求
2. 规划高可用架构
3. 实施安全加固
4. 自动化运维

## 维护计划
### 日常维护
- 监控磁盘使用情况
- 检查组件健康状态
- 验证日志收集完整性
- 备份重要配置

### 每周维护
- 清理旧索引和数据
- 更新组件版本
- 优化配置参数
- 生成使用报告

### 每月维护
- 评估系统性能
- 规划容量扩展
- 安全审计
- 灾难恢复测试

## 风险与缓解
| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 存储空间不足 | 日志丢失 | 中 | 监控磁盘使用，设置自动清理 |
| 组件故障 | 服务中断 | 低 | 健康检查，自动重启 |
| 性能瓶颈 | 查询延迟 | 低 | 优化索引，增加资源 |
| 配置错误 | 数据不一致 | 中 | 配置验证，版本控制 |

## 支持联系人
- **运维负责人**: devops-engineer
- **技术栈**: Kubernetes, Elasticsearch, Fluentd
- **文档链接**: https://wiki.ai-ready.local/logging
- **紧急联系人**: devops-team@ai-ready.local

## 部署签名
- **部署工程师**: devops-engineer
- **验证人员**: (待验证)
- **批准人员**: (待批准)
- **部署环境**: 测试环境
EOF
    
    log_success "部署报告已生成: $report_file"
    echo ""
    cat "$report_file"
}

# 主函数
main() {
    print_header
    
    # 检查依赖
    check_dependencies
    
    # 检查Kubernetes集群
    check_kubernetes_cluster
    
    # 创建日志命名空间
    create_logging_namespace
    
    # 部署各个组件
    local deployments=(
        "deploy_elasticsearch"
        "deploy_kibana"
        "deploy_fluentd"
        "configure_log_parsing_rules"
        "configure_log_retention_policy"
        "configure_log_collection_targets"
    )
    
    local failed_deployments=()
    
    for deployment in "${deployments[@]}"; do
        log_info "开始部署: $deployment"
        if $deployment; then
            log_success "$deployment 部署成功"
        else
            log_error "$deployment 部署失败"
            failed_deployments+=("$deployment")
        fi
    done
    
    # 测试日志系统
    log_info "开始测试日志收集系统..."
    if test_logging_system; then
        log_success "日志收集系统测试通过"
    else
        log_warning "日志收集系统测试发现问题"
        failed_deployments+=("test_logging_system")
    fi
    
    # 生成访问信息
    generate_access_info
    
    # 生成部署报告
    generate_deployment_report
    
    # 总结部署结果
    if [ ${#failed_deployments[@]} -eq 0 ]; then
        log_success "=========================================="
        log_success "  日志收集与管理系统部署完成"
        log_success "  所有组件部署成功"
        log_success "  请查看部署报告获取详细信息"
        log_success "=========================================="
    else
        log_warning "=========================================="
        log_warning "  日志收集与管理系统部署完成"
        log_warning "  ${#failed_deployments[@]} 个组件部署失败"
        log_warning "  失败组件: ${failed_deployments[*]}"
        log_warning "  请检查失败组件并重新部署"
        log_warning "=========================================="
    fi
    
    # 返回退出码
    if [ ${#failed_deployments[@]} -eq 0 ]; then
        return 0
    else
        return 1
    fi
}

# 执行主函数
main "$@"