-- =============================================================================
-- 人力资源模块金标准 · 后端改造（2026-09-18）
--
-- 背景：HR 为本系统独有模块（ql361 对标系统无人力资源域），按业界成熟 HR 产品
--       （Odoo 18 / SAP SF·HCM / 金蝶 s-HR / 用友 YonSuite 人力云）建模。
--       本迁移处理《人力资源模块/README.md》§7 的 P0/P1 缺口中需要改库的部分：
--
--   P0-1  HrController 类级前缀 /hr 缺 /api（代码侧已改为 /api/hr，本迁移不含）
--   P0-4  sys_user_position 缺 deleted / tenant_id 两列，而 UserPositionMapper 的
--         三个 @Select 都写了 `AND deleted = 0`，且该表未登记进 IGNORE_TENANT_TABLES
--         → 岗位列表 GET /api/position/page 500（当前 0 行，属潜伏态：新增第一个
--           岗位后必现）。修法＝补两列（而非把业务表加进忽略表，那样会彻底失去租户隔离）。
--   P0-5  sys_permission 中 HR 权限码 0 条 + HrController 无鉴权注解
--         → 全员工资数据任何登录用户可拉。本迁移按控制器实际注解补齐权限码。
--   P0-7  招聘管理（菜单 907）parent_id = 900，而 sys_menu 中**不存在 id = 900**
--         → 孤儿菜单，用户在菜单树上点不到。修法＝新建分组 61406 并把 907 挂上去。
--
--   P1    工号无号段（空工号第二条撞 UNIQUE(employee_no)）→ 补 biz_number_sequence 种子
--   P1    无人员异动模型 → 新建 hr_employee_change（入职/转正/调岗/调薪/离职留痕）
--   P1    考勤无规则（迟到早退永远判不出）→ 新建 hr_attendance_rule
--   P1    无假期额度体系 → 新建 hr_leave_quota
--   P1    个税口径错（按月单独计税）→ 代码侧改累计预扣法，需补 hr_salary_payment.gross_amount
--   P1    绩效与薪资无联动 → 补 hr_performance.performance_coefficient
--   P1    请假与考勤无联动 → 补 hr_attendance.leave_request_id
--   P1    员工与系统账号零关联 → 补 hr_employee.user_id
--
-- 说明（与金标准指南「同名建表只生效第一次」一致）：
--   hr_* 各表真正生效的建表定义是 V8.9.0__HR_Module_Tables_and_Menu.sql；
--   resources/schema.sql 里的同名 CREATE TABLE IF NOT EXISTS 在 dev profile
--   （spring.sql.init.mode: never）下**不执行**，仅作留档。故本迁移一律用
--   ALTER TABLE ... ADD COLUMN IF NOT EXISTS，不重建表。
--
-- 幂等：全部语句可重复执行（IF NOT EXISTS / NOT EXISTS 守卫），
--      与 CRM 的 V11.379.0 同一写法。
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 一、P0-4：sys_user_position 补 deleted / tenant_id
--    真库实测该表只有 id / user_id / position_id / is_primary / create_time。
--    表当前 0 行，故 DEFAULT 回填不会翻转任何既有语义。
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE sys_user_position ADD COLUMN IF NOT EXISTS deleted   INTEGER DEFAULT 0;
ALTER TABLE sys_user_position ADD COLUMN IF NOT EXISTS tenant_id BIGINT  DEFAULT 1;

-- ─────────────────────────────────────────────────────────────────────────────
-- 二、P1：hr_employee 补 user_id（账号关联）/ regular_date（转正日期）
--             resign_type / resign_reason（离职类型与原因）
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS user_id       BIGINT;
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS regular_date  DATE;
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS resign_type   INTEGER;
ALTER TABLE hr_employee ADD COLUMN IF NOT EXISTS resign_reason VARCHAR(200);

COMMENT ON COLUMN hr_employee.user_id       IS '关联系统账号 sys_user.id；为空表示未开通账号（ESS 员工自助的前置）';
COMMENT ON COLUMN hr_employee.regular_date  IS '转正日期（由「转正」动作写入，不随主档表单直改）';
COMMENT ON COLUMN hr_employee.resign_type   IS '离职类型：1-主动离职 2-协商解除 3-辞退 4-合同到期 5-退休 6-其他';
COMMENT ON COLUMN hr_employee.resign_reason IS '离职原因';

