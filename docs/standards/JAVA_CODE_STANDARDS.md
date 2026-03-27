# 智企连·AI-Ready Java 代码规范

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 1. 命名规范

### 1.1 包命名

- 全部小写，使用有意义的英文单词
- 使用倒置域名作为前缀：`cn.aiedge.aiready`

```
cn.aiedge.aiready
├── module          # 功能模块
│   ├── controller  # 控制器
│   ├── service     # 服务层
│   ├── repository  # 数据访问层
│   ├── entity      # 实体类
│   ├── dto         # 数据传输对象
│   └── vo          # 视图对象
```

### 1.2 类命名

- 大驼峰命名法（UpperCamelCase）
- 类名应准确描述其职责

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 实体类 | 名词，对应数据库表 | `User`, `Order` |
| Service | `XxxService` | `UserService` |
| ServiceImpl | `XxxServiceImpl` | `UserServiceImpl` |
| Controller | `XxxController` | `UserController` |
| Repository | `XxxRepository` | `UserRepository` |
| DTO | `XxxDTO` 或 `XxxRequest/Response` | `UserDTO`, `LoginRequest` |
| VO | `XxxVO` | `UserVO` |
| 工具类 | `XxxUtil` | `DateUtil` |
| 常量类 | `XxxConstants` | `UserConstants` |
| 枚举类 | `XxxEnum` | `UserStatusEnum` |
| 异常类 | `XxxException` | `BusinessException` |

### 1.3 方法命名

- 小驼峰命名法（lowerCamelCase）
- 动词或动词短语开头

| 操作 | 前缀 | 示例 |
|------|------|------|
| 查询单个 | `get`/`find` | `getUserById()`, `findByUsername()` |
| 查询列表 | `list`/`query` | `listUsers()`, `queryOrders()` |
| 查询分页 | `page` | `pageUsers()` |
| 新增 | `create`/`save`/`add` | `createUser()`, `saveOrder()` |
| 更新 | `update`/`modify` | `updateUser()` |
| 删除 | `delete`/`remove` | `deleteUser()` |
| 判断 | `is`/`has`/`can` | `isActive()`, `hasPermission()` |
| 统计 | `count` | `countUsers()` |
| 校验 | `validate`/`check` | `validateEmail()` |

### 1.4 变量命名

- 小驼峰命名法
- 成员变量不加前缀（`m`、`_` 等）
- 布尔类型使用 `is`、`has`、`can` 开头

```java
// ✅ 正确
private String userName;
private boolean isActive;
private List<Order> orderList;

// ❌ 错误
private String _userName;
private boolean active;
private List<Order> orders;
```

### 1.5 常量命名

- 全大写，单词间用下划线分隔
- 使用 `final static` 修饰

```java
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_CHARSET = "UTF-8";
```

---

## 2. 代码结构规范

### 2.1 类结构顺序

```java
/**
 * 类说明
 *
 * @author author
 * @since 1.0.0
 */
public class ExampleClass {
    // 1. 静态常量
    public static final String CONSTANT = "value";
    
    // 2. 静态变量
    private static int staticVar;
    
    // 3. 实例变量
    private String instanceVar;
    
    // 4. 静态方法
    public static void staticMethod() {}
    
    // 5. 构造方法
    public ExampleClass() {}
    
    // 6. 公共方法
    public void publicMethod() {}
    
    // 7. 私有方法
    private void privateMethod() {}
    
    // 8. 内部类
    private static class InnerClass {}
}
```

### 2.2 方法参数

- 参数数量不超过 5 个，超过时使用 DTO 封装
- 使用 `@Valid` 注解进行参数校验

```java
// ✅ 推荐：使用 DTO 封装
public Result<UserVO> createUser(@Valid @RequestBody CreateUserRequest request) {
    // ...
}

// ❌ 不推荐：参数过多
public Result<UserVO> createUser(String name, String email, String phone, 
                                  String address, Integer age, String gender) {
    // ...
}
```

---

## 3. Java 17 新特性使用规范

### 3.1 Records（记录类）

用于不可变数据载体，替代传统 DTO：

```java
// ✅ 简洁的记录类
public record UserDTO(Long id, String username, String email) {}

// ✅ 带验证的记录类
public record LoginRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6, max = 20) String password
) {}
```

**使用场景**：
- API 响应数据
- 方法返回值
- 配置信息

### 3.2 Sealed Classes（密封类）

用于限制继承层次：

```java
public sealed interface Result<T> permits Success, Failure, Pending {}

public final record Success<T>(T data) implements Result<T> {}
public final record Failure<T>(String code, String message) implements Result<T> {}
public final record Pending<T>() implements Result<T> {}

// 使用
public Result<User> getUser(Long id) {
    User user = userRepository.findById(id);
    if (user == null) {
        return new Failure<>("USER_NOT_FOUND", "用户不存在");
    }
    return new Success<>(user);
}
```

### 3.3 Pattern Matching（模式匹配）

```java
// ✅ instanceof 模式匹配
if (obj instanceof String s && s.length() > 5) {
    System.out.println(s.toUpperCase());
}

// ✅ switch 表达式
String status = switch (user.getStatus()) {
    case ACTIVE -> "激活";
    case INACTIVE -> "未激活";
    case LOCKED -> "锁定";
    default -> "未知";
};
```

### 3.4 Text Blocks（文本块）

用于多行字符串：

```java
// ✅ SQL 语句
String sql = """
    SELECT u.id, u.username, u.email
    FROM users u
    WHERE u.status = 'ACTIVE'
    ORDER BY u.created_at DESC
    """;

// ✅ JSON 模板
String json = """
    {
        "code": 200,
        "message": "success",
        "data": null
    }
    """;
```

---

## 4. 异常处理规范

### 4.1 异常分类

