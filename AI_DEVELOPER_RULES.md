# AI-Ready 开发规范（对标 Odoo）

> 本文件为 AI 开发人员在编码前必须阅读的最高优先级规范文档。

---

## 一、目标概述

我们正在开发一款对标 Odoo 的企业管理系统，目前页面框架已初步成型，菜单通过数据库管理，所有页面已写入菜单项，但大部分页面尚处于"开发中"状态。Odoo 对每个页面都有成熟的参考代码和文档说明，请严格按照 Odoo 官方文档完成前后端代码开发。

**已完成的参考页面**（这些页面的风格是全局 UI 的统一标准，所有新开发页面必须优先参考）：
- 工作台
- 新增销售订单表单
- 销售订单列表页
- 商品列表页
- 新增商品表单
- 新增客户/供应商/物流单位/其他往来单位表单及详情页

**核心要求**：所有新页面开发，前端组件必须优先参考上述已完成页面的风格，确保全系统 UI/UX 统一。同时，系统经过多次重大重构，前后端和数据库中存在大量冗余代码，开发过程中必须同步清理。

**总原则**：如果我们的页面和功能比 Odoo 优秀和完善，就按我们的方案执行。否则严格遵循 Odoo 的实现方式。

---

## 二、第零原则：权威文档优先（Source of Truth First）

这条原则的优先级高于 Six-Rung Ladder 和所有编码规范。任何技术疑点，必须以官方文档为唯一裁决依据，严禁凭记忆、猜测或二手博客编码。

### 2.1 遇到以下情况，必须先查文档再写代码
- 不确定某个 ORM 方法（search、read_group、write、unlink）的返回值和参数签名。
- 不清楚 OWL 组件的生命周期钩子（onMounted、onWillUpdate）或响应式机制。
- 需要继承/覆写某个原生 Odoo 视图（如 form、tree、kanban）但不确定 XML 结构。
- 涉及安全权限（ir.model.access、记录规则）的配置方式。
- 需要调用 Odoo 原生 RPC 接口或 @api.model 装饰器的正确用法。

### 2.2 指定查阅路径（按优先级排序）
1. **Odoo 官方在线文档**（当前版本为 18.0 / 17.0，按项目实际版本）：
   - 开发者文档：https://www.odoo.com/documentation/<version>/developer.html
   - ORM API：https://www.odoo.com/documentation/<version>/developer/reference/backend/orm.html
   - OWL 前端框架：https://www.odoo.com/documentation/<version>/developer/reference/frontend/owl.html

2. **本地源码**（最权威的"活文档"）：
   - 后端源码路径：odoo/odoo/models.py、odoo/odoo/api.py 中的 docstring 和类型注解。
   - 前端源码路径：odoo/addons/web/static/src/ 下的核心组件和钩子实现。
   - 官方内置模块源码（如 sale、purchase、stock）是最好的参考范本。

3. **已完成的参考页面源码**：作为本项目内部的"最佳实践样板"，优先模仿其写法。

### 2.3 执行要求（强制）
- 在提交的代码注释中，必须注明参考文档的章节或链接。
  例如：`# Ref: Odoo 18.0 ORM API - search() - domain用法`
- 禁止引用 CSDN、博客园、个人笔记等非官方来源作为技术依据（除非官方文档未覆盖且已获架构师特批）。
- 如果查遍文档仍未找到答案，必须向团队 Leader/架构师提问，并附上已查阅的文档链接，禁止闭门造车式猜测。

---

## 三、强制性架构原则（Six-Rung Ladder）

所有开发人员在编写任何代码前，必须逐级自检，通过后方可动手。这是最高优先级的"架构过滤网"。

### 阶梯 1：YAGNI（You Aren't Gonna Need It）
- **自检**：这个新功能/新字段/新表，业务方现在立刻就需要吗？还是"可能以后会用"？
- **落地**：拒绝为未来过度设计。不写预留接口、不建预留字段。如果 Odoo 原生没有，先反问：我们真的需要造这个轮子吗？若是基于 Odoo 已有模块（如 sale、purchase、stock）能通过简单配置或继承实现的，绝不自建新模块。

### 阶梯 2：Stdlib（标准库优先）
- **自检**：Python 标准库（datetime、json、re、collections 等）或 Odoo 原生 ORM 方法（search、browse、create、write、mapped、filtered、sorted）能解决吗？
- **落地**：禁止为了"方便"引入非必要的第三方 PyPI 库。能用 Odoo ORM 链式调用解决的，绝不用裸 SQL（除非极端性能优化场景，且必须经架构师审批）。能用 Python 内置函数处理的，绝不引入工具库。

