-- ============================================================
-- V9.6.4: 扁平化 交易·商城管理 结构 + 合并外部平台列
--
-- 背景：
--   61507(商城管理) 下的叶子项挂在子目录(60902/60903)下，形成三级结构。
--   Mega Menu 面板只渲染直接 children，导致面板内容缺失。
--   同时 渠道管理/外部订单/库存同步 三个列内容过少，合并为"外部平台"一列。
--
-- 解决方案：
--   1. 将 61509(商城管理)、61510(商城设置) 提升为交易(60015)的并列列组
--   2. 将 61503/61504/61505 合并为 61511(外部平台)，90106 重命名为 数据同步
--   3. 软删除多余的目录节点 61507、61503、61504、61505、60902、60903
-- ============================================================

-- Step 1: 在 交易(60015) 下插入两个并列列组（display_group=0）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61509, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '商城管理', 'mega:trade:mall-biz', 0, 300, 1, 1, 'tenant-admin', 0, 0),
  (61510, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '商城设置', 'mega:trade:mall-config', 0, 310, 1, 1, 'tenant-admin', 0, 0);

-- Step 2: 将 60902(原基础业务) 叶子移到 61509(商城管理)
UPDATE sys_menu SET parent_id = 61509, sort = 101, update_time = CURRENT_TIMESTAMP WHERE id = 80360;  -- 商品上架
UPDATE sys_menu SET parent_id = 61509, sort = 102, update_time = CURRENT_TIMESTAMP WHERE id = 80361;  -- 单位显示
UPDATE sys_menu SET parent_id = 61509, sort = 103, update_time = CURRENT_TIMESTAMP WHERE id = 80362;  -- 买家申请管理
UPDATE sys_menu SET parent_id = 61509, sort = 104, update_time = CURRENT_TIMESTAMP WHERE id = 80363;  -- 买家账号
UPDATE sys_menu SET parent_id = 61509, sort = 105, update_time = CURRENT_TIMESTAMP WHERE id = 80364;  -- 商品组合
UPDATE sys_menu SET parent_id = 61509, sort = 106, update_time = CURRENT_TIMESTAMP WHERE id = 51102;  -- 商品管理
UPDATE sys_menu SET parent_id = 61509, sort = 107, update_time = CURRENT_TIMESTAMP WHERE id = 51104;  -- 用户审核

-- Step 3: 将 60903(原商城设置) 叶子移到 61510(商城设置)
UPDATE sys_menu SET parent_id = 61510, sort = 201, update_time = CURRENT_TIMESTAMP WHERE id = 80370;  -- 基础设置
UPDATE sys_menu SET parent_id = 61510, sort = 202, update_time = CURRENT_TIMESTAMP WHERE id = 80371;  -- 店铺设置
UPDATE sys_menu SET parent_id = 61510, sort = 203, update_time = CURRENT_TIMESTAMP WHERE id = 80372;  -- 运费设置
UPDATE sys_menu SET parent_id = 61510, sort = 204, update_time = CURRENT_TIMESTAMP WHERE id = 80373;  -- 商城装修
UPDATE sys_menu SET parent_id = 61510, sort = 205, update_time = CURRENT_TIMESTAMP WHERE id = 80374;  -- 公告设置
UPDATE sys_menu SET parent_id = 61510, sort = 206, update_time = CURRENT_TIMESTAMP WHERE id = 80375;  -- 关键词库
UPDATE sys_menu SET parent_id = 61510, sort = 207, update_time = CURRENT_TIMESTAMP WHERE id = 51101;  -- 商城配置
UPDATE sys_menu SET parent_id = 61510, sort = 208, update_time = CURRENT_TIMESTAMP WHERE id = 51105;  -- 轮播图管理

-- Step 4: 插入新列 外部平台(61511)，合并 渠道管理/外部订单/库存同步
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (61511, 0, 60015, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '外部平台', 'mega:trade:external-platform', 0, 600, 1, 1, 'tenant-admin', 0, 0);

-- Step 5: 将三个旧列的叶子移到 外部平台，库存同步重命名为数据同步
UPDATE sys_menu SET parent_id = 61511, sort = 1, update_time = CURRENT_TIMESTAMP WHERE id = 90104;  -- 渠道配置
UPDATE sys_menu SET parent_id = 61511, sort = 2, update_time = CURRENT_TIMESTAMP WHERE id = 90105;  -- 外部订单
UPDATE sys_menu SET parent_id = 61511, sort = 3, menu_name = '数据同步', update_time = CURRENT_TIMESTAMP WHERE id = 90106;

-- Step 6: 硬删除多余的目录节点
DELETE FROM sys_menu WHERE id IN (61503, 61504, 61505, 61507, 60902, 60903);

-- Step 7: 调整交易下其他列的排序
UPDATE sys_menu SET sort = 500, update_time = CURRENT_TIMESTAMP WHERE id = 61501;  -- 门店零售

-- Step 8: 验证
-- SELECT id, menu_name, parent_id, sort FROM sys_menu
-- WHERE parent_id = 60015 AND deleted = 0 ORDER BY sort;
