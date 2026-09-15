-- V11.38.0 质量证书(COA)管理表

CREATE TABLE IF NOT EXISTS quality_certificate (
    id              BIGINT          NOT NULL PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL DEFAULT 1,
    certificate_no  VARCHAR(64),
    certificate_type VARCHAR(32),
    product_name    VARCHAR(255),
    product_code    VARCHAR(128),
    batch_no        VARCHAR(128),
    supplier_name   VARCHAR(255),
    inspection_date DATE,
    issue_date      DATE,
    expiry_date     DATE,
    result          VARCHAR(32),
    inspector_id    BIGINT,
    inspector_name  VARCHAR(128),
    certificate_url VARCHAR(1024),
    remark          VARCHAR(1024),
    status          INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    version         INTEGER         NOT NULL DEFAULT 0
);

COMMENT ON TABLE quality_certificate IS '质量证书(COA)管理表';
COMMENT ON COLUMN quality_certificate.certificate_no IS '证书编号(COA-yyyymmdd-xxxx)';
COMMENT ON COLUMN quality_certificate.certificate_type IS '证书类型(COA/COC/ISO/OTHER)';
COMMENT ON COLUMN quality_certificate.result IS '检验结论(QUALIFIED/UNQUALIFIED/CONDITIONAL)';
COMMENT ON COLUMN quality_certificate.status IS '状态(0-草稿 1-已生效 2-已过期 3-已撤销)';

CREATE INDEX IF NOT EXISTS idx_quality_certificate_product_code ON quality_certificate(product_code);
CREATE INDEX IF NOT EXISTS idx_quality_certificate_batch_no ON quality_certificate(batch_no);
CREATE INDEX IF NOT EXISTS idx_quality_certificate_tenant_id ON quality_certificate(tenant_id);
CREATE INDEX IF NOT EXISTS idx_quality_certificate_result ON quality_certificate(result);
CREATE INDEX IF NOT EXISTS idx_quality_certificate_certificate_no ON quality_certificate(certificate_no);
