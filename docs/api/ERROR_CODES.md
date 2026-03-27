# 智企连·AI-Ready 错误码规范

**版本**: v1.0  
**日期**: 2026-03-27  
**项目**: 智企连·AI-Ready

---

## 1. 错误码设计原则

### 1.1 错误码结构

```
错误码 = 模块编码 + 业务编码

格式: MMMBB
  MMM  - 模块编码（3位）
  BB   - 业务编码（2位）

示例: 20001
  200 - 用户模块
  01  - 用户不存在
```

### 1.2 错误码分类

| 范围 | 模块 | 编码 |
|------|------|------|
| 000xx | 通用错误 | 000 |
| 100xx | 认证授权 | 100 |
| 200xx | 用户模块 | 200 |
| 300xx | 订单模块 | 300 |
| 400xx | 支付模块 | 400 |
| 500xx | 库存模块 | 500 |
| 600xx | CRM 模块 | 600 |
| 700xx | ERP 模块 | 700 |
| 800xx | 系统配置 | 800 |
| 900xx | 第三方服务 | 900 |

---

## 2. 错误码详细定义

### 2.1 通用错误 (000xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 00000 | 成功 | 200 | 操作成功 |
| 00001 | 系统繁忙，请稍后重试 | 500 | 系统异常 |
| 00002 | 参数错误 | 400 | 请求参数校验失败 |
| 00003 | 请求方式不支持 | 405 | HTTP 方法错误 |
| 00004 | 请求频率过高 | 429 | 触发限流 |
| 00005 | 服务暂时不可用 | 503 | 服务维护中 |
| 00006 | 资源不存在 | 404 | 请求路径错误 |
| 00007 | 请求超时 | 408 | 请求处理超时 |

### 2.2 认证授权错误 (100xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 10001 | 未登录或登录已过期 | 401 | Token 无效或过期 |
| 10002 | 无访问权限 | 403 | 权限不足 |
| 10003 | 账号已被禁用 | 403 | 账号状态异常 |
| 10004 | 账号已被锁定 | 403 | 登录失败次数过多 |
| 10005 | Token 无效 | 401 | Token 格式错误 |
| 10006 | Token 已过期 | 401 | 需要刷新 Token |
| 10007 | 登录凭证错误 | 401 | 用户名或密码错误 |
| 10008 | 验证码错误 | 400 | 图形验证码不正确 |
| 10009 | 验证码已过期 | 400 | 验证码超时 |
| 10010 | 设备未授权 | 403 | 新设备需要验证 |

### 2.3 用户模块错误 (200xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 20001 | 用户不存在 | 404 | 用户 ID 或账号不存在 |
| 20002 | 用户名已存在 | 409 | 用户名已被注册 |
| 20003 | 手机号已注册 | 409 | 手机号已绑定其他账号 |
| 20004 | 邮箱已注册 | 409 | 邮箱已绑定其他账号 |
| 20005 | 密码错误 | 400 | 旧密码验证失败 |
| 20006 | 密码强度不足 | 400 | 新密码不符合要求 |
| 20007 | 原密码与新密码相同 | 400 | 修改密码时新旧密码相同 |
| 20008 | 用户状态异常 | 400 | 用户被禁用或锁定 |
| 20009 | 用户信息不完整 | 400 | 必填信息未填写 |
| 20010 | 头像上传失败 | 500 | 文件存储异常 |
| 20011 | 实名认证失败 | 400 | 身份信息验证不通过 |
| 20012 | 手机验证码错误 | 400 | 短信验证码不正确 |

### 2.4 订单模块错误 (300xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 30001 | 订单不存在 | 404 | 订单 ID 不存在 |
| 30002 | 订单状态错误 | 400 | 当前状态不允许此操作 |
| 30003 | 订单已取消 | 400 | 订单已被取消 |
| 30004 | 订单已支付 | 400 | 订单已完成支付 |
| 30005 | 订单已发货 | 400 | 订单已发货无法修改 |
| 30006 | 订单已完成 | 400 | 订单已完成无法取消 |
| 30007 | 订单超时未支付 | 400 | 订单已自动取消 |
| 30008 | 商品库存不足 | 400 | 商品数量不够 |
| 30009 | 商品已下架 | 400 | 商品不可购买 |
| 30010 | 价格已变更 | 400 | 商品价格发生变化 |
| 30011 | 优惠卷不可用 | 400 | 优惠卷不满足使用条件 |
| 30012 | 优惠卷已过期 | 400 | 优惠卷已超过有效期 |
| 30013 | 订单金额异常 | 400 | 订单金额计算错误 |

### 2.5 支付模块错误 (400xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 40001 | 支付渠道错误 | 400 | 不支持的支付方式 |
| 40002 | 支付金额错误 | 400 | 支付金额与订单不符 |
| 40003 | 支付超时 | 408 | 支付请求超时 |
| 40004 | 支付失败 | 500 | 支付渠道返回错误 |
| 40005 | 退款失败 | 500 | 退款请求被拒绝 |
| 40006 | 退款金额超限 | 400 | 退款金额超过可退金额 |
| 40007 | 不可退款 | 400 | 订单状态不允许退款 |
| 40008 | 重复支付 | 400 | 订单已完成支付 |
| 40009 | 支付密码错误 | 400 | 支付密码验证失败 |
| 40010 | 余额不足 | 400 | 账户余额不够 |

