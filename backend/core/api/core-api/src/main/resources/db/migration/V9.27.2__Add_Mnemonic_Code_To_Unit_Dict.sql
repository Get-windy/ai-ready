-- 商品单位字典表增加助记码
ALTER TABLE erp_product_unit_dict ADD COLUMN IF NOT EXISTS mnemonic_code VARCHAR(50);

COMMENT ON COLUMN erp_product_unit_dict.mnemonic_code IS '助记码(拼音首字母)';
