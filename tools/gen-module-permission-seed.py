# -*- coding: utf-8 -*-
"""
为「零权限码模块」生成权限种子迁移 + 端点→权限码对照表（只读扫描，不执行、不连库）。

## 背景（E-04）

E-04 实测：`sys_permission` 500 条里 `erp:*` 只占 31 条，而
**stock / wms / marketing / b2b / payment / party / budget / invoice / fixedasset
九个模块的域前缀在权限码库里完全不存在**（或只有个位数）。
没有码，就**不能**给这些控制器补 `@SaCheckPermission` ——
注解引用库中不存在的码会让该接口对**所有非超管一律 403**（本仓历史事故，见
记忆 `permission-code-missing-lockout`）。

⚠️ **反向纪律**：只补码不补注解 = 凭空造出几百条「僵尸码」（能在矩阵里勾选、勾了不生效），
正好加重 E-02。所以本脚本产出的码**必须**与 E-01 的注解改动同批次落地。

## 口径

1. **粒度**对齐既有约定：一码 = 一「(资源, 动作)」，不是一码一端点。
   实测 `PurchaseOrderController` 16 个端点 → 8 个码（`purchase:order:{list,detail,
   create,update,delete,submit,approve,cancel}`）。
2. **命名**对齐同域既有风格 `<域>:<资源>:<动作>`。
   动作词优先复用库中已有的（`create/update/delete/list/detail/view/approve/submit/export/...`），
   不新造同义词（既有的 `view`↔`detail`、`edit`↔`update` 两套并存是历史包袱，不再扩大）。
3. **资源名**取类级 `@RequestMapping` 去掉模块前缀后的剩余段（多段用 `-` 连接）。

## 用法

    python tools/gen-module-permission-seed.py budget            # 打印对照表
    python tools/gen-module-permission-seed.py budget --sql      # 打印迁移 SQL
    python tools/gen-module-permission-seed.py --list            # 列出已配置模块

新增模块：在 `MODULES` 里加一项（`strip` 填类级路径的公共前缀）。
"""
import os
import re
import sys
import io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# ── id 号段（2026-09-21 实测空闲；禁止与既有种子复用）──
#   权限码：每模块一个 1000 槽，起点 100000 + slot*1000（9xxxx 段已被 V11.42x 短块占满）
#   角色关联：9500000 + slot*10000（9400000 段被 V11.439.0 占用）
#   ⚠️ 各模块必须用不同槽位，否则多个迁移互相撞主键（本仓 sys_permission.id 无序列默认值）
PERM_SLOT_BASE = 100000
RP_SLOT_BASE = 9500000
SORT_BASE = 1200

# 【域如何确定】不按「模块目录」而按**类级路由路径**推导 ——
# 实测这本仓两者并不对应：`erp-stock` 目录里装的是**商品/主数据**控制器
# （`/api/erp/product/*`、`/api/erp/md/*`），`wms` 里混着 `/api/v1/warehouse/*`，
# `erp-mall` 里既有后台 `/api/erp/mall/admin/*` 也有 C 端 `/api/v1/mall/*`。
# 按目录指定前缀会把大量控制器判成「资源=完整路径」，产出 `api-erp-product-xxx` 这类垃圾码。
#
# 规则：去掉 `/api`、跳过 `erp` 或 `v1` 这类容器段后，**第一段即域**，其余段用 `-` 连成资源；
# 资源为空则省略该段（产出两段码 `<域>:<动作>`，库中已有先例：`purchase:manage`、`crm:create`）。
# 值一律是 (域, 附加资源名)：附加资源名用于把「第一段里已经含了资源信息」的路径
# 还原成正确的域+资源，例如 `mall-tag` 应为 `mall:tag:*` 而不是 `mall-tag:*`。
DOMAIN_ALIAS = {
    'warehouse': ('wms', None),          # /api/v1/warehouse/* 属仓储域
    'capital-flow': ('finance', 'capital-flow'),
    'receipt': ('finance', 'receipt'), 'pre-receipt': ('finance', 'pre-receipt'),
    'pre-payment': ('finance', 'pre-payment'), 'offset': ('finance', 'offset'),
    'write-off': ('finance', 'write-off'), 'deposit-condition': ('finance', 'deposit-condition'),
    'inventory-mode': ('stock', 'inventory-mode'),
    'stock-alert': ('stock', 'alert'), 'stock-alert-config': ('stock', 'alert-config'),
    'mall-tag': ('mall', 'tag'),
    'product-unit-dict': ('product', 'unit-dict'),
    'product-unit-group': ('product', 'unit-group'),
    'customer': ('party', 'customer'), 'contact': ('party', 'contact'),
    'member-level': ('party', 'member-level'),
    'company-subject': ('system', 'company-subject'),
    # 往来单位：库里历史码用的是 `partner:merge`，但代码/表/包的主词汇是
    # `biz_party` / `PartyController` / `cn.aiedge.erp.party`。
    # 两者本是同一业务对象被拆到两个域 ⇒ 统一到 `party`（`partner:merge` 已随本轮改名）。
    'partner': ('party', None),
}