### 2.6 库存模块错误 (500xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 50001 | 商品不存在 | 404 | 商品 ID 不存在 |
| 50002 | 商品已下架 | 400 | 商品不可操作 |
| 50003 | 库存不足 | 400 | 商品库存不够 |
| 50004 | 仓库不存在 | 404 | 仓库 ID 不存在 |
| 50005 | 入库单不存在 | 404 | 入库单不存在 |
| 50006 | 出库单不存在 | 404 | 出库单不存在 |
| 50007 | 库存锁定失败 | 400 | 库存已被其他订单锁定 |
| 50008 | 库存数量错误 | 400 | 库存数量不正确 |

### 2.7 CRM 模块错误 (600xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 60001 | 客户不存在 | 404 | 客户 ID 不存在 |
| 60002 | 客户已存在 | 409 | 客户信息重复 |
| 60003 | 线索不存在 | 404 | 线索 ID 不存在 |
| 60004 | 线索已转化 | 400 | 线索已转为客户 |
| 60005 | 商机不存在 | 404 | 商机 ID 不存在 |
| 60006 | 商机已关闭 | 400 | 商机已结束 |
| 60007 | 跟进记录不存在 | 404 | 跟进记录不存在 |

### 2.8 ERP 模块错误 (700xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 70001 | 供应商不存在 | 404 | 供应商 ID 不存在 |
| 70002 | 采购单不存在 | 404 | 采购单不存在 |
| 70003 | 采购单状态错误 | 400 | 采购单状态不允许操作 |
| 70004 | 销售单不存在 | 404 | 销售单不存在 |
| 70005 | 财务科目不存在 | 404 | 会计科目不存在 |
| 70006 | 发票信息错误 | 400 | 发票信息校验失败 |

### 2.9 系统配置错误 (800xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 80001 | 配置项不存在 | 404 | 配置项未定义 |
| 80002 | 配置值格式错误 | 400 | 配置值不符合格式要求 |
| 80003 | 字典项不存在 | 404 | 字典项未定义 |
| 80004 | 角色不存在 | 404 | 角色 ID 不存在 |
| 80005 | 菜单不存在 | 404 | 菜单 ID 不存在 |
| 80006 | 部门不存在 | 404 | 部门 ID 不存在 |

### 2.10 第三方服务错误 (900xx)

| 错误码 | 错误信息 | HTTP 状态码 | 说明 |
|--------|----------|-------------|------|
| 90001 | 短信发送失败 | 500 | 短信服务异常 |
| 90002 | 邮件发送失败 | 500 | 邮件服务异常 |
| 90003 | 文件上传失败 | 500 | 存储服务异常 |
| 90004 | 文件下载失败 | 500 | 存储服务异常 |
| 90005 | 支付渠道异常 | 500 | 第三方支付服务异常 |
| 90006 | OCR识别失败 | 500 | OCR 服务异常 |
| 90007 | AI服务异常 | 500 | AI 服务不可用 |

---

## 3. 错误响应格式

### 3.1 标准格式

```json
{
  "code": 20001,
  "message": "用户不存在",
  "data": null,
  "traceId": "abc123def456",
  "timestamp": "2026-03-27T10:30:00Z"
}
```

### 3.2 带详细错误

```json
{
  "code": 00002,
  "message": "参数错误",
  "data": null,
  "traceId": "abc123def456",
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    },
    {
      "field": "email",
      "message": "邮箱格式不正确"
    }
  ]
}
```

### 3.3 带解决方案

```json
{
  "code": 10006,
  "message": "Token 已过期",
  "data": null,
  "traceId": "abc123def456",
  "solution": {
    "action": "refresh_token",
    "description": "请使用 refresh_token 重新获取 access_token",
    "refreshEndpoint": "/api/v1/auth/refresh"
  }
}
```

---

## 4. 错误码 Java 枚举

