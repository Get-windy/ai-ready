-- =============================================================================
-- 设置模块 → 打印设置（设置 → 打印管理 → 打印设置，新建菜单 80930，挂 61205）
-- V11.402.0 · 2026-09-18
--
-- 【为什么要建这一页】
--   ① 对标缺口：ql361「设置 → 系统配置」组实测 **7 页**，本系统该组只有 6 页，缺的正是「打印设置」
--      （证据 `tool-results/ql361/设置-live/打印设置.json` + `.png`）。
--   ② 结构缺口：`61205 打印管理`（mega:set:printing，sort=500，client_type=tenant-admin）
--      是**空分组** —— 全库（含已逻辑删除）**没有任何 parent_id=61205 的子菜单**，
--      前端会渲染出一个「点不开任何页面」的菜单组。
--   → 本迁移把「打印设置」挂到 61205 之下，**一次解决两个缺口**（裁定见《设置模块/README.md》§2.3、
--     《设置模块/打印设置开发文档.md》§12.1-①）。
--
-- 【存储选型：开发文档 §7.3 的路线 B（专用表 set_print_config）】
--   文档给出 A（复用 `sys_config` 配置中心）/ B（专用表）/ C（专用表 + JSON）三条路线；
--   本次取 **B**：本页是「一行一租户」的强类型配置（9 个业务列 + 审计列 = 16 列 < 25 列，
--   符合《开发技术规范》），且**不依赖**《系统参数》页的配置中心口径，
--   避免把该页的历史问题（配置存不下 / 租户覆盖不确定）传染到打印链路。
--
-- 【字段逐字取自 ql361 实拍，非发明】
--   allow_draft_print    允许打印草稿              （实拍 ☑）
--   attr_summary_print   属性商品汇总打印          （实拍 ☑）
--   batch_summary_print  批次效期商品汇总打印      （实拍 □）
--   print_content        打印内容                  （实拍选中 `批号 *数量`）
--   decimal_enabled      单据打印小数位数（总开关）（实拍 □）
--   qty_decimal          数量的小数位              （实拍 `2位小数`）
--   price_decimal        单价的小数位              （实拍 `2位小数`）
--   assistant_enabled    助手打印                  （实拍 `开`）
--   remote_enabled       远程打印                  （实拍 `开`）
--
--   ⚠️ 上列**默认值就是 ql361 抓取时刻该租户的实际选中值**（开发文档 §7.3 明确标注），
--      不等于「本系统应采用的默认值」（后者需产品裁定）。本迁移照抄实拍值，便于对照复核。
--   ⚠️ 三个下拉的**完整选项集未实测**（文档 §5.1 / §12-P1）→ 本迁移**不造选项**：
--      `print_content` 只登记实测值；数量/单价小数位只登记 `2`，
--      页面用数字输入（0~4，与主数据精度 `unit_price numeric(18,4)` 对齐）而非枚举下拉。
--
-- 【布尔列一律 INTEGER 0/1】本库多租户/逻辑删除等既有列均为 integer；实体侧用 Integer。
--   （开发文档 §7.3 写的是 boolean 类型，此处按本库落库口径统一为 integer 0/1。）
--
-- 【租户隔离】本表**有** `tenant_id` → 不加入 MyBatisPlusConfig.IGNORE_TENANT_TABLES，
--   由全局多租户插件自动注入 `tenant_id = 当前会话租户`。一行一租户（部分唯一索引保证）。
--   本迁移**不预置配置行**：首次读取由服务端按默认值自动建行（口径明确，见开发文档 §10.1-8）。
--
-- 【幂等】建表/建索引 IF NOT EXISTS；菜单与权限 NOT EXISTS 守卫，可重复执行。
-- =============================================================================

-- ── ① 配置表 ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS set_print_config (
    id                  BIGINT       NOT NULL,
    tenant_id           BIGINT       NOT NULL,
    allow_draft_print   INTEGER      NOT NULL DEFAULT 1,
    attr_summary_print  INTEGER      NOT NULL DEFAULT 1,
    batch_summary_print INTEGER      NOT NULL DEFAULT 0,
    print_content       VARCHAR(64)  NOT NULL DEFAULT '批号 *数量',
    decimal_enabled     INTEGER      NOT NULL DEFAULT 0,
    qty_decimal         INTEGER      NOT NULL DEFAULT 2,
    price_decimal       INTEGER      NOT NULL DEFAULT 2,
    assistant_enabled   INTEGER      NOT NULL DEFAULT 1,
    remote_enabled      INTEGER      NOT NULL DEFAULT 1,
    create_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    deleted             INTEGER      NOT NULL DEFAULT 0,
    version             INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_set_print_config PRIMARY KEY (id)
);

