# 重复表候选：结构与数据比对

> 由 `tools/audit-duplicate-tables-diff.py` 生成；数据来源 devdb.public。
> 「存同类数据」按业务键（name/code/key 等，采样上限 500）比对；
> 刻意**不用 id**——自增 id 必然重叠，是伪证据。

## 表名同类: config

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `dms_config` | 97 | 12 | DMS 租户配置（含地图服务 Key 等；全局默 | 1 | 0 | 2 | - |
| `sys_config` | 173 | 22 | 系统参数配置表 | 1 | 0 | 1 | - |

- `dms_config` vs `sys_config`：存同类数据 = 否（按 `跨列合并` 比对，交集 0 条）
  - 共有业务列 0 个，`dms_config` 独有：config_desc, config_key, config_value, scope, version
  - `sys_config` 独有：builtin, config_group, config_type, enabled, help_text, locked, nav_group, param_key

## 表名同类: customer_grade_price

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `biz_customer_grade_price` | 0 | 23 |  | 1 | 0 | 0 | - |
| `erp_customer_grade_price` | 18 | 22 | 级别指定价设置（商品价格管理-子标签3） | 1 | 0 | 1 | - |

- `biz_customer_grade_price` vs `erp_customer_grade_price`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`biz_customer_grade_price` 独有：discount_rate, effective_time, expire_time, grade_price, is_default, max_price, min_price, price_type
  - `erp_customer_grade_price` 独有：base_price_type, calc_operator, calc_value, category_id, category_name, grade_name, price_rule, product_code

## 表名同类: event_outbox

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `dms_event_outbox` | 295 | 16 |  | 1 | 0 | 1 | - |
| `wms_event_outbox` | 0 | 19 | 事件发件箱（WMS→ERP 异步通知） | 1 | 0 | 0 | - |

- `dms_event_outbox` vs `wms_event_outbox`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 7 个，`dms_event_outbox` 独有：source, target
  - `wms_event_outbox` 独有：completed_time, max_retry, source_system, target_system, tenant_id

## 表名同类: expense_approval

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_expense_approval` | 79 | 18 | 费用审批记录：单号/级别/审批人/动作/意见/时 | 1 | 0 | 0 | - |
| `expense_approval` | 0 | 20 |  | 1 | 0 | 1 | - |

- `erp_expense_approval` vs `expense_approval`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 8 个，`erp_expense_approval` 独有：create_by, create_time, doc_no, expense_doc_id, update_by, update_time, version_no
  - `expense_approval` 独有：application_id, approver_department_id, approver_department_name, created_at, created_by, remark, updated_at, updated_by

## 表名同类: expense_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_expense_item` | 40 | 16 | 费用项明细（费用编号/费用名称/费用科目/金额/ | 1 | 0 | 1 | - |
| `expense_item` | 0 | 39 |  | 1 | 0 | 1 | - |

- `erp_expense_item` vs `expense_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_expense_item` 独有：create_by, create_time, expense_code, expense_doc_id, expense_name, line_no, subject_code, subject_name
  - `expense_item` 独有：account_code, attachment_id, budget_code, cost_center, created_at, created_by, description, expense_application_id

## 表名同类: payment

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `dms_payment` | 1 | 33 |  | 1 | 0 | 0 | - |
| `erp_payment` | 0 | 59 |  | 1 | 0 | 1 | - |

- `dms_payment` vs `erp_payment`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`dms_payment` 独有：amount, audit_by, audit_remark, audit_status, audit_time, callback_time, external_order_no, finance_push_status
  - `erp_payment` 独有：approved_by, approved_note, approved_time, bank_account, bank_name, check_no, completed_by, completed_time

## 表名同类: permission

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `permissions` | 0 | 16 |  | 0 | 0 | 0 | role_permissions |
| `sys_permission` | 454 | 20 |  | 1 | 3 | 1 | - |

- `permissions` vs `sys_permission`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`permissions` 独有：action, code, created_at, created_by, description, enabled, level, name
  - `sys_permission` 独有：api_path, component, create_by, create_time, deleted, icon, method, path

## 表名同类: print_task

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_print_task` | 0 | 26 |  | 1 | 0 | 1 | - |
| `sys_print_task` | 0 | 28 |  | 1 | 0 | 1 | - |

- `erp_print_task` vs `sys_print_task`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 13 个，`erp_print_task` 独有：client_ip, copies, create_by, create_time, id, operator_name, print_data, print_duration
  - `sys_print_task` 独有：chain_id, chain_item_id, client_id, created_at, created_by, data_json, page_code, printer_name

## 表名同类: print_template

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_print_template` | 0 | 21 |  | 1 | 0 | 0 | - |
| `sys_print_template` | 0 | 20 |  | 1 | 0 | 0 | - |