MODULES = {
    'budget': {
        'label': '预算',
        'slot': 0,
        'domain': 'budget',
        'dirs': ['backend/erp/erp-budget/src/main/java/cn/aiedge/erp/budget/controller'],
    },
    'fixedasset': {
        'label': '固定资产',
        'slot': 1,
        'domain': 'fixedasset',
        'dirs': ['backend/erp/erp-fixed-asset/src/main/java/cn/aiedge/erp/fixedasset/controller'],
    },
    'stock': {
        'label': '库存/商品',
        'slot': 2,
        'domain': 'stock',
        'dirs': ['backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/controller'],
    },
    'marketing': {
        'label': '营销',
        'slot': 3,
        'domain': 'marketing',
        'dirs': ['backend/erp/erp-marketing/src/main/java/cn/aiedge/erp/marketing/controller'],
    },
    'party': {
        'label': '往来单位',
        'slot': 4,
        'domain': 'party',
        'dirs': ['backend/erp/erp-partner/src/main/java/cn/aiedge/erp/party/controller'],
    },
    'invoice': {
        'label': '发票',
        'slot': 5,
        'domain': 'invoice',
        'dirs': ['backend/erp/erp-finance/src/main/java/cn/aiedge/erp/invoice/controller'],
    },
    'payment': {
        'label': '财务收付',
        'slot': 6,
        'domain': 'payment',
        'dirs': ['backend/erp/erp-finance/src/main/java/cn/aiedge/erp/payment/controller'],
    },
    #   wms —— 曾标 manual_only（PDA 设备鉴权策略未定），该阻塞已解除，见下方 wms 条目；
    #   b2b —— 曾标 manual_only（`/api/v1/mall/**` 是 C 端公开接口）。**范围已收窄**：
    #          C 端仍整类排除，后台管理侧照常补码，见下方 b2b 条目。
    # ── 2026-09-21 E-01 wms 批次 ──
    # 原标 `manual_only`（理由："PDA 设备鉴权策略未定"）。**该阻塞已解除**：
    #   · 代码实测 PDA 用真实 `sys_user` 账号 + BCrypt 登录，并且和后台**共用同一套
    #     Sa-Token 会话/token/权限链路**（`PdaAuthController` 直接用 `StpUtil`，
    #     `SaTokenJwtConfig` 全文件被注释、无自定义 StpLogic，token-name 同为 Authorization）；
    #   · 所以**不需要设备令牌**，口径定为「PDA 复用后台同名资源的码」，见 base_overrides。
    # 另外实测：库中 11 条 `wms:*` 码**全部属于 erp-stock 的 `/api/erp/warehouse*`**
    # （api_path 已核），与 wms 模块的 172 个端点零交集 ⇒ wms 是**零码模块**，需现场建码。
    'wms': {
        'label': '仓储',
        'name_prefix': '仓储',
        'slot': 7,
        'domain': 'wms',
        # 递归到模块根：`InventoryReconcileController` 不在 controller/ 下而在 inventory/service/ 下
        'dirs': ['backend/wms/src/main/java/cn/aiedge/wms'],
        # ⚠️ 安全阀：这两类端点**绝不能加权限注解**
        'skip_bases': [
            '/api/erp/wms',   # ErpCallbackController：内部 outbox 回调，调用方不带任何认证头
            '/api/wms/erp',   # ErpIntegrationController：外部系统入站指令，无用户会话
        ],
        'skip_methods': {
            # 登录端点在 SaTokenConfig 两处白名单里（未登录必须可达）；登出不能要求权限码，
            # 否则"没有该码的用户无法登出"。
            '/api/v1/warehouse': [('POST', r'/auth/(login|logout)$')],
        },
        'base_overrides': {
            # PDA 的类级路径是 /api/v1/warehouse/<资源>；推导出的「域:资源」全是 `wms`
            # （容器段 v1 之后的 warehouse 被 DOMAIN_ALIAS 吃掉、res 为空）⇒ 八个控制器
            # 会共用一个键互相覆盖，必须按**类级路径**逐个指定资源。
            # ⚠️ PDA 这个控制器叫 receive、后台叫 receipt —— 同一业务对象两种拼写。
            # 口径是「PDA 复用后台的码」，故这里统一到后台的 receipt，不要建 wms:receive:*。
            '/api/v1/warehouse/receive':   {'resource': 'receipt'},
            '/api/v1/warehouse/putaway':   {'resource': 'putaway'},
            '/api/v1/warehouse/pick':      {'resource': 'pick'},
            '/api/v1/warehouse/ship':      {'resource': 'ship'},
            '/api/v1/warehouse/move':      {'resource': 'move'},
            '/api/v1/warehouse/check':     {'resource': 'check'},
            '/api/v1/warehouse/inventory': {'resource': 'inventory'},
            '/api/v1/warehouse/tasks':     {'resource': 'task'},
        },
        'code_rules': {
            '/api/wms/inventory': [
                ('POST', r'/increase$',       'wms:inventory:update'),
                ('POST', r'/decrease$',       'wms:inventory:update'),
                ('POST', r'/move$',           'wms:inventory:update'),
                ('POST', r'/unfreeze$',       'wms:inventory:release'),
                ('POST', r'/init-from-erp$',  'wms:inventory:update'),
            ],
            '/api/wms/borrow': [
                ('POST', r'/convert-(purchase|sale)$', 'wms:borrow:convert'),
                ('POST', r'/return(-items|-page)?$',   'wms:borrow:return'),
            ],
            '/api/wms/event': [
                ('POST', r'/outbox/process$', 'wms:event:execute'),
            ],
            '/api/wms/location': [
                ('POST', r'/plan/generate/\{[^/}]+\}$', 'wms:location:generate'),
                ('POST', r'/\{[^/}]+\}/batch-delete$',  'wms:location:delete'),
            ],
            '/api/wms/ship': [
                ('POST', r'/\{[^/}]+\}/scan$', 'wms:ship:update'),
            ],
            '/api/wms/pick': [
                ('POST', r'/detail/(confirm|shortage)$', 'wms:pick:update'),
            ],
            # ── PDA 端的"扫码进单/缺货上报"：都是**改单**，通用兜底会判成 create ──
            '/api/v1/warehouse/receive':  [
                ('POST', r'/\{[^/}]+\}/scan$',      'wms:receipt:update'),
                # 收货异常上报：改的是这张收货单，不是新建单
                ('POST', r'/\{[^/}]+\}/exception$', 'wms:receipt:update'),
            ],
            '/api/v1/warehouse/putaway':  [('POST', r'/\{[^/}]+\}/scan$',     'wms:putaway:update')],
            '/api/v1/warehouse/ship':     [('POST', r'/\{[^/}]+\}/scan$',     'wms:ship:update')],
            '/api/v1/warehouse/pick':     [('POST', r'/\{[^/}]+\}/shortage$', 'wms:pick:update')],
            # PDA 的"开始任务"（PUT）也只是改任务状态，但语义上更接近"执行"
            '/api/v1/warehouse/tasks':    [('PUT',  r'/\{[^/}]+\}/start$',    'wms:task:execute')],
        },
    },
    # ── 2026-09-21 E-01 b2b 批次：把「商城」拆成 C 端（排除）与后台（补码）两侧 ──
    # 原条目只有 `manual_only: True`（理由：`/api/v1/mall/**` 是 C 端公开接口）。
    # 本批**保留该结论，但把范围收窄到 C 端**，后台管理侧照常补码。
    #
    # 【为什么不给 C 端补码（证据，不是印象）】
    #   ① `MallAuthServiceImpl#login` / `#switchIdentity` 用的是
    #      `StpUtil.login(user.getId())`，登录主体是 **shop_user（商城买家）**，
    #      不是 `sys_user`；
    #   ② `@SaCheckPermission` 由 Sa-Token 按**登录 id** 去查该主体的角色/权限
    #      （本仓 `StpInterface` 实现只认 sys_user + sys_role_permission）—— shop_user
    #      的 id 查不到任何码 ⇒ 补码 = **所有商城顾客（含已登录买家）一律 403**；
    #   ③ 其中 `/api/v1/mall/auth/**` 与 `/api/v1/mall/products/**` 还写在
    #      `SaTokenConfig` 的**两处** excludePathPatterns 里（未登录也必须可达）。
    #   故 C 端**整类排除**（skip_bases），理由与写法同 wms 的 `/api/erp/wms`（无会话）一类。
    #
    # 【后台侧为什么必须补】`/api/erp/mall/admin/**` 是 pc-admin 员工会话在调
    #   （`frontend/apps/pc-admin/src/api/erp/mall.ts`），而它今天**只被全局
    #   checkLogin 保护** —— 商城顾客同样持有 Sa-Token 会话 ⇒ 现状是「任何 C 端买家
    #   都能调后台管理接口」（既有越权面）。补码顺带把这个洞收掉。
    #
    # 【域口径】沿用库里既有的 `mall:`（sys_module_permission 里 `mall:` → trade），
    #   不新建一级域。管理侧的资源名按方法路径语义拆开（config / user / banner /
    #   template / decoration / product / order / trade-analysis），不落成 `mall:admin:*`
    #   这种「一个 admin 资源管住 41 个端点」的粗粒度（那样"能看商城配置的人就能看订单统计"）。
    #
    # 【id 号段】复用既有槽位 slot=8（权限码 108000 起、角色关联 9580000 起）。
    'b2b': {
        'label': '商城',
        'slot': 8,
        'domain': 'mall',
        'dirs': ['backend/erp/erp-mall/src/main/java/cn/aiedge/erp/b2b/controller'],
        # ── 安全阀：C 端（顾客/游客）整类不加权限注解 ──
        'skip_bases': [
            '/api/v1/mall/auth',       # 登录/注册/登出/身份列表：未登录必须可达（在两处白名单里）
            '/api/v1/mall/products',   # 商品浏览：游客进店看货（在两处白名单里，准入在 MallProductServiceImpl#requireShop）
            '/api/v1/mall/cart',       # 购物车：登录买家自助
            '/api/v1/mall/orders',     # 我的订单：登录买家自助
            '/api/v1/mall/party-link', # 企业身份关联申请/审批：登录买家自助
            '/api/v1/mall/payments',   # 支付发起 + 渠道回调（回调故意不在白名单，见类注释的三步启用说明）
            '/api/v1/mall/user',       # 我的资料/地址本：登录买家自助
            # 商城公告（商城端公开查询）：类注释写明「仅返回已发布公告」，属 C 端读，
            # 买家登录主体无任何权限码 ⇒ 补码即 403。管理侧公告是 /api/erp/mall/admin/notice。
            '/api/erp/mall/notice',
        ],
        # ── 类级路径没有资源段的管理侧控制器：显式给出资源名 ──
        # 不指定的话推导会产出 `mall:admin-keyword:*` / `mall:admin-notice:*` /
        # `mall:admin-popup-ad:*` —— 把「admin」这个**容器词**当成资源名。
        'base_overrides': {
            '/api/erp/mall/admin/keyword':   {'domain': 'mall', 'resource': 'keyword'},
            '/api/erp/mall/admin/notice':    {'domain': 'mall', 'resource': 'notice'},
            '/api/erp/mall/admin/popup-ad':  {'domain': 'mall', 'resource': 'popup-ad'},
        },
        'code_rules': {
            # ⚠️ `/api/erp/mall/admin` 这个 base **被两个控制器共用**
            #    （MallAdminController 40 个端点 + MallTradeAnalysisController 的
            #    `/trade-analysis`），且它是个「一个大控制器管七种对象」的形状
            #    （配置/买家/轮播图/模板/装修/商品/订单）⇒ 逐条给**完整码**，
            #    不落成 `mall:admin:*`。这里**一律写完整码**：本 base 无 override，
            #    但下一屏的 `/api/erp/mall/admin/popup-ad` 有，风格统一、也避免
            #    将来有人给本 base 加 override 时 `@动作词` 简写被绕过。
            '/api/erp/mall/admin': [
                # ── 商城配置 ──
                ('GET',  r'/config$',           'mall:config:view'),
                ('PUT',  r'/config$',           'mall:config:update'),
                # ── 买家账号（shop_user；页面「商城 → 买家账号」）──
                ('GET',    r'/user/page$',      'mall:user:list'),
                # 审核通过与驳回同一码：都是"审核这个买家账号"的一个决定
                # （`sale:order:approve` 也是这么覆盖 approve/reject 两条路径的）。
                ('PUT',    r'/user/\{[^/}]+\}/(approve|reject)$', 'mall:user:approve'),
                # 启用/禁用是"改这个账号"，不是新建；用 update 而不是 status ——
                # 与同 base 的其它状态推进（装修/商品）口径一致，避免同一资源两套词。
                ('PUT',    r'/user/\{[^/}]+\}/status$',           'mall:user:update'),
                ('PUT',    r'/user/\{[^/}]+\}$',                  'mall:user:update'),
                ('DELETE', r'/user/\{[^/}]+\}$',                  'mall:user:delete'),
                # ── 轮播图 ──
                ('GET',    r'/banner$',         'mall:banner:list'),
                ('POST',   r'/banner$',         'mall:banner:create'),
                ('PUT',    r'/banner/\{[^/}]+\}$',    'mall:banner:update'),
                ('DELETE', r'/banner/\{[^/}]+\}$',    'mall:banner:delete'),
                # ── 页面模板（我的模板 + 行业模板库）──
                # 裸读端点 `GET /template/list`、`/template/library` 都是**取列表**：
                # RULES 的 GET 兜底只认 `list` 段，`library` 会被判成 view ⇒ 显式归 list。
                ('GET',  r'/template/(list|library)$', 'mall:template:list'),
                ('POST', r'/template$',                'mall:template:create'),
                # 引用行业库模板 = 复制成一条新的「我的模板」⇒ create（真正的写操作）
                ('POST', r'/template/library/\{[^/}]+\}/reference$', 'mall:template:create'),
                # ── 装修配置 ──
                ('GET',    r'/decoration$',                        'mall:decoration:list'),
                # 已关联商品集合是这条装修配置的**组成部分** ⇒ 并入 detail/update
                ('GET',    r'/decoration/\{[^/}]+\}/products$',    'mall:decoration:detail'),
                ('GET',    r'/decoration/\{[^/}]+\}$',             'mall:decoration:detail'),
                ('POST',   r'/decoration$',                        'mall:decoration:create'),
                ('PUT',    r'/decoration/\{[^/}]+\}/products$',    'mall:decoration:update'),
                ('PUT',    r'/decoration/\{[^/}]+\}$',             'mall:decoration:update'),
                ('DELETE', r'/decoration/\{[^/}]+\}$',             'mall:decoration:delete'),
                # ── 商品（上架/编辑，数据源 v_mall_product 视图 + erp_product）──
                ('GET',    r'/product/page$',       'mall:product:list'),
                ('POST',   r'/product$',            'mall:product:create'),
                ('PUT',    r'/product/\{[^/}]+\}$', 'mall:product:update'),
                ('DELETE', r'/product/\{[^/}]+\}$', 'mall:product:delete'),
                # ── 商城订单（后台侧；C 端订单在 /api/v1/mall/orders，已排除）──
                ('GET',  r'/order/(page|page-detail)$', 'mall:order:list'),
                # 统计卡（总单数/GMV/客单价/退款率）是汇总读 ⇒ view
                # （对齐库中既有的 `sale:order:view`，它同样挂在 /order/stats 上）。
                ('GET',  r'/order/stats$',              'mall:order:view'),
                ('GET',  r'/order/\{[^/}]+\}$',         'mall:order:detail'),
                # 审核通过与驳回同一码
                ('PUT',  r'/order/\{[^/}]+\}/(approve|reject)$', 'mall:order:approve'),
                ('PUT',  r'/order/batch-approve$',               'mall:order:approve'),
                # 收款（/pay 与 /receive 是同一实现的两个入口）⇒ 复用库中既有动作词 payment
                # （`sale:order:payment` 先例），不新造 receive 码。
                ('POST', r'/order/\{[^/}]+\}/(pay|receive)$',    'mall:order:payment'),
                # 发货 ⇒ ship（库中既有动作词，`sale:order:ship` 先例）
                ('POST', r'/order/\{[^/}]+\}/ship$',             'mall:order:ship'),
                ('PUT',  r'/order/batch-ship$',                  'mall:order:ship'),
                # 退款是**独立的敏感动作**（不是"编辑订单"）⇒ 用专门的 refund 码；
                # `REFUND` 在库中还不是动作词，本批在 ACTION_LABEL 里补中文标签。
                ('POST', r'/order/\{[^/}]+\}/refund$',           'mall:order:refund'),
                # 强制终止 = 置为已取消 ⇒ 复用库中既有动作词 cancel
                ('POST', r'/order/\{[^/}]+\}/terminate$',        'mall:order:cancel'),
                # ── 交易分析（MallTradeAnalysisController，与上面共用同一个 base）──
                ('GET',  r'/trade-analysis$',                    'mall:trade-analysis:view'),
            ],
            # 弹窗广告的"下线"是 POST（公告的"下线"是 PUT，通用规则已判 update）：
            # 通用 POST 兜底会判成 create（"新建一个已经存在的弹窗"）⇒ 显式归 update。
            '/api/erp/mall/admin/popup-ad': [
                ('POST', r'/\{[^/}]+\}/offline$', 'mall:popup-ad:update'),
            ],
        },
    },

    # ── 2026-09-21 新增：E-02 僵尸码 A 类（"有端点但没注解"）与 E-03 缺 api_path 的码
    #    集中的模块。这七个模块的码**早已在库**（不是零码模块），所以它们的正确用法是
    #      `--apply` 把注解补到端点上 + 用推导结果回填 `api_path`，
    #    **不是**再生成一份种子迁移（那会造出重复码）。
    #    dirs 给到模块根即可 —— scan/apply 已改为递归。
    'hr': {
        'label': '人力资源',
        'slot': 9,
        'domain': 'hr',
        'dirs': ['backend/hr/hr-base/src/main/java/cn/aiedge'],
    },
    'crm': {
        'label': '客户关系',
        # 模块名已改「客户服务」，但库里既有 crm 码的 permission_name 全是「CRM…」；
        # 新码沿用 CRM 前缀，避免同一模块两种叫法
        'name_prefix': 'CRM',
        'slot': 10,
        'domain': 'crm',
        'dirs': ['backend/crm/src/main/java/cn/aiedge'],
        # ── 2026-09-21 E-01 crm 批次：为什么这个域必须显式声明映射 ──
        # 通用推导出的 69 个码与库中 57 个历史码**只交集 8 条**：
        #   ① 历史码的动作词是 view / edit（不是 list / detail / update）；
        #   ② 通用规则的 POST 兜底是 create ⇒ `POST /{id}/sign`、`/{id}/terminate`、
        #      `/mark-expired` 全被判成"新建合同"；
        #   ③ CustomerController 的类级路径原为 /api/customer（全模块唯一不守 /api/crm/* 约定的），
        #      会被推导成 party:customer:*，而库中这套码挂在 crm:customer:*。
        #      ⚠️ 2026-09-26 已把该类级路径统一为 /api/crm/customer，正常情况下推导结果即为
        #      crm:customer:*；下面的 'party:customer' 覆盖保留，用于向前兼容历史路径的推导。
        # 继续用推导结果 = 再造一套平行命名空间（本仓已踩过 product:* 那次），
        # 故对**有历史码的资源**逐条对齐；只有词表里确实没有的动作才新增码
        # （新增清单由 `--apply` 的 missing 报告给出，再人工写种子迁移）。
        'resource_overrides': {
            'party:customer': {
                'domain': 'crm', 'resource': 'customer',
                # export 无独立码 → 归 list（导出就是列表的另一种输出）；
                # status 是"改客户状态" → 归 update
                'actions': {'detail': 'view', 'export': 'list', 'status': 'update'},
            },
            'crm:customer': {
                'domain': 'crm', 'resource': 'customer',
                # export 无独立码 → 归 list（导出就是列表的另一种输出）；
                # status 是"改客户状态" → 归 update
                'actions': {'detail': 'view', 'export': 'list', 'status': 'update'},
            },
            'crm:lead':        {'actions': {'list': 'view', 'detail': 'view', 'update': 'edit'}},
            'crm:opportunity': {'actions': {'list': 'view', 'detail': 'view', 'update': 'edit'}},
            'crm:quotation':   {'actions': {'list': 'view', 'detail': 'view', 'update': 'edit'}},
            'crm:contract':    {'actions': {'list': 'view', 'detail': 'view', 'update': 'edit'}},
            # followUp 是唯一 camelCase 的资源名，与兄弟资源（customer-pool / quotation-template）
            # 的连字符风格不一致；建码时统一成 follow-up，避免同一域三种写法
            'crm:followUp':    {'resource': 'follow-up'},
        },
        'code_rules': {
            # ── 合同：POST 上挂着 7 类语义完全不同的动作，只能按路径区分 ──
            '/api/crm/contract': [
                ('POST', r'$',                                 'crm:contract:create'),
                ('POST', r'/from-quotation/\{[^/}]+\}$',       'crm:contract:create'),
                ('POST', r'/\{[^/}]+\}/submit$',               'crm:contract:edit'),
                ('POST', r'/\{[^/}]+\}/(approve|reject)$',     'crm:contract:approve'),
                ('POST', r'/\{[^/}]+\}/sign$',                 'crm:contract:sign'),
                ('POST', r'/\{[^/}]+\}/renew$',                'crm:contract:renewapply'),
                ('POST', r'/mark-expired$',                    'crm:contract:refresh'),
                # 生命周期推进（生效/完成/终止/取消）与子表写入一并归 edit：库中 contract
                # 没有对应动作码，而"能改合同的人才能推进它"与本文件既有的 submit→edit 同口径。
                # （改前的真实状态是：**任何登录用户**都能终止/取消合同。）
                ('POST', r'/\{[^/}]+\}/(effective|complete|terminate|cancel)$', 'crm:contract:edit'),
                ('POST', r'/\{[^/}]+\}/(clauses|attachments|payments|changes)$', 'crm:contract:edit'),
                ('POST', r'/\{[^/}]+\}/payments/\{[^/}]+\}/confirm$',           'crm:contract:edit'),
                ('POST', r'/\{[^/}]+\}/changes/\{[^/}]+\}/(approve|reject)$',   'crm:contract:approve'),
                ('POST', r'/\{[^/}]+\}/changes/\{[^/}]+\}/execute$',            'crm:contract:edit'),
                ('DELETE', r'/\{[^/}]+\}/(clauses|attachments)/\{[^/}]+\}$',    'crm:contract:edit'),
                # 导出复用历史码 download（同义动作，不新造 export 码）
                ('GET', r'/export$',                            'crm:contract:download'),
            ],
            # ── 线索 / 商机 / 报价：把**历史里本就为此存在的动作码**接上 ──
            # 这些码（convert / batchconvert / send / …）在库里躺了很久没有消费方，
            # 通用推导只会把它们判成 create/edit，等于把专用码浪费掉。
            '/api/crm/lead': [
                ('POST', r'/\{[^/}]+\}/convert$',   'crm:lead:convert'),
                ('POST', r'/batch-convert$',        'crm:lead:batchconvert'),
            ],
            '/api/crm/opportunity': [
                # 赢单/输单/推进阶段都是"改这个商机"，库中没有更专的码
                ('POST', r'/\{[^/}]+\}/(advance|win|lose)$',  'crm:opportunity:edit'),
            ],
            '/api/crm/quotation': [
                ('POST', r'/\{[^/}]+\}/send$',      'crm:quotation:send'),
                ('POST', r'/\{[^/}]+\}/convert$',   'crm:quotation:convert'),
                ('POST', r'/\{[^/}]+\}/(accept|reject-by-customer|cancel|mark-expired)$',
                                                    'crm:quotation:edit'),
                ('POST', r'/\{[^/}]+\}/items(/.*)?$', 'crm:quotation:edit'),
                # 导出复用历史码 downloadpdf（同义动作，不新造 export 码）
                ('GET', r'/export$',                'crm:quotation:downloadpdf'),
            ],
            '/api/crm/customer': [
                ('POST', r'/\{[^/}]+\}/follow$',    'crm:customer:follow'),
            ],
            # 历史路径（2026-09-26 前 CustomerController 的前缀），保留以便旧分支重新生成时不致错码
            '/api/customer': [
                ('POST', r'/\{[^/}]+\}/follow$',    'crm:customer:follow'),
            ],
            # ── 零码资源的共同口径：状态/生命周期类 POST 归 update，只有真正新建对象才归 create ──
            # 通用规则的 POST 兜底是 create，会把"启动/暂停/取消活动""领取/退回公海池"
            # 一并算成"新建"，那是明显的语义错位（"能建活动的人才能取消活动"说不通）。
            '/api/crm/marketing': [
                ('POST', r'/\{[^/}]+\}/(schedule|start|pause|resume|complete|cancel|update-progress)$',
                         'crm:marketing:update'),
                ('POST', r'/\{[^/}]+\}/(targets|executions)(/.*)?$', 'crm:marketing:update'),
            ],
            '/api/crm/quotation-template': [
                ('POST', r'/\{[^/}]+\}/(activate|deactivate)$', 'crm:quotation-template:update'),
            ],
            '/api/crm/customer-pool': [
                ('POST', r'/(put|claim|return)/\{[^/}]+\}$',   'crm:customer-pool:update'),
                ('POST', r'/(auto-recovery|check-expired)$',   'crm:customer-pool:update'),
            ],
        },
    },
    # core-api 一个根就覆盖 system / user / role / permission / department / position /
    # tenant / platform / datasource / scheduler / docquery 等域（域由**类级路径**推导，与目录无关）
    #
    # ── 2026-09-21 E-01 coreapi 批次 ─────────────────────────────────────────
    # 【为什么这个模块比前六个难】core-api 不是"一个业务模块"，它**就是平台本身**：
    #   ① 目录一个根（api/assistant、department、export、integration…），域只能按类级路径推；
    #   ② 大量控制器的类级路径**没有资源段**（`/api/assistant`、`/api/cache`、`/api/config`、
    #      `/api/dashboard`、`/api/department`、`/api/import`、`/api/export`、`/api/file`…），
    #      生成器兜底会拿**方法路径的第一段**当资源名 ⇒ 碎片化（`system:session:*`、
    #      `system:status:*`、`system:map:*`、`system:value:*`）甚至坏码（`file:**:view`，
    #      方法路径是 `/view/**`）。下面用 base_overrides 逐个指定真实资源名；
    #   ③ 里面**混着全站共享基础设施**（字典下拉、文件上传、工作台、个人中心、权限自省）——
    #      这类端点加权限码会让**所有非超管的通用能力整块 403**，故按铁律"宁可漏补"排除，
    #      逐条理由写在 skip_* 里。
    #
    # 【域口径】`system:` 前缀的码只给系统租户（已拍板），于是：
    #   · 租户级管理功能（部门 / 岗位 / 岗位分类）→ `tenant-admin:`（照库中既有 72 条同族码）；
    #   · 平台/运维/系统租户功能（配置、缓存、字典、监控、MQ、集成、报表、存储、租户…）→ `system:`；
    #   · 单据查询中心 → 沿用既有 `doc:` 域（与 `doc:void` / `doc:unapprove` 同族）。
    #   一律**先查库再建码**：能对上历史码的用 code_rules 接过去，不新建同义码。
    #
    # 【id 号段】权限码 111000 起（slot=11）、角色关联 9610000 起，实测两段均空闲。
    'coreapi': {
        'label': '平台与系统',
        'slot': 11,
        'domain': 'system',
        'dirs': ['backend/core/api/core-api/src/main/java/cn/aiedge'],
        # ── 安全阀：以下**整类**不加权限注解 ──
        'skip_bases': [
            # 已用 @SaCheckRole("admin") 做角色级门禁（比权限码更严）。⚠️ 库中**没有** role_code
            # = 'admin' 的角色（只有 SUPER_ADMIN / SYSTEM_ADMIN / DEPT_ADMIN / E2E_T2_ADMIN），
            # 即该注解当前会把所有人挡下 —— 这是**既有缺陷**，不是本批造成的，也不在本批修
            # （只报告）。这里再加权限码毫无意义：注解已让接口不可达，且会掩盖上面这个缺陷。
            '/api/admin/fix',
            # 工作台首页：pc-admin 的路由默认落地页（`dynamicRoutes.ts:1687` 只有 requiresAuth，
            # 没有权限门槛），`views/dashboard/index.vue` 无条件调用 stats/trend/todos。
            # 加码 ⇒ 所有非超管一登录首页就报错，属"错补"，故整类排除。
            '/api/dashboard',
            # 全站下拉字典数据源：`api/options.ts` 的 getDict(`GET /dict/{code}`) / getDicts
            # (`POST /dict/batch`) 被各页面公用。字典"读"不是可勾选的功能，加码会让所有
            # 非超管的表单下拉集体清空。（本控制器只有这 2 个端点，全是读/批量读。）
            '/api/dict',
            # 跨模块基础数据选择器（业务员/仓库下拉，见 `api/sales-analysis.ts`）：是数据源不是功能。
            '/api/erp/basic',
            # ① `GET /file/view/**` **在 SaTokenConfig 两处白名单里**（<img> 直接引用，不带
            #    Authorization 头，未登录必须可达）；② `POST /file/upload` 是平台通用上传契约
            #    （12+ 处表单/附件/企业 LOGO 复用，见 `md/components/AttachmentUpload.vue`）。
            #    给 upload 加码 ⇒ 非超管无法上传任何附件/LOGO。
            '/api/file',
            # 个人中心自助接口（改资料/改密码/头像/偏好）：任何登录用户必须可用，
            # 不存在"给某角色关掉个人中心"的语义。
            '/api/profile',
        ],
        # ── 安全阀：以下**单个端点**不加权限注解 ──
        'skip_methods': {
            # 字典项的"按字典编码取值"（`GET /dict/item/code/{dictCode}`）是**跨页面下拉源**：
            # `api/dict.ts#getByDictCode` 被 `system/user`（USER_TYPE）、`system/role`
            # （ROLE_TYPE）、`system/position`（POSITION_LEVEL）、`system/data-import`
            # （BILL_TYPE）四个管理页引用 —— 那些页面的权限码（tenant-admin:user/role/position:*）
            # 与字典码是两套，若在这里要求 `system:dict:list`，会让这些页面在非超管手里缺下拉数据。
            # 字典页自身用的 `/dict/item/type/{dictTypeId}` 等仍补码。
            '/api/dict/item': [('GET', r'/code/\{[^/}]+\}$')],
            # 部门下拉源。`/department/options` 的接口说明自己写着"含禁用部门，供全站部门下拉使用"，
            # `/department/list` 被 `api/options.ts#getDepartments` 引用（20+ 业务表单：库存/财务/
            # 采购…），`/department/tree` 被 hr/analytics 多页引用。加码 ⇒ 非超管这些下拉全部为空。
            # 保留 `/department/page`（部门管理页列表）与全部写端点。
            '/api/department': [('GET', r'/(list|tree|options)$')],
            # 反馈的**自助**侧：提交反馈（POST /submit*）与"我的反馈"（GET /my/*）。
            # 管理侧（list/detail/replies/statistics）仍补码。
            '/api/feedback': [('POST', r'/submit(/.*)?$'), ('GET', r'/my/.*$')],
            # 当前租户信息：前端应用启动/顶部栏要读它（租户名、模块开关），任何登录用户都要用。
            '/api/tenant': [('GET', r'/current$')],
            # 租户自助注册：控制器注释写明「公开接口，无需登录」，但 SaTokenConfig 两处白名单里
            # **没有** `/api/tenant-registration/register` ⇒ 现状实际是"登录后可达"，与设计不符
            # （既有缺陷，只报告）。这里加权限码会让它彻底不可用（连超管都救不回来），故排除。
            '/api/tenant-registration': [('POST', r'/register$')],
            # 当前用户**自身**权限/角色的自省接口：前端登录后必调它构建菜单。库中同类的
            # `tenant-admin:permission:view` 是"看**别人**的权限"（挂在 /user/{userId}/permissions），
            # 若把它套到自己的自省接口上，所有非超管将拿不到菜单 ⇒ 整站不可用，属最典型的"错补"。
            '/api/user-permission': [('GET', r'/current/.*$')],
            # 外部系统**入站** webhook（`POST /api/integration/webhook/{configId}`，方法体只记日志
            # 并原样返回成功，无验签、无会话依赖）。正确的做法是补服务间鉴权，不是塞权限码 ——
            # 加码后外部调用方（拿不到员工会话）永远打不通，而它本来也不该按"用户权限"判定。
            # 出站发送 `POST /webhook/send/{configId}` 是管理端动作，仍补码。
            '/api/integration': [('POST', r'/webhook/\{[^/}]+\}$')],
        },
        # ── 类级路径**没有资源段**时的资源名（域:资源）──
        'base_overrides': {
            '/api/assistant':  {'domain': 'system', 'resource': 'assistant'},
            '/api/cache':      {'domain': 'system', 'resource': 'cache'},
            '/api/config':     {'domain': 'system', 'resource': 'config'},
            # 租户级管理：库里历史码是 `tenant-admin:department:*` / `tenant-admin:position:*`
            # （V11.456.0 从裸 `department:`/`position:` 改名而来），动作词是
            # list / query / create / edit / delete（不是 detail / update）⇒ 用 actions 对齐。
            '/api/department': {'domain': 'tenant-admin', 'resource': 'department',
                                'actions': {'detail': 'query', 'update': 'edit',
                                            'status': 'edit', 'export': 'list'}},
            # ⚠️ 岗位分类**必须复用岗位的码**（resource=position），不能另立 `position-category`：
            #    `PositionCategoryController` 的 6 个方法上**早就写着** `@RequiresPermission(
            #    "tenant-admin:position:*")`（另一套注解，由 core-base 的 PermissionAspect 真实执行，
            #    见 `cn.aiedge.common.permission.PermissionAspect`）。若在这里另建同义的
            #    `tenant-admin:position-category:*`，本批插的注解会与既有注解**叠加成两个都必须满足**
            #    ⇒ 有 position 权限的角色反而被拒（"错补"）。资源名与岗位对齐后，两处注解同码，
            #    行为与改前完全一致，且 `/list`、`/tree`、`/{parentId}/children` 三个**真的裸**的读端点
            #    也顺带补上既有的 `tenant-admin:position:list`（不新建码）。
            '/api/position/category': {'domain': 'tenant-admin', 'resource': 'position',
                                       'actions': {'detail': 'query', 'update': 'edit',
                                                   'status': 'edit', 'view': 'list'}},
            '/api/position':   {'domain': 'tenant-admin', 'resource': 'position',
                                'actions': {'detail': 'query', 'update': 'edit',
                                            'status': 'edit', 'export': 'list'}},
            # 字典项 / 字典类型两个控制器共用一个资源名：既有的 12 处注解（含 DictItemController
            # 的写端点）用的都是 `system:dict:*`，资源名必须跟它一致，否则同一子系统两套码。
            '/api/dict/item':  {'domain': 'system', 'resource': 'dict'},
            '/api/dict/type':  {'domain': 'system', 'resource': 'dict'},
            # 单据查询中心：归既有 `doc:` 域（同族码 doc:void / doc:unapprove / doc:draft:view-others）。
            '/api/docquery':   {'domain': 'doc', 'resource': 'docquery'},
            # 资源名取 `dataexport` 以对称库中既有的 `system:dataimport:*`。
            '/api/export':     {'domain': 'system', 'resource': 'dataexport'},
            '/api/feedback':   {'domain': 'system', 'resource': 'feedback'},
            '/api/gateway':    {'domain': 'system', 'resource': 'gateway'},
            # `/api/import` 与 `/api/import/v2` 是同一功能的两代实现 ⇒ 共用一个资源名。
            '/api/import':     {'domain': 'system', 'resource': 'import'},
            '/api/import/v2':  {'domain': 'system', 'resource': 'import'},
            '/api/import-templates': {'domain': 'system', 'resource': 'import-template'},
            '/api/integration': {'domain': 'system', 'resource': 'integration'},
            '/api/knowledge':  {'domain': 'system', 'resource': 'knowledge'},
            # 监控按"资源"细分（概览/告警/健康/基础设施/性能），便于按需开关。
            '/api/monitor':                {'domain': 'system', 'resource': 'monitor'},
            '/api/monitor/alerts':         {'domain': 'system', 'resource': 'monitor-alert'},
            '/api/monitor/health':         {'domain': 'system', 'resource': 'monitor-health'},
            '/api/monitor/infrastructure': {'domain': 'system', 'resource': 'monitor-infrastructure'},
            '/api/monitor/performance':    {'domain': 'system', 'resource': 'monitor-performance'},
            '/api/mq':         {'domain': 'system', 'resource': 'mq'},
            '/api/mq/enhanced': {'domain': 'system', 'resource': 'mq-enhanced'},
            '/api/recommendation':   {'domain': 'system', 'resource': 'recommendation'},
            '/api/report':           {'domain': 'system', 'resource': 'report'},
            '/api/report/analytics': {'domain': 'system', 'resource': 'report-analytics'},
            '/api/report/schedule':  {'domain': 'system', 'resource': 'report-schedule'},
            # 语义检索是搜索的一部分，与 /api/search 共用一个资源名。
            '/api/search':          {'domain': 'system', 'resource': 'search'},
            '/api/search/semantic': {'domain': 'system', 'resource': 'search'},
            # 分片上传属于文件存储 ⇒ 与 FileStorageController 共用一个资源名。
            '/api/storage':       {'domain': 'system', 'resource': 'storage'},
            '/api/storage/chunk': {'domain': 'system', 'resource': 'storage'},
            # 租户管理沿用既有 `system:tenant:*`（TenantController 已注解的 8 处就是这个）。
            '/api/tenant':        {'domain': 'system', 'resource': 'tenant'},
            '/api/tenant-module': {'domain': 'system', 'resource': 'tenant-module'},
        },
        # ── 动作词修正：POST 兜底是 create，下列端点语义不是"新建" ──
        'code_rules': {
            # 字典项"按字典类型取项列表"（`/dict/item/type/{id}`）返回的是列表，
            # 通用规则看到路径以 `{dictTypeId}` 结尾会判成 detail ⇒ 归 list。
            '/api/dict/item': [('GET', r'/type/\{[^/}]+\}$', 'system:dict:list')],
            # 智能助手：对话与执行动作是"用助手"（执行），建会话才是真正的新建。
            '/api/assistant': [('POST', r'/(chat|action)$', 'system:assistant:execute')],
            # 配置的 4 个裸端点（/map、/value/{k}、/types、/groups）都是"读配置"，
            # 库中 `system:config:list` 已存在 ⇒ 全部并入它，不新造 system:config:view 之类的同义码。
            '/api/config': [('GET', r'.*', 'system:config:list')],
            # 移动部门 = 改部门（不是新建）；批删与单删同码。
            '/api/department': [
                ('POST',   r'/\{[^/}]+\}/move$', 'tenant-admin:department:edit'),
                ('DELETE', r'/batch$',           'tenant-admin:department:delete'),
            ],
            # 岗位：分配/移除用户岗位用历史码 assign（remove 是 assign 的逆操作，库中无独立码，
            # 不新造 position:remove）；两个"按部门/按岗位查人"的读端点归 list/query。
            '/api/position': [
                ('POST',   r'/assign$',                  'tenant-admin:position:assign'),
                ('DELETE', r'/remove$',                  'tenant-admin:position:assign'),
                ('GET',    r'/dept/\{[^/}]+\}$',         'tenant-admin:position:list'),
                # 同步对齐既有的 @RequiresPermission：
                # `GET /{id}/users` 上原本写的就是 query，不能改成 list（两注解叠加=双重门禁）
                ('GET',    r'/\{[^/}]+\}/users$',        'tenant-admin:position:query'),
            ],
            # 通用导出/导入服务：POST 上挂的全是"导出/导入/下载模板"，通用兜底会判成 create。
            '/api/export': [
                ('POST', r'/((excel|csv|pdf)/export|pdf/report|batch/excel)$', 'system:dataexport:export'),
                ('POST', r'/(excel|csv)/import$',        'system:dataexport:import'),
                ('POST', r'/template/download$',         'system:dataexport:view'),
                ('GET',  r'/template/\{[^/}]+\}$',       'system:dataexport:view'),
            ],
            # 刷新网关路由 = 改路由表（不是新建路由）。
            '/api/gateway': [('POST', r'/routes/refresh$', 'system:gateway:update')],
            # 取消导入任务 = 改任务状态。
            '/api/import': [('POST', r'/cancel/\{[^/}]+\}$', 'system:import:update')],
            # v2 的 preview/validate 是"试算"，不落库 ⇒ 不能算 create（否则"能导入的人才
            # 能预览"）；preview 归 view、validate 归 check。
            '/api/import/v2': [
                ('POST', r'/preview/\{[^/}]+\}$',  'system:import:view'),
                ('POST', r'/validate/\{[^/}]+\}$', 'system:import:check'),
            ],
            # 模板校验（validate / validate-row）归 check（ACTION_LABEL 已有"校验"）。
            '/api/import-templates': [
                ('POST', r'/\{templateId\}/(validate|validate-row)$', 'system:import-template:check'),
            ],
            # 集成：同步触发 / 连通性测试 / 出站 webhook 都是"执行"，重试有专门动作词。
            # ⚠️ 顺序敏感：sync/records/{id}/retry 必须排在 sync/* 通配之前，否则被吃掉。
            '/api/integration': [
                ('POST', r'/sync/records/\{[^/}]+\}/retry$', 'system:integration:retry'),
                ('POST', r'/sync/.+$',                       'system:integration:execute'),
                ('POST', r'/configs/\{[^/}]+\}/toggle$',     'system:integration:update'),
                ('POST', r'/test/\{[^/}]+\}$',               'system:integration:execute'),
                ('POST', r'/webhook/send/\{[^/}]+\}$',       'system:integration:execute'),
            ],
            # 知识库：检索是读（归 list）；文档解析是改文档状态（归 update）；重建向量是执行。
            '/api/knowledge': [
                ('POST', r'/search$',                        'system:knowledge:list'),
                ('POST', r'/document/\{[^/}]+\}/parse$',     'system:knowledge:update'),
                ('POST', r'/embedding$',                     'system:knowledge:execute'),
            ],
            # 监控：`/metrics/trend/{name}` 被通用规则判成 detail（库里没有 monitor 的 detail 码），
            # 归 view；手动 GC 是执行。
            '/api/monitor': [
                ('GET',  r'/metrics/trend/\{[^/}]+\}$', 'system:monitor:view'),
                ('POST', r'/gc$',                       'system:monitor:execute'),
            ],
            # 告警：启用/停用、确认/解决、保存通知配置都是"改"，通知测试是"执行"。
            '/api/monitor/alerts': [
                ('POST', r'/rules/\{[^/}]+\}/(enable|disable)$',   'system:monitor-alert:update'),
                ('POST', r'/\{[^/}]+\}/(acknowledge|resolve)$',    'system:monitor-alert:update'),
                ('POST', r'/notification/config$',                 'system:monitor-alert:update'),
                ('POST', r'/notification/test$',                   'system:monitor-alert:execute'),
            ],
            # 性能指标的 trend/predict 带路径变量，会被判成 detail ⇒ 归 view。
            '/api/monitor/performance': [
                ('GET', r'/(trend|predict)/\{[^/}]+\}$', 'system:monitor-performance:view'),
            ],
            # 发消息（含延迟发送）是"执行"，不是"新建消息"。
            '/api/mq': [('POST', r'.*', 'system:mq:execute')],
            # 增强 MQ：发送=执行，重发=retry（ACTION_LABEL 已有"重试"）。
            '/api/mq/enhanced': [
                ('POST', r'/retry/.+$', 'system:mq-enhanced:retry'),
                ('POST', r'.*',         'system:mq-enhanced:execute'),
            ],
            # 取推荐是读（POST 传参查询），归 list。
            '/api/recommendation': [('POST', r'/get$', 'system:recommendation:list')],
            # 报表：预览是读（归 view）；三种导出归 export；复制是真新建（保持 create）。
            '/api/report': [
                ('POST', r'/\{[^/}]+\}/preview$',              'system:report:view'),
                ('POST', r'/\{[^/}]+\}/export/(excel|csv|pdf)$', 'system:report:export'),
            ],
            # 分析接口（同环比/排名/占比/异常…）都是"算完读出来"，不是新建报表。
            '/api/report/analytics': [('POST', r'.*', 'system:report-analytics:view')],
            # 立即触发一次报表任务 = 执行。
            '/api/report/schedule': [
                ('POST', r'/\{[^/}]+\}/trigger$', 'system:report-schedule:execute'),
            ],
            # 搜索：检索类（GET/POST 都是读）归 list；索引维护是改；删除历史/索引是删除。
            '/api/search': [
                ('POST', r'/index/.+$', 'system:search:update'),
                ('GET',  r'.*',         'system:search:list'),
                ('POST', r'.*',         'system:search:list'),
            ],
            '/api/search/semantic': [
                ('GET',  r'.*', 'system:search:list'),
                ('POST', r'.*', 'system:search:list'),
            ],
            # 租户配置沿用既有 `system:tenant:update`（TenantController 已注解的同一码）。
            '/api/tenant': [('PUT', r'/\{[^/}]+\}/config$', 'system:tenant:update')],
            # 模块授权只有查询（按租户看模块），统一归 view。
            '/api/tenant-module': [('GET', r'.*', 'system:tenant-module:view')],
            # 字段映射是"数据导入配置"的一部分：库里已有 `system:dataimport:*`（含 list/create/
            # update/delete），全部接过去，不新造 system:sync-field-mapping:* 平行命名空间。
            '/api/v1/sync-config': [
                ('GET',    r'/\{configId\}/field-mappings$',             'system:dataimport:list'),
                ('GET',    r'/\{configId\}/field-mappings/\{[^/}]+\}$',  'system:dataimport:list'),
                ('POST',   r'/\{configId\}/field-mappings$',             'system:dataimport:create'),
                ('POST',   r'/\{configId\}/field-mappings/batch$',       'system:dataimport:create'),
                ('POST',   r'/\{configId\}/field-mappings/init-template$', 'system:dataimport:create'),
                ('PUT',    r'/\{configId\}/field-mappings/\{[^/}]+\}$',  'system:dataimport:update'),
                ('DELETE', r'/\{configId\}/field-mappings/\{[^/}]+\}$',  'system:dataimport:delete'),
            ],
            # 同步历史是数据导入的运行记录 ⇒ 复用同一个 list 码。
            '/api/v1/sync-history': [('GET', r'.*', 'system:dataimport:list')],
            # ── 工作流：裸端点全是"我的待办/审批/撤回"这类**用户侧**动作 ──
            # 库中既有 workflow:task:view / :approve / :transfer、workflow:instance:view，
            # 能对上的全部接过去；对不上的（发起/撤回/取消/回调补偿日志）才新建码。
            '/api/workflow': [
                ('POST', r'/start$',                          'workflow:instance:start'),
                ('GET',  r'/instances/\{[^/}]+\}$',           'workflow:instance:view'),
                ('GET',  r'/instances/\{[^/}]+\}/status$',    'workflow:instance:view'),
                ('GET',  r'/my-applications$',                'workflow:instance:view'),
                ('GET',  r'/pending$',                        'workflow:task:view'),
                ('GET',  r'/pending/count$',                  'workflow:task:view'),
                ('GET',  r'/approved$',                       'workflow:task:view'),
                ('POST', r'/\{instanceId\}/(approve|reject)$', 'workflow:task:approve'),
                ('POST', r'/\{instanceId\}/transfer$',        'workflow:task:transfer'),
                # 撤回（发起人自己收回）与取消（终止流程）：库中只有 workflow:instance:intervene
                # （管理员干预），语义不同，故新建两个码，不合并。
                ('POST', r'/\{instanceId\}/withdraw$',        'workflow:instance:withdraw'),
                ('POST', r'/\{instanceId\}/cancel$',          'workflow:instance:cancel'),
                # 审批回调补偿日志（运维/排障侧）
                ('GET',  r'/callback-log/page$',              'workflow:callback-log:list'),
                ('POST', r'/callback-log/\{[^/}]+\}/retry$',  'workflow:callback-log:retry'),
            ],
        },
    },
    'erpfinance': {
        'label': '财务域其余',
        'slot': 12,
        'domain': 'finance',
        'dirs': ['backend/erp/erp-finance/src/main/java/cn/aiedge'],
        # ── 费用（expense）子域：**必须映射到历史码，否则会再造一套平行命名空间** ──
        # 库里已有 26 条 `erp:expense:*` 码，是**四段式**（`erp:expense:<资源>:<动作>`，
        # 资源 = application/approval/payment/reimbursement/statistics）。
        # 而按类级路径推导会得到 `expense:application:create` 这种**三段式**，
        # 两者对不上 —— 若放任推导，26 条历史码会永远是没人消费的僵尸码，
        # 同时多出一整套同义新码。故这里把域固定成 `erp`、资源前缀固定成 `expense:`，
        # 并沿用历史动作词（`edit`/`query` 而非 `update`/`detail`）。
        'base_overrides': {
            '/api/erp/expense':               {'domain': 'erp', 'resource': 'expense'},
            '/api/erp/expense/approval':      {'domain': 'erp', 'resource': 'expense:approval',
                                               'actions': {'detail': 'query', 'update': 'edit'}},
            '/api/erp/expense/payment':       {'domain': 'erp', 'resource': 'expense:payment',
                                               'actions': {'detail': 'query', 'update': 'edit'}},
            '/api/erp/expense/reimbursement': {'domain': 'erp', 'resource': 'expense:reimbursement',
                                               'actions': {'detail': 'query', 'update': 'edit'}},
            '/api/erp/expense/type':          {'domain': 'erp', 'resource': 'expense:type',
                                               'actions': {'detail': 'query', 'update': 'edit'}},
        },
        'code_rules': {
            # ExpenseController 一个类里混了四种对象（申请/统计/单据/审批），逐条对齐历史码
            '/api/erp/expense': [
                ('POST',   r'/application$',                          'erp:expense:application:create'),
                ('PUT',    r'/application/\{[^/}]+\}$',               'erp:expense:application:edit'),
                ('DELETE', r'/application/\{[^/}]+\}$',               'erp:expense:application:delete'),
                ('POST',   r'/application/\{[^/}]+\}/submit$',        'erp:expense:application:submit'),
                # 撤回 = 改申请单状态；历史码里没有 withdraw，复用 edit
                ('POST',   r'/application/\{[^/}]+\}/withdraw$',      'erp:expense:application:edit'),
                ('POST',   r'/apply$',                                'erp:expense:application:create'),
                ('GET',    r'/statistics(/.*)?$',                     'erp:expense:statistics:list'),
                ('GET',    r'/list$',                                 'erp:expense:application:list'),
                ('GET',    r'/\{[^/}]+\}$',                           'erp:expense:application:query'),
                ('PUT',    r'/\{[^/}]+\}$',                           'erp:expense:application:edit'),
                ('DELETE', r'/\{[^/}]+\}$',                           'erp:expense:application:delete'),
                # 费用审批：历史 `erp:expense:application:approve` 已在 E-02 批次 4 被删，
                # 正主是 `erp:expense:approval:process`（"费用审批处理"），复用它。
                ('POST',   r'/\{[^/}]+\}/(approve|reject)$',          'erp:expense:approval:process'),
            ],
        },
    },
    'dms': {
        'label': '配送',
        'slot': 13,
        'domain': 'dms',
        'dirs': ['backend/dms/src/main/java/cn/aiedge'],
        'skip_methods': {
            # 渠道回调在 SaTokenConfig 白名单里（外部运力平台回调，无会话、靠验签），
            # 加权限码没有意义且会误导后来者。
            '/api/dms/channel': [('POST', r'/callback$')],
        },
        'code_rules': {
            # 本域 211 个端点里有 60+ 处是**状态推进/业务动作**（抢单/竞价/核销/派单/优化/
            # 指派/反审核…），通用规则的 POST 兜底会把它们全判成 `create` ——
            # "确认收款"变成"新建收款单"、"指派任务"变成"新建任务"。
            # 统一用 `@update`（只改动作词，域与资源仍按路径解析）：
            # `.+` 不匹配空路径 ⇒ 裸 `POST <资源>`（真正的新建）仍保持 create。
            '/api/dms/order-pool':    [('POST', r'.+', '@update')],
            '/api/dms/payment':       [('POST', r'.+', '@update')],
            '/api/dms/task': [
                ('POST', r'.*/save$', '@create'),   # 保存=新建/更新，按新建口径
                ('POST', r'.+',       '@update'),
            ],
            '/api/dms/route':         [('POST', r'.+', '@update')],
            '/api/dms/route/fence':   [('POST', r'.+', '@update')],
            '/api/dms/settlement': [
                ('POST', r'/rule$',   '@create'),   # 新建结算规则
                ('POST', r'.+',       '@update'),
            ],
            '/api/dms/vehicle/energy':      [('POST', r'.+', '@update')],
            '/api/dms/verification':        [('POST', r'.+', '@update')],
            '/api/dms/rider':         [('POST', r'/location$', '@update')],
            '/api/dms/config':        [('POST', r'/\{[^/}]+\}/reset$', '@update')],
        },
    },
    'sales': {
        'label': '销售',
        'slot': 14,
        'domain': 'sale',
        'dirs': ['backend/erp/erp-sales/src/main/java/cn/aiedge'],
        'base_overrides': {
            # `/api/sales/**`（注意是 sales）会被推导成 `sales:` 域，而本域历史码统一是
            # `sale:` 前缀 ⇒ 归一到 sale，否则同一模块会分裂出 sale/sales 两个域前缀。
            '/api/sales/detail-query': {'domain': 'sale'},
            '/api/sales/doc-query':    {'domain': 'sale'},
            '/api/sales/price-track':  {'domain': 'sale'},
            '/api/sales/retail':       {'domain': 'sale'},
            '/api/sales/retail/shift': {'domain': 'sale', 'resource': 'retail-shift'},
        },
        'code_rules': {
            '/api/erp/sale/order': [
                # 注：`/{id}/ship` 与 `/{id}/payment` **已有** `sale:order:ship` / `:payment`
                # 注解（写在映射行之后，属本仓两种注解写法之一），故已从扫描里排除，不需要规则。
                ('POST', r'/\{[^/}]+\}/pick-complete$',   'sale:order:update'),
                ('POST', r'/batch-pick-complete$',        'sale:order:update'),
                ('POST', r'/batch-logistics-remark$',     'sale:order:update'),
            ],
            '/api/erp/product-kit': [
                ('POST', r'/\{[^/}]+\}/(activate|deactivate|items)$', 'product:kit:update'),
                ('POST', r'/batch-(activate|deactivate)$',            'product:kit:update'),
            ],
            '/api/erp/sale/outbound': [
                # 拣货/打包/发货的每一步都是"改这张出库单"，不是新建
                ('POST', r'/\{[^/}]+\}/(start-picking|complete-picking|start-packing|complete-packing|ship|items)$',
                         'sale:outbound:update'),
                ('POST', r'/\{[^/}]+\}/items/\{[^/}]+\}/(pick|pack)$', 'sale:outbound:update'),
                ('POST', r'/batch-logistics-remark$',                  'sale:outbound:update'),
            ],
            '/api/erp/sale/logistics': [
                ('POST', r'/packages/\{[^/}]+\}/acquire-waybill$',        'sale:logistics:update'),
                ('POST', r'/freight/reconcile-mark$',                     'sale:logistics:update'),
                ('POST', r'/shipment-notify/(\{[^/}]+\}/)?send(-pending)?$', 'sale:logistics:update'),
                ('POST', r'/\{orderId\}/packages$',                       'sale:logistics:create'),
            ],
            '/api/sales/retail': [
                # 结算 / 挂单 / 解挂 / 作废：都是改这张零售单
                ('POST', r'/\{[^/}]+\}/(settle|hold|unhold|void)$', 'sale:retail:update'),
            ],
            '/api/sales/retail/shift': [
                ('POST', r'/open$', 'sale:retail-shift:create'),
            ],
            '/api/erp/sale/pre-order': [
                ('POST', r'/batch-order$', 'sale:pre-order:update'),
            ],
            '/api/erp/finance/account-delivery': [
                ('POST', r'/deliver$', 'finance:account-delivery:update'),
            ],
        },
    },
    'purchase': {
        'label': '采购',
        'slot': 15,
        'domain': 'purchase',
        'dirs': ['backend/erp/erp-purchase/src/main/java/cn/aiedge'],
        'resource_overrides': {
            # 采购订单的两个子查询控制器类级路径带子资源段 ⇒ 推导出的资源名
            # (`order-detail-query` / `order-doc-query`) 与主资源 `order` 分家。
            # 它们查的还是订单，归到 purchase:order:*，免得"同一张单三种资源名"。
            'purchase:order-detail-query': {'resource': 'order'},
            'purchase:order-doc-query':    {'resource': 'order'},
        },
        'code_rules': {
            '/api/erp/purchase/contract': [
                # 生效/终止/归档都是改合同状态；历史码里没有这三个动作
                ('POST', r'/\{[^/}]+\}/(activate|terminate|archive)$', 'purchase:contract:update'),
            ],
            '/api/erp/purchase/inbound': [
                # 收货 / 收货明细 / 质检 / 入库确认：都是"改这张入库单"，不是新建
                ('POST', r'/\{[^/}]+\}/items/\{[^/}]+\}/receive$', 'purchase:inbound:update'),
                ('POST', r'/\{[^/}]+\}/receive$',                  'purchase:inbound:update'),
                ('POST', r'/\{[^/}]+\}/items$',                    'purchase:inbound:update'),
                ('POST', r'/\{[^/}]+\}/quality-check$',            'purchase:inbound:update'),
                ('POST', r'/\{[^/}]+\}/warehouse-confirm$',        'purchase:inbound:confirm'),
            ],
            '/api/erp/purchase/quote': [
                ('POST', r'/\{[^/}]+\}/review$',   'purchase:quote:approve'),
                ('POST', r'/\{[^/}]+\}/(accept|withdraw)$', 'purchase:quote:update'),
            ],
            '/api/erp/purchase/inquiry': [
                ('POST', r'/\{[^/}]+\}/send$',     'purchase:inquiry:send'),
            ],
            '/api/erp/purchase/return': [
                ('POST', r'/\{[^/}]+\}/items$',    'purchase:return:update'),
            ],
            '/api/erp/purchase/order': [
                # `POST /order/received` 是"查已收货订单"（POST 传查询条件），不是新建
                ('POST', r'/received$',            'purchase:order:list'),
            ],
            '/api/erp/purchase/sales-driven': [
                ('POST', r'/\{[^/}]+\}/purchase-(finished|material)$', 'purchase:sales-driven:update'),
            ],
            '/api/purchase/doc-query': [
                ('POST', r'/no-settle-write-off$', 'purchase:doc-query:update'),
            ],
        },
    },

    # ══════════════════════════════════════════════════════════════════════════
    # ── 2026-09-21 E-01 erpprinting / erppricing / erpobserv / erpbatchsn 批次 ──
    #
    # 【共同背景】这四个模块在 `sys_permission` 里**几乎零码**（打印域仅 1 条历史码
    #   `print:draft`，且不对应任何现存端点；定价域 3 条 `pricing:approval:*` 是前端
    #   字符串、库中并不存在）。E-01 要给它们的裸控制器补 `@SaCheckPermission`，
    #   而**没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
    #   故按 E-04 口径**现场建码**，每个模块一个迁移：V11.465.0 / V11.466.0 /
    #   V11.467.0 / V11.468.0，槽位 16 / 17 / 18 / 19（id 号段已实测空闲）。
    #
    # 【域口径】一律按**类级路由路径**推导，不按 Maven 模块目录 —— 本仓两者不对应
    #   （先例：sales 模块里的 `/api/erp/finance/account-delivery` 归 finance 域，
    #   文件头已有说明）。只有两种情形才用 overrides 显式指定：
    #     ① 类级路径**没有资源段**（否则生成器会拿方法路径首段当资源，产出
    #        `metrics:dashboard:*`、`print:health:*` 这类碎片码）；
    #     ② 路径推导出的域/资源**与既有命名冲突或不一致**（如 `/api/erp/product-grade-price`
    #        被拆成 `product:grade-price`，与兄弟控制器 `ProductGradeController`
    #        的历史码 `product:grade:*` 同族，必须保留）。
    'erpprinting': {
        'label': '打印',
        'slot': 16,
        'domain': 'print',
        # 模块根：同时覆盖 `erp.printing.*`（v1 `/api/v1/print/*` 与 v2 `/api/v2/print/*`）
        # 与 `erp.signature.*`（`/api/signature`、`/api/rating`）—— 后者属同一 Maven 模块。
        'dirs': ['backend/erp/erp-printing/src/main/java/cn/aiedge'],
        # ── 安全阀：以下**整类**不加权限注解 ──
        'skip_bases': [
            # Windows 打印客户端**直连**的设备端 API：类注释写明「由 Windows 打印客户端
            # 直接调用，使用 auth_key 认证」，方法体里逐调用 `clientService.authenticate(
            # clientId, authKey)`，**根本没有用户会话**（不带 Authorization 头）。
            # 给它加用户权限码 ⇒ 客户端永远打不通（与 wms 的 ErpCallbackController 同类）。
            # 正确修法是服务间鉴权（authKey 已有），不是权限码。
            '/api/v2/print/client',
            # 打印客户端的**登录/注册/自省/登出**（Electron 端 `frontend/apps/print-client/
            # src/main/services/auth.ts` 调用）：login 是登录端点（未登录必须可达），
            # register/me/check/logout 是客户端会话自助（任何登录用户都该能用，同 `/api/profile` 口径）。
            # ⚠️ 既有缺陷（只报告、本批不修）：`/api/v2/print/client/auth/login` **不在**
            # SaTokenConfig 两处白名单里 ⇒ 该客户端今天实际登不进来（未登录被
            # StpUtil.checkLogin 拦下）。这里若再补权限码只会把"登不进来"变成"更登不进来"。
            '/api/v2/print/client/auth',
        ],
        # 资源名单复数/碎片纠正 + 域归位（keys 是**推导出的**「域:资源」）
        'resource_overrides': {
            # 打印域历史只有 1 条 `print:draft`，没有复数先例；统一取单数（同 stock/wms 的做法）
            'print:logs':        {'resource': 'log'},
            'print:tasks':       {'resource': 'task'},
            'print:templates':   {'resource': 'template'},
            'print:printers':    {'resource': 'printer'},
            'print:chains':      {'resource': 'chain'},
            'print:clients':     {'resource': 'client'},
            'print:screenshots': {'resource': 'screenshot'},
            'print:format':      {'resource': 'format'},
            'print:messages':    {'resource': 'message'},
        },
        # `/api/signature`、`/api/rating` 是**类级路径没有资源段**的两个控制器：
        # 不指定资源名会退化成 `signature:photo:*`、`rating:list`（方法路径首段当资源）。
        # 域统一 `signature`（签收域，库中无码；表名 erp_signature_record / erp_delivery_rating）。
        'base_overrides': {
            '/api/signature': {'domain': 'signature', 'resource': 'record'},
            '/api/rating':    {'domain': 'signature', 'resource': 'rating'},
        },
        'code_rules': {
            '/api/v1/print/logs': [
                # 裸 GET 是分页列表（RULES 的 GET 兜底会给 `view`）
                ('GET', r'$', 'print:log:list'),
            ],
            '/api/v1/print/tasks': [
                ('GET', r'$',        'print:task:list'),
                ('GET', r'/queue$',  'print:task:list'),
                ('GET', r'/history$', 'print:task:list'),
                # 批量打印 = 执行打印（不是"新建任务"）
                ('POST', r'/batch$', 'print:task:print'),
            ],
            '/api/v1/print/templates': [
                ('GET',  r'$',                        'print:template:list'),
                # `GET /type/{templateType}` 会被 RULES 判成 detail（路径以 `{...}` 结尾），
                # 语义是按类型取**模板列表** ⇒ list
                ('GET',  r'/type/\{[^/}]+\}$',        'print:template:list'),
                # 复制 = 复制（不动原模板）；预览是渲染读取，归 view
                ('POST', r'/\{[^/}]+\}/copy$',        'print:template:copy'),
                ('POST', r'/\{[^/}]+\}/preview$',     'print:template:view'),
            ],
            # v2 与 v1 是同一业务对象的**两代实现**（同 `/api/import` 与 `/api/import/v2`
            # 共用 `system:import:*` 的先例）⇒ 共用 `print:template:*` / `print:task:*`
            '/api/v2/print/templates': [
                ('GET',  r'$',                 'print:template:list'),
                ('POST', r'/\{[^/}]+\}/copy$', 'print:template:copy'),
            ],
            '/api/v2/print/chains': [
                ('GET', r'$',                     'print:chain:list'),
                ('GET', r'/by-page/\{[^/}]+\}$',  'print:chain:list'),
            ],
            '/api/v2/print/clients': [
                ('GET',  r'$',                        'print:client:list'),
                # 重置认证密钥 = 改这个客户端（不是新建），且它是敏感动作 ⇒ 归 update
                ('POST', r'/\{[^/}]+\}/reset-key$',   'print:client:update'),
            ],
            '/api/v2/print/tasks': [
                ('GET',  r'$',                                  'print:task:list'),
                ('GET',  r'/queue$',                            'print:task:list'),
                # 按链路执行打印 = 执行（POST 兜底会误判成"新建任务"）
                ('POST', r'/by-chain$',                         'print:task:execute'),
                ('POST', r'/\{[^/}]+\}/confirm-screenshot$',    'print:task:confirm'),
            ],
            '/api/v2/print/screenshots': [
                ('GET', r'$', 'print:screenshot:list'),
            ],
            '/api/v2/print/format': [
                # 三个 POST 都是**纯计算/校验**，不落库：校验公式 → check，渲染/格式化 → view
                ('POST', r'/validate-formula$',   'print:format:check'),
                ('POST', r'/(render|field)$',     'print:format:view'),
            ],
            '/api/v2/print/messages': [
                ('POST', r'/send$', 'print:message:send'),
            ],
            '/api/v1/print/printers': [
                ('GET', r'$', 'print:printer:list'),
                # 打印机分组是**独立对象**（组本身有增删改查），不并进 printer ——
                # 否则"删打印机"与"删分组"共用一个码，粒度错位。
                ('POST',   r'/groups$',                        'print:printer-group:create'),
                ('PUT',    r'/groups/\{[^/}]+\}$',             'print:printer-group:update'),
                ('DELETE', r'/groups/\{[^/}]+\}$',             'print:printer-group:delete'),
                ('GET',    r'/groups$',                        'print:printer-group:list'),
                ('POST',   r'/groups/\{[^/}]+\}/assign$',      'print:printer-group:assign'),
                ('GET',    r'/groups/\{[^/}]+\}/printers$',    'print:printer-group:detail'),
            ],
            '/api/signature': [
                ('GET', r'/list$',                          'signature:record:list'),
                ('GET', r'/delivery-person/\{[^/}]+\}$',    'signature:record:list'),
                # 完整性校验（返回 IntegrityVerifyResult）⇒ check
                ('GET', r'/\{[^/}]+\}/verify$',             'signature:record:check'),
            ],
            '/api/rating': [
                ('GET', r'/list$',                       'signature:rating:list'),
                ('GET', r'/signature/\{[^/}]+\}$',       'signature:rating:detail'),
                # 统计返回的是聚合数字，不是某条记录 ⇒ view
                ('GET', r'/stats/\{[^/}]+\}$',           'signature:rating:view'),
            ],
        },
    },
    'erppricing': {
        'label': '定价',
        'slot': 17,
        'domain': 'pricing',
        'dirs': ['backend/erp/erp-pricing/src/main/java/cn/aiedge'],
        # ── 类级路径没有资源段的三个控制器（`/api/v1/price-engine` 家族、策略配置）──
        # 不指定资源名会退化成 `price-engine:calculate:*`、`pricing:health:*` 这类碎片码，
        # 且首段 `price-engine` / `price-strategy` 会被推成**新的一级域**（与 `pricing` 分家）。
        'base_overrides': {
            '/api/v1/price-engine':          {'domain': 'pricing', 'resource': 'engine'},
            '/api/v1/price-engine/tiers':    {'domain': 'pricing', 'resource': 'tier'},
            '/api/v1/price-strategy/config': {'domain': 'pricing', 'resource': 'strategy'},
            # `/api/erp/pricing` 是"取价配置 + 价格解析 + 价格记忆"三合一的控制器
            '/api/erp/pricing':              {'domain': 'pricing', 'resource': 'price'},
            # ⚠️ `/api/erp/pricing/approval` **不需要** override：路径推导本身就是
            #    `pricing:approval`，与前端既有字符串 `pricing:approval:{create,approve,reject}`
            #    （views/erp/pricing/approval/index.vue:134,210,218）逐字一致。
        },
        'code_rules': {
            # ⚠️ 这里一律写**完整码**，不用 `@动作词` 简写：`@动作词` 只换动作词、
            #    域与资源仍走「路径推导」（见 code_of 的注释），会**绕过 base_overrides**
            #    ⇒ 产出 `price-engine:calculate:view` 这种一级域碎片码。踩过，记此备注。
            #
            # 价格引擎是**纯计算 API**（不落库）：所有 POST 都是"用引擎算价"⇒ view；
            # 只有刷缓存是执行。
            '/api/v1/price-engine': [
                ('POST', r'/cache/refresh$', 'pricing:engine:execute'),
                ('POST', r'.+',              'pricing:engine:view'),
                ('GET',  r'/history/\{[^/}]+\}$', 'pricing:engine:list'),
                ('GET',  r'.+',              'pricing:engine:view'),
            ],
            # 策略配置：解析/格式化是读，校验类归 check。这些 POST 的语义与 RULES 的
            # `create` 兜底完全不符（"能新建的人才能校验配置"说不通）。
            '/api/v1/price-strategy/config': [
                ('POST', r'/parse/(json|xml|yaml|batch)$', 'pricing:strategy:view'),
                ('POST', r'/(validate|validate/batch|validate/business-rules|check/completeness|check/consistency)$',
                         'pricing:strategy:check'),
                ('POST', r'/format$',        'pricing:strategy:view'),
                ('GET',  r'.+',              'pricing:strategy:view'),
            ],
            '/api/v1/price-engine/tiers': [
                # 计算层级价/批量算价 = 读（RULES 的 POST 兜底会判成"新建价层"）
                ('POST', r'/calculate(/batch)?$', 'pricing:tier:view'),
                ('GET',  r'/tiers$',              'pricing:tier:list'),
                ('GET',  r'/product/\{[^/}]+\}$', 'pricing:tier:list'),
            ],
            '/api/erp/pricing/approval': [
                # 待审批列表/我的申请都是**列表**（RULES 会因路径以 `{...}` 结尾判成 detail）
                ('GET', r'/pending$',              'pricing:approval:list'),
                ('GET', r'/my/\{[^/}]+\}$',        'pricing:approval:list'),
                ('POST', r'/apply$',               'pricing:approval:create'),
                ('PUT',  r'/\{[^/}]+\}/approve$',  'pricing:approval:approve'),
                ('PUT',  r'/\{[^/}]+\}/reject$',   'pricing:approval:reject'),
            ],
            # 等级价：批量保存是**全量覆盖改**（不是新建），按产品取列表归 list
            '/api/erp/product-grade-price': [
                ('GET',  r'/by-product/\{[^/}]+\}$', 'product:grade-price:list'),
                ('POST', r'/batch-save$',            'product:grade-price:update'),
            ],
        },
    },
    'erpobserv': {
        'label': '可观测性',
        'slot': 18,
        'domain': 'metrics',
        'dirs': ['backend/erp/erp-observability/src/main/java/cn/aiedge'],
        # 两个控制器的类级路径**都只有容器段**（`/api/erp/metrics`、`/api/erp/monitor`）：
        # 不指定资源名的话，资源会取方法路径第一段 ⇒ `metrics:dashboard:*`、`monitor:realtime:*`、
        # `monitor:core:*` 这类碎片（core/order/user/sales 会被当成"资源名"）。
        'base_overrides': {
            '/api/erp/metrics': {'domain': 'metrics', 'resource': 'metric'},
            '/api/erp/monitor': {'domain': 'monitor', 'resource': 'business-metric'},
        },
        'code_rules': {
            # ⚠️ 同 erppricing：这里一律写完整码 —— `@动作词` 简写会绕过 base_overrides，
            #    产出 `metrics:dashboard:view` / `monitor:list` 这类碎片码。
            '/api/erp/metrics': [
                # 三个 POST 里两个是**读**：批量取当前值、按条件查历史（POST 传查询体）
                ('POST', r'/(current/batch|history)$', 'metrics:metric:list'),
                # 刷新指标缓存 = 执行（POST 兜底会判成"新建指标"）
                ('POST', r'/refresh$',                 'metrics:metric:execute'),
                ('GET',  r'/type/\{[^/}]+\}$',         'metrics:metric:list'),
                ('GET',  r'/history$',                 'metrics:metric:list'),
            ],
            '/api/erp/monitor': [
                # `/query` 是**分页查询**（POST 传查询条件），不是新建
                ('POST', r'/query$',                   'monitor:business-metric:list'),
                ('POST', r'/refresh$',                 'monitor:business-metric:execute'),
                # 趋势返回的是时序数组 ⇒ view（RULES 会因路径以 `{...}` 结尾判成 detail）
                ('GET',  r'/trend/\{[^/}]+\}$',        'monitor:business-metric:view'),
                ('GET',  r'/definitions$',             'monitor:business-metric:list'),
                ('GET',  r'/history$',                 'monitor:business-metric:list'),
                # `POST /definitions`（保存指标定义，upsert）保持 RULES 的 create 口径
                # —— 与 dms 批次「保存 = 新建口径」一致（MODULES['dms'] code_rules 首条）。
            ],
        },
    },
    'erpbatchsn': {
        'label': '批次与序列号',
        # 权限名直接用 `ERP` 前缀（生成「ERP批次入库」），不再拼模块名 ——
        # 否则会得到「批次与序列号批次入库」这种重复；码前缀本来就是 `erp:`。
        'name_prefix': 'ERP',
        'slot': 19,
        # 【域为什么不是 stock / batch-sn】见下方 resource_overrides 的注释
        'domain': 'erp',
        # ⚠️ 目录虽在 erp-stock 模块里，但业务是**批次号/序列号**（表 batch_number /
        #    serial_number / batch_rule / batch_flow_record…），与库存账不是一回事。
        'dirs': ['backend/erp/erp-stock/src/main/java/cn/aiedge/erp/batchsn'],
        'resource_overrides': {
            # 类级路径是 `/api/erp/batch-sn/<复数资源>`：路径推导会得到域 `batch-sn`、
            # 资源 `batches`/`serials`。两条都不取：
            #   · 不用 `stock:` —— 该前缀已被 erp-stock 的库存控制器占用（库中 105 条
            #     `stock:*` 码的 api_path 全是 `/api/erp/stock/*`、`/api/erp/product*`），
            #     批次/序列号是**跨模块的批号主数据**（库存、质检、售后、追溯都用），
            #     并入 stock 域会让"批次"这个对象在权限矩阵里挂在库存模块下，语义错位；
            #   · 不用新建一级域 `batch-sn:` —— 前端 pc-admin 的批次/序列号页**早已写着**
            #     `erp:batch:*` / `erp:serial:*`（views/erp/batch/index.vue:160,177,394,402,
            #     410,418,426 与 views/erp/serial/index.vue:51,287），且 `erp:` 域在库中
            #     已存在（`erp:expense:*` 那套四段式历史码）⇒ 沿用 `erp:` + 单数资源
            #     `batch`/`serial`，前端 v-permission 零改动即可对上。
            'batch-sn:batches': {'domain': 'erp', 'resource': 'batch'},
            'batch-sn:serials': {'domain': 'erp', 'resource': 'serial'},
        },
        'code_rules': {
            # ⚠️ 一律写完整码：本模块靠 `resource_overrides` 把域归到 `erp`，
            #    而 `@动作词` 简写会绕过 overrides（按路径推导出 `batch-sn:batches:xxx`）。
            '/api/erp/batch-sn/batches': [
                # 裸 GET 是条件列表查询（无路径变量）
                ('GET',   r'$',                     'erp:batch:list'),
                # 入库/出库/质检是**状态推进**（RULES 的 POST 兜底会判成"新建批次"），
                # 且三者是三个独立职责，动作词取前端既有字符串的写法
                # （`erp:batch:inbound` / `outbound` / `inspect`，见 views/erp/batch/index.vue:394,402,410）。
                ('POST',  r'/inbound$',             'erp:batch:inbound'),
                ('POST',  r'/outbound$',            'erp:batch:outbound'),
                ('POST',  r'/\{[^/}]+\}/quality-inspection$', 'erp:batch:inspect'),
                # 改状态：复用库中已有动作词 `update-status`（system:menu:update-status 等 6 条）
                ('PATCH', r'/status$',              'erp:batch:update-status'),
                # 批次转移（改仓库/库位归属）= 改批次，复用库中已有动作词 transfer
                ('POST',  r'/transfer$',            'erp:batch:transfer'),
                # 批次盘点：调整实物数量 = 改批次
                ('POST',  r'/inventory$',           'erp:batch:update'),
                # 高级搜索（POST 传查询体）= 读
                ('POST',  r'/search$',              'erp:batch:list'),
                # 校验批号（返回 Boolean）⇒ check
                ('GET',   r'/validate-batch-no$',   'erp:batch:check'),
                # 清缓存不是"删批次"（DELETE 兜底会判成 delete）⇒ 复用库中已有动作词 clear
                ('DELETE', r'/clear-cache$',        'erp:batch:clear'),
            ],
            '/api/erp/batch-sn/serials': [
                ('GET',   r'$',                     'erp:serial:list'),
                ('POST',  r'/inbound$',             'erp:serial:inbound'),
                ('POST',  r'/outbound$',            'erp:serial:outbound'),
                ('PATCH', r'/\{[^/}]+\}/status$',   'erp:serial:update-status'),
                ('GET',   r'/validate-serial-no$',  'erp:serial:check'),
            ],
        },
    },

    # ══════════════════════════════════════════════════════════════════════════
    # ── 2026-09-21 E-01 「core 底座」批次：corebase / coreplatform /
    #    corenotify / corepayment / coreagent ──
    #
    # 【为什么单独一批】这五个是 `backend/core/*` 下的**平台底座** Maven 模块，不是业务域，
    #   此前的 E-01 批次（业务域）与 core-api（平台本身）都没覆盖它们。实测整类零权限注解的
    #   控制器：corebase 19 / coreplatform 4 / corenotify 4 / corepayment 2 / coreagent 1。
    #   其中相当一部分端点**绝不能加权限码**（登录取 token、无用户会话的机器对机器接口、
    #   前端登录后必调的自省/自助接口）—— 逐条写在各自的 skip_* 里，并在迁移头部汇总。
    #
    # 【域口径】一律**先查库再建码**：本批真正新增的一级前缀只有 4 个
    #   （`quality:` / `trade:` / `notification:` / `automation:`+`custom-field:`+`kanban:`），
    #   其余全部接到既有码族上（`log:` / `set:` / `system:` / `tenant-admin:` / `payment:`），
    #   不造同义码、不造平行命名空间。
    #
    # 【id 号段】槽位 21~25（权限码 121000/122000/…/125000 起，角色关联 9710000/…/9750000 起），
    #   执行前已实测两段均为空（9700000 段被 e2e 夹具占过，故从 9710000 续）。
    'corebase': {
        'label': '平台基础',
        'slot': 21,
        'domain': 'system',
        'dirs': ['backend/core/base/core-base/src/main/java/cn/aiedge'],
        # ── 安全阀：以下**整类**不加权限注解 ──
        'skip_bases': [
            # 认证 + "当前用户"自省。login/captcha/check 在 SaTokenConfig **两处**
            # excludePathPatterns 里（未登录必须可达）；logout 加码 ⇒ "没有该码的用户无法登出"；
            # refresh 是 token 续期；userinfo / tenants / login-history 是当前登录用户的自助接口
            # （pc-admin `api/user.ts:93,112` 取"我的租户"与"我的信息"，登录后必调）。
            '/api/auth',
            # 顶栏通知铃铛 + 通知中心页。`layouts/BasicLayout.vue:532` → `composables/useNotification.ts:53,56`
            # **无条件**轮询 unread-count / list；端点全部按**会话 receiverId** 过滤
            # （`NotificationController:70-72` `eq(SysMessage::getReceiverId, userId)`）
            # ⇒ 属"我的通知"自助接口，加码会让所有非超管顶栏报错。
            '/api/notification',
            # 个人页面配置（列显隐/查询条件/功能按钮），配置键里含**会话 userId**
            # （`UserPageConfigController#buildKey`）⇒ 个人偏好自助（同 core-api 的 /api/profile 口径）。
            '/api/system/user-config',
            # 行政区划省/市/区县三级联动。类注释："全系统公共能力：往来单位（客户/供应商/
            # 物流公司）表单的「所在地区」三级联动统一调用本接口" ⇒ 是**公共下拉数据源**
            # 不是可勾选功能（同 core-api 的 /api/dict 口径）。
            '/api/sys/region',
            # 对外开放 API：外部平台（电商/ERP）的订单回调、库存查询、商品同步。
            # 类注释："用于接收外部平台（电商、ERP等）的订单和库存请求"，调用方是**外部平台服务器**
            # （签名走 `@RequestParam String signature`），没有员工会话 ⇒ 加用户权限码必然打不通
            # （同 wms 的 ErpCallbackController / print 的 client API 口径）。
            # ⚠️ 顺带查实（只报告）：类注释第 4 条声称"安全验证 - 签名校验"，但
            #    `orderCallback` 并没有把 `signature` 传给 `orderService.receiveCallback(channelCode, data)`
            #    ⇒ 该参数被接收后**未被使用**，验签实际未实现。
            '/api/open',
            # SSE 推送通道：`utils/sseClient.ts:34` 用 `new EventSource('/api/sse/notifications?token=…')`
            # 直连（EventSource 不能设 Authorization 头，故 token 走 query）。它推的是
            # **权限缓存失效**事件（前端收到后重载权限），任何登录用户都要用 ⇒ 加码会让
            # 非超管在权限变更后永远刷不到新菜单。
            '/api/sse',
        ],
        # ── 安全阀：以下**单个端点**不加权限注解 ──
        'skip_methods': {
            # 当前会话自省（该类另 8 个会话管理端点早已挂 system:session:*）。
            '/api/session': [('GET', r'/current$')],
            # 当前用户的**菜单树**：前端据此动态注册路由
            # （`router/dynamicRoutes.ts:1132` 调 /menu/user/mega/{clientType}、
            #   `api/menu.ts:130,151` 调 /menu/user/tree）。
            # 这是"先有菜单才能点页面"的自省接口，加码 ⇒ 非超管登录后界面空白。
            # （`/menu/client/{clientType}`、`/menu/check-code` **不是**自省接口，仍补码。）
            '/api/menu': [
                ('GET', r'/user/tree$'),
                ('GET', r'/user/client/\{[^/}]+\}$'),
                ('GET', r'/user/mega/\{[^/}]+\}$'),
            ],
            # 角色下拉源：`views/system/user/index.vue:610,1218,1247,1306` 用它把 roleId 映射成角色名
            # （用户管理页"所属角色"列与编辑表单）⇒ **跨页数据源**，不是可勾选功能
            # （同 core-api 的 /department/list 口径）。角色管理页自身用的是 /role/page，不受影响。
            '/api/role': [('GET', r'/list$')],
            # 登录/登出（在两处白名单里；登出不能要求权限码）+ 改密自助
            # （方法体自己写着"只能改**自己**的密码"，`SecurityUtils.getCurrentUserId()`）。
            '/api/user': [
                ('POST', r'/(login|logout)$'),
                ('PUT',  r'/\{[^/}]+\}/password/change$'),
            ],
            # 校验"**当前用户**"对单据类型的访问级别（`RoleBillTypeService#validateBillTypeAccess`）
            # —— 自省接口：要校验自己的权限却先要求另一个权限码，是死循环。
            '/api/role-bill-type': [('GET', r'/validate$')],
            # 前端**全局错误上报**：`main.ts:59` 与 `utils/errorReporter.ts:29` 把 endpoint 指向
            # `/api/error-report`，任何页面报错都会自动 POST。加码 ⇒ 非超管的错误上报全部 403
            # （噪音 + 上报链断），且它本就不是"可勾选的功能"。（GET recent/query/statistics 仍补码。）
            '/api/error-report': [('POST', r'$')],
        },
        # ── 类级路径没有资源段 / 与既有码不同域时的显式指定 ──
        'base_overrides': {
            # 这四个控制器的**同类端点早已挂** system:menu:* / tenant-admin:permission:* /
            # tenant-admin:role:* / tenant-admin:user:*。裸端点的域必须与它们逐字一致，
            # 否则会按路径推导成 `menu:` / `permission:` / `role:` / `user:` 一级域 ⇒ 同域两套码。
            '/api/menu':       {'domain': 'system',       'resource': 'menu'},
            '/api/permission': {'domain': 'tenant-admin', 'resource': 'permission'},
            '/api/role':       {'domain': 'tenant-admin', 'resource': 'role'},
            '/api/user':       {'domain': 'tenant-admin', 'resource': 'user'},
            # `/api/logs/system/*`（SystemLogController）与 `/api/system/log/oper/*`
            # （LogManageController，13 处已挂 `log:oper:*`）是**同一功能**（系统日志的
            # 分页/详情/统计/导出/清理）的并行实现，只是换了一张表
            # （`sys_system_log`，实测 0 行）⇒ 复用 `log:oper:*` 码族，
            # **不**另立 `log:system:*` 平行命名空间。
            '/api/logs': {'domain': 'log', 'resource': 'oper'},
            # 高级日志查询直接查 `SysOperLog`（`import cn.aiedge.base.entity.SysOperLog`）
            # ⇒ 就是操作日志的查询/统计/导出，同样并入 `log:oper:*`。
            '/api/system/log/advanced': {'domain': 'log', 'resource': 'oper'},
            # 结构化日志读的是 `logs/structured/app.jsonl` **文件**（运维排障），与 DB 日志
            # 不是同一份数据 ⇒ 在 `log:` 域下新开资源 `structured`
            # （同族先例：log:audit / log:login / log:oper 各自一个资源）。
            '/api/system/log/structured': {'domain': 'log', 'resource': 'structured'},
            # 错误报告是"错误日志"⇒ 域归 `log`，资源 `error-report`。
            '/api/error-report': {'domain': 'log', 'resource': 'error-report'},
        },
        # ── 动作词修正（POST/PUT/DELETE 的通用兜底会判错语义）──
        'code_rules': {
            # ── 系统日志（/api/logs/system/*）→ 既有 log:oper:*（5 个码全部命中，零新建）──
            '/api/logs': [
                # "某用户的最近日志"是**列表**（RULES 会因路径以 `{userId}` 结尾判成 detail）
                ('GET', r'/system/recent/\{[^/}]+\}$', 'log:oper:list'),
                ('GET', r'/system/stats/\w+$',         'log:oper:stats'),
            ],
            # ── 高级日志查询 → 既有 log:oper:* ──
            # ⚠️ 顺序敏感：export / stats 必须排在 `.+` 兜底之前（code_rules 是首个匹配生效）。
            '/api/system/log/advanced': [
                ('GET',  r'/export/(excel|csv|json)$',  'log:oper:export'),
                ('GET',  r'/(summary|trend|anomaly)$',  'log:oper:stats'),
                ('GET',  r'.+',                         'log:oper:list'),
                # 高级查询是 POST 传查询体（不是"新建日志"）
                ('POST', r'/query$',                    'log:oper:list'),
            ],
            '/api/system/log/structured': [
                ('GET', r'/stats$',        'log:structured:stats'),
                ('GET', r'.+',             'log:structured:list'),
            ],
            '/api/error-report': [
                ('GET', r'/statistics$',   'log:error-report:stats'),
                ('GET', r'.+',             'log:error-report:list'),
            ],
            # 编码唯一性校验（菜单/权限管理页新建或修改时调）⇒ 动作词 check
            # （库中已有 12 条 `:check`）。`system:menu:check` 为本批新建；
            # `tenant-admin:permission:check` 库中**已存在**（"权限校验"），复用不新建。
            '/api/menu': [
                ('GET', r'/check-code$',            'system:menu:check'),
                # 按客户端类型取菜单树：RULES 会因路径以 `{clientType}` 结尾判成 detail，
                # 语义是"取菜单列表" ⇒ list（复用既有码）。
                ('GET', r'/client/\{[^/}]+\}$',     'system:menu:list'),
            ],
            '/api/permission': [
                ('GET', r'/check-code$',            'tenant-admin:permission:check'),
            ],
            '/api/role': [
                # 角色管理页的"已授权限/菜单"回显（`api/role.ts:74,79`）复用既有角色详情码。
                ('GET', r'/\{[^/}]+\}/(permissions|menus)$', 'tenant-admin:role:detail'),
            ],
            # ── 质检（quality 域，新建码）──
            # "待处理"与"历史"都是**列表**；"处理缺陷"是改单不是新建。
            '/api/quality/defect': [
                ('POST', r'/\{[^/}]+\}/handle$',    'quality:defect:update'),
                ('GET',  r'/pending$',              'quality:defect:list'),
                ('GET',  r'/\{[^/}]+\}/history$',   'quality:defect:list'),
            ],
            '/api/quality/inspection': [
                ('GET',  r'/pending$',              'quality:inspection:list'),
            ],
            # ── 外部渠道（trade 域，新建码）──
            # 这些 POST 是**状态推进/动作**，RULES 的 create 兜底会把它们判成"新建渠道"。
            '/api/trade/channel': [
                ('POST', r'/\{[^/}]+\}/toggle$',            'trade:channel:update'),
                ('POST', r'/\{[^/}]+\}/(initialize|sync)$', 'trade:channel:execute'),
                # 启用中的渠道列表（前端"渠道选择"下拉）
                ('GET',  r'/enabled$',                      'trade:channel:list'),
                # 渠道密钥属于该渠道的敏感详情（不是泛泛的"查看"）
                ('GET',  r'/\{[^/}]+\}/secret$',            'trade:channel:detail'),
            ],
            '/api/trade/api-monitor': [
                ('POST', r'/sandbox/invoke$',         'trade:api-monitor:execute'),
                # 清理过期调用记录（不是"新建监控"）⇒ 复用库中已有动作词 clear
                ('POST', r'/clean-expired$',          'trade:api-monitor:clear'),
                ('POST', r'/sync/\{[^/}]+\}/retry$',  'trade:api-monitor:retry'),
                # 两端点返回的都是**列表**（RULES 兜底会判成 view）
                ('GET',  r'/calls/endpoints$',        'trade:api-monitor:list'),
                ('GET',  r'/alerts$',                 'trade:api-monitor:list'),
            ],
        },
    },
    'coreplatform': {
        'label': '平台扩展',
        'slot': 22,
        'domain': 'system',
        'dirs': ['backend/core/platform/core-platform/src/main/java/cn/aiedge'],
        # ── 安全阀：自动化规则的"模型事件入口"整组排除 ──
        # `/trigger/create|write|delete|scheduled` 的方法体是
        # `ruleService.executeOnCreate(modelName, recordId, values)` 这类 **ORM 事件钩子**
        # （Odoo 风格），设计上由业务层/调度器触发，不是"用户点出来的功能"；全仓 grep
        # `trigger/create` / `executeOnCreate` 除本控制器外**没有任何调用方**。
        # 给它挂"某个角色的权限码"没有语义 —— 谁该拥有"记录被创建"的权限？
        'skip_methods': {
            '/api/automation': [('POST', r'/trigger/\w+$')],
        },
        # 四个控制器的类级路径都**没有资源段** ⇒ 不指定的话生成器会拿方法路径首段当资源名
        # （`custom-field:model:*`、`kanban:data:*`、`kanban:sync:*`、`automation:trigger:*`
        # 这类碎片码），且 `custom-field` / `kanban` / `automation` 会被推成一级域后又在
        # 资源上二次分裂。这里显式给出「域 + 资源」。
        'base_overrides': {
            '/api/automation':         {'domain': 'automation',   'resource': 'rule'},
            '/api/custom-field':       {'domain': 'custom-field', 'resource': 'field'},
            '/api/custom-field-value': {'domain': 'custom-field', 'resource': 'value'},
            '/api/kanban':             {'domain': 'kanban',       'resource': 'board'},
        },
        'code_rules': {
            # ⚠️ 本模块四个 base **都配了 base_overrides** ⇒ code_rules 一律写**完整码**：
            #    `@动作词` 简写只替换动作词、域与资源仍走路径推导，会**绕过 overrides**
            #    产出 `custom-field:model:view` 这类碎片码（erppricing 批次踩过，记此备注）。
            '/api/automation': [
                # 启用/停用是改规则状态（RULES 的 create 兜底会判成"新建规则"）
                ('POST', r'/\{[^/}]+\}/(activate|deactivate)$',   'automation:rule:update'),
                # 按模型/触发类型取规则：**列表**（路径以 `{...}` 结尾会被 RULES 判成 detail）
                ('GET',  r'/model/\{[^/}]+\}$',                   'automation:rule:list'),
                ('GET',  r'/model/\{[^/}]+\}/trigger/\{[^/}]+\}$', 'automation:rule:list'),
            ],
            '/api/custom-field': [
                ('POST',   r'/\{[^/}]+\}/(activate|deactivate)$',   'custom-field:field:update'),
                # 按模型/分组取字段：都是**列表**
                ('GET',    r'/model/\{[^/}]+\}$',                   'custom-field:field:list'),
                ('GET',    r'/model/\{[^/}]+\}/searchable$',        'custom-field:field:list'),
                ('GET',    r'/model/\{[^/}]+\}/group/\{[^/}]+\}$',  'custom-field:field:list'),
                # 字段定义校验（返回校验结果，不落库）⇒ check
                ('GET',    r'/validate$',                           'custom-field:field:check'),
                # 字段分组是**独立对象**（自己有增删查），不并进 field
                ('POST',   r'/group$',                              'custom-field:group:create'),
                ('DELETE', r'/group/\{[^/}]+\}$',                   'custom-field:group:delete'),
                ('GET',    r'/group/model/\{[^/}]+\}$',             'custom-field:group:list'),
            ],
            # 看板：**列**与**卡片**都是独立对象（各自有增删改查），合并进 board 会让
            # "删卡片"与"删看板列"共用一个开关，粒度错位 ⇒ 拆成 board / column / card。
            '/api/kanban': [
                # 看板数据（按模型聚合的记录）与"从模型同步"= 读 / 执行
                ('GET',    r'/\{[^/}]+\}/data$',                    'kanban:board:list'),
                ('POST',   r'/\{[^/}]+\}/sync$',                    'kanban:board:execute'),
                ('POST',   r'/column$',                             'kanban:column:create'),
                ('PUT',    r'/column/\{[^/}]+\}$',                  'kanban:column:update'),
                ('DELETE', r'/column/\{[^/}]+\}$',                  'kanban:column:delete'),
                ('GET',    r'/\{[^/}]+\}/columns$',                 'kanban:column:list'),
                ('POST',   r'/card$',                               'kanban:card:create'),
                ('PUT',    r'/card/\{[^/}]+\}$',                    'kanban:card:update'),
                ('DELETE', r'/card/\{[^/}]+\}$',                    'kanban:card:delete'),
                # 拖拽移动卡片 = 改卡片（不是新建）
                ('POST',   r'/card/move$',                          'kanban:card:update'),
                ('GET',    r'/card/list$',                          'kanban:card:list'),
                ('GET',    r'/column/\{[^/}]+\}/cards$',            'kanban:card:list'),
                ('GET',    r'/\{[^/}]+\}/record/\{[^/}]+\}/card$',  'kanban:card:detail'),
            ],
        },
    },
    'corenotify': {
        'label': '通知服务',
        'slot': 23,
        'domain': 'notification',
        'dirs': ['backend/core/notification/core-notification/src/main/java/cn/aiedge'],
        # ── 安全阀：`/api/core/notification/**` 的"我的通知"侧 ──
        # 这是 core-base `/api/notification` 的**并行实现**，且 userId 取自
        # `@RequestHeader X-User-Id` 而**不是会话** ⇒ ① 属自助接口（同 /api/notification 口径）；
        # ② 加注解也保护不了它（调用方可以自己伪造 X-User-Id 读别人的通知 —— 这是**既有越权缺陷**，
        # 本批只报告、不修业务代码）。管理侧（export / send / send-template / templates）仍补码。
        'skip_methods': {
            '/api/core/notification': [
                ('GET',    r'/(unread|list|unread-count)$'),
                ('POST',   r'/(\{[^/}]+\}/read|read-all)$'),
                ('DELETE', r'/(\{[^/}]+\}|batch|read)$'),
            ],
        },
        # 四个控制器**共用一个 `notification:` 域**（同一 Maven 模块、同一功能域）。
        # 不指定的话路径推导会产出三个一级域碎片（`notification-templates:` /
        # `notification-stats:` / `webhook:`），`/api/core/notification` 更会退化成
        # `core:notification:*`（`core` 是路径首段）。
        'base_overrides': {
            '/api/core/notification':      {'domain': 'notification', 'resource': 'message'},
            '/api/notification-templates': {'domain': 'notification', 'resource': 'template'},
            '/api/notification-stats':     {'domain': 'notification', 'resource': 'stats'},
            # webhook 与通知模板同住 core-notification（事件推送=通知的一种通道），
            # 归同一域便于模块门与权限矩阵管理。
            '/api/webhook':                {'domain': 'notification', 'resource': 'webhook'},
        },
        'code_rules': {
            # ⚠️ 同上：本模块 base 全配了 overrides ⇒ 一律写完整码。
            '/api/core/notification': [
                # 模板的读/写与 NotificationTemplateController 共用同一套码
                ('GET',  r'/templates$',          'notification:template:list'),
                ('POST', r'/templates$',          'notification:template:create'),
                # 发送通知 / 按模板发送：动作词 send（库中已有 5 条 `:send`）
                ('POST', r'/send(-template)?$',   'notification:message:send'),
            ],
            '/api/notification-templates': [
                # 裸 GET 是"取全部模板"（RULES 的 GET 兜底会给 view）⇒ list
                ('GET',  r'$',                          'notification:template:list'),
                # 预览是**渲染读取**，不落库 ⇒ view（同 print:template:view 的先例）
                ('POST', r'/\{[^/}]+\}/preview$',       'notification:template:view'),
                ('POST', r'/code/\{[^/}]+\}/preview$',  'notification:template:view'),
                # 校验模板变量 ⇒ check
                ('POST', r'/\{[^/}]+\}/validate$',      'notification:template:check'),
                # 刷新模板缓存 ⇒ refresh（库中已有 5 条 `:refresh`）
                ('POST', r'/cache/refresh$',            'notification:template:refresh'),
            ],
            '/api/notification-stats': [
                # 重试所有失败记录 ⇒ retry（不是"新建统计"）
                ('POST', r'/retry-failed$',   'notification:stats:retry'),
            ],
            '/api/webhook': [
                # 按模型/事件取钩子：**列表**（路径以 `{...}` 结尾会被 RULES 判成 detail）
                ('GET',  r'/model/\{[^/}]+\}$',                  'notification:webhook:list'),
                ('GET',  r'/model/\{[^/}]+\}/event/\{[^/}]+\}$',  'notification:webhook:list'),
                # 推送日志是**另一个资源**（钩子的执行记录，不是钩子配置本身）
                ('GET',  r'/\{[^/}]+\}/logs$',                   'notification:webhook-log:list'),
                ('GET',  r'/logs/list$',                         'notification:webhook-log:list'),
                ('POST', r'/\{[^/}]+\}/(activate|deactivate)$',  'notification:webhook:update'),
                # 手动触发一次推送 ⇒ execute；重试失败的推送走 RULES 的 retry
                ('POST', r'/trigger$',                           'notification:webhook:execute'),
                # 验签（返回 boolean，不落库）⇒ check
                ('POST', r'/verify-signature$',                  'notification:webhook:check'),
            ],
        },
    },
    'corepayment': {
        'label': '支付',
        'slot': 24,
        'domain': 'payment',
        'dirs': ['backend/core/payment/core-payment/src/main/java/cn/aiedge'],
        # ── 安全阀：渠道侧回调 ──
        # `POST /api/payment/callback/{channel}` 的类注释写明："该端点是**渠道侧服务器**调用的
        # 回调入口，调用方没有 Sa-Token 会话，加权限注解没有意义（它会先被登录拦截器挡下）"
        # —— 与 RefundController 的 `/callback/{channel}` 同类，一并排除。
        'skip_methods': {
            '/api/payment': [('POST', r'/callback/\{[^/}]+\}$')],
            '/api/refund':  [('POST', r'/callback/\{[^/}]+\}$')],
        },
        # `/api/payment` **不需要** override：它已注解的端点用的是 `payment:request:*` /
        # `payment:record:*` / `payment:channel:*`（资源来自方法路径），保持一致。
        # 另两个控制器的类级路径只有容器段或会被推成新的一级域（`refund:` / `reconciliation:`），
        # 必须显式归到 `payment:` 域 —— 它们与 payment 是同一支付网关子系统的三个部分。
        'base_overrides': {
            '/api/refund':         {'domain': 'payment', 'resource': 'refund'},
            '/api/reconciliation': {'domain': 'payment', 'resource': 'reconciliation'},
        },
        'code_rules': {
            # ⚠️ 完整码：两个 base 都配了 overrides。
            '/api/reconciliation': [
                # 处理对账差异 = 改这条对账记录（POST 兜底会判成"新建对账"）
                ('POST', r'/\{[^/}]+\}/handle$',  'payment:reconciliation:update'),
                # 待对账日期是**列表**（不是某条记录的详情）
                ('GET',  r'/pending-dates$',      'payment:reconciliation:list'),
            ],
            # ⚠️ 口径说明：**不复用** `finance:reconciliation:*`（库中已有 5 条，挂在
            #    `/api/erp/finance/reconciliation/*`，是 **ERP 财务对账**）。本控制器是
            #    **第三方支付渠道对账**（对的是支付记录与渠道账单），同名不同物 ⇒ 用
            #    `payment:reconciliation:*`，与 `payment:request/record/channel` 同族。
            # ⚠️ 同样**不复用** `payment:request:*`：退款申请（/api/refund/request）是独立对象，
            #    与支付请求（/api/payment/request）是两次不同的业务动作。
            '/api/refund': [
                # 退款审批：RULES 的 POST activate/approve 规则已命中，此处不必再写；
                # 保留一条"按渠道回调之外"的显式说明占位不必要，故只写会判错的。
                ('POST', r'/request/\{[^/}]+\}/approve$', 'payment:refund:approve'),
            ],
        },
    },
    'coreagent': {
        'label': '系统',
        'slot': 25,
        'domain': 'system',
        'dirs': ['backend/core/agent/core-agent/src/main/java/cn/aiedge'],
        # ── 安全阀：agent 是**设备/服务端**，两个控制器的入口都没有用户会话 ──
        'skip_bases': [
            # JSON-RPC 能力调用入口（`capability.invoke/list/search`、`system.ping`）。
            # 调用方是 agent 客户端：身份来自 `AgentCallContextHolder`（agentId），
            # `user_id`/`tenant_id` 从请求参数 context 里取，**不走 Sa-Token 会话**
            # ⇒ 加用户权限码等于给"机器"要一个员工权限（同 /api/open、print client API 口径）。
            '/api/agent/invoke',
        ],
        'skip_methods': {
            # agent 心跳（`@RequestParam agentCode`，调用方是 agent 客户端）与
            # API Key 自校验（`@RequestParam apiKey`，agent 用它验证自己的凭据）
            # —— 都是机器对机器，无用户会话。
            # ⚠️ 顺带查实（只报告，不修）：这两个端点**不在** SaTokenConfig 白名单里，
            #    而 agent 客户端拿不到员工 token ⇒ 该心跳/验钥链路按代码推断是断的
            #    （与打印客户端 /api/v2/print/client/auth/login 是同一类既有缺陷）。
            #    正确修法是补服务间鉴权（apiKey 已经有），不是塞权限码、更不是塞白名单。
            '/api/agent': [
                ('POST', r'/heartbeat$'),
                ('GET',  r'/validate$'),
            ],
        },
        # `/api/agent` 的 7 个已注解端点用的是 `system:agent:*`
        # （register/update/delete/activate/deactivate/list/detail）⇒ 裸端点必须归同一族，
        # 否则推导会产出 `agent:*` 一级域（split_head('agent') 不在 KNOWN_DOMAINS）。
        'base_overrides': {
            '/api/agent': {'domain': 'system', 'resource': 'agent'},
        },
        'code_rules': {
            # 活跃 agent 列表：RULES 的 GET 兜底会给 view，但库中只有 system:agent:list
            # ⇒ 复用既有码（不新建 system:agent:view）。
            '/api/agent': [
                ('GET', r'/active$', 'system:agent:list'),
            ],
        },
    },

    # ══════════════════════════════════════════════════════════════════════════
    # ── 2026-09-21 E-01 **最后一批**：supplier / deliveryroute / partnerlevel /
    #    mktanalytics / stockextra（+ 上方改造后的 b2b）──
    #
    # 【共同背景】这五个模块此前**未纳入任何 E-01 批次**：前几批扫的是 `erp/*` 的业务域
    #   控制器目录，而这些控制器要么在**子包**里（`stock/controller/initial`、
    #   `marketing/analytics`、`erp/customer`），要么整个模块当时没配（erp-supplier-portal /
    #   erp-delivery-route）。实测它们整类零权限注码：`scan-unguarded-controllers.py`
    #   本轮报 41 个裸控制器，扣掉前几批**有意排除**的（PdaAuthController / ClientApiController
    #   / AgentInvokeController / FileAccessController / ProfileController / DashboardController /
    #   AuthController / …）后，剩下的就是这 5 个模块 + b2b 的 23 个控制器。
    #
    # 【id 号段】本批新模块用槽位 26~30（权限码 126000 / 127000 / 128000 / 129000 / 130000
    #   起，角色关联 9760000 / 9770000 / 9780000 / 9790000 / 9800000 起）。
    #   执行前已实测这两段区间 **count(*) = 0**（脚本：`tools/dbq.py "<SELECT count(*)>"`）。
    #
    # 【新增一级前缀只有两个】`supplier:` 与 `delivery:`（见 V11.483.0 的模块归属映射）；
    #   其余全部落在**既有**前缀上：`mall:`（b2b，库里已有 mall:tag:*）、
    #   `party:`（客户等级，与 party:customer-region:* 同族）、`marketing:`（提成分析）、
    #   `set:`（期初库存，与 set:initial-finance:* 同族）、`stock:`（进销存分析）、
    #   `md:`（线路档案）。**不新造同义码**：能对上历史码的一律用 code_rules 接过去。
    #   ⚠️ 一条硬约束：**不要把任何码归到 `analytics` 模块**（verify-module-mapping.cjs 有
    #      「analytics 映射数必须为 0」的断言，那是记录中的已知缺口）。本批的
    #      「进销存分析」「提成分析」两张页面虽挂在分析菜单（菜单码 `ana:inventory-analysis`
    #      / `ana:commission-center`）下，但码前缀必须落在 `stock:` / `marketing:`
    #      （归属 warehouse / marketing），不能造 `ana:` 前缀。
    #
    # ── 供应商门户（erp-supplier-portal）：**认证模型已核实为员工会话** ──
    # 这个模块名字像"给外部供应商用的门户"，实测**不是**：
    #   · 三个控制器都没有任何 Sa-Token/设备令牌代码路径，`SupplierPortalController`
    #     的写端点语义甚至全是**采购方**动作（"接受报价"、"拒绝报价"注释就写着「采购方」）；
    #   · 唯一的前端调用方是 pc-admin（`views/supplier/inquiry/index.vue:485,517,540`、
    #     `views/supplier/detail.vue:515,554`、`api/supplier.ts`），走的是员工
    #     `Authorization: Bearer`（`utils/request.ts`）——即**与后台同一套 Sa-Token 会话**；
    #   · 供应商档案页的菜单码是 `md:supplier`（资料 → 供应商），询价单/绩效评估页挂在
    #     `purchase:supplier-inquiry` / `purchase:supplier-performance` 菜单下，都是后台页。
    #   ⇒ **不排除**，整模块补码（域 `supplier`，模块归属 master-data）。
    #   （若将来真开"外部供应商登录"，应另立账号体系与网关头，而不是把这一批码撤掉。）
    'supplier': {
        'label': '供应商',
        'slot': 26,
        'domain': 'supplier',
        # 模块根：三个控制器同在 cn.aiedge.erp.supplier.controller
        'dirs': ['backend/erp/erp-supplier-portal/src/main/java/cn/aiedge'],
        # ── 两个门户 base 的域归一 ──
        # `/api/v1/supplier-portal` 按路径推导会得到**新一级域 `supplier-portal`**
        # （连字符段 `supplier` 不在 KNOWN_DOMAINS 里），与 `/api/supplier` 分裂成两个域。
        # 它们同属"供应商"这一个业务对象（等级/绩效/询价/报价/积分都是供应商的属性）
        # ⇒ 统一到 `supplier:`。下面 code_rules 覆盖了这两个 base 的**全部**端点，
        # 这里的 override 只是"万一漏了一条"时的兜底（免得产出垃圾域码）。
        'base_overrides': {
            '/api/supplier-portal':    {'domain': 'supplier'},
            '/api/v1/supplier-portal': {'domain': 'supplier'},
        },
        'code_rules': {
            # ⚠️ 两个门户 base 都配了 overrides ⇒ 这里**一律写完整码**（`@动作词` 简写会被绕过）。
            # ── 供应商档案（SupplierController，base /api/supplier）──
            # 主 CRUD 走两段码 `supplier:<动作>`（同库中 `party:create` / `purchase:manage` 写法）；
            # 子对象（绩效）才是三段码。
            '/api/supplier': [
                # `/page` 与 `/list` 都是 POST 传查询体（不是新建）：RULES 的 POST 兜底会判 create
                ('POST', r'/(page|list)$', 'supplier:list'),
                # 类级路径没有资源段 ⇒ "按编码取供应商"会被生成器拿**方法路径首段**当资源名，
                # 产出 `supplier:code:detail` 这种碎片码；它查的就是供应商本身 ⇒ 归 supplier:detail。
                ('GET',  r'/code/\{[^/}]+\}$', 'supplier:detail'),
                # `/stats` 是 `/statistics` 的**别名**（控制器注释：「与 /statistics 相同，
                # 兼容前端不同拼写」）⇒ 必须同码。`statistics` 因落在 SEG_SKIP 里已判成
                # 两段码 `supplier:view`，而 `stats` 不在词表 ⇒ 不写规则会得到
                # `supplier:stats:view`（同一件事两个码）。
                ('GET',  r'/stats$', 'supplier:view'),
                # 门户账号的激活/禁用/同步、等级与合作状态的更新：**都是改这个供应商**，
                # 不是新建。RULES 的 POST 兜底会把这 5 条全判成 create。
                ('POST', r'/\{[^/}]+\}/(activate-portal|disable-portal|sync-portal)$', 'supplier:update'),
                ('POST', r'/\{[^/}]+\}/(update-level|update-cooperation-status)$',     'supplier:update'),
                # 导出/校验是 POST 且 RULES 里没有对应动作 ⇒ 不写规则会落到 create
                ('POST', r'/export$',   'supplier:export'),
                ('POST', r'/validate$', 'supplier:check'),
                # 绩效：评估是**新建一条评估记录**（同等先例：`marketing:member-level:create`
                # 就挂在 `/api/erp/marketing/member-level/evaluate` 上）。
                ('POST', r'/performance/evaluate$', 'supplier:performance:create'),
                # 绩效历史是**列表**（RULES 的 GET 兜底只认 `list`/`logs`，`history` 会判成 view）
                ('GET',  r'/\{supplierId\}/performance/history$',     'supplier:performance:list'),
                # 综合评分是**聚合数字**不是某条记录 ⇒ view
                ('GET',  r'/\{supplierId\}/comprehensive-score$',     'supplier:performance:view'),
            ],
            # ── 供应商门户（SupplierPortalController，base /api/v1/supplier-portal，23 端点）──
            '/api/v1/supplier-portal': [
                ('GET',    r'/levels$',                     'supplier:level:list'),
                # 按分数反查等级（返回单个等级实体）⇒ detail（RULES 对以 `{...}` 结尾的 GET 也是 detail）
                ('GET',    r'/levels/score/\{[^/}]+\}$',    'supplier:level:view'),
                ('POST',   r'/levels$',                     'supplier:level:create'),
                ('PUT',    r'/levels/\{[^/}]+\}$',          'supplier:level:update'),
                ('DELETE', r'/levels/\{[^/}]+\}$',          'supplier:level:delete'),
                ('POST',   r'/performances$',               'supplier:performance:create'),
                # ⚠️ 顺序：`latest/{period}` 与 `supplier/{id}` 都要排在裸 `/{id}` 之前
                #    （三条正则互不重叠，但按"最具体在前"写，免得将来加规则时踩顺序）
                ('GET',    r'/performances/supplier/\{[^/}]+\}/latest/\{[^/}]+\}$', 'supplier:performance:detail'),
                ('GET',    r'/performances/supplier/\{[^/}]+\}/(average|summary)$', 'supplier:performance:view'),
                ('GET',    r'/performances/supplier/\{[^/}]+\}$', 'supplier:performance:list'),
                ('GET',    r'/performances/\{[^/}]+\}$',          'supplier:performance:detail'),
                ('GET',    r'/inquiries/pending$',           'supplier:inquiry:list'),
                ('GET',    r'/inquiries/supplier/\{[^/}]+\}$','supplier:inquiry:list'),
                ('GET',    r'/inquiries/\{[^/}]+\}$',        'supplier:inquiry:detail'),
                ('POST',   r'/inquiries$',                   'supplier:inquiry:create'),
                # 供应商提交报价 = 新建一条报价 ⇒ create；
                # 采购方接受/拒绝报价 = 对一个决定表态 ⇒ approve（不新造 accept 动作词）
                ('POST',   r'/inquiries/\{[^/}]+\}/quotation$',      'supplier:quotation:create'),
                ('POST',   r'/quotations/\{[^/}]+\}/(accept|reject)$', 'supplier:quotation:approve'),
                # 积分加/减都是"改这个供应商的积分"⇒ update（RULES 会把 consume 判成
                # `consume`（占用）那种误义，把 add 判成 create，两条都不对）。
                ('POST',   r'/points/\{[^/}]+\}/(add|consume)$', 'supplier:points:update'),
                ('GET',    r'/points/\{[^/}]+\}/total$',     'supplier:points:view'),
                # 积分流水是**列表**（RULES 的 GET 兜底不认 `records`，会判成 view）
                ('GET',    r'/points/\{[^/}]+\}/records$',   'supplier:points:list'),
                ('GET',    r'/dashboard/\{[^/}]+\}$',        'supplier:dashboard:view'),
            ],
            # ── 门户查询（SupplierPortalInquiryController，base /api/supplier-portal）──
            # 只提供前端 `/supplier-portal/**` 两个查询（与上面 v1 版同码：同一份数据的两个入口）
            '/api/supplier-portal': [
                ('GET', r'/inquiries/supplier/\{[^/}]+\}$', 'supplier:inquiry:list'),
                ('GET', r'/points/\{[^/}]+\}/records$',     'supplier:points:list'),
            ],
        },
    },

    # ── 配送路线（erp-delivery-route）：**司机端已核实持有员工会话，不排除** ──
    # 交付判断（这一步判错会整块 403，故逐条给证据）：
    #   · 司机端 App（`frontend/apps/driver-delivery`）的登录端点是
    #     `api/index.ts:68` 的 `request.post('/auth/login')` —— 即**主站员工登录**
    #     （sys_user + BCrypt），拿到的 token 就是 Sa-Token 的员工 token
    #     （`utils/request.ts:32` 统一塞 `Authorization: Bearer`）；
    #   · 这些接口没有被任何"设备身份/机器对机器"路径调用：PC 端「配送 → 配送路线 →
    #     配送路线单」（菜单 80700 / `dms:route-list`，`api/dms/route.ts`）与司机端
    #     （`api/delivery-route.ts`）**共用同一批端点**，都是登录用户；
    #   · 库中 `erp_delivery_route.delivery_person_id` 的取值形如 1/3/64/269 与
    #     `sys_user.id` 同型（不是设备号）。
    #   ⇒ 与 wms 的 PDA 口径一致（真实 sys_user + 共用会话）⇒ **补码**。
    #   ⚠️ 交付后需给"司机"角色授予 delivery:route:{list,detail,execute,complete,update,sign}
    #      等码，否则司机端点会 403（这是 E-01 的既有代价，见报告"遗留"一节）。
    'deliveryroute': {
        'label': '配送',
        'slot': 27,
        'domain': 'delivery',
        'dirs': ['backend/erp/erp-delivery-route/src/main/java/cn/aiedge'],
        # ── 跨模块下拉源：**整条排除** ──
        # `GET /api/erp/md/route/options`（启用线路下拉）被 pc-admin **8 个页面**当
        # 「配送线路」下拉源引用（dispatch-order、ship-query、dispatch-task、order-pool、
        # dms/route、route-list、settlement…，见各文件里的 `mdRouteApi.options()`）。
        # 它和 `/api/erp/basic`、`/department/list` 同类 —— 是**数据源不是可勾选功能**：
        # 加码 ⇒ 只有拿到 `md:route:*` 的角色才能让这些页面的下拉有数据。
        'skip_methods': {
            '/api/erp/md/route': [('GET', r'/options$')],
        },
        # 资源重名消歧：RouteMasterController 推导出 `md:route`，RouteController 推导出
        # `delivery:route` —— 两条码的**资源名同为 route**，而权限名只能由
        # 「模块名 + 资源名 + 动作」拼 ⇒ 会出现两行同名（如「配送路线查询」）。
        # 按实体名（`RouteMaster` / 表 `erp_route`，页面上叫「线路档案」）把档案侧
        # 改成 `route-master`；执行单侧保持 `route`（页面就叫「配送路线**单**」）。
        'resource_overrides': {
            'md:route': {'resource': 'route-master'},
        },
        'code_rules': {
            # ⚠️ 本模块两个 base 都**没有** base_overrides，但仍一律写完整码
            #    （避免与上面的 resource_overrides 交互时出错：`@动作词` 只换动作词、
            #     资源仍走 resource_overrides，容易写出与这里不一致的码）。
            # ── 配送路线单（执行单，RouteController）──
            '/api/delivery/route': [
                # 状态流转（start→execute / complete / cancel）：RULES 已认 start/complete/cancel，
                # 但批量状态流转是裸 POST ⇒ 兜底会判 create，显式归 update。
                ('POST', r'/batch-status$',                          'delivery:route:update'),
                # 点位签收：一个独立的**业务动作**（谁签收由角色决定），
                # 不与"编辑路线单"合并（库中已有动作词 sign，见 crm:contract:sign）
                ('POST', r'/\{[^/}]+\}/point/\{[^/}]+\}/sign$',      'delivery:route:sign'),
                # 围栏自动归集预览是**干跑不落库** ⇒ view（同 core-api 的 import/v2 preview 口径）
                ('POST', r'/auto-collect/preview$',                  'delivery:route:view'),
                # 自动归集 / 手动加点位 / 路线规划 / 催单调整：都是**改这张路线单**
                # （RULES 的 POST 兜底会判成"新建路线单"）
                ('POST', r'/auto-collect$',                          'delivery:route:update'),
                ('POST', r'/\{[^/}]+\}/add-points$',                 'delivery:route:update'),
                ('POST', r'/\{[^/}]+\}/plan-order$',                 'delivery:route:update'),
                ('POST', r'/\{[^/}]+\}/point/\{[^/}]+\}/expedite$',  'delivery:route:update'),
                # 生成/投递 ETA 客户通知 = 改这张单（通知台账另算，见下）
                ('POST', r'/\{[^/}]+\}/notify-eta$',                 'delivery:route:update'),
                # 可入线的配送需求（销售出库单/销售订单的反查）是**另一个资源**：
                # 它是"配送需求"不是"配送路线单"，单独一个码便于按角色开关。
                ('GET',  r'/demands$',                               'delivery:demand:list'),
                # ETA 通知台账是**独立对象**（表 erp_delivery_eta_notify），四个端点一个资源。
                ('GET',  r'/eta-notify/page$',        'delivery:eta-notify:list'),
                ('POST', r'/eta-notify/status$',      'delivery:eta-notify:update'),
                # 补投递与发送都是"执行投递"（通道未接入时返回未配置），合并成 execute
                ('POST', r'/eta-notify/(dispatch|send)$', 'delivery:eta-notify:execute'),
            ],
            # ── 线路档案（RouteMasterController）→ `md:route-master:*` ──
            '/api/erp/md/route': [
                # Excel 导入（不是"新建一条线路"）⇒ 用库中既有动作词 import。
                # （`/import-template` 是 GET，RULES 会判 view，与"下载模板"语义一致，不再另立码。）
                ('POST', r'/import-excel$',   'md:route-master:import'),
                # 批量启用/停用与单条 `PUT /{id}/status` 共用同一个码（同一件事的两种入口）
                ('POST', r'/batch-status$',   'md:route-master:status'),
            ],
        },
    },

    # ── 客户等级（erp-partner 的 cn.aiedge.erp.customer 包）：归既有 party: 域 ──
    # 交付判断：`CustomerLevelController` 每个方法第一行都是 `StpUtil.getLoginIdAsLong()`
    #   （当 tenantId 用）⇒ **依赖 Sa-Token 会话**，是后台功能，正常补码。
    # 口径：类级路径 `/api/erp/customer/level` 推导出的「域:资源」正是
    #   `party:customer-level` —— 与库中**既有**的 `party:customer-region:*`
    #   （`/api/erp/customer/region`）同族、同命名风格，故**不用任何 override**，
    #   也不新建一级域；`party:` 已归属 master-data，无需新增映射。
    'partnerlevel': {
        'label': '往来单位',
        'slot': 28,
        'domain': 'party',
        'dirs': ['backend/erp/erp-partner/src/main/java/cn/aiedge/erp/customer'],
        'code_rules': {
            '/api/erp/customer/level': [
                # `POST /calculate` 是**纯计算**（按交易金额/频次/回款率算出应该属于哪级，
                # 方法体只调 `calculateCustomerLevel` 并返回结果）⇒ view，不是 create。
                # 同等先例：定价模块的"用引擎算价"全部归 `pricing:engine:view`。
                ('POST', r'/calculate$',         'party:customer-level:view'),
                # `POST /assign/{customerId}` 是**把等级分配给客户**（真落库）⇒ 用
                # 库中既有动作词 assign（`hr:*:assign` / `dms:dispatch:assign` 先例）。
                ('POST', r'/assign/\{[^/}]+\}$', 'party:customer-level:assign'),
            ],
        },
    },

    # ── 提成分析（erp-marketing 的 marketing.analytics 包）：域沿用 marketing ──
    # 交付判断：控制器方法体用 `StpUtil.isLogin() / getLoginId()` 取操作人
    #   ⇒ 依赖会话，后台功能，正常补码。
    # 域口径：类级路径 `/api/erp/marketing/commission/analytics` 推导出
    #   `marketing:commission-analytics`，与库中既有的 `marketing:commission-rule:*`
    #   / `marketing:commission-record:*`（同住 commission 子路径）同族，故不 override。
    #   ⚠️ 前端菜单码是 `ana:commission-center`（分析中心），但**不许**造 `ana:` 前缀：
    #      `ana:` 若映射到 analytics 模块会撞 verify-module-mapping.cjs 的
    #      「analytics 映射数必须为 0」硬断言；码前缀落在 `marketing:` 才是对的
    #      （数据本身就是提成，归营销模块）。
    'mktanalytics': {
        'label': '营销',
        'slot': 29,
        'domain': 'marketing',
        'dirs': ['backend/erp/erp-marketing/src/main/java/cn/aiedge/erp/marketing/analytics'],
        'code_rules': {
            '/api/erp/marketing/commission/analytics': [
                # 批量结算（未结 → 已结算 PAID）是**独立的财务动作**，不并入 update：
                # 库中还没有 `settle` 这个动作词，本批在 ACTION_LABEL 里补中文标签。
                # （不并进 update 的理由：能与"编辑提成配置"分离授权，避免"能改配置的人顺便能结算"。）
                ('POST', r'/settle$', 'marketing:commission-analytics:settle'),
                # `GET /plans`（工具栏「提成方案」只读入口）读的**就是**
                # `erp_commission_rule` 表（`CommissionAnalyticsServiceImpl#planList`），
                # 与提成规则页的 `marketing:commission-rule:list` 是同一个对象
                # ⇒ **复用既有码**，不新造 `marketing:commission-analytics:view`。
                ('GET',  r'/plans$',  'marketing:commission-rule:list'),
            ],
        },
    },

    # ── 库存杂项（erp-stock 的 controller.initial 与 analytics 两个子包）──
    # 为什么单独一个模块键：这两个控制器在**子包**里，`stock` 模块的 dirs 只给到
    #   `.../erp/stock/controller`（旧实现只扫一层，后来改成递归但批次已跑过）
    #   ⇒ 它们从未被任何批次覆盖。这里**必须只给两个子目录**，不能给
    #   `.../erp/stock` 根 —— 那会把整个库存控制器树重扫一遍、造出上百个已存在的码。
    # 域口径：两条类级路径推导出的域都**已经存在**，无需 override：
    #   · `/api/set/initial-stock` → `set:initial-stock`（与库中既有
    #     `set:initial-finance:*`（期初财务，同一"期初"功能族）逐字对称）；
    #   · `/api/erp/stock/analytics` → `stock:analytics`（与 `stock:take` / `stock:check`
    #     同域；⚠️ 它**不是** `analytics:` 前缀 —— 前端菜单码虽是 `ana:inventory-analysis`，
    #     但码前缀必须落 `stock:`，否则会撞 analytics 模块"必须为 0"的断言）。
    'stockextra': {
        'label': '库存',
        'slot': 30,
        'domain': 'stock',
        'dirs': [
            'backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/controller/initial',
            'backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/analytics',
        ],
        # 无需 code_rules：6 个端点的动作通用规则都判对了 ——
        #   GET /page→list、POST /save→create（与 `set:initial-finance:create` 同口径：
        #   期初的"保存"就是录入）、PUT /update→update、DELETE /{id}→delete、
        #   GET /export→export、GET /page→list。
    },
}

