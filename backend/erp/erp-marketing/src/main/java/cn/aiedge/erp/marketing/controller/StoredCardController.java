package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.StoredCard;
import cn.aiedge.erp.marketing.entity.StoredCardFlow;
import cn.aiedge.erp.marketing.mapper.StoredCardFlowMapper;
import cn.aiedge.erp.marketing.service.StoredCardService;
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
import java.util.List;

/**
 * 储值卡（菜单 80304，本系统建模页）
 * 双视图：卡档案 / 储值流水。
 *
 * <p>⚠️ 合规：本页提供**退款入口**（部分/全额）与合规提示，不得设计为"只进不出"。</p>
 */
@Slf4j
@Tag(name = "储值卡")
@RestController
@RequestMapping("/api/erp/marketing/stored-card")
@RequiredArgsConstructor
public class StoredCardController {

    private final StoredCardService storedCardService;
    private final StoredCardFlowMapper flowMapper;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    // ══════ Tab1 卡档案 ══════

    @Operation(summary = "分页查询储值卡")
    @GetMapping("/page")
    public Result<IPage<StoredCard>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cardType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<StoredCard> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            w.and(x -> x.like(StoredCard::getCardNo, keyword).or().like(StoredCard::getPartnerName, keyword));
        }
        if (cardType != null && !cardType.isEmpty()) w.eq(StoredCard::getCardType, cardType);
        if (status != null && !status.isEmpty()) w.eq(StoredCard::getStatus, status);
        if (partnerId != null) w.eq(StoredCard::getPartnerId, partnerId);
        w.orderByDesc(StoredCard::getIssueTime).orderByDesc(StoredCard::getId);
        return Result.ok(storedCardService.page(new Page<>(pageNum, pageSize), w));
    }

    @Operation(summary = "卡详情")
    @GetMapping("/{id}")
    public Result<StoredCard> getById(@PathVariable Long id) {
        StoredCard card = storedCardService.getById(id);
        if (card == null) return Result.fail("卡不存在");
        return Result.ok(card);
    }

    @Operation(summary = "按卡号查询（收银/开单场景）")
    @GetMapping("/by-no")
    public Result<StoredCard> getByCardNo(@RequestParam String cardNo) {
        return Result.ok(storedCardService.getByCardNo(cardNo));
    }

    @Operation(summary = "查询某会员的全部卡")
    @GetMapping("/by-partner/{partnerId}")
    public Result<List<StoredCard>> byPartner(@PathVariable Long partnerId) {
        return Result.ok(storedCardService.listByPartner(partnerId));
    }

    @Operation(summary = "开卡（面值 + 可选赠送）")
    @PostMapping
    public Result<Long> issue(@RequestBody IssueRequest req) {
        return Result.ok(storedCardService.issue(req.getCard(), req.getBonusAmount(), req.getRemark()));
    }

    @Operation(summary = "充值（金额 + 赠送）")
    @PostMapping("/{id}/recharge")
    public Result<StoredCard> recharge(@PathVariable Long id, @RequestBody AmountRequest req) {
        // settleAccount：CASH 库存现金 / BANK 银行存款 —— 决定凭证的借方科目
        return Result.ok(storedCardService.recharge(id, req.getAmount(), req.getBonusAmount(),
                req.getSourceBillNo(), req.getSettleAccount()));
    }

    @Operation(summary = "消费扣减")
    @PostMapping("/{id}/consume")
    public Result<StoredCard> consume(@PathVariable Long id, @RequestBody AmountRequest req) {
        return Result.ok(storedCardService.consume(id, req.getAmount(), req.getSourceBillNo()));
    }

    @Operation(summary = "退款（预付费合规入口：部分/全额）")
    @PostMapping("/{id}/refund")
    public Result<StoredCard> refund(@PathVariable Long id, @RequestBody AmountRequest req) {
        return Result.ok(storedCardService.refund(id, req.getAmount(), req.getRemark()));
    }

    @Operation(summary = "冻结 / 解冻")
    @PostMapping("/{id}/status")
    public Result<StoredCard> changeStatus(@PathVariable Long id, @RequestParam String status,
                                           @RequestParam(required = false) String remark) {
        return Result.ok(storedCardService.changeStatus(id, status, remark));
    }

    @Operation(summary = "卡统计（在用卡数 / 余额合计 / 累计充值 / 累计消费）")
    @GetMapping("/stat")
    public Result<java.util.Map<String, Object>> stat() {
        Long tid = tenantId();
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        List<StoredCard> all = storedCardService.list(new LambdaQueryWrapper<StoredCard>()
                .eq(StoredCard::getTenantId, tid).eq(StoredCard::getDeleted, 0));
        out.put("cardCount", all.size());
        out.put("activeCount", all.stream().filter(c -> StoredCard.STATUS_ACTIVE.equals(c.getStatus())).count());
        out.put("balanceTotal", all.stream().map(c -> nz(c.getBalance())).reduce(BigDecimal.ZERO, BigDecimal::add));
        out.put("rechargeTotal", all.stream().map(c -> nz(c.getTotalRecharge())).reduce(BigDecimal.ZERO, BigDecimal::add));
        out.put("consumeTotal", all.stream().map(c -> nz(c.getTotalConsume())).reduce(BigDecimal.ZERO, BigDecimal::add));
        out.put("bonusTotal", all.stream().map(c -> nz(c.getTotalBonus())).reduce(BigDecimal.ZERO, BigDecimal::add));
        return Result.ok(out);
    }

    // ══════ Tab2 储值流水 ══════

    @Operation(summary = "分页查询储值流水")
    @GetMapping("/flow/page")
    public Result<IPage<StoredCardFlow>> flowPage(
            @RequestParam(required = false) Long cardId,
            @RequestParam(required = false) String cardNo,
            @RequestParam(required = false) String flowType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<StoredCardFlow> w = new LambdaQueryWrapper<>();
        if (cardId != null) w.eq(StoredCardFlow::getCardId, cardId);
        if (cardNo != null && !cardNo.isEmpty()) w.like(StoredCardFlow::getCardNo, cardNo);
        if (flowType != null && !flowType.isEmpty()) w.eq(StoredCardFlow::getFlowType, flowType);
        w.orderByDesc(StoredCardFlow::getCreateTime);
        return Result.ok(flowMapper.selectPage(new Page<>(pageNum, pageSize), w));
    }

    // ══════ 内部 ══════

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    @Data
    public static class IssueRequest {
        private StoredCard card;
        private BigDecimal bonusAmount;
        private String remark;
    }

    @Data
    public static class AmountRequest {
        private BigDecimal amount;
        private BigDecimal bonusAmount;
        private String sourceBillNo;
        private String remark;
        /** 结算账户：CASH 库存现金 / BANK 银行存款（开卡·充值·退款用，决定凭证借方） */
        private String settleAccount;
    }
}
