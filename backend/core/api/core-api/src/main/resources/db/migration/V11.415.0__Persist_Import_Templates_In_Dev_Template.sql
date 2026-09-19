-- ============================================================================
-- 模板管理（系统 → 开发工具 → 模板管理，菜单 62402）导入模板落库改造
-- 2026-09-19
--
-- 背景：ImportTemplateController（前缀 /api/import-templates，13 个端点）此前把 4 个导入模板
--       定义硬编码在**进程内存**（ImportTemplateRegistry 的两个 ConcurrentHashMap）里 →
--       删除不落库、重启即复原、多实例各读各的内存（配套文档 §7.5 / §12-P0②）。
-- 本迁移：把 dev_template 复用为导入模板的**持久化落位**（配套文档 §7.3-P1 处置方案②）。
--
-- ⚠️ 为什么需要 template_kind 判别列：
--     dev_template 现有 7 行是「代码生成」路线的遗留种子（type = entity/controller/service/mapper/frontend，
--     content 是含 package 占位符的代码片段），与导入模板（type = user/customer/product/order，
--     ⚠️ 写注释时**不要出现**「美元符号 + 花括号」的占位符写法：Flyway 会对**整个迁移文件
--        （含注释）**做占位符替换，未定义的键会直接让应用启动失败
--        （实踩：本文件初版注释里写了该写法 → 启动报
--        `Unable to parse statement ... No value provided for placeholder`）。
--     content 是**字段定义 JSON**）**语义完全不同**。若不加判别列：
--       ① 这 7 行会混进 /api/import-templates 列表（预览 0 字段、下载出空模板）；
--       ② 页面「删除」会误删这批遗留种子；
--       ③ dev_template.type 一列会同时承载两套值域（本仓已多次踩过「一列两套语义」的坑）。
--     故新增 template_kind 判别列；已有 7 行按 DEFAULT 归为 codegen，**数据一字不改**。
--
-- 列映射（内存对象 ImportTemplateDefinition → dev_template）：
--   templateId   → code          templateName → name
--   dataType     → type          description  → description
--   version      → version       createTime/updateTime → create_time/update_time
--   fields / sampleRowCount / maxImportRows / strictValidation → content（JSON；表列无法表达的部分，
--                                                                 不重复存储 name/code 等已有列）
--   enabled        恒 TRUE（/api/import-templates 无启用/停用概念，不下发该字段）
--   created_by / updated_by  由应用层按会话登录账号写入（此前内存态无任何审计留痕）
--
-- ⚠️ 本表**没有 tenant_id 列、也没有 deleted 列**（真库 information_schema 已核）：
--    · tenant_id → 本表已登记进 MyBatisPlusConfig.IGNORE_TENANT_TABLES（不登记时多租户拦截器
--      会注入 `AND tenant_id = ?` → 直接报「字段 tenant_id 不存在」500）；
--    · deleted   → 删除走**物理删**，不使用 @TableLogic（无列却标 @TableLogic 会报列不存在）。
-- ============================================================================

-- 1. 判别列（幂等；PG 11+ 带默认值加列不重写表，已有 7 行按 DEFAULT 取 codegen）
ALTER TABLE dev_template ADD COLUMN IF NOT EXISTS template_kind VARCHAR(20) NOT NULL DEFAULT 'codegen';
COMMENT ON COLUMN dev_template.template_kind IS '模板类别: codegen=代码生成模板(遗留种子) / import=导入模板(模板管理页 62402)';

-- 2. 同类别下 code(=templateId) 唯一：内存 Map 是「后写覆盖」，落库后由唯一索引兜住重复注册
--    部分索引（WHERE code IS NOT NULL）以容忍 code 为空的 codegen 行
CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_template_kind_code
    ON dev_template (template_kind, code) WHERE code IS NOT NULL;

-- 3. 种子：原 ImportTemplateRegistry 硬编码的 4 个导入模板，逐字迁移（字段定义/示例值/校验规则原样）
--    此行起为唯一数据源：Java 侧不再保留第二份硬编码定义（避免双份真相）
INSERT INTO dev_template (name, code, type, content, description, version, enabled, template_kind)
VALUES
('用户导入模板', 'tpl_user_import', 'user',
 '{"fields":[{"fieldName":"username","fieldTitle":"用户名","fieldType":"STRING","required":true,"maxLength":50,"unique":true,"sampleValue":"zhangsan","description":"登录用户名，唯一标识","order":1},{"fieldName":"email","fieldTitle":"邮箱","fieldType":"EMAIL","required":true,"sampleValue":"zhangsan@example.com","description":"用户邮箱地址","order":2},{"fieldName":"phone","fieldTitle":"手机号","fieldType":"PHONE","required":false,"sampleValue":"13800138000","regexPattern":"^1[3-9]\\d{9}$","description":"11位手机号码","order":3},{"fieldName":"realName","fieldTitle":"真实姓名","fieldType":"STRING","required":true,"maxLength":20,"sampleValue":"张三","order":4},{"fieldName":"deptId","fieldTitle":"部门ID","fieldType":"REFERENCE","required":false,"referenceType":"department","description":"所属部门ID","order":5},{"fieldName":"status","fieldTitle":"状态","fieldType":"ENUM","required":true,"dropdownOptions":{"0":"禁用","1":"启用"},"defaultValue":"1","sampleValue":"1","order":6}],"sampleRowCount":3,"maxImportRows":10000,"strictValidation":true}',
 '批量导入系统用户数据', '1.0.0', TRUE, 'import'),