# 端点 → 动作。**有序**，首个匹配生效。
# 只使用库中已有的动作词；不新造同义词。
RULES = [
    # ── GET ──
    ('GET',    r'.*/(export|download)(/.*)?$', 'export'),
    # 注意要含 `list-by-*`（如 list-by-budget/{id}、list-by-year）——
    # 只写 `list` 会漏掉它们，落到下面的 `detail`/`view` 兜底规则上。
    ('GET',    r'.*/(page|list|list-by-[^/]*|rows|items|logs|warnings|next-no|doc-query|options|tree)(/.*)?$', 'list'),
    ('GET',    r'.*/\{[^/}]+\}$',              'detail'),
    ('GET',    r'.*',                          'view'),      # 兜底：报表/汇总/统计类读接口
    # ── POST ──
    ('POST',   r'.*/batch-delete$',            'delete'),
    ('POST',   r'.*/batch-submit$',            'submit'),
    ('POST',   r'.*/batch-(approve|reject)$',  'approve'),
    ('POST',   r'.*/(approve|reject|audit)$',  'approve'),
    ('POST',   r'.*/submit$',                  'submit'),
    ('POST',   r'.*/(start-exec|execute|run)$', 'execute'),
    ('POST',   r'.*/close$',                   'close'),
    # wms/purchase 批次补：`batch-print` 此前不匹配 ⇒ 落到 create 兜底（"批量打印"变成"新建单据"）
    ('POST',   r'.*/(batch-)?print$',          'print'),
    ('POST',   r'.*/publish$',                 'publish'),
    ('POST',   r'.*/check$',                   'check'),
    ('POST',   r'.*/freeze$',                  'freeze'),
    ('POST',   r'.*/release$',                 'release'),
    ('POST',   r'.*/consume$',                 'consume'),
    ('POST',   r'.*/import$',                  'import'),
    # 业务动作（都要先于 POST 兜底，否则会被当成"新建"）。
    # 这些动作词都来自**库中已有的历史码**（hr:salary:generate / hr:candidate:interview /
    # hr:attendance:clock），保住它们就等于保住职责分离，不是新造词。
    ('POST',   r'.*/(generate|gen)$',          'generate'),
    ('POST',   r'.*/interview$',               'interview'),
    ('POST',   r'.*/clock(-in|-out)?$',        'clock'),
    # ── 2026-09-21 wms 批次补：**状态/生命周期类 POST**。此前 POST 只有 approve/submit 等几条，
    #    其余一律落到下面的 create 兜底 ⇒ 「启动拣货任务」「完成盘点」「取消入库」全被算成"新建单据"，
    #    那是明显的语义错位（"能建收货单的人才能取消它"说不通）。动作词沿用库中已有写法。
    ('POST',   r'.*/(start|begin|launch)$',    'execute'),
    ('POST',   r'.*/cancel$',                  'cancel'),
    ('POST',   r'.*/(confirm|verify)$',        'confirm'),
    ('POST',   r'.*/(complete|finish)$',       'complete'),
    ('POST',   r'.*/(pause|suspend)$',         'pause'),
    ('POST',   r'.*/(resume|restart)$',        'resume'),
    ('POST',   r'.*/retry$',                   'retry'),
    ('POST',   r'.*/post$',                    'post'),
    ('POST',   r'.*/reconcile$',               'reconcile'),
    ('POST',   r'.*',                          'create'),    # 兜底：新建/保存
    # ── PUT / PATCH：动作后缀要**先于**下面的兜底匹配 ──
    # 为什么必须补这几条：本仓大量"动作"端点用的是 PUT 而不是 POST
    # （`PUT /api/hr/leave/{id}/approve`、`PUT /api/hr/performance/{id}/confirm`…），
    # 只按 POST 写规则会让它们全部落到 `update` 兜底上 ⇒ **把「审批」与「编辑」合并成同一个码**，
    # 等于丢掉职责分离（能改请假单的人顺便能批准它）。实测 hr 域有 6 条历史动作码会被这样吃掉。
    ('PUT',    r'.*/(approve|reject|audit)$',  'approve'),
    ('PUT',    r'.*/submit$',                  'submit'),
    ('PUT',    r'.*/(confirm|verify)$',        'confirm'),
    ('PUT',    r'.*/(generate|gen)$',          'generate'),
    ('PUT',    r'.*/(status|state)$',          'status'),
    ('PUT',    r'.*/cancel$',                  'cancel'),
    ('PUT',    r'.*/close$',                   'close'),
    ('PUT',    r'.*/publish$',                 'publish'),
    ('PUT',    r'.*/execute$',                 'execute'),
    # wms 批次补：PDA 的"完成任务"是 PUT（与 PC 端的 POST /complete 同义）
    ('PUT',    r'.*/(complete|finish)$',       'complete'),
    # ── PUT / PATCH / DELETE ──
    ('PUT',    r'.*',                          'update'),
    ('PATCH',  r'.*',                          'update'),
    ('DELETE', r'.*',                          'delete'),
]