### 阶梯 3：Native Platform Feature（原生平台特性）
- **自检**：浏览器原生 API（Fetch、ES2022+、CSS Grid/Flexbox、CSS 自定义属性）或 Odoo 框架原生 JS API（@web/core 下的工具函数）能搞定吗？
- **落地**：禁止引入冗余的第三方 JS 库（如 Lodash、Moment.js 等）。使用原生 fetch 替代老旧 AJAX 库；使用原生 Intl.DateTimeFormat 处理日期；使用 OWL 框架自带的响应式和钩子（useState、onMounted）替代外部状态管理库。

### 阶梯 4：Installed Dependency（已安装依赖复用）
- **自检**：当前模块 __manifest__.py 的 depends 中已有的依赖（如 mail、web、sale_management）能复用其功能吗？
- **落地**：坚决不引入新供应链风险。若要新增第三方依赖，必须提供充分理由（安全审计通过+必要性证明），否则一律否决。宁可多写几行继承代码，也不随意扩大依赖树。

### 阶梯 5：OneLine（一行代码原则）
- **自检**：这段逻辑能否用一行干净利落、可读性强的代码表达？
- **落地**：
  - Python 侧：允许使用列表推导式、生成器表达式、filtered/mapped/sorted 链式调用、解构赋值。
  - JS/OWL 侧：允许使用箭头函数隐式返回、模板字符串、解构赋值、可选链（?.）、空值合并（??）。
  - **底线**：单行必须以不影响可读性为前提。若一行过于晦涩（如嵌套三层推导式），必须拆解为多行并添加注释。绝不为了追求"一行"而写出反人类的长行代码。

### 阶梯 6：The Minimum That Works（最小可用闭环）
- **自检**：如果前面 5 步都走不通，我能否只实现当前迭代所需的最小功能集？
- **落地**：开发新页面/新功能时，先走通"幸福路径"（Happy Path）——即正常操作下的 CRUD 闭环。边缘异常、复杂校验、高级配置统统放到后续迭代。提交 PR 前，确保页面能渲染、数据能保存、列表能刷新。拒绝"大爆炸式"开发，拥抱渐进式交付。

---

## 四、死守红线：Lazy, not negligent（慵懒，但绝不疏漏）

架构师的修养：能少写绝不多写，但防御性代码一条都不能省。

### 4.1 安全红线（跨信任边界）
- **SQL 注入防御**：禁止拼接原生 SQL 字符串。必须使用 Odoo ORM 安全方法，或使用参数化游标（`cr.execute(sql, (params,))`）。
- **XSS 防御**：前端渲染用户动态数据时，必须依赖 OWL/QWeb 的自动转义机制，禁止使用 t-raw 输出未经验证的 HTML 内容。
- **权限校验**：所有后端方法（尤其是 @api.model 和 @api.depends 之外的自定义 RPC 接口）必须显式检查 `self.env.user.has_group()`，禁止信任前端传来的权限标识。

### 4.2 容灾与健壮性红线
- **异常捕获**：调用外部 API 或执行文件 IO 时，必须有 try...except 兜底，并记录清晰的错误日志（`_logger.error`）。禁止用空的 `except: pass` 吞掉异常。
- **事务一致性**：涉及多表写操作时，必须确保在同一事务上下文中。使用 @api.atomic 装饰器或在 Environment 中显式管理事务。禁止在循环中逐条 create 且不包裹事务，防止部分成功导致脏数据。
- **数据删除防护**：unlink 方法必须重写以添加业务校验（如已审核单据不可删除），或使用 active 标志位实现逻辑删除，杜绝物理删除关键业务数据。

### 4.3 性能红线
- **N+1 查询**：在列表视图和循环中，必须使用 prefetch 或 mapped 预加载关联数据，禁止在循环内触发新的数据库查询。
- **大数据量操作**：超过 1000 条记录的批量操作必须使用 `models.execute_in_batch()` 或游标分页处理，禁止一次性加载全部数据到内存。

---

## 五、前端开发规范（细化）