-- ─────────────────────────────────────────────────────────────────────────────
-- 三、P1：hr_contract 补 trial_date_end（试用期到期日，Odoo hr.contract 口径）
--            terminate_reason（终止原因）
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE hr_contract ADD COLUMN IF NOT EXISTS trial_date_end   DATE;
ALTER TABLE hr_contract ADD COLUMN IF NOT EXISTS terminate_reason VARCHAR(200);

COMMENT ON COLUMN hr_contract.trial_date_end   IS '试用期到期日（试用期到期提醒的数据来源）';
COMMENT ON COLUMN hr_contract.terminate_reason IS '合同终止原因';

-- ─────────────────────────────────────────────────────────────────────────────
-- 四、P1：hr_attendance 补 leave_request_id（请假↔考勤联动的溯源列）
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE hr_attendance ADD COLUMN IF NOT EXISTS leave_request_id BIGINT;

COMMENT ON COLUMN hr_attendance.leave_request_id IS '状态为 LEAVE（休假）时，指向写入该标记的 hr_leave_request.id';

-- ─────────────────────────────────────────────────────────────────────────────
-- 五、P1：hr_salary_payment 补 gross_amount（应发合计）
--    旧实现只存实发，个税累计预扣法需要「本年累计收入」→ 必须有应发列才可算。
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE hr_salary_payment ADD COLUMN IF NOT EXISTS gross_amount NUMERIC(14, 2);

COMMENT ON COLUMN hr_salary_payment.gross_amount IS '应发合计（税前）= 基本+绩效+津贴+加班；累计预扣法按本列累计';

-- ─────────────────────────────────────────────────────────────────────────────
-- 六、P1：hr_performance 补 performance_coefficient（绩效系数，绩效→薪资联动）
--    由考核人填写，薪资生成时 绩效工资 = 绩效基数 × 系数（未填按 1.0，不做调整）。
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE hr_performance ADD COLUMN IF NOT EXISTS performance_coefficient NUMERIC(6, 4);

COMMENT ON COLUMN hr_performance.performance_coefficient IS '绩效系数：薪资生成时 绩效工资 = 绩效基数 × 本系数；为空按 1.0';

-- ─────────────────────────────────────────────────────────────────────────────
-- 七、P1：新建 hr_employee_change —— 人事异动记录
--    对标 SAP Personnel Action（写入 Actions infotype 0000）、
--         用友 DHR「变动类型 + 变动原因 + 生效日期」、金蝶 s-HR「人事快速异动」。
--    由 HrEmployeeServiceImpl 在建档/转正/调岗/调薪/离职时自动写入前后快照。
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hr_employee_change (
    id            BIGINT PRIMARY KEY,
    tenant_id     BIGINT,
    employee_id   BIGINT       NOT NULL,
    employee_no   VARCHAR(50),
    employee_name VARCHAR(50),
    change_type   VARCHAR(32)  NOT NULL,
    effective_date DATE,
    before_json   TEXT,
    after_json    TEXT,
    reason        VARCHAR(500),
    operator_id   BIGINT,
    operator_name VARCHAR(50),
    remark        VARCHAR(500),
    deleted       INTEGER      DEFAULT 0,
    create_time   TIMESTAMP,
    update_time   TIMESTAMP,
    create_by     BIGINT,
    update_by     BIGINT
);

COMMENT ON TABLE  hr_employee_change             IS '人事异动记录（入职/转正/调岗/调薪/离职的历史留痗）';
COMMENT ON COLUMN hr_employee_change.change_type IS '异动类型：ENTRY 入职 / REGULAR 转正 / TRANSFER 调岗 / SALARY_ADJUST 调薪 / RESIGN 离职 / REHIRE 复职 / UPDATE 信息变更';
COMMENT ON COLUMN hr_employee_change.before_json IS '变更前快照（JSON）';
COMMENT ON COLUMN hr_employee_change.after_json  IS '变更后快照（JSON）';

