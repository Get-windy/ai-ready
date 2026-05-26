# 企智连自动化测试框架

## 项目概述

本项目是企智连（AI-Ready）项目的自动化测试框架，支持接口测试、UI测试和移动端测试。

## 技术栈

- **测试框架**: JUnit 5
- **接口测试**: REST Assured
- **UI测试**: Selenium WebDriver / Playwright
- **移动端测试**: Appium
- **报告工具**: Allure
- **构建工具**: Maven
- **CI/CD**: Jenkins

## 项目结构

```
automation/
├── pom.xml                      # Maven配置文件
├── Jenkinsfile                  # Jenkins流水线配置
├── README.md                    # 项目说明文档
├── src/
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── qizhilian/
│       │           ├── api/     # 接口测试
│       │           │   ├── base/
│       │           │   ├── user/
│       │           │   └── inventory/
│       │           ├── ui/      # UI测试
│       │           │   ├── base/
│       │           │   ├── pages/
│       │           │   └── auth/
│       │           ├── mobile/  # 移动端测试
│       │           │   ├── base/
│       │           │   └── tests/
│       │           ├── config/  # 配置管理
│       │           └── datadriven/  # 数据驱动
│       └── resources/
│           ├── config/          # 配置文件
│           └── data/            # 测试数据
└── docs/                        # 文档目录
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- Chrome/Firefox/Edge浏览器（UI测试）
- Appium Server（移动端测试）

### 安装依赖

```bash
mvn clean install -DskipTests
```

### 运行测试

#### 运行所有测试
```bash
mvn test
```

#### 运行接口测试
```bash
mvn test -Papi-test
```

#### 运行UI测试
```bash
mvn test -Pui-test -Dbrowser.type=chrome -Dheadless.mode=false
```

#### 运行移动端测试
```bash
mvn test -Pmobile-test
```

### 生成Allure报告

```bash
mvn allure:serve
```

## 配置说明

### 测试配置文件

配置文件位于 `src/test/resources/config/test.properties`：

```properties
# 基础配置
base.url=http://localhost:8080
api.base.path=/api/v1
ui.base.url=http://localhost:3000

# UI测试配置
browser.type=chrome
headless.mode=false

# 移动端配置
mobile.platform=android
appium.server.url=http://localhost:4723
```

### 命令行参数覆盖

所有配置都支持通过命令行参数覆盖：

```bash
mvn test -Dbase.url=http://test-server:8080 -Dbrowser.type=firefox
```

## 测试数据管理

### 数据驱动测试

支持Excel、JSON、CSV格式的测试数据：

```java
// 从Excel读取数据
List<Map<String, Object>> data = DataProvider.readExcelData("users.xlsx", "Sheet1");

// 从JSON读取数据
List<User> users = DataProvider.readJsonData("users.json", User.class);

// 从CSV读取数据
List<Map<String, Object>> data = DataProvider.readCsvData("users.csv");
```

## CI/CD集成

### Jenkins配置

1. 安装Allure插件
2. 配置Maven和JDK工具
3. 创建Pipeline项目，使用Jenkinsfile

### 流水线参数

- `TEST_SUITE`: 选择测试套件 (all/api/ui/mobile)
- `BROWSER`: UI测试浏览器类型
- `HEADLESS`: 是否使用无头模式
- `SKIP_TESTS`: 跳过测试阶段

## 测试报告

### Allure报告

- 包含详细的测试步骤、附件、历史记录
- 支持分类查看（按功能、按严重程度）
- 支持失败重试历史

### 代码覆盖率

使用JaCoCo生成代码覆盖率报告：

```bash
mvn jacoco:report
```

## 开发规范

### 测试类命名

- 接口测试: `*ApiTest.java`
- UI测试: `*UiTest.java`
- 移动端测试: `*MobileTest.java`

### 测试方法命名

使用 `@DisplayName` 注解描述测试用例：

```java
@Test
@DisplayName("创建用户成功")
void testCreateUserSuccess() {
    // 测试逻辑
}
```

### Allure注解

```java
@Epic("企智连API测试")
@Feature("用户管理")
@Story("用户认证")
@Severity(SeverityLevel.CRITICAL)
@DisplayName("用户登录成功")
```

## 常见问题

### 1. ChromeDriver版本不匹配

下载与Chrome浏览器版本匹配的ChromeDriver，并配置路径：

```properties
chromedriver.path=/path/to/chromedriver
```

### 2. Appium连接失败

确保Appium Server已启动：

```bash
appium
```

### 3. 测试数据文件找不到

确保测试数据文件放在 `src/test/resources/data/` 目录下。

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 创建Pull Request

## 许可证

MIT License

## 联系方式

如有问题，请联系QA团队。
