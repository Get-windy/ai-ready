-- 地图服务配置落位（DMS《路线规划》+ 配送路线单路线优化共用）
--
-- 背景：路线规划页的降级提示要求「Key 配在哪」可见可操作。此前 Key 只写在 dms 模块自带的
--   dms-application.yml 里，而**模块级 application.yml 位于嵌套 jar 内不会被 core-api 加载**，
--   环境变量 AMAP_API_KEY 实际读不到 → 页面永远降级。修复分两步：
--   ① 在 core-api application.yml 显式声明 dms.map.* + amap.key（环境变量/外部配置文件生效）；
--   ② 本迁移把「配置中心」四个键预置进 dms_config（《配送参数》页面可直接编辑、保存即热生效）。
--
-- 读取优先级（MapKeyResolver）：环境变量 > Spring 配置 > 本表(配置中心) > 内置默认（直线降级）
-- 预置说明：全局默认行 tenant_id = 0，value 留空（未配置 → 页面显示降级指引）；
--   租户在《配送参数》保存时会生成自己的覆盖行（tenant_id = 当前租户），只影响本租户。

INSERT INTO dms_config (tenant_id, config_key, config_value, config_desc, scope, deleted, version, create_time, update_time)
VALUES
    (0, 'map.default-provider',  'amap', '地图服务商：amap-高德 / tencent-腾讯 / baidu-百度；未配置对应 Key 时自动降级为本地直线模式（local）', 'TENANT', 0, 0, NOW(), NOW()),
    (0, 'map.amap.api-key',      '',     '高德地图 Web 服务 Key（推荐用环境变量 AMAP_API_KEY 配置；此处填写后保存即热生效，无需重启）', 'TENANT', 0, 0, NOW(), NOW()),
    (0, 'map.tencent.api-key',   '',     '腾讯位置服务 Key（推荐用环境变量 TENCENT_MAP_API_KEY 配置；需同时把 map.default-provider 设为 tencent）', 'TENANT', 0, 0, NOW(), NOW()),
    (0, 'map.baidu.api-key',     '',     '百度地图 Web 服务 AK（推荐用环境变量 BAIDU_MAP_API_KEY 配置；需同时把 map.default-provider 设为 baidu）', 'TENANT', 0, 0, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 防止同一租户下配置键重复（并发保存会产生重复行 → 读取取第一条，语义不确定）
CREATE UNIQUE INDEX IF NOT EXISTS uk_dms_config_tenant_key
    ON dms_config (tenant_id, config_key) WHERE deleted = 0;

COMMENT ON TABLE dms_config IS 'DMS 租户配置（含地图服务 Key 等；全局默认 tenant_id=0 + 租户覆盖）';
