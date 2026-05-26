# 企智连接口测试方案

## 1. 测试概述

### 1.1 测试目标
- 验证企智连系统API接口的功能正确性
- 确保接口在各种场景下的稳定性和可靠性
- 建立接口自动化测试体系

### 1.2 测试范围
- **认证接口**: 登录、登出、Token刷新
- **ERP核心接口**: 库存、采购、销售、财务
- **CRM核心接口**: 客户、订单、合同
- **系统管理接口**: 用户、角色、权限、菜单

### 1.3 测试环境
- **测试环境**: http://test.qizhilian.com:8080
- **API版本**: v1
- **认证方式**: JWT Bearer Token

## 2. 接口测试范围

### 2.1 认证接口

| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 用户登录 | POST | /api/v1/auth/login | P0 |
| 用户登出 | POST | /api/v1/auth/logout | P1 |
| Token刷新 | POST | /api/v1/auth/refresh | P1 |
| 获取当前用户 | GET | /api/v1/auth/me | P1 |

### 2.2 ERP核心接口

#### 库存管理
| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 入库单创建 | POST | /api/v1/inventory/inbound | P0 |
| 出库单创建 | POST | /api/v1/inventory/outbound | P0 |
| 库存查询 | GET | /api/v1/inventory | P0 |
| 库存盘点 | POST | /api/v1/inventory/stock-check | P1 |
| 库存预警 | GET | /api/v1/inventory/alerts | P1 |

#### 采购管理
| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 采购申请 | POST | /api/v1/purchase/requests | P0 |
| 采购订单 | POST | /api/v1/purchase/orders | P0 |
| 供应商管理 | CRUD | /api/v1/purchase/suppliers | P1 |

#### 销售管理
| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 销售订单 | POST | /api/v1/sales/orders | P0 |
| 客户管理 | CRUD | /api/v1/sales/customers | P1 |
| 销售退货 | POST | /api/v1/sales/returns | P1 |

#### 财务管理
| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 应收管理 | GET/POST | /api/v1/finance/receivables | P1 |
| 收款记录 | POST | /api/v1/finance/payments | P1 |
| 财务报表 | GET | /api/v1/finance/reports | P1 |

### 2.3 CRM核心接口

| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 客户列表 | GET | /api/v1/crm/customers | P0 |
| 客户详情 | GET | /api/v1/crm/customers/{id} | P0 |
| 客户创建 | POST | /api/v1/crm/customers | P0 |
| 客户更新 | PUT | /api/v1/crm/customers/{id} | P1 |
| 合同管理 | CRUD | /api/v1/crm/contracts | P1 |
| 跟进记录 | CRUD | /api/v1/crm/follow-ups | P1 |

### 2.4 系统管理接口

| 接口 | 方法 | 路径 | 优先级 |
|------|------|------|--------|
| 用户管理 | CRUD | /api/v1/users | P0 |
| 角色管理 | CRUD | /api/v1/roles | P1 |
| 权限管理 | GET | /api/v1/permissions | P1 |
| 菜单管理 | CRUD | /api/v1/menus | P1 |
| 部门管理 | CRUD | /api/v1/departments | P1 |

## 3. 测试工具选型

### 3.1 工具对比

| 工具 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **REST Assured** | Java集成好、灵活、可编程 | 需要编程基础 | 自动化测试、CI/CD |
| **Postman** | 易用、可视化、团队协作 | 高级功能收费 | 手动测试、API文档 |
| **JMeter** | 性能测试强、开源 | 学习曲线陡 | 性能测试 |

### 3.2 选型决策

**主选工具**: REST Assured + JUnit 5
- 已在自动化框架中集成
- 支持复杂的业务逻辑验证
- 易于CI/CD集成

**辅助工具**: Postman
- 用于快速手动验证
- API文档维护
- 团队协作

## 4. 测试用例设计

### 4.1 测试用例分类

| 类型 | 说明 | 占比 |
|------|------|------|
| 正常场景 | 标准业务流程 | 40% |
| 异常场景 | 错误处理、异常输入 | 30% |
| 边界值 | 极限值测试 | 20% |
| 安全测试 | 权限、注入攻击 | 10% |

### 4.2 认证接口测试用例

