package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.PromoCustomerRow;
import cn.aiedge.erp.marketing.dto.PromoProductRow;
import cn.aiedge.erp.marketing.entity.PromoActivity;
import cn.aiedge.erp.marketing.mapper.PromoActivityMapper;
import cn.aiedge.erp.marketing.mapper.PromoRefQueryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 促销活动（营销 → 营销活动 → 商品促销 80312 / 整单促销 80313 / 特价 80314）
 * 三页共用本端点，以 {@code activityType}（PRODUCT / ORDER / SPECIAL_PRICE）区分。
 */
@Slf4j
@Tag(name = "促销活动")
@RestController
@RequestMapping("/api/erp/marketing/promotion-activity")
@RequiredArgsConstructor
public class PromoActivityController {

    private final PromoActivityMapper mapper;
    private final PromoRefQueryMapper refQueryMapper;
    private final JdbcTemplate jdbcTemplate;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    @Operation(summary = "分页查询促销活动（activityType 省略＝全部促销方式，供「我要推广-促销」使用）")
    @SaCheckPermission("marketing:promotion-activity:list")
    @GetMapping("/page")
    public Result<IPage<PromoActivity>> page(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<PromoActivity> w = new LambdaQueryWrapper<>();
        if (activityType != null && !activityType.isEmpty()) w.eq(PromoActivity::getType, activityType);
        if (name != null && !name.isEmpty()) w.like(PromoActivity::getName, name);
        if (status != null && !status.isEmpty()) w.eq(PromoActivity::getStatus, status);
        w.orderByDesc(PromoActivity::getCreateTime).orderByDesc(PromoActivity::getId);
        return Result.ok(mapper.selectPage(new Page<>(pageNum, pageSize), w));
    }

    @Operation(summary = "促销活动详情")
    @SaCheckPermission("marketing:promotion-activity:detail")
    @GetMapping("/{id}")
    public Result<PromoActivity> getById(@PathVariable Long id) {
        PromoActivity a = mapper.selectById(id);
        if (a == null) return Result.fail("活动不存在");
        return Result.ok(a);
    }

    @Operation(summary = "新增促销活动")
    @SaCheckPermission("marketing:promotion-activity:create")
    @PostMapping
    public Result<Long> create(@RequestBody PromoActivity req) {
        validate(req);
        req.setId(null);
        req.setTenantId(tenantId());
        if (req.getStatus() == null) req.setStatus("published");
        if (req.getCreatorName() == null) req.setCreatorName(SecurityUtils.getCurrentUsername());
        req.setCreateBy(SecurityUtils.getCurrentUserId());
        req.setCreateTime(LocalDateTime.now());
        req.setUpdateTime(LocalDateTime.now());
        mapper.insert(req);
        return Result.ok(req.getId());
    }

    @Operation(summary = "修改促销活动")
    @SaCheckPermission("marketing:promotion-activity:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PromoActivity req) {
        validate(req);
        req.setId(id);
        req.setTenantId(null);
        req.setUpdateBy(SecurityUtils.getCurrentUserId());
        req.setUpdateTime(LocalDateTime.now());
        return Result.ok(mapper.updateById(req) > 0);
    }

    @Operation(summary = "删除促销活动")
    @SaCheckPermission("marketing:promotion-activity:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(mapper.deleteById(id) > 0);
    }

