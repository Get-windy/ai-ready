# db/migration/archive — 孤儿脚本归档

本目录脚本原为 `db/migration/` 根目录下的孤儿脚本：不在应用 Flyway locations
（`classpath:db/migration`，即 `backend/core/api/core-api/src/main/resources/db/migration/`）
之内，从未被 Flyway 执行。2026-07-22 逐文件审读并与 devdb 实际结构比对后归档，
各文件头部已加"已废弃"标注。

| 脚本 | 判定 | 证据 |
|------|------|------|
| V20260622100000__add_sale_order_item_extended_fields.sql | 已废弃-被 V11.17.0 覆盖 | devdb `erp_sale_order_item` 缺其 47 列中的 47 列（仅 restaurant 等 8 列已由 V9.52.0 建好）；缺失列已被实体 `SaleOrderItem` 映射，属真实差额，已由应用内迁移 V11.17.0 收编（vip_level_1/2 按库内实际命名 vip_level1/2 未重复建列）。原脚本为 MySQL 语法（COMMENT），无法在 PostgreSQL 执行。 |
| V20260630100000__create_hr_tables.sql | 已废弃-被 V8.9.0 覆盖 | devdb 已有 V8.9.0 所建 8 张 hr_* 表，列与 `HrPosition` 等实体一致（如 hr_position 用 quota_count/responsibility，非本脚本的 headcount/description）；hr_training/hr_training_participant 全后端无代码引用，未建；默认岗位示例数据未入库也不需要。 |
| V20260707100000__add_sale_order_full_fields.sql | 已废弃-被 V9.49.0 等覆盖 | devdb `erp_sale_order` 逐列比对，本脚本全部 ~100 列均已存在（由 V9.49.0__Add_Missing_Sale_Order_Columns.sql 等应用内迁移建好，如 fulfillment_rate、collection_location 见 V9.49.0:141/145）。原脚本同为 MySQL 语法。 |

注意：归档目录不在 Flyway locations 内，这些脚本不会也不应被执行。
