# 商城 App 设计方案（订货商城 C 端）

> **状态**：首版方案，待评审（2026-09-26）
> **载体**：`frontend/apps/mobile-mall`（Vue 3.4 + Vite 5 + **Vant 4** + Pinia + PWA，独立 dev 端口 `3002`）
> **后端面**：现有 `/api/v1/mall/{auth,cart,orders,party-link,payments,products,user}` 7 族 **49 个端点**（auth 6 / cart 8 / orders 6 / party-link 11 / payments 3 / products 8 / user 7）
> **交互范式对标**：**手机淘宝**（顶部横向二级 Tab 吸顶）+ 4 张同行业订货 App 实拍截图（餐饮食材 / 餐饮货仓 / 生鲜果蔬）
> **前置阅读**：`../../TRADE_MODULE_AUDIT_20260923.md`（商城现状、5 个 P0）、`../交易模块/README.md`（菜单与页面口径）、`../交易模块/_开发指南-金标准.md`
> **已定口径（2026-09-26 用户拍板）**：① 一期只做 H5/PWA（小程序留二期）｜② **支付先接支付宝 + 银联**（2026-09-26 深夜已真接，见文末第四批；微信留二期）｜③ **商品分组 = 商品分类**，直接接 `erp_product_category`，不新造"分组"概念｜④ **商品标签 = 分类页最顶部的标签 Tab**（`erp_mall_tag`）。
> **一句话定位**：**一个 App、两种买家、价格按认证分级** —— 企业客户（`erp_sale_order.order_source=2`）与个人会员（`=3`）共用一套界面，靠会话激活身份切换。

---

## 一、定位与设计原则

### 1.1 客群与场景

从 4 张参考截图看，这类订货 App 的真实客群高度集中在**餐饮食材 / 生鲜果蔬批发**：早餐面点、调料副食、冻品、鲜肉、酒水饮料。据此确定本 App 的**场景假设**：

| 场景特征 | 对设计的约束 |
|---|---|
| 买家是**门店采购/店长**，边干活边下单 | 大字号、大点击区、下单路径 ≤3 步、支持"常购清单"一键复购 |
| 采购频次高（日/周） | 首页要能"直达上次买的"，购物车要按供应商/仓库分组 |
| **价格是商业机密**，非认证客户不能看 | 三态价格 + 底部提示条（本方案的灵魂，见 §七） |
| 下单走**账期/挂账**，不是先付款 | 确认订单页要展示账期与额度，支付方式含"挂账" |
| 议价按**斤/箱/件**，且常有"净重 vs 毛重" | 商品卡与详情要显示计量单位、拆零单位、单位换算 |

### 1.2 四条设计原则

1. **能配置的绝不写死**：`tenant_shop_config` 现有 **82 个字段**，审计发现**只有 4 个被运行期读取**。本方案的硬性目标 —— 每接一个 C 端页面，就把对应字段从"假开关"变成真开关（§五给出逐字段落位表）。
2. **一屏一决策**：首页只回答"买什么"，分类只回答"找什么"，购物车只回答"这单多少钱"。
3. **未认证不是错误，是一种状态**：游客/待审买家看到的应是**引导**（去登录/去认证），不是 400/403 报错页。
4. **复用现有组件与契约**：Vant 4 组件优先；接口优先复用已就绪的 52 个端点，**只补真正缺的**（§六）。

---

## 二、信息架构

### 2.1 底部 5 个主 Tab（已定）

| # | Tab | Vant 图标 | 角标 | 路由 | 说明 |
|:-:|---|---|---|---|---|
| 1 | **首页** | `wap-home-o` | — | `/` | 顶部二级 Tab：推荐（默认）/ 新品 / 活动 |
| 2 | **分类** | `apps-o` | — | `/category` | 顶部二级 Tab：全部 / 各商品标签 |
| 3 | **消息** | `chat-o` | **未读数** | `/message` | 顶部二级 Tab：客服（默认）/ 消息 |
| 4 | **购物车** | `shopping-cart-o` | **件数**（>99 → `99+`） | `/cart` | 无二级 Tab |
| 5 | **我的** | `user-o` | — | `/user` | 无二级 Tab；**订单并入此处**（五宫格 + 二级列表） |

> **与现有代码的差异**：`App.vue` 现为 4 个 Tab（首页/分类/购物车/我的），需新增「消息」并调整为 5 个；`TabBar.vue` 里写死的 `index === 2` 购物车角标要改为**按 key 判断**，并新增消息未读角标。

### 2.2 顶部二级 Tab（吸顶，淘宝式 —— 本方案的核心范式）

参考截图 4（手机淘宝）顶部「关注 / 推荐 / 闪购 / 国补 / 飞猪 / 家装节」这一行。规范：

- **位置**：一级页内容区顶部，随页面滚动**吸顶**（`van-sticky`），滚动时标题栏渐显。
- **交互**：横向可滑；选中项**自动居中**；选中态为主题色粗体 + 2px 下划线（下划线宽度=文字宽度）。
- **切换**：**不重建页面**（`keep-alive` + 每个 Tab 独立滚动位置与分页游标），只换内容区。
- **下拉刷新**：只刷新当前 Tab 的内容，不整页 reload。
- **可配置**：首页/分类的 Tab 集合由后台决定（分类页 = 商品标签 `erp_mall_tag`，见 `F-03`），不写死在前端。

| 主 Tab | 顶部二级 Tab | 默认 | Tab 集合从哪来 |
|---|---|---|---|
| 首页 | 推荐 / 新品 / 热销 | **推荐** | 三组均为**已就绪**接口：`/products/recommendations`、`/products`（按 `mall_sort_type`）、`/products/hot`。原计划的「活动」需促销数据源（营销域无 C 端接口），**顺延至二期** |
| 分类 | **全部 / 各商品标签**（标签栏位于分类页**最顶部**） | **全部** | `erp_mall_tag`（现有 20 条，`tag_code` 形如 `TAG_n`；取 `status=1`） |
| 消息 | 客服 / 消息 | **客服** | 固定两项 |
| 购物车 / 我的 | — | — | — |

### 2.3 完整页面清单（路由表）

**一级（Tab）页 5 个**

| 路由 | 组件（新增/改造） | 页面 |
|---|---|---|
| `/` | 改造 `views/home/index.vue` | 首页（含顶部 Tab 容器） |
| `/category` | 改造 `views/category/index.vue` | 分类（顶部 Tab + 分类树/宫格） |
| `/message` | **新增** `views/message/index.vue` | 消息（客服 / 消息双 Tab） |
| `/cart` | 改造 `views/cart/index.vue` | 购物车（按供应商/仓库分组） |
| `/user` | 改造 `views/user/index.vue` | 我的（身份卡 + 订单五宫格 + 资产 + 服务） |

**二级页 24 个**

| 分组 | 路由 | 现状 | 页面 |
|---|---|---|---|
| 商品 | `/product/:id` | ✅ 已有 | 商品详情 |
| | `/search` | ✅ 已有 | 搜索（含热词 ← 关键词库） |
| | `/category/:id` | ✅ 已有 | 分类商品列表 |
| | `/favorites` | **新增** | 常购清单（← 订单历史聚合，B2B 高频） |
| 交易 | `/order` | ✅ 已有 | 确认订单（含运费/自提/起送门槛） |
| | `/order/list` | ✅ 已有 | 订单列表（顶部二级 Tab：全部/待付款/待发货/待收货/已完成/售后） |
| | `/order/:id` | ✅ 已有 | 订单详情（补物流轨迹） |
| | `/pay/:orderId` | **新增** | 支付（跳渠道/小程序唤起/挂账确认） |
| | `/pay/result` | **新增** | 支付结果 |
| | `/after-sales/apply` | **新增** | 售后申请（退货/换货/退款） |
| | `/after-sales/list` | **新增** | 售后列表 |
| 账户 | `/login`、`/register` | ✅ 已有 | 登录 / 注册（含注册协议、默认级别/分类） |
| | `/user/profile` | ✅ 已有 | 个人资料 |
| | `/user/address` | ✅ 已有 | 收货地址簿 |
| | `/user/identity` | **新增** | 身份切换（个人会员 ⇄ 企业客户） |
| | `/user/enterprise` | **新增** | 企业关联申请（`/party-link/*` 双向审核） |
| | `/user/coupon` | **新增** | 我的优惠券 |
| | `/user/points` | **新增** | 我的积分 |
| | `/user/invoice` | **新增** | 发票抬头 |
| | `/user/settings` | **新增** | 设置（消息订阅/关于/协议/退出） |
| 店铺 | `/shop/about` | **新增** | 店铺资质与联系方式（← `qualifications`/`contacts`/`official_qr_code`） |
| | `/notice` | **新增** | 公告列表/详情（← `mall_notice`） |
| | `/notice/:id` | **新增** | 公告详情 |

