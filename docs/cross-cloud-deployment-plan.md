# ERP系统跨云部署方案

## 1. 云服务选型与配置

### 1.1 云服务商选择矩阵
| 云服务商 | 主要优势 | 适用场景 | 推荐产品 | 成本优化策略 |
|----------|----------|----------|----------|--------------|
| **阿里云** | 国内网络最优，生态完善 | 核心业务主云，国内用户访问 | ECS, RDS, SLB, VPC | 预留实例，按需竞价实例 |
| **腾讯云** | 性价比高，CDN优秀 | 灾备云，双活部署，内容分发 | CVM, TDSQL, CLB, VPC | 包年包月，阶梯定价 |
| **AWS** | 全球覆盖，技术领先 | 海外业务，国际化部署 | EC2, RDS, ELB, VPC | Spot实例，Savings Plans |
| **Azure** | 企业级服务，混合云强 | 混合云场景，微软生态集成 | VM, SQL Database, Load Balancer | 预留实例，混合使用权益 |

### 1.2 资源规格标准化
```yaml
# 标准计算资源配置
compute_tiers:
  small:
    cpu: 2
    memory_gb: 4
    disk_gb: 100
    use_case: "开发环境、测试环境"
  
  medium:
    cpu: 4
    memory_gb: 8
    disk_gb: 200
    use_case: "预生产环境、小型应用"
  
  large:
    cpu: 8
    memory_gb: 16
    disk_gb: 500
    use_case: "生产环境核心应用"
  
  xlarge:
    cpu: 16
    memory_gb: 32
    disk_gb: 1000
    use_case: "数据库、大数据处理"
```

## 2. 环境标准化规范

### 2.1 命名规范
```yaml
# 资源命名约定
naming_convention:
  region: "{cloud}-{region}"  # 如: aliyun-cn-beijing
  vpc: "vpc-{env}-{app}-{region}"  # 如: vpc-prod-erp-cn-beijing
  subnet: "subnet-{env}-{app}-{purpose}-{az}"  # 如: subnet-prod-erp-web-az-a
  instance: "{env}-{app}-{role}-{seq}"  # 如: prod-erp-web-001
  security_group: "sg-{env}-{app}-{purpose}"  # 如: sg-prod-erp-web
```

