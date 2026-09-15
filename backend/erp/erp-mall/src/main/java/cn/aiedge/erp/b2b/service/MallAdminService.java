package cn.aiedge.erp.b2b.service;

import cn.aiedge.erp.b2b.dao.ErpProductMall;
import cn.aiedge.erp.b2b.dao.ErpSaleOrderMall;
import cn.aiedge.erp.b2b.dto.MallOrderItemPageDTO;
import cn.aiedge.erp.b2b.dto.MallOrderRefundRequest;
import cn.aiedge.erp.b2b.dto.MallOrderShipRequest;
import cn.aiedge.erp.b2b.dto.ShopUserUpdateRequest;
import cn.aiedge.erp.b2b.dto.TradeAnalysisDTO;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopDecoration;
import cn.aiedge.erp.b2b.model.ShopTemplate;
import cn.aiedge.erp.b2b.model.ShopUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 商城管理后台服务接口
 */
public interface MallAdminService {

    // ==================== 商城配置 ====================

    /**
     * 获取当前租户的商城配置
     */
    ShopConfig getConfig();

    /**
     * 更新商城配置
     */
    void updateConfig(ShopConfig config);

    // ==================== 商城用户审核 ====================

    /**
     * 分页查询商城用户（买家账号 / 买家申请管理共用）。
     *
     * <p>过滤语义（参数为空即不过滤）：</p>
     * <ul>
     *   <li>{@code keyword}：用户名/昵称/手机号/公司名 模糊</li>
     *   <li>{@code auditStatus}：审核状态 0待审 1通过 2驳回</li>
     *   <li>{@code status}：启用状态 1正常 0禁用</li>
     *   <li>{@code createTimeStart}/{@code createTimeEnd}：注册时间范围（按 shop_user.create_time；
     *       起始含当日 00:00:00，截止含当日 23:59:59，支持 yyyy-MM-dd 或 ISO 日期时间串）</li>
     *   <li>{@code categoryId}：归属分类（shop_user.category_id）</li>
     *   <li>{@code gradeId}：客户级别（biz_customer_grade.id）→ 解析为级别名称后匹配
     *       shop_user.customer_level（该表无 grade_id 列，级别以名称存于 customer_level）</li>
     *   <li>{@code showDisabled}：是否显示停用。false = 只看启用（强制 status=1，忽略 status 参数）；
     *       true / null = 不过滤（此时仍可用 status 参数显式指定）</li>
     * </ul>
     */
    Page<ShopUser> pageUsers(Integer pageNum, Integer pageSize, String keyword, Integer auditStatus, Integer status,
                             String createTimeStart, String createTimeEnd, Long categoryId, Long gradeId,
                             Boolean showDisabled);

    /**
     * 逻辑删除买家账号（shop_user.deleted 置 1，@TableLogic 逻辑删除，非物理删除）
     *
     * @return 是否删除成功（记录不存在或非本租户时抛 {@code BusinessException.notFound}，不静默成功）
     */
    boolean deleteUser(Long userId);

    /**
     * 编辑买家账号（仅更新 {@link ShopUserUpdateRequest} 中非 null 的可编辑字段）
     *
     * @return 更新后的买家账号（含最新字段，供前端刷新列表）
     */
    ShopUser updateUser(Long userId, ShopUserUpdateRequest request);

    /**
     * 审核通过
     */
    void approveUser(Long userId);

    /**
     * 审核驳回
     */
    void rejectUser(Long userId, String reason);

    /**
     * 启用/禁用用户
     */
    void toggleUserStatus(Long userId, Integer status);

    // ==================== 轮播图管理 ====================

    /**
     * 获取轮播图列表（按排序升序）
     */
    List<ShopBanner> listBanners();

    /**
     * 创建轮播图
     */
    void createBanner(ShopBanner banner);

    /**
     * 更新轮播图
     */
    void updateBanner(ShopBanner banner);

    /**
     * 删除轮播图
     */
    void deleteBanner(Long id);

    // ==================== 页面模板 ====================

    /**
     * 获取本租户「我的模板」列表（库条目 is_library=1 不在此列，见
     * {@link #listTemplateLibrary()}）。
     *
     * <p>过滤口径：status=1 且 (is_library 为空 或 is_library=0) —— 括注部分用于兼容
     * V11.366.0 之前已存在的历史模板行（其 is_library 为 NULL）。</p>
     */
    List<ShopTemplate> listTemplates();

