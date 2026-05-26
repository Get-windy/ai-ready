# 基础设施即代码（IaC）设计

## 1. 设计概述

### 1.1 设计目标
- **自动化**：实现测试环境基础设施的完全自动化部署
- **一致性**：确保不同环境部署的一致性
- **可重复性**：支持环境的快速重建和复制
- **版本控制**：所有基础设施配置纳入版本管理
- **安全性**：内置安全最佳实践和合规检查

### 1.2 技术栈选择
- **编排工具**: Terraform (多云支持)
- **配置管理**: Ansible (应用层配置)
- **镜像构建**: Packer (标准化镜像)
- **容器编排**: Kubernetes + Helm (应用部署)
- **状态管理**: Terraform Cloud/Enterprise (协作和状态管理)

## 2. Terraform架构设计

### 2.1 模块化设计
```
terraform/
├── modules/                          # 可重用模块
│   ├── networking/                   # 网络模块
│   │   ├── vpc/                      # VPC模块
│   │   ├── subnet/                   # 子网模块
│   │   ├── security-group/           # 安全组模块
│   │   └── route-table/              # 路由表模块
│   ├── compute/                      # 计算模块
│   │   ├── ec2-instance/             # EC2实例模块
│   │   ├── autoscaling-group/        # 自动伸缩组模块
│   │   └── load-balancer/            # 负载均衡器模块
│   ├── database/                     # 数据库模块
│   │   ├── rds/                      # RDS数据库模块
│   │   ├── redis/                    # Redis缓存模块
│   │   └── elasticache/              # ElastiCache模块
│   ├── storage/                      # 存储模块
│   │   ├── s3-bucket/                # S3存储桶模块
│   │   ├── ebs-volume/               # EBS卷模块
│   │   └── efs/                      # EFS文件系统模块
│   └── monitoring/                   # 监控模块
│       ├── cloudwatch/               # CloudWatch监控模块
│       ├── sns/                      # SNS通知模块
│       └── lambda/                   # Lambda函数模块
├── environments/                     # 环境配置
│   ├── development/                  # 开发环境
│   │   ├── main.tf                   # 主配置文件
│   │   ├── variables.tf              # 变量定义
│   │   ├── terraform.tfvars          # 变量值文件
│   │   └── backend.tf                # 后端配置
│   ├── testing/                      # 测试环境
│   │   ├── main.tf                   # 主配置文件
│   │   ├── variables.tf              # 变量定义
│   │   ├── terraform.tfvars          # 变量值文件
│   │   └── backend.tf                # 后端配置
│   ├── staging/                      # 预发布环境
│   │   ├── main.tf                   # 主配置文件
│   │   ├── variables.tf              # 变量定义
│   │   ├── terraform.tfvars          # 变量值文件
│   │   └── backend.tf                # 后端配置
│   └── production/                   # 生产环境
│       ├── main.tf                   # 主配置文件
│       ├── variables.tf              # 变量定义
│       ├── terraform.tfvars          # 变量值文件
│       └── backend.tf                # 后端配置
├── providers/                        # 云提供商配置
│   ├── aws/                          # AWS配置
│   ├── azure/                        # Azure配置
│   ├── gcp/                          # GCP配置
│   └── aliyun/                       # 阿里云配置
└── shared/                           # 共享配置
    ├── terraform.tfvars.example      # 变量示例文件
    ├── backend.hcl.example           # 后端配置示例
    └── providers.tf                  # 提供商通用配置
```

### 2.2 VPC模块设计
```hcl
# modules/networking/vpc/main.tf
module "vpc" {
  source = "../../modules/networking/vpc"

  # 基础配置
  name               = var.vpc_name
  cidr_block         = var.vpc_cidr_block
  enable_dns_support = var.enable_dns_support
  
  # 可用区配置
  availability_zones = var.availability_zones
  
  # 子网配置
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
  
  # 标签
  tags = merge(var.tags, {
    Environment = var.environment
    ManagedBy   = "Terraform"
  })
}

# 输出定义
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "公有子网ID列表"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "私有子网ID列表"
  value       = aws_subnet.private[*].id
}

output "database_subnet_ids" {
  description = "数据库子网ID列表"
  value       = aws_subnet.database[*].id
}
```