---

## 三、参考截图研读：采纳 / 改造 / 不采纳

### 图 1 · 餐饮食材订货（蓝调，5 Tab：首页/分类/**常购清单**/购物车/我的）

| 元素 | 结论 | 说明 |
|---|---|---|
| 顶部「定位 + 店铺」+ 小程序胶囊 | **改造采纳** | 定位改为**店铺切换/仓库切换**（B2B 无附近概念） |
| 搜索框 + 搜索按钮（占位=热词） | **采纳** | 占位文案取 `mall_keyword` 第一条；热词库终于有消费方 |
| 搜索热词两行（鸡肉/猪肉/酒水饮料…+「全部⌄」） | **采纳** | 直接 ← `mall_keyword`（现 35 条、`keyword_type=1` 热词） |
| 「限时秒杀」+ 更多抢购 | **二期** | 依赖促销引擎（营销域），一期不做，但**预留位** |
| 行业图标宫格 2×5 + 页码点 | **采纳** | 见 §4.2；行业分类标签 ← `erp_product_category` 一级 / `industry_category` |
| 商品分组 Tab（精品推荐/行业热销/新品上架/省钱专区/网红产品） | **改造采纳** | 收敛为首页顶部二级 Tab（推荐/新品/活动），不做两排 Tab 叠罗汉 |
| 双列瀑布流商品卡 | **采纳** | 见 §4.1 商品卡字段表 |
| 商品卡「荐」角标 / 「HOT 热卖」角标 | **采纳** | ← `erp_product.mall_tags`（逗号分隔槽位码）→ `erp_mall_tag.tag_code`（TAG_1…TAG_20） |
| 商品卡红底卖点条「3成熟、已处理干净、出9成」 | **采纳** | ← `erp_product.mall_description` 首行（截断 30 字） |
| **底部固定条「登录认证后可查看商品价格」+ 去登录** | **强烈采纳** | 本方案的标志性交互，见 §七 |
| 右下角浮动客服按钮 | **采纳** | 与底部「消息」Tab 二选一：**保留底部 Tab，去掉浮动球**（避免遮挡商品） |
| 底部第 3 个 Tab =「常购清单」 | **改造** | 你已定第 3 个 Tab 为「消息」⇒ 常购清单**降级**为首页顶部入口 + 购物车页内入口 +「我的」服务项 |

### 图 2 · 餐饮货仓（绿调，5 Tab：首页/**商品**/购物车/订单/我的）

| 元素 | 结论 | 说明 |
|---|---|---|
| 标题栏 = 店铺名居中 + 返回首页按钮 | **采纳** | 与图 1 的定位栏统一为「店铺名 + 切换 + 返回」 |
| 绿色搜索条 + **扫一扫** + 消息按钮 | **采纳** | 扫一扫用于扫码找货（条码 ← `erp_product.barcode`）；消息按钮与底部 Tab 重复，**去掉**（Tab 已有角标） |
| **纯图标宫格分类页**（4 列 × 5 行 + 分隔线分组） | **强烈采纳** | 这是 B2B 订货最省事的找货方式，见 §4.2「宫格/树双形态」 |
| 底部「订单」独立 Tab | **改造** | 按你的决定并入「我的」 |

### 图 3 · 启果果（绿调，4 Tab：首页/分类/购物车/个人中心）

| 元素 | 结论 | 说明 |
|---|---|---|
| 顶部「Logo + 店铺名 + **仓库切换**」 | **采纳（收敛）** | 一期只读展示 `default_warehouse_id` 对应仓库名；多仓切换二期做 |
| 活动 Banner「门店认证优惠 / 结算手续费 0.24%」+ 轮播点 + **「不感兴趣 ×」** | **采纳** | Banner ← `shop_banner`；"不感兴趣"用本地存储静默 7 天 |
| 5 列商品分组宫格（标题 + 图 + 「点击查看 >」） | **采纳** | **用户 2026-09-26 定：商品分组即商品分类**，直接用 `erp_product_category` 一级分类，不新造"分组"概念 |
| 双列分组入口卡（新鲜果蔬·近期热卖果蔬 / 休闲零食·热卖零食饮料） | **二期** | 属营销位，一期用商品分类宫格覆盖 |
| 商品卡显示**供应商名 + 近7日疯抢 703 + 净单价约 3.46元/斤** | **采纳** | 供应商 ← 商品关联往来单位；"疯抢 N" ← `mall_sales_count`；"净单价约 x元/斤" ← `quantity_scale` + `price_track_with_unit` 按计价单位换算 |

### 图 4 · 手机淘宝（5 Tab：淘/视频/**消息**/购物车/我的淘宝）

| 元素 | 结论 | 说明 |
|---|---|---|
| **顶部横向二级 Tab 吸顶**（关注/推荐/闪购/国补/…） | **强烈采纳（本方案范式）** | 见 §2.2 |
| 消息 Tab 带角标（79） | **采纳** | 未读数角标，与购物车角标并列 |
| 双列瀑布流、活动卡三连、促销条 | **部分采纳** | 瀑布流采纳；活动卡二期；促销条一期只做单条公告滚动 |
| 直播 / 内容化 / 百亿补贴 / 签到红包 | **不采纳** | 与 B2B 订货场景无关，避免界面噪音 |

---

## 四、页面级设计

### 4.1 首页（`/`）

**自上而下 8 个区块**

| # | 区块 | 数据来源 | 一期 |
|:-:|---|---|:-:|
| 1 | 标题栏：店铺 Logo/名 + **店铺切换** + 小程序胶囊 | `tenant_shop_config.shop_name/shop_logo` | ✅ |
| 2 | 搜索栏（占位=首条热词） | `mall_keyword` | ✅ |
| 3 | **顶部二级 Tab**：推荐 / 新品 / 热销 | `recommendations` / `products` / `hot` 三个已就绪接口 | ✅ |
| 4 | 公告滚动条 | `mall_notice`（status=1，notice_type=1） | ✅ |
| 5 | 轮播 + 开屏弹窗 | `shop_banner` / `mall_popup_ad`（`show_type=once` 每日一次；⚠️ 现有 18 条弹窗广告 `status=3` 均非发布态，需先确认真库 status 字典） | ✅ |
| 6 | **商品分类宫格**（5 列 × N 行） | `/v1/mall/products/categories`（`erp_product_category` 一级分类） | ✅ |
| 7 | 双列瀑布流商品 | `v_mall_product` | ✅ |
| 8 | **未认证时的底部固定提示条**「登录认证后可查看商品价格 · 去登录」 | `allow_guest` / `guest_show_price` | ✅ |

**商品卡字段口径（B2B 特色，逐项标注配置开关）**

| 位置 | 内容 | 来源 / 开关 |
|---|---|---|
| 左上角标 | 分类标签（如「预制菜」） | `erp_product.industry_category` |
| 右上角标 | 「荐」「HOT」等；最多 2 个 | `erp_product.mall_tags`（逗号分隔 `tag_code`）→ `erp_mall_tag`；开关 `display_list_fields` |
| 主图 | 商品图 | `erp_product.image_url`（无图用店铺默认图） |
| 卖点条 | 红底一行，截断 30 字 | `mall_description` 首行 |
| 名称 | 2 行截断 | `product_name` |
| 规格 | 如「3成熟 净重10kg/箱」 | `spec` + 单位显示（`unit_display`/`enable_split_unit`） |
| 计价 | 如「净单价约 3.46元/斤」 | `quantity_scale` + `price_track_with_unit` |
| 价格 | **三态**（见 §七） | `guest_show_price` / `buyer_hide_level` / `enable_retail_price` |
| 起订量 | 「N 起订」 | `mall_min_order_qty` |
| 库存 | 「库存 N」/「缺货」置灰 | `stock_display` / `out_of_stock_display` |
| 销量 | 「近7日疯抢 N」 | `mall_sales_count`，开关 `show_sales` |
| 供应商 | 供货单位名 | 商品 → 往来单位（B2B 特有） |

