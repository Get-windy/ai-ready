-- ============================================================================
-- V11.364.0 运费设置（商城 → 商城设置 → 运费设置）「物流」子项按对标实测重建
-- ============================================================================
-- 页面：views/mall/freight-config/index.vue（子项一「物流」）
-- 对标：ql361 v2.2「商城 → 商城设置 → 运费设置 → 物流」实测抓取 2026-09-14
-- 文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/运费设置开发文档.md
--
-- 背景（对标实测结论，权威）：
--   1. 「物流」子项**没有**「物流方式」动态行列表 —— 我们落库的 logistics_methods
--      JSON 数组（[{type,name,enabled}]）是自造结构，对标不存在。
--   2. freight_template 的原形状 {templateName,chargeType,freeEnabled,freeThreshold,
--      tiers:[{from,to,fee}]} 与实测不符：
--        · templateName 对标没有（无「模板名称」字段）；
--        · 包邮是**每行（地区）一个金额**，不是模板级开关 + 阈值；
--        · tiers:[{from,to,fee}] 只对应「按订单金额」那一张表的金额阶梯，且字段名不同
--          （后端为 prilist:[{min,max,freight,caninput}]）；
--        · 完全缺失核心维度：**地区（省/市/区）** 与 **首重/续重**。
--   3. 实测的真实结构：两个固定开关 + 「运费计算方式」下拉（3 项）+ **三张随选择切换的
--      地区运费表**（按重量 / 按订单金额 / 按订单数量）。
--
-- 本迁移动作（幂等）：
--   A. freight_template 列：仅更新列注释，改为**新的存储形状口径**（列类型 TEXT 不变，
--      无需 ALTER TYPE —— 本列无生产积累，JSON 形状由前后端约定，换形不需要 DDL 变更）。
--        新形状（本实现存储口径，覆盖全部实测维度）：
--        {
--          "wftype": 1,                                  -- 运费计算方式：1=按重量 2=按订单金额 3=按订单数量
--          "defaultFreight": 0,                         -- 未设置运费的地区的默认运费（对标红字提示 N 元，实测默认 0）
--          "regions": [                                 -- 三张表的共同行集合，维度=地区（省/市/区）
--            {
--              "code": "", "province": "", "city": "", "area": "",
--              "firstWeight": 0, "freight": 0,          -- 按重量「首重(KG)」「运费(元)」
--              "addWeight": 0, "addFreight": 0,         -- 按重量「续重(KG)」「续费(元)」
--              "startCount": 0, "addCount": 0,          -- 按订单数量「起算数量」「增加数量」
--              "freeFreight": 0,                        -- 「满额包邮(元)」（每行一个金额）
--              "amountTiers": [{"min":0,"max":null,"freight":0,"caninput":true}]
--                                                       -- 仅「按订单金额」表行内「设置」的金额阶梯
--                                                       -- （对标后端 prilist，max=null 表示不限）
--            }
--          ]
--        }
--   B. logistics_methods 列：**保留列不删、不清空既有数据**，仅更新列注释标注「已废弃」。
--        理由：① 该列 V11.361.7 才新增、无生产积累，但删列会破坏仍可能携带该字段的旧版本
--              前端 / 已发布 JAR 的兼容性（列不存在时 MyBatis-Plus 全量 SQL 会直接报错）；
--              ② 置空既有数据属破坏性动作且无收益（前端已不再读写该列）；
--              ③ 保留列的成本仅为一次 COMMENT，风险为 0。
--        → 前端 `views/mall/freight-config/index.vue` 已**不再读写** logistics_methods；
--          `api/erp/mall.ts` 的 `ShopConfig.logisticsMethods` 标注 `@deprecated`。
--
-- ⚠️ 本迁移**不新增、不改名任何列**：tenant_shop_config 真库生效建表为
--    V6.4.0__Create_B2B_Mall_Tables.sql，V11.361.7 已补齐本页 6 列。
--    故这里只做 COMMENT，避免把不存在的列写进 SQL。
--
-- 幂等性：COMMENT ON COLUMN 可重复执行；列不存在时不会报错（列已由上迁移保证存在）。
-- ============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
-- A. freight_template：列注释改为新形状口径（列类型 TEXT 不变）
-- ─────────────────────────────────────────────────────────────────────────────
COMMENT ON COLUMN tenant_shop_config.freight_template IS
'【运费设置】地区运费表 JSON（对标实测重建，2026-09-14）：
{"wftype":1|2|3, "defaultFreight":0, "regions":[{"code","province","city","area","firstWeight","freight","addWeight","addFreight","startCount","addCount","freeFreight","amountTiers":[{"min","max","freight","caninput"}]}]}。
wftype=运费计算方式（1=按重量 2=按订单金额 3=按订单数量，后端字段名 wftype）；
regions 为三张表的共同行集合，维度=地区（省/市/区）；
「按重量」表用 firstWeight(首重KG)/freight(运费元)/addWeight(续重KG)/addFreight(续费元)；
「按订单金额」表用 freight(运费元) + amountTiers(行内「设置」的金额阶梯，对标 prilist)；
「按订单数量」表用 startCount(起算数量)/freight(运费元)/addCount(增加数量)/addFreight(续费元)；
freeFreight=满额包邮(元)，**每行一个金额**（对标无模板级包邮开关）；
defaultFreight=未设置运费的地区的默认运费（对标红字提示「未设置运费的地区，默认运费为N元」，实测默认 0）。
⚠️ 三张表的列头按对标实测逐字对齐（无「启用」列）；未实现的后端字段清单见《运费设置开发文档》「剩余缺口」。';


-- ─────────────────────────────────────────────────────────────────────────────
-- B. logistics_methods：列保留、数据保留，仅标注废弃（对标不存在该结构）
-- ─────────────────────────────────────────────────────────────────────────────
COMMENT ON COLUMN tenant_shop_config.logistics_methods IS
'【运费设置】⚠️ 已废弃（DEPRECATED，V11.364.0 起）：对标 ql361「运费设置 → 物流」实测**没有**「物流方式」列表，
本列为历史自造结构 [{type:CITY|EXPRESS|SELF,name,enabled}]，前端已不再读写。
保留列与既有数据仅为兼容（删列会破坏旧版本前端/已发布 JAR 的 SQL 兼容性），新代码**禁止**使用。';


-- ============================================================================
-- 说明：本迁移只改注释，不改数据、不改结构 —— 在 MigCheck 事务内干跑后回滚，库不变。
-- ============================================================================