# 中文名后缀，用于 sys_permission.permission_name（矩阵里显示的就是它）
ACTION_LABEL = {
    'list': '查询', 'detail': '详情', 'view': '查看', 'create': '新增',
    'update': '编辑', 'delete': '删除', 'submit': '提交', 'approve': '审批',
    'export': '导出', 'import': '导入', 'execute': '执行', 'close': '关闭',
    'print': '打印', 'publish': '发布', 'check': '校验', 'freeze': '冻结',
    'release': '解冻', 'consume': '占用', 'cancel': '取消',
    'confirm': '确认', 'generate': '生成', 'interview': '面试',
    'clock': '打卡', 'rule': '规则', 'quota': '额度', 'status': '状态',
    # ── crm 批次（2026-09-21）：这批动作词全部**取自库中既有 crm 码**，
    #    标签也照库中既有 permission_name 的用词（如 crm:contract:sign = "CRM合同签署"），
    #    不新造词、不改写既有叫法。
    'edit': '编辑', 'convert': '转换', 'batchconvert': '批量转换',
    'send': '发送', 'batchsend': '批量发送', 'sendfromdetail': '明细发送',
    'sign': '签署', 'renewapply': '续签申请', 'refresh': '刷新',
    'download': '下载', 'downloadpdf': '下载PDF', 'follow': '跟进',
    'detailrefresh': '明细刷新', 'batchapprove': '批量审批',
    'batchassign': '批量分配', 'assign': '分配', 'addtolevel': '加入等级',
    'issue': '开具', 'cancelconfirm': '取消确认',
    'convertfromdetail': '明细转换', 'manage': '管理',
    # ── wms 批次（2026-09-21）：作业类动作
    'retry': '重试', 'post': '过账', 'reconcile': '对账', 'return': '归还',
    'pause': '暂停', 'resume': '恢复', 'complete': '完成',
    # ── erpprinting / erppricing / erpobserv / erpbatchsn 批次（2026-09-21）──
    #    只新增本批用到的动作词；`update-status`/`transfer`/`clear`/`check` 都是
    #    **库中既有**的动作词（sys_permission 里分别有 6/1/2/7 条），此处只是补中文标签。
    'copy': '复制', 'update-status': '更新状态', 'transfer': '调拨', 'clear': '清理',
    'inbound': '入库', 'outbound': '出库', 'inspect': '质检',
    # ── core 底座批次（2026-09-21：corebase / coreplatform / corenotify / corepayment）──
    #    只补本批用到的动作词；`stats` 是**库中既有**的动作词
    #    （log:audit:stats / log:login:stats / log:oper:stats），此处只是补中文标签。
    'stats': '统计',
    # ── E-01 最后一批（2026-09-21：b2b / supplier / deliveryroute / partnerlevel /
    #    mktanalytics / stockextra）──
    #    只补本批用到的动作词：`refund`（商城订单退款，独立敏感动作）、
    #    `settle`（提成批量结算）。其余全部复用库中既有动作词
    #    （payment / ship / cancel / approve / assign / status / import / check / view …）。
    'refund': '退款', 'settle': '结算',
    #    另两个是本批**从历史码里借来的动作词**（`sale:order:ship`=「销售订单发货」、
    #    `sale:order:payment`=「销售订单收款」），此前 ACTION_LABEL 里没有对应中文，
    #    补上后本批的 mall:order:{ship,payment} 才会拼出同一个叫法。
    'ship': '发货', 'payment': '收款',
}

