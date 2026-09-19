# 系统模块 · 开发文档集

> **对应系统**：来肯企汇 ql361 v2.2（`22stable.ql361.com`）**没有这个模块** —— ql361 是**租户级**进销存系统，它是被"平台方"托管的那个产品，**不会把自己的 SaaS 控制台给客户看**。
> **本模块口径**：系统 = **平台级的设置与管理**（`client_type = system-admin`）—— 即"**平台运营方**怎么管所有租户"。这就是它与《设置模块》（租户级、`client_type = tenant-admin`）的根本区别。
> **撰写口径**：按**业界成熟生产级多租户 SaaS 平台**（SAP BTP、Odoo/Odoo.sh、金蝶云苍穹、用友 YonSuite、AWS SaaS Lens、jeecg-boot）的标准能力建模，**不照搬任何一家界面**。
> **事实来源**：① 本系统源码（引用必须带 `文件:行号`）；② devdb 实测（`sys_menu` 菜单树、`sys_*` 表结构与行数，2026-09-18 直连查询）；③ 业界官方文档（标题 + URL + 章节 + 查阅日期 + **可信度级别**）。
> **业界对标底稿**：`docs/Yh-Spec/抓取结果/业界对标-系统模块-20260918.md`（1090 行，30 页逐页 + 9 个专题 + 未佐证清单）。
> **通用规范**：见《ql361对标/对标开发技术参考文档》第 4、5 章；布局金标准施工手册见《交易模块/_开发指南-金标准.md》。
> **模块定位一句话**：系统模块 = **平台运营方的控制面（Control Plane）** —— 它的核心资产是三本账：**租户目录（tenant catalog）＋ 权益与配额账本（entitlement & quota ledger）＋ 运营审计流水（audit trail）**；其余页面都是这三本账的读写界面或衍生视图（监控 / 日志 / 备份 / 任务）。

---

## 1. 模块定位与边界（最重要的一节，先读这里）

### 1.1 「系统」与「设置」的边界（**本模块最容易搞错的地方**）

本系统菜单按 `sys_menu.client_type` 分成**两套互不可见的域**。**你日常登录看到的「设置」菜单，是租户级的那一套；「系统」这一套在普通租户登录时根本不在菜单树里。**

| | **设置模块**（`../设置模块/README.md`） | **系统模块**（本文档集） |
|---|---|---|
| 顶级菜单 | `60012 设置`（`mega:settings`, sort=1500） | `60013 系统`（`mega:system`, sort=1600） |
| `client_type` | **`tenant-admin`** | **`system-admin`** |
| 登录可见性 | **租户管理员可见** | **普通租户登录：不可见**（菜单树里不出现，路由不生成） |
| 使用者 | 租户自己（客户的 IT/管理员） | **平台运营方**（软件厂商/服务商自己） |
| 管什么 | **租户自己怎么用系统** | **平台方怎么管所有租户** |
| 作用域 | 单租户内（`tenant_id = 当前租户`） | **跨租户**（`tenant_id = 0` 全局，或指定租户） |
| 数据豁免 | 走全局多租户插件（自动注入 `tenant_id`） | **平台级表必须显式豁免多租户过滤**（见 §1.3 红线②） |
| ql361 对标 | ✅ 有（4 分组 13 页，已实测） | ❌ **无**（ql361 是租户级产品） |

> **⚠️ 一句话记住区别**：
> - **设置 → 菜单配置**（80620）= **租户**改自己**已授权菜单的显隐**；
> - **系统 → 菜单管理**（6130701）= **平台**定义**全局菜单树 + 权限码 + 路由/组件**。
> - **设置 → 系统参数**（80621）= **租户级**开关与枚举；
> - **系统 → 平台参数**（62501）= **平台级**全局键值。

### 1.2 本模块的 7 个分组各管什么

```
系统
├─ 租户管理   控制面的核心：租户目录（列表/审批）+ 权益账本（套餐/模块授权）+ 配额账本（配额）
├─ 模块管理   平台自身的功能模块：模块目录 / 版本 / 发布 / 使用统计
├─ 系统监控   平台运行观测：服务状态 / 性能 / 接口 / 日志 / 审计 / 缓存
├─ 数据管理   平台数据基础设施：连接 / 慢查询 / 备份 / 同步 / 清理
├─ 开发工具   平台研发支撑：模板 / API 文档 / API 测试 / 定时任务
├─ 平台设置   平台级参数：参数 / 邮件 / 短信 / 存储 / 安全策略
└─ 系统管理   平台菜单与权限定义：菜单管理
```

### 1.3 三条红线（平台级模块独有，触犯即返工）

**① 平台级菜单**不得**混进租户套餐。**
jeecg 官方文档明确「系统用户、系统角色菜单，这个是给超级管理员用的，**不做租户隔离**」（A/B 级，`https://help.jeecg.com/java/saas/open/`）。把平台菜单挂进 `sys_tenant_menu` → 租户能看到平台菜单。→ 本系统实测 `sys_tenant_menu` 全表仅 4 行且 `tenant_id` 全为 0，**未包含任何系统模块菜单**，这一点是对的，**开发时不要破坏**。

**② 平台级数据必须显式豁免多租户过滤 —— 但「超管会话豁免」已生效，别把它当不存在。**
`sys_tenant_package`(3 行 `tenant_id=0`)、`sys_tenant_menu`(4 行全 0)、`sys_module`(6 行全 0)、`sys_module_version`(12 行全 0)、`sys_mail_config`、`sys_sms_config`、`sys_storage_config`、`sys_security_policy` 等表**都不在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 白名单**。

**但这不等于"超管看不到数据"** —— 存在一条**会话级整体豁免**（2026-09-18 实测确认，两个调用点都在）：
- **写入点**：登录时 `SysUserServiceImpl.java:88` → `StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"))`；
- **读取点**：`AiReadyTenantLineInnerInterceptor.shouldSkip()`（`:41-44`）→ `getCurrentTenantIdValue() == null || MyBatisPlusConfig.isTenantScopeExempt()`（`MyBatisPlusConfig.java:157-166`）→ 豁免时**直接 return，不注入 `tenant_id`**。

**因此准确的结论是**：
| 账号 | 平台级表（不在白名单）的查询结果 |
|---|---|
| **超管（`SUPER_ADMIN`）且登录后拿到新 token** | ✅ **能查到全部租户的数据**（豁免生效） |
| **超管但用的是"改动前已签发的旧 token"** | ❌ 被判为不豁免 → 按会话租户收敛 → **查回 0 行**（`MybatisPlusConfig.java:150-152` 注释明写"需**重新登录一次**即可恢复全局视野"） |
| **非超管账号**（`SYSTEM_ADMIN`/`DEPT_ADMIN` 等） | ❌ 被收敛到自己的会话租户 → **查回 0 行**，且**不报错** |

> **⚠️ 验收必须写死"用哪个账号 + 是否重新登录"**：同一页面，超管重登后可能正常、旧 token 下就空表 —— 这是"页面空白而非报错"类缺陷的根源。§10 要加一条「**超管（重登后）登录时本表能查到 ≥ N 行**」的专门断言（N = 已实测行数）。
> **修法（新写平台级查询时仍推荐显式声明，不依赖会话豁免）**：读全局用 `@InterceptorIgnore(tenantLine = "true")`；写目标租户行前 `MyBatisPlusConfig.setTempTenantId(tid)`（见《配置落位总则》与记忆中的「全局默认行被租户插件过滤」口径）。**理由**：会话豁免依赖 Sa-Token Session，对**定时任务/无会话上下文**的链路无效（那时 `getCurrentTenantIdValue() == null` 也会 skip，但语义不同），显式声明更稳。

**③ 平台侧写操作必须有审计。**
"谁在什么时候从哪个 IP 给哪个租户开了哪个模块"—— 这类问题**事后无法从业务表还原**，只能靠审计流水。SAP SAL 的 6 类事件里一半是"权限/配置/主体变更"（B 级，`https://help.sap.com/` 的《The Security Audit Log》）。本系统 `sys_audit_log` 实测 **0 行**（见 §3.3），等于**平台侧目前无审计**。

---

## 2. 模块菜单结构与文档清单（`sys_menu` 实测 2026-09-18）

### 2.1 菜单树（devdb 直查，递归展开；`deleted=0`）

```
60013  系统                                 (mega:system, sort=1600, client_type=system-admin)
├─ 61301  租户管理                          (mega:sys:tenant, sort=100)
│   ├─ 62001  租户列表   system:tenant:list        单入口  admin/tenant/list
│   ├─ 62002  租户审批   system:tenant:approve     单入口  admin/tenant/approval
│   ├─ 62003  租户套餐   system:tenant:package     单入口  admin/tenant/package
│   ├─ 62004  模块授权   system:tenant:module-auth 单入口  admin/tenant/module-auth
│   └─ 62005  配额管理   system:tenant:quota       单入口  admin/tenant/quota
├─ 61302  模块管理                          (mega:sys:module, sort=200)
│   ├─ 62101  模块列表   system:module:list        单入口  admin/module/list
│   ├─ 62102  模块版本   system:module:version     单入口  admin/module/version
│   ├─ 62103  模块发布   system:module:release     单入口  admin/module/release
│   └─ 62104  使用统计   system:module:usage       单入口  admin/module/usage
├─ 61303  系统监控                          (mega:sys:monitor, sort=300)
│   ├─ 62201  服务状态   system:monitor:health     单入口  admin/monitor/health
│   ├─ 62202  性能监控   system:monitor:performance 单入口 admin/monitor/performance
│   ├─ 62203  接口监控   system:monitor:api        单入口  admin/monitor/api
│   ├─ 62204  系统日志   system:monitor:log        单入口  admin/monitor/log
│   ├─ 62205  操作审计   system:monitor:audit      单入口  admin/monitor/audit
│   └─ 62206  缓存管理   system:monitor:cache      单入口  admin/monitor/cache
├─ 61304  数据管理                          (mega:sys:data, sort=400)
│   ├─ 62301  连接管理   system:data:connection    单入口  admin/data/connection
│   ├─ 62302  慢查询     system:data:slow-query    单入口  admin/data/slow-query
│   ├─ 62303  备份管理   system:data:backup        单入口  admin/data/backup
│   ├─ 62304  同步任务   system:data:sync          单入口  admin/data/sync
│   └─ 62305  清理规则   system:data:cleanup       单入口  admin/data/cleanup
├─ 61305  开发工具                          (mega:sys:devtools, sort=500)
│   ├─ 62402  模板管理   system:dev:template       单入口  admin/dev/template
│   ├─ 62403  API文档    system:dev:api-doc        单入口  admin/dev/api-doc
│   ├─ 62404  API测试    system:dev:api-test       单入口  admin/dev/api-test
│   └─ 62405  定时任务   system:dev:scheduler      单入口  admin/dev/scheduler
├─ 61306  平台设置                          (mega:sys:platform, sort=600)
│   ├─ 62501  平台参数   system:platform:params    单入口  admin/platform/params
│   ├─ 62502  邮件配置   system:platform:mail      单入口  admin/platform/mail
│   ├─ 62503  短信配置   system:platform:sms       单入口  admin/platform/sms
│   ├─ 62504  存储配置   system:platform:storage   单入口  admin/platform/storage
│   └─ 62505  安全策略   system:platform:security  单入口  admin/platform/security
└─ 61307  系统管理                          (mega:sys:admin, sort=700)
    └─ 6130701  菜单管理  system:menu              单入口  system/menu/index.vue  ⚠️ client_type=tenant-admin
```

**菜单数**：1 个顶级 + 7 个分组 + **30 个页面项**。

**⚠️ 三处菜单结构问题**：

