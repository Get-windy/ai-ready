-- ───────────────────────────────────────────────────────────
-- V11.62.0 预警设置页对标复刻
-- 商品主数据补充属性列：口味（taste）、小单位（small_unit）
-- 供「库存预警固定值设置」与「预警查询」展示真实商品属性，
-- 消除 StockAlertQueryMapper 中 NULL AS taste / small_unit 的占位。
-- ───────────────────────────────────────────────────────────
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS taste VARCHAR(100);
ALTER TABLE erp_product ADD COLUMN IF NOT EXISTS small_unit VARCHAR(50);

-- 预警配置查询性能：按「商品×仓库」建立组合索引（仅未删除）
CREATE INDEX IF NOT EXISTS idx_erp_stock_alert_config_pd_wh
    ON erp_stock_alert_config (product_id, warehouse_id)
    WHERE deleted = 0;

-- 确保 5013「预警设置」菜单指向正确的维护页（单入口 display_mode=0）
UPDATE sys_menu SET
    menu_code   = 'erp:stock-alert-config',
    path        = 'erp/stock-alert-config',
    component   = 'erp/stock-alert-config/index',
    list_path   = 'erp/stock-alert-config/index',
    display_mode = 0,
    update_time = CURRENT_TIMESTAMP
WHERE id = 5013;
