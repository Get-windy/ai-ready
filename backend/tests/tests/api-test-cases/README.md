# 企智连接口测试用例集

## 项目概述

本项目包含企智连系统的接口测试用例，覆盖认证、ERP、CRM、系统管理等核心模块。

## 目录结构

```
api-test-cases/
├── API_TEST_PLAN.md          # 接口测试方案文档
├── README.md                 # 项目说明
└── test-cases/               # 测试用例目录
    ├── auth_test_cases.json  # 认证模块测试用例
    ├── inventory_test_cases.json  # 库存管理测试用例
    └── user_test_cases.json  # 用户管理测试用例
```

## 测试用例统计

| 模块 | 用例数 | P0用例 | P1用例 |
|------|--------|--------|--------|
| 认证模块 | 9 | 5 | 4 |
| 库存管理 | 10 | 5 | 5 |
| 用户管理 | 12 | 5 | 7 |
| **合计** | **31** | **15** | **16** |

## 用例类型分布

| 类型 | 数量 | 占比 |
|------|------|------|
| 正常场景 | 15 | 48% |
| 异常场景 | 10 | 32% |
| 边界值 | 6 | 20% |

## 测试用例格式

```json
{
  "id": "TC-AUTH-001",
  "title": "用户登录成功",
  "method": "POST",
  "path": "/login",
  "priority": "P0",
  "type": "正常场景",
  "precondition": "用户已注册",
  "headers": {
    "Authorization": "Bearer ${token}"
  },
  "request": {
    "username": "admin",
    "password": "admin123"
  },
  "expected": {
    "statusCode": 200,
    "response": {
      "code": "0",
      "data": {
        "token": "notNull"
      }
    }
  }
}
```

## 使用方法

### 1. 导入Postman

1. 打开Postman
2. 选择 File -> Import
3. 选择对应的JSON文件
4. 设置环境变量（baseUrl, token等）

### 2. 自动化测试

与自动化测试框架集成：

```java
// 读取测试用例
List<TestCase> testCases = TestCaseLoader.load("test-cases/auth_test_cases.json");

// 执行测试
for (TestCase tc : testCases) {
    executeTestCase(tc);
}
```

## 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| baseUrl | API基础地址 | http://localhost:8080 |
| token | 认证Token | eyJhbG... |
| refreshToken | 刷新Token | eyJhbG... |

## 维护说明

1. 新增用例时，按模块分类存放
2. 用例ID格式：TC-{模块}-{序号}
3. 优先级定义：P0（核心）、P1（重要）、P2（一般）
4. 类型定义：正常场景、异常场景、边界值、安全测试

## 版本记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-04-14 | 初始版本，包含31个测试用例 |

---

**负责人**: QA Lead  
**创建日期**: 2026-04-14
