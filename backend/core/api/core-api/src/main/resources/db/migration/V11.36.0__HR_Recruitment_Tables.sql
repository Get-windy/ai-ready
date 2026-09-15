-- ============================================================
-- V11.36.0: HR 招聘模块建表 + 菜单种子数据
--
-- 新增：
--   1. hr_recruitment   —— 招聘岗位表
--   2. hr_candidate     —— 候选人表
--   3. sys_menu 招聘管理目录 + 按钮权限
--
-- 幂等：IF NOT EXISTS / ON CONFLICT DO NOTHING / WHERE NOT EXISTS 守卫
-- ============================================================

-- ----------------------------------------------------------
-- 1. hr_recruitment 招聘岗位表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_recruitment (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          DEFAULT 0,
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    position_id         BIGINT,
    position_name       VARCHAR(100),
    dept_id             BIGINT,
    dept_name           VARCHAR(100),
    headcount           INTEGER         DEFAULT 1,
    channel             VARCHAR(20),
    urgency             INTEGER         DEFAULT 1,
    status              INTEGER         DEFAULT 0,
    required_education  INTEGER,
    required_experience VARCHAR(100),
    salary_min          DECIMAL(12,2),
    salary_max          DECIMAL(12,2),
    description         TEXT,
    requirements        TEXT,
    publisher_id        BIGINT,
    publisher_name      VARCHAR(100),
    publish_date        DATE,
    expire_date         DATE,
    applicant_count     INTEGER         DEFAULT 0,
    hired_count         INTEGER         DEFAULT 0,
    remark              VARCHAR(500)
);

COMMENT ON TABLE hr_recruitment IS 'HR招聘岗位表';
COMMENT ON COLUMN hr_recruitment.id IS '主键ID';
COMMENT ON COLUMN hr_recruitment.tenant_id IS '租户ID';
COMMENT ON COLUMN hr_recruitment.deleted IS '逻辑删除(0=未删除)';
COMMENT ON COLUMN hr_recruitment.position_id IS '招聘岗位ID';
COMMENT ON COLUMN hr_recruitment.position_name IS '岗位名称';
COMMENT ON COLUMN hr_recruitment.dept_id IS '部门ID';
COMMENT ON COLUMN hr_recruitment.dept_name IS '部门名称';
COMMENT ON COLUMN hr_recruitment.headcount IS '招聘人数';
COMMENT ON COLUMN hr_recruitment.channel IS '招聘渠道';
COMMENT ON COLUMN hr_recruitment.urgency IS '紧急程度(1=普通,2=紧急,3=特急)';
COMMENT ON COLUMN hr_recruitment.status IS '状态(0=草稿,1=招聘中,2=已暂停,3=已完成,4=已关闭)';
COMMENT ON COLUMN hr_recruitment.required_education IS '学历要求(1=不限,2=高中,3=大专,4=本科,5=硕士,6=博士)';
COMMENT ON COLUMN hr_recruitment.required_experience IS '经验要求';
COMMENT ON COLUMN hr_recruitment.salary_min IS '薪资下限';
COMMENT ON COLUMN hr_recruitment.salary_max IS '薪资上限';
COMMENT ON COLUMN hr_recruitment.description IS '岗位描述';
COMMENT ON COLUMN hr_recruitment.requirements IS '任职要求';
COMMENT ON COLUMN hr_recruitment.publisher_id IS '发布人ID';
COMMENT ON COLUMN hr_recruitment.publisher_name IS '发布人姓名';
COMMENT ON COLUMN hr_recruitment.publish_date IS '发布日期';
COMMENT ON COLUMN hr_recruitment.expire_date IS '截止日期';
COMMENT ON COLUMN hr_recruitment.applicant_count IS '应聘人数';
COMMENT ON COLUMN hr_recruitment.hired_count IS '录用人数';

CREATE INDEX IF NOT EXISTS idx_hr_recruitment_dept_id     ON hr_recruitment(dept_id);
CREATE INDEX IF NOT EXISTS idx_hr_recruitment_status      ON hr_recruitment(status);
CREATE INDEX IF NOT EXISTS idx_hr_recruitment_tenant_id   ON hr_recruitment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_hr_recruitment_publish_date ON hr_recruitment(publish_date);