### 2.2 网络架构标准化
```
┌─────────────────────────────────────────────────────────────┐
│                    多VPC互联架构                            │
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    │
│  │  阿里云VPC  │◄──►│  腾讯云VPC  │◄──►│   AWS VPC   │    │
│  │ 10.0.0.0/16 │    │ 10.1.0.0/16 │    │ 10.2.0.0/16 │    │
│  └─────────────┘    └─────────────┘    └─────────────┘    │
│         │                    │                    │        │
│         ▼                    ▼                    ▼        │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    │
│  │  应用子网   │    │  应用子网   │    │  应用子网   │    │
│  │ 10.0.1.0/24 │    │ 10.1.1.0/24 │    │ 10.2.1.0/24 │    │
│  └─────────────┘    └─────────────┘    └─────────────┘    │
│         │                    │                    │        │
│         ▼                    ▼                    ▼        │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    │
│  │  数据子网   │    │  数据子网   │    │  数据子网   │    │
│  │ 10.0.2.0/24 │    │ 10.1.2.0/24 │    │ 10.2.2.0/24 │    │
│  └─────────────┘    └─────────────┘    └─────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 3. 部署工具链设计

### 3.1 基础设施即代码（IaC）
```hcl
# Terraform 多Provider配置
terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    alicloud = {
      source  = "aliyun/alicloud"
      version = "~> 1.209.0"
    }
    tencentcloud = {
      source  = "tencentcloudstack/tencentcloud"
      version = "~> 1.81.0"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# 多环境配置
locals {
  environments = {
    dev = {
      instance_type = "medium"
      instance_count = 2
      database_size = 100
    }
    staging = {
      instance_type = "large"
      instance_count = 4
      database_size = 500
    }
    prod = {
      instance_type = "xlarge"
      instance_count = 8
      database_size = 1000
    }
  }
}
```

### 3.2 容器编排部署
```yaml
# Kubernetes 多集群配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: multi-cloud-config
data:
  # 集群配置
  clusters: |
    - name: aliyun-beijing
      provider: aliyun
      region: cn-beijing
      endpoint: https://cs.cn-beijing.aliyuncs.com
    - name: tencent-shanghai
      provider: tencent
      region: ap-shanghai
      endpoint: https://tke.ap-shanghai.tencentcloudapi.com
    - name: aws-tokyo
      provider: aws
      region: ap-northeast-1
      endpoint: https://eks.ap-northeast-1.amazonaws.com
  
  # 部署策略
  deployment_strategy: |
    - name: canary-deployment
      steps:
        - weight: 10
          clusters: [aliyun-beijing]
        - weight: 30
          clusters: [aliyun-beijing, tencent-shanghai]
        - weight: 60
          clusters: [aliyun-beijing, tencent-shanghai, aws-tokyo]
        - weight: 100
          all_clusters: true
```

### 3.3 CI/CD流水线
```yaml
# GitLab CI/CD 多环境部署
stages:
  - build
  - test
  - security_scan
  - deploy_dev
  - deploy_staging
  - deploy_prod

variables:
  DOCKER_REGISTRY: "registry.cn-beijing.aliyuncs.com"
  TERRAFORM_VERSION: "1.5.0"
  KUBECTL_VERSION: "1.28.0"

build:
  stage: build
  script:
    - docker build -t $DOCKER_REGISTRY/erp:$CI_COMMIT_SHA .
    - docker push $DOCKER_REGISTRY/erp:$CI_COMMIT_SHA

deploy_to_aliyun:
  stage: deploy_dev
  environment: dev/aliyun
  script:
    - echo "Deploying to Aliyun Beijing..."
    - terraform init -backend-config="backend-aliyun.hcl"
    - terraform apply -auto-approve -var="environment=dev"
    - kubectl set image deployment/erp erp=$DOCKER_REGISTRY/erp:$CI_COMMIT_SHA

deploy_to_tencent:
  stage: deploy_staging
  environment: staging/tencent
  script:
    - echo "Deploying to Tencent Shanghai..."
    - terraform init -backend-config="backend-tencent.hcl"
    - terraform apply -auto-approve -var="environment=staging"
    - kubectl set image deployment/erp erp=$DOCKER_REGISTRY/erp:$CI_COMMIT_SHA

deploy_to_all:
  stage: deploy_prod
  environment: production
  script:
    - echo "Deploying to all clouds..."
    - ./scripts/deploy-multi-cloud.sh $CI_COMMIT_SHA
```

## 4. 配置管理方案

### 4.1 统一配置中心
```yaml
# Spring Cloud Config 多环境配置
spring:
  cloud:
    config:
      server:
        git:
          uri: https://gitlab.com/erp/config-repo.git
          search-paths: '{application}/{profile}'
          
  profiles:
    active: ${DEPLOY_ENV:dev}

# 多环境配置示例
---
spring:
  config:
    activate:
      on-profile: dev-aliyun
  
  datasource:
    url: jdbc:mysql://rm-uf6xxxx.mysql.rds.aliyuncs.com:3306/erp_dev
    username: ${DB_USER_DEV}
    password: ${DB_PASSWORD_DEV}

---
spring:
  config:
    activate:
      on-profile: prod-aliyun
  
  datasource:
    url: jdbc:mysql://rm-uf6xxxx.mysql.rds.aliyuncs.com:3306/erp_prod
    username: ${DB_USER_PROD}
    password: ${DB_PASSWORD_PROD}

---
spring:
  config:
    activate:
      on-profile: prod-tencent
  
  datasource:
    url: jdbc:mysql://cdb-xxxxxx.tencentcdb.com:3306/erp_prod
    username: ${DB_USER_PROD_TENCENT}
    password: ${DB_PASSWORD_PROD_TENCENT}
```

### 4.2 密钥管理
```yaml
# HashiCorp Vault 多云配置
vault:
  # 主Vault集群（阿里云）
  primary:
    address: "https://vault.aliyun.internal:8200"
    namespace: "erp"
    auth:
      method: "kubernetes"
      role: "erp-app"
  
  # 备Vault集群（腾讯云）
  secondary:
    address: "https://vault.tencent.internal:8200"
    namespace: "erp"
    auth:
      method: "kubernetes"
      role: "erp-app"
  
  # 密钥路径
  secrets:
    database:
      path: "database/creds/erp-role"
      ttl: "1h"
    api_keys:
      path: "kv/erp/api-keys"
    certificates:
      path: "pki/issue/erp-dot-com"
```

## 5. 部署验证与回滚

### 5.1 部署验证清单
```yaml
deployment_verification:
  pre_deployment:
    - check: "数据库连接测试"
      command: "mysql -h ${DB_HOST} -u ${DB_USER} -p${DB_PASSWORD} -e 'SELECT 1'"
      expected: "1"
    
    - check: "Redis连接测试"
      command: "redis-cli -h ${REDIS_HOST} ping"
      expected: "PONG"
    
    - check: "服务健康检查"
      command: "curl -f http://localhost:8080/health"
      expected_status: 200
  
  post_deployment:
    - check: "应用启动验证"
      command: "kubectl get pods -l app=erp -o jsonpath='{.items[*].status.phase}'"
      expected: "Running Running Running"
    
    - check: "服务端点验证"
      command: "curl -f http://erp-service/health"
      expected_status: 200
    
    - check: "业务功能验证"
      command: "./scripts/verify-business-flows.sh"
      timeout: 300
```

### 5.2 自动回滚机制
```python
# 自动回滚脚本
import requests
import json
import time
from kubernetes import client, config

class AutoRollback:
    def __init__(self):
        config.load_kube_config()
        self.apps_v1 = client.AppsV1Api()
        
    def check_deployment_health(self, deployment_name, namespace="default"):
        """检查部署健康状态"""
        deployment = self.apps_v1.read_namespaced_deployment(
            name=deployment_name,
            namespace=namespace
        )
        
        # 检查副本数
        desired = deployment.spec.replicas
        available = deployment.status.available_replicas or 0
        
        if available < desired * 0.8:  # 可用副本少于80%
            return False, f"Available replicas ({available}) less than 80% of desired ({desired})"
        
        # 检查Pod状态
        v1 = client.CoreV1Api()
        pods = v1.list_namespaced_pod(
            namespace=namespace,
            label_selector=f"app={deployment_name}"
        )
        
        error_pods = []
        for pod in pods.items:
            if pod.status.phase != "Running":
                error_pods.append(pod.metadata.name)
        
        if error_pods:
            return False, f"Pods not running: {error_pods}"
        
        return True, "Deployment healthy"
    
    def rollback_deployment(self, deployment_name, namespace="default"):
        """执行回滚"""
        print(f"Starting rollback for {deployment_name}...")
        
        # 获取部署历史
        deployment = self.apps_v1.read_namespaced_deployment(
            name=deployment_name,
            namespace=namespace
        )
        
        # 回滚到上一个版本
        self.apps_v1.patch_namespaced_deployment(
            name=deployment_name,
            namespace=namespace,
            body={
                "spec": {
                    "template": {
                        "metadata": {
                            "annotations": {
                                "rollback-to": "previous-version"
                            }
                        }
                    }
                }
            }
        )
        
        print(f"Rollback initiated for {deployment_name}")
```

## 6. 实施计划

### 6.1 阶段实施计划
| 阶段 | 时间线 | 目标 | 关键交付物 |
|------|--------|------|------------|
| **阶段1：基础架构搭建** | 第1-2周 | 建立多VPC网络互联，配置基础安全组 | 1. 多VPC互联架构<br>2. 安全策略配置<br>3. 基础监控配置 |
| **阶段2：容器化部署** | 第3-4周 | 搭建Kubernetes集群，部署基础服务 | 1. 多集群K8s环境<br>2. 容器镜像仓库<br>3. CI/CD流水线 |
| **阶段3：配置管理** | 第5-6周 | 建立统一配置中心，实现密钥管理 | 1. 配置中心部署<br>2. 密钥管理方案<br>3. 配置版本控制 |
| **阶段4：应用迁移** | 第7-8周 | 迁移ERP应用到多云环境 | 1. 应用容器化<br>2. 数据迁移方案<br>3. 流量切换策略 |
| **阶段5：优化与监控** | 第9-10周 | 优化性能，建立完整监控体系 | 1. 性能优化报告<br>2. 监控告警配置<br>3. 运维自动化脚本 |

### 6.2 风险评估与应对
| 风险项 | 影响程度 | 概率 | 应对措施 |
|--------|----------|------|----------|
| 云服务商API变更 | 高 | 中 | 1. 使用Terraform抽象层<br>2. 定期更新Provider版本<br>3. 建立API变更监控 |
| 网络延迟问题 | 高 | 高 | 1. 部署CDN加速<br>2. 数据本地化存储<br>3. 异步数据同步 |
| 成本超支 | 中 | 高 | 1. 建立成本监控<br>2. 使用自动伸缩<br>3. 优化资源利用率 |
| 安全合规风险 | 高 | 中 | 1. 定期安全审计<br>2. 合规性检查工具<br>3. 数据加密传输 |

## 7. 运维指南

### 7.1 日常运维操作
```bash
# 查看多集群状态
./scripts/cluster-status.sh

# 部署应用到所有集群
./scripts/deploy-all.sh <version>

# 回滚部署
./scripts/rollback.sh <deployment> <version>

# 成本分析报告
./scripts/cost-report.sh --month $(date +%Y-%m)

# 安全合规检查
./scripts/security-audit.sh
```

### 7.2 故障处理流程
1. **故障检测**：监控系统告警
2. **影响评估**：确定影响范围和严重程度
3. **故障定位**：使用链路追踪和日志分析
4. **应急处理**：执行应急预案
5. **恢复操作**：修复问题或切换流量
6. **事后分析**：编写故障报告，优化系统

---

**文档版本**: 1.0  
**最后更新**: 2026-05-04  
**作者**: 运维工程师  
**审核状态**: ✅ 已完成