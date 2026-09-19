-- =============================================================================
-- 设置模块 → 系统参数（菜单 80621 / set:sys-params）· 把 `sys_config` 接为真实参数载体
-- （2026-09-18，依据《设置模块/系统参数开发文档.md》§7、§8.1、§8.4、§12）
--
-- 【修的是什么】
--   本页原先的全部读写发生在 JVM 内存静态 Map（`SystemConfigServiceImpl.BUILTIN_CONFIGS`
--   12 条）里，与数据库完全无关：
--     · 新增/修改配置**不落库**（`saveConfig` 只写 Redis + log），重启即失，且新增项
--       因 `getConfigList` 只遍历内置集合而**永不出现**在列表里；
--     · 「分页」是假分页（`pages` 恒 1，`pageNum/pageSize` 被忽略）；
--     · `deleteConfig` / `batchDelete` / `refreshCache` 是空实现；
--     · `sys_config`（17 列 / 8 行）**没有任何 Java 载体**（无 @TableName、无 Mapper），
--       且内置 12 条的键名与表里 8 行**只有 `system.logo` 一个字面相同** →
--       表里存量 8 行是**孤儿数据，永远读不到**。
--   本迁移为 `sys_config` 补齐本页建模所需的列，并把开发文档 §8.1 实测到的
--   「行业设置」配置项 seed 成初始数据（可重复执行）。
--
-- 【新增列（5 个，逐列说明来源，不发明文档没有的字段）】
--   nav_group   左列纵向标签的**8 个视图**维度（文档 §8.1-1/2 实测 8 个标签）。
--               注意：它**不等于** `config_group`（`config_group` 是「基础配置/登录配置/
--               上传配置…」7 值的老维度，文档 §5.1 已登记两套维度语义不清）；
--               本列是 ql361 左标签导航的**单一维度**，由本页专用。
--   parent_key  文档 §8.1-6「可展开的父开关」（批次、批号管理 → 内联展开 3 个子项）。
--               子项的 parent_key = 父项的 param_key；顶层项为 NULL。
--   help_text   文档 §8.1-5「`?` 帮助气泡（悬停显示说明）」的正文载体。
--               ⚠️ 缺口：ql361 实测「批次商品成本规则」「批次保质期商品默认出库规则」
--               带 `?`，但气泡内文案**未采集**（文档仅记「悬停显示说明」）→ 本列为空，
--               不编造；页面在该列为空时回退展示 `remark`（配置项说明）。
--   tip_text    文档 §8.1-7「灰色温馨提示」的正文载体（含「此配置被商品启用后不能更改，
--               请慎重选择」与「订单指定批次后，拣货时优先使用批次条码匹配…」两条逐字文案）。
--   locked      文档 §8.4-①「不可逆配置」需要的布尔列（`sys_config` 原无此列，文档已明示
--               「需新增字段」）。页面在 locked = true 时禁用控件并打「已锁定」标记。
--               ⚠️ 缺口：ql361 的锁定触发条件是「配置被商品启用后」——本系统**没有**
--               「商品是否引用了该配置」的判定链路，故本迁移全部 seed 为 false（= 未锁定），
--               自动置锁未实现（如实登记，见开发文档 §12-⑩）。
--
-- 【明确**不加**的列】
--   · `default_value`：`SystemConfig.defaultValue` 字段存在但 `sys_config` 无该列，文档
--     §8.4 承诺「可只靠已有 17 列完成」，且本页新形态（左标签 + 右卡片）无「默认值」录入位
--     → 保持 `@TableField(exist = false)`，不新增列（文档 §12-⑳ 的「无重置为默认值」缺口保留）。
--   · `apply_mode`（生效时机）：文档 §8.2 将其标为 **D 级（推论、不承诺对标）** → 不新增。
--
-- 【存量 8 行的归位】
--   表里原有 8 行（system.title / system.logo / system.defaultPassword / system.passwordPolicy /
--   system.sessionTimeout / system.uploadMaxSize / system.uploadAllowedTypes /
--   system.loginLockCount）的 `config_group` 是 basic/password/session/upload/login，
--   不属于 ql361 的 8 个业务视图；统一落到**「其他」（other）** —— 该视图即本系统自定义
--   配置的兜底视图。这样存量行在本页**真实可见、可编辑、可保存**（P0 修复的验收点之一），
--   而不是读不到的死数据。
-- =============================================================================