CREATE INDEX IF NOT EXISTS idx_hr_employee_change_emp    ON hr_employee_change (employee_id);
CREATE INDEX IF NOT EXISTS idx_hr_employee_change_tenant ON hr_employee_change (tenant_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 八、P1：新建 hr_attendance_rule —— 考勤规则（班次/上下班时间/迟到早退阈值）
--    此前完全没有考勤规则 → 打卡恒为 NORMAL，late_minutes/early_minutes/work_hours
--    永远为空 → 「迟到早退永远判不出来、加班时长永远为 0」，薪资侧没有可信输入。
--    按租户一行。
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hr_attendance_rule (
    id                    BIGINT PRIMARY KEY,
    tenant_id             BIGINT,
    work_start_time       TIME,
    work_end_time         TIME,
    late_grace_minutes    INTEGER,
    early_grace_minutes   INTEGER,
    standard_work_hours   NUMERIC(6, 2),
    auto_absent           INTEGER DEFAULT 0,
    remark                VARCHAR(500),
    deleted               INTEGER DEFAULT 0,
    create_time           TIMESTAMP,
    update_time           TIMESTAMP,
    create_by             BIGINT,
    update_by             BIGINT
);

COMMENT ON TABLE  hr_attendance_rule                     IS '考勤规则（按租户一行，缺省 09:00/18:00/宽限 0 分钟/标准 8 小时）';
COMMENT ON COLUMN hr_attendance_rule.late_grace_minutes  IS '迟到宽限：打卡晚于 上班时间 + 本值 才判迟到';
COMMENT ON COLUMN hr_attendance_rule.auto_absent         IS '是否启用「未打卡即缺勤」自动判定：0 否 1 是';

CREATE UNIQUE INDEX IF NOT EXISTS uk_hr_attendance_rule_tenant ON hr_attendance_rule (tenant_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 九、P1：新建 hr_leave_quota —— 假期额度（按 租户 × 类型 × 年度）
--    对标 Odoo 假期三件套的「额度分配 Allocation」。
--    未实现的部分（不建表、不造假）：按月/按工龄累积 Accrual、跨年结转 Rollover、
--    有效期与扣减期分离（SAP IT2006）——见《请假管理开发文档》「剩余缺口」。
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hr_leave_quota (
    id          BIGINT PRIMARY KEY,
    tenant_id   BIGINT,
    leave_type  VARCHAR(20) NOT NULL,
    year        INTEGER     NOT NULL,
    quota_days  NUMERIC(6, 2),
    remark      VARCHAR(500),
    deleted     INTEGER DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT
);

COMMENT ON TABLE  hr_leave_quota            IS '假期额度（按租户×请假类型×年度；已用天数为实时聚集，不落冗余列）';
COMMENT ON COLUMN hr_leave_quota.leave_type IS 'ANNUAL 年假 / SICK 病假 / PERSONAL 事假 / MATERNITY 产假 / MARRIAGE 婚假';
COMMENT ON COLUMN hr_leave_quota.quota_days IS '年度额度天数；为 0 或未配置时不校验余额（避免"未配置即无法请假"）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_hr_leave_quota_type_year
    ON hr_leave_quota (tenant_id, leave_type, year) WHERE deleted = 0;

-- ─────────────────────────────────────────────────────────────────────────────
-- 十、P1：工号 / 合同号 / 岗位编码 号段种子
--    hr_employee.employee_no 是 UNIQUE 且无默认值 → 旧实现从不写它，
--    第一条空工号可插入、第二条即撞唯一约束。改走全站号段体系。
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time)
SELECT 'EMP', '19700101', 0, 999999, 'EMP', 4, 1, 'zh_CN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'EMP' AND tenant_id = 1 AND locale = 'zh_CN');

INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time)
SELECT 'EMP', '19700101', 0, 999999, 'EMP', 4, 1, 'en_US', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'EMP' AND tenant_id = 1 AND locale = 'en_US');

INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time)
SELECT 'HT', '19700101', 0, 999999, 'HT', 4, 1, 'zh_CN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'HT' AND tenant_id = 1 AND locale = 'zh_CN');

INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time)
SELECT 'HT', '19700101', 0, 999999, 'HT', 4, 1, 'en_US', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'HT' AND tenant_id = 1 AND locale = 'en_US');

INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time)
SELECT 'HRPOS', '19700101', 0, 999999, 'HRPOS', 4, 1, 'zh_CN', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'HRPOS' AND tenant_id = 1 AND locale = 'zh_CN');

INSERT INTO biz_number_sequence (biz_type, seq_date, current_seq, max_seq, prefix, seq_length, tenant_id, locale, update_time)
SELECT 'HRPOS', '19700101', 0, 999999, 'HRPOS', 4, 1, 'en_US', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM biz_number_sequence WHERE biz_type = 'HRPOS' AND tenant_id = 1 AND locale = 'en_US');

-- ─────────────────────────────────────────────────────────────────────────────
-- 十一、P0-5：HR 权限码种子
--    sys_permission 原本 permission_code LIKE 'hr:%' **0 行**，
--    而代码侧已按这些码加 @RequiresPermission → 不补则除超管（走 `*` 通配）外
--    所有 HR 按钮与接口一律 403。这里按控制器实际注解逐个补齐。
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    -- 岗位
    (91101::BIGINT, 'HR岗位查询',       'hr:position:list',      '/api/hr/positions/page',        'GET',    301),
    (91102, 'HR岗位新增',               'hr:position:create',    '/api/hr/positions',             'POST',   302),
    (91103, 'HR岗位修改',               'hr:position:update',    '/api/hr/positions/*',           'PUT',    303),
    (91104, 'HR岗位删除',               'hr:position:delete',    '/api/hr/positions/*',           'DELETE', 304),
    -- 员工
    (91111, 'HR员工查询',               'hr:employee:list',      '/api/hr/employees/page',        'GET',    311),
    (91112, 'HR员工新增',               'hr:employee:create',    '/api/hr/employees',             'POST',   312),
    (91113, 'HR员工修改',               'hr:employee:update',    '/api/hr/employees/*',           'PUT',    313),
    (91114, 'HR员工删除',               'hr:employee:delete',    '/api/hr/employees/*',           'DELETE', 314),
    (91115, 'HR员工转正离职',           'hr:employee:status',    '/api/hr/employees/*/status',    'PUT',    315),
    -- 合同
    (91121, 'HR合同查询',               'hr:contract:list',      '/api/hr/contracts/page',        'GET',    321),
    (91122, 'HR合同新增',               'hr:contract:create',    '/api/hr/contracts',             'POST',   322),
    (91123, 'HR合同修改',               'hr:contract:update',    '/api/hr/contracts/*',           'PUT',    323),
    (91124, 'HR合同删除',               'hr:contract:delete',    '/api/hr/contracts/*',           'DELETE', 324),
    -- 考勤
    (91131, 'HR考勤查询',               'hr:attendance:list',    '/api/hr/attendance/page',       'GET',    331),
    (91132, 'HR考勤打卡',               'hr:attendance:clock',   '/api/hr/attendance/clock-in',   'POST',   332),
    (91133, 'HR考勤更正',               'hr:attendance:update',  '/api/hr/attendance/*',          'PUT',    333),
    (91134, 'HR考勤删除',               'hr:attendance:delete',  '/api/hr/attendance/*',          'DELETE', 334),
    (91135, 'HR考勤规则配置',           'hr:attendance:rule',    '/api/hr/attendance/rule',       'PUT',    335),
    -- 请假
    (91141, 'HR请假查询',               'hr:leave:list',         '/api/hr/leave/page',            'GET',    341),
    (91142, 'HR请假提交',               'hr:leave:create',       '/api/hr/leave',                 'POST',   342),
    (91143, 'HR请假修改撤销',           'hr:leave:update',       '/api/hr/leave/*',               'PUT',    343),
    (91144, 'HR请假审批',               'hr:leave:approve',      '/api/hr/leave/*/approve',       'PUT',    344),
    (91145, 'HR请假删除',               'hr:leave:delete',       '/api/hr/leave/*',               'DELETE', 345),
    (91146, 'HR假期额度配置',           'hr:leave:quota',        '/api/hr/leave/quota',           'PUT',    346),
    -- 薪资（薪资保密：与其余 HR 权限分开授予）
    (91151, 'HR薪资查询',               'hr:salary:list',        '/api/hr/salary/payment/page',   'GET',    351),
    (91152, 'HR薪资结构维护',           'hr:salary:update',      '/api/hr/salary/structure',      'POST',   352),
    (91153, 'HR薪资发放确认',           'hr:salary:confirm',     '/api/hr/salary/payment/*/confirm', 'PUT', 353),
    (91154, 'HR薪资生成',               'hr:salary:generate',    '/api/hr/salary/payment/generate',  'POST',354),
    -- 绩效
    (91161, 'HR绩效查询',               'hr:performance:list',   '/api/hr/performance/page',      'GET',    361),
    (91162, 'HR绩效提交修改',           'hr:performance:update', '/api/hr/performance',           'POST',   362),
    (91163, 'HR绩效确认',               'hr:performance:confirm','/api/hr/performance/*/confirm', 'PUT',    363),
    -- 招聘
    (91171, 'HR招聘查询',               'hr:recruitment:list',   '/api/hr/recruitment/page',      'GET',    371),
    (91172, 'HR招聘新增',               'hr:recruitment:create', '/api/hr/recruitment',           'POST',   372),
    (91173, 'HR招聘修改',               'hr:recruitment:update', '/api/hr/recruitment/*',         'PUT',    373),
    (91174, 'HR招聘删除',               'hr:recruitment:delete', '/api/hr/recruitment/*',         'DELETE', 374),
    -- 候选人
    (91181, 'HR候选人查询',             'hr:candidate:list',     '/api/hr/candidate/page',        'GET',    381),
    (91182, 'HR候选人新增',             'hr:candidate:create',   '/api/hr/candidate',             'POST',   382),
    (91183, 'HR候选人修改',             'hr:candidate:update',   '/api/hr/candidate/*',           'PUT',    383),
    (91184, 'HR候选人面试评价',         'hr:candidate:interview','/api/hr/candidate/*/interview', 'PUT',    384),
    -- 兼容：菜单 9071/9072/9073 已声明的历史权限码（保持既有菜单按钮可见）
    (91191, '招聘新增(历史码)',         'hr-recruitment:add',    NULL,                            NULL,     391),
    (91192, '招聘编辑(历史码)',         'hr-recruitment:edit',   NULL,                            NULL,     392),
    (91193, '招聘删除(历史码)',         'hr-recruitment:delete', NULL,                            NULL,     393)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- ─────────────────────────────────────────────────────────────────────────────