    @Operation(summary = "启用/停用促销活动")
    @SaCheckPermission("marketing:promotion-activity:create")
    @PostMapping("/{id}/status")
    public Result<Boolean> changeStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(mapper.update(null, new LambdaUpdateWrapper<PromoActivity>()
                .eq(PromoActivity::getId, id)
                .set(PromoActivity::getStatus, status)
                .set(PromoActivity::getUpdateTime, LocalDateTime.now())) > 0);
    }

    @Operation(summary = "查看促销商品")
    @SaCheckPermission("marketing:promotion-activity:view")
    @GetMapping("/{id}/products")
    public Result<List<PromoProductRow>> products(@PathVariable Long id) {
        PromoActivity a = mapper.selectById(id);
        List<Long> ids = parseIds(a == null ? null : a.getProductIds());
        if (ids.isEmpty()) return Result.ok(new ArrayList<>());
        return Result.ok(refQueryMapper.selectPromoProducts(tenantId(), ids));
    }

    @Operation(summary = "查看促销客户")
    @SaCheckPermission("marketing:promotion-activity:view")
    @GetMapping("/{id}/customers")
    public Result<List<PromoCustomerRow>> customers(@PathVariable Long id) {
        PromoActivity a = mapper.selectById(id);
        List<Long> ids = parseIds(a == null ? null : a.getCustomerIds());
        if (ids.isEmpty()) return Result.ok(new ArrayList<>());
        return Result.ok(refQueryMapper.selectPromoCustomers(tenantId(), ids));
    }

    // ── 内部 ──

    /**
     * 促销价合规校验（《明码标价和禁止价格欺诈规定》第 19 条：促销"原价"应为促销前七日内的最低成交价）。
     *
     * <p>本实体不单独存"原价"，只存特价单价 {@code promoPrice}；因此判据取等价形式：
     * 若特价 <b>高于</b>该商品近 7 日最低成交价，说明这次"特价"其实比平时还贵——
     * 「虚构原价 / 虚假折扣」，予以拒绝。无成交记录（新品首次上架）时无从比对，如实放行。</p>
     */
    private void validatePromoPrice(PromoActivity req) {
        if (req.getPromoPrice() == null) return;
        if (!"SPECIAL_PRICE".equals(req.getType())) return;
        List<Long> ids = parseIds(req.getProductIds());
        if (ids.isEmpty()) return;
        for (Long pid : ids) {
            BigDecimal minDeal = minDealPriceLast7Days(pid);
            if (minDeal != null && minDeal.signum() > 0 && req.getPromoPrice().compareTo(minDeal) > 0) {
                throw new IllegalArgumentException(
                        "价格合规校验未通过：该商品近 7 日最低成交价为 " + minDeal.stripTrailingZeros().toPlainString()
                                + " 元，特价（" + req.getPromoPrice().stripTrailingZeros().toPlainString()
                                + " 元）高于它，属虚构原价 / 虚假折扣");
            }
        }
    }

    /** 近 7 日最低成交价（无记录返回 null）。手写 SQL，显式带 tenant_id */
    private BigDecimal minDealPriceLast7Days(Long productId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT MIN(soi.unit_price) AS min_price FROM erp_sale_order_item soi "
                            + "JOIN erp_sale_order so ON so.id = soi.order_id AND so.deleted = 0 "
                            + "WHERE soi.product_id = ? AND so.tenant_id = ? "
                            + "AND so.order_date >= CURRENT_DATE - 7",
                    productId, tenantId());
            if (rows.isEmpty() || rows.get(0).get("min_price") == null) return null;
            return new BigDecimal(String.valueOf(rows.get(0).get("min_price")));
        } catch (Exception e) {
            log.warn("[促销活动] 近 7 日最低成交价查询失败（跳过该校验）：productId={}, {}", productId, e.getMessage());
            return null;
        }
    }

    private void validate(PromoActivity req) {
        if (req.getName() == null || req.getName().isBlank()) throw new IllegalArgumentException("活动名称不能为空");
        validatePromoPrice(req);
        if (req.getType() == null || req.getType().isBlank()) throw new IllegalArgumentException("促销方式不能为空");
        if (req.getStartTime() != null && req.getEndTime() != null && req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("结束时间不得早于起始时间");
        }
    }

    private List<Long> parseIds(String csv) {
        List<Long> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty() && s.matches("\\d+"))
                .forEach(s -> out.add(Long.valueOf(s)));
        return out;
    }
}
