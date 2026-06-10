# B2B 订货商城 - 微信小程序配置文档

## 1. 概述

本文档说明 B2B 订货商城用户端（H5/小程序）的微信小程序接入配置。

### 1.1 技术架构

```
小程序/公众号 ──→ Nginx ──→ MallAuthController (认证)
                          ──→ MallController (商品/订单)
                          ──→ 标准 ERP 接口 (需授权)
```

- **前端**: uni-app 构建，编译为 H5 / 微信小程序
- **后端**: Spring Boot + Sa-Token + MyBatis-Plus
- **认证**: 小程序 code 登录 / 账号密码登录，签发 Sa-Token

---

## 2. 微信小程序注册与配置

### 2.1 注册小程序

1. 前往 [微信公众平台](https://mp.weixin.qq.com/) 注册小程序账号
2. 完成主体信息认证（个人/企业）
3. 获取 **AppID** 和 **AppSecret**

### 2.2 白名单配置

在小程序后台「开发 → 开发设置」配置：

| 类别 | 配置项 | 说明 |
|------|--------|------|
| **request 合法域名** | `https://api.yourdomain.com` | 后端 API 域名 |
| **socket 合法域名** | `wss://api.yourdomain.com` | WebSocket（如有） |
| **uploadFile 合法域名** | `https://api.yourdomain.com` | 文件上传 |
| **downloadFile 合法域名** | `https://api.yourdomain.com` | 文件下载 |
| **业务域名** | `https://h5.yourdomain.com` | H5 WebView 嵌入 |

### 2.3 服务器 IP 白名单

在小程序后台「开发 → 开发设置 → IP 白名单」中添加后端服务器的公网 IP，小程序服务端 API 调用（如 code2session）仅允许白名单内 IP。

---

## 3. 后端配置

### 3.1 application.yml 配置

```yaml
mall:
  miniapp:
    app-id: wx1234567890abcdef    # 小程序 AppID
    app-secret: your-app-secret   # 小程序 AppSecret
    token: your-token             # 消息校验 Token（可选）
    aes-key:                      # 消息加解密 Key（可选）
  h5:
    base-url: https://h5.yourdomain.com  # H5 域名
```

### 3.2 Sa-Token 排除路径

已配置的排除路径（`SaTokenConfig.java`）：

```java
// 商城认证接口（无需登录）
"/api/v1/mall/auth/**",
// 商城公开接口（无需登录）
"/api/v1/mall/public/**"
```

### 3.3 Redis 配置

小程序登录使用 code2session 换取 openId，建议使用 Redis 缓存 session_key：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 5000
```

---

## 4. 微信支付配置

### 4.1 开通条件

- 已完成微信认证的小程序
- 已开通微信支付商户号

### 4.2 商户号绑定

1. 微信支付商户平台 → 产品中心 → 小程序支付 → 关联 AppID
2. 配置支付回调域名

### 4.3 后端支付配置

```yaml
wx:
  pay:
    app-id: wx1234567890abcdef       # 小程序 AppID
    mch-id: 1234567890               # 商户号
    key: your-api-key                # API v2 密钥
    cert-path: /path/to/apiclient_cert.p12  # 证书路径（退款需要）
    notify-url: https://api.yourdomain.com/api/v1/mall/pay/notify  # 支付回调
    refund-notify-url: https://api.yourdomain.com/api/v1/mall/pay/refund-notify
```

### 4.4 支付流程

```
小程序端 → 后端统一下单 → 微信支付 → 支付回调 → 更新订单状态
  1. 用户点击"支付"
  2. 后端调用微信统一下单 API，获取 prepay_id
  3. 后端签名返回给小程序端
  4. 小程序端调起 wx.requestPayment
  5. 用户输入密码完成支付
  6. 微信异步通知后端回调接口
  7. 后端更新订单状态为"待审核"/"已付款"
```

---

## 5. 小程序前端项目结构

### 5.1 技术选型

- **框架**: uni-app (Vue 3 + Vite)
- **UI 组件**: uView Plus / 自研组件
- **状态管理**: Pinia
- **请求封装**: axios / uni-request

### 5.2 目录结构建议

```
src/
├── api/                  # API 接口
│   ├── auth.ts           # 认证相关
│   ├── product.ts        # 商品
│   ├── cart.ts           # 购物车
│   ├── order.ts          # 订单
│   └── user.ts           # 用户
├── components/           # 公共组件
├── pages/                # 页面
│   ├── index/            # 首页
│   ├── category/         # 分类
│   ├── product/          # 商品详情
│   ├── cart/             # 购物车
│   ├── order/            # 订单列表/详情
│   └── user/             # 个人中心
├── stores/               # Pinia stores
├── utils/                # 工具
│   ├── request.ts        # 请求封装
│   └── auth.ts           # 鉴权工具
└── App.vue
```

### 5.3 Token 存储与传递

```typescript
// 登录后存储 token
uni.setStorageSync('token', res.data.token)

// 请求拦截器附加 token
axios.interceptors.request.use(config => {
  const token = uni.getStorageSync('token')
  if (token) config.headers['Sa-Token'] = token
  return config
})
```

---

## 6. 模板消息配置

### 6.1 选用订阅消息

小程序模板消息已下线，改用**订阅消息**：

1. 小程序后台 → 功能 → 订阅消息 → 选用模板
2. 选用合适的模板（订单通知、审核结果等）

### 6.2 推送时机

| 场景 | 模板类型 | 触发时机 |
|------|----------|----------|
| 订单提交成功 | 订单通知 | 用户提交订单后 |
| 订单审核通过 | 审核结果通知 | 管理员审核通过 |
| 订单审核驳回 | 审核结果通知 | 管理员驳回（附原因） |
| 订单发货 | 物流通知 | 管理员标记发货 |

### 6.3 后端推送

```java
// 调用微信订阅消息推送
wxMaTemplateService.sendSubscribeMsg(
    WxMaSubscribeMessage.builder()
        .toUser(openId)
        .templateId(templateId)
        .data(...)
        .build()
);
```

---

## 7. 环境部署

### 7.1 H5 部署

```bash
# 构建 H5
npm run build:h5

# 部署到 Nginx
scp -r dist/build/h5/* user@server:/var/www/h5/
```

### 7.2 小程序上传

```bash
# 构建小程序
npm run build:mp-weixin

# 使用微信开发者工具上传
# 或使用 CI (miniprogram-ci)
```

### 7.3 Nginx 配置示例

```nginx
# H5 前端
server {
    listen 443 ssl;
    server_name h5.yourdomain.com;

    root /var/www/h5;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}

# API 反向代理
server {
    listen 443 ssl;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 8. 安全注意事项

| 风险 | 防范措施 |
|------|----------|
| openId 泄露 | 不返回前端明文 openId，仅服务端使用 |
| 支付参数篡改 | 统一下单参数由服务端生成，前端只调起支付 |
| 接口频率限制 | 登录接口限制频率，防止暴力破解 |
| 数据越权 | Sa-Token 鉴权 + 租户隔离（tenant_id） |
| 敏感操作日志 | 审核、修改密码等操作记录操作日志 |

---

## 9. 相关文档

- [微信小程序开发文档](https://developers.weixin.qq.com/miniprogram/dev/framework/)
- [微信支付开发文档](https://pay.weixin.qq.com/wiki/doc/apiv3/)
- [uni-app 跨平台开发](https://uniapp.dcloud.net.cn/)
- [Sa-Token 文档](https://sa-token.cc/)
