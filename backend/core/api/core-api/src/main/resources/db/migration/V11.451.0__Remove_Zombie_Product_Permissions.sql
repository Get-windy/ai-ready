-- ══════════════════════════════════════════════════════════════════════════════
-- E-02 批次 3：删除商品域的 19 条「僵尸权限码」
--
-- 判据（逐条读源码复核，非启发式猜测）：
--   这批码在 sys_permission 里可勾选，但**没有任何消费方** —— 后端无 @SaCheckPermission
--   引用、前端无 v-permission/checkPermission 引用，勾进角色不控制任何东西。
--
-- 为什么是「删」而不是「补注解」——两类各有出处：
--   ① 平行命名空间残留（8 条）：`product:list/view/detail/export/create/update/delete/import`
--      是 E-04 按模块批量造码的产物。但真正的《商品》控制器
--      （erp-stock ProductController，/api/erp/product）用的是 **erp:product:*** 8 条
--      （list/view/create/update/delete/status/approval/price-batch）。
--      同一件事两套码，真在把门的是 erp: 那套 ⇒ 裸 product: 这套是重影，删。
--   ② 端点已被既有码覆盖（11 条）：端点在，但注解用的是粗粒度码：
--      · ProductBarcodeController  /page、/export        → @SaCheckPermission("erp:product:list")
--      · ProductShieldController   /page                  → @SaCheckPermission("erp:product:list")
--      · ProductShieldController   POST 保存/批量屏蔽/取消  → @SaCheckPermission("erp:product:update")
--      · ProductPriceController    全部读 / 全部写          → erp:product:list / erp:product:price-batch
--      ⇒ 给同一端点再挂第二个码会造成「两道门」且没人能勾明白，删。
--
-- 删前已核对（真库）：
--   · 19 条均为叶子节点（无 parent_id 引用、无子节点）；
--   · 持有者只有「超级管理员」一个角色，无租户侧授权会被误删；
--   · sys_menu.menu_code 无任何菜单引用这些码。
--
-- 保留的：erp:product:* 8 条（真实消费方），product:attributes/category/brand/grade/
--   units/unit-dict/unit-group/location/related/sku-rules/attachments/barcodes:{detail,
--   create,update,delete} 等子实体码（各自控制器真在用）。
-- ══════════════════════════════════════════════════════════════════════════════

-- ── 1. 软删 19 条僵尸码（软删保住审计与回滚能力） ──
UPDATE sys_permission
SET deleted = 1
WHERE deleted = 0
  AND permission_code IN (
    -- ① 平行命名空间残留（8 条）
    'product:list', 'product:view', 'product:detail', 'product:export',
    'product:create', 'product:update', 'product:delete', 'product:import',
    -- ② 端点已被既有粗粒度码覆盖（11 条）
    'product:barcodes:list', 'product:barcodes:export',
    'product:shield:list', 'product:shield:create',
    'md:product-price:list', 'md:product-price:view', 'md:product-price:create',
    'md:product-price:update', 'md:product-price:delete',
    'md:product-price:export', 'md:product-price:import'
  );

-- ── 2. 清掉这些码残留的角色授权行（授权指向一个已不存在的功能点，留着只会误导） ──
DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission
    WHERE deleted = 1
      AND permission_code IN (
        'product:list', 'product:view', 'product:detail', 'product:export',
        'product:create', 'product:update', 'product:delete', 'product:import',
        'product:barcodes:list', 'product:barcodes:export',
        'product:shield:list', 'product:shield:create',
        'md:product-price:list', 'md:product-price:view', 'md:product-price:create',
        'md:product-price:update', 'md:product-price:delete',
        'md:product-price:export', 'md:product-price:import'
      )
);

-- ── 3. 自检 ──
DO $$
DECLARE
    left_zombies int;
    live_guards  int;
    dup          int;
BEGIN
    -- 3.1 19 条必须都已下架
    SELECT count(*) INTO left_zombies
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN (
        'product:list', 'product:view', 'product:detail', 'product:export',
        'product:create', 'product:update', 'product:delete', 'product:import',
        'product:barcodes:list', 'product:barcodes:export',
        'product:shield:list', 'product:shield:create',
        'md:product-price:list', 'md:product-price:view', 'md:product-price:create',
        'md:product-price:update', 'md:product-price:delete',
        'md:product-price:export', 'md:product-price:import'
      );
    IF left_zombies > 0 THEN
        RAISE EXCEPTION '批次3清理后仍有 % 条僵尸码未下架', left_zombies;
    END IF;

    -- 3.2 商品域真正把门的 erp:product:* 8 条必须一条不少（删错就没人能进商品页了）
    SELECT count(*) INTO live_guards
    FROM sys_permission
    WHERE deleted = 0
      AND permission_code IN ('erp:product:list', 'erp:product:view', 'erp:product:create',
                              'erp:product:update', 'erp:product:delete', 'erp:product:status',
                              'erp:product:approval', 'erp:product:price-batch');
    IF live_guards <> 8 THEN
        RAISE EXCEPTION '商品域在役权限码异常：期望 8 条 erp:product:*，实际 % 条', live_guards;
    END IF;

    -- 3.3 不得出现同码多行
    SELECT count(*) INTO dup
    FROM (SELECT permission_code FROM sys_permission WHERE deleted = 0
          GROUP BY permission_code HAVING count(*) > 1) t;
    IF dup > 0 THEN
        RAISE EXCEPTION '清理后出现 % 组同码多行，请人工核查', dup;
    END IF;
END $$;