```java
package cn.aiedge.aiready.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {
    
    // ==================== 通用错误 ====================
    SUCCESS(0, "成功"),
    SYSTEM_ERROR(1, "系统繁忙，请稍后重试"),
    PARAM_ERROR(2, "参数错误"),
    METHOD_NOT_SUPPORTED(3, "请求方式不支持"),
    RATE_LIMITED(4, "请求频率过高"),
    SERVICE_UNAVAILABLE(5, "服务暂时不可用"),
    NOT_FOUND(6, "资源不存在"),
    REQUEST_TIMEOUT(7, "请求超时"),
    
    // ==================== 认证授权 ====================
    UNAUTHORIZED(10001, "未登录或登录已过期"),
    FORBIDDEN(10002, "无访问权限"),
    ACCOUNT_DISABLED(10003, "账号已被禁用"),
    ACCOUNT_LOCKED(10004, "账号已被锁定"),
    TOKEN_INVALID(10005, "Token 无效"),
    TOKEN_EXPIRED(10006, "Token 已过期"),
    LOGIN_FAILED(10007, "登录凭证错误"),
    CAPTCHA_ERROR(10008, "验证码错误"),
    CAPTCHA_EXPIRED(10009, "验证码已过期"),
    DEVICE_UNAUTHORIZED(10010, "设备未授权"),
    
    // ==================== 用户模块 ====================
    USER_NOT_FOUND(20001, "用户不存在"),
    USERNAME_EXISTS(20002, "用户名已存在"),
    PHONE_EXISTS(20003, "手机号已注册"),
    EMAIL_EXISTS(20004, "邮箱已注册"),
    PASSWORD_ERROR(20005, "密码错误"),
    PASSWORD_WEAK(20006, "密码强度不足"),
    PASSWORD_SAME(20007, "原密码与新密码相同"),
    USER_STATUS_ERROR(20008, "用户状态异常"),
    USER_INFO_INCOMPLETE(20009, "用户信息不完整"),
    AVATAR_UPLOAD_FAILED(20010, "头像上传失败"),
    REAL_NAME_FAILED(20011, "实名认证失败"),
    SMS_CODE_ERROR(20012, "手机验证码错误"),
    
    // ==================== 订单模块 ====================
    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_STATUS_ERROR(30002, "订单状态错误"),
    ORDER_CANCELED(30003, "订单已取消"),
    ORDER_PAID(30004, "订单已支付"),
    ORDER_SHIPPED(30005, "订单已发货"),
    ORDER_COMPLETED(30006, "订单已完成"),
    ORDER_TIMEOUT(30007, "订单超时未支付"),
    STOCK_NOT_ENOUGH(30008, "商品库存不足"),
    PRODUCT_OFF_SHELF(30009, "商品已下架"),
    PRICE_CHANGED(30010, "价格已变更"),
    COUPON_NOT_AVAILABLE(30011, "优惠卷不可用"),
    COUPON_EXPIRED(30012, "优惠卷已过期"),
    ORDER_AMOUNT_ERROR(30013, "订单金额异常"),
    
    // ==================== 支付模块 ====================
    PAY_CHANNEL_ERROR(40001, "支付渠道错误"),
    PAY_AMOUNT_ERROR(40002, "支付金额错误"),
    PAY_TIMEOUT(40003, "支付超时"),
    PAY_FAILED(40004, "支付失败"),
    REFUND_FAILED(40005, "退款失败"),
    REFUND_AMOUNT_ERROR(40006, "退款金额超限"),
    REFUND_NOT_ALLOWED(40007, "不可退款"),
    PAY_DUPLICATED(40008, "重复支付"),
    PAY_PASSWORD_ERROR(40009, "支付密码错误"),
    BALANCE_NOT_ENOUGH(40010, "余额不足");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

---

## 5. 错误处理最佳实践

### 5.1 业务异常抛出

```java
// 使用预定义错误码
public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
}

// 使用自定义消息
public void checkPassword(String rawPassword, String encodedPassword) {
    if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
        throw new BusinessException(ErrorCode.PASSWORD_ERROR, "原密码错误，请重新输入");
    }
}
```

### 5.2 参数校验异常

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        
        List<ErrorDetail> errors = fieldErrors.stream()
            .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
        
        return Result.fail(ErrorCode.PARAM_ERROR, errors);
    }
}
```

### 5.3 错误日志记录

```java
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception e, HttpServletRequest request) {
    String traceId = MDC.get("traceId");
    
    log.error("系统异常 - traceId: {}, uri: {}, message: {}", 
        traceId, request.getRequestURI(), e.getMessage(), e);
    
    return Result.fail(ErrorCode.SYSTEM_ERROR);
}
```

---

## 6. 前端错误处理

### 6.1 统一处理

```typescript
// utils/request.ts
request.interceptors.response.use(
  (response) => {
    const { code, message, data } = response.data
    
    if (code === 0) {
      return data
    }
    
    // 业务错误处理
    const errorHandler: Record<number, () => void> = {
      10001: () => router.push('/login'),
      10006: () => refreshToken(),
      10002: () => message.error('无访问权限'),
    }
    
    const handler = errorHandler[code]
    if (handler) {
      handler()
    } else {
      message.error(message || '请求失败')
    }
    
    return Promise.reject(response.data)
  }
)
```

### 6.2 错误码映射

```typescript
// constants/errorCode.ts
export const ERROR_MESSAGES: Record<number, string> = {
  20001: '用户不存在',
  20002: '用户名已存在',
  30001: '订单不存在',
  // ...
}

export function getErrorMessage(code: number): string {
  return ERROR_MESSAGES[code] || '操作失败，请稍后重试'
}
```

---

**文档更新**: 2026-03-27  
**维护者**: doc-writer