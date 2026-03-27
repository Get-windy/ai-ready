# 智企连·AI-Ready API 接口规范

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 1. 接口设计原则

### 1.1 RESTful 设计

- 使用名词表示资源
- 使用 HTTP 方法表示操作
- 使用 HTTP 状态码表示结果

### 1.2 资源命名

```
✅ 推荐
GET    /api/v1/users           # 获取用户列表
GET    /api/v1/users/{id}      # 获取单个用户
POST   /api/v1/users           # 创建用户
PUT    /api/v1/users/{id}      # 更新用户
DELETE /api/v1/users/{id}      # 删除用户

❌ 不推荐
GET    /api/v1/getUsers
POST   /api/v1/createUser
GET    /api/v1/user/query
```

### 1.3 版本控制

```
URL 版本控制（推荐）
/api/v1/users
/api/v2/users

Header 版本控制
Accept: application/vnd.aiready.v1+json
```

---

## 2. 请求规范

### 2.1 HTTP 方法

| 方法 | 用途 | 是否幂等 |
|------|------|----------|
| GET | 查询资源 | 是 |
| POST | 创建资源 | 否 |
| PUT | 全量更新资源 | 是 |
| PATCH | 部分更新资源 | 是 |
| DELETE | 删除资源 | 是 |

### 2.2 请求头

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}
X-Request-Id: {uuid}          # 请求追踪ID
X-Tenant-Id: {tenantId}       # 租户ID（多租户）
```

### 2.3 查询参数

```
# 分页
GET /api/v1/users?page=1&size=20

# 排序
GET /api/v1/users?sort=createdAt&order=desc

# 过滤
GET /api/v1/users?status=active&role=admin

# 字段选择
GET /api/v1/users?fields=id,username,email

# 搜索
GET /api/v1/users?keyword=张三
```

### 2.4 请求体格式

```json
// 创建用户
POST /api/v1/users
{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "roleId": 1
}

// 批量操作
POST /api/v1/users/batch
{
  "userIds": [1, 2, 3],
  "action": "disable"
}
```

---

## 3. 响应规范

### 3.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 业务数据
  },
  "traceId": "abc123def456"
}
```

### 3.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | integer | 业务状态码 |
| message | string | 状态描述 |
| data | object/array/null | 业务数据 |
| traceId | string | 请求追踪ID |

### 3.3 成功响应

```json
// 单条数据
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "email": "zhangsan@example.com"
  }
}

// 列表数据
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "size": 20
  }
}

// 无数据返回
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 3.4 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      { "id": 1, "username": "user1" },
      { "id": 2, "username": "user2" }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "totalPages": 5
    }
  }
}
```

---

## 4. 错误响应规范

### 4.1 错误响应格式

```json
{
  "code": 40001,
  "message": "用户名已存在",
  "data": null,
  "traceId": "abc123def456",
  "errors": [
    {
      "field": "username",
      "message": "用户名已被注册"
    }
  ]
}
```

### 4.2 HTTP 状态码

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求成功 |
| 201 | 已创建 | POST 创建成功 |
| 204 | 无内容 | DELETE 成功 |
| 400 | 请求错误 | 参数校验失败 |
| 401 | 未认证 | 未登录或 Token 失效 |
| 403 | 禁止访问 | 无权限 |
| 404 | 未找到 | 资源不存在 |
| 409 | 冲突 | 资源已存在 |
| 422 | 无法处理 | 业务校验失败 |
| 429 | 请求过多 | 限流 |
| 500 | 服务器错误 | 系统异常 |

### 4.3 业务错误码

```java
public enum ErrorCode {
    // 通用错误 1xxxx
    SUCCESS(200, "成功"),
    SYSTEM_ERROR(10001, "系统错误"),
    PARAM_ERROR(10002, "参数错误"),
    
    // 用户模块 2xxxx
    USER_NOT_FOUND(20001, "用户不存在"),
    USER_DISABLED(20002, "用户已禁用"),
    USER_PASSWORD_ERROR(20003, "密码错误"),
    USER_USERNAME_EXISTS(20004, "用户名已存在"),
    
    // 订单模块 3xxxx
    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_STATUS_ERROR(30002, "订单状态错误"),
    ORDER_ALREADY_PAID(30003, "订单已支付"),
    
    // 支付模块 4xxxx
    PAY_AMOUNT_ERROR(40001, "支付金额错误"),
    PAY_CHANNEL_ERROR(40002, "支付渠道错误");
    
    private final int code;
    private final String message;
}
```

### 4.4 错误码分类

| 范围 | 模块 |
|------|------|
| 10xxx | 通用错误 |
| 20xxx | 用户模块 |
| 30xxx | 订单模块 |
| 40xxx | 支付模块 |
| 50xxx | 库存模块 |
| 60xxx | CRM 模块 |
| 70xxx | ERP 模块 |

---

## 5. 接口安全

### 5.1 认证方式

```http
# Bearer Token 认证
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# API Key 认证（服务间调用）
X-Api-Key: your-api-key
```