# ⚠️ 资源标签**不要**再带模块名（模块名由 `MODULES[..]['label']` 提供），
#    否则会拼出「预算预算调整审批」这种重复。
RESOURCE_LABEL = {
    'annual': '年度', 'adjustment': '调整', 'control': '控制',
    'execution': '执行', 'item': '明细', 'report': '报表',
    'template': '模板',
    # ── crm 批次（2026-09-21）：照库中既有 permission_name 的中文用词
    'contract': '合同', 'customer': '客户', 'lead': '线索',
    'opportunity': '商机', 'quotation': '报价单', 'quotation-template': '报价模板',
    'follow-up': '跟进', 'customer-pool': '公海池', 'marketing': '营销活动',
    'visit': '拜访',
    # ── wms 批次（2026-09-21）
    'receipt': '收货单', 'putaway': '上架单', 'pick': '拣货单', 'ship': '发货单',
    'move': '移库单', 'check': '盘点单', 'inventory': '库存', 'location': '库位',
    'warehouse': '仓库', 'borrow': '借还单', 'event': '事件', 'task': '任务',
    # ── erpprinting / erppricing / erpobserv / erpbatchsn 批次（2026-09-21）──
    #    printer / chain 不写「打印机」「打印链」—— 模块名前缀已是「打印」，
    #    否则会拼出「打印打印机新增」「打印打印链发布」这种重复。
    'log': '日志', 'printer': '设备', 'printer-group': '设备分组', 'chain': '链路',
    'client': '客户端', 'screenshot': '截图', 'format': '格式化', 'message': '消息',
    'record': '签收记录', 'rating': '配送评价',
    'engine': '引擎', 'strategy': '策略', 'tier': '价层', 'price': '取价',
    'approval': '价格变更', 'grade-price': '等级价',
    'metric': '指标', 'business-metric': '业务指标',
    'batch': '批次', 'serial': '序列号',
    # ── core 底座批次（2026-09-21：corebase / coreplatform / corenotify / corepayment）──
    #    ⚠️ 本字典是**全局**的（按资源名取中文），少数键与既有域同名（channel/card），
    #       只影响**将来重新生成时**的显示名，已落库的 permission_name 不会被改动。
    #    刻意**不**为 `record` 加标签（它已被 signature 批次占用为「签收记录」），
    #       /api/core/notification 的资源改用 `message`，避免拼出「通知服务签收记录…」。
    'menu-config': '菜单配置', 'structured': '结构化日志', 'error-report': '错误报告',
    'channel': '渠道', 'stats': '统计',
    # 'menu' 只被 `system:menu:*` 用到（该族既有 permission_name 本来就是「菜单管理新增」这种中文），
    # 补上后本批的 `system:menu:check` 不会再拼出「…menu校验」这种半英文名。
    'menu': '菜单',
    'certificate': '质量证书', 'inspection': '质检单', 'standard': '质量标准',
    'defect': '缺陷记录',
    'external-order': '外部订单', 'inventory-sync': '库存同步', 'api-monitor': '接口监控',
    'rule': '自动化规则', 'field': '自定义字段', 'value': '字段值', 'group': '字段分组',
    'board': '看板', 'column': '看板列', 'card': '看板卡片',
    'webhook': '事件钩子', 'webhook-log': '钩子日志',
    'refund': '退款申请', 'reconciliation': '对账',
    # ── E-01 最后一批（2026-09-21：b2b / supplier / deliveryroute / partnerlevel /
    #    mktanalytics / stockextra）──
    #    ⚠️ 同 core 底座批次的注意事项：本字典是**全局**的，下列键若与既有域同名，
    #       只影响**将来重新生成**时的显示名，已落库的 permission_name 不会被改动。
    #       这里给的都是通名（配置/用户/订单/商品…），对同名的其它域也成立，不会造成错名。
    # b2b（域 mall）
    'config': '配置', 'user': '用户', 'banner': '轮播图', 'decoration': '装修',
    'product': '商品', 'order': '订单', 'trade-analysis': '交易分析',
    'keyword': '关键词', 'notice': '公告', 'popup-ad': '弹窗广告',
    # supplier（域 supplier）
    # ⚠️ `supplier` 指向**两段码**（`supplier:list` 这种「域:动作」）：资源位取的是域自身，
    #    这里映射成空串，让名称退回「模块名 + 动作」——与库中既有的两段码同款写法
    #    （`party:create` = 「往来单位新增」、`stock:list` = 「库存查看」）。
    'supplier': '', 'level': '等级', 'performance': '绩效', 'inquiry': '询价单',
    'quotation': '报价', 'points': '积分', 'dashboard': '工作台',
    # deliveryroute（域 delivery / md）
    'route': '路线', 'route-master': '线路', 'demand': '需求', 'eta-notify': 'ETA通知',
    # partnerlevel（域 party）
    'customer-level': '客户等级',
    # mktanalytics（域 marketing）
    'commission-analytics': '提成分析',
    # stockextra（域 set / stock）
    'initial-stock': '期初', 'analytics': '分析',
}

