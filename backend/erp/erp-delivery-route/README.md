# ERP配送路线优化模块

## 概述

配送路线优化模块是企智连ERP配送系统的核心功能，集成高德地图API，支持多点路径规划、实时路况调整、导航功能。

## 功能特性

### 核心功能
- ✅ **高德API集成**：路线规划、地址解析、距离计算
- ✅ **多点路径规划**：支持多个配送点的最优路线规划
- ✅ **路线优化**：基于距离最短原则优化配送顺序
- ✅ **实时路况调整**：根据实时路况重新优化路线
- ✅ **导航功能**：生成导航链接，支持跳转高德导航

### 业务场景
- 配送员订单自动匹配分配
- 配送路线智能规划
- 实时路况响应调整

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **第三方API**：高德地图API
- **缓存**：Redis（API结果缓存）
- **API文档**：Knife4j

## API接口

### 配送路线 `/api/delivery/route`
- `POST /plan` - 规划配送路线
- `POST /{routeId}/optimize` - 优化配送路线
- `GET /{routeId}` - 获取路线详情
- `GET /active/{deliveryPersonId}` - 获取配送员当前路线
- `GET /list` - 路线列表查询
- `POST /{routeId}/start` - 开始配送
- `POST /{routeId}/complete` - 完成配送
- `POST /{routeId}/cancel` - 取消路线
- `POST /{routeId}/point/{pointOrder}/status` - 更新配送点状态
- `POST /navigation` - 获取导航信息
- `POST /{routeId}/reoptimize` - 根据实时路况重新优化

## 高德API配置

需要在配置文件中设置高德API Key：
```yaml
amap:
  key: your-amap-key
  base-url: https://restapi.amap.com/v3
```

或通过环境变量：
```bash
export AMAP_KEY=your-amap-key
```

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_delivery_route_tables.sql`

### 2. 高德API Key配置
申请高德开放平台API Key，配置到application.yml

### 3. 启动服务
```bash
mvn spring-boot:run
```

### 4. 访问地址
- 服务地址：http://localhost:8097
- API文档：http://localhost:8097/doc.html

## 许可证

Apache License 2.0