-- ── 1. 补齐本页建模所需的 5 列 ───────────────────────────────────────────────
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS nav_group  varchar(64);
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS parent_key varchar(128);
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS help_text  varchar(500);
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS tip_text   varchar(500);
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS locked     boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN sys_config.nav_group  IS '左列纵向视图编码（本页 8 视图：industry/flow/bill/stock/finance/data_perm/notify/other）';
COMMENT ON COLUMN sys_config.parent_key IS '父配置键（param_key），用于「可展开的父开关」；顶层项为 NULL';
COMMENT ON COLUMN sys_config.help_text  IS '`?` 帮助气泡文案（ql361 实测带 ? 的配置项）';
COMMENT ON COLUMN sys_config.tip_text   IS '灰色「温馨提示」文案（不可逆/依赖说明）';
COMMENT ON COLUMN sys_config.locked     IS '不可逆配置锁定（锁定后页面禁止修改；自动置锁未实现）';

-- 左标签导航是页面主查询维度，建索引
CREATE INDEX IF NOT EXISTS idx_sys_config_nav_group ON sys_config (nav_group);

-- ── 2. 存量 8 行归入「其他」视图（使孤儿数据可达） ──────────────────────────
UPDATE sys_config
   SET nav_group = 'other'
 WHERE nav_group IS NULL
   AND deleted = 0;

-- ── 3. seed「行业设置」（industry）视图配置项 ───────────────────────────────
-- 逐项取自开发文档 §8.1-13（ql361 截图 + JSON 实测 2026-09-18 的可见项清单）：
--   商品规格属性 关 / 批次、批号管理 开（展开含 保质期管理 / 批号管理 / 批号启用大小写）/
--   批次条码规则生成 无 + 无 + 无 / 批次商品成本规则 按移动加权平均 /
--   批次保质期商品默认出库规则 近效先出 / 订单启用批次保质期 / 序列号管理 关
--
-- 口径：
--   · param_key 是本系统自定的键名（ql361 的键名不可见，文档 §7.2 已登记「键名两套」的历史问题，
--     本次统一用 industry.* 前缀；既有的 system.* 存量键**保持原样不动**）。
--   · param_value 统一存**文本**（`sys_config.param_value` 是 text 列，文档 §11 已确认本系统
--     配置中心统一用「字符串值」）：布尔写 'true'/'false'，枚举写枚举值，多段控件写逗号分隔。
--   · config_type：行业设置项属业务配置 → 'business'（沿用文档 §5.1 的 5 值口径）。
--   · config_group：与 nav_group 同值 'industry'（新配置项按左标签维度归档）。
--   · 布尔默认值逐字取自截图：关 = false、开/☑ = true、☐ = false。
--   · sort_order 按截图出现顺序 10~70。
--   · ON CONFLICT (param_key) DO NOTHING → 可重复执行（唯一约束 sys_config_param_key_key）。
INSERT INTO sys_config
    (param_key, param_name, param_value, config_type, config_group, nav_group, parent_key,
     value_type, builtin, remark, help_text, tip_text, sort_order, enabled, locked, tenant_id, deleted)
