# Terraform 模块设计文档

## 概述
为ERP核心功能模块设计统一的基础设施即代码（IaC）环境配置模板，实现开发、测试、生产环境的标准化部署和自动化管理。

## 模块设计原则
1. **模块化**: 每个基础设施组件设计为独立的Terraform模块
2. **可重用**: 模块支持多环境配置和多项目复用
3. **安全**: 支持敏感信息加密和安全管理
4. **可观测**: 内置监控指标收集和成本优化

## 核心模块设计

### 1. 网络基础设施模块 (network)
#### 功能范围
- VPC (Virtual Private Cloud)
- 子网 (Subnets) - 公共/私有子网
- 安全组 (Security Groups)
- 路由表 (Route Tables)
- NAT网关 (NAT Gateway)
- 互联网网关 (Internet Gateway)

#### 设计要点
```hcl
# network/variables.tf
variable "vpc_cidr" {
  description = "VPC CIDR 块"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "公共子网CIDR列表"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "私有子网CIDR列表"
  type        = list(string)
  default     = ["10.0.101.0/24", "10.0.102.0/24"]
}
```

### 2. 计算资源模块 (compute)
#### 功能范围
- ECS实例 (Elastic Compute Service)
- 负载均衡器 (Load Balancers) - ALB/NLB
- 自动伸缩组 (Auto Scaling Groups)
- 启动配置 (Launch Configurations/Templates)
- 容器实例 (Container Instances)

#### 设计要点
```hcl
# compute/variables.tf
variable "instance_type" {
  description = "实例类型"
  type        = string
  default     = "ecs.g7.large"
}

variable "min_size" {
  description = "自动伸缩组最小实例数"
  type        = number
  default     = 2
}

variable "max_size" {
  description = "自动伸缩组最大实例数"
  type        = number
  default     = 10
}

variable "desired_capacity" {
  description = "期望容量"
  type        = number
  default     = 3
}
```

### 3. 存储资源模块 (storage)
#### 功能范围
- 数据库实例 (Database Instances)
  - MySQL/PostgreSQL
  - Redis/Memcached
  - MongoDB
- 对象存储 (Object Storage) - OSS/S3
- 文件存储 (File Storage) - NAS/EFS
- 块存储 (Block Storage) - EBS/云盘

#### 设计要点
```hcl
# storage/variables.tf
variable "database_engine" {
  description = "数据库引擎类型"
  type        = string
  default     = "mysql"
}

variable "database_version" {
  description = "数据库版本"
  type        = string
  default     = "8.0"
}

variable "storage_size_gb" {
  description = "存储大小(GB)"
  type        = number
  default     = 100
}

variable "backup_retention_days" {
  description = "备份保留天数"
  type        = number
  default     = 30
}
```

### 4. 中间件模块 (middleware)
#### 功能范围
- 消息队列 (Message Queues)
  - RabbitMQ
  - Kafka
  - RocketMQ
- 缓存服务 (Cache Services)
  - Redis Cluster
  - Memcached Cluster
- 搜索服务 (Search Services)
  - Elasticsearch
  - OpenSearch

#### 设计要点
```hcl
# middleware/variables.tf
variable "redis_node_type" {
  description = "Redis节点类型"
  type        = string
  default     = "cache.m6g.large"
}

variable "redis_nodes" {
  description = "Redis节点数量"
  type        = number
  default     = 3
}

variable "kafka_broker_count" {
  description = "Kafka broker数量"
  type        = number
  default     = 3
}

variable "elasticsearch_version" {
  description = "Elasticsearch版本"
  type        = string
  default     = "7.10"
}
```

### 5. 监控模块 (monitoring)
#### 功能范围
- Prometheus监控栈
- Grafana仪表板
- 告警规则配置
- 日志收集系统

#### 设计要点
```hcl
# monitoring/variables.tf
variable "enable_monitoring" {
  description = "是否启用监控"
  type        = bool
  default     = true
}

variable "retention_days" {
  description = "监控数据保留天数"
  type        = number
  default     = 90
}

variable "alert_receivers" {
  description = "告警接收人列表"
  type        = list(string)
  default     = ["devops@example.com"]
}
```