**排序与分页**：默认 `mall_sort_type` + `mall_sort_order`；瀑布流分页 20 条/页，触底加载。

### 4.2 分类（`/category`）

**双形态，由后台开关决定**（对应 `category_style`：纯文本 / 图文）

- **形态 A · 宫格**（默认，取自图 2）：一级分类以图标宫格平铺（4 列），分组间加分隔线；点图标进二级列表。适合分类 ≤30 个。
- **形态 B · 左树右列表**（分类多时）：左侧一级分类竖排，右侧商品列表，顶部二级 Tab = 商品标签。

**顶部二级 Tab**：`全部 / <各标签>` —— 标签取 `erp_mall_tag` 中 `status=1` 且已被在售商品使用（`mall_tags` 命中）的槽位，按 `sort_order`。

**其它开关落位**：分类默认排序 `category_default_sort`；显示商品数 `category_show_count`；是否启用商城分类体系 `enable_mall_category`；主推分类 `main_category`（宫格首屏置顶）。

### 4.3 消息（`/message`）—— 本 App 的特色页

**顶部二级 Tab：客服（默认）/ 消息**

**客服 Tab**（对接 AI + 企业微信客服）
- 一期：FAQ 快捷问（发货/账期/退换/发票）+ **AI 问答**（后端复用 `core/agent` 的 `AgentController` / `AgentInvokeController`；C 端需新增 `/api/v1/mall/chat/*` 薄封装，注入当前买家身份上下文：等级价、最近订单）→ 命中不了转人工。
- 二期：**企业微信客服**（扫码或 `wx.openCustomerServiceChat` 跳转）；会话记录落库以便质检。
- 落库建议：新建 `mall_chat_session` / `mall_chat_message`（避免污染 `sys_notification_record` 的"系统通知"语义）。

**消息 Tab**
- 类型：订单状态变更、审核结果、发货通知、活动/公告；数据源 `sys_notification_record`（**需新增按 `shop_user` 维度的 C 端端点**，现端点是按 `sys_user`）。
- 交互：未读置顶加粗、左滑已读、全部已读；未读数驱动底部 Tab 角标。

### 4.4 购物车（`/cart`）

- **按供应商 / 仓库分组**（多仓订货必须，否则不知道谁发货）。
- 行内：图片、名称、规格、单价（三态）、数量步进器（步长受 `quantity_scale` 约束）、小计、失效标记。
- **失效行**：已下架 / 已删除 / 库存不足 → 置灰 + 「找相似」，不参与结算（`out_of_stock_display` 控制是置灰还是直接隐藏）。
- **底部结算栏**：合计金额、起送门槛提示（`min_order_amount`：「还差 ¥N 起送」）、去结算。
- **"常购清单"入口**：右上角入口，按历史订单聚合出高频商品，一键加购（承接图 1 的常购清单能力）。

### 4.5 我的（`/user`）—— 订单并入此处

| 区块 | 内容 |
|---|---|
| **身份卡** | 头像 + 昵称 + **身份切换器**（个人会员 ⇄ 企业客户）；企业身份显示客户等级与账期/额度；未认证显示「去认证」 |
| **订单五宫格** | 待付款 / 待发货 / 待收货 / 已完成 / 售后 —— 每格带**数量角标**，点进 `/order/list?tab=xxx` |
| 我的资产 | 优惠券（`reg_give_coupon` 送券）、积分（`mall_points` 累计）、余额/账期额度 |
| 我的服务 | 收货地址、常购清单、企业关联（`/party-link`）、发票抬头、浏览足迹、店铺资质 |
| 设置 | 消息订阅（`message_subscribe`）、关于我们、用户协议（`register_agreement`）、退出登录 |

**订单列表页**（`/order/list`）顶部二级 Tab 与 §2.2 同款：全部 / 待付款 / 待发货 / 待收货 / 已完成 / 售后。状态映射见 §六 `状态口径`。

### 4.6 关键二级页要点

| 页面 | 要点 |
|---|---|
| **确认订单** `/order` | 收货地址（或**自提点** ← `self_delivery`/`enable_pickup`/`pickup_addresses`）；配送方式（快递 ← `logistics_methods` / 自提）；**运费试算**（`freight_amount` / `free_shipping_amount` / `freight_template`）；起送门槛 `min_order_amount`；支付方式（← `payment_methods` + `payment_scenes`，含"挂账"）；备注；提交 |
| **支付** `/pay/:orderId` | 调 `POST /v1/mall/payments` 拿渠道参数 → H5 跳转 / 小程序 `requestPayment` → 轮询 `GET /v1/mall/payments/{id}/status` → 结果页 |
| **商品详情** `/product/:id` | 主图轮播、价格三态、规格与单位换算、库存、**阶梯价/等级价**、起订量、图文详情（`display_detail_fields` 控制字段显隐）、加入购物车/立即下单、客服入口 |
| **搜索** `/search` | 热词（`mall_keyword`）、历史（本地）、结果排序、无结果推荐 |
| **售后申请** | 选订单 → 选类型（退货/换货/退款）→ 选商品与数量 → 原因与凭证 → 提交（后端对接 `/erp/sale/return-apply` 的 C 端封装，**待补**） |
| **企业关联** `/user/enterprise` | 提交申请（`POST /party-link/apply`）→ 企业侧审 → 租户侧审 → 通过后可切到企业身份下单；被驳回显示原因 |

---

## 五、配置落位表：后台字段 → C 端消费点 ⭐

> **这是本方案最有价值的部分**。`tenant_shop_config` 共 82 列，审计实测**运行期只读 4 个**（`allow_guest`/`guest_show_price`/`reg_audit_required`/`enable_auto_audit`），其余 78 个是"只写不读"的假开关。下表把每个字段指到 C 端**具体位置**，做完即激活。
> 图例：**✅ 已有消费方** ｜ **①** 一期 ｜ **②** 二期 ｜ **③** 三期

### 5.1 店铺主体与装修（商城设置 → 基础设置 / 店铺设置 / 商城装修）

| 字段 | C 端消费位置 | 期 |
|---|---|:-:|
| `shop_name` `shop_logo` `shop_desc` | 首页标题栏、店铺资质页、分享卡片 | ① |
| `theme_color` | 全局主题色（Vant `ConfigProvider` 主题变量） | ① |
| `banner_ids` | 首页轮播 | ① |
| `template_id` | 首页布局模板（装修模板库 14 套：办公用品/服装鞋帽/化妆…） | ① |
| `official_qr_code` `contacts` `qualifications` `main_category` | 店铺资质与联系方式页 `/shop/about` | ① |
| `mall_qr_code` `wechat_link` | 分享/加微信入口 | ② |
| `shop_enabled` `open_time` `close_time` | 非营业时间：首页顶部横幅提示 + 结算页禁用提交 | ① |
| `auth_domain` | 小程序业务域名（部署配置，非前端逻辑） | ② |

### 5.2 准入与注册（店铺设置 → 注册设置）

| 字段 | C 端消费位置 | 期 |
|---|---|:-:|
| `allow_guest` | **游客能否进店**：否 → 未登录直接引导登录 | ✅ |
| `guest_show_price` | **游客能否看价**：否 → 价格显示「登录可见价」 | ✅ |
| `enable_register` | 注册页是否可提交 | ① |
| `reg_audit_required` `enable_auto_audit` | 注册后状态：待审核页 / 直接可买 | ✅ |
| `reg_default_grade_id` | 注册成功后的默认客户等级（影响价格） | ① |
| `reg_default_category` | 注册时写入的归属分类（后台筛客用） | ① |
| `reg_give_coupon` `reg_give_coupons` | 注册成功 → 自动发券 + 引导弹窗 | ① |
| `register_agreement` | 注册页协议勾选内容 | ① |
| `enable_join_apply` | 是否允许"申请成为买家"（配合准入审核） | ① |
| `wechat_only_login` | 登录页只显示微信登录 | ② |

### 5.3 商品展示口径（基础设置 / 单位显示 / 商品上架）

