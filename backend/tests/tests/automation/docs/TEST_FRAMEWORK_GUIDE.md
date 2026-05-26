# 企智连自动化测试框架使用指南

## 目录

1. [快速开始](#快速开始)
2. [项目结构](#项目结构)
3. [编写测试用例](#编写测试用例)
4. [运行测试](#运行测试)
5. [查看报告](#查看报告)
6. [CI/CD集成](#cicd集成)
7. [常见问题](#常见问题)

## 快速开始

### 1. 环境准备

确保已安装以下工具：
- JDK 17 或更高版本
- Maven 3.9 或更高版本
- Chrome/Firefox/Edge 浏览器（用于UI测试）
- Appium Server（用于移动端测试）

### 2. 克隆项目

```bash
cd I:\AI-Ready\tests\automation
```

### 3. 安装依赖

```bash
mvn clean install -DskipTests
```

### 4. 运行第一个测试

```bash
mvn test -Dtest=UserApiTest#testLoginSuccess
```

## 项目结构

```
automation/
├── pom.xml                      # Maven配置
├── Jenkinsfile                  # Jenkins流水线
├── src/
│   └── test/
│       ├── java/
│       │   └── com/qizhilian/
│       │       ├── api/         # 接口测试
│       │       │   ├── base/    # API测试基类
│       │       │   ├── user/    # 用户模块测试
│       │       │   └── inventory/   # 库存模块测试
│       │       ├── ui/          # UI测试
│       │       │   ├── base/    # UI测试基类
│       │       │   ├── pages/   # 页面对象
│       │       │   └── auth/    # 认证模块测试
│       │       ├── mobile/      # 移动端测试
│       │       │   ├── base/    # 移动端测试基类
│       │       │   └── tests/   # 移动端测试用例
│       │       ├── config/      # 配置管理
│       │       ├── datadriven/  # 数据驱动
│       │       └── util/        # 工具类
│       └── resources/
│           ├── config/          # 配置文件
│           └── data/            # 测试数据
└── docs/                        # 文档
```

## 编写测试用例

### 接口测试示例

```java
@Feature("用户管理")
@Epic("企智连API测试")
public class UserApiTest extends ApiBaseTest {
    
    @Test
    @Story("用户认证")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("用户登录成功")
    void testLoginSuccess() {
        Map<String, Object> loginBody = new HashMap<>();
        loginBody.put("username", "admin");
        loginBody.put("password", "admin123");
        
        Response response = post("/auth/login", loginBody);
        
        assertSuccess(response);
        assertFieldExists(response, "data.token");
    }
}
```

### UI测试示例

```java
@Epic("企智连UI测试")
@Feature("用户认证")
public class LoginUiTest extends UiBaseTest {
    
    @Test
    @DisplayName("使用有效凭据登录成功")
    void testLoginWithValidCredentials() {
        openPath("/login");
        LoginPage loginPage = new LoginPage(driver);
        
        DashboardPage dashboard = loginPage.login("admin", "admin123");
        
        assertTrue(dashboard.isDashboardLoaded());
    }
}
```

### 数据驱动测试示例

```java
@ParameterizedTest
@CsvSource({
    "admin, admin123, true",
    "admin, wrongpass, false",
    "user, user123, true"
})
@DisplayName("登录参数化测试")
void testLoginWithMultipleUsers(String username, String password, boolean shouldSucceed) {
    Map<String, Object> loginBody = new HashMap<>();
    loginBody.put("username", username);
    loginBody.put("password", password);
    
    Response response = post("/auth/login", loginBody);
    
    if (shouldSucceed) {
        assertSuccess(response);
    } else {
        assertStatusCode(response, 401);
    }
}
```

## 运行测试

### 运行所有测试

```bash
mvn test
```

### 运行特定测试类

```bash
mvn test -Dtest=UserApiTest
```

### 运行特定测试方法

```bash
mvn test -Dtest=UserApiTest#testLoginSuccess
```

### 运行接口测试套件

```bash
mvn test -Papi-test
```

### 运行UI测试套件

```bash
mvn test -Pui-test -Dbrowser.type=chrome -Dheadless.mode=false
```

### 运行移动端测试套件

```bash
mvn test -Pmobile-test
```

### 使用特定环境配置

```bash
mvn test -Dbase.url=http://test-server:8080 -Denv=test
```

## 查看报告

### 生成Allure报告

```bash
# 生成并打开报告
mvn allure:serve

# 仅生成报告
mvn allure:report
```

### 查看代码覆盖率

```bash
mvn jacoco:report
```

报告位于 `target/site/jacoco/index.html`

## CI/CD集成

### Jenkins配置

1. 安装插件：Allure、JaCoCo
2. 配置全局工具：Maven、JDK
3. 创建Pipeline项目，使用Jenkinsfile

### 流水线参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| TEST_SUITE | 测试套件 | all |
| BROWSER | UI测试浏览器 | chrome |
| HEADLESS | 无头模式 | true |
| SKIP_TESTS | 跳过测试 | false |

### 手动触发构建

```bash
curl -X POST http://jenkins-server/job/automation-test/build \
  --data-urlencode "TEST_SUITE=api" \
  --data-urlencode "BROWSER=chrome"
```

## 常见问题

### Q1: ChromeDriver版本不匹配

**解决方案：**
1. 查看Chrome版本：`chrome://version/`
2. 下载对应版本的ChromeDriver
3. 配置路径：`chromedriver.path=/path/to/chromedriver`

### Q2: Appium连接失败

**解决方案：**
1. 确保Appium Server已启动：`appium`
2. 检查设备是否已连接：`adb devices`
3. 验证Appium配置参数

### Q3: 测试数据文件找不到

**解决方案：**
确保测试数据文件放在 `src/test/resources/data/` 目录下

### Q4: Allure报告中文乱码

**解决方案：**
在pom.xml中添加编码配置：
```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
```

### Q5: 测试执行超时

**解决方案：**
调整超时配置：
```bash
mvn test -Dtimeout.seconds=60
```

## 最佳实践

1. **测试隔离**：每个测试用例应该独立运行，不依赖其他测试
2. **数据清理**：测试完成后清理测试数据
3. **断言明确**：使用具体的断言消息
4. **日志记录**：使用@Slf4j记录关键步骤
5. **Allure注解**：使用@Step、@Attachment等注解丰富报告

## 联系方式

如有问题，请联系QA团队或提交Issue。