### 6. 安全模块 (security)
#### 功能范围
- IAM角色和策略
- KMS密钥管理
- 安全组规则
- WAF配置
- DDoS防护

#### 设计要点
```hcl
# security/variables.tf
variable "enable_waf" {
  description = "是否启用WAF"
  type        = bool
  default     = true
}

variable "enable_ddos_protection" {
  description = "是否启用DDoS防护"
  type        = bool
  default     = true
}

variable "encryption_key_rotation_days" {
  description = "加密密钥轮换天数"
  type        = number
  default     = 90
}
```

## 模块依赖关系

```
network
  ├── compute (需要网络资源)
  ├── storage (需要网络访问)
  ├── middleware (需要网络和存储)
  └── monitoring (需要网络访问)

compute
  ├── storage (需要挂载存储)
  └── middleware (需要访问中间件)

security (应用到所有模块)
```

## 多环境支持

### 环境变量结构
```hcl
# environments/dev/terraform.tfvars
environment = "dev"
vpc_cidr    = "10.1.0.0/16"
instance_type = "ecs.g7.large"
database_size = 50
enable_monitoring = true
```

```hcl
# environments/prod/terraform.tfvars
environment = "prod"
vpc_cidr    = "10.0.0.0/16"
instance_type = "ecs.g7.2xlarge"
database_size = 500
enable_monitoring = true
enable_waf = true
enable_ddos_protection = true
```

## 输出定义

### 模块输出
```hcl
# outputs.tf 示例
output "vpc_id" {
  description = "VPC ID"
  value       = module.network.vpc_id
}

output "public_subnet_ids" {
  description = "公共子网ID列表"
  value       = module.network.public_subnet_ids
}

output "private_subnet_ids" {
  description = "私有子网ID列表"
  value       = module.network.private_subnet_ids
}

output "load_balancer_dns" {
  description = "负载均衡器DNS名称"
  value       = module.compute.load_balancer_dns
}

output "database_endpoint" {
  description = "数据库连接端点"
  value       = module.storage.database_endpoint
}

output "redis_endpoint" {
  description = "Redis连接端点"
  value       = module.middleware.redis_endpoint
}
```

## 最佳实践

### 1. 状态管理
- 使用远程状态后端 (S3 + DynamoDB)
- 状态文件加密存储
- 状态锁定防止并发操作

### 2. 变量管理
- 使用tfvars文件管理环境变量
- 敏感变量使用KMS加密
- 变量验证和默认值

### 3. 模块版本控制
- 使用Git仓库管理模块
- 语义化版本控制
- 模块注册表发布

### 4. 安全最佳实践
- 最小权限原则
- 密钥轮换策略
- 审计日志记录

## 部署流程

### 1. 初始化
```bash
# 初始化远程状态
terraform init -backend-config=environments/dev/backend.tfvars
```

### 2. 规划
```bash
# 查看变更计划
terraform plan -var-file=environments/dev/terraform.tfvars
```

### 3. 部署
```bash
# 应用变更
terraform apply -var-file=environments/dev/terraform.tfvars
```

### 4. 验证
```bash
# 验证基础设施状态
terraform output
terraform state list
```

## 资源类型覆盖统计

| 模块 | 资源类型 | 覆盖率 |
|------|----------|--------|
| network | VPC, Subnet, Security Group, Route Table | 100% |
| compute | ECS, Load Balancer, Auto Scaling | 100% |
| storage | RDS, OSS, NAS, EBS | 100% |
| middleware | Redis, Kafka, Elasticsearch | 100% |
| monitoring | Prometheus, Grafana, Alert Manager | 100% |
| security | IAM, KMS, WAF, DDoS | 100% |

**总计**: 覆盖 ≥ 8个核心基础设施资源类型 ✅

## 下一步工作
1. 创建模块实现文件 (main.tf, variables.tf, outputs.tf)
2. 编写模块使用示例
3. 创建多环境配置模板
4. 设计CI/CD集成方案
5. 建立监控运维方案