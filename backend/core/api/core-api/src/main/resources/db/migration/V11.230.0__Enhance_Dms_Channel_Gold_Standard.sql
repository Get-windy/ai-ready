-- 渠道管理金标准：dms_channel 补「对接状态 / 连通性试探 / 覆盖区域 / 计费规则」字段
-- 背景：《渠道管理开发文档》§4 差异表 —— 原表仅有 编码/名称/类型/优先级/状态/备注，
--       无法承载外部运力平台的对接与计费信息；在线运力不落冗余列（实时按 dms_rider 统计）。
-- 列数：17 + 6 = 23（符合「单表 ≤ 25 列」规范）。

ALTER TABLE dms_channel ADD COLUMN IF NOT EXISTS link_status      INTEGER      DEFAULT 0;
ALTER TABLE dms_channel ADD COLUMN IF NOT EXISTS last_test_time   TIMESTAMP;
ALTER TABLE dms_channel ADD COLUMN IF NOT EXISTS last_test_result VARCHAR(500);
ALTER TABLE dms_channel ADD COLUMN IF NOT EXISTS coverage_area    VARCHAR(500);
ALTER TABLE dms_channel ADD COLUMN IF NOT EXISTS billing_type     INTEGER      DEFAULT 1;
ALTER TABLE dms_channel ADD COLUMN IF NOT EXISTS billing_config   VARCHAR(1000);

COMMENT ON COLUMN dms_channel.link_status      IS '对接状态：0-未对接 1-已对接 2-对接异常';
COMMENT ON COLUMN dms_channel.last_test_time   IS '最近一次连通性测试时间';
COMMENT ON COLUMN dms_channel.last_test_result IS '最近一次连通性测试结果摘要';
COMMENT ON COLUMN dms_channel.coverage_area    IS '覆盖区域（行政区划名称，逗号分隔）';
COMMENT ON COLUMN dms_channel.billing_type     IS '计费方式：1-按单 2-按距 3-按重';
COMMENT ON COLUMN dms_channel.billing_config   IS '计费规则 JSON：起步价/单价/加价等';

-- 渠道编码同租户内唯一（软删除后允许复用），与线路主数据同一约定
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_channel_code
    ON dms_channel (tenant_id, channel_code) WHERE deleted = 0;