### 5.1 组件开发规范（OWL）
- **组件结构**：每个组件必须包含 .js、.xml、.scss 三个文件，置于 `static/src/components/<component_name>/` 目录下。
- **代码量红线**：单个组件 JS 逻辑代码不得超过 300 行。超过则必须拆分子组件。
- **状态管理**：使用 useState 管理内部状态。跨组件通信优先使用 props+emit 事件，禁止滥用全局状态。
- **遵循 OneLine 原则**：模板中的动态属性绑定（t-attf）和事件处理（t-on-click）尽量简洁；JS 中的数据处理优先使用原生数组方法链式调用。

### 5.2 样式规范（SCSS）
- **强制统一**：所有新页面的字体、间距、颜色、圆角、阴影必须与已完成的参考页面（工作台、销售订单表单等）保持一致。禁止私自定义新的主色或间距变量，必须复用 assets 中已定义的 SCSS 变量。
- **响应式**：使用 CSS Grid/Flexbox 布局，结合 Bootstrap 栅格系统（Odoo 已集成）。
- **类命名**：推荐 BEM 规范（.block\_\_element--modifier），避免深层嵌套（不超过 3 层）。

---

## 六、后端开发规范（细化 + 冗余清理）

### 6.1 模型与文件命名（严格执行）
- 主模型文件以主模型名命名，例如 `sale.order` 对应 `sale_order.py`。
- 继承已有模型（如 `res.partner`）时，单独放在 `models/res_partner.py` 中，禁止写在主模型文件里。

### 6.2 数据库冗余字段处理（必须执行）
结合 YAGNI 原则，彻底清理历史包袱：
1. **审计清单**：每开发一个模块，必须同步输出该模块的"字段使用情况报告"，标记出未被任何视图、方法、报表引用的字段。
2. **清理动作**：
   - 对未使用字段，编写迁移脚本（migrations/ 目录）执行 DROP COLUMN（仅限自定义模块新增字段）或设置 delete=True。
   - 对含义重复的字段（如两个模型都有 partner_id 但指向不同表），合并为统一的关联字段。
   - 对废弃的 many2many 关系表，确认无数据后删除并清理对应的 _sql_constraints。
3. **预防机制**：新增字段前，强制检索代码库确认无同名/同义字段。新增字段必须添加清晰的 help 字符串和 string 标签。

### 6.3 视图与安全
- 视图文件（`_views.xml`）必须与模型文件分开。
- 权限 CSV 文件（`ir.model.access.csv`）必须覆盖所有新增模型，禁止开放 `base.group_system` 以外的写入权限给普通用户。

---

## 七、开发流程与协作要求

### 7.1 编码前自检清单（提交 PR 前必须过一遍）
- [ ] 第零原则（文档）：涉及 Odoo 原生 API/ORM/OWL 的写法，是否已查官方文档并在注释中注明出处？
- [ ] YAGNI：这个功能是否真的需要？能否用现有模块配置/继承实现？
- [ ] Stdlib/Native：是否用了 Python/浏览器/Odoo 原生能力，而非引入新库？
- [ ] Installed：是否复用了现有依赖，无新增风险包？
- [ ] OneLine：核心逻辑是否足够简洁优雅？
- [ ] Minimum：是否只实现了本次迭代的最小闭环（幸福路径）？
- [ ] 红线安全：是否存在 SQL 注入/XSS/越权漏洞？异常是否被妥善处理？
- [ ] 冗余清理：新增字段是否重复？是否标记了废弃字段？

### 7.2 代码审查重点（Reviewer 必查）
- 第零原则违规：是否凭印象使用了 Odoo API 而没查文档？注释中是否缺少官方参考链接？
- 是否为了"一行"而牺牲了可读性？
- 是否在循环中触发了数据库查询？（性能红线）
- 是否拷贝了 Odoo 原生代码而未做精简？（拒绝无脑搬运，必须按 OneLine 和 Min 原则做减法）

### 7.3 验收脚本落位（金标准）

- **新增模块级 E2E 脚本统一放 `tools/`**，命名 `tools/e2e-<模块>.cjs`；用 `NODE_PATH` 加载依赖、**不依赖脚本自身所在位置**（可被自由移动/复用）。
- 历史上散落在项目根目录的 30 个脚本已统一归置到 **`tools/acceptance/`**，**该目录不可再移动**（这批脚本用 `__dirname` 定位项目根），运行方式见 `tools/acceptance/README.md`。
- 每个模块的验收口径（脚本 + 通过数）必须回写到对应《XX开发文档》的「实现差异说明」中。