CLS_RM = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?["\']([^"\']*)["\']')
MAPPING = re.compile(
    r'@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(\s*(?:value\s*=\s*)?(?:["\']([^"\']*)["\'])?[^)]*\))?')
REST = re.compile(r'@(RestController|Controller)\b')


def iter_controllers(dirs):
    """递归产出 dirs 下所有「控制器」Java 文件的绝对路径。

    ⚠️ 原实现用 `os.listdir`（**只扫一层**），于是「控制器按子包分目录」的模块整片漏掉：
    `crm/contract/controller`、`erp-finance/expensedoc/controller`、
    `erp-stock/controller/initial` 在旧实现下**一个端点都扫不到**。
    改为递归后，MODULES 里每个模块只需给一个**模块根目录**。
    """
    for d in dirs:
        full = os.path.join(ROOT, d)
        if not os.path.isdir(full):
            continue
        for dirpath, dirnames, filenames in os.walk(full):
            dirnames[:] = [x for x in dirnames if x != 'target']
            if os.sep + 'src' + os.sep + 'test' in dirpath:
                continue
            for name in sorted(filenames):
                if name.endswith('.java'):
                    yield os.path.join(dirpath, name)


def scan(dirs):
    """返回端点行，按扫描顺序。

    ⚠️ 与 `apply_annotations` **共用同一套 `MAP_LINE` 逐行解析**，并且逐条标记
    `annotated`（该方法是否已带权限注解）。为什么必须这样做（2026-09-21 实测踩到）：
    原实现用另一条正则 `MAPPING.findall(text)` 抽端点，**看不见方法上已有的注解** ——
    于是 `SalesPriceTrackController`（整类 6 个端点早就挂 `sale:price:edit`）仍被算成
    "需要建码"，`--sql` 会给它造出 `sale:price-track:*` 五个**永远不会被插入的僵尸码**
    （`--apply` 侧会因为幂等而跳过，两边结论分叉）。现在 `--sql` 只统计
    **确实会被插注解** 的端点，与 `--apply` 结论一致。
    """
    rows = []
    for fp in iter_controllers(dirs):
        text = open(fp, encoding='utf-8').read()
        if not REST.search(text):
            continue
        m = CLS_RM.search(text)
        if not m:
            continue
        base = m.group(1).rstrip('/')
        lines = text.splitlines()
        # 类级已有权限注解 ⇒ 整类都不需要插（与 apply_annotations 的判据一致）
        class_covered = bool(PERM_ONLY.search(strip_comments(text.split('class ')[0])))
        for i, line in enumerate(lines):
            mm = MAP_LINE.match(line)
            if not mm:
                continue
            verb = mm.group(2).upper()
            rest = mm.group(3)
            pm = re.search(r'["\']([^"\']*)["\']', rest)
            path = pm.group(1) if pm else ''
            # 路径换行写在下一行时的兜底。
            # ⚠️ 下一行必须以**引号开头**（注解参数的续行）才认，否则会误抓下一行里
            # 别的字符串 —— 实测 dms 的 `@PostMapping`（无路径）后面紧跟
            # `@Parameter(description = "任务信息")`，被当成路径 ⇒ 产出
            # `dms:task任务信息:create` 这种垃圾码（幸好铁律校验会拦住，不会插注解）。
            if not pm and i + 1 < len(lines):
                if lines[i + 1].strip()[:1] in ('"', "'"):
                    pm2 = re.search(r'["\']([^"\']*)["\']', lines[i + 1])
                    if pm2:
                        path = pm2.group(1)
            path = (path or '').strip()
            window = []
            for prev in reversed(lines[max(0, i - 6):i]):
                s = prev.strip()
                if s == '' or s.startswith('}') or s.startswith('{'):
                    break
                window.append(s)
            for nxt in lines[i + 1:i + 4]:
                s = nxt.strip()
                if s == '' or s.startswith('}') or s.startswith('{'):
                    break
                window.append(s)
            rows.append({
                'file': os.path.basename(fp), 'base': base, 'verb': verb, 'path': path,
                'endpoint': f'{verb} {base}{path}',
                'annotated': class_covered or bool(PERM_ONLY.search(strip_comments('\n'.join(window)))),
            })
    return rows


