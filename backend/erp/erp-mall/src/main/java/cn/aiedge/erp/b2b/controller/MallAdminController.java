package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
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
import cn.aiedge.erp.b2b.service.MallAdminService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/mall/admin")
@Tag(name = "商城管理后台", description = "商城配置、用户审核、轮播图管理等管理接口")
@RequiredArgsConstructor
public class MallAdminController {

    private final MallAdminService mallAdminService;

    // ==================== 商城配置 ====================

    @GetMapping("/config")
    @Operation(summary = "获取商城配置")
    public Result<ShopConfig> getConfig() {
        return Result.ok(mallAdminService.getConfig());
    }

    @PutMapping("/config")
    @Operation(summary = "更新商城配置")
    public Result<Void> updateConfig(@RequestBody ShopConfig config) {
        mallAdminService.updateConfig(config);
        return Result.ok();
    }

    // ==================== 商城用户审核 ====================

    @GetMapping("/user/page")
    @Operation(summary = "分页查询商城用户（买家账号页：注册时间范围/归属分类/客户级别/显示停用）")
    public Result<Page<ShopUser>> pageUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "关键词(用户名/昵称/手机号/公司)") @RequestParam(required = false) String keyword,
            @Parameter(description = "审核状态 0待审 1通过 2驳回") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "状态 1正常 0禁用") @RequestParam(required = false) Integer status,
            @Parameter(description = "注册时间(起) yyyy-MM-dd，含当日 00:00:00") @RequestParam(required = false) String createTimeStart,
            @Parameter(description = "注册时间(止) yyyy-MM-dd，含当日 23:59:59") @RequestParam(required = false) String createTimeEnd,
            @Parameter(description = "归属分类ID(biz_party_category.id)") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "客户级别ID(biz_customer_grade.id)：解析为级别名称后匹配 customer_level") @RequestParam(required = false) Long gradeId,
            @Parameter(description = "是否显示停用：false=只看启用(强制 status=1)；true/缺省=不过滤") @RequestParam(required = false) Boolean showDisabled) {
        return Result.ok(mallAdminService.pageUsers(pageNum, pageSize, keyword, auditStatus, status,
                createTimeStart, createTimeEnd, categoryId, gradeId, showDisabled));
    }

    @PutMapping("/user/{id}/approve")
    @Operation(summary = "审核通过")
    public Result<Void> approveUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        mallAdminService.approveUser(id);
        return Result.ok();
    }

    @PutMapping("/user/{id}/reject")
    @Operation(summary = "审核驳回")
    public Result<Void> rejectUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "驳回原因") @RequestParam String reason) {
        mallAdminService.rejectUser(id, reason);
        return Result.ok();
    }

    @PutMapping("/user/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> toggleUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态 1正常 0禁用") @RequestParam Integer status) {
        mallAdminService.toggleUserStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/user/{id}")
    @Operation(summary = "删除买家账号（逻辑删除 shop_user.deleted=1，非物理删除；记录不存在返回 404 业务异常）")
    public Result<Boolean> deleteUser(@Parameter(description = "买家账号ID") @PathVariable Long id) {
        return Result.ok(mallAdminService.deleteUser(id));
    }

    @PutMapping("/user/{id}")
    @Operation(summary = "编辑买家账号（部分更新：仅更新请求体中非 null 的可编辑字段，返回更新后对象）")
    public Result<ShopUser> updateUser(
            @Parameter(description = "买家账号ID") @PathVariable Long id,
            @RequestBody ShopUserUpdateRequest request) {
        return Result.ok(mallAdminService.updateUser(id, request));
    }

    // ==================== 轮播图管理 ====================

    @GetMapping("/banner")
    @Operation(summary = "获取轮播图列表")
    public Result<List<ShopBanner>> listBanners() {
        return Result.ok(mallAdminService.listBanners());
    }

    @PostMapping("/banner")
    @Operation(summary = "创建轮播图")
    public Result<Void> createBanner(@RequestBody ShopBanner banner) {
        mallAdminService.createBanner(banner);
        return Result.ok();
    }

    @PutMapping("/banner/{id}")
    @Operation(summary = "更新轮播图")
    public Result<Void> updateBanner(
            @Parameter(description = "轮播图ID") @PathVariable Long id,
            @RequestBody ShopBanner banner) {
        banner.setId(id);
        mallAdminService.updateBanner(banner);
        return Result.ok();
    }

    @DeleteMapping("/banner/{id}")
    @Operation(summary = "删除轮播图")
    public Result<Void> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        mallAdminService.deleteBanner(id);
        return Result.ok();
    }

    // ==================== 页面模板（「我的模板」+ 行业模板库） ====================

    @GetMapping("/template/list")
    @Operation(summary = "获取启用的模板列表（本租户「我的模板」；行业库条目 is_library=1 不在本列表，见 /template/library）")
    public Result<List<ShopTemplate>> listTemplates() {
        return Result.ok(mallAdminService.listTemplates());
    }

    @GetMapping("/template/library")
    @Operation(summary = "行业模板库列表（对标实测 14 行业；⚠️ 各行业模板的内部布局内容未实测，库条目 configJson 一律为空）")
    public Result<List<ShopTemplate>> listTemplateLibrary() {
        return Result.ok(mallAdminService.listTemplateLibrary());
    }

    @PostMapping("/template")
    @Operation(summary = "新增「我的模板」（对标「装修模板 → 我的模板 → 新增模板」；templateName 必填，"
            + "templateCode 为空时按 DECO_<时间戳> 自动生成；configJson 为装修结构 JSON 文本，"
            + "⚠️ 拖拽排版结构未实测，后端按不透明文本存取）")
    public Result<ShopTemplate> createTemplate(@RequestBody ShopTemplate template) {
        return Result.ok(mallAdminService.createTemplate(template));
    }

    @PostMapping("/template/library/{id}/reference")
    @Operation(summary = "引用行业库模板（复制库条目为本租户「我的模板」，is_library=0；"
            + "⚠️ 库条目内容为空，引用结果同样为空 —— 如实留缺口，不臆造模板内容）")
    public Result<ShopTemplate> referenceLibraryTemplate(
            @Parameter(description = "行业库模板 id（shop_template.is_library=1）") @PathVariable Long id) {
        return Result.ok(mallAdminService.referenceLibraryTemplate(id));
    }

    // ==================== 装修配置（排版布局存储） ====================

    @GetMapping("/decoration")
    @Operation(summary = "查询装修配置列表（可按 scope 过滤：HOME 首页 / CATEGORY 分类页 / PRODUCT_DETAIL 商品详情）")
    public Result<List<ShopDecoration>> listDecorations(
            @Parameter(description = "装修范围 HOME/CATEGORY/PRODUCT_DETAIL，为空则全部")
            @RequestParam(required = false) String scope) {
        return Result.ok(mallAdminService.listDecorations(scope));
    }

    @GetMapping("/decoration/{id}")
    @Operation(summary = "获取装修配置详情")
    public Result<ShopDecoration> getDecoration(@Parameter(description = "装修配置 id") @PathVariable Long id) {
        return Result.ok(mallAdminService.getDecoration(id));
    }

    @PostMapping("/decoration")
    @Operation(summary = "新增装修配置（name 必填；scope 为空默认 HOME；configJson 为装修结构 JSON 文本）")
    public Result<ShopDecoration> createDecoration(@RequestBody ShopDecoration decoration) {
        return Result.ok(mallAdminService.createDecoration(decoration));
    }

    @PutMapping("/decoration/{id}")
    @Operation(summary = "更新装修配置（部分更新：仅覆盖请求体中非 null 字段）")
    public Result<ShopDecoration> updateDecoration(
            @Parameter(description = "装修配置 id") @PathVariable Long id,
            @RequestBody ShopDecoration decoration) {
        return Result.ok(mallAdminService.updateDecoration(id, decoration));
    }

    @DeleteMapping("/decoration/{id}")
    @Operation(summary = "逻辑删除装修配置（及其应用商品关联，同一事务）")
    public Result<Void> deleteDecoration(@Parameter(description = "装修配置 id") @PathVariable Long id) {
        mallAdminService.deleteDecoration(id);
        return Result.ok();
    }

    @GetMapping("/decoration/{id}/products")
    @Operation(summary = "查询装修配置已关联的商品 id 集合（对标商品详情「设置应用商品」）")
    public Result<List<Long>> listDecorationProducts(
            @Parameter(description = "装修配置 id") @PathVariable Long id) {
        return Result.ok(mallAdminService.listDecorationProducts(id));
    }

    @PutMapping("/decoration/{id}/products")
    @Operation(summary = "全量替换装修配置的关联商品集合（先逻辑删旧、再插新，同一事务；"
            + "⚠️ 对标「设置应用商品」交互未实测，本端点为「能承载前端多选集合」的本实现口径）")
    public Result<List<Long>> replaceDecorationProducts(
            @Parameter(description = "装修配置 id") @PathVariable Long id,
            @RequestBody(required = false) List<Long> productIds) {
        return Result.ok(mallAdminService.replaceDecorationProducts(id, productIds));
    }

    // ==================== 商品管理 ====================

    @GetMapping("/product/page")
    @Operation(summary = "分页查询商城商品（数据源 v_mall_product 视图，含商品上架页 8 项查询条件）")
    public Result<Page<ErpProductMall>> pageProducts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "筛选条件(商品名称/货号/条码/规格/型号)") @RequestParam(required = false) String keyword,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) String categoryId,
            @Parameter(description = "上架状态 ON_SHELF 上架 / OFF_SHELF(INACTIVE) 下架") @RequestParam(required = false) String status,
            @Parameter(description = "品牌(erp_product.brand)") @RequestParam(required = false) String brand,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "商品标签(erp_product.mall_tags 槽位编码，如 TAG_1)") @RequestParam(required = false) String productTag,
            @Parameter(description = "使用优惠券 yes/1 是，no/0 否") @RequestParam(required = false) String couponUsed,
            @Parameter(description = "商品类型 SINGLE单品/KIT套件/SERVICE服务") @RequestParam(required = false) String productType,
            @Parameter(description = "显示状态 ENABLED 启用 / DISABLED 停用") @RequestParam(required = false) String visibleStatus) {
        return Result.ok(mallAdminService.pageProducts(pageNum, pageSize, keyword, categoryId, status,
                brand, productName, productTag, couponUsed, productType, visibleStatus));
    }

    @PostMapping("/product")
    @Operation(summary = "创建商品（⚠️ 未闭环缺口：仍写已废弃的 mall_product 表，列表读 v_mall_product 视图，"
            + "故新增后列表不反映；正确做法应为从 ERP 商品档案选取既有 erp_product 再上架）")
    public Result<Void> createProduct(@RequestBody MallProduct product) {
        mallAdminService.saveProduct(product);
        return Result.ok();
    }

    @PutMapping("/product/{id}")
    @Operation(summary = "更新商品（写权威表 erp_product：product_name/image_url/retail_price(销售价)/"
            + "wholesale_price(市场价)/mall_category_name/mall_shelf_status(上架状态)/mall_description，"
            + "与列表数据源 v_mall_product 视图同源，保存后列表即反映）")
    public Result<Void> updateProduct(
            @PathVariable Long id,
            @RequestBody MallProduct product) {
        product.setId(id);
        mallAdminService.saveProduct(product);
        return Result.ok();
    }

    @DeleteMapping("/product/{id}")
    @Operation(summary = "删除商品（⚠️ 遗留：仍按已废弃的 mall_product 表定位与软删，未改）")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        mallAdminService.deleteProduct(id);
        return Result.ok();
    }

    // ==================== 订单管理 ====================

    @GetMapping("/order/page")
    @Operation(summary = "分页查询商城订单（管理端；查询条件为空即不过滤）",
            description = "状态口径：erp_sale_order.status 0草稿/1待审批/2已审批/3部分出库/4完成/5交易完成/6已取消。"
                    + "orderStatus 支持商城状态串（逗号分隔，如 PENDING_AUDIT,PENDING_PAYMENT，按 toErpStatus 映射）；"
                    + "status 支持数字单值或逗号分隔多值（商城订单页 Tab1 提交数字）。两者可同时给出，取并集。")
    public Result<Page<ErpSaleOrderMall>> pageOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "综合关键词（单据编号/客户/收货人）") @RequestParam(required = false) String keyword,
            @Parameter(description = "商城状态串（逗号分隔多值，按 toErpStatus 映射；无法识别值忽略）") @RequestParam(required = false) String orderStatus,
            @Parameter(description = "erp_sale_order.status 数字，单值或逗号分隔多值（与 orderStatus 取并集）") @RequestParam(required = false) String status,
            @Parameter(description = "单据编号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "单据日期(起) yyyy-MM-dd[ HH:mm:ss]") @RequestParam(required = false) String startDate,
            @Parameter(description = "单据日期(止) yyyy-MM-dd[ HH:mm:ss]，只传日期时含当日 23:59:59") @RequestParam(required = false) String endDate,
            @Parameter(description = "收货人（模糊）") @RequestParam(required = false) String consignee,
            @Parameter(description = "支付方式（等值 ALIPAY/WECHAT/UNIONPAY/BANK/CASH）") @RequestParam(required = false) String paymentMethod,
            @Parameter(description = "订单来源（等值 2=企业客户商城 3=个人会员商城）") @RequestParam(required = false) Integer orderSource,
            @Parameter(description = "商品名称/货号（反查 erp_sale_order_item 后按单据过滤）") @RequestParam(required = false) String productName) {
        return Result.ok(mallAdminService.pageOrders(pageNum, pageSize, keyword, orderStatus, status,
                orderNo, startDate, endDate, consignee, paymentMethod, orderSource, productName));
    }

    @GetMapping("/order/{id}")
    @Operation(summary = "获取订单详情")
    public Result<ErpSaleOrderMall> getOrderDetail(@PathVariable Long id) {
        return Result.ok(mallAdminService.getOrderDetail(id));
    }

    @PutMapping("/order/{id}/approve")
    @Operation(summary = "审核通过订单")
    public Result<Void> approveOrder(@PathVariable Long id) {
        mallAdminService.approveOrder(id);
        return Result.ok();
    }

    @PutMapping("/order/{id}/reject")
    @Operation(summary = "审核驳回订单")
    public Result<Void> rejectOrder(
            @PathVariable Long id,
            @RequestParam String reason) {
        mallAdminService.rejectOrder(id, reason);
        return Result.ok();
    }

    @GetMapping("/order/stats")
    @Operation(summary = "商城订单统计卡（总订单数/GMV/客单价/退款率，真实聚合口径）")
    public Result<TradeAnalysisDTO.Summary> orderStats() {
        return Result.ok(mallAdminService.getOrderStats());
    }

    @GetMapping("/order/page-detail")
    @Operation(summary = "按明细分页查询商城订单（erp_sale_order_item JOIN erp_sale_order）",
            description = "查询条件与 /order/page 同口径（状态、单据编号、日期范围、收货人、支付方式、订单来源、商品）；"
                    + "明细行已透出 departmentName（erp_sale_order.dept_name）。")
    public Result<Page<MallOrderItemPageDTO>> pageOrderDetail(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "综合关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "商城状态串（逗号分隔多值）") @RequestParam(required = false) String orderStatus,
            @Parameter(description = "erp_sale_order.status 数字，单值或逗号分隔多值") @RequestParam(required = false) String status,
            @Parameter(description = "单据编号（模糊）") @RequestParam(required = false) String orderNo,
            @Parameter(description = "单据日期(起)") @RequestParam(required = false) String startDate,
            @Parameter(description = "单据日期(止)，只传日期时含当日") @RequestParam(required = false) String endDate,
            @Parameter(description = "收货人（模糊）") @RequestParam(required = false) String consignee,
            @Parameter(description = "支付方式（等值）") @RequestParam(required = false) String paymentMethod,
            @Parameter(description = "订单来源（等值 2/3）") @RequestParam(required = false) Integer orderSource,
            @Parameter(description = "商品名称/货号（明细行 LIKE）") @RequestParam(required = false) String productName) {
        return Result.ok(mallAdminService.pageOrderItems(pageNum, pageSize, keyword, orderStatus, status,
                orderNo, startDate, endDate, consignee, paymentMethod, orderSource, productName));
    }

    @PostMapping("/order/{id}/pay")
    @Operation(summary = "订单收款（记收款金额并置为已支付）")
    public Result<Void> payOrder(@PathVariable Long id) {
        mallAdminService.receiveOrder(id);
        return Result.ok();
    }

    @PostMapping("/order/{id}/receive")
    @Operation(summary = "订单收款（订单处理页行级收款，与 /pay 同实现）")
    public Result<Void> receiveOrder(@PathVariable Long id) {
        mallAdminService.receiveOrder(id);
        return Result.ok();
    }

    @PostMapping("/order/{id}/ship")
    @Operation(summary = "订单发货（物流公司/运单号落 logistics_company/waybill_no，发货状态置已发货）")
    public Result<Void> shipOrder(
            @PathVariable Long id,
            @RequestBody(required = false) MallOrderShipRequest request) {
        mallAdminService.shipOrder(id, request);
        return Result.ok();
    }

    @PostMapping("/order/{id}/refund")
    @Operation(summary = "订单退款（支付状态置为已退款）")
    public Result<Void> refundOrder(
            @PathVariable Long id,
            @RequestBody(required = false) MallOrderRefundRequest request) {
        mallAdminService.refundOrder(id, request);
        return Result.ok();
    }

    @PostMapping("/order/{id}/terminate")
    @Operation(summary = "强制终止订单（状态置为已取消）")
    public Result<Void> terminateOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        mallAdminService.terminateOrder(id, body != null ? (String) body.get("reason") : null);
        return Result.ok();
    }

    @PutMapping("/order/batch-approve")
    @Operation(summary = "批量审核通过订单（返回成功条数）")
    public Result<Integer> batchApproveOrders(@RequestBody List<Long> ids) {
        return Result.ok(mallAdminService.batchApproveOrders(ids));
    }

    @PutMapping("/order/batch-ship")
    @Operation(summary = "批量发货订单（返回成功条数）")
    public Result<Integer> batchShipOrders(@RequestBody List<Long> ids) {
        return Result.ok(mallAdminService.batchShipOrders(ids));
    }
}