- `erp_print_template` vs `sys_print_template`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 3 个，`erp_print_template` 独有：content, copies, create_by, create_time, file_path, file_type, height, id
  - `sys_print_template` 独有：created_at, created_by, is_default, margin_bottom, margin_left, margin_right, margin_top, page_code

## 表名同类: purchase_order_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_purchase_order_item` | 12 | 74 | 采购订单明细(生产级完整字段版) | 1 | 2 | 0 | - |
| `purchase_order_item` | 0 | 20 |  | 0 | 0 | 0 | - |

- `erp_purchase_order_item` vs `purchase_order_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 11 个，`erp_purchase_order_item` 独有：amount_with_tax, area, available_stock, available_stock_converted, barcode, batch_code, big_pack, book_stock
  - `purchase_order_item` 独有：created_at, delivery_location, fulfillment_percent, item_note, lead_time, material_name, origin_country, quality_level

## 表名同类: role

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `roles` | 0 | 13 |  | 0 | 0 | 0 | role_permissions,user_roles |
| `sys_role` | 5 | 16 |  | 1 | 3 | 1 | - |

- `roles` vs `sys_role`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`roles` 独有：code, created_at, created_by, description, enabled, is_default, level, name
  - `sys_role` 独有：create_by, create_time, data_scope, deleted, parent_id, remark, role_code, role_name

## 表名同类: role_permission

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `role_permissions` | 0 | 2 |  | 0 | 0 | 0 | - |
| `sys_role_permission` | 490 | 6 |  | 1 | 3 | 0 | - |

- `role_permissions` vs `sys_role_permission`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`role_permissions` 独有：（无）
  - `sys_role_permission` 独有：create_by, create_time, id, tenant_id

## 表名同类: scheduled_task

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `scheduled_task` | 16 | 23 | 定时任务表 | 1 | 0 | 1 | - |
| `sys_scheduled_task` | 0 | 18 |  | 0 | 0 | 0 | - |

- `scheduled_task` vs `sys_scheduled_task`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 7 个，`scheduled_task` 独有：enabled, execute_params, job_key, retry_count, retry_interval, success_count, task_desc, task_type
  - `sys_scheduled_task` 独有：description, last_execute_result, task_class, task_group, task_params

## 表名同类: supplier

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_supplier` | 0 | 44 |  | 1 | 0 | 2 | - |
| `supplier` | 0 | 13 |  | 1 | 0 | 3 | - |

- `erp_supplier` vs `supplier`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`erp_supplier` 独有：attachment_info, bank_account, bank_name, business_license, business_scope, category_tags, certification_status, company_address
  - `supplier` 独有：address, category, code, contact_person_id, created_at, email, emergency_phone, name

## 表名同类: user

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `sys_user` | 75 | 29 |  | 1 | 3 | 10 | - |
| `users` | 0 | 21 |  | 0 | 0 | 0 | user_roles |

- `sys_user` vs `users`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 6 个，`sys_user` 独有：avatar, create_by, create_time, data_scope, deleted, dept_id, ext_info, gender
  - `users` 独有：avatar_url, created_at, created_by, department, email_verified, enabled, failed_login_attempts, full_name

## 表名同类: user_role

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `sys_user_role` | 46 | 6 |  | 1 | 7 | 2 | - |
| `user_roles` | 0 | 2 |  | 0 | 0 | 0 | - |

- `sys_user_role` vs `user_roles`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`sys_user_role` 独有：create_by, create_time, id, tenant_id
  - `user_roles` 独有：（无）

## 表名同类: warehouse

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_warehouse` | 105 | 19 |  | 1 | 1 | 3 | - |
| `wms_warehouse` | 3 | 18 | 仓库扩展信息（WMS） | 1 | 0 | 0 | - |

- `erp_warehouse` vs `wms_warehouse`：存同类数据 = **是**（按 `warehouse_name` 比对，交集 3 条）
  - 共有业务列 2 个，`erp_warehouse` 独有：address, category_id, contact_person, contact_phone, easy_code, parent_id, sort_order, status
  - `wms_warehouse` 独有：is_wms_enabled, location_count, total_capacity, used_capacity, version, warehouse_id, warehouse_type, zone_count

## 结构相似: biz_party_category

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `biz_party_category` | 20 | 15 |  | 1 | 1 | 1 | - |
| `erp_partner_category` | 0 | 16 |  | 0 | 0 | 0 | - |
| `erp_product_category` | 2 | 16 |  | 1 | 0 | 11 | - |
| `erp_warehouse_category` | 32 | 14 | 仓库分类树（资料 → 仓库管理 → 仓库规划 左 | 1 | 0 | 0 | - |
| `mall_category` | 0 | 12 | 商城商品分类 | 0 | 0 | 0 | - |

- `biz_party_category` vs `erp_partner_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`biz_party_category` 独有：level, party_type
  - `erp_partner_category` 独有：category_level, category_type, description