    /**
     * 行业模板库列表（is_library=1）。
     *
     * <p>⚠️ 库条目只承载**实测到的 14 个行业名**，各行业模板的**内部布局内容未实测**，
     * 故 {@code configJson} 一律为空（见 V11.366.0 迁移注释）。</p>
     */
    List<ShopTemplate> listTemplateLibrary();

    /**
     * 新增「我的模板」（对标「装修模板 → 我的模板 → 新增模板」）。
     *
     * @param template 待新增模板；{@code templateName} 必填；
     *                 {@code templateCode} 为空时按 {@code DECO_<时间戳>} 自动生成（表上有唯一约束）；
     *                 {@code configJson} 为装修结构 JSON 文本（⚠️ 结构未实测，按不透明文本存取）
     * @return 新增后的模板实体（含生成的 id / templateCode）
     */
    ShopTemplate createTemplate(ShopTemplate template);

    /**
     * 引用行业库模板：复制库条目为本租户的「我的模板」（is_library=0）。
     *
     * <p>⚠️ 因库条目的 {@code configJson} 为空（内容未实测），复制出的模板
     * {@code configJson} 亦为空 —— 这是**如实留缺口**，不臆造模板内容。</p>
     *
     * @param libraryTemplateId 库条目 id（必须存在且 is_library=1）
     * @return 复制出的本租户模板
     */
    ShopTemplate referenceLibraryTemplate(Long libraryTemplateId);

    // ==================== 装修配置（商城装修 → 排版布局存储） ====================

    /**
     * 查询装修配置列表（可按 scope 过滤；scope 为空即不过滤）。
     *
     * @param scope HOME 首页 / CATEGORY 分类页 / PRODUCT_DETAIL 商品详情，为空则全部
     */
    List<ShopDecoration> listDecorations(String scope);

    /**
     * 查询单条装修配置（不存在或非本租户时抛 {@code BusinessException.notFound}）。
     */
    ShopDecoration getDecoration(Long id);

    /**
     * 新增装修配置（对标「新增模板」进入排版布局编辑器后的保存）。
     *
     * @param decoration {@code name} 必填；{@code scope} 为空默认 HOME；
     *                   {@code configJson} 为装修结构 JSON 文本
     * @return 新增后的装修配置（含生成的 id）
     */
    ShopDecoration createDecoration(ShopDecoration decoration);

    /**
     * 更新装修配置（仅覆盖请求体中非 null 字段：{@code name}/{@code scope}/
     * {@code templateId}/{@code configJson}/{@code status}）。
     *
     * @return 更新后的装修配置
     */
    ShopDecoration updateDecoration(Long id, ShopDecoration decoration);

    /**
     * 逻辑删除装修配置（及其商品关联行，同一事务）。
     */
    void deleteDecoration(Long id);

    /**
     * 查询某装修配置已关联的商品 id 集合（对标「商品详情 → 设置应用商品」）。
     */
    List<Long> listDecorationProducts(Long decorationId);

    /**
     * 全量替换某装修配置的关联商品集合（先逻辑删旧、再插新，同一事务）。
     *
     * @param productIds 商品 id 集合（null 或空集合 = 清空关联）
     * @return 写入后的商品 id 集合（去重后）
     */
    List<Long> replaceDecorationProducts(Long decorationId, List<Long> productIds);

    // ==================== 商品管理 ====================

    /**
     * 分页查询商城商品（数据源 v_mall_product 视图，ErpProductMall）
     *
     * <p>商品上架页 8 项查询条件对应参数：keyword（商品名称/货号/条码/规格/型号）、
     * brand、productName、productTag、status、couponUsed、productType、visibleStatus。
     * 产品类参数为空（null 或空串）时不做该条件过滤。</p>
     *
     * @param couponUsed 使用优惠券："yes"/"1"/"true"=是，"no"/"0"/"false"=否，其余忽略
     */
    Page<ErpProductMall> pageProducts(Integer pageNum, Integer pageSize, String keyword, String categoryId,
                                      String status, String brand, String productName, String productTag,
                                      String couponUsed, String productType, String visibleStatus);