### 2.3 安全组模块设计
```hcl
# modules/networking/security-group/main.tf
resource "aws_security_group" "main" {
  name        = "${var.name}-${var.environment}"
  description = var.description
  vpc_id      = var.vpc_id
  
  # 动态规则定义
  dynamic "ingress" {
    for_each = var.ingress_rules
    content {
      description = ingress.value.description
      from_port   = ingress.value.from_port
      to_port     = ingress.value.to_port
      protocol    = ingress.value.protocol
      cidr_blocks = ingress.value.cidr_blocks
      security_groups = ingress.value.security_groups
    }
  }
  
  dynamic "egress" {
    for_each = var.egress_rules
    content {
      description = egress.value.description
      from_port   = egress.value.from_port
      to_port     = egress.value.to_port
      protocol    = egress.value.protocol
      cidr_blocks = egress.value.cidr_blocks
      security_groups = egress.value.security_groups
    }
  }
  
  tags = merge(var.tags, {
    Environment = var.environment
    ManagedBy   = "Terraform"
  })
}

# 预定义的安全组规则
locals {
  # Web服务器安全组规则
  web_server_rules = {
    http = {
      description = "HTTP access"
      from_port   = 80
      to_port     = 80
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
    https = {
      description = "HTTPS access"
      from_port   = 443
      to_port     = 443
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
    ssh = {
      description = "SSH access"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = ["10.0.0.0/8"]  # 仅内部网络
    }
  }
  
  # 数据库安全组规则
  database_rules = {
    mysql = {
      description = "MySQL database access"
      from_port   = 3306
      to_port     = 3306
      protocol    = "tcp"
      cidr_blocks = ["10.0.0.0/8"]  # 仅内部网络
    }
    redis = {
      description = "Redis access"
      from_port   = 6379
      to_port     = 6379
      protocol    = "tcp"
      cidr_blocks = ["10.0.0.0/8"]  # 仅内部网络
    }
  }
}
```

## 3. 环境配置管理

### 3.1 环境变量管理
```hcl
# environments/testing/variables.tf
variable "environment" {
  description = "环境名称"
  type        = string
  default     = "testing"
}

variable "region" {
  description = "AWS区域"
  type        = string
  default     = "ap-southeast-1"
}

variable "vpc_cidr_block" {
  description = "VPC CIDR块"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "可用区列表"
  type        = list(string)
  default     = ["ap-southeast-1a", "ap-southeast-1b"]
}

variable "instance_type" {
  description = "实例类型"
  type        = string
  default     = "t3.medium"
}

variable "database_instance_type" {
  description = "数据库实例类型"
  type        = string
  default     = "db.t3.small"
}

variable "enable_monitoring" {
  description = "是否启用监控"
  type        = bool
  default     = true
}

variable "tags" {
  description = "资源标签"
  type        = map(string)
  default = {
    Project     = "AI-Ready"
    Department  = "Engineering"
    CostCenter  = "12345"
    ManagedBy   = "Terraform"
  }
}
```

### 3.2 环境差异化配置
```hcl
# environments/testing/terraform.tfvars
# 测试环境特定配置
environment = "testing"
region      = "ap-southeast-1"

# 网络配置
vpc_cidr_block = "10.0.0.0/16"
availability_zones = ["ap-southeast-1a", "ap-southeast-1b"]

# 子网配置
public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs = ["10.0.11.0/24", "10.0.12.0/24"]
database_subnet_cidrs = ["10.0.21.0/24", "10.0.22.0/24"]

# 计算配置
instance_count = 2
instance_type  = "t3.medium"

# 数据库配置
database_instance_type = "db.t3.small"
database_storage_size  = 20
database_backup_retention = 7

# 监控配置
enable_cloudwatch = true
enable_vpc_flow_logs = true

# 成本控制
enable_cost_optimization = true
budget_limit = 500  # 每月500美元
```