- `biz_party_category` vs `erp_product_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`biz_party_category` 独有：level, party_type
  - `erp_product_category` 独有：category_level, description, icon
- `biz_party_category` vs `erp_warehouse_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`biz_party_category` 独有：level, party_type
  - `erp_warehouse_category` 独有：category_level
- `biz_party_category` vs `mall_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`biz_party_category` 独有：category_code, level, party_type, remark
  - `mall_category` 独有：icon_url
- `erp_partner_category` vs `erp_product_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 7 个，`erp_partner_category` 独有：category_type
  - `erp_product_category` 独有：icon
- `erp_partner_category` vs `erp_warehouse_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 6 个，`erp_partner_category` 独有：category_type, description
  - `erp_warehouse_category` 独有：（无）
- `erp_partner_category` vs `mall_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`erp_partner_category` 独有：category_code, category_level, category_type, description, remark
  - `mall_category` 独有：icon_url
- `erp_product_category` vs `erp_warehouse_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 6 个，`erp_product_category` 独有：description, icon
  - `erp_warehouse_category` 独有：（无）
- `erp_product_category` vs `mall_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`erp_product_category` 独有：category_code, category_level, description, icon, remark
  - `mall_category` 独有：icon_url
- `erp_warehouse_category` vs `mall_category`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`erp_warehouse_category` 独有：category_code, category_level, remark
  - `mall_category` 独有：icon_url

## 结构相似: dms_return_receive

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `dms_logistics_ship` | 0 | 15 |  | 0 | 0 | 0 | - |
| `dms_return_receive` | 0 | 15 |  | 0 | 0 | 0 | - |
| `dms_ship_order` | 0 | 16 |  | 0 | 0 | 0 | - |

- `dms_logistics_ship` vs `dms_return_receive`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`dms_logistics_ship` 独有：logistics_company, logistics_no, receiver_name, ship_no
  - `dms_return_receive` 独有：customer_id, customer_name, reason, return_no
- `dms_logistics_ship` vs `dms_ship_order`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`dms_logistics_ship` 独有：receiver_name
  - `dms_ship_order` 独有：customer_id, customer_name
- `dms_return_receive` vs `dms_ship_order`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`dms_return_receive` 独有：reason, return_no
  - `dms_ship_order` 独有：logistics_company, logistics_no, ship_no

## 结构相似: erp_ar_ap_adjust_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_ar_ap_adjust_item` | 0 | 14 | 应收应付调整-科目明细（实现对转科目，金额可多行 | 1 | 0 | 0 | - |
| `erp_expense_item` | 40 | 16 | 费用项明细（费用编号/费用名称/费用科目/金额/ | 1 | 0 | 1 | - |

- `erp_ar_ap_adjust_item` vs `erp_expense_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`erp_ar_ap_adjust_item` 独有：adjust_id
  - `erp_expense_item` 独有：expense_code, expense_doc_id, expense_name

## 结构相似: erp_kit_assembly

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_kit_assembly` | 0 | 31 |  | 0 | 0 | 0 | - |
| `erp_kit_disassembly` | 0 | 32 |  | 0 | 0 | 0 | - |

- `erp_kit_assembly` vs `erp_kit_disassembly`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 20 个，`erp_kit_assembly` 独有：assembly_date, assembly_no, assembly_quantity
  - `erp_kit_disassembly` 独有：batch_no, disassembly_date, disassembly_no, disassembly_quantity

## 结构相似: erp_kit_assembly_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_kit_assembly_item` | 0 | 22 |  | 0 | 0 | 0 | - |
| `erp_kit_disassembly_item` | 0 | 22 |  | 0 | 0 | 0 | - |

