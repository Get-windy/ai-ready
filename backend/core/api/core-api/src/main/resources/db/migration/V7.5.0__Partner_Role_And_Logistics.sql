-- V7.5.0: 往来单位"主体+角色"模型扩展
-- 1. 新增 erp_partner_role 角色关联表（支持一个单位多角色）
-- 2. 新增 erp_partner_logistics_ext 物流公司扩展表
-- 3. 迁移现有 partner 数据到角色表

-- ============================================================
-- 1. 往来单位角色关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_partner_role (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL,
    role_type       VARCHAR(30) NOT NULL,
    is_primary      INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'ENABLED',
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(partner_id, role_type)
);

COMMENT ON TABLE erp_partner_role IS '往来单位角色关联表（支持多角色：客户/供应商/物流商等）';

CREATE INDEX IF NOT EXISTS idx_prole_tenant ON erp_partner_role(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prole_partner ON erp_partner_role(partner_id);
CREATE INDEX IF NOT EXISTS idx_prole_type ON erp_partner_role(role_type);

-- ============================================================
-- 2. 物流公司扩展信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_partner_logistics_ext (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    partner_id          BIGINT NOT NULL,
    logistics_type      INTEGER,
    service_area        VARCHAR(500),
    transport_modes     VARCHAR(200),
    vehicle_count       INTEGER DEFAULT 0,
    cold_chain          INTEGER DEFAULT 0,
    hazardous           INTEGER DEFAULT 0,
    service_phone       VARCHAR(50),
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(partner_id)
);

COMMENT ON TABLE erp_partner_logistics_ext IS '物流公司扩展信息表';

CREATE INDEX IF NOT EXISTS idx_logext_tenant ON erp_partner_logistics_ext(tenant_id);
CREATE INDEX IF NOT EXISTS idx_logext_partner ON erp_partner_logistics_ext(partner_id);

-- ============================================================
-- 3. 迁移现有 partner 数据到角色表
-- ============================================================
INSERT INTO erp_partner_role (tenant_id, partner_id, role_type, is_primary, status, create_time, update_time)
SELECT DISTINCT tenant_id, id, partner_type, 1, status, create_time, update_time
FROM erp_partner
WHERE deleted = 0 AND partner_type IS NOT NULL AND partner_type != ''
ON CONFLICT (partner_id, role_type) DO NOTHING;