## 4. 状态管理和协作

### 4.1 Terraform后端配置
```hcl
# environments/testing/backend.tf
terraform {
  backend "s3" {
    bucket         = "ai-ready-terraform-state"
    key            = "testing/terraform.tfstate"
    region         = "ap-southeast-1"
    encrypt        = true
    dynamodb_table = "ai-ready-terraform-locks"
    
    # S3状态版本控制
    versioning_config {
      enabled = true
    }
  }
  
  # Terraform版本约束
  required_version = ">= 1.3.0"
  
  # 提供商版本约束
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.0"
    }
  }
}
```

### 4.2 状态锁定机制
```hcl
# 创建DynamoDB表用于状态锁
resource "aws_dynamodb_table" "terraform_locks" {
  name         = "ai-ready-terraform-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"
  
  attribute {
    name = "LockID"
    type = "S"
  }
  
  tags = {
    Name        = "Terraform State Locks"
    Environment = "global"
    ManagedBy   = "Terraform"
  }
}

# S3状态桶配置
resource "aws_s3_bucket" "terraform_state" {
  bucket = "ai-ready-terraform-state"
  
  # 版本控制
  versioning {
    enabled = true
  }
  
  # 服务器端加密
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
  
  # 生命周期规则
  lifecycle_rule {
    id      = "state-versions"
    enabled = true
    
    # 保留90天的状态版本
    expiration {
      days = 90
    }
    
    # 30天后转换为标准IA存储
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
    
    # 60天后转换为Glacier存储
    transition {
      days          = 60
      storage_class = "GLACIER"
    }
  }
  
  # 公共访问块
  public_access_block {
    block_public_acls       = true
    block_public_policy     = true
    ignore_public_acls      = true
    restrict_public_buckets = true
  }
  
  tags = {
    Name        = "Terraform State Bucket"
    Environment = "global"
    ManagedBy   = "Terraform"
  }
}
```

## 5. 安全设计

### 5.1 IAM角色和策略
```hcl
# IAM角色用于Terraform执行
resource "aws_iam_role" "terraform_execution" {
  name = "TerraformExecutionRole"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.account_id}:root"
        }
        Condition = {
          StringEquals = {
            "aws:PrincipalTag/Environment" = var.environment
          }
        }
      }
    ]
  })
  
  tags = {
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# IAM策略附加
resource "aws_iam_role_policy_attachment" "terraform_execution" {
  role       = aws_iam_role.terraform_execution.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
  
  # 生产环境使用更严格的策略
  count = var.environment == "production" ? 0 : 1
}

# 生产环境特定策略
resource "aws_iam_policy" "production_terraform_policy" {
  name        = "ProductionTerraformPolicy"
  description = "生产环境Terraform执行策略"
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:*",
          "s3:*",
          "rds:*",
          "elasticache:*",
          "iam:*",
          "cloudwatch:*",
          "logs:*"
        ]
        Resource = "*"
      },
      {
        Effect = "Deny"
        Action = [
          "iam:Delete*",
          "s3:DeleteBucket",
          "rds:DeleteDBInstance",
          "ec2:TerminateInstances"
        ]
        Resource = "*"
      }
    ]
  })
  
  count = var.environment == "production" ? 1 : 0
}
```

### 5.2 密钥管理
```hcl
# AWS Secrets Manager用于存储敏感信息
resource "aws_secretsmanager_secret" "database_credentials" {
  name = "${var.environment}/database/credentials"
  
  description = "数据库连接凭据"
  
  # 自动轮换配置
  rotation_rules {
    automatically_after_days = 30
  }
  
  tags = {
    Environment = var.environment
    Type       = "credentials"
    ManagedBy  = "Terraform"
  }
}

# 数据库密码
resource "aws_secretsmanager_secret_version" "database_credentials" {
  secret_id = aws_secretsmanager_secret.database_credentials.id
  
  secret_string = jsonencode({
    username = var.database_username
    password = random_password.database_password.result
    host     = aws_db_instance.main.endpoint
    port     = 3306
    database = var.database_name
  })
}

# 生成随机密码
resource "random_password" "database_password" {
  length           = 16
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}
```