- `erp_kit_assembly_item` vs `erp_kit_disassembly_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 12 个，`erp_kit_assembly_item` 独有：assembly_id, required_quantity
  - `erp_kit_disassembly_item` 独有：disassembly_id, expected_quantity

## 结构相似: erp_mall_tag

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_mall_tag` | 39 | 11 | 商城标签（用户自定义） | 1 | 0 | 1 | - |
| `erp_product_recommend` | 0 | 10 | 推荐商品关联表 | 1 | 0 | 0 | - |
| `erp_product_unit_group` | 22 | 8 | 商品单位组（商品多单位换算模板：小/中/大单位及 | 1 | 0 | 1 | - |
| `mkt_sms_template` | 19 | 11 | 短信模板（营销→营销活动→发短信→短信模板管理  | 1 | 0 | 0 | - |
| `shop_decoration_product` | 0 | 9 | 商品详情装修-应用商品关联（⚠️ 表结构为**本 | 1 | 0 | 0 | - |
| `sys_user_tenant` | 45 | 9 |  | 1 | 0 | 3 | - |

- `erp_mall_tag` vs `erp_product_recommend`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_mall_tag` 独有：status, tag_code, tag_name
  - `erp_product_recommend` 独有：product_id, recommend_product_id
- `erp_mall_tag` vs `erp_product_unit_group`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_mall_tag` 独有：sort_order, tag_code, tag_name
  - `erp_product_unit_group` 独有：（无）
- `erp_mall_tag` vs `mkt_sms_template`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_mall_tag` 独有：sort_order, tag_code, tag_name
  - `mkt_sms_template` 独有：sms_type, template_content, template_title
- `erp_mall_tag` vs `shop_decoration_product`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`erp_mall_tag` 独有：sort_order, status, tag_code, tag_name
  - `shop_decoration_product` 独有：decoration_id, product_id
- `erp_mall_tag` vs `sys_user_tenant`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_mall_tag` 独有：deleted, sort_order, tag_code, tag_name
  - `sys_user_tenant` 独有：is_default, user_id
- `erp_product_recommend` vs `erp_product_unit_group`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`erp_product_recommend` 独有：product_id, recommend_product_id, sort_order
  - `erp_product_unit_group` 独有：status
- `erp_product_recommend` vs `mkt_sms_template`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`erp_product_recommend` 独有：product_id, recommend_product_id, sort_order
  - `mkt_sms_template` 独有：sms_type, status, template_content, template_title
- `erp_product_recommend` vs `shop_decoration_product`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_product_recommend` 独有：recommend_product_id, sort_order
  - `shop_decoration_product` 独有：decoration_id
- `erp_product_recommend` vs `sys_user_tenant`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`erp_product_recommend` 独有：deleted, product_id, recommend_product_id, sort_order
  - `sys_user_tenant` 独有：is_default, status, user_id
- `erp_product_unit_group` vs `mkt_sms_template`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_product_unit_group` 独有：（无）
  - `mkt_sms_template` 独有：sms_type, template_content, template_title
- `erp_product_unit_group` vs `shop_decoration_product`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`erp_product_unit_group` 独有：status
  - `shop_decoration_product` 独有：decoration_id, product_id
- `erp_product_unit_group` vs `sys_user_tenant`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`erp_product_unit_group` 独有：deleted
  - `sys_user_tenant` 独有：is_default, user_id
- `mkt_sms_template` vs `shop_decoration_product`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`mkt_sms_template` 独有：sms_type, status, template_content, template_title
  - `shop_decoration_product` 独有：decoration_id, product_id
- `mkt_sms_template` vs `sys_user_tenant`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`mkt_sms_template` 独有：deleted, sms_type, template_content, template_title
  - `sys_user_tenant` 独有：is_default, user_id
- `shop_decoration_product` vs `sys_user_tenant`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 0 个，`shop_decoration_product` 独有：decoration_id, deleted, product_id
  - `sys_user_tenant` 独有：is_default, status, user_id

## 结构相似: erp_partner_attachment

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_partner_attachment` | 11 | 14 | 往来单位附件表 | 1 | 0 | 1 | - |
| `erp_product_attachment` | 0 | 14 | 产品附件表 | 1 | 0 | 0 | - |

- `erp_partner_attachment` vs `erp_product_attachment`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 6 个，`erp_partner_attachment` 独有：partner_id
  - `erp_product_attachment` 独有：product_id

## 结构相似: erp_partner_grade

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_partner_grade` | 0 | 15 |  | 0 | 0 | 1 | - |
| `erp_product_grade` | 8 | 14 |  | 1 | 0 | 3 | - |

- `erp_partner_grade` vs `erp_product_grade`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 6 个，`erp_partner_grade` 独有：grade_type
  - `erp_product_grade` 独有：（无）

## 结构相似: erp_payment_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_payment_item` | 0 | 40 |  | 1 | 0 | 1 | - |
| `erp_receipt_item` | 0 | 41 |  | 1 | 0 | 1 | - |

