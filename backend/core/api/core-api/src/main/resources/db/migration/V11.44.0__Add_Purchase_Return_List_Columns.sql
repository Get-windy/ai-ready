-- 采购退货单主表补充列表/表单扩展列
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS supplier_no          VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS bank_name           VARCHAR(200);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS bank_account        VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS tax_no              VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS warehouse_id        BIGINT;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS warehouse_name      VARCHAR(200);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS purchaser_id        BIGINT;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS purchaser_name      VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS department_id       BIGINT;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS department_name     VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS return_date         DATE;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS contact_name        VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS contact_phone       VARCHAR(50);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS contact_address     VARCHAR(500);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS supplier_remark     VARCHAR(500);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS return_type_desc    VARCHAR(50);

ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS discount_amount     DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS tax_amount          DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS total_amount_with_tax DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS settled_amount      DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS settle_status       INTEGER;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS weight              DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS volume              DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS summary             VARCHAR(500);

ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS ext_num1            DECIMAL(18,2);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS ext_num2            DECIMAL(18,2);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS ext_text1           VARCHAR(200);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS ext_text2           VARCHAR(200);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS ext_text3           VARCHAR(200);

ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS payment_account     VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS payment_amount      DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS more_accounts       VARCHAR(200);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS prev_prepaid        DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS refund_prepay       DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS prepaid_balance     DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS current_debt        DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS prev_debt           DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS debt_balance        DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS payment_deadline    DATE;

ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS create_by_name       VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS poster_name          VARCHAR(100);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS post_time            TIMESTAMP;
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS attachment           VARCHAR(500);
ALTER TABLE erp_purchase_return ADD COLUMN IF NOT EXISTS print_count          INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_warehouse ON erp_purchase_return(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_status ON erp_purchase_return(status);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_supplier ON erp_purchase_return(supplier_id);

-- 采购退货明细表补充扩展列（商品、库存、批次、包装、价格等级、单据自定义等）
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS image                    VARCHAR(500);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS barcode                  VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS model                    VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS origin                   VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS brand                    VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS region                   VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS location                 VARCHAR(100);

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS available_stock           DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS available_stock_converted  DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS book_stock               DECIMAL(18,4) DEFAULT 0;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS batch_no                 VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS batch_code               VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS production_date          TIMESTAMP;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS shelf_life               VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS expiry_date              TIMESTAMP;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS conversion_relation       VARCHAR(100);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS piece_quantity           DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS big_pack                 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS mid_pack                 DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS small_pack               DECIMAL(18,4) DEFAULT 0;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS latest_purchase_date      TIMESTAMP;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS retail_price             DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS wholesale_price          DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS small_unit               VARCHAR(50);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS small_unit_price         DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS small_unit_quantity      DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS unit_cost                DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS cost_amount              DECIMAL(18,4) DEFAULT 0;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS tax_rate                 DECIMAL(10,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS tax_amount               DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS line_total               DECIMAL(18,2) DEFAULT 0;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS volume                   DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS weight                   DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS gift                     BOOLEAN DEFAULT FALSE;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS restaurant               BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS canteen                  BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS out_restaurant           BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS vip_self                 BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS large_group              BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS vip_level1               BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS vip_level2               BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS special_customer         BOOLEAN DEFAULT FALSE;

ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field1             DECIMAL(18,4);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field2             DECIMAL(18,4);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field3             DECIMAL(18,4);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field4             VARCHAR(500);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field5             VARCHAR(500);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field6             DECIMAL(18,4);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field7             DECIMAL(18,4);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field8             VARCHAR(500);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field9             VARCHAR(500);
ALTER TABLE erp_purchase_return_item ADD COLUMN IF NOT EXISTS custom_field10            VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_return_item_return ON erp_purchase_return_item(return_id);
