-- 存量 wms_inventory 库存行补商品快照
-- 历史行在 increase 建行时未写商品冗余字段（productCode/productName/spec/unit），此处从 erp_product 回填；
-- 仅更新冗余展示字段，不动 quantity/available_quantity 等数量字段。
UPDATE wms_inventory wi
SET product_code = p.product_code,
    product_name = p.product_name,
    product_spec = p.spec,
    product_unit = p.unit
FROM erp_product p
WHERE wi.product_id = p.id
  AND (wi.product_code IS NULL OR wi.product_code = ''
       OR wi.product_name IS NULL OR wi.product_name = ''
       OR wi.product_spec IS NULL OR wi.product_spec = ''
       OR wi.product_unit IS NULL OR wi.product_unit = '');
