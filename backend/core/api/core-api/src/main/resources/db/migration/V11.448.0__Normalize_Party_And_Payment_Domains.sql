-- =============================================================================
-- 域前缀归一：往来单位统一到 party、ERP 付款单归入 finance（2026-09-21）
--
-- 【为什么】E-04 补码时按**类级路由路径**推导域，暴露出两处「同名不同物 / 同名两地」，
--   现在改最便宜（码刚落地、除本轮注解外无其它引用）；等前端授权矩阵与角色配置用起来
--   再改，就要连角色授权数据一起迁。
--
-- 【问题 1：`partner` 与 `party` 是同一业务对象被拆到两个域】
--   · `/api/erp/party`      → `party:*`   （PartyController，主档 biz_party）
--   · `/api/erp/partner/*`  → `partner:*` （同对象的子表：contacts/roles/attachments/categories/grades）
--   两者在权限矩阵里会显示成两个不相干的模块，实际是同一套主数据。
--   代码/表/包的主词汇是 `biz_party` / `PartyController` / `cn.aiedge.erp.party`，
--   故统一到 **`party`**。历史遗留的 `partner:merge`（往来单位合并）一并改名 ——
--   实测全仓（backend + frontend）**零引用**，改名无影响。
--
-- 【问题 2：`payment` 这个域历史上指「第三方支付网关」，被误用于「ERP 付款单」】
--   · 历史 `payment:*`（9 个，id 91301-91308/91577）对应 `/api/payment/*`、`/api/payment/config/*`、
--     `/api/reconciliation/*`、`/api/refund/*` —— **核心支付模块**的渠道配置与支付请求。
--   · 本轮的 `/api/erp/payment`（PaymentController）是 **ERP 付款单**，属财务域，
--     与 `finance:receipt` / `finance:pre-payment` 同类。
--   两者共用 `payment` 会让矩阵出现「支付-查询」这种指代不明的条目。
--   ⇒ 后者改为 **`finance:payment:*`**；历史 9 个 `payment:*` **保持不动**。
--
-- 【做法】`UPDATE permission_code` 而不是删旧建新 —— 保留 id 与 `sys_role_permission` 关联，
--   已授权给角色的配置不会丢。改名后需同步改控制器里 `@SaCheckPermission` 的字面量（同批次代码改动）。
--
-- 【防撞】改名目标若已存在同码行则跳过（本仓 `permission_code` 无唯一约束，
--   盲改会造出同码两行）。改完由迁移末尾的自检查断言「无同码多行」。
-- =============================================================================

-- ── 1. 往来单位：partner:* → party:*（保留资源名，只换域） ──
UPDATE sys_permission p
SET permission_code = 'party:' || substring(p.permission_code from 9)
WHERE p.permission_code LIKE 'partner:%'
  AND NOT EXISTS (SELECT 1 FROM sys_permission q
                  WHERE q.deleted = 0
                    AND q.permission_code = 'party:' || substring(p.permission_code from 9));

-- ── 2. ERP 付款单：payment:<动作> → finance:payment:<动作> ──
--    只改**两段式**的（`payment:list` 这种，即没有资源名的那批），
--    历史 `payment:channel:list`、`payment:request:create` 是三段式，**不动**。
UPDATE sys_permission p
SET permission_code = 'finance:payment:' || split_part(p.permission_code, ':', 2)
WHERE p.id BETWEEN 100000 AND 109999
  AND p.permission_code LIKE 'payment:%'
  AND array_length(string_to_array(p.permission_code, ':'), 1) = 2
  AND NOT EXISTS (SELECT 1 FROM sys_permission q
                  WHERE q.deleted = 0
                    AND q.permission_code = 'finance:payment:' || split_part(p.permission_code, ':', 2));

-- ── 3. 自检：不得出现同码多行 ──
DO $$
DECLARE dup int;
BEGIN
    SELECT count(*) INTO dup
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup > 0 THEN
        RAISE EXCEPTION '域名归一后出现 % 组同码多行，请人工核查', dup;
    END IF;
END $$;
