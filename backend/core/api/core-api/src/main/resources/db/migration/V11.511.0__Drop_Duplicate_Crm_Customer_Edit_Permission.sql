-- 2026-09-26 删除与 crm:customer:update 完全同义的重复权限码 crm:customer:edit
--
-- 问题：同一个动作（编辑客户）在库里有两个码
--   · `crm:customer:edit`  —— 名称「CRM客户编辑」，`api_path`/`method` 均为空，**后端从未使用**
--   · `crm:customer:update` —— 名称「客户更新」，`api_path=/api/customer/*`，`method=PUT`，**后端真实守卫**
-- 而前端客户列表的「编辑」按钮此前挂的是 `edit` ⇒ 典型的**假门**：
-- 持 `crm:customer:update` 的角色（如新授码的 DEPT_ADMIN）看不到编辑按钮，
-- 持 `crm:customer:edit` 的角色能看到按钮、点下去却由后端按 `update` 判定。
--
-- 前端已改为 `crm:customer:update`（2026-09-26），本迁移删掉这个重复码。
--
-- 与「暂不删除的其它无后端支撑码」的区别：那些（`crm:customer:addtolevel`、`crm:customer:batchassign`、
-- `crm:lead:assign`、`crm:lead:batchassign`、`crm:opportunity:move`、`crm:quotation:batchsend`、
-- `crm:quotation:sendfromdetail`、`crm:quotation:convertfromdetail`）是**独立业务动作**，
-- 只是暂时复用粗粒度端点，属「待拆独立端点」；而 `edit` 与 `update` 是**同一个动作的两个名字**，
-- 不存在将来分家的可能，留着只会再次被误用。
--
-- 前置已核：全仓（前端 / 后端 / tools / 其它迁移）对 `crm:customer:edit` 的引用为 0 处。

DELETE FROM sys_role_permission
 WHERE permission_id IN (SELECT id FROM sys_permission WHERE permission_code = 'crm:customer:edit');
DELETE FROM sys_permission WHERE permission_code = 'crm:customer:edit';

-- ──────────────────────────────────────────────────────────────────────────
-- 二、同步客户控制器的前缀变更（代码侧：`/api/customer` → `/api/crm/customer`）
-- ──────────────────────────────────────────────────────────────────────────
-- `CustomerController` 此前是 CRM 里**唯一**不守 `/api/crm/*` 约定的控制器
-- （另外 9 个都在 `/api/crm/` 下）。运行时无冲突，但会让「URL 前缀 → 模块」类工具
-- 推出 `customer:*` 而非 `crm:customer:*`，属维护性陷阱，已统一。
-- 权限码本身的 `permission_code` 不变（一直是 crm:customer:*），只订正登记用的 `api_path`。
UPDATE sys_permission SET api_path = REPLACE(api_path, '/api/customer', '/api/crm/customer')
 WHERE api_path LIKE '/api/customer%';

-- 自检：期望 0
--   SELECT count(*) FROM sys_permission WHERE permission_code IN ('crm:customer:edit');
--   SELECT count(*) FROM sys_permission WHERE api_path LIKE '/api/customer%';