### 5.2 权限控制

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping
    @SaCheckPermission("user:list")
    public Result<Page<UserVO>> list(UserQuery query) {
        // ...
    }
    
    @PostMapping
    @SaCheckPermission("user:create")
    public Result<UserVO> create(@Valid @RequestBody UserForm form) {
        // ...
    }
}
```

### 5.3 参数校验

```java
public record UserForm(
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度3-20位")
    String username,
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    String email,
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    String phone
) {}
```

### 5.4 请求限流

```java
@RestController
public class UserController {
    
    @GetMapping("/api/v1/users")
    @RateLimiter(value = 100, timeout = 1) // 每秒100次
    public Result<Page<UserVO>> list(UserQuery query) {
        // ...
    }
}
```

---

## 6. 接口示例

### 6.1 用户模块

#### 获取用户列表

```http
GET /api/v1/users?page=1&size=20&status=active

Authorization: Bearer {token}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "username": "zhangsan",
        "email": "zhangsan@example.com",
        "phone": "138****8000",
        "status": "active",
        "createdAt": "2026-03-27T10:00:00Z"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100
    }
  }
}
```

#### 创建用户

```http
POST /api/v1/users

Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "lisi",
  "email": "lisi@example.com",
  "phone": "13900139000",
  "roleId": 2
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "username": "lisi",
    "email": "lisi@example.com",
    "status": "active",
    "createdAt": "2026-03-27T10:30:00Z"
  }
}
```

### 6.2 订单模块

#### 创建订单

```http
POST /api/v1/orders

Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ],
  "addressId": 1,
  "remark": "尽快发货"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": "ORD202603270001",
    "totalAmount": 299.00,
    "status": "pending_payment",
    "paymentUrl": "https://pay.example.com/ORD202603270001"
  }
}
```

---

## 7. API 文档

### 7.1 OpenAPI 规范

使用 Knife4j（基于 Swagger）生成 API 文档：

```java
@RestController
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {
    
    @Operation(summary = "获取用户列表", description = "分页查询用户列表")
    @Parameters({
        @Parameter(name = "page", description = "页码", example = "1"),
        @Parameter(name = "size", description = "每页数量", example = "20")
    })
    @GetMapping("/api/v1/users")
    public Result<Page<UserVO>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        // ...
    }
}
```

### 7.2 访问地址

```
开发环境：http://localhost:8080/doc.html
测试环境：https://api-test.aiready.cn/doc.html
生产环境：https://api.aiready.cn/doc.html（需权限）
```

---

## 8. SDK 示例

### 8.1 JavaScript/TypeScript

```typescript
import request from '@/utils/request'

export const userApi = {
  getList: (params: UserQuery) => 
    request.get('/api/v1/users', { params }),
  
  getDetail: (id: number) => 
    request.get(`/api/v1/users/${id}`),
  
  create: (data: UserForm) => 
    request.post('/api/v1/users', data),
  
  update: (id: number, data: Partial<UserForm>) => 
    request.put(`/api/v1/users/${id}`, data),
  
  delete: (id: number) => 
    request.delete(`/api/v1/users/${id}`)
}
```

### 8.2 Java

```java
@Service
public class UserClient {
    
    private final RestTemplate restTemplate;
    
    public Page<UserVO> getUserList(UserQuery query) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v1/users")
            .queryParam("page", query.getPage())
            .queryParam("size", query.getSize())
            .toUriString();
        
        ResponseEntity<ApiResponse<Page<UserVO>>> response = 
            restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {});
        
        return response.getBody().getData();
    }
}
```

---

## 9. 性能规范

### 9.1 响应时间要求

| 接口类型 | 响应时间要求 |
|----------|--------------|
| 查询接口 | < 200ms |
| 列表接口 | < 500ms |
| 写入接口 | < 300ms |
| 批量接口 | < 1s |

### 9.2 数据量限制

```json
// 分页限制
{
  "page": 1,
  "size": 20,  // 最大 100
}

// 批量操作限制
{
  "ids": [1, 2, 3],  // 最多 100 个
  "action": "delete"
}
```

---

## 10. 变更管理

### 10.1 接口变更原则

1. **向后兼容**：新增字段不影响旧版本
2. **废弃通知**：旧接口标记 `@Deprecated`，提前通知
3. **版本升级**：破坏性变更升级版本号

### 10.2 变更记录

```markdown
## v1.1.0 (2026-03-27)

### 新增
- POST /api/v1/users/batch - 批量创建用户
- GET /api/v1/users/export - 导出用户列表

### 修改
- GET /api/v1/users 响应新增 roleIds 字段

### 废弃
- GET /api/v1/user/list (请使用 GET /api/v1/users)

### 移除
- POST /api/v1/user/add (已废弃超过 6 个月)
```

---

## 11. 参考资料

- [RESTful API Design Guide](https://restfulapi.net/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Google API Design Guide](https://cloud.google.com/apis/design)

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer