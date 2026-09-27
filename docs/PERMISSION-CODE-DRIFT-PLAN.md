# 前端 v-permission 权限码漂移：现状、已修与待决方案

> 2026-09-27 取证。工具：`tools/plan-permission-code-fix.py`（只读，出方案）、
> `tools/audit-permission-codes.py`（后端侧对账）。

## 一、为什么这件事归"系统级"

用户明确的授权三层模型：

1. **系统模块是系统级的**；其余模块是租户级的；
2. 但**需要系统给租户授予模块使用权**——租户不能凭空获得一个模块；
3. 租户内，**租户超管**给自己的员工与角色做权限控制。

⇒ **"权限码在 `sys_permission` 里不存在"属于系统级定义缺失**：租户侧无权也无力新增，
必须先由系统层把权限点定义出来，才谈得上给谁授。所以本文只出方案，不动库。

## 二、事实（2026-09-27 实测）

| 项 | 数值 |
|---|---|
| 后端 `@SaCheckPermission` 引用的码 | 1712 个，**missing_in_db = 0**（后端侧干净） |
| `sys_permission` 有效码 | 1821 个 |
| 前端 `v-permission` 用到的码 | 282 个 |
| **前端用、库里没有** | 起初 **107** → 已修 33 处 + 删 7 个本就不该有的 gate → **剩 67** |

机制：`v-permission` 校验不到就 **removeChild**（不是置灰）⇒ 码不存在 = 按钮
**对所有人隐藏且不报错**（管理员也看不见）。

## 三、已修（33 处，纯前端，未动库）

规则只认**同一动作的不同叫法**与**多余的命名空间**，绝不跨功能点猜：

| 类 | 例 | 处数 |
|---|---|---|
| 动词同义 | `supplier:add → supplier:create`、`finance:payment:edit → finance:payment:update`、`wms:event:process → wms:event:execute` | 10 |
| 多了一层命名空间 | `erp:fixed-asset:asset:list → fixed-asset:asset:list`（19 条）、`erp:mall:product:edit → mall:product:update`、`erp:stock:export → stock:export` | 21 |

## 四、待决 67 个，分三类

### A. 前端命名空间/动作词对不上，**改前端即可，不动库**（约 30 个）

库里都有**真实命名空间**，前端用的是自造或旧的：

| 前端在用 | 库里的真实命名空间 |
|---|---|
| `erp:stock:*`（5） | `stock:` **106 个** |
| ~~`erp:partner:*`（4）~~ 已改 3 条 → `party:create/update/delete`；剩 `toggle-status` 需定 | `party:` **45 个** |
| `erp:sales:*`（4） | `sale:` **102 个** |
| `erp:shipment:*`（9） | 待定（可能是 `dms:` 103 个 / `sale:`） |
| `order:center:*`（8） | 待定（可能是 `sale:order:*`） |
| `budget:plan:*`（4） | 待定（可能是 `budget:adjustment:*` 36 个） |
| `wms:wave:*`（5） | 待定（`wms:wave` 库里 0 个，波次模块的命名空间需确认） |

**为何不自动改**：这些码的**子模块名与动作词都与库里的不同**（如 `erp:stock:inbound`
对应的可能是 `stock:inbound:create`），保留路径的规则匹配不到；而"猜一个不同子模块"
的规则会把 `budget:plan:view` 猜成 `budget:report:view`、`erp:serial:export` 猜成
`erp:batch:export`——**猜错等于把 A 功能的 gate 换成 B 功能的权限**，比不修更糟。
（这条规则我写了又删了，记在脚本注释里。）

### B. 功能点确实缺码 → 需**系统级补 `sys_permission`**（约 30 个）

| 模块 | 缺的码 | 说明 |
|---|---|---|
| `sale:return-doc` | `print` | **其他 5 种销售单据都有 `sale:*:print`（订单/出库/换货/预订/零售），只差退货单** |
| `finance:receivable` | `writeoff`、`baddebt` | 核销/坏账是真实业务动作 |
| `finance:payable` | `writeoff` | 同上 |
| `finance:pre-payment` | `offset`、`recover`、`refund` | 预收冲抵/收回/退款 |
| `finance:payment` / `finance:receipt` | `cancel`、`complete` | |
| `finance:offset` | `view`、`edit`、`delete` | |
| `finance:deposit` / `finance:writeoff` | `view` / `edit` | |
| `supplier:inquiry` | `acceptquotation`、`rejectquotation` | 询价答复 |
| `supplier:performance` | 整块（`list`/`back`/`evaluate`） | 绩效模块 |
| `supplier:portal` | 整个 | 供应商门户 |
| `erp:serial` | `delete`、`export` | 序列号 |
| `erp:batch` | `delete` | |
| `erp:product` | `inventory-mode` | 库存模式设置 |
| `fixed-asset:asset/depreciation/purchase/report` | `depreciate`、`calculate`、`accept`、`query` | 命名空间已对（改过），缺的是这几个动作 |

### C. ~~这些"gate"本就不该存在~~ → **已删除（7 个，2026-09-27 执行）**

| 码 | 为什么不是权限点 |
|---|---|
| `profile:view:changepassword` / `:openeditprofile` / `:savepreferences` | 个人中心的改密码/改资料/存偏好——**自己的东西**，不是权限 |
| `notification:view:markread` / `:markallread` / `:refresh` / `:resetfilters` | 标记已读/刷新/重置筛选——纯 UI 动作 |

（这三条也符合业界做法：个人资料与自己的通知不需要管理员授权。）

## 五、建议的处置顺序

1. **先做 C**（7 个）：删掉这些 gate，按钮立刻对所有人可见且语义正确 —— 零风险、零库改动。
2. **再做 A**（约 30 个）：逐页把命名空间换成真实的、动作词对齐该模块既有码。
   我需要逐页确认目标码（可以按模块批量确认，每模块一次）。
3. **最后做 B**（约 30 个）：这批要**改库**（补 `sys_permission` 权限点 + 按你的模型
   给系统侧/租户侧分配）。等你点头我再动，方案会写成可回滚的迁移脚本。

## 六、复现方式

```bash
python tools/plan-permission-code-fix.py            # 出方案（只读）
python tools/plan-permission-code-fix.py --json out.json
python tools/audit-permission-codes.py              # 后端侧对账（只读）
```
