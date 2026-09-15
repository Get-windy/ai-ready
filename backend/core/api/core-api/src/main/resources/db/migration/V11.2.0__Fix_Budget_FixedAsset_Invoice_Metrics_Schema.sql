-- ============================================================
-- V11.2.0 budget / fixed-asset / invoice / metrics 四域 schema 对账收尾
--
-- 背景：
--   V11.1.0 已完成主体修复（16 张 0 行残表 DROP+按实体重建、
--   erp_business_metric 87 万行表仅 ALTER 补"指标定义"列）。
--   本迁移是对账复核后发现的剩余差异，全部为幂等补索引。
--
-- 剩余差异（实体 @Table(indexes=@Index(...)) 已声明但库中缺失）：
--   fixed_asset_disposal   4 个索引
--   fixed_asset_inventory  4 个索引
--   fixed_asset_transfer   4 个索引
--
-- 有意不做的事（如实记录，交由上级决策）：
--   1. payment_record：现表结构属于 core-payment 域（MyBatis-Plus
--      @TableName("payment_record")，IdType.ASSIGN_ID 雪花主键，0 行），
--      与 erp-finance invoice 域 JPA 实体 PaymentRecord（@Table(name="payment_record")，
--      40+ 列付款匹配结构）语义完全不同。两域共用同名表是设计冲突，
--      不擅自合并/重建，invoice 域实体需改名（如 invoice_payment_record）或另行决策。
--   2. erp_business_metric.unit 现为 VARCHAR(20)，实体声明 length=32。
--      表有 87 万行数据，按修复规则禁止改类型，仅记录（JPA 不校验长度，
--      仅当写入超过 20 字符的 unit 时才会报错，风险低）。
--   3. erp_metric_aggregation / erp_metric_data 数值列为 NUMERIC(18,4)，
--      实体声明 precision=19。有数据表禁止改类型，仅记录（精度上限差一位，
--      实际业务数值不会触顶，风险可忽略）。
--   4. erp_business_metric 实体 nullable=false 列（metric_code/metric_name/
--      metric_type/period/status）在现有 87 万行表上无法安全加 NOT NULL，
--      不在本迁移处理。
-- ============================================================

-- fixed_asset_disposal（@Index: disposal_no / asset_id / asset_code / status）
CREATE INDEX IF NOT EXISTS idx_fadisposal_no ON fixed_asset_disposal (disposal_no);
CREATE INDEX IF NOT EXISTS idx_fadisposal_asset_id ON fixed_asset_disposal (asset_id);
CREATE INDEX IF NOT EXISTS idx_fadisposal_asset_code ON fixed_asset_disposal (asset_code);
CREATE INDEX IF NOT EXISTS idx_fadisposal_status ON fixed_asset_disposal (status);

-- fixed_asset_inventory（@Index: inventory_no / asset_id / asset_code / status）
CREATE INDEX IF NOT EXISTS idx_fai_inventory_no ON fixed_asset_inventory (inventory_no);
CREATE INDEX IF NOT EXISTS idx_fai_asset_id ON fixed_asset_inventory (asset_id);
CREATE INDEX IF NOT EXISTS idx_fai_asset_code ON fixed_asset_inventory (asset_code);
CREATE INDEX IF NOT EXISTS idx_fai_status ON fixed_asset_inventory (status);

-- fixed_asset_transfer（@Index: transfer_no / asset_id / asset_code / status）
CREATE INDEX IF NOT EXISTS idx_fat_transfer_no ON fixed_asset_transfer (transfer_no);
CREATE INDEX IF NOT EXISTS idx_fat_asset_id ON fixed_asset_transfer (asset_id);
CREATE INDEX IF NOT EXISTS idx_fat_asset_code ON fixed_asset_transfer (asset_code);
CREATE INDEX IF NOT EXISTS idx_fat_status ON fixed_asset_transfer (status);