    /**
     * 创建/更新商品
     */
    void saveProduct(MallProduct product);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);

    // ==================== 订单管理 ====================

    /**
     * 分页查询商城订单（管理端，数据源 erp_sale_order）
     *
     * <p>查询条件全部可选，为空即不过滤：</p>
     * <ul>
     *   <li>{@code keyword} 综合模糊（单据编号 / 客户名称 / 收货人）</li>
     *   <li>{@code orderStatus} 商城状态串（逗号分隔多值，如 "PENDING_AUDIT,PENDING_PAYMENT"，按 {@code toErpStatus} 映射）；
     *       无法识别的值显式忽略并记日志，不再退化到 0</li>
     *   <li>{@code status} erp_sale_order.status 数字，单值或逗号分隔多值（商城订单页 Tab1 提交数字）；
     *       与 {@code orderStatus} 取并集</li>
     *   <li>{@code orderNo} 单据编号模糊</li>
     *   <li>{@code startDate}/{@code endDate} 单据日期范围（endDate 只传日期时按含当日处理）</li>
     *   <li>{@code consignee} 收货人模糊</li>
     *   <li>{@code paymentMethod} 支付方式等值</li>
     *   <li>{@code orderSource} 订单来源等值（2=企业客户商城 3=个人会员商城）</li>
     *   <li>{@code productName} 商品名称/货号：反查 erp_sale_order_item 的单据 ID 集合后过滤</li>
     * </ul>
     */
    Page<ErpSaleOrderMall> pageOrders(Integer pageNum, Integer pageSize, String keyword, String orderStatus,
                                      String status, String orderNo, String startDate, String endDate,
                                      String consignee, String paymentMethod, Integer orderSource,
                                      String productName);

    /**
     * 获取订单详情（数据源 erp_sale_order）
     */
    ErpSaleOrderMall getOrderDetail(Long id);

    /**
     * 管理端审核通过订单
     */
    void approveOrder(Long orderId);

    /**
     * 管理端审核驳回订单
     */
    void rejectOrder(Long orderId, String reason);

    /**
     * 商城订单统计卡（真实聚合 SQL，非当前页口径）
     *
     * <p>总订单数 / GMV（订单总额合计）/ 客单价 / 退款率（payment_status=4 占比，%）。
     * 口径与 /trade-analysis 一致：取 order_source IN (2,3)、deleted=0、排除已取消（status IN (5,6)）。</p>
     */
    TradeAnalysisDTO.Summary getOrderStats();

    /**
     * 按明细分页（数据源 erp_sale_order_item JOIN erp_sale_order）
     *
     * <p>查询条件与 {@link #pageOrders} 同口径；明细行已透出 {@code departmentName}
     * （来源列 erp_sale_order.dept_name）。</p>
     *
     * @param orderStatus 商城状态串（逗号分隔，如 "PAID,APPROVED"），内部映射为 erp_sale_order.status 整数集合
     * @param status      erp_sale_order.status 数字单值/多值（与 orderStatus 取并集）
     */
    Page<MallOrderItemPageDTO> pageOrderItems(Integer pageNum, Integer pageSize, String keyword, String orderStatus,
                                              String status, String orderNo, String startDate, String endDate,
                                              String consignee, String paymentMethod, Integer orderSource,
                                              String productName);

    /**
     * 收款：received_amount 置为订单总额，payment_status 置 2（已支付）
     */
    void receiveOrder(Long orderId);

    /**
     * 发货：delivery_status 置 2（已发货），物流公司/运单号落单头
     * logistics_company / waybill_no，若订单状态低于 3 则同步推进为 3（部分出库/已发货）
     */
    void shipOrder(Long orderId, MallOrderShipRequest request);

    /**
     * 退款：payment_status 置 4（已退款）
     */
    void refundOrder(Long orderId, MallOrderRefundRequest request);

    /**
     * 强制终止订单：status 置 6（已取消），原因落 remark
     */
    void terminateOrder(Long orderId, String reason);

    /**
     * 批量审核通过，逐条处理并返回成功条数
     */
    int batchApproveOrders(List<Long> ids);

    /**
     * 批量发货，逐条处理并返回成功条数
     */
    int batchShipOrders(List<Long> ids);
}