| 类型 | 说明 | 处理方式 |
|------|------|----------|
| `BusinessException` | 业务异常 | 捕获并返回友好提示 |
| `ValidationException` | 参数校验异常 | 自动处理，返回字段错误 |
| `SystemException` | 系统异常 | 记录日志，返回系统错误 |
| `Exception` | 未知异常 | 记录日志，返回系统错误 |

### 4.2 异常抛出

```java
// ✅ 使用业务异常
public void withdraw(Long userId, BigDecimal amount) {
    User user = userRepository.findById(userId);
    if (user == null) {
        throw new BusinessException("USER_NOT_FOUND", "用户不存在");
    }
    if (user.getBalance().compareTo(amount) < 0) {
        throw new BusinessException("INSUFFICIENT_BALANCE", "余额不足");
    }
    // ...
}
```

### 4.3 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return Result.fail("PARAM_ERROR", message);
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("SYSTEM_ERROR", "系统繁忙，请稍后重试");
    }
}
```

---

## 5. 注释规范

### 5.1 类注释

```java
/**
 * 用户服务实现类
 * 
 * 提供用户的增删改查、状态管理等功能
 *
 * @author zhangsan
 * @since 1.0.0
 */
@Service
public class UserServiceImpl implements UserService {
    // ...
}
```

### 5.2 方法注释

```java
/**
 * 根据ID查询用户
 *
 * @param id 用户ID
 * @return 用户信息，如果不存在返回null
 * @throws BusinessException 当用户被禁用时
 */
public UserVO getUserById(Long id) {
    // ...
}
```

### 5.3 代码注释

```java
// ✅ 解释复杂逻辑
// 计算折扣金额：总价 * 折扣率 - 优惠券金额
BigDecimal discountAmount = totalPrice.multiply(discountRate).subtract(couponAmount);

// ✅ TODO 注释
// TODO: 后续支持多租户
```

---

## 6. 日志规范

### 6.1 日志级别

| 级别 | 使用场景 |
|------|----------|
| ERROR | 错误，需要立即处理 |
| WARN | 警告，潜在问题 |
| INFO | 重要业务信息 |
| DEBUG | 调试信息（生产环境关闭） |
| TRACE | 详细追踪（开发调试用） |

### 6.2 日志格式

```java
// ✅ 使用占位符
log.info("用户登录成功, userId={}", userId);

// ✅ 异常日志
log.error("创建订单失败, userId={}, productId={}", userId, productId, e);

// ❌ 字符串拼接
log.info("用户登录成功, userId=" + userId);
```

### 6.3 敏感信息处理

```java
// ✅ 脱敏处理
log.info("用户登录, phone={}", DesensitizationUtil.phone(phone));

// ❌ 明文记录敏感信息
log.info("用户登录, password={}", password);
```

---

## 7. 性能规范

### 7.1 集合操作

```java
// ✅ 指定初始容量
List<User> users = new ArrayList<>(100);
Map<Long, User> userMap = new HashMap<>(16);

// ✅ 使用 Stream API
List<UserVO> voList = users.stream()
    .map(this::toVO)
    .collect(Collectors.toList());
```

### 7.2 字符串操作

```java
// ✅ 使用 StringBuilder
StringBuilder sb = new StringBuilder();
sb.append("a").append("b").append("c");

// ✅ 使用 Text Block
String sql = """
    SELECT * FROM users
    WHERE status = ?
    """;

// ❌ 循环中拼接字符串
String result = "";
for (String s : list) {
    result += s; // 每次创建新对象
}
```

### 7.3 数据库操作

```java
// ✅ 批量操作
@Insert("<script>" +
    "INSERT INTO users (name, email) VALUES " +
    "<foreach collection='list' item='user' separator=','>" +
    "(#{user.name}, #{user.email})" +
    "</foreach>" +
    "</script>")
void batchInsert(@Param("list") List<User> users);

// ✅ 分页查询
Page<User> page = new Page<>(pageNum, pageSize);
userMapper.selectPage(page, queryWrapper);
```

---

## 8. 安全规范

### 8.1 敏感数据处理

```java
// ✅ 密码加密存储
String encodedPassword = passwordEncoder.encode(rawPassword);

// ✅ 敏感字段脱敏
public record UserVO(
    Long id,
    String username,
    @JsonSerialize(using = PhoneDesensitizeSerializer.class)
    String phone
) {}
```

### 8.2 SQL 注入防护

```java
// ✅ 使用参数化查询
@Select("SELECT * FROM users WHERE username = #{username}")
User findByUsername(@Param("username") String username);

// ❌ 字符串拼接
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

### 8.3 XSS 防护

```java
// ✅ 输出转义
String safeContent = HtmlUtils.htmlEscape(content);
```

---

## 9. 代码质量检查

### 9.1 SonarQube 规则

- 无严重/阻断性问题
- 代码重复率 < 3%
- 测试覆盖率 > 60%

### 9.2 Checkstyle 配置

```xml
<!-- checkstyle.xml -->
<module name="Checker">
    <module name="TreeWalker">
        <module name="MethodName"/>
        <module name="ParameterName"/>
        <module name="LocalVariableName"/>
        <module name="MemberName"/>
        <module name="ConstantName"/>
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
    </module>
</module>
```

---

## 10. 附录

### 10.1 常用工具类

| 工具类 | 用途 |
|--------|------|
| `cn.hutool.core.util.StrUtil` | 字符串工具 |
| `cn.hutool.core.collection.CollUtil` | 集合工具 |
| `cn.hutool.core.date.DateUtil` | 日期工具 |
| `cn.hutool.core.bean.BeanUtil` | Bean 转换 |
| `cn.hutool.crypto.SecureUtil` | 加密工具 |

### 10.2 参考资料

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer