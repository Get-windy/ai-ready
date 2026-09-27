# 财务域权限授予方案（2026-09-27 · 待拍板）

> 背景：`FINANCE_MODULE_AUDIT_20260923.md` §3.7 记录「财务权限配好了但落不到人」。
> 菜单侧的问题**已闭环**（`menu_level=3` 已全部改为 0，财务 39 条菜单普通租户可见），
> 但**权限侧仍是主要缺口**。本方案只改**权限授予数据**（`sys_role_permission`），不动代码、不动菜单。

---

## 一、现状（2026-09-27 实测）

财务域权限码（`finance:%` / `receipt:%` / `payment:%` / `budget:%`）共 **208 个**，按「持有它的非超管角色数」分布：

| 非超管角色数 | 码数 | 含义 |
|---|---|---|
| **0** | **156** | **仅 SUPER_ADMIN 持有** —— 非超管完全不可用 |
| 1 | 28 | |
| 2 | 24 | |

现有角色（`sys_role`）：

| role_code | 角色名 | 权限总数 | 其中财务码 |
|---|---|---|---|
| SUPER_ADMIN | 超级管理员 | 1850 | 208（全部） |
| SYSTEM_ADMIN | 系统管理员 | 783 | 52 |
| DEPT_ADMIN | 部门管理员 | 332 | 24 |
| E2E_T2_ADMIN | E2E 租户2管理员 | 16 | 0 |
| ROLE_MQD2X2DI | （空角色） | 0 | 0 |

**156 个「仅超管」的码覆盖了全部核心业务**（抽样）：

| 资源 | 码数 | 动作 |
|---|---|---|
| `budget:annual` | 11 | approve, close, create, delete, detail, execute, export, list, print, submit, update |
| `finance:payment` | 9 | approve, create, delete, detail, export, list, submit, update, view |
| `finance:receipt` | 8 | approve, create, delete, detail, list, submit, update, view |
| `finance:voucher` | 7 | **audit, create, delete, edit, post, reverse, view** |
| `finance:receivable` | 7 | analysis, bad-debt, create, delete, payment, view, write-off |
| `finance:cash-transfer` | 7 | cancel, confirm, create, delete, detail, list, view |
| `finance:expense-doc` | 7 | cancel, confirm, create, delete, detail, list, view |
| `finance:ar-ap-adjust` | 6 | cancel, confirm, create, delete, detail, list |
| … | … | （其余同理，含预算调整/模板/科目、应收应付、预收预付、资金流水、辅助核算等） |

**后果**：非超管登录后，财务菜单**能看见**（菜单已修），但点进去**任何写操作都 403**；
甚至**查看**也多数不可用（`*:view` / `*:list` 也在仅超管集合里）。

---

## 二、方案 A（最小改动 · 建议先做）

**只做「接线」**：把**只读类**码授给两个现有功能角色，不动审批类。
**不动** `*:approve` / `*:audit` / `*:post` / `*:reverse`（审批与过账权，涉职责分离，留待方案 B）。

| 授予对象 | 授予范围 |
|---|---|
| `DEPT_ADMIN`（部门管理员） | 财务域全部 **只读类**：`*:view` / `*:list` / `*:detail` / `*:export` |
| `SYSTEM_ADMIN`（系统管理员） | 同上**只读类** |

**收益**：非超管至少能**查看**财务数据（报表、账簿、单据列表），不再全盘 403。
**风险**：低（只读，无写权限、无审批权）。
**不解决**：非超管仍**不能制单**（create/update）—— 若业务上需要"会计制单"，需方案 B。

**SQL（幂等，可重复执行）**：

```sql
-- 授「只读类」财务码给 DEPT_ADMIN 与 SYSTEM_ADMIN
-- ⚠️ 主键用「当前 max(id) + row_number()」动态生成；不要硬编码 id
--    （本仓已两次踩坑：V11.496.0 / V11.512.0 都是硬编码 id 撞 sys_permission_pkey / sys_role_permission_pkey）
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time, create_by)
SELECT (SELECT COALESCE(MAX(id), 0) FROM sys_role_permission) + ROW_NUMBER() OVER (ORDER BY r.id, p.id),
       r.id, p.id, 1, NOW(), 1
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('DEPT_ADMIN', 'SYSTEM_ADMIN')
  AND r.deleted = 0
  AND p.deleted = 0
  AND (p.permission_code LIKE 'finance:%' OR p.permission_code LIKE 'receipt:%'
       OR p.permission_code LIKE 'payment:%' OR p.permission_code LIKE 'budget:%')
  AND (p.permission_code LIKE '%:view' OR p.permission_code LIKE '%:list'
       OR p.permission_code LIKE '%:detail' OR p.permission_code LIKE '%:export')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission x WHERE x.role_id = r.id AND x.permission_id = p.id
  );
```

