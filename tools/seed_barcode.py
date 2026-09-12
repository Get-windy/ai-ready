# -*- coding: utf-8 -*-
"""为商品条码页端到端验证准备最小测试数据（dev 环境，可重复执行）"""
import psycopg2

DSN = "host=localhost port=5432 dbname=devdb user=devuser password=devuser123"

STATEMENTS = [
    # 1) 修正既有单位行（补单位名与条码）
    "UPDATE erp_product_unit SET unit_name='瓶', barcode='6901234567892' WHERE id=2073290060458909697",
    "UPDATE erp_product_unit SET unit_name='箱', barcode='6901234567908' WHERE id=2073290060521824258",
    "UPDATE erp_product_unit SET unit_name='袋', barcode='6901234567915' WHERE id=2073290060567961602",
    # 2) 测试商品（3 个：已上架/未上架/已停用各一）
    """INSERT INTO erp_product (id, tenant_id, product_code, product_name, spec, model, origin, status,
        mall_shelf_status, category_id, unit, image_url, create_time, update_time, deleted, is_standard_product, use_coupon)
       VALUES (990000000000000001,1,'SP-TEST-001','博多家园百香果果酱','1kg*6瓶','BD-100','上海','ENABLED',1,
        2072844550224740354,'瓶','',now(),now(),0,1,0)
       ON CONFLICT (id) DO NOTHING""",
    """INSERT INTO erp_product (id, tenant_id, product_code, product_name, spec, model, origin, status,
        mall_shelf_status, category_id, unit, image_url, create_time, update_time, deleted, is_standard_product, use_coupon)
       VALUES (990000000000000002,1,'SP-TEST-002','海鲜全家福','500g','HX-200','福建','ENABLED',0,
        2072844550224740354,'袋','',now(),now(),0,1,0)
       ON CONFLICT (id) DO NOTHING""",
    """INSERT INTO erp_product (id, tenant_id, product_code, product_name, spec, model, origin, status,
        mall_shelf_status, category_id, unit, image_url, create_time, update_time, deleted, is_standard_product, use_coupon)
       VALUES (990000000000000003,1,'SP-TEST-003','手工蒸饺','1kg*4袋','ZJ-300','河南','DISABLED',1,
        2072844513319059457,'袋','',now(),now(),0,1,0)
       ON CONFLICT (id) DO NOTHING""",
    # 3) 商品多单位行（含条码，其中一行不设条码用于验证「未设置条码」筛选）
    """INSERT INTO erp_product_unit (id, tenant_id, product_id, unit_name, is_base_unit, conversion_rate, barcode, sort_order, deleted, create_time, update_time)
       VALUES (990000000000000011,1,990000000000000001,'瓶',1,1,'6930010100011',1,0,now(),now()),
              (990000000000000012,1,990000000000000001,'箱',0,6,'6930010100028',2,0,now(),now()),
              (990000000000000013,1,990000000000000002,'袋',1,1,'6930010200010',1,0,now(),now()),
              (990000000000000014,1,990000000000000002,'箱',0,4,'6930010200027',2,0,now(),now()),
              (990000000000000015,1,990000000000000003,'袋',1,1,NULL,1,0,now(),now()),
              (990000000000000016,1,990000000000000003,'箱',0,4,'6930010300024',2,0,now(),now())
       ON CONFLICT (id) DO NOTHING""",
]

conn = psycopg2.connect(DSN)
cur = conn.cursor()
for sql in STATEMENTS:
    cur.execute(sql)
    print('OK', cur.rowcount, sql.strip().split('\n')[0][:70])
conn.commit()

cur.execute("""SELECT count(*) FROM erp_product_unit u JOIN erp_product p ON p.id=u.product_id AND p.deleted=0
               WHERE u.deleted=0""")
print('页面可见行数 =', cur.fetchone()[0])
cur.close()
conn.close()