| # | 问题 | 实测 |
|:-:|------|------|
| 1 | **`61305 开发工具` 的 `sort=100` 位置空缺**（`62401` 不存在） | **「代码生成」页被删过两次**：`V6.13.0` 播种（sort=100）→ `V6.15.0` 物理 `DELETE`（理由"确认不开发"）→ `V6.23.0` 以 `id=90002` 挂回 → `V9.5.0` 二次 `DELETE` 并把 `90002` 让给 HR 的「考勤记录」。**连带**：`V6.23.0` 同批补的另两页（`90001 错误统计` 61302 / `90003 数据字典` 61306）**同批被删且 ID 被 HR 复用**。→ 这是"曾经规划过、后被判定不做"的空洞，**不是遗漏**。 |
| 2 | **`6130701 菜单管理` 的 `client_type = 'tenant-admin'`**，却挂在 `system-admin` 的分组 `61307` 下；且它是 `sys_menu` 中**唯一 `tenant_id ≠ 0` 的行**（实测 `tenant_id` 分布 `0 → 380`、`1 → 1`） | 详细影响见 §2.4 |
| 3 | **`61307 系统管理` 组只有 1 个子节点** | 其余"用户/角色/部门/岗位/字典/权限"等平台管理页**都在 `views/system/` 下存在文件却没有菜单**，见 §2.5 |

### 2.2 页面 ↔ 文档对照表（**30 篇，一字不差一一对应**）

| 分组 | 页面 | 菜单ID | menu_code | 路由 | 组件（相对 `views/`） | 文档 |
|------|------|:-----:|-----------|------|------|------|
| 租户管理 | 租户列表 | 62001 | `system:tenant:list` | `admin/tenant/list` | `admin/tenant/list/index.vue` | [租户列表开发文档](./租户列表开发文档.md) |
| | 租户审批 | 62002 | `system:tenant:approve` | `admin/tenant/approval` | `admin/tenant/approval/index.vue` | [租户审批开发文档](./租户审批开发文档.md) |
| | 租户套餐 | 62003 | `system:tenant:package` | `admin/tenant/package` | `admin/tenant/package/index.vue` | [租户套餐开发文档](./租户套餐开发文档.md) |
| | 模块授权 | 62004 | `system:tenant:module-auth` | `admin/tenant/module-auth` | `admin/tenant/module-auth/index.vue` | [模块授权开发文档](./模块授权开发文档.md) |
| | 配额管理 | 62005 | `system:tenant:quota` | `admin/tenant/quota` | `admin/tenant/quota/index.vue` | [配额管理开发文档](./配额管理开发文档.md) |
| 模块管理 | 模块列表 | 62101 | `system:module:list` | `admin/module/list` | `admin/module/list/index.vue` | [模块列表开发文档](./模块列表开发文档.md) |
| | 模块版本 | 62102 | `system:module:version` | `admin/module/version` | `admin/module/version/index.vue` | [模块版本开发文档](./模块版本开发文档.md) |
| | 模块发布 | 62103 | `system:module:release` | `admin/module/release` | `admin/module/release/index.vue` | [模块发布开发文档](./模块发布开发文档.md) |
| | 使用统计 | 62104 | `system:module:usage` | `admin/module/usage` | `admin/module/usage/index.vue` | [使用统计开发文档](./使用统计开发文档.md) |
| 系统监控 | 服务状态 | 62201 | `system:monitor:health` | `admin/monitor/health` | `admin/monitor/health/index.vue` | [服务状态开发文档](./服务状态开发文档.md) |
| | 性能监控 | 62202 | `system:monitor:performance` | `admin/monitor/performance` | `admin/monitor/performance/index.vue` | [性能监控开发文档](./性能监控开发文档.md) |
| | 接口监控 | 62203 | `system:monitor:api` | `admin/monitor/api` | `admin/monitor/api/index.vue` | [接口监控开发文档](./接口监控开发文档.md) |
| | 系统日志 | 62204 | `system:monitor:log` | `admin/monitor/log` | **`system/log/index.vue`**（别名，见 §2.5） | [系统日志开发文档](./系统日志开发文档.md) |
| | 操作审计 | 62205 | `system:monitor:audit` | `admin/monitor/audit` | `admin/monitor/audit/index.vue` | [操作审计开发文档](./操作审计开发文档.md) |
| | 缓存管理 | 62206 | `system:monitor:cache` | `admin/monitor/cache` | `admin/monitor/cache/index.vue` | [缓存管理开发文档](./缓存管理开发文档.md) |
| 数据管理 | 连接管理 | 62301 | `system:data:connection` | `admin/data/connection` | `admin/data/connection/index.vue` | [连接管理开发文档](./连接管理开发文档.md) |
| | 慢查询 | 62302 | `system:data:slow-query` | `admin/data/slow-query` | `admin/data/slow-query/index.vue` | [慢查询开发文档](./慢查询开发文档.md) |
| | 备份管理 | 62303 | `system:data:backup` | `admin/data/backup` | `admin/data/backup/index.vue` | [备份管理开发文档](./备份管理开发文档.md) |
| | 同步任务 | 62304 | `system:data:sync` | `admin/data/sync` | `admin/data/sync/index.vue` | [同步任务开发文档](./同步任务开发文档.md) |
| | 清理规则 | 62305 | `system:data:cleanup` | `admin/data/cleanup` | `admin/data/cleanup/index.vue` | [清理规则开发文档](./清理规则开发文档.md) |
| 开发工具 | 模板管理 | 62402 | `system:dev:template` | `admin/dev/template` | `admin/dev/template/index.vue` | [模板管理开发文档](./模板管理开发文档.md) |
| | API文档 | 62403 | `system:dev:api-doc` | `admin/dev/api-doc` | `admin/dev/api-doc/index.vue` | [API文档开发文档](./API文档开发文档.md) |
| | API测试 | 62404 | `system:dev:api-test` | `admin/dev/api-test` | `admin/dev/api-test/index.vue` | [API测试开发文档](./API测试开发文档.md) |
| | 定时任务 | 62405 | `system:dev:scheduler` | `admin/dev/scheduler` | `admin/dev/scheduler/index.vue` | [定时任务开发文档](./定时任务开发文档.md) |
| 平台设置 | 平台参数 | 62501 | `system:platform:params` | `admin/platform/params` | `admin/platform/params/index.vue` | [平台参数开发文档](./平台参数开发文档.md) |
| | 邮件配置 | 62502 | `system:platform:mail` | `admin/platform/mail` | `admin/platform/mail/index.vue` | [邮件配置开发文档](./邮件配置开发文档.md) |
| | 短信配置 | 62503 | `system:platform:sms` | `admin/platform/sms` | `admin/platform/sms/index.vue` | [短信配置开发文档](./短信配置开发文档.md) |
| | 存储配置 | 62504 | `system:platform:storage` | `admin/platform/storage` | `admin/platform/storage/index.vue` | [存储配置开发文档](./存储配置开发文档.md) |
| | 安全策略 | 62505 | `system:platform:security` | `admin/platform/security` | `admin/platform/security/index.vue` | [安全策略开发文档](./安全策略开发文档.md) |
| 系统管理 | 菜单管理 | 6130701 | `system:menu` | `system/menu/index` | `system/menu/index.vue` | [菜单管理开发文档](./菜单管理开发文档.md) |

### 2.3 后端控制器落位（**根因级 P0：5 个包漏配 `scanBasePackages`，19 个控制器 / 126 个端点从未装配**）

`backend/core/api/core-api/src/main/java/cn/aiedge/AiReadyApplication.java:21-96` 的 `scanBasePackages` **不包含**下列 5 个包（2026-09-18 复核，`grep -c "\"cn.aiedge.<pkg>"` 命中数**均为 0**）：

```java
// AiReadyApplication.java:21 起（节选）
@SpringBootApplication(scanBasePackages = {
    "cn.aiedge.base", "cn.aiedge.cache", "cn.aiedge.common", "cn.aiedge.config",
    "cn.aiedge.api", ..., "cn.aiedge.tenant", ...
    // ❌ 缺 "cn.aiedge.module"      → 1 控制器 / 10 端点
    // ❌ 缺 "cn.aiedge.monitor"     → 5 控制器 / 52 端点
    // ❌ 缺 "cn.aiedge.platform"    → 4 控制器 / 11 端点
    // ❌ 缺 "cn.aiedge.datasource"  → 5 控制器 / 22 端点
    // ❌ 缺 "cn.aiedge.export"      → 4 控制器 / 31 端点   ← 本轮新查出（此前误记为"已装配"）
    "cn.aiedge.scheduler",   // ✅ 已于 2026-09-14 补配（见文件内注释）
```

| 未装配包 | 控制器数 | 端点数 | 控制器清单 |
|---|:---:|:---:|---|
| `cn.aiedge.module` | 1 | **10** | `ModuleController` |
| `cn.aiedge.monitor` | 5 | **52** | `AlertManagement`、`HealthMonitor`、`InfrastructureMonitor`、`PerformanceMetrics`、`SystemMonitor` |
| `cn.aiedge.platform` | 4 | **11** | `MailConfig`、`SecurityPolicy`、`SmsConfig`、`StorageConfig` |
| `cn.aiedge.datasource` | 5 | **22** | `Backup`、`CleanupRule`、`DataSource`、`SlowQuery`、`SyncTask` |
| `cn.aiedge.export` | 4 | **31** | `BatchImport`、`DataExport`、`DataImport`、`ImportTemplate` |
| **合计** | **19** | **126** | — |

**受影响页面（本模块 15 页）**：

| 受影响分组 | 页面数 | 受影响包 | 端点数 |
|---|:---:|---|:---:|
| 模块管理 | **4**（62101-62104） | `module` | 10 |
| 系统监控 | **2**（62201 服务状态 / 62202 性能监控） | `monitor` | 52（本组只用其中 13） |
| 平台设置 | **4**（62502-62505 邮件/短信/存储/安全策略） | `platform` | 11 |
| 数据管理 | **5**（62301-62305） | `datasource` | 22 |
| 开发工具 | **1**（62402 模板管理） | `export` | 31（本页只用其中 13） |
| **合计** | **16** —— 注：62001-62005 租户管理 5 页与 62501 平台参数走 `cn.aiedge.tenant` / `cn.aiedge.config` / `cn.aiedge.base`（**已装配**），不受此影响 | — | — |

> **⚠️ 跨模块外溢**：`cn.aiedge.export` 的 `DataImport`/`DataExport`/`BatchImport` 三个控制器（31 端点）**不止服务本模块的「模板管理」** —— 它们还是**全站 Excel 导入/导出**的后端。该包未装配意味着**其它模块的导入导出链路也在运行期 404**（见记忆《Excel导入与antd下拉两个跨模块陷阱》：`/import/v2/excel/*` 是假导入，只校验不落库）。**这一条须单独立项核查，不在本模块文档集范围内。**
> **⚠️ 这不是"配置疏漏"这么简单**：`@MapperScan("cn.aiedge.**.mapper")` 是**通配**的（`AiReadyApplication.java:117`），所以这些模块的 Mapper 会被注册，但 **Service/Controller 不会** —— 表现为「**表能建、点不动**」的假可用状态。
> **修法**：把上述 5 个包补进 `scanBasePackages`（并**穷尽核查其余未列出的 `cn.aiedge.*` 包**，本模块只查了与自己相关的），再按 §1.3 红线②处理多租户豁免。

### 2.4 `6130701 菜单管理` 的 `client_type` / `tenant_id` 异常（**已查透**）

**事实**：`6130701` 是 `sys_menu` 中**唯一 `tenant_id=1` 的行**，`client_type='tenant-admin'`，挂 `system-admin` 的父节点 `61307` 下。

**后端菜单树过滤链**（`core-base/.../base/service/impl/SysMenuServiceImpl.java`）：

