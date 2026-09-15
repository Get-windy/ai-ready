-- ============================================================
-- V11.49.0: 仓储模块菜单变更（列组重组 + 双入口转换 + 硬删除）
--
-- 依据：《系统菜单开发文档-仓储模块菜单变更.md》
--
-- 变更内容：
--   1. 其他出入库/盘点/生产 下的单据菜单改名并对齐业务语义，统一转为双入口
--   2. 库存预警(5013) 改名「预警设置」（原与预警查询重名有歧义），删除仓库侧重复的补货管理(5012)
--   3. 收货/上架/拣货/发货/库存 5 个作业列组合并 → 新增【库内作业】列组(60312)，5 张单据移入
--   4. 盘点作业单(80017)与盘点单(5003)功能重复，硬删除
--   5. 质检单(90201) 落地简单表单页并转双入口
--   6. 5 个旧作业列组(60306-60310)单据迁走后硬删除
--
-- 幂等设计：INSERT 使用 ON CONFLICT DO NOTHING，UPDATE/DELETE 按主键幂等，
--          可安全地在已应用/未应用两种状态下重复执行。
-- ============================================================

-- ---------- 1. 其他出入库(60301) ----------
-- 其他入库单(5002)：转双入口（与其他出库单 5001 对齐）
UPDATE sys_menu SET
  path = 'erp/stock-in/form',
  component = 'erp/stock-in/form',
  list_path = 'erp/stock-in/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5002;

-- 调拨管理(5011) → 调拨单：改名 + 转双入口
UPDATE sys_menu SET
  menu_name = '调拨单',
  path = 'erp/stock-transfer/form',
  component = 'erp/stock-transfer/form',
  list_path = 'erp/stock-transfer/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5011;

-- ---------- 2. 盘点(60302) ----------
-- 报损管理(5009) → 报损单
UPDATE sys_menu SET
  menu_name = '报损单',
  path = 'erp/stock-damage/form',
  component = 'erp/stock-damage/form',
  list_path = 'erp/stock-damage/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5009;

-- 溢余管理(5010) → 报溢单
UPDATE sys_menu SET
  menu_name = '报溢单',
  path = 'erp/stock-overflow/form',
  component = 'erp/stock-overflow/form',
  list_path = 'erp/stock-overflow/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5010;

-- 库存盘点(5003) → 盘点单
UPDATE sys_menu SET
  menu_name = '盘点单',
  path = 'erp/stocktake/form',
  component = 'erp/stocktake/form',
  list_path = 'erp/stocktake/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5003;

-- 成本调整(5008) → 成本调价单
UPDATE sys_menu SET
  menu_name = '成本调价单',
  path = 'erp/stock-cost-adjust/form',
  component = 'erp/stock-cost-adjust/form',
  list_path = 'erp/stock-cost-adjust/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5008;

-- ---------- 3. 生产(60303) ----------
-- 组装管理(5015) → 组装单
UPDATE sys_menu SET
  menu_name = '组装单',
  path = 'erp/stock-assemble/form',
  component = 'erp/stock-assemble/form',
  list_path = 'erp/stock-assemble/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5015;

-- 拆分管理(5016) → 拆分单（不采用对标系统“拆卸单”命名，沿用“拆分单”）
UPDATE sys_menu SET
  menu_name = '拆分单',
  path = 'erp/stock-split/form',
  component = 'erp/stock-split/form',
  list_path = 'erp/stock-split/index',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 5016;

-- ---------- 4. 库存预警(60304) ----------
-- 库存预警(5013) → 预警设置（原与预警查询重名/歧义，本菜单为预警规则维护）
UPDATE sys_menu SET
  menu_name = '预警设置',
  update_time = CURRENT_TIMESTAMP
WHERE id = 5013;

-- 补货管理(5012) 与采购模块「库存预警补货/缺货补货/智能补货」(70051-70053) 重复，删除
DELETE FROM sys_menu WHERE id = 5012;

-- ---------- 5. 作业列合计并 → 库内作业 ----------
-- 新增列组【库内作业】(60312)，承接原收货/上架/拣货/发货/库存 5 个作业列组下的单据
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
  menu_name, menu_code, menu_type, path, component, icon, sort,
  visible, status, client_type, display_group, display_mode, list_path, tag_label, menu_level)
VALUES (60312, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
  '库内作业', 'mega:wh:inventory-work', 0, NULL, NULL, NULL, 600,
  1, 1, 'tenant-admin', 0, 0, NULL, NULL, 0)
ON CONFLICT (id) DO NOTHING;

-- 移动 5 张单据到库内作业(60312)：收货单/上架单/拣货单/发货单/移库单
UPDATE sys_menu SET parent_id = 60312, update_time = CURRENT_TIMESTAMP
WHERE id IN (80012, 80013, 80014, 80015, 80016);

-- ---------- 6. 质量管理 · 质检单(90201) 转双入口 ----------
UPDATE sys_menu SET
  path = 'quality/inspection/form',
  component = 'quality/inspection/form',
  list_path = 'quality/inspection/list',
  tag_label = '历史',
  display_mode = 1,
  update_time = CURRENT_TIMESTAMP
WHERE id = 90201;

-- ---------- 7. 硬删除（冗余/废弃） ----------
-- 盘点作业单(80017)：与盘点单(5003)功能重复，物理删除
DELETE FROM sys_menu WHERE id = 80017;

-- 5 个旧作业列组(60306-60310)：单据已迁走且盘点作业单已删，物理删除
DELETE FROM sys_menu WHERE id IN (60306, 60307, 60308, 60309, 60310);

-- ============================================================
-- 验证（可手动执行）：
--   SELECT id, parent_id, menu_name, menu_code, path, list_path, tag_label, display_mode
--   FROM sys_menu WHERE id IN (5002,5003,5008,5009,5010,5011,5015,5016,5013,90201,60312,80012,80013,80014,80015,80016)
--   ORDER BY id;
--   SELECT id, parent_id, menu_name FROM sys_menu WHERE id IN (5012,60306,60307,60308,60309,60310,80017);
-- ============================================================
