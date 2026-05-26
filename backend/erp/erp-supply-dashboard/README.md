# ERP供应链数据可视化仪表板

## 项目概述

ERP供应链数据可视化仪表板是为供应链管理模块设计的实时监控和分析平台，提供供应链关键指标的实时监控、深度分析和决策支持能力。

## 项目结构

```
erp-supply-dashboard/
├── src/main/java/cn/aiedge/erp/supply/dashboard/
│   ├── controller/           # 控制器层
│   │   └── SupplyDashboardController.java
│   ├── entity/              # 实体类
│   │   ├── MetricConfig.java
│   │   ├── MetricData.java
│   │   └── DashboardConfig.java
│   ├── repository/          # 数据仓库接口
│   │   ├── MetricConfigRepository.java
│   │   └── MetricDataRepository.java
│   ├── service/            # 服务层接口
│   │   └── MetricService.java
│   └── ErpSupplyDashboardApplication.java  # 启动类
├── src/main/resources/
│   └── application.yml     # 配置文件
├── docs/                   # 文档
│   └── supply-chain-dashboard-requirements.md
├── pom.xml                # Maven配置
└── README.md              # 项目说明
```

## 核心功能

### 1. 指标管理
- **指标配置管理**：支持定义、编辑、删除供应链指标
- **指标计算**：自动计算各类供应链指标值
- **指标监控**：实时监控指标状态和趋势

### 2. 仪表板管理
- **多角色视图**：为供应链总监、采购经理、物流主管提供定制化视图
- **可配置布局**：支持网格、弹性等多种布局方式
- **主题定制**：提供多种主题样式

### 3. 数据可视化
- **多种图表类型**：柱状图、折线图、饼图、雷达图、热力图等
- **实时数据更新**：支持定时刷新和手动刷新
- **数据钻取**：支持从汇总数据下钻到明细数据

### 4. 预警管理
- **多级预警**：正常、预警、紧急、异常四级状态
- **实时告警**：自动检测异常指标并发送告警
- **预警历史**：记录预警历史和分析

## 技术架构

### 后端技术栈
- **框架**：Spring Boot 3.2.5
- **数据库**：MySQL 8.0 + JPA/Hibernate
- **缓存**：Redis
- **安全**：Spring Security
- **API文档**：SpringDoc OpenAPI 3.0
- **构建工具**：Maven

### 前端技术栈（待开发）
- **框架**：Vue 3 + TypeScript
- **UI库**：Element Plus
- **可视化**：ECharts
- **状态管理**：Pinia
- **构建工具**：Vite

## 核心指标

### 库存管理指标
- 库存周转率
- 库存天数
- 安全库存水平
- 呆滞库存比例

### 采购管理指标
- 采购订单满足率
- 供应商准时交货率
- 采购成本节约率
- 供应商绩效评分

### 物流管理指标
- 订单配送准时率
- 运输成本占比
- 仓储利用率
- 平均配送时间

## API接口

### 健康检查
- `GET /api/supply-dashboard/health`

### 指标配置管理
- `GET /api/supply-dashboard/metrics/config/{metricCode}`
- `GET /api/supply-dashboard/metrics/category/{category}`

### 指标数据查询
- `GET /api/supply-dashboard/metrics/data/latest/{metricCode}`
- `GET /api/supply-dashboard/metrics/data/history/{metricCode}`
- `POST /api/supply-dashboard/metrics/calculate/{metricCode}`

### 仪表板概览
- `GET /api/supply-dashboard/dashboard/overview`

### 预警管理
- `GET /api/supply-dashboard/metrics/warnings`

## 部署说明

### 环境要求
- Java 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 部署步骤

1. **数据库准备**
   ```sql
   CREATE DATABASE erp_supply_dashboard CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **编译项目**
   ```bash
   mvn clean package -DskipTests
   ```

3. **运行应用**
   ```bash
   java -jar target/erp-supply-dashboard-1.0.0-SNAPSHOT.jar
   ```

4. **访问应用**
   - 应用地址：http://localhost:8085/supply-dashboard
   - API文档：http://localhost:8085/supply-dashboard/swagger-ui.html

## 开发指南

### 开发环境设置
1. 克隆项目
2. 导入IDE（推荐IntelliJ IDEA）
3. 配置数据库连接
4. 启动Redis服务
5. 运行启动类 `ErpSupplyDashboardApplication`

### 代码规范
- 遵循阿里巴巴Java开发规范
- 使用Lombok简化代码
- 使用MapStruct进行对象映射
- 日志使用SLF4J

### 测试
- 单元测试：JUnit 5
- 集成测试：Spring Boot Test
- API测试：Swagger UI

## 后续计划

### 第一阶段（已完成）
- [x] 项目架构设计
- [x] 基础框架搭建
- [x] 核心实体设计
- [x] API接口设计

### 第二阶段（进行中）
- [ ] 指标计算引擎实现
- [ ] 数据可视化服务
- [ ] 预警管理系统
- [ ] 缓存优化

### 第三阶段（待开始）
- [ ] 前端界面开发
- [ ] 权限管理系统
- [ ] 数据导出功能
- [ ] 性能优化

## 联系信息

- **项目负责人**：AI-Edge
- **创建日期**：2026-05-04
- **最后更新**：2026-05-04

## 许可证

本项目采用 MIT 许可证。详情请参阅 LICENSE 文件。