| 场景 | 是否出现「菜单管理」 | 判定依据（行号） |
|---|:---:|---|
| **超管 + 系统租户（tenantId=1）** | **有** | 前端 `dynamicRoutes.ts:7` 固定 `CLIENT_TYPE='tenant-admin'`、`:1150` 请求 `/menu/user/mega/tenant-admin?tenantId=1`；后端 `SysMenuServiceImpl.java:250` 判 `isSystemTenant`、`:255` 判 `isSuperAdmin` → 走 `:259-265` → `getAllMenusForSystemAdmin()`（`:331-339`，Wrapper 为 `client_type IN ('tenant-admin','system-admin')`，**忽略入参**）→ `buildMenuTree(allMenus, 0L)`（`:264`）按 `parent_id` 链 `0 → 60013 → 61307 → 6130701` 组装。**没有任何条件挡掉它。** |
| **非超管 + 系统租户**（`SYSTEM_ADMIN` / `DEPT_ADMIN`） | **没有** | 走 `:266-272`，`finalMenuIds = roleMenuIds`；实测 `sys_role_menu` 中 `menu_id=6130701` **只挂 `role_id=1`（SUPER_ADMIN）** → `:296` 的 `in(getId, finalMenuIds)` 把它排除 |
| **非系统租户任意用户** | **没有** | 走 `:273-287`；`tenantAuthorizedIds = tenantMenuService.getAuthorizedMenuIds(tenantId)`（`:276`）→ `sys_tenant_menu` 全表仅 4 行、`tenant_id` 全 0、**不含 6130701** → `finalMenuIds` 为空 → `:289-292` 返回空数组 |
| **页面自身的表格（`GET /api/menu/tree`）** | **永远不出现** | `SysMenuServiceImpl.java:98` 与 `:206-211` **强制 `eq(tenant_id, 0L)`**；6130701 是唯一 `tenant_id=1` 的行 → **本页无法在本页的表格里编辑/删除它自己** |

> **一句话**：超管**能看到**「菜单管理」（`client_type` 与 `tenant_id` 那两个"异常"在 `:259-265` 分支下都不构成拦截条件），但**它管不到自己**。

### 2.5 孤儿页、幽灵页与重复实现（**本模块最乱的一块，必须如实标注**）

**可达性机制**（判定前提，`frontend/apps/pc-admin/src/router/dynamicRoutes.ts`）：

| 机制 | 位置 | 是否产生路由 |
|---|---|---|
| `componentMap`（含 `system/*` 段） | 定义起 `:49`；`system/*` 条目 `:103-116`；别名单条 `:248` | **否**。只在"数据库给了 component 字符串"时被 `getComponent()`（`:921-948`）查询 |
| `getRequiredRoutes()` | `:1253` 起 | **是，无条件注入**（`:1200` + `:1208`） |
| `getFallbackRoutes()` | `:1730` 起 | **是，但仅在 `menuTree` 为空时**（`:1220-1222`） |

**实测结论**：`SELECT count(*) FROM sys_menu WHERE component LIKE '%system/%'` = **1**（仅 6130701）。14 个候选 `views/system/**` 与 `views/admin/**` 文件的判定：

| 判定 | 数量 | 文件 |
|---|:---:|---|
| **完全不可达** | **8** | `admin/tenant/permissions`(175)、`admin/sys/permissions`(621)、`system/department`(1138)、`system/dict`(1067)、`system/permission`(888)、`system/position`(1301)、`system/tenant`(1124)、`system/tenant-approval`(563) |
| **仅 fallback 可达** | **3** | `system/config`(847)、`system/role`(931)、`system/user`(918)（正常运行时不可达，仅当菜单接口返回空数组时才出现） |
| **无条件可达** | **1** | `system/data-import`(1390)（`getRequiredRoutes`，`:1454-1459`） |
| **经别名复用而可达** | **1** | `system/log`(733)（被 `componentMap:248` 的键 `admin/monitor/log` 复用，由菜单 62204 加载） |
| **有真实 DB 绑定** | **1** | `system/menu`(1641)（6130701） |

**幽灵文件（1 个）**：`views/admin/monitor/log/index.vue`（167 行 / 6174 字节）—— 菜单 62204 的 `component` 是 `views/admin/monitor/log`，归一化后**命中 `componentMap:248`**，实际加载的是 `views/system/log/index.vue`（733 行 / 21529 字节）。→ **`views/admin/monitor/log/index.vue` 永远不会被加载**，且其内唯一接口 `/api/audit-log/page` 后端也不存在。

**重复实现（4 组）**：

| 组 | 两份/三份 | 谁的菜单生效 |
|:-:|---|---|
| **A** | `system/tenant/index.vue`(1124) ↔ `admin/tenant/list/index.vue`(316) | 后者（菜单 62001） |
| **B** | `system/tenant-approval/index.vue`(563) ↔ `admin/tenant/approval/index.vue`(110) | 后者（菜单 62002） |
| **C** | `system/menu/index.vue`(1641) ↔ **`set/menu-config/index.vue`(362)** | **两份都被绑定**（6130701 与 80620）—— 同一功能在**平台侧与租户侧各一份**（见《设置模块/README》§2.4） |
| **D** | `admin/tenant/permissions`(175) ↔ `admin/sys/permissions`(621) ↔ `system/permission`(888) | **三份全是孤儿**（无任何菜单绑定） |

**其它重叠**（详见各页文档与取证 `_重叠与重复B.md`）：
- **接口监控 62203**（走 `/monitor/dashboard`、`/monitor/info`，**全后端零命中**）**vs 交易模块 API 监控**（`views/trade/api-monitor/`，走 `/api/trade/api-monitor/*` 13 端点 + `api_access_log` 表 63 行，记忆记载 E2E 119/119）—— **两套完全不同的实现**，前者是整页桩。
- **定时任务 62405**（真接 `/api/scheduler/task`，15 端点，`scheduled_task` 16 行 + `scheduled_task_log` 15 行）**vs xxl-job 遗留**（`sys_job`/`sys_job_log` 两张表各 0 行，`xxl.job.*` 配置**未在 core-api yml 声明** → 执行器默认关闭）。**当前生效的是前者**。
- **系统日志 62204**（`SysLogStubController` `/api/log` 6 端点，**零权限注解 + 租户硬编码 `DEFAULT_TENANT_ID=1L`**）**vs** 设置模块的 `/api/system/log`（`LogManageController` **13** 端点，权限注解齐全）**vs** 平台侧 `views/system/log/index.vue` 调的是 `/api/log`。

---

## 3. 本模块当前实现现状（2026-09-18 源码核对）

> 本节所有结论来自 `tool-results/docgen/evidence/系统模块/*.md`（30 页逐页 `文件:行号` 取证 + 4 个汇总文件）。凡"预期 404"均为**静态比对结论**（当时本机无监听的后端进程，未做运行期复验）。

### 3.1 页面外壳符合度（**30 页全部未达金标准**）

| 判定 | 页数 | 说明 |
|---|:---:|---|
| 用 `PageContainer`（多数 `full-height`）+ `a-card` + 裸 `a-table` | **28** | 外加各页自绘的统计卡 / 折叠面板 / 表单 |
| 用 `BillTableList` | **1** | 系统日志（62204）—— 但页面实际加载的是 `views/system/log/index.vue`，且其列定义有 3 处插槽缺陷（见 §3.4 P0-14） |
| 用 `a-tabs` | **1** | 系统日志的抽屉内有 Tab |
| 用 `CategoryListLayout` / `DocCenterLayout` | **0** | — |
| 用 `ARReportPage`（路线 B） | **0** | — |
| 用 `StandardPagination`（经典分页） | **0** | — |
| 有 `PageConfigPanel` | **0** | — |
| 有 `storage-key` / `global-config-key` | **0** | — |
| 列定义含 `type` / `defaultHidden` / `slotName` | **极少** | 列数组普遍只有 `title`/`dataIndex`/`width`，**列配置能力整体缺失** |
| `({row})=>` formatter 缺陷模式 | **0 命中** | 因为这些列根本没有 `formatter` |

> **一句话**：系统模块的 30 页，**没有一页达到金标准路线 A**；且因为没有一页用 `BillDetailTable`，**全模块零列配置持久化能力**。

### 3.2 数据真实性分布（**这是本模块最刺眼的一张表**）

| 判定 | 页数 | 页面 |
|---|:---:|---|
| **整页桩**（表格/卡片 100% 前端写死，或后端返回硬编码假数据） | **3** | 接口监控（62203）、缓存管理（62206）、API文档（62403） |
| **部分桩** | **2** | 服务状态（62201）、API测试（62404） |
| **核心能力桩**（页面能用，但关键动作不产生真实效果） | **3** | 备份管理（62303）、同步任务（62304）、清理规则（62305） |
| **真实数据** | **7** | 性能监控（62202）、系统日志（62204）、操作审计（62205）、连接管理（62301）、慢查询（62302）、模板管理（62402）、定时任务（62405） |
| **其余**（结构化表单/配置页，数据结构真实但**后端未装配或路径不匹配**） | **15** | 租户管理 5 + 模块管理 4 + 平台设置 5 + 菜单管理 1 |

> ⚠️ **「真实数据」≠「能用」**：数据管理组 5 页（连接/慢查询/备份/同步/清理）的**表结构是真的、页面逻辑是真的，但 22 个端点因后端未装配而运行期 404** → 用户看到的是**空表**。

### 3.3 后端能力盘点（**普遍"文件在、Bean 不在"**）

| 页面 | 控制器（相对 `BE/`） | 类前缀 | 端点数 | 实际可用 | 权限注解 |
|---|---|---|---|:---:|---|
| 租户列表 | `core/api/.../tenant/controller/TenantController.java` | `/api/tenant` | **9**（`page`/`{id}`/POST/PUT/DELETE/DELETE batch/PATCH status/GET config/PUT config） | ✅ 包已扫描 | 类级 `@SaCheckLogin` + 写端点 `system:tenant:update` |
| 租户审批 | `core/api/.../tenant/controller/TenantRegistrationController.java` | `/api/tenant-registration` | 4 | ⚠️ 前端打的是 `/api/tenant/*` → **前缀不匹配 404** | — |
| 租户套餐 | `core/api/.../tenant/controller/TenantPackageController.java` | `/api/tenant-package` | 5 | ✅ | `platform:tenant-package:*` |
| 模块授权 | `core/base/.../base/controller/SysTenantMenuController.java` | `/api/tenant-menu` | 4 | ✅ | — |
| 配额管理 | `core/api/.../tenant/controller/TenantQuotaController.java` | `/api/tenant-quota` | 5 | ✅ | — |
| 模块列表/版本/发布/使用统计 | `core/api/.../module/controller/ModuleController.java` | `/api/module` | 9 | ❌ **`cn.aiedge.module` 未装配 → 全 404** | — |
| 服务状态 | `core/api/.../monitor/controller/HealthMonitorController.java` | `/api/monitor/health` | 7 | ❌ 包未装配 | 无 |
| 性能监控 | `core/api/.../monitor/controller/PerformanceMetricsController.java` | `/api/monitor/performance` | 6 | ❌ 包未装配 | 无 |
| 接口监控 | **未找到** | — | **0** | ❌ `/api/monitor/dashboard`、`/api/monitor/info` 全后端零命中 | — |
| 系统日志 | `core/api/.../audit/controller/SysLogStubController.java` | `/api/log` | 6 | ✅ | ❌ 全裸 + `DEFAULT_TENANT_ID=1L` |
| 操作审计 | `core/api/.../audit/controller/AuditLogController.java` | `/api/audit` | 8（本页用 2） | ✅ | 无 |
| 缓存管理 | `core/api/.../cache/controller/CacheManageController.java` | `/api/cache` | 5 | ✅（但返假数据） | 无（5 端点全裸） |
| 连接/慢查询/备份/同步/清理 | `core/api/.../datasource/controller/*.java` | `/api/data-source/*` | 6+2+4+5+5 = **22** | ❌ **`cn.aiedge.datasource` 未装配 → 全 404** | 21 个权限码**在 `sys_permission` 中均不存在** |
| 模板管理 | `core/api/.../export/controller/ImportTemplateController.java` | `/api/import-templates` | 13 | ❌ **`cn.aiedge.export` 未装配 → 全 404**（2026-09-18 复核订正：此前误记为 ✅） | 类级 `@SaCheckLogin`，无端点级权限码 |
| API文档 | **未找到** | — | **0** | ❌ | — |
| API测试 | **无专用端点**（走通用 axios 通道） | — | 0 | ❌ **url/method 被 `buildConfig` 静默丢弃** | — |
| 定时任务 | `core/api/.../scheduler/controller/ScheduledTaskController.java` | `/api/scheduler/task` | 15 | ✅ | ✅ **15 个端点全部有 `@SaCheckPermission`**（2026-09-18 复核订正：此前误记为"全无注解"）；权限码 5 条已落库 |
| 平台参数 | `core/api/.../config/controller/SystemConfigController.java` | `/api/config` | 14 | ⚠️ 后端**全程不落库**（内存 11 条） | 部分 `system:config:*` |
| 邮件配置 | `core/api/.../platform/controller/MailConfigController.java` | `/api/mail` | 3 | ❌ `cn.aiedge.platform` 未装配 | — |
| 短信配置 | `core/api/.../platform/controller/SmsConfigController.java` | `/api/sms` | 3 | ❌ 同上 | — |
| 存储配置 | `core/api/.../platform/controller/StorageConfigController.java` | `/api/storage-config` | 3 | ❌ 同上 **且前端打 `/api/storage/*` → 路径也不匹配** | — |
| 安全策略 | `core/api/.../platform/controller/SecurityPolicyController.java` | `/api/system/security/policy` | 2 | ❌ 同上 | — |
| 菜单管理 | `core/base/.../base/controller/SysMenuController.java` | `/api/menu` | 14 | ✅ | `system:menu:*`（**6 个码在 `sys_permission` 中 0 行**） |

