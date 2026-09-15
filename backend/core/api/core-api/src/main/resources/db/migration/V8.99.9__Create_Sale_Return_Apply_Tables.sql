-- 创建销售退货申请主表和明细表
-- 对标金蝶/用友/管家婆：字段按业务实体分组（快照冗余设计，宽表查询性能优先）

-- ═══════════════════════════════════════
-- 1. 销售退货申请主表
-- ═══════════════════════════════════════
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'erp_sale_return') THEN
        CREATE TABLE erp_sale_return (
            -- 主键/租户
            id BIGINT NOT NULL PRIMARY KEY,
            tenant_id BIGINT,
            version INTEGER DEFAULT 0,
            deleted INTEGER DEFAULT 0,

            -- 单据编号（XSTHSQD-日期-序列）
            return_no VARCHAR(64),

            -- ═══ 客户快照 ═══
            customer_id BIGINT,
            customer_name VARCHAR(200),
            customer_code VARCHAR(64),
            customer_level VARCHAR(64),
            contact_name VARCHAR(100),
            contact_phone VARCHAR(64),
            contact_address VARCHAR(255),
            customer_remark TEXT,
            customer_ticket VARCHAR(64),
            bank_name VARCHAR(200),
            bank_account VARCHAR(100),
            tax_no VARCHAR(64),

            -- ═══ 仓库快照 ═══
            warehouse_id BIGINT,
            warehouse_name VARCHAR(200),

            -- ═══ 经手人/部门 ═══
            handler_id BIGINT,
            handler_name VARCHAR(100),
            dept_id BIGINT,
            dept_name VARCHAR(100),

            -- ═══ 单据核心字段 ═══
            order_date TIMESTAMP,
            return_type INTEGER DEFAULT 0,
            expected_receive_date VARCHAR(32),
            status INTEGER DEFAULT 0,
            print_count INTEGER DEFAULT 0,
            generate_type VARCHAR(32),
            settle_status VARCHAR(32),
            sales_type VARCHAR(64),
            product_line_attr VARCHAR(64),
            summary TEXT,

            -- ═══ 金额计算链（对标SAP/金蝶标准） ═══
            product_amount DECIMAL(18,2) DEFAULT 0,
            promo_discount DECIMAL(18,2) DEFAULT 0,
            coupon_amount DECIMAL(18,2) DEFAULT 0,
            direct_discount DECIMAL(18,2) DEFAULT 0,
            discount_amount DECIMAL(18,2) DEFAULT 0,
            other_fee DECIMAL(18,2) DEFAULT 0,
            bill_amount DECIMAL(18,2) DEFAULT 0,
            total_amount DECIMAL(18,2) DEFAULT 0,
            total_quantity DECIMAL(18,2) DEFAULT 0,

            -- 数量汇总
            ordered_quantity DECIMAL(18,2) DEFAULT 0,
            received_quantity DECIMAL(18,2) DEFAULT 0,
            unreceived_quantity DECIMAL(18,2) DEFAULT 0,
            return_quantity_total DECIMAL(18,2) DEFAULT 0,

            -- 物理属性汇总
            total_weight DECIMAL(18,2) DEFAULT 0,
            total_volume DECIMAL(18,2) DEFAULT 0,

            -- ═══ 付款/信用 ═══
            credit_limit DECIMAL(18,2) DEFAULT 0,
            available_credit DECIMAL(18,2) DEFAULT 0,
            current_debt DECIMAL(18,2) DEFAULT 0,
            prev_debt DECIMAL(18,2) DEFAULT 0,
            debt_balance DECIMAL(18,2) DEFAULT 0,
            collection_deadline VARCHAR(32),
            settlement_method VARCHAR(32),
            settled_amount DECIMAL(18,2) DEFAULT 0,
            payment_account1 VARCHAR(64),
            payment_account2 VARCHAR(64),
            payment_account3 VARCHAR(64),
            payment_account4 VARCHAR(64),

            -- ═══ 物流信息（对标管家婆/金蝶标准） ═══
            delivery_method VARCHAR(32),
            delivery_route VARCHAR(100),
            delivery_route_id BIGINT,
            delivery_no VARCHAR(64),
            delivery_order_no VARCHAR(64),
            waybill_no VARCHAR(64),
            logistics_company VARCHAR(200),
            logistics_no VARCHAR(64),
            shipping_fee DECIMAL(18,2) DEFAULT 0,
            freight_payer VARCHAR(32),
            cod_amount DECIMAL(18,2) DEFAULT 0,
            driver_name VARCHAR(64),
            driver_id BIGINT,
            delivery_vehicle VARCHAR(64),

            -- ═══ 会员/积分 ═══
            member_card_no VARCHAR(64),
            member_name VARCHAR(100),
            member_discount DECIMAL(18,2) DEFAULT 0,
            prev_points DECIMAL(18,2) DEFAULT 0,
            member_generated_points DECIMAL(18,2) DEFAULT 0,
            member_exchange_points DECIMAL(18,2) DEFAULT 0,
            member_used_points DECIMAL(18,2) DEFAULT 0,
            current_points DECIMAL(18,2) DEFAULT 0,

            -- ═══ 源单关联 ═══
            source_order VARCHAR(64),
            source_order_id BIGINT,
            delivery_order_id BIGINT,

            -- ═══ 审批 ═══
            auditor VARCHAR(64),
            auditor_id BIGINT,
            auditor_name VARCHAR(64),
            submit_by BIGINT,
            submit_time TIMESTAMP,
            approved_by BIGINT,
            approved_time TIMESTAMP,
            approved_note TEXT,
            reason TEXT,
            audit_time TIMESTAMP,

            -- ═══ 收货信息 ═══
            receiver_name VARCHAR(100),
            receiver_phone VARCHAR(64),
            shipping_address VARCHAR(255),

            -- ═══ 结算/支付 ═══
            payment_date TIMESTAMP,
            reconciliation_date TIMESTAMP,

            -- ═══ 表头自定义字段（数字） ═══
            ext_num1 DECIMAL(18,2),
            ext_num2 DECIMAL(18,2),
            ext_num3 DECIMAL(18,2),
            ext_num4 DECIMAL(18,2),
            ext_num5 DECIMAL(18,2),

            -- ═══ 表头自定义字段（文本） ═══
            ext_text1 VARCHAR(255),
            ext_text2 VARCHAR(255),
            ext_text3 VARCHAR(255),
            ext_text4 VARCHAR(255),
            ext_text5 VARCHAR(255),

            -- ═══ 表头自定义字段（往来单位/职员/部门） ═══
            ext_partner BIGINT,
            ext_staff BIGINT,
            ext_dept BIGINT,

            -- ═══ 表尾自定义字段 ═══
            footer_ext_text1 VARCHAR(255),
            footer_ext_text2 VARCHAR(255),

            -- ═══ 备注 ═══
            remark TEXT,
            internal_note TEXT,
            buyer_remark TEXT,

            -- ═══ 附件 ═══
            attachment VARCHAR(500),

            -- ═══ 区域/记账/打印 ═══
            region VARCHAR(64),
            bookkeeping_time TIMESTAMP,
            print_time TIMESTAMP,

            -- ═══ 审计字段 ═══
            create_by BIGINT,
            creator_name VARCHAR(64),
            create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            update_by BIGINT,
            update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        COMMENT ON TABLE erp_sale_return IS '销售退货申请单（宽表快照设计，对标SAP/金蝶/用友标准）';
    END IF;
END $$;

-- ═══════════════════════════════════════
-- 2. 销售退货申请明细表
-- ═══════════════════════════════════════
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'erp_sale_return_item') THEN
        CREATE TABLE erp_sale_return_item (
            id BIGINT NOT NULL PRIMARY KEY,
            tenant_id BIGINT,
            return_id BIGINT NOT NULL,
            line_no INTEGER DEFAULT 0,
            deleted INTEGER DEFAULT 0,

            -- ═══ 商品快照 ═══
            product_id BIGINT,
            product_code VARCHAR(64),
            product_name VARCHAR(200),
            product_spec VARCHAR(255),
            product_unit VARCHAR(32),
            barcode VARCHAR(128),
            specification VARCHAR(255),
            image_url VARCHAR(500),

            -- 商品属性
            area VARCHAR(64),
            model_no VARCHAR(64),
            origin_place VARCHAR(100),
            brand VARCHAR(100),

            -- ═══ 单位/包装 ═══
            unit VARCHAR(32),
            small_unit VARCHAR(32),
            conversion_relation VARCHAR(64),
            conversion_result DECIMAL(18,2) DEFAULT 0,
            big_pack DECIMAL(18,2) DEFAULT 0,
            mid_pack DECIMAL(18,2) DEFAULT 0,
            small_pack DECIMAL(18,2) DEFAULT 0,
            piece_quantity DECIMAL(18,2) DEFAULT 0,

            -- ═══ 数量 ═══
            return_quantity DECIMAL(18,2) DEFAULT 0,
            received_quantity DECIMAL(18,2) DEFAULT 0,
            unreceived_quantity DECIMAL(18,2) DEFAULT 0,
            terminated_quantity DECIMAL(18,2) DEFAULT 0,
            terminated_amount DECIMAL(18,2) DEFAULT 0,

            -- ═══ 价格/金额 ═══
            unit_price DECIMAL(18,6) DEFAULT 0,
            line_amount DECIMAL(18,2) DEFAULT 0,
            tax_rate DECIMAL(5,2) DEFAULT 13,
            discount_rate DECIMAL(10,4) DEFAULT 0,
            discounted_price DECIMAL(18,6) DEFAULT 0,
            discounted_amount DECIMAL(18,2) DEFAULT 0,

            -- 小单位
            small_unit_price DECIMAL(18,6) DEFAULT 0,
            small_unit_quantity DECIMAL(18,2) DEFAULT 0,

            -- 价格体系
            last_sale_date TIMESTAMP,
            last_sale_price DECIMAL(18,2) DEFAULT 0,
            retail_price DECIMAL(18,2) DEFAULT 0,
            wholesale_price DECIMAL(18,2) DEFAULT 0,
            min_sale_price DECIMAL(18,2) DEFAULT 0,

            -- ═══ 库存 ═══
            available_stock DECIMAL(18,2) DEFAULT 0,
            available_stock_converted DECIMAL(18,2) DEFAULT 0,
            book_stock DECIMAL(18,2) DEFAULT 0,

            -- ═══ 成本（参考成本） ═══
            ref_cost_price DECIMAL(18,6) DEFAULT 0,
            ref_cost_amount DECIMAL(18,2) DEFAULT 0,

            -- ═══ 物理属性 ═══
            weight DECIMAL(18,4) DEFAULT 0,
            volume DECIMAL(18,4) DEFAULT 0,

            -- ═══ 行属性 ═══
            is_gift BOOLEAN DEFAULT FALSE,
            product_line_attr VARCHAR(32) DEFAULT '正常',
            item_remark TEXT,
            remark TEXT,
            exchange_gift VARCHAR(100),
            exchange_points DECIMAL(18,2) DEFAULT 0,
            member_generated_points DECIMAL(18,2) DEFAULT 0,
            member_used_points DECIMAL(18,2) DEFAULT 0,

            -- ═══ 8个标准化价格等级（对标开发文档） ═══
            price_level1 DECIMAL(18,2) DEFAULT 0,
            price_level2 DECIMAL(18,2) DEFAULT 0,
            price_level3 DECIMAL(18,2) DEFAULT 0,
            price_level4 DECIMAL(18,2) DEFAULT 0,
            price_level5 DECIMAL(18,2) DEFAULT 0,
            price_level6 DECIMAL(18,2) DEFAULT 0,
            price_level7 DECIMAL(18,2) DEFAULT 0,
            price_level8 DECIMAL(18,2) DEFAULT 0,

            -- ═══ 单据自定义字段（数字1-7） ═══
            ext_num1 DECIMAL(18,2),
            ext_num2 DECIMAL(18,2),
            ext_num3 DECIMAL(18,2),
            ext_num4 DECIMAL(18,2),
            ext_num5 DECIMAL(18,2),
            ext_num6 DECIMAL(18,2),
            ext_num7 DECIMAL(18,2),

            -- ═══ 单据自定义字段（文本1-2） ═══
            ext_text1 VARCHAR(255),
            ext_text2 VARCHAR(255),

            -- ═══ 单据自定义字段（往来单位/职员/部门） ═══
            ext_partner BIGINT,
            ext_staff BIGINT,
            ext_dept BIGINT,

            -- ═══ 审计字段 ═══
            create_by BIGINT,
            create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            update_by BIGINT,
            update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );

        COMMENT ON TABLE erp_sale_return_item IS '销售退货申请明细行（宽表快照设计，对标SAP/金蝶/用友标准）';

        CREATE INDEX idx_erp_sale_return_item_return_id ON erp_sale_return_item(return_id);
    END IF;
END $$;

-- ═══════════════════════════════════════
-- 3. 索引（主表）
-- ═══════════════════════════════════════
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_erp_sale_return_no') THEN
        CREATE INDEX idx_erp_sale_return_no ON erp_sale_return(return_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_erp_sale_return_status') THEN
        CREATE INDEX idx_erp_sale_return_status ON erp_sale_return(status);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_erp_sale_return_customer') THEN
        CREATE INDEX idx_erp_sale_return_customer ON erp_sale_return(customer_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_erp_sale_return_order_date') THEN
        CREATE INDEX idx_erp_sale_return_order_date ON erp_sale_return(order_date DESC);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_erp_sale_return_create_time') THEN
        CREATE INDEX idx_erp_sale_return_create_time ON erp_sale_return(create_time DESC);
    END IF;
END $$;