| 字段 | C 端消费位置 | 期 |
|---|---|:-:|
| `display_list_fields` `display_detail_fields` | 列表卡 / 详情页**字段显隐** | ① |
| `show_sales` | 商品卡「近7日疯抢 N」是否显示 | ① |
| `stock_display` | 商品卡/详情是否显示库存数字 | ① |
| `out_of_stock_display` | 缺货商品：置灰 or 隐藏 | ① |
| `quantity_scale` | 加购步进器步长、是否只允许整数倍 | ① |
| `enable_split_unit` | 是否展示拆零单位（小/中/大单位换算） | ① |
| `price_track` `price_track_with_unit` | 「净单价约 x元/斤」换算展示 | ① |
| `enable_retail_price` | 是否展示零售价（划线价） | ① |
| `buyer_hide_level` | 是否对未认证买家隐藏客户等级信息 | ① |
| `enable_mall_category` | 分类页用商城自建分类还是商品分类树 | ① |
| `watermark_type` `watermark_image` `watermark_text` | 商品图水印（前端加水印层） | ③ |
| `category_display_mode` `category_default_sort` `category_style` `category_show_count` | 分类页形态（宫格/树）、默认排序、样式、是否显示数量 | ① |

### 5.4 交易与履约（运费设置 / 店铺设置）

| 字段 | C 端消费位置 | 期 |
|---|---|:-:|
| `min_order_amount` | 购物车/结算页起送门槛（「还差 ¥N 起送」） | ① |
| `freight_amount` `free_shipping_amount` `freight_template` `enable_logistics` `logistics_methods` | 确认订单页**运费试算** + 满额包邮 | ① |
| `self_delivery` `enable_pickup` `pickup_addresses` | 结算页配送方式：快递 / **到店自提**（含自提点选择） | ① |
| `payment_methods` `payment_scenes` | 结算页支付方式（替换现有硬编码 wechat/alipay/bank，并支持挂账） | ① |
| `auto_receive_enabled` `auto_receive_days` | 订单详情「N 天后自动确认收货」文案（实际自动收货需后端定时任务） | ② |
| `default_warehouse_id` | 首页/购物车展示默认发货仓；多仓切换入口 | ①（只读）/ ②（切换） |
| `message_subscribe` | 「我的→设置」订阅消息开关 + 小程序订阅授权 | ② |
| `sms_signature` | 短信签名（后端用，前端不消费） | — |

### 5.5 支付与小程序凭据（**前端不可下发**）

| 字段 | 处置 |
|---|---|
| `miniapp_appid` `miniapp_mall_url` | 前端可用（appid 用于小程序授权） |
| `miniapp_appsecret` | **绝不下发**，仅后端用 |
| `miniapp_pay_mch_id/key` `b2b_pay_mch_id/key` | **绝不下发**，仅后端用；下发的"店铺配置"接口必须**字段白名单**（见 §六 `F-01`） |

---

## 六、接口契约

### 6.1 已就绪（直接可用，49 个端点）

| 族 | 端点 | 用途 |
|---|---|---|
| `/v1/mall/auth` | login / register / logout / refresh-token / identities / switch-identity | 登录、注册、**多身份** |
| `/v1/mall/products` | `GET` 列表 / `{id}` 详情 / categories / categories/{id}/products / search / recommendations / hot / banners | 商品与分类 |
| `/v1/mall/cart` | `GET` 全量 / `GET /page` 管理端 / `POST` / `PUT {id}` / `DELETE {id}` / `DELETE /batch` / `POST /check-stock` | 购物车 |
| `/v1/mall/orders` | `POST` 建单 / `GET` 列表 / `{id}` 详情 / `{id}/cancel` / `{id}/confirm` / payment-methods | 交易 |
| `/v1/mall/payments` | `POST` 发起 / `{id}/status` 查询 / callback | 支付 |
| `/v1/mall/user` | info / profile / addresses CRUD / addresses/{id}/default | 账户 |
| `/v1/mall/party-link` | apply / withdraw / my / enterprise-pending·approve·reject·revoke / tenant-pending·approve·reject·revoke（11 个） | **企业关联双向审核**（现前端零调用，本方案正式接入） |

### 6.2 待补接口（P0 = 不做则跑不通）

| 编号 | 方法与路径 | 用途 | 依赖/备注 |
|---|---|---|---|
| `F-01` | ✅ **已实现** `GET /v1/mall/shop/config` | 下发店铺装修/主题/展示开关 | `ShopConfigVO` **字段白名单**（56 字段），剔除 `*_appsecret`/`*_mch_key`/`smsSignature`/`authDomain`；匿名可达 + `requireShop()` 准入；**真机验证无凭据泄露**。`etag` 缓存留待压测需要时再加 |
| `F-02` | ~~`GET /v1/mall/home/groups`~~ **已取消** | ~~首页分组宫格~~ | **用户 2026-09-26 定：商品分组即商品分类** ⇒ 直接复用已就绪的 `GET /v1/mall/products/categories`，不新增接口、不新增"分组"管理页 |
| `F-03` | ✅ **已实现** `GET /v1/mall/tags` | 商品标签（分类页顶部 Tab、商品卡角标） | 新建只读 `MallTagOption`/`MallTagOptionMapper`（权威写入方在 erp-stock 的 `/api/erp/mall-tag`）；并给 `GET /products` 增加 **`tagCode` 过滤**，让标签 Tab 真正能筛商品（`mall_tags` 逗号分隔 → `like` 匹配） |
| `F-04` | ✅ **已实现** `GET /v1/mall/notice/list` | 首页公告（**匿名可读**） | 由 `/api/erp/mall/notice` **迁到 C 端前缀**并登记匿名白名单；返回改 `ApiResponse` + 只回 id/title/content/type/publishTime/publisher |
| `F-05` | `GET /v1/mall/orders/counts` | "我的"订单五宫格角标 | 按状态分组计数 |
| `F-06` | `GET /v1/mall/orders/{id}/track` | 物流轨迹 | 前端**曾恒发 404**（已被 catch 吞掉）；2026-09-26 已从 `api/index.ts` **摘除该调用**并留注释，待 F-06 落地后恢复 |
| `F-07` | 支付回调验签接线 | 支付真实入账 | 后端 P0-2：`Alipay/Wechat/UnionPayCallbackVerifier` 现为**死代码**，渠道实现是 `setStatus(2)` 桩 |
| `F-08` | 下单**占用库存** | 防超卖 | 现值"只校验不占用"（`MallOrderServiceImpl.createOrder`） |
| `F-09` | `POST /v1/mall/chat/ask` | AI 客服问答 | 复用 `core/agent`；注入买家身份与最近订单上下文 |
| `F-10` | `GET /v1/mall/messages` `PUT /{id}/read` | 消息列表/已读（按 `shop_user` 维度） | 现有通知端点是按 `sys_user` |

### 6.3 待补接口（P1 = 体验完整）

| 编号 | 方法与路径 | 用途 |
|---|---|---|
| `F-11` | `POST /v1/mall/orders/preview` | 确认订单试算：商品额 + 运费 + 起送 + 优惠 |
| `F-12` | `GET /v1/mall/coupons` `POST /{id}/claim` | 优惠券（营销域**目前无任何 C 端接口**） |
| `F-13` | `GET /v1/mall/user/points` | 积分余额与流水 |
| `F-14` | `POST /v1/mall/after-sales` `GET /list` | 售后申请与列表 |
| `F-15` | 搜索接入 `mall_keyword` | 现搜索只 `like(product_name)`，关键词库是死配置 |
| `F-16` | `POST /v1/mall/user/invoices` | 发票抬头 |
| `F-17` | `GET /v1/mall/favorites` | 常购清单（按历史订单聚合） |
| `F-18` | `GET /v1/mall/price/ladder?productId=` | 阶梯价/等级价（价格引擎已存在，需 C 端封装） |

### 6.4 状态口径（前端展示映射，**不要自造**）

**订单**：`erp_sale_order.status` 为唯一基准（0 草稿 / 1 待审批 / 2 已审批 / 3 部分出库 / 4 完成 / 5 交易完成 / 6 已取消）；商城的细分串（待付款/审核中/已付款…）从 `extInfo.originalMallStatus` 取，**不落独立列**。前端 Tab 分组：