**权限码体系与页面全面脱节**（实测）：
- `sys_permission` 中 `system:%` 共 **58 条**，但 `system:tenant:%` / `system:module:%` / `system:platform:%` / **`system:menu%`** 全部 **0 条**；
- 前端引用了 **5 个库中不存在的码**（`system:config:query`、`system:config:create`、`system:user:query`、`system:tenant:approve`、`system:tenant:reject`）；
- 后端注解引用了 **6 个库中不存在的码**（`system:menu:create/update/delete/list/detail/update-status`，`SysMenuController.java:39/53/68/82/136/149/160/171/183`）→ **非超管用户对 `/api/menu/**` 的任何调用都 403**（超管靠 `UnifiedPermissionCacheService.java:84-88` 的 `*` 通配旁路）；
- 「菜单管理」页的**按钮**权限码用 `system:permission:*`（`views/system/menu/index.vue:96/302/312/322/332`），与后端要求的 `system:menu:*` **不同域** → 即使授了 `system:permission:*`，按钮显示、接口照样 403；
- 库里有而 30 页**完全没用**的 `system:%` 码 **43 条**（含整组 `system:dev:scheduler:*`、`system:sod-rule:*`、`system:data-scope:*`、`system:field-permission:*`、`system:role:*`、`system:user:*`）。

### 3.4 P0 缺陷清单（**14 条**，按严重度）

| # | 缺陷 | 影响页面 | 证据 |
|:-:|------|---------|------|
| **P0-1** | **5 个包漏配 `scanBasePackages` → 19 控制器 / 126 端点从未装配，运行期全部 404** | 模块管理 4 + 系统监控 2 + 平台设置 4 + 数据管理 5 + 开发工具 1 = **16 页**（且跨模块外溢到全站导入导出，见 §2.3） | `AiReadyApplication.java:21-96`（5 个包 `grep` 命中均为 0）；端点数实测 |
| **P0-2** | **数据管理组 5 页 22 个端点全 404**（P0-1 的具体化） | 连接 / 慢查询 / 备份 / 同步 / 清理 | 各页 §10 |
| **P0-3** | **接口监控整页桩 + 双端点不存在**（`/api/monitor/dashboard`、`/api/monitor/info` 全后端零命中） | 接口监控 62203 | `views/admin/monitor/api/index.vue:221,251` |
| **P0-4** | **缓存管理后端返回硬编码假数据**（缓存区域/键数量/内存/命中率/TTL/过期键全部写死或公式现生成）；**但 `DELETE /api/cache/all` 会真清空整库 Redis** | 缓存管理 62206 | `CacheManageController.java:28-41,66,139-162`；`views/admin/monitor/cache/index.vue:244-253` |
| **P0-5** | **同步任务「立即执行」是状态桩**：只 `setLastSyncTime + setStatus("running")`，无任何数据搬运；与 `backend/sync-engine/` **零连线**；前端仍弹「同步任务已触发执行」 | 同步任务 62304 | `SyncTaskServiceImpl.java:72-80`；`SyncTaskController.java:88-94`；`sync/index.vue:321` |
| **P0-6** | **清理规则「立即执行」不删任何数据**：只 `setStatus("running")`，无 DELETE/TRUNCATE/JdbcTemplate，`targetTable`/`conditionColumn`/`retentionDays` **不被读取**；前端谎报「清理任务已触发执行」 | 清理规则 62305 | `CleanupRuleServiceImpl.java:72-79`；`cleanup/index.vue:269` |
| **P0-7** | **备份管理核心能力缺失**：「创建备份」只记台账（无 `pg_dump`、无文件落盘、状态永不推进）；**「恢复」是空实现且谎报成功** | 备份管理 62303 | 该页 §12 |
| **P0-8** | **系统日志响应解包缺陷 → 表格 / 2 个下拉 / 3 个统计卡片全部恒空** | 系统日志 62204 | `views/system/log/index.vue`；该页 §12 |
| **P0-9** | **操作审计 4 处字段名与实体错配**（`userName`←`username`、`status`←`result`、`ipAddress`←`operIp`、`createTime`←`operTime`）→ 有数据时**每行都显示红色「失败」** | 操作审计 62205 | `views/admin/monitor/audit/index.vue:251-254`；`AuditLog.java:67,87,72,82` |
| **P0-10** | **定时任务前端 `GET /page` 硬编码 `{current:1,size:100}`**（绕过后端已支持的筛选）<br>~~原记「15 个端点全部无权限注解」~~ —— **2026-09-18 复核不成立**：15 个端点**全部有** `@SaCheckPermission`（`PERM_LIST`/`CREATE`/`UPDATE`/`DELETE`/`EXECUTE`），5 个权限码已落库 | 定时任务 62405 | `scheduler/index.vue:401`；`ScheduledTaskController.java:53-164`（15 处注解） |
| **P0-11** | **API测试页 URL/方法/请求体被 `buildConfig` 静默丢弃**，实际只发 `GET /api`；成功状态码写死 200。**设计上若修好透传，立即变成「带当前用户 token 的任意 URL 请求器」（SSRF + 凭据外泄 + 请求头注入三重风险）** | API测试 62404 | `api-test/index.vue:144-145`；`utils/request.ts:375-400`、`:121-129` |
| **P0-12** | **服务状态页「依赖服务状态」4 项中 3 项是后端硬编码常量**（Redis/MQ 恒 DOWN、外部 API 恒 UP）；「服务名称」「运行状态」前端写死；**4 个接口异常被静默吞掉、空值兜底为「正常」→ 故障时会显示成健康** | 服务状态 62201 | 该页 §12 |
| **P0-13** | **API文档页整页桩**：表格 100% 前端写死；唯一真实分支指向不存在的 `/api/monitor/info`；两个跳转按钮在 dev 环境打开 SPA 自身（`/swagger-ui/**`、`/doc.html` 未被 vite 代理，且 `/swagger-ui/index.html` 不在 Sa-Token 放行清单） | API文档 62403 | `api-doc/index.vue:118-131,136`；`vite.config.ts:109-137`；`SaTokenConfig.java:60-64,100-105` |
| **P0-14** | **系统日志 11 列中第 9/10 列 `slotName` 缺 `type:'slot'`** → 状态显示裸 `0/1`；第 11 列 `type:'action'` 无 `slotName`（应为 `actionCell`）→ **操作列渲染为空白** | 系统日志 62204 | `views/system/log/index.vue:345-347`；`BillDetailTable/index.vue:223,335-337,195-202` |

**另 4 条 P0（平台侧红线，与上表并列）**：

