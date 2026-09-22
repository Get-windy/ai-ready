-- 商城（mall）权限码种子 —— **仅后台管理侧**（E-01 b2b 批次，2026-09-21）
--
-- 【与本批其它种子的区别】b2b 此前在生成器里标着 `manual_only`，理由是
--   「`/api/v1/mall/**` 是 C 端公开接口，自动补注解会把商城顾客挡在门外」。
--   本批**保留该结论，但把范围收窄到 C 端**：
--     · C 端（7 个 `/api/v1/mall/**` 控制器 + 商城端公告 `/api/erp/mall/notice`）**整类排除**，一码不建；
--     · 后台管理侧（`/api/erp/mall/admin/**`）**正常补码**，即本文件。
--
-- 【为什么 C 端必须排除（证据）】`MallAuthServiceImpl#login` 用的是
--   `StpUtil.login(shopUser.getId())` —— 登录主体是 **shop_user（商城买家）**，不是 sys_user；
--   `@SaCheckPermission` 按登录 id 去查该主体的角色/权限（本仓实现只认 sys_user +
--   sys_role_permission）⇒ shop_user 查不到任何码，补注解 = **所有商城顾客（含已登录买家）
--   一律 403**。其中 `/api/v1/mall/auth/**`、`/api/v1/mall/products/**` 还写在
--   `SaTokenConfig` 的**两处** excludePathPatterns 里（未登录也必须可达）。
--
-- 【为什么后台侧必须补】`/api/erp/mall/admin/**` 是 pc-admin 的员工会话在调
--   （`frontend/apps/pc-admin/src/api/erp/mall.ts`），而它今天只被全局
--   `StpUtil.checkLogin()` 保护 —— 商城顾客同样持有 Sa-Token 会话 ⇒ 现状是
--   「任何 C 端买家都能调商城后台管理接口」。补码顺带把这个既有越权面收掉。
--
-- 【口径要点】全部写在 `tools/gen-module-permission-seed.py` 的 `MODULES['b2b']`：
--   · 域沿用库里既有的 `mall:`（`sys_module_permission` 里 `mall:` → trade 模块，
--     故本批**不需要**新增前缀映射行）；
--   · `MallAdminController` 是"一个控制器管七种对象"（配置/买家/轮播图/模板/装修/商品/订单），
--     故用 `code_rules` 逐条给**完整码**，不落成 `mall:admin:*`（否则"能看商城配置的人
--     就能看订单统计"）；买家账号统一走 `mall:user:*`；审核驳回与通过共用一个 approve 码；
--   · 订单的收款/发货/退款/终止分别对齐动作词 `payment` / `ship` / `refund`（本批新增词）/
--     `cancel`，不新造 receive / terminate 等同义码；
--   · 三个类级路径没有资源段的管理控制器（keyword / notice / popup-ad）用 `base_overrides`
--     指定资源名，否则会产出 `mall:admin-keyword:*` 这种把容器词当资源名的碎片码。
--
-- 【只补不删 / 复用】60 端点 → 47 码，**全部新增**（库里 `mall:` 只有 5 条 `mall:tag:*`，
--   挂的是另一个控制器 `/api/erp/mall-tag`，与本批零交集）。本文件的 NOT EXISTS 守卫
--   保证重复执行不产生第二行，也保证将来若有人先建了同码不会被覆盖。
--
-- 【本批有意排除的端点 —— 共 53 个】`/api/v1/mall/{auth(6), products(8), cart(8), orders(9),
--   party-link(11), payments(3), user(7)}` = 52 个，加 `/api/erp/mall/notice`(1)。
--   理由：调用主体是 C 端顾客/游客（shop_user 登录），没有任何权限码，补码即整块 403。
--
-- 【id 号段】权限码 **108000 起**（复用既有槽位 slot=8）、角色关联 **9580000 起** ——
--   执行前实测两段 count(*) = 0（b2b 此前 manual_only，从未占用过该槽位）。
--   9xxxx 段已被 V11.42x 系列的短块占满，故本批的**新模块**另开 126000~130000 段（见 V11.478.0 起）。
--
-- 【遗留（只报告，不修）】商城端公告 `/api/erp/mall/notice` 的类注释自称"免登录"，
--   但它**不在** SaTokenConfig 白名单里 —— 与打印客户端登录同类（设计意图与实现不一致）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (108000, '商城轮播图新增', 'mall:banner:create', '/api/erp/mall/admin/banner', 'POST', 1200),
 (108001, '商城轮播图删除', 'mall:banner:delete', '/api/erp/mall/admin/banner/{id}', 'DELETE', 1201),
 (108002, '商城轮播图查询', 'mall:banner:list', '/api/erp/mall/admin/banner', 'GET', 1202),
 (108003, '商城轮播图编辑', 'mall:banner:update', '/api/erp/mall/admin/banner/{id}', 'PUT', 1203),
 (108004, '商城配置编辑', 'mall:config:update', '/api/erp/mall/admin/config', 'PUT', 1204),
 (108005, '商城配置查看', 'mall:config:view', '/api/erp/mall/admin/config', 'GET', 1205),
 (108006, '商城装修新增', 'mall:decoration:create', '/api/erp/mall/admin/decoration', 'POST', 1206),
 (108007, '商城装修删除', 'mall:decoration:delete', '/api/erp/mall/admin/decoration/{id}', 'DELETE', 1207),
 (108008, '商城装修详情', 'mall:decoration:detail', '/api/erp/mall/admin/decoration/{id}', 'GET', 1208),
 (108009, '商城装修查询', 'mall:decoration:list', '/api/erp/mall/admin/decoration', 'GET', 1209),
 (108010, '商城装修编辑', 'mall:decoration:update', '/api/erp/mall/admin/decoration/{id}', 'PUT', 1210),
 (108011, '商城关键词新增', 'mall:keyword:create', '/api/erp/mall/admin/keyword', 'POST', 1211),
 (108012, '商城关键词删除', 'mall:keyword:delete', '/api/erp/mall/admin/keyword/{id}', 'DELETE', 1212),
 (108013, '商城关键词查询', 'mall:keyword:list', '/api/erp/mall/admin/keyword/page', 'GET', 1213),
 (108014, '商城关键词状态', 'mall:keyword:status', '/api/erp/mall/admin/keyword/{id}/status', 'PUT', 1214),
 (108015, '商城关键词编辑', 'mall:keyword:update', '/api/erp/mall/admin/keyword/{id}', 'PUT', 1215),
 (108016, '商城公告新增', 'mall:notice:create', '/api/erp/mall/admin/notice', 'POST', 1216),
 (108017, '商城公告删除', 'mall:notice:delete', '/api/erp/mall/admin/notice/{id}', 'DELETE', 1217),
 (108018, '商城公告详情', 'mall:notice:detail', '/api/erp/mall/admin/notice/{id}', 'GET', 1218),
 (108019, '商城公告查询', 'mall:notice:list', '/api/erp/mall/admin/notice/page', 'GET', 1219),
 (108020, '商城公告发布', 'mall:notice:publish', '/api/erp/mall/admin/notice/{id}/publish', 'PUT', 1220),
 (108021, '商城公告编辑', 'mall:notice:update', '/api/erp/mall/admin/notice/{id}', 'PUT', 1221),
 (108022, '商城订单审批', 'mall:order:approve', '/api/erp/mall/admin/order/{id}/approve', 'PUT', 1222),
 (108023, '商城订单取消', 'mall:order:cancel', '/api/erp/mall/admin/order/{id}/terminate', 'POST', 1223),
 (108024, '商城订单详情', 'mall:order:detail', '/api/erp/mall/admin/order/{id}', 'GET', 1224),
 (108025, '商城订单查询', 'mall:order:list', '/api/erp/mall/admin/order/page', 'GET', 1225),
 (108026, '商城订单收款', 'mall:order:payment', '/api/erp/mall/admin/order/{id}/pay', 'POST', 1226),
 (108027, '商城订单退款', 'mall:order:refund', '/api/erp/mall/admin/order/{id}/refund', 'POST', 1227),
 (108028, '商城订单发货', 'mall:order:ship', '/api/erp/mall/admin/order/{id}/ship', 'POST', 1228),
 (108029, '商城订单查看', 'mall:order:view', '/api/erp/mall/admin/order/stats', 'GET', 1229),
 (108030, '商城弹窗广告新增', 'mall:popup-ad:create', '/api/erp/mall/admin/popup-ad', 'POST', 1230),
 (108031, '商城弹窗广告删除', 'mall:popup-ad:delete', '/api/erp/mall/admin/popup-ad/{id}', 'DELETE', 1231),
 (108032, '商城弹窗广告详情', 'mall:popup-ad:detail', '/api/erp/mall/admin/popup-ad/{id}', 'GET', 1232),
 (108033, '商城弹窗广告查询', 'mall:popup-ad:list', '/api/erp/mall/admin/popup-ad/page', 'GET', 1233),
 (108034, '商城弹窗广告发布', 'mall:popup-ad:publish', '/api/erp/mall/admin/popup-ad/{id}/publish', 'POST', 1234),
 (108035, '商城弹窗广告编辑', 'mall:popup-ad:update', '/api/erp/mall/admin/popup-ad/{id}', 'PUT', 1235),
 (108036, '商城商品新增', 'mall:product:create', '/api/erp/mall/admin/product', 'POST', 1236),
 (108037, '商城商品删除', 'mall:product:delete', '/api/erp/mall/admin/product/{id}', 'DELETE', 1237),
 (108038, '商城商品查询', 'mall:product:list', '/api/erp/mall/admin/product/page', 'GET', 1238),
 (108039, '商城商品编辑', 'mall:product:update', '/api/erp/mall/admin/product/{id}', 'PUT', 1239),
 (108040, '商城模板新增', 'mall:template:create', '/api/erp/mall/admin/template', 'POST', 1240),
 (108041, '商城模板查询', 'mall:template:list', '/api/erp/mall/admin/template/list', 'GET', 1241),
 (108042, '商城交易分析查看', 'mall:trade-analysis:view', '/api/erp/mall/admin/trade-analysis', 'GET', 1242),
 (108043, '商城用户审批', 'mall:user:approve', '/api/erp/mall/admin/user/{id}/approve', 'PUT', 1243),
 (108044, '商城用户删除', 'mall:user:delete', '/api/erp/mall/admin/user/{id}', 'DELETE', 1244),
 (108045, '商城用户查询', 'mall:user:list', '/api/erp/mall/admin/user/page', 'GET', 1245),
 (108046, '商城用户编辑', 'mall:user:update', '/api/erp/mall/admin/user/{id}/status', 'PUT', 1246)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9580000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 108000 AND 108047
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