- `erp_payment_item` vs `erp_receipt_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 28 个，`erp_payment_item` 独有：payable_id, payment_id, payment_term, purchase_amount
  - `erp_receipt_item` 独有：receipt_id, receivable_id, receiver_address, receiver_name, receiver_phone

## 结构相似: erp_pre_payment

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_pre_payment` | 0 | 43 |  | 1 | 0 | 1 | - |
| `erp_pre_receipt` | 2 | 44 |  | 1 | 0 | 1 | - |

- `erp_pre_payment` vs `erp_pre_receipt`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 29 个，`erp_pre_payment` 独有：attachment, payment_date, pre_payment_amount, pre_payment_no, supplier_id, supplier_name
  - `erp_pre_receipt` 独有：customer_id, customer_name, gift_amount, pre_receipt_amount, pre_receipt_no, receipt_date, total_amount

## 结构相似: erp_pre_payment_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_pre_payment_item` | 0 | 14 | 预付款单-付款账户明细（一单多账户） | 1 | 0 | 0 | - |
| `erp_pre_receipt_item` | 0 | 14 | 预收款单-收款账户明细（一单多账户） | 1 | 0 | 0 | - |

- `erp_pre_payment_item` vs `erp_pre_receipt_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`erp_pre_payment_item` 独有：pre_payment_id
  - `erp_pre_receipt_item` 独有：pre_receipt_id

## 结构相似: erp_printer_group

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_printer_group` | 0 | 12 |  | 1 | 0 | 0 | - |
| `sys_custom_field_group` | 0 | 13 |  | 1 | 0 | 1 | - |

- `erp_printer_group` vs `sys_custom_field_group`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 4 个，`erp_printer_group` 独有：status
  - `sys_custom_field_group` 独有：active, model_name

## 结构相似: erp_product_attribute_def

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_product_attribute_def` | 0 | 12 | 产品辅助属性定义表 | 1 | 0 | 1 | - |
| `erp_product_brand` | 39 | 12 | 商品品牌管理表 | 1 | 0 | 1 | - |

- `erp_product_attribute_def` vs `erp_product_brand`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`erp_product_attribute_def` 独有：attr_name, attr_type
  - `erp_product_brand` 独有：brand_name, mnemonic_code

## 结构相似: erp_purchase_exchange

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_purchase_exchange` | 0 | 72 | 采购换货表 | 1 | 0 | 0 | - |
| `erp_sale_exchange` | 9 | 81 |  | 1 | 0 | 2 | - |

- `erp_purchase_exchange` vs `erp_sale_exchange`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 56 个，`erp_purchase_exchange` 独有：paid_amount, payment_deadline, prepaid_balance, prev_prepaid, supplier_code, supplier_id, supplier_name, supplier_remark
  - `erp_sale_exchange` 独有：advance_balance, available_advance, available_credit, collection_deadline, create_by, creator_id, creator_name, credit_limit

## 结构相似: erp_purchase_exchange_approval_record

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_purchase_exchange_approval_record` | 0 | 10 |  | 1 | 0 | 0 | - |
| `erp_sale_exchange_approval_record` | 0 | 11 |  | 1 | 0 | 0 | - |

- `erp_purchase_exchange_approval_record` vs `erp_sale_exchange_approval_record`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 5 个，`erp_purchase_exchange_approval_record` 独有：（无）
  - `erp_sale_exchange_approval_record` 独有：tenant_id

## 结构相似: erp_purchase_exchange_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_purchase_exchange_item` | 0 | 87 |  | 1 | 0 | 0 | - |
| `erp_sale_exchange_item` | 9 | 90 |  | 1 | 0 | 1 | - |

- `erp_purchase_exchange_item` vs `erp_sale_exchange_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 70 个，`erp_purchase_exchange_item` 独有：price_level_1, price_level_2, price_level_3, price_level_4, price_level_5, price_level_6, price_level_7, price_level_8
  - `erp_sale_exchange_item` 独有：min_sale_price, price_level1, price_level2, price_level3, price_level4, price_level5, price_level6, price_level7

## 结构相似: erp_purchase_price_track

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_purchase_price_track` | 8 | 24 |  | 1 | 0 | 0 | - |
| `erp_sale_price_track` | 15 | 24 |  | 1 | 0 | 0 | - |

- `erp_purchase_price_track` vs `erp_sale_price_track`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 13 个，`erp_purchase_price_track` 独有：item_code, purchase_date, purchase_price
  - `erp_sale_price_track` 独有：discount_rate, sale_date, sale_price