| 前端 Tab | 判定 |
|---|---|
| 待付款 | `originalMallStatus = PENDING_PAYMENT` |
| 待发货 | `PAID`（已付待审/待发） |
| 待收货 | `SHIPPED` |
| 已完成 | `COMPLETED` |
| 售后 | 关联退货申请（`/erp/sale/return-apply`） |

**上架状态（易错）**：写入用 `ON_SHELF`/`OFF_SHELF`，**读出只有 `ON_SHELF`/`INACTIVE`**（视图由 `mall_shelf_status` 1/0 派生）—— 前端判断上架必须用 `ON_SHELF`，别拿 `OFF_SHELF` 去比。

**价格三态**（见 §七）：`GUEST_HIDDEN` → `PENDING_AUTH` → `LEVEL_PRICE`。

---

## 七、准入与三态价格（本方案的灵魂）

参考截图 1 底部那条「登录认证后可查看商品价格 · 去登录」与截图 3 的「门店认证优惠」，正是本系统的既有设计意图 —— 行业订货商城**不公开价格**。系统中对应的开关已存在：`allow_guest`（能否进店）、`guest_show_price`（游客能否看价）、`reg_audit_required`（注册是否需审核）、`buyer_hide_level`（是否隐藏等级信息）。

**三态定义**

| 态 | 判定条件 | 界面表现 |
|---|---|---|
| **T1 游客** | 无 token | 商品可浏览；价格位显示「登录可见价」；**底部固定条**「登录后可查看商品价格」+「去登录」；详情页按钮改为「登录」 |
| **T2 待认证** | 已登录但 `shop_user_tenant.status ≠ 1`（0 待审 / 2 驳回） | 价格位显示「**认证后可见价**」；顶部提示条「您的入店申请审核中，通过后即可查看价格并下单」；驳回则显示原因 + 「重新提交」；**禁止下单** |
| **T3 已认证** | `shop_user_tenant.status = 1` | 显示**该身份对应的价格**（个人会员价 / 企业客户等级价）；可按 `guest_show_price` 之外的规则展示划线零售价（`enable_retail_price`） |

**其它准入规则**

- **入店**：每个 `shop_user` 对每个租户（店铺）单独审核（`shop_user_tenant`）；同一买家的 A 店停用不影响 B 店（后端已按关联表逐店处理）。
- **店铺识别**：游客无会话 ⇒ 必须带 `X-Tenant-Id`（B2B 一租户一店）。建议 **H5 入口按域名/URL 参数确定店铺并写 localStorage**，避免每次 400「无法确定店铺」。
- **非营业时间**（`shop_enabled`/`open_time`/`close_time`）：可浏览、可加购，**不允许提交订单**，结算按钮置灰并说明原因。

---

## 八、支付闭环（一期必修）

现状：`api/index.ts` 里 `order.pay` / `payment.createPayment` / `getPaymentStatus` / `payment.callback` 四个方法**定义了但零调用**，且 `/order/:id/pay` 路由不存在（点了白屏）。本方案要求打通：

```
确认订单 → POST /v1/mall/orders（建单，status=0 待付款）
        → POST /v1/mall/payments（createPayment，拿渠道参数）
        → 唤起渠道（H5 跳转 / 小程序 requestPayment）
        → 渠道异步回调 POST /api/payment/callback/{channel}   ← 必须带验签（F-07）
        → 前端轮询 GET /v1/mall/payments/{id}/status
        → 成功页 → 订单 status=1（待审批）→ 管理端审核 → 发货 → 确认收货
```

**挂账/账期**（B2B 必需）：企业身份可走「挂账」支付方式 —— 订单直接置为待审批，由 ERP 侧生成应收（**依赖财务口径确认**，见 §十③）。

**⚠️ 前置红线**：在渠道验签接线完成前，**不得**上线支付入口。原因是当前 `/api/payment/callback/{channel}` 不验签、渠道实现无条件 `setStatus(2)`，等于开了"白拿单"通道（详见审计报告 P0-2；本方案已将买家端 `POST /orders/{id}/pay` 等三个越权端点删除）。

> **✅ 2026-09-26 深夜更新（第四批）**：该红线**已解除**——回调已按租户分发 + fail-closed（第二批），
> 支付宝/银联渠道已**真发报文、真签名**（第四批），回调白名单已按「启用三步」放开。
> **但仍未用真实商户凭据对网关联调**：上生产前必须用沙箱密钥跑一次真实「下单 + 回调」，详见文末「第四批 · E」。

---

## 九、分期实施计划

| 期 | 范围 | 出口标准（可验收） |
|---|---|---|
| **一期 · 打通** | 5 主 Tab + 顶部二级 Tab 框架；`F-01/F-02/F-03/F-04/F-05/F-06`；首页（公告/轮播/弹窗/分组宫格/瀑布流）；分类双形态；购物车分组；我的（订单五宫格/身份卡/地址）；确认订单 + **支付闭环（含 F-07 验签、F-08 占库存）** | 一个**真实买家**能从扫码进店走到"已付款"，全程无 400/403 报错页，价格三态正确 |
| **二期 · B2B 特色** | 身份切换 UI、企业关联双审（`party-link` 13 端点接入）、等级价/阶梯价（`F-18`）、起送门槛与运费/自提、常购清单、多仓切换 | 企业客户能用**自己的协议价**下单并选择账期 |
| **三期 · 营销与售后** | 优惠券/积分（`F-12`/`F-13`）、售后申请（`F-14`）、发票（`F-16`）、自动确认收货、订阅消息、图片水印 | 与营销域、售后链路闭环 |

**每期的"配置激活"指标**：一期结束时 `tenant_shop_config` 的**运行期读取字段数应 ≥ 35**（现为 4）。

---

## 十、风险与待拍板

| # | 事项 | 影响 | 建议 |
|:-:|---|---|---|
| ① | **只做 H5/PWA 还是也要小程序？** 配置里有 `miniapp_appid`/`miniapp_pay_mch_id`，原计划像是有小程序 | 小程序支付、订阅消息、企业微信客服**都与 H5 实现不同**，工期差一倍 | 一期先 H5/PWA（现有栈直接跑），小程序作为二期独立交付 |
| ② | ~~**支付渠道真接哪几家**~~ | **已定（2026-09-26）**：先接 **支付宝 + 银联**（两者共用 H5 跳转收银台，且银联是 B2B 大额常用）；微信待二期（需 APIv3 平台证书轮换 + AES-GCM 解 resource） | — |
| ③ | **商城订单与 ERP 库存/财务的联动口径** | 发货是否生成销售出库单、收款是否生成应收 —— 一旦有真实订单，账实不符立刻暴露 | 与财务模块一并拍板；本方案默认"确认后生成销售出库单，由出库单驱动库存与应收" |
| ④ | **是否多仓 / 多店铺** | 现为"一租户一店 + `default_warehouse_id` 单默认仓" | 一期按单店单仓做，仓库切换留二期 |
| ⑤ | **AI 客服的落点与数据边界** | 复用 `core/agent` 需确认其现状与鉴权；买家上下文（等级价、订单）注入是敏感操作 | 先做"只答不见数据"的 FAQ+通用问答，涉及订单/价格的问题一律转人工 |
| ⑥ | ~~商品分组从哪配~~ | **已定（2026-09-26）**：商品分组 = 商品分类，直接接 `erp_product_category`；商品标签 = 分类页最顶部标签栏（`erp_mall_tag`）。不新造"分组"概念、不新增管理页 | — |

---

## 一期落地记录（2026-09-26，两批）

> 用户口径：「按你推荐的进行，**商品分组实际就是商品分类**，接过来就可以了；**商品标签做分类页最上面的商品标签 Tab**」。

### 已实现