## 6. 部署流程

### 6.1 自动化部署脚本
```bash
#!/bin/bash
# deploy-infrastructure.sh

set -e

# 环境变量
ENVIRONMENT=${1:-testing}
REGION=${2:-ap-southeast-1}
ACTION=${3:-apply}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境
check_environment() {
    log_info "检查环境配置..."
    
    # 检查Terraform版本
    if ! command -v terraform &> /dev/null; then
        log_error "Terraform未安装"
        exit 1
    fi
    
    # 检查AWS CLI
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI未安装"
        exit 1
    fi
    
    # 检查环境目录
    if [ ! -d "environments/${ENVIRONMENT}" ]; then
        log_error "环境目录不存在: environments/${ENVIRONMENT}"
        exit 1
    fi
    
    log_info "环境检查通过"
}

# 初始化Terraform
init_terraform() {
    log_info "初始化Terraform..."
    
    cd "environments/${ENVIRONMENT}"
    
    # 初始化
    terraform init \
        -backend-config="region=${REGION}" \
        -reconfigure
    
    # 验证配置
    terraform validate
    
    log_info "Terraform初始化完成"
}

# 计划变更
plan_changes() {
    log_info "计划基础设施变更..."
    
    # 生成计划文件
    terraform plan \
        -var-file="terraform.tfvars" \
        -out="tfplan-$(date +%Y%m%d-%H%M%S).tfplan"
    
    log_info "变更计划已生成"
}

# 应用变更
apply_changes() {
    log_info "应用基础设施变更..."
    
    # 查找最新的计划文件
    LATEST_PLAN=$(ls -t tfplan-*.tfplan 2>/dev/null | head -1)
    
    if [ -z "$LATEST_PLAN" ]; then
        log_warn "未找到计划文件，重新生成..."
        plan_changes
        LATEST_PLAN=$(ls -t tfplan-*.tfplan | head -1)
    fi
    
    # 应用变更
    terraform apply "${LATEST_PLAN}"
    
    log_info "基础设施变更已应用"
}

# 销毁环境
destroy_environment() {
    log_warn "即将销毁环境: ${ENVIRONMENT}"
    read -p "确认要销毁吗？(yes/no): " CONFIRM
    
    if [ "$CONFIRM" != "yes" ]; then
        log_info "操作已取消"
        exit 0
    fi
    
    log_info "销毁环境..."
    
    terraform destroy \
        -var-file="terraform.tfvars" \
        -auto-approve
    
    log_info "环境已销毁"
}

# 输出信息
output_info() {
    log_info "输出基础设施信息..."
    
    terraform output -json > outputs.json
    
    log_info "输出信息已保存到 outputs.json"
}

# 主函数
main() {
    log_info "开始部署基础设施 - 环境: ${ENVIRONMENT}, 区域: ${REGION}"
    
    # 检查环境
    check_environment
    
    # 初始化
    init_terraform
    
    case $ACTION in
        "plan")
            plan_changes
            ;;
        "apply")
            apply_changes
            output_info
            ;;
        "destroy")
            destroy_environment
            ;;
        "refresh")
            terraform refresh -var-file="terraform.tfvars"
            log_info "状态已刷新"
            ;;
        *)
            log_error "未知操作: ${ACTION}"
            log_info "可用操作: plan, apply, destroy, refresh"
            exit 1
            ;;
    esac
    
    log_info "基础设施部署完成"
}

# 执行主函数
main "$@"
```

