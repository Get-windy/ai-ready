-- 联系人升级为「与往来单位平行的独立实体」+ 多对多关联；会员级别独立字典
--
-- 背景：biz_party_contact 原设计把「人」的属性（姓名/手机/性别/微信…）与「人-单位关系」属性
--       （是否主联系人/职务/配送方式/区域…）混在一张表，导致同一个人服务 N 个单位就冗余 N 份。
--       实测 devdb：67 条记录其实只有 16 个不同的人。
-- 改造：biz_contact = 人（独立主数据）；biz_party_contact = 人↔往来单位 的关联（多对多，保留关系上下文）。

-- ─────────────────────────────────────────────
-- 1) 独立联系人实体
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS biz_contact (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL DEFAULT 0,
    contact_name VARCHAR(100) NOT NULL,
    gender       VARCHAR(10),
    mobile       VARCHAR(50),
    phone        VARCHAR(50),
    email        VARCHAR(200),
    wechat       VARCHAR(100),
    qq           VARCHAR(50),
    birthday     DATE,
    remark       VARCHAR(500),
    status       SMALLINT     DEFAULT 1,
    create_by    BIGINT,
    create_time  TIMESTAMP    DEFAULT NOW(),
    update_by    BIGINT,
    update_time  TIMESTAMP    DEFAULT NOW(),
    deleted      SMALLINT     DEFAULT 0
);
COMMENT ON TABLE biz_contact IS '联系人（与往来单位平行的独立主数据；一个联系人可服务多个往来单位）';

CREATE INDEX IF NOT EXISTS idx_biz_contact_tenant ON biz_contact (tenant_id);
CREATE INDEX IF NOT EXISTS idx_biz_contact_mobile ON biz_contact (mobile);
CREATE INDEX IF NOT EXISTS idx_biz_contact_name ON biz_contact (contact_name);

-- ─────────────────────────────────────────────
-- 2) 关联表：biz_party_contact 增加 contact_id（保留原列以兼容历史读取）
-- ─────────────────────────────────────────────
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS contact_id BIGINT;
COMMENT ON COLUMN biz_party_contact.contact_id IS '关联的独立联系人 biz_contact.id（多对多）';
CREATE INDEX IF NOT EXISTS idx_biz_party_contact_cid ON biz_party_contact (contact_id);

-- ─────────────────────────────────────────────
-- 3) 数据迁移：把「人」按 手机号 > 电话 > 姓名 归并后抽到 biz_contact
-- ─────────────────────────────────────────────
INSERT INTO biz_contact (tenant_id, contact_name, gender, mobile, phone, email, wechat, qq, remark, status, create_time, update_time, deleted)
SELECT DISTINCT ON (pc.tenant_id, COALESCE(NULLIF(pc.mobile, ''), NULLIF(pc.phone, ''), pc.contact_name))
       pc.tenant_id,
       pc.contact_name,
       NULLIF(pc.gender, ''),
       NULLIF(pc.mobile, ''),
       NULLIF(pc.phone, ''),
       NULLIF(pc.email, ''),
       NULLIF(pc.wechat, ''),
       NULLIF(pc.qq, ''),
       NULLIF(pc.remark, ''),
       1,
       COALESCE(pc.create_time, NOW()),
       COALESCE(pc.update_time, NOW()),
       0
FROM biz_party_contact pc
WHERE pc.deleted = 0
  AND pc.contact_name IS NOT NULL
  AND pc.contact_name <> ''
  AND NOT EXISTS (
      SELECT 1 FROM biz_contact c
      WHERE c.deleted = 0
        AND c.tenant_id = pc.tenant_id
        AND COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, ''), c.contact_name)
            = COALESCE(NULLIF(pc.mobile, ''), NULLIF(pc.phone, ''), pc.contact_name)
  )
ORDER BY pc.tenant_id,
         COALESCE(NULLIF(pc.mobile, ''), NULLIF(pc.phone, ''), pc.contact_name),
         pc.id;

-- ─────────────────────────────────────────────
-- 4) 回填关联：把每条 (单位, 人) 关系指向独立联系人
-- ─────────────────────────────────────────────
UPDATE biz_party_contact pc
SET contact_id = c.id
FROM biz_contact c
WHERE pc.deleted = 0
  AND pc.contact_id IS NULL
  AND c.deleted = 0
  AND c.tenant_id = pc.tenant_id
  AND COALESCE(NULLIF(c.mobile, ''), NULLIF(c.phone, ''), c.contact_name)
      = COALESCE(NULLIF(pc.mobile, ''), NULLIF(pc.phone, ''), pc.contact_name);

-- ─────────────────────────────────────────────
-- 5) 会员级别：独立字典（对标「会员卡」上的会员级别*，与「客户级别=拿货价格等级」是两套体系）
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS erp_member_level (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL DEFAULT 0,
    level_name    VARCHAR(64)  NOT NULL,
    discount_rate NUMERIC(5,2) DEFAULT 100.00,
    sort_order    INTEGER      DEFAULT 0,
    status        SMALLINT     DEFAULT 1,
    remark        VARCHAR(200),
    create_by     BIGINT,
    create_time   TIMESTAMP    DEFAULT NOW(),
    update_by     BIGINT,
    update_time   TIMESTAMP    DEFAULT NOW(),
    deleted       SMALLINT     DEFAULT 0
);
COMMENT ON TABLE erp_member_level IS '会员级别（营销权益等级，区别于客户级别/价格等级）';

INSERT INTO erp_member_level (tenant_id, level_name, discount_rate, sort_order, status)
SELECT t.id, v.level_name, v.discount_rate, v.sort_order, 1
FROM sys_tenant t
CROSS JOIN (VALUES
    ('普通会员',   100.00, 1),
    ('银卡会员',    95.00, 2),
    ('金卡会员',    90.00, 3),
    ('钻石会员',    85.00, 4)
) AS v(level_name, discount_rate, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM erp_member_level m WHERE m.tenant_id = t.id AND m.level_name = v.level_name AND m.deleted = 0
);