| 范围 | 内容 | 文件 |
|---|---|---|
| **后端 F-01** | `GET /api/v1/mall/shop/config` —— 新增 `MallShopController`/`MallShopService(Impl)` + `ShopConfigVO`（**56 字段白名单**，剔除 `*Appsecret`/`*PayMchKey`/`smsSignature`/`authDomain`） | `erp-mall/.../controller/MallShopController.java` 等 4 个文件 |
| **后端 F-03** | `GET /api/v1/mall/tags` —— 新增 `MallTagController` + 只读 `MallTagOption`/`MallTagOptionMapper`（权威写入方在 erp-stock，只读引用避免模块环）；并给 `GET /products` 增加 **`tagCode` 过滤**（`mall_tags` 逗号分隔 → `like`） | `MallTagController.java`、`dao/MallTagOption*.java`、`MallProductService(Impl)`、`MallProductController` |
| **后端 F-04** | 公告端点由 `/api/erp/mall/notice` **迁到 `/api/v1/mall/notice/list`**，返回改 `ApiResponse` 且只回 6 个字段 | `MallNoticeController.java` |
| **后端 · 白名单** | `SaTokenConfig` 两个拦截器各登记 `/api/v1/mall/shop/**`、`/api/v1/mall/notice/**`、`/api/v1/mall/tags`（匿名**可达**，准入仍由 `requireShop()` 收口） | `core-base/.../SaTokenConfig.java` |
| **后端 · 收敛** | `requireShop()` 从 `MallProductServiceImpl` 私有方法**上提到 `MallGuestAccess`**，店铺配置/商品/标签共用同一套准入（消除三份重复判定） | `MallGuestAccess.java`、`MallProductServiceImpl.java` |
| **后端 · 商品卡字段** | `ProductListDTO` 由 8 字段扩到 17 字段（+`categoryName`/`industryCategory`/`specification`/`unitName`/`productTag`/`description`/`minOrderQuantity`/`productType`），来源均为 `v_mall_product` 已有列 | `ProductListDTO.java`、`MallProductServiceImpl.java` |
| **后端 · 价格三态判据** | `UserInfo` 增 `auditStatus`/`shopEnabled`（来自 `shop_user_tenant`），让前端能区分**已登录但未过审**态 | `UserInfo.java`、`MallUserServiceImpl.java` |
| **前端 · 5 主 Tab** | 首页/分类/**消息**/购物车/我的；`TabBar` 角标改**按 key**（原写死 `index===2`）、高亮改按 **`meta.tab`**；`meta.showTabBar` 首次真正生效 | `App.vue`、`TabBar.vue`、`router/index.ts` |
| **前端 · 顶部二级 Tab** | 新增 `TopTabs.vue`（吸顶/横滑/选中自动居中/不重建页面） | `components/layout/TopTabs.vue` |
| **前端 · 首页** | 8 区块：店铺标题栏（店铺名+主题色）→ 搜索 → 顶部 Tab（推荐/新品/热销）→ 公告条 → 轮播 → **商品分类宫格** → 双列瀑布流 → **底部价格提示条** | `views/home/index.vue` |
| **前端 · 分类页** | 最顶部 = **商品标签 Tab**（全部 + `erp_mall_tag`）；「全部」→ 商品分类宫格；选标签 → 该标签商品 | `views/category/index.vue` |
| **前端 · 消息页** | 顶部 Tab 客服（默认）/ 消息；客服含 FAQ 快捷问 + 转人工入口（AI/企微接口未就绪，**不造假回答**） | `views/message/index.vue`、`stores/message.ts` |
| **前端 · 价格三态** | `stores/shop.ts`：`priceMode`（GUEST_HIDDEN/PENDING_AUTH/READY）+ `pricePlaceholder` + `priceHint`；商品卡与首页提示条统一消费 | `stores/shop.ts`、`ProductCard.vue` |
| **前端 · 店铺识别** | `utils/shop.ts`：`?tenantId=` → localStorage → `VITE_DEFAULT_TENANT_ID`；请求拦截器注入 `X-Tenant-Id`（游客没有会话，缺它一律 400「无法确定店铺」） | `utils/shop.ts`、`api/index.ts` |
| **前端 · 开发代理** | `vite.config.ts` 补 `/api` 代理（原先**没配** ⇒ 本地 dev 所有接口必 404，C 端无法联调）；默认 `127.0.0.1:5655`，可用 `VITE_API_TARGET` 覆盖。⚠️ 必须写 `127.0.0.1`：Node 18+ 把 `localhost` 解析成 IPv6(::1) 而 Java 只监听 IPv4 → 代理 ECONNREFUSED（实踩） | `vite.config.ts` |

### 真机验证（独立实例 :5691 + C 端 dev :3012，均不影响他人会话）

| 脚本 | 结果 |
|---|---|
| `node tools/probe-mall-c.cjs 5691 1` | **24/24** —— 三接口匿名不再 401；缺 `X-Tenant-Id` → 400；`allow_guest=NOT_ALLOW` 时一律 403；临时置 ALLOW 后 200 且**无凭据泄露**、标签 20 条、按 `TAG_1` 过滤有效；**改动已还原并复核** |
| `node tools/verify-mall-c-ui.cjs http://localhost:3012 1` | **20/20**（修完下述四处后重跑）—— 底部 5 Tab 名称与顺序、顶部 Tab 三个、店铺名来自后台、公告条、分类宫格、商品卡、切 Tab 不重建页面、分类页标签栏、消息页双 Tab、无 JS 报错 |
| `vue-tsc --noEmit`（mobile-mall） | 本次改动文件 **0 报错**；仓库余 56 条全在既有页面（见"待修"） |
| `vite build`（mobile-mall） | **✓ built in 10.06s** |

### 真机暴露并当场修掉的三个缺陷

1. **匿名公告恒空**（本轮引入的回归）：`MallNoticeServiceImpl.getCurrentTenantId()` 只取 `securityContext`，匿名下返回 `null` ⇒ `tenant_id = null` 恒不成立 ⇒ 200 + 空数组（**静默错**）。已改为「会话优先，其次 `X-Tenant-Id`」。
2. **价格文案误导**：`ProductCard` 原先把「价格被规则隐藏」与「商品本身无价」混为一谈。真库 `erp_product.retail_price` **6/6 全为空** ⇒ 已开放看价的游客看到的是「登录可见价」。已拆三态：规则隐藏 → 占位文案；规则允许但无价 → **「暂无价格」**；否则显示数字。
3. **底部 Tab 栏没吸底**（先于本轮就存在）：`:fixed="true"` 已加 `van-tabbar--fixed`，Vant 样式表里也确有 `.van-tabbar--fixed{position:fixed}`，但真机 computed `position` 恒为 **`relative`** ⇒ Tab 栏被排在内容之后、要滚到底才可见（首页/分类/消息三页实测一致）。用组件内 `:deep(.van-tabbar)` 覆盖**无效** —— 编译后 `.van-tabbar[data-v-x]` 与 `.van-tabbar--fixed` **特异度相同(0,2,0)**，Vant 样式运行时后注入 ⇒ 同分后到者胜。**已改为在组件上下发行内样式**（不依赖样式注入链路），实测三页均 `position=fixed, top=794/bottom=844（视口 844）`。
4. **分类名回落成裸 id**：`getCategories()` 的 name 是 `COALESCE(mall_category_name, category)`，真库两列皆空时回落成**分类 id**（如 `2072844513319059457`），宫格上就是「20 / 20」+ 一长串数字，既不可读又像 bug。已在首页/分类页统一兜底为「未分类」（前端 2 处，不再显示裸 id）。

### 第二批（同日）：数据、失效按钮、F-05/F-06/F-07

