# 企智连API自动化测试

## 项目简介

本项目是企智连系统的接口自动化测试框架，基于Java + REST Assured + JUnit 5构建，支持生成美观的Allure测试报告。

## 项目结构

```
api-automation/
├── pom.xml                      # Maven配置文件
├── README.md                    # 项目说明
├── docs/                        # 文档目录
│   └── API_TEST_CASES.md       # 测试用例文档
├── src/
│   └── test/
│       ├── java/
│       │   └── com/qizhilian/api/
│       │       ├── base/
│       │       │   └── BaseApiTest.java      # 测试基类
│       │       ├── config/
│       │       │   └── ApiConfig.java        # 配置管理
│       │       ├── utils/
│       │       │   └── TestDataGenerator.java # 测试数据生成
│       │       ├── auth/
│       │       │   └── AuthApiTest.java      # 认证模块测试
│       │       ├── erp/
│       │       │   └── PurchaseApiTest.java  # ERP模块测试
│       │       ├── crm/
│       │       │   └── CustomerApiTest.java  # CRM模块测试
│       │       └── system/
│       │           └── SystemApiTest.java    # 系统管理测试
│       └── resources/
│           ├── application.properties        # 配置文件
│           └── logback.xml                   # 日志配置
```

## 技术栈

- **Java 17** - 开发语言
- **Maven 3.8+** - 构建工具
- **JUnit 5** - 测试框架
- **REST Assured** - API测试库
- **Allure** - 测试报告
- **JavaFaker** - 测试数据生成

## 快速开始

### 1. 环境准备

确保已安装：
- JDK 17+
- Maven 3.8+

### 2. 配置测试环境

编辑 `src/test/resources/application.properties`：

```properties
# API基础URL
base.url=http://localhost:8080
api.version=/api/v1

# 超时配置
connection.timeout=10000
socket.timeout=30000
```

### 3. 执行测试

```bash
# 进入项目目录
cd api-automation

# 编译项目
mvn clean compile

# 执行所有测试
mvn clean test

# 执行指定模块
mvn test -Dtest=AuthApiTest

# 执行指定方法
mvn test -Dtest=AuthApiTest#testLoginWithUsername
```

### 4. 查看报告

```bash
# 生成Allure报告
mvn allure:report

# 启动Allure服务
mvn allure:serve
```

## 测试模块

| 模块 | 测试类 | 说明 |
|------|--------|------|
| 用户认证 | AuthApiTest | 登录、注册、Token管理 |
| ERP | PurchaseApiTest | 采购、销售、库存管理 |
| CRM | CustomerApiTest | 客户、线索、商机管理 |
| 系统管理 | SystemApiTest | 用户、角色、部门管理 |

## 测试用例

详见 [API_TEST_CASES.md](docs/API_TEST_CASES.md)

## 核心特性

- ✅ 完整的CRUD测试覆盖
- ✅ 正向/负向测试场景
- ✅ 参数化测试支持
- ✅ 测试数据自动生成
- ✅ 测试依赖管理
- ✅ 详细的Allure报告
- ✅ 重试机制
- ✅ 配置化执行

## CI/CD集成

支持Jenkins、GitLab CI等持续集成工具，示例配置：

```groovy
stage('API Tests') {
    steps {
        sh 'mvn clean test'
    }
    post {
        always {
            allure([
                results: [[path: 'target/allure-results']]
            ])
        }
    }
}
```

## 注意事项

1. 测试前确保被测服务已启动
2. 测试数据会自动生成，无需手动准备
3. 测试执行顺序由@Order注解控制
4. 部分测试需要前置条件（如先登录）

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交变更
4. 发起Pull Request

## 许可证

MIT License