## 结构相似: erp_sale_return

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_sale_return` | 25 | 133 |  | 1 | 0 | 3 | - |
| `erp_sale_return_doc` | 1 | 132 | 销售退货单 | 1 | 0 | 3 | - |

- `erp_sale_return` vs `erp_sale_return_doc`：存同类数据 = **是**（按 `warehouse_name` 比对，交集 1 条）
  - 共有业务列 111 个，`erp_sale_return` 独有：applicant_id, applicant_name, apply_time, delivery_order_id, expected_receive_date, logistics_no, ordered_quantity, received_quantity
  - `erp_sale_return_doc` 独有：advance_balance, available_advance, bookkeeper_name, creator, prev_advance, product_line_count, receivable_reduce, return_advance

## 结构相似: erp_sale_return_doc_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_sale_return_doc_item` | 1 | 86 | 销售退货单明细 | 1 | 0 | 3 | - |
| `erp_sale_return_item` | 26 | 80 |  | 1 | 0 | 1 | - |

- `erp_sale_return_doc_item` vs `erp_sale_return_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 69 个，`erp_sale_return_doc_item` 独有：batch_barcode, expiry_date, generated_points, production_date, return_doc_id, shelf_life, sort, storage_location
  - `erp_sale_return_item` 独有：reason, return_id, tax_rate

## 结构相似: erp_stock_assemble

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_stock_assemble` | 0 | 54 | 库存组装表 | 1 | 0 | 0 | - |
| `erp_stock_split` | 0 | 52 |  | 1 | 0 | 0 | - |

- `erp_stock_assemble` vs `erp_stock_split`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 40 个，`erp_stock_assemble` 独有：assemble_date, assemble_fee, assemble_no, assemble_quantity, output_quantity, produce_unit
  - `erp_stock_split` 独有：output_total_cost, split_date, split_no, split_quantity

## 结构相似: erp_stock_assemble_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_stock_assemble_item` | 1 | 50 |  | 1 | 0 | 0 | - |
| `erp_stock_damage_item` | 1 | 43 |  | 1 | 0 | 1 | - |
| `erp_stock_overflow_item` | 1 | 43 |  | 1 | 0 | 1 | - |
| `erp_stock_split_item` | 0 | 51 |  | 1 | 0 | 0 | - |

- `erp_stock_assemble_item` vs `erp_stock_damage_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 34 个，`erp_stock_assemble_item` 独有：assemble_id, available_stock, available_stock_converted, cost, create_by, expiry_date, retail_price, unit_price
  - `erp_stock_damage_item` 独有：amount, damage_id, validity_date
- `erp_stock_assemble_item` vs `erp_stock_overflow_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 34 个，`erp_stock_assemble_item` 独有：assemble_id, available_stock, available_stock_converted, cost, create_by, expiry_date, retail_price, unit_price
  - `erp_stock_overflow_item` 独有：amount, overflow_id, validity_date
- `erp_stock_assemble_item` vs `erp_stock_split_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 41 个，`erp_stock_assemble_item` 独有：assemble_id
  - `erp_stock_split_item` 独有：allocation_ratio, split_id
- `erp_stock_damage_item` vs `erp_stock_overflow_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 36 个，`erp_stock_damage_item` 独有：damage_id
  - `erp_stock_overflow_item` 独有：overflow_id
- `erp_stock_damage_item` vs `erp_stock_split_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 34 个，`erp_stock_damage_item` 独有：amount, damage_id, validity_date
  - `erp_stock_split_item` 独有：allocation_ratio, available_stock, available_stock_converted, cost, create_by, expiry_date, retail_price, split_id
- `erp_stock_overflow_item` vs `erp_stock_split_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 34 个，`erp_stock_overflow_item` 独有：amount, overflow_id, validity_date
  - `erp_stock_split_item` 独有：allocation_ratio, available_stock, available_stock_converted, cost, create_by, expiry_date, retail_price, split_id

## 结构相似: erp_stock_check_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_stock_check_item` | 0 | 32 |  | 1 | 0 | 1 | - |
| `wms_check_result` | 0 | 26 | 盘点结果 | 1 | 0 | 0 | - |

- `erp_stock_check_item` vs `wms_check_result`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 16 个，`erp_stock_check_item` 独有：actual_amount, book_amount, check_id, check_note, checked_by, checked_time, note, status
  - `wms_check_result` 独有：task_id, version

