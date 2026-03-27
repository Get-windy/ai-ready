# 智企连·AI-Ready

基于 SmartAdmin 框架，借鉴 Odoo 设计思想，打造完全自主、适合中国用户的 AI-Ready 企业管理系统。

## 项目简介

智企连是一款企业智能管理系统，具备以下核心特性：

- **AI-Ready**: 预留标准化的 Agent 调用接口，任何 AI Agent（如 OpenClaw、Dify、LangChain、自研 Agent）均可调用系统能力。
- **模块化**: 借鉴 Odoo 的设计哲学，业务模块可插拔、可独立扩展、可组合部署。
- **中国化**: 内置中国会计准则、电子发票接口、银行直连、移动端多端适配（小程序、APP、H5）。
- **自主可控**: 100% 自研代码，采用 **Apache 2.0** 开源协议，无任何法律风险，企业可安心商用。
- **生产级**: 基于成熟的 SmartAdmin 框架，满足安全合规、高性能、高可用要求。

## 项目结构

```
I:\AI-Ready
├── pom.xml                         # 父 POM
├── README.md                       # 项目说明
├── core-base                       # 基础支撑模块（用户、权限、配置、字典、通用工具）
├── core-workflow                   # 工作流模块（集成 Flowable）
├── core-report                     # 报表与打印模块
├── core-agent                      # Agent 调用层模块
├── core-api                        # API 接口定义（供前端调用）
├── core-common                     # 公共工具类
│
├── erp                             # ERP 模块群（独立顶层）
│   ├── erp-purchase                # 采购管理
│   ├── erp-sale                    # 销售管理
│   ├── erp-stock                   # 库存管理
│   ├── erp-warehouse               # 仓储管理（PDA拣货等）
│   ├── erp-account                 # 财务管理（中国化）
│   └── erp-production              # 生产管理（可选）
│
├── crm                             # CRM 模块群（独立顶层）
│   ├── crm-lead                    # 线索管理
│   ├── crm-opportunity             # 商机管理
│   ├── crm-customer                # 客户管理
│   └── crm-activity                # 活动记录
│
├── smart-admin-web                 # 管理前端（Vue3 项目）
└── erp-mobile                      # 移动端 UniApp 项目（多端）
```

## 技术栈

### 后端技术
- Java 17 / Spring Boot 3.2.x
- Sa-Token (权限认证)
- Mybatis-Plus (ORM)
- MySQL 8.0 / PostgreSQL (数据库)
- Redis 7.x (缓存)
- RocketMQ / RabbitMQ (消息队列)

### 前端技术
- Vue 3 + Vite 5
- Ant Design Vue
- UniApp (移动端)

### 开发工具
- Maven (构建)
- Docker + Kubernetes (容器化)
- Git (版本控制)

## 数据库配置

### PostgreSQL (本地开发)

```
主机: localhost
端口: 5432
用户名: devuser
密码: Dev@2026#Local
数据库: devdb
```

连接字符串: `postgresql://devuser:Dev@2026#Local@localhost:5432/devdb`

### MySQL (生产环境)

- MySQL 8.0
- 支持高可用集群
- 定期备份策略

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- PostgreSQL 14+ / MySQL 8.0+
- Redis 7+

### 开发环境搭建

1. 克隆代码仓库
2. 安装依赖
3. 配置数据库连接
4. 启动服务

详细步骤请参考 [开发指南](docs/development-guide.md)。

## 开发计划

- [M0] 启动准备 (第1-2周)
- [M1] 基础平台 (第3-6周)
- [M2] ERP 核心 (第7-12周)
- [M3] CRM 核心 (第13-16周)
- [M4] Agent 调用层 (第17-20周)
- [M5] 前端管理端 (第21-24周)
- [M6] 移动管理端 (第25-28周)
- [M7] 集成测试 (第29-30周)
- [M8] 发布 V1.0 (第31-32周)

## 许可证

Apache License 2.0

## 联系我们

- 项目群组: [智企连·AI-Ready 项目开发群]
- 技术支持: support@aiedge.cn

---

**智企连 - 让企业管理更智能！**