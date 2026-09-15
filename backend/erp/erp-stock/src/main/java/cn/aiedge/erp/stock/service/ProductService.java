package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.Product;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 产品Service接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface ProductService extends IService<Product> {

    /**
     * 商城默认排序配置在配置中心（sys_project_config）的键名
     *
     * <p>config_key = {@code mall.product.default.sort}，group = {@code mall}，type = {@code json}，
     * 值形如 {@code {"field":"sort","direction":"asc"}}。管理端「设置商城默认排序」与商城列表共用。</p>
     */
    String MALL_SORT_CONFIG_KEY = "mall.product.default.sort";

    /**
     * 获取产品列表
     */
    List<Product> getProductList();

    /**
     * 分页查询产品(含分类、等级名称)
     *
     * <p>商城「单位显示」页新增条件（V11.361.8 建列 / V11.361.9 改正取值口径）：</p>
     * <ul>
     *   <li>{@code productTag} —— 商品标签（erp_product.mall_tags 逗号分隔槽位编码 TAG_1..TAG_20），
     *       按**整槽位包含**匹配（逗号包裹，避免 TAG_1 误命中 TAG_10..TAG_19）；</li>
     *   <li>{@code unitDisplayType} —— **单位粒度**显示类型（erp_product_unit.unit_display_type，
     *       对标实测取值 {@code -1}=全部 / {@code 0}=只显示常用单位 / {@code 1}=只显示小单位 /
     *       {@code 2}=只显示中/大单位），先反查命中单位所属商品ID集合，再 {@code p.id IN (...)}；
     *       无反查结果时直接返回空页；{@code -1}（全部）不额外加粒度条件；
     *       与「是否显示」的区别见 ProductService#batchUpdateUnitDisplay。</li>
     *   <li>{@code unitDisplay} —— 商品级整品显示开关（erp_product.unit_display，1显示/0隐藏），
     *       即对标查询区「单位显示」条件（全部(-1)/是(1)/否(2)），与单位级 unit_display_type
     *       是**两个概念**，分工见 ProductService#batchUpdateUnitDisplayFlag。</li>
     * </ul>
     */
    IPage<Product> getProductPage(Long categoryId, String keyword, String status,
                                   String brand, String industryCategory,
                                   String createTimeStart, String createTimeEnd,
                                   Integer useCoupon, Integer isStandardProduct,
                                   String productType, Integer mallShelfStatus,
                                   String productTag, String unitDisplayType, Integer unitDisplay,
                                   Integer pageNum, Integer pageSize);

    /**
     * 获取产品详情(含等级价格)
     */
    Product getProductDetail(Long id);

    /**
     * 新增产品
     */
    boolean createProduct(Product product);

    /**
     * 更新产品
     */
    boolean updateProduct(Product product);

    /**
     * 更新产品状态
     */
    boolean updateProductStatus(Long id, String status);

    /**
     * 批量更新产品价格
     *
     * @param items 价格更新列表
     * @return 是否成功
     */
    boolean batchUpdatePrices(List<cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO.PriceItem> items);

    /**
     * 导出产品列表(不含分页)
     *
     * @param categoryId 分类ID(可选)
     * @param keyword    关键字(可选)
     * @param status     状态(可选)
     * @param brand      品牌(可选)
     * @param industryCategory 行业类别(可选)
     * @param createTimeStart  创建日期起始(可选)
     * @param createTimeEnd    创建日期截止(可选)
     * @param useCoupon        使用优惠券(可选)
     * @param isStandardProduct 是否标品(可选)
     * @return 产品列表
     */
    List<Product> exportList(Long categoryId, String keyword, String status,
                              String brand, String industryCategory,
                              String createTimeStart, String createTimeEnd,
                              Integer useCoupon, Integer isStandardProduct);

    /**
     * 批量更新商品状态
     *
     * <p>status 取值：</p>
     * <ul>
     *   <li>ENABLED / DISABLED —— 商品档案启用/停用，写 erp_product.status；</li>
     *   <li>ON_SHELF / OFF_SHELF —— 商城上架/下架，写 erp_product.mall_shelf_status（1/0），
     *       与 {@link #batchUpdateShelfStatus(List, Integer)} 同口径（商品上架页批量按钮即用此端点）。</li>
     * </ul>
     */
    boolean batchUpdateStatus(List<Long> ids, String status);

    /**
     * 批量删除商品
     */
    boolean batchDelete(List<Long> ids);

    /**
     * 获取不重复的行业类别列表
     */
    List<String> getDistinctIndustryCategories();

    /**
     * 获取不重复的品牌列表
     */
    List<String> getDistinctBrands();

    /**
     * 获取商品标签槽位编码列表（erp_product.mall_tags 已打标槽位去重，升序）
     *
     * <p>商品上架页「商品标签」查询条件选项来源；昵称显示名由
     * 「商品辅助资料 → 商品标签」维护（erp_mall_tag，见 V11.161.0）。</p>
     */
    List<String> getDistinctMallTags();

    /**
     * 批量搬移分类
     *
     * @return 影响行数
     */
    int batchMoveCategory(List<Long> ids, Long categoryId);

    /**
     * 批量修改字段（brand/industryCategory/categoryId/isStandardProduct/useCoupon）
     */
    int batchUpdateFields(List<Long> ids, java.util.Map<String, Object> fields);

    /**
     * 批量上架/下架（商城）
     */
    int batchUpdateShelfStatus(List<Long> ids, Integer mallShelfStatus);

    /**
     * 批量设置**单位粒度显示类型**（erp_product_unit.unit_display_type）
     *
     * <p>取值口径 = 对标 ql361「单位显示」页查询区「单位显示类型」下拉 DOM 实测
     * （2026-09-14）：{@code "-1"}=全部 / {@code "0"}=只显示常用单位 / {@code "1"}=只显示小单位 /
     * {@code "2"}=只显示中/大单位。见 Flyway V11.361.9（改正 V11.361.8 的 SHOW/HIDE 猜测口径）。</p>
     *
     * <p><b>与「单位显示」布尔开关的分工（唯一口径）：</b></p>
     * <ul>
     *   <li><b>单位粒度类型</b> {@code erp_product_unit.unit_display_type}（-1/0/1/2）——
     *       「显示哪些粒度的单位」，本方法写该商品**全部有效单位**；</li>
     *   <li><b>是否显示</b> {@code erp_product.unit_display}（0隐藏/1显示）——
     *       本页「单位显示」列的 √/× 勾选与批量显示/隐藏，走
     *       {@link #batchUpdateUnitDisplayFlag(java.util.List, Integer)}，**不写**单位粒度列。</li>
     * </ul>
     *
     * @param ids              商品ID集合
     * @param unitDisplayType  {@code -1/0/1/2}（兼容 {@code ALL/全部}、{@code 只显示常用单位} 等中文写法；
     *                         四种粒度均属"显示"，商品级整品开关同步置 1）
     * @return 处理的商品行数
     * @throws IllegalArgumentException 取值非法时抛出（不静默改数据）
     */
    int batchUpdateUnitDisplay(List<Long> ids, String unitDisplayType);

    /**
     * 批量设置「单位显示」**布尔开关**（商品级整品开关 erp_product.unit_display）
     *
     * <p>语义 = 对标查询区「单位显示」条件（实测 全部(-1)/是(1)/否(2)，本系统商品级列用 1显示/0隐藏）：
     * 「该商品（含其全部单位）是否在商城显示」。这是**与「单位显示类型」（单位粒度类型 -1/0/1/2）
     * 不同的概念**，因此只写商品级 {@code erp_product.unit_display}，不改单位粒度列。</p>
     *
     * @param ids         商品ID集合
     * @param unitDisplay 1=显示 / 0=隐藏（null 或非法值抛 IllegalArgumentException）
     * @return 处理的商品行数
     */
    int batchUpdateUnitDisplayFlag(List<Long> ids, Integer unitDisplay);

    /**
     * 设置商城默认排序方式（全租户商品统一）
     */
    int setMallSortType(String sortType);

    /**
     * Excel 导入商品（真实解析，逐行建档并生成基本单位）
     *
     * @return { count, skipped, errors }
     */
    java.util.Map<String, Object> importFromExcel(org.springframework.web.multipart.MultipartFile file);
}