| # | 缺陷 | 证据 |
|:-:|------|------|
| **P0-15** | **平台级表未豁免多租户插件**：8 张平台表均不在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`。**超管会话经 `tenantScopeExempt` 整体豁免（登录时写入，见 §1.3 红线②）→ 超管重登后正常**；但 **① 旧 token 不豁免 ② 非超管账号一律被收敛到 `tenant_id=1`** → 两种情况都**查回 0 行且不报错** | `MyBatisPlusConfig.java:157-166`、`SysUserServiceImpl.java:88`、`AiReadyTenantLineInnerInterceptor.java:41-44`；各页 §7 |
| **P0-16** | **6 个 `system:menu:*` 权限码在 `sys_permission` 中 0 行** → 非超管对 `/api/menu/**` 的任何调用都 403；且本页按钮码用 `system:permission:*`（**不同域**） | §3.3；`SysMenuController.java:39/53/68/82/136/149/160/171/183` |
| **P0-17** | **平台侧无审计**：`sys_audit_log` 实测 **0 行**；`sys_oper_log` 有 13474 行但属租户侧（`SysLogStubController` 租户硬编码为 1）→ **"谁给哪个租户开了哪个模块"无迹可查** | §3.5 表 |
| **P0-18** | **存储配置整页死**：前端打 `/api/storage/*`，后端是 `/api/storage-config/*` → 路径不匹配 + 包未装配，**双保险不可用** | `views/admin/platform/storage/index.vue`；`StorageConfigController.java` |

### 3.5 关键表实测（psql，2026-09-18）

| 表名 | 列数 | 行数 | 归属页面 / 说明 |
|---|---|---:|---|
| `sys_tenant` | 15 | **3** | 租户列表 / 租户审批 / 企业信息（设置侧） |
| `sys_tenant_package` | 17 | **3** | 租户套餐（**行 `tenant_id=0`**） |
| `sys_tenant_module` | 12 | **0** | 应用中心（设置侧）/ 模块授权 |
| `sys_tenant_menu` | — | **4**（`tenant_id` 全 0） | 模块授权 |
| `sys_tenant_quota` | — | **3** | 配额管理 |
| `sys_module` | — | **6**（`tenant_id` 全 0） | 模块列表 / 使用统计 |
| `sys_module_version` | — | **12**（`tenant_id` 全 0） | 模块版本 / 模块发布 |
| `sys_mail_config` | — | **2** | 邮件配置 |
| `sys_sms_config` | — | **2** | 短信配置 |
| `sys_storage_config` | — | **3** | 存储配置 |
| `sys_security_policy` | — | **2** | 安全策略 |
| `sys_config` | 17 | **8** | **孤儿表**（无 Java 载体；平台参数页读的是内存） |
| `sys_oper_log` | 18 | **13474** | 系统日志（生效页 `views/system/log/index.vue`） |
| `sys_audit_log` | 24 | **0** | 操作审计 |
| `sys_system_log` | 26 | **0** | （无页面使用） |
| `sys_login_log` | 16 | **6177** | （**无页面使用** ← 明显缺口，见 §3.6） |
| `sys_data_source` | 16 | **0** | 连接管理 |
| `sys_slow_query` | 13 | **0** | 慢查询（僵尸表，恒空） |
| `sys_backup_record` | 13 | **0** | 备份管理 |
| `sys_sync_task` | 16 | **0** | 同步任务 |
| `sys_data_cleanup_rule` | 14 | **0** | 清理规则 |
| `dev_template` | 12 | **7** | 模板管理（**本页未使用**，模板存后端内存） |
| `scheduled_task` | 23 | **16** | 定时任务 |
| `scheduled_task_log` | 14 | **15** | 定时任务 |
| `sys_job` / `sys_job_log` | 19 / 15 | **0 / 0** | xxl-job 遗留，未启用 |
| `gateway_log` | 17 | **0** | （无页面使用） |
| `api_access_log` | 18 | **63** | 交易模块 API 监控（**非本模块**） |
| `sys_menu` | 28 | **381** | 菜单管理（其中 `tenant_id=0` 380 行、`tenant_id=1` **1 行**） |

### 3.6 明显缺口（有数据/有表但**没有页面**）

| 缺口 | 现状 |
|---|---|
| **登录日志页** | `sys_login_log` 有 **6177 行真实数据**，但 **30 个菜单里没有"登录日志"页**（租户侧 ql361 有"登录日志"Tab，见《设置模块/操作日志开发文档》）。而 SAP SAL 的审计重点正是**登录失败 / RFC 登录 / 用户主记录变更**（B 级）。 |
| **用户 / 角色 / 部门 / 岗位 / 字典 / 权限矩阵页** | `views/system/` 下有 12 个文件（共 12541 行）却**只有 `menu` 一个挂上了菜单**（见 §2.5）|
| **SOD 规则 / 数据权限 / 字段权限 / 数据范围** | `sys_permission` 里有 **16 条**对应权限码（`system:sod-rule:*` 5、`system:data-scope:*` 3、`system:field-permission:*` 3、`system:simulate` 等），**但没有任何页面** → 权限码是"为未来预留"的。 |
| **公式/代码生成** | `62401 代码生成` 被两次删除（§2.1 问题 1）。 |

---

## 4. 与其它模块的接线点

| 方向 | 接线点 | 实现位置 | 状态 |
|------|--------|---------|------|
| 系统 → 设置 | **同一功能的两侧实现** | 菜单管理（6130701）↔ 菜单配置（80620），**两者都绑了菜单** | ⚠️ 重复实现，见 §2.5 组 C |
| 系统 → 设置 | 平台参数（62501）↔ 系统参数（80621） | 两侧都打 `/api/config` | ⚠️ **平台侧与租户侧共用同一个"内存假实现"** |
| 系统 → 设置 | 系统日志（62204）↔ 操作日志（80630） | 两侧都打 `SysLogStubController`（`/api/log`） | ⚠️ **同一后端两副前端**，且都无权限 |
| 系统 → 交易 | 接口监控（62203）↔ 交易模块 API 监控 | 前者零后端；后者 `ApiMonitorController`（`core-base`）+ `api_access_log` | ❌ 两套不相干，前者的能力应**复用后者** |
| 系统 → 调度 | 定时任务（62405）↔ 既有调度能力 | `scheduled_task` 表 + `JobHandlerRegistry`（SPI 在 `core-base`，记忆中的 `scheduler-job-handler-whitelist` 改造） ✅ 已真实连通 | ✅ 无缺陷（仅备案） |
| 系统 → 租户库 | 模块授权（62004）→ `sys_tenant_menu` | `SysTenantMenuController` | ✅ 端点可用；但 `sys_tenant_menu` **仅 4 行** → 授权面几乎为空 |
| 系统 → 邮件/短信 | 邮件配置（62502）/ 短信配置（62503）→ 通知模块 | `core/notification` 的 `EmailSenderImpl` / `SmsChannel` | ⚠️ 配置表已建但控制器未装配；通知模块的 `spring.mail.*` / `notification.sms.*` **未在 core-api yml 声明**（见《对标开发技术参考文档》§5.6.3） |
| 系统 → 存储 | 存储配置（62504）→ `LocalStorageStrategy` | 配置表已建；实际生效的是 `storage.local.base-path`（**必须绝对路径**，多实例要统一，否则上传文件跨实例 404） | ⚠️ 配置与实现**未接线** |
| 系统 → 安全 | 安全策略（62505）→ Sa-Token / 密码策略 | 配置表已建；实际生效的是 `password.policy.max-age-days` / `system.super-admin.role-codes`（core-api yml） | ⚠️ 未接线 |

---

## 5. 业界对标口径（本模块无 ql361 对标页）

### 5.1 为什么不能照 ql361 抄

ql361（来肯企汇）是**被托管的租户级产品**，它对外暴露的只有"设置"（租户自己怎么用），**没有"系统"（平台方怎么管租户）**。这不是 ql361 的缺陷 —— **任何厂商都不会把自己的 SaaS 控制台给客户看**。因此本模块必须按业界成熟的多租户 SaaS 平台建模。

### 5.2 一句话结论与「该学/不该学」

**平台级系统管理在业界是一个「控制面（Control Plane）」产品，不是业务系统的管理员菜单。** 核心资产 = **租户目录 + 权益/配额账本 + 运营审计流水**。

| 主题 | 业界一致的做法（**可学**） | 不该照搬（理由） |
|---|---|---|
| 租户生命周期 | 「订阅/租用」是一个**带状态的对象**；SAP BTP 明确「删除租用后该子账户中所有与多租户应用相关的数据都会被删除」 | 厂商的「删除即销毁」不适用本系统：**租户数据是客户资产**，必须做冷静期 + 软删除 |
| 套餐与授权 | **套餐（plan/tier）是"权利包的模板"，租户持有的是"分配记录"**；分配分「数字分配（限量）」与「非数字分配（开关）」两类（SAP BTP 原文） | **不要把套餐字段冗余到租户表**：套餐改版会污染存量租户 |
| 配额 | **配额是"授权数字"（有权用多少），用量是"计量数字"（实际用了多少），两者分离**；超限处置分「硬阻断 / 软告警 / 局部降级」 | **不做实时精确计量**：业界普遍是「周期汇总 + 准实时看板」 |
| 模块版本 | **状态机 + 版本三字段 + 依赖/互斥校验**；升级不是全量重来，而是「标记 to upgrade → 由 cron 串行执行」（Odoo） | Odoo 的「模块 = Python 包」模型不适用：本系统模块 = **菜单 + 权限 + 表结构的功能块** |
| 监控 | 用 **Spring Boot Actuator** 标准端点（`/health`、`/metrics`、`/caches`、`/scheduledtasks`、`/loggers`），**不要自造**；**liveness 不得依赖外部系统** | 自研线程池/连接池探针不如直接暴露 Actuator |
| 审计日志 | 字段的"最小公分母"：**时间 + 主体（account/user）+ 来源 IP + 目标模型/记录 + 操作（CRUD）+ 变更前后值 + 结果** | **别只记成功记录**：SAP SAL 的登录失败/RFC 登录/用户主记录变更才是审计重点 |
| 数据管理 | 备份/慢SQL/连接池**全部用数据库与框架原生能力**（`pg_stat_statements`、`pg_dump` + PITR、HikariCP 旋钮），页面只是可视化层 | **不要自研"慢SQL抓取"**：`pg_stat_statements` 是官方扩展且给出完整列 |
| 开发工具 | API 文档用 **OpenAPI**；API 调试台**必须按 OWASP SSRF 清单做 allowlist + 禁止跳转 + 屏蔽内网网段** | API 调试台**默认不要带"平台自身凭据"发请求** |
| 平台菜单 | **平台级菜单不进租户套餐**（jeecg 官方明文：「系统用户、系统角色菜单……不做租户隔离」） | **不要把平台菜单混进 `tenant_id` 过滤**，否则平台运营方自己被隔离掉 |

### 5.3 30 页「最接近的业界对象」一览

| # | 我们的页面 | 最接近的业界对象 | 最该对标的一家 | 落地 |
|:-:|---|---|---|:--:|
| 1 | 租户列表 | SAP BTP 全局账户/子账户；jeecg「租户管理」 | SAP + jeecg | ⚠️ |
| 2 | 租户审批 | 用友「企业账号/企业认证」 | 用友 | ⚠️ |
| 3 | 租户套餐 | SAP「服务计划」；jeecg「租户套餐包」 | jeecg + SAP | ⚠️ |
| 4 | 模块授权 | SAP「权利 Entitlement」+ Odoo `ir.module.module` | SAP | ⚠️ |
| 5 | 配额管理 | SAP「配额 Quota + 数字/非数字分配」 | SAP | ⚠️ |
| 6 | 模块列表 | Odoo `ir.module.module`（状态机） | Odoo | ⚠️ |
| 7 | 模块版本 | Odoo `installed_version`/`latest_version` 三字段 + `to upgrade` | Odoo | ⚠️ |
| 8 | 模块发布 | SAP 传输请求（TMS/STMS）+ Odoo.sh 分支 | SAP | ⚠️ |
| 9 | 使用统计 | AWS SaaS Lens「每租户消耗度量」 | AWS | ⚠️ |
| 10 | 服务状态 | Actuator `/health`（含 liveness/readiness 分组） | Spring Boot | ✅ |
| 11 | 性能监控 | Actuator `/metrics` + HikariCP + `pg_stat_activity` | Spring Boot + PG | ✅/⚠️ |
| 12 | 接口监控 | Actuator `/httpexchanges` + `/metrics` | Spring Boot | ✅/⚠️ |
| 13 | 系统日志 | Actuator `/logfile` `/loggers`；SAP SAL（SM19/SM20） | Spring Boot | ✅ |
| 14 | 操作审计 | SAP 安全审计日志；Odoo ACL 拒绝日志 | SAP | ⚠️ |
| 15 | 缓存管理 | Actuator `/caches`；Redis `INFO keyspace` | Spring Boot + Redis | ✅/⚠️ |
| 16 | 连接管理 | `spring.datasource.*` + HikariCP | Spring Boot | ✅ |
| 17 | 慢查询 | `pg_stat_statements`；SAP HANA Cockpit Expensive Statements | PostgreSQL | ✅ |
| 18 | 备份管理 | `pg_dump` / `pg_basebackup` + PITR | PostgreSQL + Odoo.sh | ⚠️ |
| 19 | 同步任务 | 金蝶「调度中心」；Odoo `ir.cron` | 金蝶 + Odoo | ⚠️ |
| 20 | 清理规则 | PostgreSQL 分区 DETACH/DROP；WAL 保留策略 | PostgreSQL | ⚠️ |
| 21 | 模板管理 | jeecg/RuoYi 代码生成器 | jeecg | ⚠️（无官方正文，见 §5.5） |
| 22 | API文档 | OpenAPI 3.1.0 | OpenAPI | ✅ |
| 23 | API测试 | OWASP SSRF 防护清单 | OWASP | ⚠️ |
| 24 | 定时任务 | Odoo `ir.cron`（失败/超时/批处理）+ Quartz misfire | Odoo | ✅/⚠️ |
| 25 | 平台参数 | Odoo `ir.config_parameter`（key/value，唯一） | Odoo | ✅/⚠️ |
| 26 | 邮件配置 | SAP SCOT SMTP 节点；Odoo 外发邮件服务器 | SAP | ✅ |
| 27 | 短信配置 | 阿里云短信（签名 + 模板 + 频控） | 阿里云 | ⚠️ |
| 28 | 存储配置 | Odoo.sh filestore 与备份 | — | ⚠️（未取证官方） |
| 29 | 安全策略 | OWASP Authentication / Session 清单 | OWASP | ✅/⚠️ |
| 30 | 菜单管理 | Odoo `ir.ui.menu` + `ir.model.access` + `ir.rule`；RuoYi `sys_menu`（M/C/F） | Odoo + RuoYi | ✅/⚠️ |

### 5.4 九个高价值专题（详见底稿 §8）

底稿用一整章回答了 9 个"业界怎么做"的问题，各页 §8 应引用其中最相关的一条：

1. **租户生命周期状态机**（试用/待审/正式/停用/欠费冻结/到期只读/注销）
2. **套餐 ↔ 模块授权 ↔ 租户三者建模**（含"降配时已超限数据"怎么处理）
3. **配额与用量计量**（维度 / 计量方式 / 超限处置三档）
4. **模块的版本与发布**（Odoo `ir.module.module` 的 `state` 状态机；灰度与回滚）
5. **平台级系统监控**（Actuator / OTel / `pg_stat_statements` 的标准字段）
6. **数据管理**（数据源密码加密、慢查询、备份与 PITR、retention policy）
7. **开发工具**（OpenAPI；**API 调试台的 SSRF/凭据外泄安全边界**；cron 的 misfire 处理）
8. **平台设置**（`ir.config_parameter` 的键/值/分组模型；SMTP/短信/存储/安全的**逐项配置清单**）
9. **平台级菜单 / 权限管理**（Odoo `ir.ui.menu`+`ir.model.access` vs RuoYi `sys_menu` M/C/F）

### 5.5 ⚠️ 未取得官方文档佐证的 18 项（**写作纪律**）

底稿 §9.1 列出 **18 项**查不到厂商官方文档的能力，其中与本模块直接相关的几条：

| 能力 | 检索结论 | 对本模块的影响 |
|---|---|---|
| **平台参数的"租户可覆盖"模型** | 检索 Odoo（`ir.config_parameter` 是**每数据库一份**，无租户维度）、SAP BTP（配置在子账户/服务实例层）→ **未找到任何厂商的"参数表支持租户级覆盖"官方文档** | 本系统 `tenant_id=0 全局 / >0 覆盖` 方案是**本系统自建设计（D 级）**，**不得写"业界标准"** |
| **"降配时已超限数据"的处理** | 无任何厂商官方明文 | "降配后存量只读不删 + 新建硬阻断 + 宽限期"是 **D 级推论** |
| **租户生命周期的「到期只读」「欠费冻结」** | 均无官方明文 | 应作为**本系统设计**写入，不得称业界标准 |
| **模块级灰度发布（按租户逐步放量）** | 未找到任何厂商的"按租户灰度"官方机制（SAP 的灰度是**环境级**） | 本系统的"按租户放量"是 **D 级自建设计** |
| **「模板管理 / 代码生成」的业界官方文档** | 未取到任何官方正文 | 本页标为「**业界无同形对标，本系统自建能力**」 |
| **API 调试台"凭据不外泄"的官方防护指南** | OWASP SSRF Cheat Sheet **完全没有**针对"API 测试/调试工具"的内容 | "调试台剥离平台内部凭据 + 响应大小/超时上限"是 **D 级自建安全设计** |
| **数据源密码加密的官方做法** | Spring Boot 参考页 + HikariCP README **均无属性加密章节** | **实施前须另行取证** |
| **「IP 白名单」的官方佐证** | 未找到厂商官方文档或 OWASP 直接条文 | **不得写成业界标准** |
| **retention policy 作为"产品能力"的官方文档** | 未找到任何厂商把「清理规则」作为独立产品能力的官方文档 | 只能"用数据库原生能力自建"，须在文档中明示 |
| **OpenTelemetry 能否兜底审计字段** | OTel 通用属性页**只有** `client.address`/`network.peer.address`/`server.address`；**没有** `enduser.id`/`enduser.role`/`error.type` | **审计模型必须自建**，OTel 兜不住 |
| **RuoYi `sys_menu` 的 M/C/F 表结构** | 官方文档站**未提供表结构章节**；源码 SQL 两次 `ECONNRESET` 未取得 | 该模型只能作**参考（C 级）**，不能写"业界标准" |
| **jeecg 的 `sys_tenant` / 租户套餐表结构** | 官方文档**只给功能与操作流程，不给表结构** | 只能提供**功能清单级依据**（C 级） |

> **写作纪律（三条硬提示，引自底稿附章）**：
> ① 平台侧数据**必须豁免多租户过滤** —— 漏掉这一步的表现是"页面空白/查不到数据"而**不是报错**，验收必须专门断言；
> ② 平台侧写操作**必须有审计** —— "谁在什么时候从哪个 IP 给哪个租户开了哪个模块"，事后无法从业务表还原；
> ③ 凡底稿标 **D 级**的方案，开发文档里必须写"**本系统设计**"而非"业界标准"；凡 §9.1 的 18 项能力，文档中**不得出现"业界通行做法是……"的表述**。

---

## 6. 写作与开发原则

- **两类配置严格分开**：查询条件/功能按钮 →「页面配置」；默认列 + 全量可配置列 →「数据表格列配置」。
- **不重复通用规范**：统一指向《对标开发技术参考文档》与《交易模块/_开发指南-金标准.md》。
- **不发明字段**：列名/按钮逐字取自本系统源码；业界补充项**必须单独标注为"业界建议、本系统未实现"**。
- **页面组件优先复用**、**功能/模块不重复开发**。本模块**尤其**要复用：Actuator（服务状态/性能/缓存）、`pg_stat_statements`（慢查询）、`pg_dump`（备份）、既有 `ApiMonitorController`（接口监控）、既有 `JobHandler` SPI（定时任务）、既有 `LogManageController`（日志）。
- **阶段结束前回写开发文档**：把「实现差异说明」更新为当前代码实际实现情况。
- **无模块级 E2E**：`tools/` 与 `tools/acceptance/` 下**不存在** `e2e-system-*.cjs` / `e2e-admin-*.cjs`。建议落地 `tools/e2e-system.cjs`（接口 + 直连 DB 对账 + 写库后复原 + Playwright UI 截屏，参照 `tools/e2e-crm.cjs`）。
- **【金标准】启动进程随手关**：见 `AI_DEVELOPER_RULES.md` 7.3 会话收尾纪律；按端口/PID 精确 kill，勿用文件名批量匹配。

---

## 7. 全局缺口汇总（P0 → P2）

> **P0** = 页面不可用/数据不落库/越权/红线；**P1** = 与业界能力差距大但页面能开；**P2** = 体验与一致性。
> 各页证据见 `tool-results/docgen/evidence/系统模块/<页>.md`。

### 7.1 P0（18 条，详见 §3.4）

1. **5 个包漏配 `scanBasePackages` → 19 控制器 / 126 端点从未装配（16 页受影响 + 跨模块外溢）** —— 本模块**最大的单点根因**
2. 数据管理 5 页 22 端点全 404
3. 接口监控整页桩 + 双端点不存在
4. 缓存管理后端返假数据，但 `DELETE /api/cache/all` 真清 Redis
5. 同步任务「立即执行」是状态桩，与 `sync-engine` 零连线
6. 清理规则「立即执行」不删任何数据
7. 备份管理「恢复」空实现且谎报成功
8. 系统日志解包缺陷致整页恒空
9. 操作审计 4 处字段错配 → 每行恒显「失败」
10. 定时任务前端硬编码 `{current:1,size:100}` 绕过后端筛选（~~"15 端点无权限注解"~~ 已复核不成立）
11. API测试页 url/method 被丢弃；修好即变 SSRF + 凭据外泄
12. 服务状态 3/4 依赖项是硬编码常量，故障会显示成健康
13. API文档页整页桩
14. 系统日志 3 处插槽列缺陷（操作列空白）
15. 平台级表未豁免多租户插件（8 张表预期查回 0 行）
16. 6 个 `system:menu:*` 权限码 0 行 → 非超管对菜单接口全 403
17. 平台侧无审计（`sys_audit_log` 0 行）
18. 存储配置前后端路径不匹配 + 包未装配（双保险不可用）

### 7.2 P1

| 缺口 | 证据 |
|---|---|
| **30 页全部无 `PageConfigPanel` / 无 `storage-key` / 无经典分页 / 无列配置齿轮** | §3.1 |
| **列定义普遍只有 `title`/`dataIndex`/`width`**，无 `type`/`defaultHidden`/`slotName` → 列配置能力整体缺失 | §3.1 |
| **`62401 代码生成` 空洞**（两次被删，ID 让给 HR） | §2.1 问题 1 |
| **`views/admin/monitor/log/index.vue` 幽灵文件**（永不被加载，其接口也不存在） | §2.5 |
| **8 个完全不可达的孤儿页**（含 5 个共 5000+ 行的平台管理页） | §2.5 |
| **4 组重复实现**（A/B/C/D） | §2.5 |
| **数据管理 5 页共 21 个权限码在 `sys_permission` 中均不存在** | §3.3 |
| **数据管理 5 页无查询表单 / 查询项极少**（同步任务、清理规则 **0 项**），且 `a-table` 全是**前端分页**而非后端分页 | 各页 §4/§6 |
| 连接管理页 placeholder 写"连接名称/主机搜索"，但后端 `like` **不匹配 host** | `DataSourceServiceImpl.java:33-38`；`connection/index.vue:20-27` |
| 清理规则 `status`（DB `integer`）与本页/实体语义冲突，写操作注定失败 | `sys_data_cleanup_rule` 实测 14 列 |
| 服务状态页「系统 CPU 使用率/进程 CPU 使用率」**二次 ×100** 显示放大 | 该页 §12.2 |
| **平台参数后端全程不落库**（内存 11 条），`sys_config` 8 行是孤儿表 | `SystemConfigServiceImpl` |
| **租户列表字段名错位**致「联系人/到期时间」恒空 | 该页 §3.5 |
| **租户审批 3 端点前缀不匹配 404**（前端打 `/api/tenant/*`，后端是 `/api/tenant-registration`） | §3.3 |
| **提交的凭据明文**（邮件密码、短信/存储 AccessSecret 无脱敏） | 邮件/短信/存储配置页 §11 |
| **模块版本/发布/使用统计写死假数据且发布链路零接线** | §3.4 P0-1 连带 |
| **`sys_login_log` 有 6177 行真实数据却没有页面** | §3.6 |
| **16 条 SOD/数据权限/字段权限权限码没有任何页面** | §3.6 |
| **定时任务前端硬编码 `{current:1,size:100}`** 绕过后端筛选 | §3.4 P0-10 |

### 7.3 P2

| 缺口 | 说明 |
|---|---|
| `61305 开发工具` 的 `sort=100` 空洞（`62401` 不存在） | §2.1 问题 1 |
| `6130701 菜单管理` 的 `client_type='tenant-admin'` 挂在 `system-admin` 组下 | §2.4 |
| `6130701` 是唯一 `tenant_id=1` 的行 → **本页无法编辑自己** | §2.4 |
| `61307 系统管理` 组只有 1 个子节点 | §2.1 问题 3 |
| `sys_tenant_menu` 仅 4 行 → 模块授权面几乎为空 | §3.5 |
| `sys_system_log`(26 列/0 行)、`gateway_log`(17 列/0 行)、`sys_job*`(0 行) 等**无页面使用的遗留表** | §3.5 |
| **无模块级 E2E、无验收截图资产** | §6 |
| 本模块**无 ql361 截图可嵌入**（无对标页） | §5.1 |

---

## 8. 本模块的施工建议（路线选择）

> 施工顺序：**先装配后端（P0-1）→ 再豁免租户（P0-15）→ 再补权限码（P0-16）→ 再修桩（P0-3~P0-14）→ 最后做外壳金标准**。
> 理由：**外壳换掉而后端仍是 404，验收一样不过**；反之后端通了、外壳还是旧 `a-card + a-table`，至少功能可用。

| 批次 | 内容 | 覆盖 |
|:----:|------|------|
| **B0** | **修 `scanBasePackages`**（补 5 个包：`module`/`monitor`/`platform`/`datasource`/**`export`**）+ **穷尽核查其余未列出的 `cn.aiedge.*` 包**；平台级查询补 `@InterceptorIgnore`（不依赖会话豁免） | 16 页 + 跨模块导入导出 |
| **B1** | **补权限码种子**：`system:menu:*` 6 个、`system:tenant:*` 2 个、`system:config:create/query`、`system:user:query`、数据管理 21 个；并**统一菜单管理页按钮码**到 `system:menu:*` | 全模块 |
| **B2** | **消除桩与假数据**：接口监控（复用交易 `ApiMonitorController`）、缓存管理（改 Actuator `/caches` + Redis `INFO`）、API文档（改 OpenAPI 3.1）、服务状态（改 Actuator `/health` 分组）、备份/同步/清理（接 `pg_dump`/`sync-engine`/原生 SQL）、API测试（按 OWASP SSRF 加固后再透传） | 9 页 |
| **B3** | **修数据正确性**：系统日志解包 + 3 处插槽、操作审计 4 处字段错配、租户列表字段错位、租户审批前缀、存储配置路径、平台参数落库（`sys_config`）、定时任务 `{current,size}` 硬编码 | 7 页 |
| **B4** | **外壳升金标准路线 A**：30 页统一 `ErrorBoundary > PageContainer(full-height) > CategoryListLayout(可选) > BillDetailTable` + `StandardPagination` + 表头齿轮列配置 + `PageConfigPanel` | 30 页 |
| **B5** | **清理架构债**：处置 8 个孤儿页 + 4 组重复实现 + 1 个幽灵文件；补「登录日志」页；评估 SOD/数据权限/字段权限三类权限码对应的页面是否要建 | — |
| **B6** | **补审计**（P0-17）：平台侧写操作落 `sys_audit_log`，按 SAP SAL 的字段口径 | 全模块 |

---

## 9. 文档集清单

```
docs/Yh-Spec/手动整理对标开发文档/系统模块/
  README.md                        （本文件）
  ── 租户管理 ──
  租户列表开发文档.md               62001
  租户审批开发文档.md               62002
  租户套餐开发文档.md               62003
  模块授权开发文档.md               62004
  配额管理开发文档.md               62005
  ── 模块管理 ──
  模块列表开发文档.md               62101
  模块版本开发文档.md               62102
  模块发布开发文档.md               62103
  使用统计开发文档.md               62104
  ── 系统监控 ──
  服务状态开发文档.md               62201
  性能监控开发文档.md               62202
  接口监控开发文档.md               62203
  系统日志开发文档.md               62204
  操作审计开发文档.md               62205
  缓存管理开发文档.md               62206
  ── 数据管理 ──
  连接管理开发文档.md               62301
  慢查询开发文档.md                 62302
  备份管理开发文档.md               62303
  同步任务开发文档.md               62304
  清理规则开发文档.md               62305
  ── 开发工具 ──
  模板管理开发文档.md               62402
  API文档开发文档.md                62403
  API测试开发文档.md                62404
  定时任务开发文档.md               62405
  ── 平台设置 ──
  平台参数开发文档.md               62501
  邮件配置开发文档.md               62502
  短信配置开发文档.md               62503
  存储配置开发文档.md               62504
  安全策略开发文档.md               62505
  ── 系统管理 ──
  菜单管理开发文档.md               6130701
```

**30 个菜单页 ↔ 30 篇页面文档**，一一对应，无多余、无遗漏。

**取证素材**（供复核）：`tool-results/docgen/evidence/系统模块/`（34 个 `.md`，含 `_汇总A.md`、`_汇总B.md`、`_孤儿与重复.md`、`_重叠与重复B.md`）；
**业界对标底稿**：`docs/Yh-Spec/抓取结果/业界对标-系统模块-20260918.md`（1090 行）。

---

## 10. 2026-09-19 实施结果（**本节是各页缺陷状态的权威口径**）

> 本轮把 30 页按本文档集实施完毕，并逐页真机验证。**各页文档 §9「实现差异说明」/ §12「剩余缺口」写的是实施前的状态，凡与本节冲突，以本节为准。**
> 验收脚本：`node tools/e2e-system.cjs`（**67/67 通过**，含直连 devdb 对账 + 写后复原）。

### 10.1 后端根因修复（对应 §2.3 / §3.4 / §7.1）

| # | 原缺陷 | 处置 | 文件 |
|:-:|--------|------|------|
| 1 | **5 个包漏配 `scanBasePackages` → 19 控制器 / 126 端点从未装配**（P0-1） | 补 `cn.aiedge.{module,monitor,platform,datasource,export}` | `AiReadyApplication.java` |
| 2 | **装配后暴露 `Ambiguous mapping` → 应用启动失败**（新发现） | `SystemMonitorController` 与 `AlertManagementController` 有 7 组完全相同的 method+path；删除前者的告警段，保留功能更全的后者 | `SystemMonitorController.java`；新增静态检查 `tools/check-api-mapping-conflicts.py` |
| 3 | **并行会话 Flyway 迁移抢固定 id**（新发现） | `91301` / `9130701` / `91321` 三处撞号，改到实测空闲段 | `V11.396.0` / `V11.398.0` |
| 4 | **租户菜单授权链路整体不可用**（新发现，3 个缺陷叠加） | ① `insertFill` 会用会话租户覆盖实体 tenantId → 改独立参数 `#{tenantId}`；② `uk_tenant_menu` 唯一索引不含 `deleted` → 改物理删除；③ 空数组语义 = 撤销全部（此前直接 return 造成谎报成功） | `SysTenantMenuMapper.{java,xml}`、`SysTenantMenuServiceImpl.java`、`MyBatisPlusConfig.java` |
| 5 | **`sys_data_cleanup_rule.status` 是 integer 而实体是 String → 写操作全 500** | 列改 varchar，向同组三表看齐 | `V11.406.0` |
| 6 | **`sys_security_policy.rate_limit` 是 integer 而实体是 boolean → 读写双 500**（P0-18 的真相） | 实体改 `Integer`、前端改数值输入，三方口径统一 | `SecurityPolicy.java`、`security/index.vue` |
| 7 | **平台配置表每表 2 行重复种子**（新发现） | `selectOne` 撞多行 → 去重 + 加 `UNIQUE(tenant_id)` | `V11.408.0` |
| 8 | **平台设置按会话租户写入，产生 `tenant_id=1` 影子行**（新发现） | 4 个 platform 控制器固定全局口径 `tenant_id=0` | `Mail/Sms/Storage/SecurityPolicyController.java` |
| 9 | **权限码 0 行**（P0-16） | 全量扫描 `@SaCheckPermission` 与库对比，补齐系统模块相关 **58 个**权限码并授权超管 | `V11.407.0` |
| 10 | **缓存管理整页假数据但删除是真删**（P0-4） | 改真实读 Redis `INFO` + `SCAN`（区域按真实键前缀聚合；区域级内存/命中率 Redis 不提供 → 返回 null 由前端显示「—」，不再编数） | `CacheManageController.java` |

### 10.2 30 页实施状态

| 分组 | 页面 | 实施结果 | 仍未闭环 |
|---|---|---|---|
| 租户管理 | 62001 租户列表 | ✅ 路线 A 全套；修字段错位（`contactName/expireDate` → `contactPerson/expireTime`）致两列恒空；`PATCH status` 405 修复；状态切换去乐观更新 | 权限码已补但未接 `v-permission` |
| | 62002 租户审批 | ✅ 路线 A；修状态列写死；驳回改真实原因弹窗（不再写死 `'驳回'`） | 无审批流水表，驳回原因随逻辑删除不可见 |
| | 62003 租户套餐 | ✅ 路线 A；`fetchData` 补 catch；价格 0 不再显示成 `-` | 后端 `/list` 无入参（筛选/分页为本地） |
| | 62004 模块授权 | ✅ 卡片字段错配修复（`menuName`/`id`）；空数组二次确认 + 提交后回读校验，**不再谎报成功**；增搜索/全选/切换清空 | 后端写入链路已修（见 10.1-4） |
| | 62005 配额管理 | ✅ 路线 A；弹窗改租户下拉（不再自由文本导致归属错） | used_* 无采集路径；超限无处置 |
| 模块管理 | 62101 模块列表 | ✅ 路线 A；后端加 **分页 + 名称/编码模糊 + 状态筛选**；新增/删除入口接线；停用乐观更新改回读刷新；发布状态中文化 | — |
| | 62102 模块版本 | ✅ 路线 A + 详情/发布弹窗；后端加筛选分页；**排序改 `NULLS LAST`**（草稿不再顶到最前） | 版本表无唯一约束（存量 12 行有重复，未删数据） |
| | 62103 模块发布 | ✅ 路线 A；修 `status` 字段名错配（后端是 `releaseStatus`）；回滚接真实端点 | 无独立发布单实体（与 `/versions` 同源） |
| | 62104 使用统计 | ✅ 4 张统计卡改**真实汇总**（此前写死 12/10/48/78 无任何数据源）；窗口可调（7/30/90 天）；模块名可下钻 | — |
| 系统监控 | 62201 服务状态 | ✅ 删 `\|\|100`/`\|\|'UP'` 兜底（故障不再伪装成健康）；磁盘表上 `BillDetailTable`；补 database/ready/live 卡 | `/dependencies` 的 redis/mq 恒 DOWN、externalApi 恒 UP 是**后端常量**（页面已如实标注） |
| | 62202 性能监控 | ✅ **修 CPU 二次 ×100**；补统计窗口/指标/刷新间隔；接 `/aggregate`、`/trend`（折线）、`/predict` | `/compare` 未接（依赖进程内历史） |
| | 62203 接口监控 | ✅ **整页桩 → 复用交易侧 `ApiMonitorController`**（8 张真实汇总卡 + 台账 + 告警双 Tab）；删 404 端点与写死状态 | 错误率/吞吐量后端确无（页面已注明口径） |
| | 62204 系统日志 | ✅ 修**响应解包错致整页恒空** + 5 处字段错配（`realName`/`operIp`/`operTime`/`action`）+ 3 处插槽缺陷 | 后端无统计端点（后 3 卡为本页口径） |
| | 62205 操作审计 | ✅ 修 4 处字段错配（每行恒显「失败」）+ `failureCount`；详情改真实调 `/detail/{logId}` | `sys_audit_log` 0 行（无写入方） |
| | 62206 缓存管理 | ✅ **读假 → 真实 Redis**；危险操作加二次确认；失败不再伪造本地删除 | 区域级内存/命中率 Redis 不提供（显示「—」） |
| 数据管理 | 62301 连接管理 | ✅ 路线 A + 后端分页；placeholder 文案改准 | 类型/状态筛选后端无入参（本地过滤） |
| | 62302 慢查询 | ✅ 路线 A + 后端分页 | 关键字/阈值后端无入参（本地过滤） |
| | 62303 备份管理 | ✅ 路线 A + 查询区（原 0 项）；**核心动作如实标注「仅台账、未真实备份」**，回读校验后才提示 | 后端无 `pg_dump`、恢复空实现 |
| | 62304 同步任务 | ✅ 路线 A + 查询区（原 0 项）；**执行后回读，不谎报** | 后端只 `setStatus("running")`，与 `sync-engine` 零连线 |
| | 62305 清理规则 | ✅ 路线 A + 查询区；**status 列类型修复后写路径已通**；执行不谎报 | 后端无清理执行器（不读 targetTable/retentionDays） |
| 开发工具 | 62402 模板管理 | ✅ 路线 A；删假数据兜底 | 后端读**内存注册表**（非 `dev_template` 表），重启即复原 |
| | 62403 API文档 | ✅ **整页桩 → 真实 OpenAPI**（`/v3/api-docs` 2997 paths / 3431 操作）；删写死数组与死分支 | `/swagger-ui/index.html` 未在 Sa-Token 放行（按钮置灰） |
| | 62404 API测试 | ✅ **整页桩 → 真实调试台**（真实状态码/耗时/响应体）；按 OWASP 做路径白名单、禁重定向、5s 超时、256KB 上限 | allowlist/凭据剥离/调用审计需**后端**兜底（页面已声明） |
| | 62405 定时任务 | ✅ 路线 A；**去掉硬编码 `{current:1,size:100}`**，改真实分页+筛选 | 无 |
| 平台设置 | 62501 平台参数 | ✅ 路线 A；后端**已真实读写 `sys_config`**（18 行，不再读内存） | 内置参数不可删；变更日志操作人恒空 |
| | 62502 邮件配置 | ✅ 分区表单 + `SecretInput` 掩码回显（**按值判定提交**，不用掩码覆盖真凭据） | 3 个 `/test` 全是桩；无发送链路消费方 |
| | 62503 短信配置 | ✅ 同上；删假模板表与假统计卡 | 同上 |
| | 62504 存储配置 | ✅ **修前后端路径不匹配**（`/api/storage/*` → `/api/storage-config/*`） | `local_path` 仅文件读取有消费方 |
| | 62505 安全策略 | ✅ **rate_limit 三方类型统一**，读写恢复正常；其余布尔字段与列一致 | 全仓无 `sys_security_policy` 消费方（策略不生效） |
| 系统管理 | 6130701 菜单管理 | ✅ 按钮权限码**统一到后端真实要求的 `system:menu:*`**；补表头齿轮（`ColumnConfigTable`）；修「上级菜单」字符串 id 恒显 `-` | `BillDetailTable` 不支持树形（保留原表 + 注释说明）；「角色」依赖的 `GET /api/role/{id}/menus` 后端 404 |

### 10.3 本轮新增/变更的交付物

| 类型 | 路径 |
|---|---|
| 模块级 E2E | `tools/e2e-system.cjs`（67 项，含 DB 对账与写后复原） |
| 静态检查工具 | `tools/check-api-mapping-conflicts.py`（扫「已装配包的跨类映射冲突」，装配任何新包前先跑） |
| 迁移 | `V11.406.0`（清理规则 status 类型）、`V11.407.0`（58 个权限码种子）、`V11.408.0`（平台配置去重 + 唯一约束） |
| 前端共享组件 | `SecretInput`（凭据掩码回显，按值判定提交） |

### 10.4 §10.4 清单的处置结果（2026-09-19 第二轮，**真机验证**）

> 验证脚本：`node tools/e2e-system-fixes.cjs`（`SYS_PORT=<端口>`，**64/64 通过**）。
> 回归：`node tools/e2e-system.cjs <端口>`（**67/67 通过**）。
> 本轮后端跑在**独立端口 5690**（不干扰并行会话的 5655），jar 为冻结副本。

| # | 原登记项 | 处置 | 证据 / 实测 |
|:-:|---------|------|------------|
| 1 | 平台设置 3 个 `/test` 是桩（恒 true） | ✅ **改真实探测**：SMTP 真实建连并认证；短信做「配置完整性 + 服务商端点 TCP 建连」；存储做「本地写入探测 / 对象存储端点连通」 | 连不通的主机 → `success=false` 且带原因；本地目录写入探测 → `success=true`；不可用路径 → `success=false`。`MailConfigServiceImpl` / `SmsConfigServiceImpl` / `StorageConfigServiceImpl`；返回体由 `boolean` 改为 `ConnectionTestResult{success,message}` |
| 2 | 备份/同步/清理「核心动作」未实现 | ✅ **备份真实现**（`pg_dump --format=custom` + 异步 + 恢复三重闸门）；✅ **清理真实现**（服务端白名单 + 分批 DELETE + 上限）；⚠️ **同步诚实降级**（复用 `SyncConfigService.triggerSync` 真实投递，但本系统无法确认搬运结果，明确报 `dispatched` 而非成功） | 五页端点在独立端口下均 200；不存在的记录 → `{"success":false,"message":"备份记录不存在"}`；`/data-source/cleanup/allowed-tables` 白名单端点可用；迁移 `V11.409.0` |
| 3 | 平台级配置无消费方 | ✅ **邮件/短信/密码策略已接线**：新增 `core-base` SPI（`PlatformMailSettingsProvider` / `PlatformSmsSettingsProvider` / `PlatformSecuritySettingsProvider`）+ `core-api` 实现 `DbPlatformSettingsProvider`；`EmailSenderImpl`、`SmsSenderImpl`、`EmailChannel`、`SmsChannel`、`PasswordPolicy`、`SysUserServiceImpl` 全部改为**DB 优先、yml 兜底** | 邮件/短信「存了没人读」已终结；`PasswordPolicy` 现按 `password_min_length` + 四个字符类别开关校验；`password_expire_days` 进登录过期判定。**⚠️ 安全策略仍只接了这两项**，登录锁定/会话超时/IP 白名单/单设备/限流/审计保留天数**仍无消费方**（已如实标注在页面） |
| 4 | 模板管理读内存注册表 | ✅ **改读 `dev_template` 表**（迁移 `V11.415.0`）：新增 `template_kind` 判别列把 7 行「代码生成」遗留与 4 行「导入模板」隔离；删除 `ImportTemplateRegistry`（内存注册表），不留双份真相；`dev_template` 已加入 `IGNORE_TENANT_TABLES`（该表无 `tenant_id`，不忽略会整页 500） | `GET /api/import-templates` 返回 4 条且不含 codegen 行；DB 实测 `codegen=7 / import=4` |
| 5 | API 测试台仅前端校验 | ✅ **后端闸门到位**：新包 `cn.aiedge.devtool`（`POST /api/dev/api-test/send` + `GET /policy`），10 条闸门全实现（协议白名单 / 服务端 allowlist / 内网及云元数据网段 / **钉 IP 防 DNS Rebinding** / 禁跳转 / 超时 / 流式 256KB 截断 / 凭据剥离 + 逐跳头丢弃 / 权限码 / 限流 30 次·分⁻¹·人⁻¹） | 实测：`file://`、`169.254.169.254`、`127.0.0.1`、`10.0.0.1`、`Host` 头、`Authorization` 头**全部被拒并给出原因**；同源 `/api/auth/check` 正常返回 `status=200 elapsedMs=162`。迁移 `V11.418.0`（权限码 `system:dev:api-test:send`） |
| 6 | `GET /api/role/{id}/menus` 缺失（404） | ✅ **补端点**（`SysRoleController`）：与既有 `POST /{id}/menus` 配对，服务层 `getRoleMenuIds` 早已存在，只缺控制器入口 | 实测 200 且返回数组；对照 `GET /{id}/permissions` 仍正常 |
| 7 | 孤儿页 / 重复实现 / 幽灵文件 | ✅ **已处置**（迁移 `V11.419.0`）：**删 5 个文件**（幽灵 `admin/monitor/log/index.vue`、重复 `system/tenant`、`system/tenant-approval`、`admin/tenant/permissions`、`admin/sys/permissions`）；**新挂 3 个菜单**（`6130702 数据字典` / `6130703 系统配置` / `6130704 权限配置`，父 `61307`）；保留 `system/permission`（三份权限页中最完整、无桩） | 实测：5 个文件均不存在；3 个新菜单 `client_type=system-admin`、`status=1`；`/api/menu/list` 仍 200。E2E 菜单计数由 30→**33 页**（已同步更新断言） |

#### 10.4.1 本轮**真机跑出来**的新缺陷（静态核对查不出）

| 级别 | 缺陷 | 真机证据 | 处置 |
|:----:|------|---------|------|
| **P0** | **API 测试台同源请求必失败**：`PoolingHttpClientConnectionManager(registry, dnsResolver)` 的第一个参数传了 `null`，构造能过、**发请求时**抛 `Socket factory registry may not be null` | `POST /api/dev/api-test/send {url:'/api/auth/check'}` → `{"code":400,"message":"Socket factory registry may not be null"}` | ✅ 改为真实的 `RegistryBuilder` 注册表（http/https socket 工厂 + 钉住的 `DnsResolver`）。**⚠️ 这个缺陷极其阴险：所有「被拒绝」的用例都能通过**（它们在建立连接之前就被闸门挡下），只有**放行路径**才会暴露 —— 只测拒绝路径会 100% 漏掉 |
| **P0** | **Flyway 占位符误伤**：`V11.415.0` 的**注释**里写了「美元符号 + 花括号」的占位符字样，Flyway 对**整个迁移文件（含注释）**做占位符替换 → 应用**启动直接失败** | `Unable to parse statement in V11.415.0__....sql at line 10 col 1 ... No value provided for placeholder` | ✅ 改写注释措辞并在文件内留下警示。**新写迁移时注释里不要出现该写法** |
| **P1** | **构建参数会静默关掉 Lombok** | 用 `-Dmaven.compiler.useIncrementalCompilation=false` 构建时，**大量** `@Slf4j` 类报「找不到符号 log」（`SearchController`/`ReportAnalyticsController`/`PermissionAspect` 等）→ 看起来像"全仓编译坏了"，实际是注解处理没跑 | ✅ **不要**用这个参数；用普通 `mvn clean install` 即可（本仓的残类问题用 `clean` 解决，而不是关增量） |
| **P1** | **`clean` 构建暴露大量"假通过"**：增量构建下 `target/classes` 里是 IDE(ECJ) 留下的**残类**，缺 `$1` 匿名内部类 → 打包出的 fat jar 启动即 `NoClassDefFoundError` | `AuditRuleService$1.class` 缺失 → 启动报 `NoClassDefFoundError: cn/aiedge/workflow/service/AuditRuleService$1` | ✅ 用 `mvn clean install` 重建。**建议把「发布前必须 clean 构建」写进流程**（增量构建会让人以为一切正常） |

#### 10.4.2 仍未闭环（本轮之后）

1. **安全策略只接了 2 项**（密码长度与字符类别、密码有效期）；**登录锁定阈值/时长、验证码、双因素、会话超时、IP 白名单、单设备登录、接口限流、审计保留天数仍无消费方** —— 改这些字段不会改变运行时行为（页面已如实标注）。
2. **同步任务的「数据搬运」本身仍不在本系统内**：本系统只做到「真实投递给同步引擎」，**无法确认搬运结果**（引擎侧是独立 Python 服务，其 `/api/sync/trigger` 自身也只返回"下一次调度周期执行"）。页面文案已改为不谎报。
3. **备份只支持 PostgreSQL 单库**：无保留策略（retention）、无定时备份、无异地存储；恢复是**同步阻塞**且未做"恢复前自动快照"。
4. **平台设置 3 个 `/test` 的边界**：短信/对象存储**未集成厂商 SDK**，故只验证「网络连通 + 配置完整性」，**不校验凭据有效性**（返回文案已写明）。
5. **孤儿页仍有 4 份未裁定**：`views/system/user`、`role`、`department`、`position` —— 与「人力资源模块」「资料 → 职员权限」职责重叠，**归属待业务裁定**（本轮按规则未删、未挂菜单）。
6. **菜单管理「角色」入口的完整闭环**：端点已补，但该弹窗依赖的其它链路（角色列表 `listAll`）在超管下正常、非超管仍受权限码约束，未做端到端 UI 验收。
7. **API 测试台的限流是单实例内存**（非分布式）；拒绝原因只进应用日志、未进 `sys_oper_log`；无专用审计表。