('客户导入模板', 'tpl_customer_import', 'customer',
 '{"fields":[{"fieldName":"customerName","fieldTitle":"客户名称","fieldType":"STRING","required":true,"maxLength":100,"sampleValue":"测试公司","order":1},{"fieldName":"contact","fieldTitle":"联系人","fieldType":"STRING","required":true,"maxLength":50,"sampleValue":"李经理","order":2},{"fieldName":"phone","fieldTitle":"联系电话","fieldType":"PHONE","required":true,"sampleValue":"13800138000","order":3},{"fieldName":"email","fieldTitle":"邮箱","fieldType":"EMAIL","required":false,"sampleValue":"contact@test.com","order":4},{"fieldName":"address","fieldTitle":"地址","fieldType":"STRING","required":false,"maxLength":200,"sampleValue":"北京市朝阳区xxx","order":5},{"fieldName":"customerType","fieldTitle":"客户类型","fieldType":"ENUM","required":true,"dropdownOptions":{"1":"企业客户","2":"个人客户","3":"VIP客户"},"defaultValue":"1","order":6},{"fieldName":"remark","fieldTitle":"备注","fieldType":"STRING","required":false,"maxLength":500,"order":7}],"sampleRowCount":3,"maxImportRows":5000,"strictValidation":true}',
 '批量导入客户数据', '1.0.0', TRUE, 'import'),
('产品导入模板', 'tpl_product_import', 'product',
 '{"fields":[{"fieldName":"productCode","fieldTitle":"产品编码","fieldType":"STRING","required":true,"maxLength":50,"unique":true,"sampleValue":"PROD-001","order":1},{"fieldName":"productName","fieldTitle":"产品名称","fieldType":"STRING","required":true,"maxLength":100,"sampleValue":"智能手表","order":2},{"fieldName":"category","fieldTitle":"分类","fieldType":"ENUM","required":true,"dropdownOptions":{"electronics":"电子产品","clothing":"服装","food":"食品","other":"其他"},"order":3},{"fieldName":"price","fieldTitle":"价格","fieldType":"DECIMAL","required":true,"minValue":0.0,"maxValue":999999.99,"sampleValue":"299.00","order":4},{"fieldName":"stock","fieldTitle":"库存","fieldType":"INTEGER","required":true,"minValue":0.0,"sampleValue":"1000","order":5},{"fieldName":"status","fieldTitle":"状态","fieldType":"ENUM","required":true,"dropdownOptions":{"1":"上架","0":"下架"},"defaultValue":"1","order":6},{"fieldName":"description","fieldTitle":"描述","fieldType":"STRING","required":false,"maxLength":1000,"order":7}],"sampleRowCount":3,"maxImportRows":20000,"strictValidation":true}',
 '批量导入产品数据', '1.0.0', TRUE, 'import'),
('订单导入模板', 'tpl_order_import', 'order',
 '{"fields":[{"fieldName":"orderNo","fieldTitle":"订单号","fieldType":"STRING","required":true,"maxLength":50,"unique":true,"sampleValue":"ORD-20260404-001","order":1},{"fieldName":"customerName","fieldTitle":"客户名称","fieldType":"STRING","required":true,"maxLength":100,"order":2},{"fieldName":"productCode","fieldTitle":"产品编码","fieldType":"REFERENCE","required":true,"referenceType":"product","order":3},{"fieldName":"quantity","fieldTitle":"数量","fieldType":"INTEGER","required":true,"minValue":1.0,"maxValue":9999.0,"sampleValue":"10","order":4},{"fieldName":"unitPrice","fieldTitle":"单价","fieldType":"DECIMAL","required":true,"minValue":0.01,"sampleValue":"299.00","order":5},{"fieldName":"orderDate","fieldTitle":"订单日期","fieldType":"DATE","required":true,"sampleValue":"2026-04-04","order":6},{"fieldName":"remark","fieldTitle":"备注","fieldType":"STRING","required":false,"maxLength":500,"order":7}],"sampleRowCount":3,"maxImportRows":50000,"strictValidation":true}',
 '批量导入订单数据', '1.0.0', TRUE, 'import')
ON CONFLICT (template_kind, code) WHERE code IS NOT NULL DO NOTHING;