#### TC-AUTH-001: 用户登录成功
```
前置条件: 用户已注册且状态正常
输入: username=admin, password=admin123
预期结果:
  - HTTP状态码: 200
  - 返回token不为空
  - 返回用户信息正确
```

#### TC-AUTH-002: 用户登录失败-密码错误
```
前置条件: 用户已注册
输入: username=admin, password=wrongpass
预期结果:
  - HTTP状态码: 401
  - 错误信息: 用户名或密码错误
```

#### TC-AUTH-003: 用户登录失败-用户不存在
```
输入: username=notexist, password=123456
预期结果:
  - HTTP状态码: 401
  - 错误信息: 用户不存在
```

#### TC-AUTH-004: Token刷新
```
前置条件: 用户已登录，有有效token
输入: refreshToken=xxx
预期结果:
  - HTTP状态码: 200
  - 返回新的accessToken
```

### 4.3 库存接口测试用例

#### TC-INV-001: 入库单创建成功
```
前置条件: 用户已登录，有库存管理权限
输入:
  - warehouseId: 1
  - materialId: 1
  - quantity: 100
  - batchNo: BATCH001
预期结果:
  - HTTP状态码: 200
  - 返回入库单ID
  - 库存数量增加100
```

#### TC-INV-002: 入库单创建-库存不足
```
前置条件: 用户已登录
输入:
  - warehouseId: 1
  - materialId: 1
  - quantity: -10 (非法值)
预期结果:
  - HTTP状态码: 400
  - 错误信息: 数量不能为负数
```

#### TC-INV-003: 库存查询
```
前置条件: 用户已登录
输入: page=1, size=20
预期结果:
  - HTTP状态码: 200
  - 返回库存列表
  - 分页信息正确
```

### 4.4 用户管理接口测试用例

#### TC-USER-001: 创建用户成功
```
前置条件: 管理员已登录
输入:
  - username: testuser
  - password: Test@123
  - email: test@example.com
  - roleIds: [1, 2]
预期结果:
  - HTTP状态码: 200
  - 返回用户ID
  - 数据库中用户存在
```

#### TC-USER-002: 创建用户-用户名重复
```
前置条件: 用户testuser已存在
输入: username=testuser
预期结果:
  - HTTP状态码: 400
  - 错误信息: 用户名已存在
```

#### TC-USER-003: 创建用户-邮箱格式错误
```
输入: email=invalid-email
预期结果:
  - HTTP状态码: 400
  - 错误信息: 邮箱格式不正确
```

## 5. 接口自动化测试

### 5.1 项目结构

```
api-test-cases/
├── src/
│   └── test/
│       ├── java/
│       │   └── com/qizhilian/
│       │       ├── api/
│       │       │   ├── auth/
│       │       │   ├── erp/
│       │       │   ├── crm/
│       │       │   └── system/
│       │       └── base/
│       └── resources/
│           ├── testdata/
│           └── config/
├── pom.xml
└── README.md
```

### 5.2 核心代码示例

```java
@Test
@DisplayName("用户登录成功")
void testLoginSuccess() {
    Map<String, Object> body = new HashMap<>();
    body.put("username", "admin");
    body.put("password", "admin123");
    
    Response response = given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/v1/auth/login")
        .then()
        .statusCode(200)
        .body("code", equalTo("0"))
        .body("data.token", notNullValue())
        .extract()
        .response();
    
    // 保存token供后续使用
    token = response.jsonPath().getString("data.token");
}
```

## 6. CI/CD集成

### 6.1 Jenkins Pipeline配置

```groovy
stage('API Test') {
    steps {
        sh 'mvn test -Papi-test'
    }
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            allure([
                includeProperties: false,
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])
        }
    }
}
```

### 6.2 测试报告

- **Allure报告**: 详细的测试步骤、附件、趋势图
- **JUnit报告**: 标准XML格式，CI/CD集成
- **覆盖率报告**: JaCoCo代码覆盖率统计

## 7. 交付物清单

- [x] 接口测试方案文档
- [ ] 接口测试用例集合（Excel/JSON）
- [ ] 自动化测试脚本（Java）
- [ ] 测试数据文件
- [ ] CI/CD配置
- [ ] 测试报告模板

---

**文档版本**: v1.0  
**创建日期**: 2026-04-14  
**负责人**: QA Lead
