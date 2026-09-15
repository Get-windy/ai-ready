-- ============================================================================
-- V11.136.0 图片管理（资料 → 商品管理 → 图片管理）从零复刻（2026-09-11）
--
-- 背景：对标 ql361「资料 → 商品管理 → 图片管理」为多 Tab 组合页
--      （商品图片列表 + 图片空间），本系统原为只读桩页（apiUrl=/md/image/page 404）。
--
-- 口径（对标系统实测抓取 2026-09-11 22stable.ql361.com）：
--   1) 商品图片列表：商品维度 9 列（商品名称/商品状态/货号/规格/型号/产地/品牌/
--      上传图片/图片），行内「选择图片」，图片列支持删除 + 主图标记。
--   2) 图片空间：图片库网格（素材），支持上传图片 / 自动匹配 / 删除 / 搬移，
--      匹配方式「按名称」或「按商品货号」，图片命名 sp001-1、sp001-2 表示多张。
--
-- P0 图片单一口径（红线）：商品图片素材为全局基础数据（商品/营销/商城统一引用），
--      仅此一张表承载「商品图片 + 图片空间素材」，严禁另建重复图片表；
--      product_id 为空即图片空间未匹配素材，非空即已关联商品的图片。
--
-- 处理：新增表幂等（IF NOT EXISTS）；仅新增，不改动既有表结构。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 商品图片 / 图片空间素材统一表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS erp_product_image (
    id          BIGINT       PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    product_id  BIGINT,
    image_name  VARCHAR(255),
    match_key   VARCHAR(255),
    image_url   VARCHAR(500),
    file_path   VARCHAR(500),
    file_size   BIGINT,
    file_type   VARCHAR(100),
    is_main     SMALLINT     DEFAULT 0,
    sort_no     INTEGER      DEFAULT 0,
    source      VARCHAR(20)  DEFAULT 'UPLOAD',
    remark      VARCHAR(500),
    deleted     INTEGER      NOT NULL DEFAULT 0,
    create_by   BIGINT,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT,
    update_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  erp_product_image             IS '商品图片/图片空间素材表（统一口径）';
COMMENT ON COLUMN erp_product_image.product_id  IS '关联商品ID（NULL=图片空间未匹配素材）';
COMMENT ON COLUMN erp_product_image.image_name  IS '图片名称（上传原始文件名，自动匹配依据）';
COMMENT ON COLUMN erp_product_image.match_key   IS '匹配键（去掉 -N 序号后缀的名称）';
COMMENT ON COLUMN erp_product_image.image_url   IS '图片访问URL';
COMMENT ON COLUMN erp_product_image.file_path   IS '存储相对路径';
COMMENT ON COLUMN erp_product_image.is_main     IS '是否主图 0否 1是';
COMMENT ON COLUMN erp_product_image.source      IS '来源：UPLOAD 上传 / AUTO_MATCH 自动匹配 / PRODUCT_FORM 商品表单';

CREATE INDEX IF NOT EXISTS idx_epi_tenant      ON erp_product_image (tenant_id);
CREATE INDEX IF NOT EXISTS idx_epi_product     ON erp_product_image (product_id);
CREATE INDEX IF NOT EXISTS idx_epi_match_key   ON erp_product_image (match_key);
CREATE INDEX IF NOT EXISTS idx_epi_deleted     ON erp_product_image (deleted);

-- ------------------------------------------------------------
-- 2. 菜单组件指向真实页面（原 V6.21.0 占位 views/common/placeholder/index.vue）
-- ------------------------------------------------------------
UPDATE sys_menu
   SET component = 'views/md/image/index.vue',
       update_time = CURRENT_TIMESTAMP
 WHERE id = 70505
   AND menu_code = 'md:image';