| 范围 | 内容 | 关键点 |
|---|---|---|
| **演示数据** | 新增 `tools/mall-demo-seed.sql`（幂等 + 文末**回滚段**；⚠️ 放 `tools/` 而非 `db/` 是因为 `.gitignore` 的 `*.sql` 会把它**静默吞掉**，而 `!tools/*.sql` 是仓库给"运维 SQL 也是源码"留的例外）：4 个真实商品补售价/批发价、上架 2 个已下架商品、补主仓库存、给 2 个 E2E 残留商品**下架**（只下架不删） | 现在首页 4 张卡都显示**真实价格**与库存（实测「数字 4 个 / 暂无价格 0 个」） |
| **分类名** | `getCategories()` 与列表 DTO 的分类名**改回分类树取权威名称**（新增 `ErpProductMallMapper.selectCategoryNames`，列表**批量**填充，避免 N+1） | 宫格从「裸 id / 未分类」变成真实名称「饮品原料 / 茶酱果味」（实测截图） |
| **修两个点不动的按钮** | ① `category/detail.vue` 调的是**不存在**的 `api.product.getByCategory` ⇒ 改用真实存在的 `/products/categories/{id}/products`，并修正响应体口径（`records` 而非 `list`），**删掉"商品示例1/2/3"假数据兜底**；② `register.vue` 与后端契约三处不一致（缺 `username`、要填无后端支撑的验证码、读不存在的 token）—— 现按 `LoginRequest` 对齐为「用户名/密码/手机号/昵称」，去掉验证码字段与按钮，注册成功后去登录 | register 此前**必然失败**（username 为空 ⇒ 撞 NOT NULL ⇒ 500） |
| **F-05 订单计数** | `GET /v1/mall/orders/counts` → 待付款/待发货/待收货/已完成/售后；**口径与列表共用** `resolveExpectedOrderSource()`（抽出一处，避免两边漂移） | 真机断言「五键齐全 + 计数之和 == 列表 total」 |
| **F-06 物流信息** | `GET /v1/mall/orders/{id}/track` → 物流公司/运单号/配送状态 + `traces`（**无承运商数据源，如实为空数组** + `traceTip` 说明），前端订单详情接回 | 真机：自己的单 200、他人的单 403、`traces=[]` |
| **F-07 支付回调 fail-closed** | **core-payment 的 `/api/payment/callback/{channel}` 此前仍是"不验签当成功"**（渠道实现是 `setStatus(2)` 桩）—— 照抄 erp-mall 9-21 已成熟的写法改造：新增 `/callback/{tenantId}/{channel}`（验签分发 + fail-closed + `ackBody`），旧无租户路径**一律拒绝**；删掉 `PaymentChannel.handleCallback`（5 个渠道的假成功桩）与其服务入口；退款回调**改为拒绝（501）**并删除假的 `RefundService.handleCallback` | 真机 6 条断言全绿：旧路径 400、未配凭据 401、未实现渠道 401、**伪造回调既不改单也不落记录**、退款回调 501 |

**第二批真机结果**：`tools/probe-trade-f050607.cjs` → **17/17**；`tools/probe-mall-c.cjs` → **24/24**；`tools/verify-mall-c-ui.cjs` → **20/20**。

### ⚠️ 构建陷阱（本轮实踩，影响"改了没生效"的判断）

`mvn -pl <app> -am package` **会从 `~/.m2` 取到旧的内嵌模块 jar**：本次 `core-payment/target/*.jar` 已是新的，但 fat jar 里 `BOOT-INF/lib/core-payment-*.jar` 还是旧的 ⇒ 真机表现像"改错了"。
判据：直接读 **fat jar 内嵌的模块 jar** 里的特征串。解法：走**全量**构建（`cd backend && mvn -o package -DskipTests`，即 `tools/build-backend.sh --build-only` 的口径），不要用 `-pl` 抄近路。

### 第三批（同日）：C 端类型错误清零 + 连带挖出 4 个 P0 真 bug

用户要求「C 端 44 条类型错误继续扫掉」。扫完是 0，但过程中发现**类型错误只是表象** —— 其中多条对应运行期真缺陷：

| # | 真 bug | 症状 | 修法 |
|:-:|---|---|---|
| 1 | **C 端认证头写错** | `api/index.ts` 发的是 `Sa-Token: <token>`，而后端 sa-token 配置是 `token-name: Authorization` + `Bearer` 前缀 ⇒ **登录成功但之后每个请求都 401**，购物车/我的/订单/地址/资料**全进不去**（真机 A/B：`Sa-Token` 头 401、`Authorization: Bearer` 200） | 改为 `Authorization: Bearer ${token}`（与 pc-admin 一致） |
| 2 | **`mall_address` 实体与表结构不符** | 实体映射 `customer_id / province / city / district / detail_address / label` —— 表里**一个都没有**（真表是 `user_id / region / address`）⇒ 地址簿增删改查**全 500** | 按 `V9.34.0` 权威结构重写实体 + `AddressDTO`，前端（地址簿/确认订单/订单详情）同步改字段 |
| 3 | **实体继承 `BaseEntity` 带了表里没有的审计列** | `create_by / update_by` 在 `mall_address`、`mall_cart` **不存在**（全库 229/572 张表也没有 ⇒ 不是全局约定）⇒ 修完列名紧接着报 `字段 "create_by" 不存在` | 这两个实体改为不继承 `BaseEntity`，自己声明实际列 |
| 4 | **软删不生效**：`setDeleted(1) + updateById` | `@TableLogic` 下 MP 不把逻辑删除列放进 UPDATE 的 SET ⇒ 地址/购物车「删除成功」但**列表里还在**（真机：删完仍读回 1 条） | 改用 `deleteById` / `delete(wrapper)`（与 `removeBatch` 已有的 `deleteBatchIds` 同口径） |

另有 2 处同源问题：`is_default` 表列是 **integer** 而实体用 `Boolean`（插入报「类型为 integer 但表达式为 boolean」）；`checked` 是表里不存在的派生列，却被写进 SQL 的 where（`checkStock` 必 500）。

**类型错误明细（44 → 0）**中真正对应运行期的还有：
`order/list.vue` 把 `PageResult` 当数组 ⇒ **订单列表恒空**；`search` 的 `api.product.search` 少传参数（keyword 传成对象 ⇒ **永远搜不到**）+ 响应体取 `list` 而非 `records`；`category/detail`/`search`/`address` 三处**「示例商品」假数据兜底**；`register` 缺 username（**注册必 500**，上一批已修）。

**系统化收口**：新增 `tools/audit-mall-entity-columns.py` —— 一次扫出「实体字段 ↔ 真表列」全部不一致（此前是**修一个、真机再暴露一个**，每轮要等一次全量构建）。结果：**全部实体与真表结构一致**。

**第三批真机结果（五套全绿，97/97）**

| 脚本 | 结果 |
|---|---|
| `tools/probe-mall-c-crud.cjs`（**新增**：地址/购物车 增查改删往返，自建自清） | **14/14** |
| `tools/verify-mall-c-ui-auth.cjs`（**新增**：演示买家登录后逐页，含地址簿/我的/订单/资料/搜索/商品详情） | **22/22** |
| `tools/probe-mall-c.cjs` | 24/24 |
| `tools/probe-trade-f050607.cjs` | 17/17 |
| `tools/verify-mall-c-ui.cjs` | 20/20 |
| `vue-tsc --noEmit`（mobile-mall） | **0 报错** |
| `vite build`（mobile-mall） | ✓ built |

**演示买家**（`tools/mall-demo-seed.sql` 第 ⑤ 段）：`demo_buyer` / `admin123`，`shop_user_tenant.status=1`（本店已过审）⇒ 价格三态落在「已认证」。需登录页面因此可被真机验证。

### 第四批（2026-09-26 深夜）：支付宝/银联**真接** + `mall_cart` 补列

> 用户口径：「**支付真接继续接支付宝和银联**」「`mall_cart.checked/subtotal` 表里没有，**给表加列**」。
> 上一批把回调改成 fail-closed 后，渠道侧仍是「能下单但拿不到收银台地址」的桩 —— 本批把渠道实现做成**真发报文、真签名**。

#### A. `mall_cart` 补两列（迁移 `V11.516.0__Add_Mall_Cart_Checked_Subtotal.sql`）

`checked`（勾选，integer，默认 1）与 `subtotal`（小计，numeric(14,2)）此前是实体里的 `@TableField(exist=false)` 派生字段，
但 `checkStock` 的 wrapper 里有 `.eq(MallCart::getChecked, 1)` ⇒ **结算必 500**。上一批只用「改 SQL 不查该列」绕过，
本批按用户口径**给表加真列**（`ADD COLUMN IF NOT EXISTS` + 存量行回填 `checked=1`/`subtotal=单价×数量`），
实体字段转为真实列映射，`checkStock` 的过滤恢复为「只校验已勾选行」。

#### B. 支付渠道真接（`core-payment`）

