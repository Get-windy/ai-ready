-- 营销模块 → 营销活动 → 优惠券（80311）：「券模板制券」链路修复
--
-- 背景：`erp_loyalty_coupon.program_id` 建表即 NOT NULL，该列属早期 Odoo 风格「忠诚程序（program）」模型。
--       2026-09-18 起优惠券改由「券模板（mkt_coupon_template）」定义、「发优惠券」按模板批量发券，
--       券实例不再必然归属某个 program → 不放开约束时 INSERT 直接报
--       「null value in column "program_id" ... violates not-null constraint」（实测，发放整条链路 400）。
-- 处置：放开 NOT NULL；program_id 保留可用（历史数据与既有 LoyaltyCouponController 仍可写）。
ALTER TABLE erp_loyalty_coupon ALTER COLUMN program_id DROP NOT NULL;
