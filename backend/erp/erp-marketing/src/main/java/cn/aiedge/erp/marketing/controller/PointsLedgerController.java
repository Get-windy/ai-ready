package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.PointsBatch;
import cn.aiedge.erp.marketing.entity.PointsJournal;
import cn.aiedge.erp.marketing.mapper.PointsJournalMapper;
import cn.aiedge.erp.marketing.service.PointsLedgerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 会员积分台账（批次 / 流水 / 过期）——积分有效期闭环。
 *
 * <p>营销 → 会员中心 → 会员设置（有效期参数）与 会员管理（「积分明细」查看批次）。</p>
 */
@Slf4j
@Tag(name = "会员积分台账")
@RestController
@RequestMapping("/api/erp/marketing/points-ledger")
@RequiredArgsConstructor
public class PointsLedgerController {

    private final PointsLedgerService pointsLedgerService;
    private final PointsJournalMapper pointsJournalMapper;

    @Operation(summary = "查询某会员的积分批次（含到期时间与剩余）")
    @SaCheckPermission("marketing:points-ledger:list")
    @GetMapping("/batch/list")
    public Result<List<PointsBatch>> batches(@RequestParam String memberCardNo) {
        return Result.ok(pointsLedgerService.listBatches(memberCardNo));
    }

    @Operation(summary = "查询某会员当前可用积分（各有效批次剩余之和）")
    @SaCheckPermission("marketing:points-ledger:view")
    @GetMapping("/available")
    public Result<BigDecimal> available(@RequestParam String memberCardNo) {
        return Result.ok(pointsLedgerService.available(memberCardNo));
    }

    @Operation(summary = "分页查询积分变动流水（EARN/USE/EXPIRE/ADJUST）")
    @SaCheckPermission("marketing:points-ledger:list")
    @GetMapping("/journal/page")
    public Result<IPage<PointsJournal>> journalPage(
            @RequestParam(required = false) String memberCardNo,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<PointsJournal> w = new LambdaQueryWrapper<>();
        if (memberCardNo != null && !memberCardNo.isEmpty()) {
            w.eq(PointsJournal::getMemberCardNo, memberCardNo);
        }
        if (changeType != null && !changeType.isEmpty()) w.eq(PointsJournal::getChangeType, changeType);
        w.orderByDesc(PointsJournal::getCreateTime);
        return Result.ok(pointsJournalMapper.selectPage(new Page<>(pageNum, pageSize), w));
    }

    @Operation(summary = "手工记一笔积分获得（写入批次，按配置的有效期计算到期）")
    @SaCheckPermission("marketing:points-ledger:create")
    @PostMapping("/earn")
    public Result<Long> earn(@RequestBody EarnRequest req) {
        if (req.getMemberCardNo() == null || req.getMemberCardNo().isBlank()) {
            throw new IllegalArgumentException("会员卡号不能为空");
        }
        if (req.getPoints() == null || req.getPoints().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("积分必须为正数");
        }
        return Result.ok(pointsLedgerService.earn(req.getMemberCardNo(), req.getPartnerId(),
                req.getPoints(), req.getSource() == null ? "ADJUST" : req.getSource(), req.getSourceBillNo()));
    }

    @Operation(summary = "手工扣减积分（FIFO 扣减各批次剩余）")
    @SaCheckPermission("marketing:points-ledger:create")
    @PostMapping("/use")
    public Result<BigDecimal> use(@RequestBody EarnRequest req) {
        return Result.ok(pointsLedgerService.use(req.getMemberCardNo(), req.getPoints(), req.getSourceBillNo()));
    }

    @Operation(summary = "执行积分过期处理（把已到期批次的剩余清零并写 EXPIRE 流水）")
    @SaCheckPermission("marketing:points-ledger:create")
    @PostMapping("/expire")
    public Result<Map<String, Object>> expire(@RequestParam(required = false) String asOf) {
        Map<String, BigDecimal> detail = pointsLedgerService.expireDue(
                asOf == null || asOf.isBlank() ? LocalDate.now() : LocalDate.parse(asOf));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("memberCount", detail.size());
        out.put("expiredPoints", detail.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        out.put("detail", detail);
        return Result.ok(out);
    }

    @Operation(summary = "近 N 天内到期且仍有剩余的批次（到期提醒 / 触达数据源）")
    @SaCheckPermission("marketing:points-ledger:view")
    @GetMapping("/expiring-soon")
    public Result<List<PointsBatch>> expiringSoon(@RequestParam(defaultValue = "30") Integer days) {
        return Result.ok(pointsLedgerService.expiringSoon(days));
    }

    @Data
    public static class EarnRequest {
        private String memberCardNo;
        private Long partnerId;
        private BigDecimal points;
        private String source;
        private String sourceBillNo;
    }
}