# 路径级覆盖：**整条基路径**决定域+资源，优先于一切段级规则。
# 用于消解「同名不同物」——最简单的段级规则解决不了这类冲突。
# 例：`payment` 这个域在库里**历史上指第三方支付网关**（`/api/payment/*`、`/refund`、
# `/reconciliation`，核心支付模块）；而 `/api/erp/payment` 是 **ERP 付款单**（财务域）。
# 两者共用一个域会让权限矩阵出现「支付-查询」这种指代不明的条目 —— 故后者强制归 finance。
PATH_OVERRIDE = {
    '/api/erp/payment': ('finance', 'payment'),
}

# 已知域：用于把 `product-category`、`stock-in` 这类**域+资源连写**的第一段拆回「域, 资源」，
# 否则会产出一堆 `product-category:*`、`warehouse-category:*` 这种一级域，
# 与 `/api/erp/product/units → product:units:*` 自相矛盾。
KNOWN_DOMAINS = {
    'stock', 'product', 'md', 'marketing', 'mall', 'party', 'partner', 'invoice',
    'payment', 'finance', 'purchase', 'sale', 'system', 'crm', 'hr', 'dms', 'wms',
    'budget', 'fixed-asset', 'doc', 'log', 'workflow', 'tenant', 'user', 'role',
    'permission', 'datasource', 'platform', 'set', 'position', 'department',
    'receipt', 'print', 'erp',
}


def split_head(head):
    """把路径首段拆成 (域, 附加资源名)。"""
    if head in DOMAIN_ALIAS:
        return DOMAIN_ALIAS[head]
    if '-' in head:
        pre, rest = head.split('-', 1)
        if pre in DOMAIN_ALIAS:
            d, e = DOMAIN_ALIAS[pre]
            return d, (f'{e}-{rest}' if e else rest)
        if pre in KNOWN_DOMAINS:
            return pre, rest
    return head, None


def domain_resource_of(base):
    """由类级路由路径推出 (域, 资源)。资源为空时返回 None（产出两段码）。"""
    for prefix, (dom, res) in PATH_OVERRIDE.items():
        if base == prefix or base.startswith(prefix + '/'):
            tail = base[len(prefix):].lstrip('/')
            parts = ([res] if res else []) + ([tail] if tail else [])
            return dom, ('-'.join(parts) if parts else None)
    segs = [s for s in base.strip('/').split('/') if s]
    if segs and segs[0] == 'api':
        segs = segs[1:]
    # 跳过容器段：/api/erp/... 与 /api/v1/... 里的 erp / v1
    # ⚠️ 不能跳 'admin'：`/api/erp/mall/admin/*` 里 admin 是**资源段**
    if segs and segs[0] in ('erp', 'v1', 'v2'):
        segs = segs[1:]
    if not segs:
        return 'misc', None
    dom, extra = split_head(segs[0])
    rest = segs[1:]
    parts = ([extra] if extra else []) + rest
    return dom, ('-'.join(parts) if parts else None)


def resource_of(base):
    """仅返回资源部分（无资源时返回域自身，避免 NPE）。"""
    dom, res = domain_resource_of(base)
    return res or dom


def domain_of(base, fallback):
    return domain_resource_of(base)[0] or fallback


# 这些段是"动作/容器"而不是"实体"，取资源时要跳过
SEG_SKIP = {
    'page', 'list', 'detail', 'create', 'update', 'delete', 'view', 'export', 'import',
    'batch', 'stat', 'statistics', 'options', 'tree', 'next-no', 'search', 'query',
    'submit', 'approve', 'reject', 'cancel', 'confirm', 'generate', 'refresh', 'save',
    'test', 'logs', 'history', 'all', 'count', 'simple',
}

# 域内"历史资源名"词表（由库中已有码的第 2 段构成），用于单复数对齐 ——
# 目标是让新码与**前端 v-permission 里的旧字符串**尽量同名，减少接线改动。
RESOURCE_VOCAB = {}


def first_resource_segment(path):
    """从方法路径里取「这是哪个实体的接口」。

    仅当**类级路径没有给出资源**时使用。本仓存在把整个域挂在一个扁平基路径下的控制器
    （`HrController` 的 base 就是 `/api/hr`，全部实体都写在方法路径上：
    `/positions/page`、`/employees`、`/salary/structure`、`/leave/{id}/approve`…）。
    不做这层回退的话，这些端点会全部塌成 `hr:create` / `hr:update` 这类**没有任何实体信息**的码
    —— 实测 92 个端点只产出 18 个码，其中 `hr:update` 一个码覆盖 19 个端点，
    等于「能改职位的人也能改工资结构」，粒度反而比历史码更粗。
    """
    segs = [s for s in path.strip('/').split('/') if s and not s.startswith('{')]
    for s in segs:
        if s in SEG_SKIP:
            continue
        return s
    return None


def normalize_resource(res, domain):
    """把资源名对齐到该域历史的单复数写法（`positions` → `position`）。

    只做**保守的单复数对齐**：仅当去掉尾部 `s` 后能在历史词表里命中才替换，
    避免把 `status` / `statistics` 这类本身以 s 结尾的词改坏。
    """
    vocab = RESOURCE_VOCAB.get(domain)
    if not res or not vocab or res in vocab:
        return res
    if res.endswith('s') and res[:-1] in vocab:
        return res[:-1]
    return res