**执行后验证**：
```sql
-- 期望：DEPT_ADMIN / SYSTEM_ADMIN 的财务码数量显著上升
SELECT r.role_code, COUNT(*) FILTER (WHERE p.permission_code ~ '^(finance|receipt|payment|budget):') AS finance_codes
FROM sys_role r
JOIN sys_role_permission rp ON rp.role_id = r.id
JOIN sys_permission p ON p.id = rp.permission_id
GROUP BY r.role_code ORDER BY 2 DESC;
```

---

## 三、方案 B（标准财务岗责 · 需业务确认）

若业务上需要**非超管制单**，建议按岗责建角色并落实**职责分离（SoD）**：

| 角色（建议新建） | 定位 | 授予范围 | 关键约束 |
|---|---|---|---|
| **财务主管** | 审核/过账 | `*:approve` `*:audit` `*:post` `*:reverse` + 全部只读 | ⚠️ **不得含** `*:create` / `*:update`（SoD：审批人 ≠ 制单人） |
| **会计** | 制单 | `*:create` `*:update` `*:submit` + 全部只读（凭证、收付款、费用、调整、预算） | ⚠️ **不得含** `*:approve` / `*:audit` / `*:post` |
| **出纳** | 资金收付 | `finance:receipt:*` `finance:payment:*` `finance:cash-transfer:*` + 只读 | ⚠️ 不得含 `finance:voucher:audit` |
| **预算管理员** | 预算 | `budget:*` + 只读 | |

**为什么必须分权（SoD）**：`FINANCE_MODULE_AUDIT` 已确认 `sys_sod_rule` **0 行**（职责分离零落地）。
若把 `create` 与 `approve` 授给同一角色，等于**同一人可自制自审**，是审计上的红线。

**前置条件**：
1. 需业务确认岗位划分（是否真有"会计/出纳/财务主管"这些角色）；
2. 若新建角色，需同步设计**数据权限**（目前 `sys_role.data_scope` 全为 0，即无行级数据范围）。

---

## 四、方案对比

| | 方案 A（最小） | 方案 B（岗责） |
|---|---|---|
| 改动 | 1 条幂等 SQL | 新建 4 个角色 + 分权 SQL + 数据权限设计 |
| 非超管可查看财务数据 | ✅ | ✅ |
| 非超管制单 | ❌ 仍不可 | ✅ |
| SoD 合规 | 不涉及（无审批权） | ✅ 按岗责分离 |
| 风险 | 低 | 中（角色模型变更，影响面大） |
| 建议 | **先做** | 待业务确认后做 |

---

## 五、回滚

两次改动都只**新增** `sys_role_permission` 行，不删不改。回滚 =
```sql
-- 按 create_time 回滚本次新增（执行前先记下时间点）
DELETE FROM sys_role_permission WHERE create_time >= '<执行时间>' AND create_by = 1;
```
> ⚠️ 务必先备份：`CREATE TABLE sys_role_permission_backup_YYYYMMDD AS SELECT * FROM sys_role_permission;`

---

## 六、附：另需处理的两件小事（与本方案无依赖）

1. **5 个僵尸权限码**仍在库中（`finance:other-income-doc:approve`、`finance:receivable:analysis`、
   `finance:receivable:payment`、`payment:account:select`、`receipt:account:select`）——
   后端 `.java` 与前端 `src` 零引用，属可「下线」项；其中 `finance:other-income-doc:approve`
   **当前还被 DEPT_ADMIN / SYSTEM_ADMIN 真实持有**，删除前需确认。
2. **空角色 `ROLE_MQD2X2DI`**（0 权限）与 `E2E_T2_ADMIN`（16 个非财务码）—— 疑似造数残留，建议确认后清理。

---

**待你拍板**：先执行**方案 A**，还是直接上**方案 B**（需要你先给岗位划分）？