### 7.4 会话收尾纪律（强制，2026-09-12 起）

> **每个人 / 每个 AI 会话在结束当前工作前，必须关闭自己启动的实例，并清理自己产生的临时产物。**
> 目的：避免重复启动造成系统阻塞（端口占用、内存耗尽、共享产物互相覆盖）。

**① 关闭自己启动的实例**

| 类型 | 识别方式 |
|------|---------|
| 后端 | `java.exe` 且命令行含 `-jar` + `core-api`（含为验证临时复制的 `<模块>-run.jar`） |
| 前端 | `node.exe` 且命令行含 `vite`（dev server） |

```powershell
# 查看
Get-CimInstance Win32_Process -Filter "name='java.exe'" |
  Where-Object { $_.CommandLine -like '*-jar*' -and $_.CommandLine -like '*core-api*' } |
  Select-Object ProcessId, CommandLine
Get-CimInstance Win32_Process -Filter "name='node.exe'" |
  Where-Object { $_.CommandLine -like '*vite*' } | Select-Object ProcessId

# 关闭
Get-CimInstance Win32_Process -Filter "name='java.exe'" |
  Where-Object { $_.CommandLine -like '*-jar*' -and $_.CommandLine -like '*core-api*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Get-CimInstance Win32_Process -Filter "name='node.exe'" |
  Where-Object { $_.CommandLine -like '*vite*' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

> 注意：**不要**误杀 IDE 的 Java 语言服务（命令行含 `redhat.java` 的 JRE）。

**② 清理自己产生的临时产物**（只清自己的）

- 临时 fat jar 副本（`backend/*-run.jar`、`tool-results/core-api-*.jar` —— 单个约 180 MB）
- 日志（`*.log`）、验收截图（`screenshots-*`）、`tool-results/` 下自己生成的一次性抓取/诊断产物

**③ 不要清理别人的产物**

- 其它会话正在使用的 jar / 实例；
- **刚打包好的正式产物**（如 `backend/core/api/core-api/target/core-api-0.3.17-exec.jar`）——删掉会打断他人验证，即使可以重新 `mvn package`；
- 拿不准就先问，**不要"顺手"删 `target/` 或别人的 jar**。

**背景（为什么要有这条）**：并行会话各自复制 fat jar 到 `backend/`、`tool-results/` 下运行，收尾前项目里残留了 **21 个临时 fat jar（3.7 GB）** 与十多个后台实例，造成磁盘与端口被大量占用、共享产物互相覆盖（表现为莫名的 `NoClassDefFoundError`、端口占用）。

---

## 八、验收标准（交付物）

### 8.1 交付前必须满足
- [ ] 会话收尾：自己启动的实例已全部关闭，自己产生的临时产物已清理（见 **7.4 会话收尾纪律**）。
- [ ] UI 一致性：新页面与参考页面截图对比，视觉差异 ≤ 5%（字体、间距、交互反馈）。
- [ ] 无冗余字段：该模块数据库表中不存在未引用的字段（已全部清理或迁移）。
- [ ] 架构自检通过：Six-Rung Ladder 六步全部合规。
- [ ] 文档溯源完整：每个涉及 Odoo 原生机制的代码块，注释中均附带官方文档链接或章节号。
- [ ] 安全无漏洞：所有用户输入经过 ORM 转义，所有 RPC 接口有权限校验。
- [ ] 性能达标：列表页 1000 条数据加载时间 < 2 秒（含关联字段预取）。

### 8.2 最终交付物
一个前后端风格统一、代码极致精简（YAGNI）、数据模型干净无冗余、安全防御到位的生产级企业管理系统。凡是我们比 Odoo 做得好的地方（如 UI 细节、业务流适配）必须保留优势，其余严格参照 Odoo 实现，绝不自创反模式。

---

## 九、数据与授权落位规则（2026-09-20 新增）

> 立此章的直接原因：审计中发现同一个 `tenant_id`、同一类"配置表"被多种口径混用，
> 导致跨租户读写、新租户看不到菜单、看板显示别家数据等一串问题。以下三条是**强制规则**。

### 9.1 租户语义：`0` 与 `1` 各自代表什么（不得混用）

| 值 | 含义 | 举例 |
|---|---|---|
| `tenant_id = 0` | **全局默认 / 租户初始化模板容器** —— 不是租户 | `sys_menu` 416 行、`sys_config` 173 行、`sys_permission`（全局码）、`erp_product_grade`/`erp_member_level`（等级字典）、`sys_tenant_package` |
| `tenant_id = 1` | **平台租户 SYSTEM**（`admin` 所在，`SYSTEM_TENANT_ID = 1L`） | 平台自身用户、`SUPER_ADMIN` 角色 |

**规则**
1. `tenant_id = 0` 的租户记录（若有）**不是真租户**，不得作为登录/切换目标；DB 中已有一条
   `tenant_mqd4cr9h`（status=0）属注册残留，应作废。
2. 全局表读取用 `@InterceptorIgnore(tenantLine = "true")` + **显式** `tenant_id = 0`，
   不要依赖"租户插件放行"（那会变成全租户可见）。
3. **严禁**在业务 SQL 里把租户写死成常量。定时任务等无会话上下文的场景，必须
   `MyBatisPlusConfig.setTempTenantId(...)` 逐租户设置并在 `finally` 中 `clearTempTenantId()`
   （ThreadLocal + 线程池复用，不清理会串租户）。
4. 新建的"租户初始化"能力，默认数据放 0 号容器，初始化时复制/派生到新租户 ——
   参考既有实现：`TenantRegistrationService#approve` → `PermissionTemplateService.applyTemplateToRole`。

### 9.2 参数落在哪张表（避免"能力重复"继续扩散）

| 参数类型 | 落位 | 读写口径 |
|---|---|---|
| 系统级参数（行业设置、平台配置、安全策略等） | `sys_config` | 全局默认行 `tenant_id = 0`；读 `tenant_id IN (0, 会话租户)` |
| DMS 配送业务参数（键前缀 `dms.*`） | `dms_config` | 同上（全局默认 + 租户覆盖） |
| 外部服务/密钥/通道配置 | 见《对标开发技术参考文档》§5.6 与各模块文档「配置落位」小节 | 按该章的四通道口径 |

**⚠️ 已知结构限制**：`sys_config` 目前是 `UNIQUE(param_key)`（**不含 tenant_id**），
**容不下"租户覆盖"** —— 租户行与全局行不能共存，`selectByKey` 永远命中全局行。
在改为 `UNIQUE(param_key, tenant_id) WHERE deleted = 0` 并把 `selectByKey` 改成"租户行优先"
之前，**不要**对外宣称配置支持租户覆盖。

### 9.3 对称模块必须同步改（采购 ↔ 销售）

以下接口逐个方法一一对应（`FUNCTION_DUPLICATE_AUDIT` §1.2 实测），
**任何一侧改审批流、单号规则、字段或校验，另一侧必须同步改**：

| 对称组 | 接口数 | 示例 |
|---|---|---|
| `order/*` | 11 | `PurchaseOrderController` ↔ `SaleOrderController` |
| `exchange/*` | 14 | `PurchaseExchangeController` ↔ `SaleExchangeController` |
| `return/*` | 10 | `PurchaseReturnController` ↔ `SaleReturnController` |
| `contract/*` | 5 | `ContractController`（CRM） ↔ `PurchaseContractController` |

对应表结构相似度 0.70–0.79，字段复制程度高，改一处漏一处是历史高发问题。

### 9.4 新增权限码的标准流程（顺序不可反）

1. **先写种子迁移**：`INSERT INTO sys_permission`，口径对齐同域既有码
   （`tenant_id=0, parent_id=0, permission_type=3, status=0, visible=1`），
   `id` 取实测空闲段（不要用序列，也不要猜）。
2. **同时关联超管角色**：本仓超管权限**不是**硬编码放行，而是来自 `sys_role_permission`
   （`StpInterfaceImpl#getPermissionList` → `permissionCacheService.getPermissions`）。
   沿用既有写法：
   ```sql
   INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
   SELECT 9400000 + (p.sort - <基准>), 1, p.id, 1, now()
   FROM sys_permission p
   WHERE p.permission_code IN (...)
     AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                     WHERE rp.role_id = 1 AND rp.permission_id = p.id);
   ```
3. **最后才加注解**（`@SaCheckPermission`）。
   > 铁律来历：曾有 90 个注解引用了库中不存在的权限码，导致**所有非超管用户全 403**。
4. 平台专属能力（如租户菜单授权）不能只靠权限码，**必须加身份硬校验**
   （`MyBatisPlusConfig.isTenantScopeExempt()`），因为权限码可被授予租户角色。