COMMENT ON TABLE  set_print_config                    IS '打印设置（租户级，一行一租户；设置 → 打印管理 → 打印设置 80930）';
COMMENT ON COLUMN set_print_config.tenant_id          IS '租户ID（多租户红线，服务端取，不由客户端传）';
COMMENT ON COLUMN set_print_config.allow_draft_print  IS '允许打印草稿（0否 1是，ql361 实拍 1）';
COMMENT ON COLUMN set_print_config.attr_summary_print IS '属性商品汇总打印（0否 1是，ql361 实拍 1）';
COMMENT ON COLUMN set_print_config.batch_summary_print IS '批次效期商品汇总打印（0否 1是，ql361 实拍 0）';
COMMENT ON COLUMN set_print_config.print_content      IS '打印内容（ql361 实拍值「批号 *数量」；完整选项集未实测，不造值）';
COMMENT ON COLUMN set_print_config.decimal_enabled    IS '单据打印小数位数总开关（0关 1开，ql361 实拍 0）';
COMMENT ON COLUMN set_print_config.qty_decimal        IS '数量小数位（ql361 实拍 2；取值域按主数据精度 0~4）';
COMMENT ON COLUMN set_print_config.price_decimal      IS '单价小数位（ql361 实拍 2；取值域按主数据精度 0~4）';
COMMENT ON COLUMN set_print_config.assistant_enabled  IS '助手打印（0关 1开，ql361 实拍 开）';
COMMENT ON COLUMN set_print_config.remote_enabled     IS '远程打印（0关 1开，ql361 实拍 开）';

-- 一行一租户（只对未删除行生效；deleted 行不参与唯一性，避免逻辑删除后无法重建）
CREATE UNIQUE INDEX IF NOT EXISTS uk_set_print_config_tenant
    ON set_print_config (tenant_id) WHERE deleted = 0;

-- ── ② 菜单行：设置 → 打印管理（61205）→ 打印设置 ──────────────────────────────
-- ⚠️ 三处**缺一不可**（本模块已实踩多页）：
--   · tenant_id = 0        —— 菜单树只取 tenant_id = 0 的行（否则不进树，页面 404）
--   · client_type = 'tenant-admin' —— 否则租户侧菜单里看不到（只有平台侧可见）
--   · deleted = 0 / status = 1 / visible = 1
--   id 取 80930：实测 devdb 80921~80999 整段空闲（`SELECT ... WHERE id BETWEEN 80921 AND 80999` → 0 行），
--   留出 80921~80929 给并行会话，降低抢号风险。
-- ⚠️ 本迁移**只加不改**：不动 61205 这一行本身，也不动其它任何菜单行。
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component,
                      sort, is_external, is_cache, visible, status, client_type,
                      display_group, display_mode, list_path, tag_label, menu_level)
SELECT 80930::BIGINT, 0, 61205, 0, now(), now(),
       '打印设置', 'set:print-config', 1, 'set/print-config', 'views/set/print-config/index.vue',
       1, 0, 1, 1, 1, 'tenant-admin',
       0, 0, NULL, NULL, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = 80930);

-- ── ③ 权限码种子：set:print-config:view / set:print-config:update ─────────────
-- 命名口径：沿用本模块既有做法（`set:sys-params` / `set:payment-config` / `set:company-info:save`），
--   第一段为 `set:`（与菜单 menu_code 一致），第二段为页面名 `print-config`。
-- ⚠️ 授权只给超级管理员角色（role_id = 1），与 V11.394.0 / V11.395.0 / V11.396.0 同口径；
--   普通租户角色由租户管理员在「系统 → 角色管理」按需勾选，本迁移不越权代配。
-- id 取 91360 / 91361（permission）+ 9136001 / 9136002（role_permission）：
--   已核 devdb 实际占用：91201~91206、91301~91311、91321~91322、91341~91345（permission）；
--   9120701~9120706、9130301~9130302、9130701~9130708、9130741~9130745、9130901、9133101~9133102（role_permission）。
--   ⚠️ 原稿用 91320/91321，与已应用的 V11.398.0（workflow:audit:list 占 91321）撞主键 → 迁移会失败，已改号。
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method,
                            sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91360::BIGINT, '打印设置查询', 'set:print-config:view',   '/api/set/print-config',         'GET', 402),
    (91361::BIGINT, '打印设置保存', 'set:print-config:update', '/api/set/print-config',         'PUT', 403)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9136000 + (p.sort - 402), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('set:print-config:view', 'set:print-config:update')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