## 结构相似: erp_stock_damage

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_stock_damage` | 1 | 41 | 库存报损表 | 1 | 0 | 0 | - |
| `erp_stock_in` | 1 | 42 |  | 1 | 0 | 0 | - |
| `erp_stock_out` | 0 | 42 |  | 1 | 0 | 0 | - |
| `erp_stock_overflow` | 1 | 41 | 库存报溢表 | 1 | 0 | 0 | - |

- `erp_stock_damage` vs `erp_stock_in`：存同类数据 = 否（按 `warehouse_name` 比对，交集 0 条）
  - 共有业务列 28 个，`erp_stock_damage` 独有：damage_cause, damage_date, damage_no, location_code, location_id
  - `erp_stock_in` 独有：partner_code, partner_id, partner_name, stock_in_date, stock_in_no, stock_in_type
- `erp_stock_damage` vs `erp_stock_out`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 28 个，`erp_stock_damage` 独有：damage_cause, damage_date, damage_no, location_code, location_id
  - `erp_stock_out` 独有：partner_code, partner_id, partner_name, stock_out_date, stock_out_no, stock_out_type
- `erp_stock_damage` vs `erp_stock_overflow`：存同类数据 = 否（按 `warehouse_name` 比对，交集 0 条）
  - 共有业务列 30 个，`erp_stock_damage` 独有：damage_cause, damage_date, damage_no
  - `erp_stock_overflow` 独有：overflow_date, overflow_no, source_type
- `erp_stock_in` vs `erp_stock_out`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 31 个，`erp_stock_in` 独有：stock_in_date, stock_in_no, stock_in_type
  - `erp_stock_out` 独有：stock_out_date, stock_out_no, stock_out_type
- `erp_stock_in` vs `erp_stock_overflow`：存同类数据 = 否（按 `warehouse_name` 比对，交集 0 条）
  - 共有业务列 28 个，`erp_stock_in` 独有：partner_code, partner_id, partner_name, stock_in_date, stock_in_no, stock_in_type
  - `erp_stock_overflow` 独有：location_code, location_id, overflow_date, overflow_no, source_type
- `erp_stock_out` vs `erp_stock_overflow`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 28 个，`erp_stock_out` 独有：partner_code, partner_id, partner_name, stock_out_date, stock_out_no, stock_out_type
  - `erp_stock_overflow` 独有：location_code, location_id, overflow_date, overflow_no, source_type

## 结构相似: erp_stock_in_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `erp_stock_in_item` | 1 | 51 |  | 1 | 0 | 0 | - |
| `erp_stock_out_item` | 0 | 51 |  | 1 | 0 | 0 | - |
| `wms_borrow_order_item` | 2 | 63 | 借进借出单明细 | 1 | 0 | 1 | - |

- `erp_stock_in_item` vs `erp_stock_out_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 44 个，`erp_stock_in_item` 独有：stock_in_id
  - `erp_stock_out_item` 独有：stock_out_id
- `erp_stock_in_item` vs `wms_borrow_order_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 41 个，`erp_stock_in_item` 独有：brand, product_unit, stock_in_id, unit_price
  - `wms_borrow_order_item` 独有：book_stock, create_by, line_no, min_price, non_processed_amount, non_processed_quantity, order_id, price
- `erp_stock_out_item` vs `wms_borrow_order_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 41 个，`erp_stock_out_item` 独有：brand, product_unit, stock_out_id, unit_price
  - `wms_borrow_order_item` 独有：book_stock, create_by, line_no, min_price, non_processed_amount, non_processed_quantity, order_id, price

## 结构相似: finance_payable

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `finance_payable` | 3 | 23 |  | 1 | 0 | 1 | - |
| `finance_receivable` | 6 | 20 |  | 1 | 0 | 2 | - |

- `finance_payable` vs `finance_receivable`：存同类数据 = 否（按 `跨列合并` 比对，交集 0 条）
  - 共有业务列 13 个，`finance_payable` 独有：reconcile_at, reconcile_by_name, reconcile_flag, supplier_id, supplier_name
  - `finance_receivable` 独有：customer_id, customer_name

## 结构相似: invoice_application_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `invoice_application_item` | 0 | 27 |  | 1 | 0 | 0 | - |
| `invoice_item` | 0 | 27 |  | 1 | 0 | 0 | - |