VALUES
    -- ① 商品规格属性（截图：关）
    ('industry.product.specAttr', '商品规格属性', 'false', 'business', 'industry', 'industry', NULL,
     'boolean', true, '商品规格属性开关（ql361 实测默认：关）', NULL, NULL, 10, true, false, 0, 0),

    -- ② 批次、批号管理（截图：开）—— 可展开的父开关，子项见 ③④⑤
    ('industry.batch.enabled', '批次、批号管理', 'true', 'business', 'industry', 'industry', NULL,
     'boolean', true, '批次/批号管理总开关，开启后展开子项（ql361 实测默认：开）', NULL, NULL, 20, true, false, 0, 0),

    -- ③④⑤ 父开关的子项（截图：☑ 保质期管理 / ☐ 批号管理 / ☐ 批号启用大小写）
    ('industry.batch.shelfLife', '保质期管理', 'true', 'business', 'industry', 'industry', 'industry.batch.enabled',
     'boolean', true, '商品保质期管理（ql361 实测默认：勾选）', NULL,
     '此配置被商品启用后不能更改，请慎重选择', 21, true, false, 0, 0),
    ('industry.batch.batchNo', '批号管理', 'false', 'business', 'industry', 'industry', 'industry.batch.enabled',
     'boolean', true, '商品批号管理（ql361 实测默认：未勾选）', NULL,
     '此配置被商品启用后不能更改，请慎重选择', 22, true, false, 0, 0),
    ('industry.batch.batchNoCase', '批号启用大小写', 'false', 'business', 'industry', 'industry', 'industry.batch.enabled',
     'boolean', true, '批号是否区分大小写（ql361 实测默认：未勾选）', NULL, NULL, 23, true, false, 0, 0),

    -- ⑥ 批次条码规则生成（截图：三个下拉 `无 + 无 + 无`，带依赖说明文案）
    --    value_type = 'list' → 页面按逗号分隔的段数渲染对应个数的下拉，用 `+` 连接
    ('industry.batch.barcodeRule', '批次条码规则生成', 'none,none,none', 'business', 'industry', 'industry', NULL,
     'list', true,
     '批次条码规则生成（ql361 实测为 3 个下拉串联，默认 无 + 无 + 无；其余可选项未实测，见开发文档 §8.1-9）',
     NULL,
     '订单指定批次后，拣货时优先使用批次条码匹配，批次没有条码时才能使用商品条码匹配', 30, true, false, 0, 0),

    -- ⑦⑧ 两个带 `?` 气泡的枚举项（气泡正文未采集 → help_text 留空，页面回退展示 remark）
    ('industry.batch.costRule', '批次商品成本规则', 'movingAverage', 'business', 'industry', 'industry', NULL,
     'enum', true, '批次商品成本的计算规则（ql361 实测默认：按移动加权平均）', NULL, NULL, 40, true, false, 0, 0),
    ('industry.batch.outboundRule', '批次保质期商品默认出库规则', 'nearExpiryFirst', 'business', 'industry', 'industry', NULL,
     'enum', true, '批次保质期商品的默认出库规则（ql361 实测默认：近效先出）', NULL, NULL, 50, true, false, 0, 0),

    -- ⑨ 订单启用批次保质期（截图：☑）
    ('industry.batch.orderShelfLife', '订单启用批次保质期', 'true', 'business', 'industry', 'industry', NULL,
     'boolean', true, '订单是否启用批次保质期（ql361 实测默认：勾选）', NULL, NULL, 60, true, false, 0, 0),

    -- ⑩ 序列号管理（截图：关）
    ('industry.serial.enabled', '序列号管理', 'false', 'business', 'industry', 'industry', NULL,
     'boolean', true, '商品序列号管理开关（ql361 实测默认：关）', NULL, NULL, 70, true, false, 0, 0)

ON CONFLICT (param_key) DO NOTHING;

-- ── 4. 其余 6 个视图（流程启用/单据设置/库存设置/财务设置/数据权限/消息提醒）────
-- 开发文档 §8.1 只实测了「行业设置」的内容（截图仅有该 Tab 展开态），其余 6 个视图
-- **只采集到标签名**，未采集到任何配置项 → **不编造配置项**，不 seed 任何行。
-- 左列 8 个标签由后端常量给出（`SystemConfigController#getNavGroups`），页面照常渲染，
-- 空视图显示「该视图配置项未实测」的缺口提示（如实登记，见开发文档 §12）。