-- ----------------------------------------------------------
-- 2. hr_candidate 候选人表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS hr_candidate (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          DEFAULT 0,
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    recruitment_id      BIGINT          NOT NULL,
    name                VARCHAR(100)    NOT NULL,
    gender              INTEGER         DEFAULT 0,
    phone               VARCHAR(20),
    email               VARCHAR(100),
    birth_date          DATE,
    education           INTEGER,
    school              VARCHAR(200),
    major               VARCHAR(100),
    experience          VARCHAR(50),
    current_company     VARCHAR(200),
    current_position    VARCHAR(100),
    expected_salary     DECIMAL(12,2),
    source              VARCHAR(50),
    resume_url          VARCHAR(500),
    status              INTEGER         DEFAULT 0,
    interviewer_id      BIGINT,
    interviewer_name    VARCHAR(100),
    interview_time      TIMESTAMP,
    interview_comment   TEXT,
    rating              INTEGER,
    remark              VARCHAR(500)
);

COMMENT ON TABLE hr_candidate IS 'HR候选人表';
COMMENT ON COLUMN hr_candidate.id IS '主键ID';
COMMENT ON COLUMN hr_candidate.tenant_id IS '租户ID';
COMMENT ON COLUMN hr_candidate.deleted IS '逻辑删除(0=未删除)';
COMMENT ON COLUMN hr_candidate.recruitment_id IS '关联招聘岗位ID';
COMMENT ON COLUMN hr_candidate.name IS '候选人姓名';
COMMENT ON COLUMN hr_candidate.gender IS '性别(0=未知,1=男,2=女)';
COMMENT ON COLUMN hr_candidate.phone IS '手机号';
COMMENT ON COLUMN hr_candidate.email IS '邮箱';
COMMENT ON COLUMN hr_candidate.birth_date IS '出生日期';
COMMENT ON COLUMN hr_candidate.education IS '学历(1=不限,2=高中,3=大专,4=本科,5=硕士,6=博士)';
COMMENT ON COLUMN hr_candidate.school IS '毕业院校';
COMMENT ON COLUMN hr_candidate.major IS '专业';
COMMENT ON COLUMN hr_candidate.experience IS '工作年限';
COMMENT ON COLUMN hr_candidate.current_company IS '当前公司';
COMMENT ON COLUMN hr_candidate.current_position IS '当前职位';
COMMENT ON COLUMN hr_candidate.expected_salary IS '期望薪资';
COMMENT ON COLUMN hr_candidate.source IS '来源渠道';
COMMENT ON COLUMN hr_candidate.resume_url IS '简历文件URL';
COMMENT ON COLUMN hr_candidate.status IS '状态(0=待筛选,1=初试,2=复试,3=终面,4=待录用,5=已录用,6=已淘汰)';
COMMENT ON COLUMN hr_candidate.interviewer_id IS '面试官ID';
COMMENT ON COLUMN hr_candidate.interviewer_name IS '面试官姓名';
COMMENT ON COLUMN hr_candidate.interview_time IS '面试时间';
COMMENT ON COLUMN hr_candidate.interview_comment IS '面试评价';
COMMENT ON COLUMN hr_candidate.rating IS '评分(1-5)';

CREATE INDEX IF NOT EXISTS idx_hr_candidate_recruitment_id ON hr_candidate(recruitment_id);
CREATE INDEX IF NOT EXISTS idx_hr_candidate_status         ON hr_candidate(status);
CREATE INDEX IF NOT EXISTS idx_hr_candidate_phone          ON hr_candidate(phone);
CREATE INDEX IF NOT EXISTS idx_hr_candidate_tenant_id      ON hr_candidate(tenant_id);

-- ----------------------------------------------------------
-- 3. sys_menu 招聘管理目录 + 按钮权限
--    目录：id=907, parent_id=900 (HR模块)
--    按钮：9071=新增, 9072=编辑, 9073=删除
-- ----------------------------------------------------------

-- 3.1 招聘管理目录菜单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
SELECT 907, 0, 900, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '招聘管理', 'hr-recruitment', 1,
    '/hr/recruitment', 'views/hr/recruitment/list', 7,
    1, 1, 'tenant-admin', 0, 0, 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 907 AND tenant_id = 0);

-- 3.2 按钮权限 - 新增
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort,
    visible, status, client_type, menu_level)
SELECT 9071, 0, 907, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '新增', 'hr-recruitment:add', 3, 1,
    1, 1, 'tenant-admin', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 9071 AND tenant_id = 0);

-- 3.3 按钮权限 - 编辑
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort,
    visible, status, client_type, menu_level)
SELECT 9072, 0, 907, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '编辑', 'hr-recruitment:edit', 3, 2,
    1, 1, 'tenant-admin', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 9072 AND tenant_id = 0);

-- 3.4 按钮权限 - 删除
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort,
    visible, status, client_type, menu_level)
SELECT 9073, 0, 907, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '删除', 'hr-recruitment:delete', 3, 3,
    1, 1, 'tenant-admin', 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 9073 AND tenant_id = 0);
