# 企智连测试脚本编写规范

## 目录
1. [命名规范](#命名规范)
2. [注释规范](#注释规范)
3. [结构规范](#结构规范)
4. [断言规范](#断言规范)
5. [数据管理规范](#数据管理规范)
6. [异常处理规范](#异常处理规范)
7. [测试报告规范](#测试报告规范)

---

## 命名规范

### 1. 包命名规范
```
com.qizhilian.{测试类型}.{模块名}

示例：
com.qizhilian.api.user        # 用户模块API测试
com.qizhilian.ui.inventory    # 库存模块UI测试
com.qizhilian.mobile.auth     # 认证模块移动端测试
```

### 2. 类命名规范
```
{功能模块}Test 或 {功能模块}{测试类型}Test

示例：
UserApiTest         # 用户API测试
LoginUiTest         # 登录UI测试
InventoryMobileTest # 库存移动端测试
```

### 3. 方法命名规范
```
test{功能点}{场景}

示例：
testLoginSuccess          # 登录成功测试
testLoginFailure          # 登录失败测试
testCreateOrderValid      # 创建订单-有效数据
testCreateOrderInvalid    # 创建订单-无效数据
```

### 4. 测试ID命名规范
```
{模块代码}-{测试序号}

示例：
SM-001  # 销售管理模块测试001
PM-002  # 采购管理模块测试002
UAT-003 # 用户认证测试003
```

---

## 注释规范

### 1. 类注释规范
```java
/**
 * {模块名称}测试类
 * 
 * 测试范围：
 * 1. {功能点1}
 * 2. {功能点2}
 * 
 * @author {作者}
 * @version {版本}
 * @since {日期}
 */
@Feature("{模块名}")
@Epic("企智连{测试类型}测试")
public class {ClassName}Test extends BaseTest {
    // ...
}
```

### 2. 方法注释规范
```java
/**
 * 测试{功能点}
 * 
 * 测试步骤：
 * 1. {步骤1}
 * 2. {步骤2}
 * 
 * 预期结果：{预期结果描述}
 */
@Test
@Story("{功能点}")
@Severity(SeverityLevel.{级别})
@DisplayName("{测试名称}")
void test{MethodName}() {
    // ...
}
```

---

## 结构规范

### 1. API测试结构
```java
@Test
void testApiStructure() {
    // 1. 准备测试数据
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("key", "value");
    
    // 2. 发送请求
    Response response = post("/api/path", requestBody);
    
    // 3. 验证响应
    assertSuccess(response);
    assertFieldEquals(response, "data.field", expectedValue);
    
    // 4. 清理数据（如有必要）
    delete("/api/path/{id}", createdId);
}
```

### 2. UI测试结构（Page Object Model）
```java
@Test
void testUiStructure() {
    // 1. 初始化页面对象
    LoginPage loginPage = new LoginPage(driver);
    
    // 2. 执行操作
    loginPage.navigate();
    loginPage.login(username, password);
    
    // 3. 验证结果
    assertTrue(loginPage.isLoginSuccessful());
    
    // 4. 清理（如有必要）
    loginPage.logout();
}
```

---

## 断言规范

### 1. API断言规范
```java
// 状态码断言
assertStatusCode(response, 200);

// 成功状态断言
assertSuccess(response);

// 字段存在断言
assertFieldExists(response, "data.token");

// 字段值断言
assertFieldEquals(response, "data.userId", expectedId);

// 字段非空断言
assertFieldNotNull(response, "data.username");

// 列表长度断言
assertListSize(response, "data.list", expectedSize);
```

### 2. UI断言规范
```java
// 元素可见断言
assertTrue(page.isElementVisible(elementLocator));

// 文本内容断言
assertEquals(page.getElementText(elementLocator), expectedText);

// 元素状态断言
assertTrue(page.isElementEnabled(buttonLocator));

// 页面URL断言
assertEquals(driver.getCurrentUrl(), expectedUrl);
```

---

## 数据管理规范

### 1. 测试数据文件位置
```
src/test/resources/data/{模块名}/{数据文件}

示例：
src/test/resources/data/user/test_users.json
src/test/resources/data/order/test_orders.json
```

### 2. 测试数据格式
```json
{
  "users": [
    {
      "id": 1,
      "username": "test_user_1",
      "password": "Test@123",
      "email": "test1@example.com"
    }
  ]
}
```

### 3. 数据驱动测试示例
```java
@ParameterizedTest
@CsvSource({
    "user1, password1, true",
    "user2, password2, true",
    "invalid, invalid, false"
})
@DisplayName("参数化登录测试")
void testParameterizedLogin(String username, String password, boolean expectedSuccess) {
    // 使用参数执行测试
}
```

---

## 异常处理规范

### 1. 预期异常测试
```java
@Test
void testExpectedException() {
    assertThrows(IllegalArgumentException.class, () -> {
        service.method(invalidInput);
    });
}
```

### 2. 异常场景验证
```java
@Test
void testApiErrorHandling() {
    Response response = post("/api/path", invalidData);
    
    // 验证错误响应
    assertStatusCode(response, 400);
    assertFieldEquals(response, "code", "ERROR_CODE");
    assertFieldExists(response, "message");
}
```

---

## 测试报告规范

### 1. Allure注解使用
```java
@Epic("企智连API测试")          // Epic级别
@Feature("用户管理")            // Feature级别
@Story("用户认证")              // Story级别
@Severity(SeverityLevel.CRITICAL) // 严重程度
@DisplayName("用户登录成功")     // 显示名称
@Description("详细描述...")      // 详细描述
```

### 2. 测试步骤记录
```java
@Test
void testWithSteps() {
    Allure.step("步骤1: 准备数据", () -> {
        prepareData();
    });
    
    Allure.step("步骤2: 执行操作", () -> {
        executeAction();
    });
    
    Allure.step("步骤3: 验证结果", () -> {
        verifyResult();
    });
}
```

### 3. 添加附件
```java
Allure.addAttachment("请求体", "application/json", requestBody);
Allure.addAttachment("响应体", "application/json", response.asString());
Allure.addAttachment("截图", "image/png", screenshotBytes);
```

---

## 最佳实践

### 1. 测试独立性
- 每个测试应独立运行，不依赖其他测试
- 使用@BeforeEach/@AfterEach清理测试环境

### 2. 测试可重复性
- 测试数据应可重复使用
- 避免硬编码时间戳等不可重复数据

### 3. 测试可维护性
- 使用Page Object Model分离页面逻辑
- 使用常量类管理配置和URL
- 使用工具类封装通用操作

### 4. 测试覆盖率
- 正向场景：有效数据、正常流程
- 反向场景：无效数据、异常流程
- 边界场景：边界值、极限值

---

## 检查清单

提交测试脚本前请确认：
- [ ] 类名和方法名符合命名规范
- [ ] 类和方法有完整注释
- [ ] 测试步骤清晰明确
- [ ] 断言覆盖所有验证点
- [ ] 异常场景已处理
- [ ] Allure注解已添加
- [ ] 测试数据已准备好
- [ ] 测试可独立运行

---

**文档版本**: v1.0  
**最后更新**: 2026-04-14  
**作者**: QA Lead