def code_of(base, verb, path, fallback_domain, cfg=None):
    """把「(类级路径, 动词, 方法路径)」解析成权限码。

    <p>解析优先级：**① 模块的 `code_rules`（按方法路径精确命中）→ ② 模块的
    `resource_overrides`（按「域:资源」换名 + 换动作词）→ ③ 通用推导**。</p>

    <p><b>为什么需要 ①②</b>（2026-09-21 crm 批次新增）：通用推导只能给到
    `<域>:<资源>:<动作>`，而**老域的历史码粒度更细、动词表也不同** —— 实测 crm 185 个端点
    推导出的 69 个码与库中 57 个历史码**只交集 8 条**。更要命的是通用规则的 POST 兜底是
    `create`，于是 `POST /{id}/sign`、`/{id}/terminate`、`/mark-expired` 全被当成"新建合同"。
    继续用推导结果就等于**再造一套平行命名空间**（本仓已踩过 `product:*` 那次）。
    故对这类域改为**显式声明**映射：命中即用，未命中才回落到通用推导（用于该域里
    确实没有历史码的资源，如 crm 的 followUp / customer-pool / visit / marketing /
    quotation-template —— 它们现场建码，推导与库天然一致）。</p>
    """
    dom, res = domain_resource_of(base)
    dom = dom or fallback_domain
    if not res:
        res = first_resource_segment(path)
    res = normalize_resource(res, dom)

    cfg = cfg or {}

    # ① 路径级规则（最高优先）：同一资源下动作语义差异大的控制器只能这样表达。
    #    **按类级路径分组**，不写成全局规则 —— 方法路径不带资源信息，
    #    全局的 `/{id}/convert` 会同时命中 lead 与 quotation 两个控制器（实踩风险）。
    for v, rx, code in (cfg.get('code_rules') or {}).get(base, []):
        if v == verb and re.fullmatch(rx, path):
            # `@动作词` = **只改动作词**，域与资源仍走正常解析。
            # 用于「同一控制器里一批 POST 子路径都该算作 update，但资源各不相同」的情形
            # （dms 的 211 个端点里 60+ 处是这种状态推进：抢单/核销/派单/优化…），
            # 逐个写完整码既冗长又容易写错资源名。
            if code.startswith('@'):
                act = code[1:]
                return f'{dom}:{res}:{act}' if res else f'{dom}:{act}'
            return code

    # ①.5 类级覆盖（按**类级路径**为键）：当推导出的「域:资源」不足以区分控制器时必须用它。
    #      典型是 wms 的 PDA 端：`/api/v1/warehouse/receipt`、`/api/v1/warehouse/pick`…
    #      推导出来全是 `dom=wms, res=None`（容器段 `v1` 之后只剩 `warehouse` 被别名吃掉），
    #      用「域:资源」当键会**八个控制器共用一个键**、互相覆盖。
    base_spec = (cfg.get('base_overrides') or {}).get(base)
    if base_spec:
        dom = base_spec.get('domain', dom)
        res = base_spec.get('resource', res)
        act0 = action_of(verb, path)
        act = (base_spec.get('actions') or {}).get(act0, act0)
        return f'{dom}:{res}:{act}' if res else f'{dom}:{act}'

    # ② 资源级覆盖：换域/资源名 + 换动作词
    spec = (cfg.get('resource_overrides') or {}).get(f'{dom}:{res}' if res else dom)
    if spec:
        dom = spec.get('domain', dom)
        res = spec.get('resource', res)
        act = (spec.get('actions') or {}).get(action_of(verb, path), action_of(verb, path))
    else:
        act = action_of(verb, path)
    return f'{dom}:{res}:{act}' if res else f'{dom}:{act}'


def action_of(verb, path):
    for v, rx, act in RULES:
        if v == verb and re.fullmatch(rx, path):
            return act
    return 'view'


def is_skipped(cfg, base, verb=None, path=None):
    """按配置排除「绝不能加权限注解」的端点。

    <p>这是 **E-01 的安全阀**：不是所有端点都该有权限码。已知两类必须排除：</p>
    <ul>
      <li><b>登录端点</b>：未登录必须可达（在 `SaTokenConfig` 白名单里）。给它加码 = 永远登不进来。</li>
      <li><b>机器对机器接口</b>：没有用户会话（外部系统入站指令、内部 outbox 回调），
          加码必然打不通。**这类接口的正确做法是补服务间鉴权（验签/内部令牌），
          不是塞进白名单** —— 塞白名单等于把它变成匿名可达。</li>
    </ul>
    <p>配置形态：`skip_bases`（整个控制器）与 `skip_methods`（按类级路径 → [(动词, 方法路径正则)]）。</p>
    """
    if base in (cfg.get('skip_bases') or []):
        return True
    if verb is None:
        return False
    for v, rx in (cfg.get('skip_methods') or {}).get(base, []):
        if v == verb and re.fullmatch(rx, path):
            return True
    return False


def build(module):
    cfg = MODULES[module]
    load_resource_vocab(cfg['domain'])
    rows = [r for r in scan(cfg['dirs'])
            if not is_skipped(cfg, r['base'], r['verb'], r['path'])
            # 已带权限注解的端点不进对照表 / 不建码（否则会造出永远不会被插的僵尸码）
            and not r.get('annotated')]
    for r in rows:
        # 一律走 code_of（含 code_rules / resource_overrides），
        # 保证「扫描出的对照表」与「--apply 实际插入的注解」是同一个来源 ——
        # 两处各算一遍必然分叉，而分叉的表现是"看起来跑过了、注解却不是对照表里那个"。
        r['code'] = code_of(r['base'], r['verb'], r['path'], cfg['domain'], cfg)
        parts = r['code'].split(':')
        r['domain'] = parts[0]
        r['resource'] = ':'.join(parts[1:-1]) or parts[0]
        r['action'] = parts[-1]
    codes = {}
    for r in rows:
        codes.setdefault(r['code'], []).append(r)
    return rows, codes


def load_resource_vocab(domain):
    """把该域**库中已有码**的第 2 段收成词表（只读库；失败则退化为空词表 = 不做单复数对齐）。"""
    if domain in RESOURCE_VOCAB:
        return RESOURCE_VOCAB[domain]
    try:
        import psycopg2
        conn = psycopg2.connect(host='localhost', port=5432, dbname='devdb',
                                user='devuser', password='devuser123')
        try:
            cur = conn.cursor()
            cur.execute('SELECT permission_code FROM sys_permission')
            codes = [r[0] for r in cur.fetchall()]
            cur.close()
        finally:
            conn.close()
    except Exception:
        RESOURCE_VOCAB[domain] = set()
        return RESOURCE_VOCAB[domain]
    RESOURCE_VOCAB[domain] = {c.split(':')[1] for c in codes
                              if c.count(':') >= 2 and c.split(':')[0] == domain}
    return RESOURCE_VOCAB[domain]


def label_for(module, resource, action):
    res = RESOURCE_LABEL.get(resource, resource)
    # name_prefix 用于「模块显示名 与 既有权限码名里的模块前缀不一致」的域：
    # crm 的模块名已按用户口径改成「客户服务」，但库里 57 个既有码一律叫「CRM合同审批」，
    # 新码必须沿用同一个前缀，否则权限矩阵里同一模块会出现两种叫法。
    prefix = MODULES[module].get('name_prefix') or MODULES[module]['label']
    return f"{prefix}{res}{ACTION_LABEL.get(action, action)}"


MAP_LINE = re.compile(r'^(\s*)@(Get|Post|Put|Delete|Patch)Mapping\b(.*)$')
# 类级覆盖判定：任何鉴权注解都算"这个类已经管了"
ANY_AUTH = re.compile(r'@(SaCheckPermission|SaCheckLogin|SaCheckRole|RequirePermission|RequiresPermission|SaIgnore)\b')
# 方法级幂等判定：只认**权限类**注解。
# ⚠️ 不能复用 ANY_AUTH —— 本仓方法上普遍有 `@SaCheckLogin`（只要登录即可访问），
# 若把它当成"已有权限控制"，E-01 要补的注解会被**整片跳过**（看起来跑了、实际什么都没补）。
# ⚠️ 2026-09-21 core-api 批次实测补齐 `RequiresPermission`：它是 core-base 的注解
#    （`cn.aiedge.common.permission.RequiresPermission`，由 `PermissionAspect` 真实执行），
#    此前不在此正则里 ⇒ 用它做门禁的 28 个方法被判成"裸端点"、会被插出第二个注解。
#    本批恰好把新码与既有注解对齐成同一个码，行为零变化；但判据本身必须认得它，
#    否则下一个模块就会插出真正的**双重门禁**（两个不同码 ⇒ 两个都要满足）。
PERM_ONLY = re.compile(r'@(SaCheckPermission|RequirePermission|RequiresPermission)\b')
IMPORT_LINE = 'import cn.dev33.satoken.annotation.SaCheckPermission;\n'

# ── 注释剥离（2026-09-21 erpprinting/erppricing 批次实测补）──
# ⚠️ 判"这个类/方法是否已有权限注解"之前**必须先剥注释**：本仓 Javadoc 会写
# `{@code @SaCheckPermission}` 来解释设计（实测 `erp-pricing/.../PriceApprovalController.java:43`），
# 不剥就会被当成真注解 ⇒ 整个控制器被跳过、8 个端点（含审批通过/拒绝）**漏补**，
# 而且不报错不告警（`--apply` 只打印"跳过（类级已有权限注解）"）。
# 这是 fail-open（漏补 = 登录后可访问），但下一个模块必再踩。
_BLOCK_COMMENT = re.compile(r'/\*.*?\*/', re.S)
_LINE_COMMENT = re.compile(r'//[^\n]*')


def strip_comments(s):
    """去掉 Java 块注释与行注释（只用于"是否已有注解"的判据）。"""
    return _LINE_COMMENT.sub('', _BLOCK_COMMENT.sub('', s))


def load_db_codes():
    """只读 `sys_permission.permission_code`（用于 --apply 前的铁律校验）。

    ⚠️ **这是本脚本唯一连库的地方，且只读**。为什么要连库：
    `--apply` 会给端点插 `@SaCheckPermission("<推导出的码>")`，而**如果那个码不在库里，
    该接口会对所有非超管一律 403**（本仓铁律，见记忆 `permission-code-missing-lockout`）。
    推导规则（路径 → 码）与库里历史码**不一定逐字一致**（历史码是人写的），
    所以必须逐条比对；不一致的一律**不插**并列入 `missing` 报告。
    """
    if 'codes' in DB_CODE_CACHE:
        return DB_CODE_CACHE['codes']
    import psycopg2
    conn = psycopg2.connect(host='localhost', port=5432, dbname='devdb',
                            user='devuser', password='devuser123')
    try:
        cur = conn.cursor()
        cur.execute('SELECT permission_code FROM sys_permission')
        codes = {r[0] for r in cur.fetchall()}
        cur.close()
    finally:
        conn.close()
    DB_CODE_CACHE['codes'] = codes
    return codes


DB_CODE_CACHE = {}


def apply_annotations(module, check_db=True):
    """把 @SaCheckPermission("码") 插到各映射注解之前（幂等 + 铁律校验）。

    为什么用脚本而不是手改：端点→码的映射是**同一套规则**推导出来的，
    手改 7 个文件（后续还有 120+ 个）必然出现漏改/改错，而漏改是静默失效、改错是 403。
    脚本逐行处理，并**显式报告**每一处跳过/无法判定/码不在库，不静默放过。

    `check_db=True`（默认）：推导出的码**必须在库**才插注解，否则跳过并计入 missing。
    """
    cfg = MODULES[module]
    changed, skipped, unresolved, missing = [], [], [], []
    known = load_db_codes() if check_db else None
    for fp in iter_controllers(cfg['dirs']):
        name = os.path.basename(fp)
        text = open(fp, encoding='utf-8').read()
        if not REST.search(text):
            continue
        m = CLS_RM.search(text)
        if not m:
            continue
        base = m.group(1).rstrip('/')
        lines = text.splitlines(keepends=True)
        # 类级已有**权限类**注解 ⇒ 整类已覆盖，不动。
        # ⚠️ 2026-09-21 修正：此处原用 ANY_AUTH（把 `@SaCheckLogin` 也算"已覆盖"），
        #    后果是**类上只写了 `@SaCheckLogin` 的控制器被整片跳过** ——
        #    但 `@SaCheckLogin` 的语义只是"登录即可访问"，正是 E-01 要补的缺口
        #    （`scan-unguarded-controllers.py` 会把它标成「仅 @SaCheckLogin」）。
        #    实测踩到：`UnifiedPurchaseDocQueryController` 因此被跳过，
        #    其 `/api/purchase/doc-query/page` 对非超管仍然 200。
        #    改为只看 PERM_ONLY，与上面"方法级幂等"用同一个判据。
        if PERM_ONLY.search(strip_comments(text.split('class ')[0])):
            skipped.append((name, '类级已有权限注解'))
            continue
        # 按配置整体排除（登录端点 / 无会话的机器对机器接口），见 is_skipped
        if base in (cfg.get('skip_bases') or []):
            skipped.append((name, f'按配置排除（{base}：无用户会话或未登录必须可达）'))
            continue

        out, i, hits = [], 0, 0
        while i < len(lines):
            line = lines[i]
            mm = MAP_LINE.match(line)
            if not mm:
                out.append(line)
                i += 1
                continue
            indent, verb, rest = mm.group(1), mm.group(2).upper(), mm.group(3)
            # 路径可能写在同一行，也可能换行写在下一行
            pm = re.search(r'["\']([^"\']*)["\']', rest)
            path = pm.group(1) if pm else ''
            if not pm and i + 1 < len(lines):
                # 同 scan()：下一行须以引号开头才认（避免把 @Parameter 的描述当路径）
                if lines[i + 1].strip()[:1] in ('"', "'"):
                    pm2 = re.search(r'["\']([^"\']*)["\']', lines[i + 1])
                    if pm2:
                        path = pm2.group(1)
            # 按配置排除单方法（如 PDA 的 /auth/login、/auth/logout）
            if is_skipped(cfg, base, verb, path):
                skipped.append((name, f'{verb} {base}{path}'))
                out.append(line)
                i += 1
                continue
            # 已在该方法上有注解 ⇒ 幂等跳过。
            # ⚠️ 必须**同时往前往后看**，只看前一行会漏判并**重复插入同一条注解**。
            # 本仓两种写法都有：`@Operation` / `@SaCheckPermission` / `@GetMapping`（注解在映射前）
            # 和 `@Operation` / `@PostMapping` / `@SaCheckPermission`（注解在映射后）。
            # 2026-09-21 在销售域实测踩到：只回溯前几行时，后一种写法会被判成"没注解"，
            # 一次 --apply 就在 SaleOrderController 里插出重复注解。
            # 双向窗口都以「空行 / 方法体边界」为终止，避免把相邻方法的注解当成本方法的。
            window = []
            for prev in reversed(out[-6:]):
                s = prev.strip()
                if s == '' or s.startswith('}') or s.startswith('{'):
                    break
                window.append(s)
            for nxt in lines[i + 1:i + 4]:
                s = nxt.strip()
                if s == '' or s.startswith('}') or s.startswith('{'):
                    break
                window.append(s)
            if PERM_ONLY.search(strip_comments('\n'.join(window))):
                out.append(line)
                i += 1
                continue
            if path is None:
                unresolved.append((name, f'{verb} {rest.strip()}'))
                out.append(line)
                i += 1
                continue
            code = code_of(base, verb, path, cfg['domain'], cfg)
            if known is not None and code not in known:
                # 铁律：码不在库 ⇒ 绝不能插注解（插了就是该接口对所有非超管 403）
                missing.append((name, code, f'{verb} {base}{path}'))
                out.append(line)
                i += 1
                continue
            out.append(f'{indent}@SaCheckPermission("{code}")\n')
            out.append(line)
            hits += 1
            i += 1

        if hits:
            new = ''.join(out)
            if 'import cn.dev33.satoken.annotation.SaCheckPermission;' not in new:
                # 插到最后一个 import 之后，保持 import 块连续
                nl = new.splitlines(keepends=True)
                last = max(idx for idx, l in enumerate(nl) if l.startswith('import '))
                nl.insert(last + 1, IMPORT_LINE)
                new = ''.join(nl)
            open(fp, 'w', encoding='utf-8', newline='\n').write(new)
            changed.append((name, hits))
    return changed, skipped, unresolved, missing


def main():
    args = sys.argv[1:]
    if not args or args[0] == '--list':
        for k, v in MODULES.items():
            flag = ' (manual_only)' if v.get('manual_only') else ''
            print(f"{k:12s} {v['label']:6s} domain={v['domain']:12s} slot={v.get('slot')}{flag}")
        return
    module = args[0]
    if module not in MODULES:
        print(f'未知模块: {module}（用 --list 查看）', file=sys.stderr)
        sys.exit(2)

    cfg = MODULES[module]

    if '--apply' in args:
        check_db = '--no-db-check' not in args
        changed, skipped, unresolved, missing = apply_annotations(module, check_db=check_db)
        print(f"═══ {cfg['label']}（{module}）注解写入 ═══\n")
        for name, n in changed:
            print(f"  ✅ {name:42s} +{n} 处 @SaCheckPermission")
        for name, why in skipped:
            print(f"  ⏭  {name:42s} 跳过（{why}）")
        for name, what in unresolved:
            print(f"  ⚠️  {name:42s} 无法判定路径：{what}")
        if missing:
            print(f"\n  🛑 {len(missing)} 处**码不在库**，已按铁律跳过（未插注解）：")
            seen = set()
            for name, code, ep in missing:
                if code in seen:
                    continue
                seen.add(code)
                print(f"     {code:44s} ← {name} {ep}")
            print('     ⇒ 要么补种子迁移把这些码建出来，要么说明推导规则与历史码不一致（需人工定夺）')
        print(f"\n改动 {len(changed)} 个文件 / 新增 {sum(n for _, n in changed)} 处注解"
              + (f"；跳过（码不在库）{len(missing)} 处" if missing else ""))
        if unresolved or missing:
            print('⚠️ 有未处理项（无法判定路径 / 码不在库），请人工处理后重跑', file=sys.stderr)
            sys.exit(3)
        return

    rows, codes = build(module)
    want_sql = '--sql' in args

    if not want_sql:
        print(f"═══ {cfg['label']}（{module}）端点 → 权限码 ═══\n")
        cur = None
        for r in rows:
            if r['resource'] != cur:
                cur = r['resource']
                print(f"\n── 资源 {cur}（{r['base']}）──")
            print(f"  {r['endpoint']:60s} → {r['code']}")
        print(f"\n端点 {len(rows)} 个 → 权限码 {len(codes)} 个")
        print("\n动作分布：", end='')
        dist = {}
        for c in codes:
            a = c.rsplit(':', 1)[1]
            dist[a] = dist.get(a, 0) + 1
        print(', '.join(f'{k}×{v}' for k, v in sorted(dist.items(), key=lambda x: -x[1])))
        return

    # ── 生成 SQL ──
    lines = []
    lines.append(f"-- {cfg['label']}域权限码种子（E-04，2026-09-21）")
    lines.append('--')
    lines.append(f"-- 【为什么】{module} 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器")
    lines.append('--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**')
    lines.append('--   （本仓历史事故，铁律：先补种子后补注解）。')
    lines.append('--')
    lines.append('-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController')
    lines.append('--   16 端点 → 8 码）。本批：' + f'{len(rows)} 端点 → {len(codes)} 码。')
    lines.append('--')
    perm_base = PERM_SLOT_BASE + cfg['slot'] * 1000
    rp_base = RP_SLOT_BASE + cfg['slot'] * 10000
    lines.append('-- 【id 号段】权限码 %d 起（本模块独占槽位 slot=%d）、角色关联 %d 起。' % (perm_base, cfg['slot'], rp_base))
    lines.append('--   9xxxx 段已被 V11.42x 系列的短块占满，故另开 100000 段；')
    lines.append('--   各模块用不同槽位避免多迁移撞主键（本仓 sys_permission.id 无序列默认值）。')
    lines.append('--')
    lines.append('-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。')
    lines.append('-- =============================================================')
    lines.append('')
    # 用 INSERT ... SELECT ... WHERE NOT EXISTS 而非 VALUES：
    # `sys_permission.permission_code` **没有唯一约束**，若某个生成码恰好已存在
    # （实测 486 个里有 1 个：`stock:list`），VALUES 会插入**同码不同 id 的第二行**，
    # 授权矩阵里出现两条同名项、后端取码行为依赖行序 —— 静默的脏数据。
    lines.append('INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,')
    lines.append('                            permission_name, permission_code, permission_type,')
    lines.append('                            api_path, method, sort, visible, status)')
    lines.append('SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0')
    lines.append('FROM (VALUES')
    vals = []
    order = sorted(codes.keys())
    for i, code in enumerate(order):
        parts = code.split(':')
        dom, act = parts[0], parts[-1]
        res = '-'.join(parts[1:-1])
        name = label_for(module, res, act)
        # api_path/method 顺带落地（对应 E-03「api_path 只填 238/474」）：
        # 一个码可能带多个端点，取扫描到的第一个作为代表路径。
        rep = codes[code][0]
        api = rep['base'] + rep['path']
        vals.append(f" ({perm_base + i}, '{name}', '{code}', '{api}', '{rep['verb']}', {SORT_BASE + i})")
    lines.append(',\n'.join(vals))
    lines.append(') AS v(id, name, code, api, method, sort)')
    lines.append('WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);')
    lines.append('')
    lines.append('INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)')
    lines.append('SELECT %d + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()' % rp_base)
    lines.append('FROM sys_permission p')
    # 只按 id 区间圈定本批（域随控制器而不同，不能再按域前缀 LIKE）
    lines.append(f'WHERE p.id BETWEEN {perm_base} AND {perm_base + len(order)}')
    lines.append('  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp')
    lines.append('                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);')
    print('\n'.join(lines))
    print(f"\n-- 统计：端点 {len(rows)} / 权限码 {len(codes)}", file=sys.stderr)


if __name__ == '__main__':
    main()
