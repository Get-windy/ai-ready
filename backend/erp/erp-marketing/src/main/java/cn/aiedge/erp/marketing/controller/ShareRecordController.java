package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.ShareSummaryRow;
import cn.aiedge.erp.marketing.entity.ShareRecord;
import cn.aiedge.erp.marketing.mapper.ShareRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 推广分享（营销 → 营销推广 → 我要推广 80330 / 推广历史查询 80331）
 */
@Slf4j
@Tag(name = "推广分享")
@RestController
@RequestMapping("/api/erp/marketing/share")
@RequiredArgsConstructor
public class ShareRecordController {

    private final ShareRecordMapper mapper;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    @Operation(summary = "分页查询推广历史（全部分享人）")
    @SaCheckPermission("marketing:share:list")
    @GetMapping("/page")
    public Result<IPage<ShareRecord>> page(
            @RequestParam(required = false) String shareType,
            @RequestParam(required = false) String sharer,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(mapper.selectPage(new Page<>(pageNum, pageSize),
                buildWrapper(shareType, sharer, startDate, endDate, null)));
    }

    @Operation(summary = "分页查询我的推广（仅当前登录人）")
    @SaCheckPermission("marketing:share:list")
    @GetMapping("/my/page")
    public Result<IPage<ShareRecord>> myPage(
            @RequestParam(required = false) String shareType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(mapper.selectPage(new Page<>(pageNum, pageSize),
                buildWrapper(shareType, null, startDate, endDate, SecurityUtils.getCurrentUserId())));
    }

    private LambdaQueryWrapper<ShareRecord> buildWrapper(String shareType, String sharer,
                                                         LocalDate startDate, LocalDate endDate, Long sharerId) {
        LambdaQueryWrapper<ShareRecord> w = new LambdaQueryWrapper<>();
        if (shareType != null && !shareType.isEmpty()) w.eq(ShareRecord::getShareType, shareType);
        if (sharer != null && !sharer.isEmpty()) w.like(ShareRecord::getSharerName, sharer);
        if (sharerId != null) w.eq(ShareRecord::getSharerId, sharerId);
        if (startDate != null) w.ge(ShareRecord::getShareTime, startDate.atStartOfDay());
        if (endDate != null) w.le(ShareRecord::getShareTime, endDate.atTime(23, 59, 59));
        w.orderByDesc(ShareRecord::getShareTime).orderByDesc(ShareRecord::getId);
        return w;
    }

    @Operation(summary = "按分享对象聚合的分享统计（我要推广各物料 Tab 的 5 个统计列）")
    @SaCheckPermission("marketing:share:view")
    @GetMapping("/summary")
    public Result<List<ShareSummaryRow>> summary(
            @RequestParam String shareType,
            @RequestParam(required = false) Boolean mine) {
        Long sharerId = Boolean.TRUE.equals(mine) ? SecurityUtils.getCurrentUserId() : null;
        return Result.ok(mapper.selectSummaryByTarget(tenantId(), shareType, sharerId));
    }

    @Operation(summary = "登记一次分享（我要推广 →「分享」按钮）")
    @SaCheckPermission("marketing:share:create")
    @PostMapping
    public Result<Long> create(@RequestBody ShareRecord req) {
        if (req.getShareType() == null || req.getShareType().isBlank()) {
            throw new IllegalArgumentException("分享类型不能为空");
        }
        req.setId(null);
        req.setTenantId(tenantId());
        if (req.getTargetId() == null) throw new IllegalArgumentException("请先选择要推广的对象");
        req.setSharerId(SecurityUtils.getCurrentUserId());
        req.setSharerName(SecurityUtils.getCurrentUsername());
        req.setShareTime(LocalDateTime.now());
        req.setViewCount(nvl(req.getViewCount()));
        req.setViewerCount(nvl(req.getViewerCount()));
        req.setReceiveCount(nvl(req.getReceiveCount()));
        req.setOrderUserCount(nvl(req.getOrderUserCount()));
        req.setOrderCount(nvl(req.getOrderCount()));
        req.setOrderAmount(req.getOrderAmount() == null ? BigDecimal.ZERO : req.getOrderAmount());
        req.setCreateTime(LocalDateTime.now());
        req.setUpdateTime(LocalDateTime.now());
        mapper.insert(req);
        log.info("[推广] 用户 {} 分享了 {} #{}", req.getSharerName(), req.getShareType(), req.getTargetId());
        return Result.ok(req.getId());
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