- `invoice_application_item` vs `invoice_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 20 个，`invoice_application_item` 独有：application_id
  - `invoice_item` 独有：invoice_id

## 结构相似: mall_keyword

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `mall_keyword` | 35 | 12 | 商城搜索关键词库 | 1 | 0 | 0 | - |
| `mkt_points_exchange_product` | 19 | 12 | 可兑换商品目录（营销→会员中心→积分兑换） | 1 | 0 | 1 | - |

- `mall_keyword` vs `mkt_points_exchange_product`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 2 个，`mall_keyword` 独有：keyword, keyword_type
  - `mkt_points_exchange_product` 独有：exchange_points, product_id

## 结构相似: payment_record

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `payment_record` | 0 | 18 | 支付记录表(第三方返回) | 1 | 0 | 0 | - |
| `refund_record` | 0 | 17 | 退款记录表 | 1 | 0 | 0 | - |

- `payment_record` vs `refund_record`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 8 个，`payment_record` 独有：channel_order_no, channel_trade_no
  - `refund_record` 独有：channel_refund_no

## 结构相似: permissions

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `permissions` | 0 | 16 |  | 0 | 0 | 0 | role_permissions |
| `roles` | 0 | 13 |  | 0 | 0 | 0 | role_permissions,user_roles |

- `permissions` vs `roles`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 9 个，`permissions` 独有：action, parent_id, resource, visible
  - `roles` 独有：is_default

## 结构相似: purchase_order_item

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `purchase_order_item` | 0 | 20 |  | 0 | 0 | 0 | - |
| `purchase_quote_item` | 0 | 19 |  | 1 | 0 | 1 | - |

- `purchase_order_item` vs `purchase_quote_item`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 15 个，`purchase_order_item` 独有：fulfillment_percent, order_id, received_quantity
  - `purchase_quote_item` 独有：inquiry_item_id, quote_id

## 结构相似: sys_department

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `sys_department` | 2 | 19 |  | 2 | 1 | 1 | - |
| `sys_dept` | 0 | 15 |  | 1 | 1 | 4 | - |

- `sys_department` vs `sys_dept`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 8 个，`sys_department` 独有：ancestors, create_by, leader_name, update_by, version
  - `sys_dept` 独有：leader

## 结构相似: sys_role_menu

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `sys_role_menu` | 3 | 6 |  | 1 | 4 | 1 | - |
| `sys_tenant_menu` | 4 | 5 | 租户菜单授权表(系统管理员授权给租户的菜单) | 1 | 1 | 0 | - |

- `sys_role_menu` vs `sys_tenant_menu`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`sys_role_menu` 独有：role_id
  - `sys_tenant_menu` 独有：（无）

## 结构相似: sys_role_permission

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `sys_role_permission` | 490 | 6 |  | 1 | 3 | 0 | - |
| `sys_user_role` | 46 | 6 |  | 1 | 7 | 2 | - |

- `sys_role_permission` vs `sys_user_role`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 1 个，`sys_role_permission` 独有：permission_id
  - `sys_user_role` 独有：user_id

## 结构相似: wms_move_detail

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `wms_move_detail` | 0 | 24 | 移库明细 | 1 | 0 | 0 | - |
| `wms_putaway_detail` | 0 | 25 | 上架明细 | 1 | 0 | 0 | - |

- `wms_move_detail` vs `wms_putaway_detail`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 14 个，`wms_move_detail` 独有：serial_no
  - `wms_putaway_detail` 独有：production_date, validity_date

## 结构相似: wms_pick_detail

| 表 | 行数 | 列数 | 注释 | 实体 | mapper | 查询SQL | 被外键引用 |
|---|---|---|---|---|---|---|---|
| `wms_pick_detail` | 0 | 24 | 拣货明细 | 1 | 0 | 0 | - |
| `wms_receipt_detail` | 0 | 26 | 收货明细 | 1 | 0 | 0 | - |
| `wms_ship_detail` | 0 | 24 | 发货复核明细 | 1 | 0 | 0 | - |

- `wms_pick_detail` vs `wms_receipt_detail`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 12 个，`wms_pick_detail` 独有：picked_quantity, serial_no, shortage_quantity
  - `wms_receipt_detail` 独有：broken_quantity, production_date, putaway_quantity, received_quantity, validity_date
- `wms_pick_detail` vs `wms_ship_detail`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 12 个，`wms_pick_detail` 独有：picked_quantity, shortage_quantity, task_id
  - `wms_ship_detail` 独有：confirmed_quantity, scanned_quantity, ship_id
- `wms_receipt_detail` vs `wms_ship_detail`：存同类数据 = 无法判定（有一方为空 / 无共有的业务键列）
  - 共有业务列 11 个，`wms_receipt_detail` 独有：broken_quantity, production_date, putaway_quantity, received_quantity, task_id, validity_date
  - `wms_ship_detail` 独有：confirmed_quantity, scanned_quantity, serial_no, ship_id
