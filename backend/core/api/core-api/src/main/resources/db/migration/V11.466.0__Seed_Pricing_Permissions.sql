-- 定价域权限码种子（E-01 erppricing 批次，2026-09-21）
--
-- 【零码模块】库中**没有任何 `pricing:` 前缀的码**（实测 0 条）。前端
--   `views/erp/pricing/approval/index.vue:134,210,218` 用到的三个字符串
--   `pricing:approval:{create,approve,reject}` 只是前端字符串，库里并不存在。
--   故本迁移按 E-04 口径**现场建码**：42 个待补端点 → 19 个码，**全部新增、无命中**，
--   VALUES 占 117000~117018。
--
-- 【为什么必须补】E-01 要给定价域裸控制器补 @SaCheckPermission，而
--   **没有码就补注解 ⇒ 该接口对所有非超管一律 403**（本仓铁律/历史事故）。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['erppricing']`：
--   · 域统一 `pricing`。三个类级路径首段带连字符且没有资源段（`/api/v1/price-engine`、
--     `/api/v1/price-engine/tiers`、`/api/v1/price-strategy/config`）⇒ 用 base_overrides
--     指定资源名 engine/tier/strategy，否则会推成 `price-engine:`、`price-strategy:`
--     两个**新的一级域**，并拿方法路径首段当资源（`pricing:calculate:*`、`pricing:health:*`）。
--   · `/api/erp/pricing/approval` 无需 override —— 路径推导即 `pricing:approval`，
--     与本域前端的三个既有字符串逐字一致（授权后那三个按钮即可显示）。
--   · 价格引擎（`/api/v1/price-engine`）是**纯计算 API**（不落库）：全部 POST 归 `view`，
--     只有刷缓存归 `execute`；策略配置的解析/格式化归 `view`、校验类归 `check`
--     （这些 POST 若落 RULES 的 `create` 兜底，会变成"能新建的人才能校验配置"）。
--   · `/api/erp/product-grade-price` 的域按**路径**推导为 `product`（不是 pricing）——
--     与它的兄弟控制器 `ProductGradeController`（erp-stock 里 `/api/erp/product-grade`）
--     的历史码 `product:grade:*` 同族，保持一致；批量保存是"全量覆盖改"⇒ update。
--
-- 【id 号段】权限码 117000 起（工具槽位 slot=17）、角色关联 9670000 起。
--   实测 117000~117999 = 0 行、9670000~9679999 = 0 行，均空闲。
--
-- 【本批未能覆盖的端点 —— 8 个，且**不是有意排除**】
--   · `PriceApprovalController`（`/api/erp/pricing/approval` 的 8 个端点）整类未进入本批。
--     原因是**生成器的既有缺陷**（只报告、本批不修）：该文件 Javadoc 第 43 行写着
--     「不加 `{@code @SaCheckPermission}`」这句**注释**，而 `scan()` / `apply_annotations()`
--     用 `PERM_ONLY.search(text.split('class ')[0])` 判断"类级是否已有权限注解" ——
--     它不区分注释与真注解 ⇒ 整类被判成"已覆盖"而跳过。
--     后果：这 8 个端点本批**仍然裸奔**（登录即可访问），基线清单里的
--     `cn.aiedge.erp.pricing.controller.PriceApprovalController` 一行**必须保留**（不可删）。
--     ⚠️ 该缺陷是 fail-open（漏补），不是 fail-closed，不会造成 403；但下一个模块会再踩。
--
-- 【遗留 / 拿不准】
--   · `/api/v1/price-engine/**` 与 `/api/v1/price-strategy/config/**` 在 pc-admin 里
--     **零调用方**（实测 grep 无命中），疑似给外部/未来接入的 API。本批照样建码
--     （一旦接线即生效），但码的"动作粒度"是按当前语义定的，若将来接线方式不同需复核。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (117000, '定价引擎执行', 'pricing:engine:execute', '/api/v1/price-engine/cache/refresh', 'POST', 1200),
 (117001, '定价引擎查询', 'pricing:engine:list', '/api/v1/price-engine/history/{productId}', 'GET', 1201),
 (117002, '定价引擎查看', 'pricing:engine:view', '/api/v1/price-engine/calculate', 'POST', 1202),
 (117003, '定价取价新增', 'pricing:price:create', '/api/erp/pricing/price-memory', 'POST', 1203),
 (117004, '定价取价详情', 'pricing:price:detail', '/api/erp/pricing/price-memory/latest/{productId}/{customerId}', 'GET', 1204),
 (117005, '定价取价查询', 'pricing:price:list', '/api/erp/pricing/price-memory/page', 'GET', 1205),
 (117006, '定价取价编辑', 'pricing:price:update', '/api/erp/pricing/configs/{id}', 'PUT', 1206),
 (117007, '定价取价查看', 'pricing:price:view', '/api/erp/pricing/configs', 'GET', 1207),
 (117008, '定价策略校验', 'pricing:strategy:check', '/api/v1/price-strategy/config/validate', 'POST', 1208),
 (117009, '定价策略查看', 'pricing:strategy:view', '/api/v1/price-strategy/config/parse/json', 'POST', 1209),
 (117010, '定价价层新增', 'pricing:tier:create', '/api/v1/price-engine/tiers/tier', 'POST', 1210),
 (117011, '定价价层删除', 'pricing:tier:delete', '/api/v1/price-engine/tiers/tier/{tierId}', 'DELETE', 1211),
 (117012, '定价价层查询', 'pricing:tier:list', '/api/v1/price-engine/tiers/tiers', 'GET', 1212),
 (117013, '定价价层编辑', 'pricing:tier:update', '/api/v1/price-engine/tiers/tier/{tierId}', 'PUT', 1213),
 (117014, '定价价层查看', 'pricing:tier:view', '/api/v1/price-engine/tiers/calculate', 'POST', 1214),
 (117015, '定价等级价新增', 'product:grade-price:create', '/api/erp/product-grade-price', 'POST', 1215),
 (117016, '定价等级价删除', 'product:grade-price:delete', '/api/erp/product-grade-price/{id}', 'DELETE', 1216),
 (117017, '定价等级价查询', 'product:grade-price:list', '/api/erp/product-grade-price/by-product/{productId}', 'GET', 1217),
 (117018, '定价等级价编辑', 'product:grade-price:update', '/api/erp/product-grade-price/batch-save', 'POST', 1218)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9670000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 117000 AND 117019
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