| 范围 | 内容 |
|---|---|
| **加密底座** | 新增 `crypto/Rsa2`：`sign(content, 私钥)`（PKCS8）/ `verify(content, 签名, 公钥)`（X509，异常一律 **false**）/ `buildSignContent(params, exclude)` + **值变换重载** / `urlEncode` / `stripPem`。**支付宝与银联共用这一个类** —— 两家的**口径差异**只体现在「是否对值做 URL 编码」这一次变换上 |
| **渠道返回值** | `PaymentChannel.createPayment` 改返回 `ChannelPayResult`（渠道单号 + `payUrl`/`payParams` + 原文），替代原先只回一个单号；`PaymentRequest` 加 `@TableField(exist=false) payUrl`，`createPayment` 透传给前端 |
| **支付宝** | `alipay.trade.page.pay` 拼**带 RSA2 签名的收银台 URL**（排序串 `k=v&`、值**不编码**）；查单/关单/退款/退款查询走 `alipay.trade.*`，用 `HttpRequest.form(...).timeout(10s)`；**响应验签**用原文 JSON 节点抽取（`extractRawNode`，避免反序列化后再序列化改变字节） |
| **银联** | 全渠道网关：`txnType` 01(消费)/00(查单)/04(退货)；**`txnAmt` 单位是分**（`toFen`，12.34 元 → `1234`）；签名时值**要 URL 编码**（`Rsa2::urlEncode`），验签同口径；退货必须带 `origQryId`；应答按 `k=v&` 解析 |
| **微信** | 仍为「尚未接入」，显式抛错（**不假装能付**） |
| **凭据读取** | 新增 `support/ChannelCredentialAccessor`（**会话租户**取参，与回调验签器读**同一行** `payment.channel.<渠道>` 配置）—— 下单与验签读同一个键，杜绝「A 私钥签、B 公钥验」这类必然验不过的接线错 |
| **验签器去重** | `AlipayCallbackVerifier`/`UnionPayCallbackVerifier` 的 `buildSignContent`/`verifyRsa2`/`verifyRsa` 改为**委托 `Rsa2`** ⇒ 下单侧与回调侧的口径**结构上不可能漂移**（原先各写一份） |
| **前端表单** | `pc-admin` 支付配置页补 **5 个字段**：支付宝私钥/网关、银联商户私钥/证书 ID/网关；`api/payment/index.ts` 类型同步（含回填/重置）。并把注释里「验签器生产代码零调用」的**过时结论**更正为现状 |

#### C. 回调链路两个真 bug（本轮修复）

| # | 真 bug | 症状 | 修法 |
|:-:|---|---|---|
| 1 | **`payment_record.tenant_id` 恒 null** | 回调是匿名请求，MyBatis-Plus 的 `insertFill` 无会话 ⇒ 租户列为空，按租户对账**一行都查不到** | `handleVerifiedCallback(tenantId, ...)`（租户从**回调路径**拿）显式 `record.setTenantId(tenantId)` |
| 2 | **`payment_record.request_id` 从不写入** | 原代码**先 insert 记录、后查支付请求** ⇒ 记录与请求**脱钩**（按 `request_id` 关联恒为空） | 改为**先查请求、再插记录**，插入时显式 `setRequestId(request.getId())` |

#### D. 白名单放开（前提已满足）

`SaTokenConfig` 两个拦截器各登记 `/api/payment/callback/**`、`/api/v1/mall/payments/callback/**`。
放开的**前提**是第四批已完成的三件事：① 三家验签器均已认领渠道；② 凭据按租户取、`isConfigured` 才为 true；
③ 无实现/未配凭据/验签失败一律 **fail-closed**。缺任一环都不能放开（审计 P0-2）。

#### E. 本批的验证方式 —— **"自签自验闭合回路"**（关键方法论）

**验证不了的事先说清楚**：没有商户号/证书、构建环境**不出网** ⇒ **无法对真实支付宝/银联网关联调**。
能验证的是**"接线是否自洽"** —— 而这恰恰是本地能查出、事后最难查的一类错：

- 下单签名用**自己的私钥**、验签用**配置的公钥**（都在本地生成）⇒ **口径错立刻暴露**；
- 用 **Node 的 `crypto`（与 Java 完全独立的另一套实现）** 去验 Java 侧签出来的串 ⇒ 排除「两边共用同一段错代码自证清白」；
- 伪造一份**合法渠道回调**（同样用配置私钥签）→ 走**真实** callback 端点 → 断言支付请求被置为已支付、支付记录回写了 `request_id`；
  再**篡改金额** → 断言被拒（fail-closed）、且**不改单**。

| 脚本 / 用例 | 结果 |
|---|---|
| `tools/probe-payment-channels.cjs`（**新增**：写两条渠道配置 → 下单 → 独立实现验签 → 自签回调 → 篡改金额被拒 → 未配凭据渠道被拒 → **按原值还原配置 + 自造数据全清**） | **19/19** |
| `AlipayChannelTest`（7 例）+ `UnionPayChannelTest`（6 例）：自生成密钥对闭合回路、篡改检测、错公钥、金额单位、缺凭据 fail-fast、**银联「不编码口径必须验不过」** | **13/13** |

> ⚠️ **诚实边界**：本实现按开放平台文档构造报文与签名，但**未经真实网关联调**（没有商户号/证书、构建环境不出网）。
> 自证方式是「签名 → 按验签规则验签」的闭合回路 + 独立实现（Node crypto）交叉验证。
> **上生产前必须用沙箱密钥跑一次真实「下单 + 回调」。不要把"实现了"当成"已验证能收款"。**

### 待修（本轮发现，未处理）

| # | 问题 | 位置 |
|:-:|---|---|
| ~~1~~ | ~~`api.product.getByCategory` 不存在~~ ✅ **已修（第二批）** | — |
| ~~2~~ | ~~`api.auth.sendVerifyCode` 不存在 + 注册缺 username~~ ✅ **已修（第二批）** | — |
| ~~3~~ | ~~C 端 44 条类型错误~~ ✅ **已清零（第三批）**，并连带修掉 4 个 P0 真 bug | — |
| 4 | **数据**：`erp_product.retail_price` **6/6 全为空** ⇒ 商城即使开放看价也显示「暂无价格」；需业务补商品售价，或确认商城售价应取其它价格列 | 商品档案 |
| 5 | 搜索未接热词库（`F-15`）：搜索仍只 `like(product_name)`，`mall_keyword` 未被消费 | `MallProductServiceImpl` |
| 6 | **数据**：在售商品**无图**（`image_url` 空）、**无库存**（`erp_stock` 无行 ⇒ 全部显示「缺货」）、**无分类名**（`mall_category_name`/`category` 空 ⇒ 显示「未分类」）、且混有 E2E 造数商品（`E2E单位-…`）。这些是数据与清理问题，不是代码 | 商品档案 / erp_stock / 收尾清理 |
| 7 | **微信支付仍为「尚未接入」**（`WechatChannel` 显式抛错）。要做需先定：APIv3 平台证书轮换表 + `Wechatpay-Serial` 路由 + `resource` 的 AES-GCM 解密（字段已在 `PaymentChannelParam` 里预留） | `channel/WechatChannel.java` |
| 8 | **支付宝/银联未经真实网关联调**（无商户号/证书、构建环境不出网）。上生产前必须用**沙箱密钥**跑一次真实下单 + 回调；另需确认公网可达的 `notifyUrl`（内网地址渠道回不来） | 上线前置 |

---

## 附：与现有代码的差异清单（一期开工前必读）

| 位置 | 现状 | 需改成 |
|---|---|---|
| `src/App.vue` | `tabBarItems` 4 项（首页/分类/购物车/我的） | 5 项（+消息），顺序按 §2.1 |
| `src/components/layout/TabBar.vue` | 角标写死 `index === 2` 给购物车；`active` 用 if-else 硬编码路径 | 改为**按 `key` 判断**角标；`active` 由路由 `meta.tab` 决定，避免再加 Tab 就漏改 |
| `src/router/index.ts` | 15 条路由，`meta.requiresAuth` 控制登录 | 新增 §2.3 的 14 个页面；`meta` 增补 `tab` / `topTab` / `keepAlive` |
| `src/api/index.ts` | `order.track` 指向后端不存在的端点；`order.pay` 指向已被删除的越权端点；`payment.callback` 路径写错 | ✅ **已处理**：删除 `order.pay`、`payment.callback`、`order.track` 三个错误定义并留注释；新增 `shop.getConfig`/`notice.getList`/`tag.getList`；请求拦截器注入 `X-Tenant-Id` |
| `src/views/order/detail.vue` `list.vue` | `router.push('/order/'+id+'/pay')` —— **该路由不存在**，点击白屏 | 新增 `/pay/:orderId` |
| `src/views/user/index.vue` | 无身份切换、无订单五宫格 | 按 §4.5 重做 |
| 全局 | 无 `X-Tenant-Id` 注入逻辑 | 按 §七 增加"店铺识别"（域名/参数 → localStorage → 请求头） |

