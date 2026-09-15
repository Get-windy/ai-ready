-- ============================================================================
-- V11.150.0 biz_party_contact 补 tenant_id（2026-09-11）
--
-- 背景：多租户插件（TenantLineHandler）对 biz_party_contact 生效——INSERT 会自动
--      追加 tenant_id、SELECT 会自动追加 tenant_id = 当前租户 过滤，但该表建表时
--      漏了这一列。结果：往来单位「联系人/网点」子表的读写全部 500
--      （PSQLException: 关系 "biz_party_contact" 的 "tenant_id" 字段不存在）。
--
-- 影响面：客户 / 供应商 / 物流公司 / 其他往来单位 四类基础资料的表单联系人、
--        物流公司网点，均无法保存。
--
-- 处理：补列 + 按所属往来单位回填 + 对齐 biz_party 的默认值口径（0，非空）。
--      幂等，可重复执行。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 补列
-- ------------------------------------------------------------
ALTER TABLE biz_party_contact ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

-- ------------------------------------------------------------
-- 2. 回填：继承所属往来单位的租户；无归属的置 0（与 biz_party 默认值一致）
-- ------------------------------------------------------------
UPDATE biz_party_contact c
   SET tenant_id = COALESCE(p.tenant_id, 0)
  FROM biz_party p
 WHERE c.party_id = p.id
   AND c.tenant_id IS NULL;

UPDATE biz_party_contact SET tenant_id = 0 WHERE tenant_id IS NULL;

-- ------------------------------------------------------------
-- 3. 对齐 biz_party：默认 0 + 非空
-- ------------------------------------------------------------
ALTER TABLE biz_party_contact ALTER COLUMN tenant_id SET DEFAULT 0;
ALTER TABLE biz_party_contact ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_biz_party_contact_tenant ON biz_party_contact (tenant_id);

-- ------------------------------------------------------------
-- 4. 验证
-- ------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'biz_party_contact' AND column_name = 'tenant_id') THEN
        RAISE EXCEPTION 'biz_party_contact.tenant_id 未创建成功';
    END IF;
    IF EXISTS (SELECT 1 FROM biz_party_contact WHERE tenant_id IS NULL) THEN
        RAISE EXCEPTION 'biz_party_contact.tenant_id 仍存在 NULL';
    END IF;
END $$;