-- 十二、P0-7：修复孤儿菜单「招聘管理」（907）
--    907 的 parent_id = 900，而 sys_menu 中根本没有 id = 900 的记录
--    （V9.6.1 删了 901–906 却漏了 907；V11.36.0 又把它写成 parent_id=900）。
--    父节点不存在 → 菜单树上不可达 → 招聘管理页面用户点不到。
--    修法：新建分组 61406「招聘管理」挂到 60014 下，把 907 迁过去并归一化路径。
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component,
                      menu_level, sort, status, visible, deleted, create_time, update_time)
SELECT 61406, 60014, '招聘管理', 'mega:hr:recruitment', 0, NULL, NULL,
       0, 600, 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 61406);

UPDATE sys_menu
   SET parent_id  = 61406,
       menu_level = 3,
       path       = 'hr/recruitment',
       component  = 'views/hr/recruitment/list.vue',
       update_time = CURRENT_TIMESTAMP
 WHERE id = 907
   AND (parent_id IS DISTINCT FROM 61406
        OR menu_level IS DISTINCT FROM 3
        OR path IS DISTINCT FROM 'hr/recruitment'
        OR component IS DISTINCT FROM 'views/hr/recruitment/list.vue');

-- 9071/9072/9073 三个按钮型子菜单的层级同步到 4（页面 3 的下一层）
UPDATE sys_menu SET menu_level = 4, update_time = CURRENT_TIMESTAMP
 WHERE id IN (9071, 9072, 9073) AND menu_level IS DISTINCT FROM 4;
