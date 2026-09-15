-- V8.9.0 人力资源管理模块表 + HR菜单项

-- ─────────────────────────────────────────────────────────────────────
-- 1. 岗位表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_position (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    dept_id              BIGINT,
    position_code        VARCHAR(50),
    position_name        VARCHAR(100) NOT NULL,
    position_level       INTEGER DEFAULT 3,
    responsibility       TEXT,
    quota_count          INTEGER DEFAULT 1,
    current_count        INTEGER DEFAULT 0,
    sort                 INTEGER DEFAULT 0,
    status               INTEGER DEFAULT 1,
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_position IS '岗位表 - 组织架构岗位定义';
COMMENT ON COLUMN hr_position.position_level IS '岗位级别: 1-高管 2-中层 3-基层 4-普通';

CREATE INDEX idx_hr_position_dept ON hr_position(dept_id);
CREATE INDEX idx_hr_position_tenant ON hr_position(tenant_id);

-- ─────────────────────────────────────────────────────────────────────
-- 2. 员工档案表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_employee (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_no          VARCHAR(50) UNIQUE,
    employee_name        VARCHAR(100) NOT NULL,
    dept_id              BIGINT,
    position_id          BIGINT,
    gender               INTEGER DEFAULT 0,
    birth_date           DATE,
    phone                VARCHAR(20),
    email                VARCHAR(100),
    id_card              VARCHAR(20),
    education            INTEGER DEFAULT 5,
    school               VARCHAR(200),
    major                VARCHAR(100),
    hire_date            DATE,
    leave_date           DATE,
    employee_type        INTEGER DEFAULT 1,
    status               INTEGER DEFAULT 1,
    avatar_url           VARCHAR(200),
    emergency_contact    VARCHAR(100),
    emergency_phone      VARCHAR(20),
    hometown_address     VARCHAR(200),
    current_address      VARCHAR(200),
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_employee IS '员工档案表';
COMMENT ON COLUMN hr_employee.education IS '学历: 1-小学 2-初中 3-高中 4-大专 5-本科 6-硕士 7-博士';
COMMENT ON COLUMN hr_employee.employee_type IS '员工类型: 1-全职 2-兼职 3-实习 4-外包';
COMMENT ON COLUMN hr_employee.status IS '员工状态: 0-离职 1-在职 2-试用';

CREATE INDEX idx_hr_employee_dept ON hr_employee(dept_id);
CREATE INDEX idx_hr_employee_tenant ON hr_employee(tenant_id);
CREATE INDEX idx_hr_employee_no ON hr_employee(employee_no);

-- ─────────────────────────────────────────────────────────────────────
-- 3. 劳动合同表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_contract (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_id          BIGINT NOT NULL,
    contract_type        INTEGER DEFAULT 1,
    contract_no          VARCHAR(50),
    contract_name        VARCHAR(100),
    start_date           DATE,
    end_date             DATE,
    salary_amount        DECIMAL(12,2),
    sign_date            DATE,
    status               INTEGER DEFAULT 1,
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_contract IS '劳动合同表';
COMMENT ON COLUMN hr_contract.contract_type IS '合同类型: 1-固定期限 2-无固定期限 3-试用期';

CREATE INDEX idx_hr_contract_employee ON hr_contract(employee_id);

-- ─────────────────────────────────────────────────────────────────────
-- 4. 考勤记录表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_attendance (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_id          BIGINT NOT NULL,
    attendance_date      DATE NOT NULL,
    clock_in_time        TIME,
    clock_out_time       TIME,
    status               VARCHAR(20) DEFAULT 'NORMAL',
    late_minutes         INTEGER DEFAULT 0,
    early_minutes        INTEGER DEFAULT 0,
    work_hours           DECIMAL(4,1),
    remark               VARCHAR(200)
);

COMMENT ON TABLE hr_attendance IS '考勤记录表';
COMMENT ON COLUMN hr_attendance.status IS '考勤状态: NORMAL-正常 LATE-迟到 EARLY-早退 ABSENT-缺勤';

CREATE INDEX idx_hr_attendance_employee ON hr_attendance(employee_id);
CREATE INDEX idx_hr_attendance_date ON hr_attendance(attendance_date);

-- ─────────────────────────────────────────────────────────────────────
-- 5. 请假申请表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_leave_request (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_id          BIGINT NOT NULL,
    leave_type           VARCHAR(20) NOT NULL,
    start_date           DATE NOT NULL,
    end_date             DATE NOT NULL,
    days                 DECIMAL(4,1) NOT NULL,
    reason               TEXT,
    status               INTEGER DEFAULT 0,
    approve_id           BIGINT,
    approve_comment      TEXT,
    approve_time         TIMESTAMP,
    workflow_instance_id BIGINT,
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_leave_request IS '请假申请表';
COMMENT ON COLUMN hr_leave_request.leave_type IS '请假类型: ANNUAL-年假 SICK-病假 PERSONAL-事假 MATERNITY-产假 MARRIAGE-婚假';
COMMENT ON COLUMN hr_leave_request.status IS '申请状态: 0-待审批 1-已批准 2-已拒绝 3-已撤销';

CREATE INDEX idx_hr_leave_employee ON hr_leave_request(employee_id);

-- ─────────────────────────────────────────────────────────────────────
-- 6. 薪资结构表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_salary_structure (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_id          BIGINT NOT NULL,
    base_salary          DECIMAL(12,2) NOT NULL,
    performance_salary   DECIMAL(12,2) DEFAULT 0,
    position_allowance   DECIMAL(12,2) DEFAULT 0,
    transport_allowance  DECIMAL(12,2) DEFAULT 0,
    meal_allowance       DECIMAL(12,2) DEFAULT 0,
    housing_allowance    DECIMAL(12,2) DEFAULT 0,
    other_allowance      DECIMAL(12,2) DEFAULT 0,
    social_base          DECIMAL(12,2) DEFAULT 0,
    fund_base            DECIMAL(12,2) DEFAULT 0,
    effective_date       DATE,
    expiry_date          DATE,
    status               INTEGER DEFAULT 1,
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_salary_structure IS '薪资结构表';

CREATE INDEX idx_hr_salary_employee ON hr_salary_structure(employee_id);

-- ─────────────────────────────────────────────────────────────────────
-- 7. 薪资发放表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_salary_payment (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_id          BIGINT NOT NULL,
    payment_month        VARCHAR(10) NOT NULL,
    base_amount          DECIMAL(12,2) NOT NULL,
    performance_amount   DECIMAL(12,2) DEFAULT 0,
    allowance_amount     DECIMAL(12,2) DEFAULT 0,
    overtime_amount      DECIMAL(12,2) DEFAULT 0,
    deduct_amount        DECIMAL(12,2) DEFAULT 0,
    social_deduct        DECIMAL(12,2) DEFAULT 0,
    fund_deduct          DECIMAL(12,2) DEFAULT 0,
    tax_deduct           DECIMAL(12,2) DEFAULT 0,
    actual_amount        DECIMAL(12,2) NOT NULL,
    payment_date         DATE,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_salary_payment IS '薪资发放表';
COMMENT ON COLUMN hr_salary_payment.payment_month IS '发放月份(格式: yyyy-MM)';
COMMENT ON COLUMN hr_salary_payment.status IS '发放状态: 0-待发放 1-已发放 2-已撤销';

CREATE INDEX idx_hr_salary_payment_employee ON hr_salary_payment(employee_id);
CREATE INDEX idx_hr_salary_payment_month ON hr_salary_payment(payment_month);

-- ─────────────────────────────────────────────────────────────────────
-- 8. 绩效考核表
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS hr_performance (
    id                   BIGINT PRIMARY KEY,
    tenant_id            BIGINT DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by            BIGINT,
    update_by            BIGINT,
    employee_id          BIGINT NOT NULL,
    review_period        VARCHAR(20) NOT NULL,
    review_type          VARCHAR(20) DEFAULT 'MONTHLY',
    score                DECIMAL(5,2),
    level                VARCHAR(10),
    attitude_score       DECIMAL(5,2),
    ability_score        DECIMAL(5,2),
    achievement_score    DECIMAL(5,2),
    comment              TEXT,
    reviewer_id          BIGINT,
    reviewer_name        VARCHAR(100),
    review_time          TIMESTAMP,
    status               INTEGER DEFAULT 0,
    remark               VARCHAR(500)
);

COMMENT ON TABLE hr_performance IS '绩效考核表';
COMMENT ON COLUMN hr_performance.review_type IS '考核类型: MONTHLY-月度 QUARTERLY-季度 YEARLY-年度';
COMMENT ON COLUMN hr_performance.level IS '考核等级: S-A-B-C-D';

CREATE INDEX idx_hr_performance_employee ON hr_performance(employee_id);

-- ─────────────────────────────────────────────────────────────────────
-- 9. HR菜单项
-- ─────────────────────────────────────────────────────────────────────

-- HR主菜单 (id=900)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (900, 0, '人力资源', 'hr', 0, 'Layout', '/hr', 'team', 7, 1)
ON CONFLICT (id) DO NOTHING;

-- 组织架构菜单 (id=901)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (901, 900, '岗位管理', 'hr-position', 1, 'views/hr/organization/position-list', '/hr/position', NULL, 1, 1)
ON CONFLICT (id) DO NOTHING;

-- 员工管理菜单 (id=902)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (902, 900, '员工管理', 'hr-employee', 1, 'views/hr/employee/list', '/hr/employee', NULL, 2, 1)
ON CONFLICT (id) DO NOTHING;

-- 考勤管理菜单 (id=903)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (903, 900, '考勤管理', 'hr-attendance', 1, 'views/hr/attendance/list', '/hr/attendance', NULL, 3, 1)
ON CONFLICT (id) DO NOTHING;

-- 请假管理菜单 (id=904)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (904, 900, '请假管理', 'hr-leave', 1, 'views/hr/leave/list', '/hr/leave', NULL, 4, 1)
ON CONFLICT (id) DO NOTHING;

-- 薪酬管理菜单 (id=905)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (905, 900, '薪酬管理', 'hr-salary', 1, 'views/hr/salary/list', '/hr/salary', NULL, 5, 1)
ON CONFLICT (id) DO NOTHING;

-- 绩效管理菜单 (id=906)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES (906, 900, '绩效管理', 'hr-performance', 1, 'views/hr/performance/list', '/hr/performance', NULL, 6, 1)
ON CONFLICT (id) DO NOTHING;

-- 员工管理按钮权限
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, component, path, icon, sort, status)
VALUES
(9021, 902, '新增员工', 'hr-employee-create', 2, NULL, NULL, NULL, 1, 1),
(9022, 902, '编辑员工', 'hr-employee-edit', 2, NULL, NULL, NULL, 2, 1),
(9023, 902, '删除员工', 'hr-employee-delete', 2, NULL, NULL, NULL, 3, 1)
ON CONFLICT (id) DO NOTHING;