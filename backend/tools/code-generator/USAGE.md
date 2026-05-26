# AI-Ready 代码生成器使用指南

## 概述
AI-Ready 代码生成器模块是一个强大的自动化工具，可以根据数据库表结构自动生成前后端代码。它支持多种模板引擎和高度定制化的配置。

## 功能特点
- 支持多种数据库（MySQL, PostgreSQL, Oracle, SQL Server）
- 可定制的代码模板
- RESTful API 接口
- 完整的CRUD功能
- 支持生成实体类、Mapper、Service、Controller和前端页面

## 快速开始

### 1. 配置数据源
首先，您需要配置数据源信息：

```bash
POST /api/codegen/datasource/create
Content-Type: application/json

{
  "name": "My MySQL DB",
  "description": "My MySQL database",
  "jdbcUrl": "jdbc:mysql://localhost:3306/mydb?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8",
  "driverName": "com.mysql.cj.jdbc.Driver",
  "username": "root",
  "password": "password",
  "dbType": "mysql"
}
```

### 2. 测试数据源连接
```bash
POST /api/codegen/datasource/test/{id}
```

### 3. 创建生成配置
```bash
POST /api/codegen/config/create
Content-Type: application/json

{
  "name": "My Config",
  "packageName": "com.mycompany.project",
  "moduleName": "demo",
  "author": "Developer Name",
  "enableLombok": true,
  "enableSwagger": false
}
```

### 4. 生成代码
```bash
POST /api/codegen/generate
Content-Type: application/json

{
  "dataSourceId": 1,
  "configId": 1,
  "tables": "user,role,permission",
  "outputDir": "./generated-code"
}
```

## API 接口说明

### 数据源管理接口
- `GET /api/codegen/datasource/list` - 获取数据源列表
- `POST /api/codegen/datasource/create` - 创建数据源
- `PUT /api/codegen/datasource/update` - 更新数据源
- `DELETE /api/codegen/datasource/delete/{id}` - 删除数据源
- `GET /api/codegen/datasource/detail/{id}` - 获取数据源详情
- `POST /api/codegen/datasource/test/{id}` - 测试数据源连接

### 模板管理接口
- `GET /api/codegen/template/list` - 获取模板列表
- `POST /api/codegen/template/create` - 创建模板
- `PUT /api/codegen/template/update` - 更新模板
- `DELETE /api/codegen/template/delete/{id}` - 删除模板
- `GET /api/codegen/template/detail/{id}` - 获取模板详情

### 生成配置接口
- `GET /api/codegen/config/list` - 获取生成配置列表
- `POST /api/codegen/config/create` - 创建生成配置
- `PUT /api/codegen/config/update` - 更新生成配置
- `DELETE /api/codegen/config/delete/{id}` - 删除生成配置

### 代码生成接口
- `POST /api/codegen/generate` - 生成代码
- `POST /api/codegen/generate/default` - 使用默认配置生成代码

## 模板系统

代码生成器支持自定义模板，您可以根据项目需求创建自己的代码模板：

1. **实体类模板** - 生成Java实体类
2. **Mapper模板** - 生成MyBatis Mapper接口和XML文件
3. **Service模板** - 生成业务逻辑层接口和实现
4. **Controller模板** - 生成REST控制器
5. **前端模板** - 生成Vue/React组件

## 配置选项

生成配置包括以下选项：

- `author` - 代码作者
- `packageName` - 包名
- `moduleName` - 模块名
- `enableLombok` - 是否启用Lombok
- `enableSwagger` - 是否启用Swagger
- `enableChainModel` - 是否启用链式模型
- `fileOverride` - 是否覆盖已存在的文件

## 最佳实践

1. **数据源安全**：不要在生产环境中暴露数据库凭证
2. **模板版本控制**：为模板维护版本历史
3. **生成前备份**：在生成代码前备份现有代码
4. **代码审查**：生成的代码需要人工审查后再提交

## 注意事项

- 确保数据库连接正常
- 检查输出目录权限
- 验证生成的代码质量
- 遵循项目编码规范

## 故障排除

常见问题：

1. **连接数据库失败** - 检查数据库连接参数和网络连接
2. **生成代码不完整** - 检查表结构和字段定义
3. **模板渲染错误** - 检查模板语法和变量引用

## 扩展性

代码生成器具有良好的扩展性，您可以：

1. 自定义代码模板
2. 添加新的生成策略
3. 集成其他框架和工具
4. 扩展API接口