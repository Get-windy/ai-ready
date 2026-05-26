# ERP电子签收模块

## 概述

电子签收模块是企智连ERP配送系统的核心功能，支持拍照签收、电子签名签收，并提供防篡改验证机制，防止配送纠纷。

## 功能特性

### 核心功能
- ✅ **拍照签收**：上传照片作为签收凭证
- ✅ **电子签名签收**：绘制签名作为签收凭证
- ✅ **签收记录查询**：按订单、配送员查询签收记录
- ✅ **防篡改验证**：SHA256哈希校验，确保签收数据完整性
- ✅ **配送评价**：客户对配送服务进行评分评价

### 业务场景
- 配送员送货后客户签收
- 防止配送纠纷（签收凭证可追溯）
- 配送服务质量评价

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **安全**：SHA256哈希校验（防篡改）
- **API文档**：Knife4j

## API接口

### 电子签收 `/api/signature`
- `POST /photo` - 拍照签收
- `POST /electronic` - 电子签名签收
- `GET /{id}` - 获取签收详情
- `GET /{id}/verify` - 签收防篡改验证
- `GET /list` - 签收记录列表查询
- `GET /order/{orderNo}` - 获取订单最新签收记录
- `GET /delivery-person/{deliveryPersonId}` - 配送员签收记录查询

### 配送评价 `/api/rating`
- `POST` - 提交配送评价
- `GET /{id}` - 获取评价详情
- `GET /signature/{signatureId}` - 获取签收记录的评价
- `GET /list` - 评价列表查询
- `GET /stats/{deliveryPersonId}` - 配送员评价统计

## 防篡改机制

签收时生成完整性哈希：
```
hash = SHA256(orderNo + signerName + signTime + dataHash)
```

验证时重新计算哈希，与原始哈希比对，确保数据未被篡改。

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_signature_tables.sql`

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问地址
- 服务地址：http://localhost:8096
- API文档：http://localhost:8096/doc.html

## 许可证

Apache License 2.0