### 6.2 CI/CD集成
```yaml
# .gitlab-ci.yml
stages:
  - validate
  - plan
  - apply
  - test

variables:
  TERRAFORM_VERSION: "1.3.0"
  AWS_DEFAULT_REGION: "ap-southeast-1"

before_script:
  - apt-get update && apt-get install -y unzip curl
  - curl -sSL https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip -o terraform.zip
  - unzip terraform.zip
  - chmod +x terraform
  - mv terraform /usr/local/bin/

validate:
  stage: validate
  script:
    - cd environments/${ENVIRONMENT}
    - terraform init -backend=false
    - terraform validate
  only:
    - merge_requests

plan:
  stage: plan
  script:
    - cd environments/${ENVIRONMENT}
    - terraform init
    - terraform plan -out=tfplan
  artifacts:
    paths:
      - environments/${ENVIRONMENT}/tfplan
  only:
    - merge_requests

apply:
  stage: apply
  script:
    - cd environments/${ENVIRONMENT}
    - terraform init
    - terraform apply -auto-approve tfplan
  environment:
    name: $ENVIRONMENT
  when: manual
  only:
    - main
    - master

test:
  stage: test
  script:
    - echo "测试基础设施..."
    # 这里可以添加基础设施测试脚本
    - ./scripts/test-infrastructure.sh
  needs: ["apply"]
```

## 7. 监控和审计

### 7.1 变更审计
```hcl
# CloudTrail配置用于审计
resource "aws_cloudtrail" "infrastructure_changes" {
  name                          = "infrastructure-changes-trail"
  s3_bucket_name                = aws_s3_bucket.cloudtrail_logs.id
  include_global_service_events = true
  is_multi_region_trail         = true
  enable_log_file_validation    = true
  
  # 记录所有管理事件
  event_selector {
    read_write_type           = "All"
    include_management_events = true
    
    data_resource {
      type   = "AWS::S3::Object"
      values = ["arn:aws:s3:::${aws_s3_bucket.terraform_state.bucket}/"]
    }
  }
  
  tags = {
    Environment = "global"
    Purpose    = "Audit"
    ManagedBy  = "Terraform"
  }
}

# S3桶用于存储CloudTrail日志
resource "aws_s3_bucket" "cloudtrail_logs" {
  bucket = "ai-ready-cloudtrail-logs"
  
  # 生命周期规则
  lifecycle_rule {
    id      = "cloudtrail-logs"
    enabled = true
    
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
    
    transition {
      days          = 90
      storage_class = "GLACIER"
    }
    
    expiration {
      days = 365
    }
  }
  
  # 服务器端加密
  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
  
  tags = {
    Environment = "global"
    Purpose    = "Audit"
    ManagedBy  = "Terraform"
  }
}
```

### 7.2 成本监控
```hcl
# AWS Cost Explorer预算
resource "aws_budgets_budget" "infrastructure" {
  name              = "infrastructure-monthly-budget"
  budget_type       = "COST"
  limit_amount      = var.budget_limit
  limit_unit        = "USD"
  time_unit         = "MONTHLY"
  
  # 通知配置
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = var.budget_notification_emails
  }
  
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = var.budget_notification_emails
  }
  
  tags = {
    Environment = var.environment
    ManagedBy  = "Terraform"
  }
}
```

## 8. 最佳实践

### 8.1 代码组织最佳实践
1. **模块化设计**：将相关资源组织到模块中
2. **环境隔离**：不同环境使用独立配置
3. **变量管理**：敏感信息使用变量和密钥管理
4. **状态管理**：使用远程后端和状态锁定

### 8.2 安全最佳实践
1. **最小权限原则**：IAM角色只授予必要权限
2. **密钥管理**：敏感信息使用密钥管理服务
3. **网络隔离**：使用私有子网和安全组
4. **审计跟踪**：启用CloudTrail记录所有操作

### 8.3 运维最佳实践
1. **变更管理**：所有变更通过代码和评审流程
2. **备份策略**：定期备份状态和重要数据
3. **监控告警**：设置资源使用和成本告警
4. **文档维护**：保持文档与代码同步更新

---

**版本**：v1.0  
**更新日期**：2026-05-05  
**负责人**：test-agent-1  
**审核人**：main