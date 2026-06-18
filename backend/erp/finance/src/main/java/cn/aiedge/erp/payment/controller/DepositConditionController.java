package cn.aiedge.erp.payment.controller;

import cn.aiedge.erp.payment.entity.DepositCondition;
import cn.aiedge.erp.payment.service.DepositConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/erp/deposit-condition")
@RequiredArgsConstructor
@Tag(name = "定金/押金条件管理", description = "管理定金/押金的约定条件、违约条款、状态变更")
public class DepositConditionController {

    private final DepositConditionService depositConditionService;

    @GetMapping("/pre-receipt/{preReceiptId}")
    @Operation(summary = "根据预收款查询定金条件")
    public DepositCondition getByPreReceiptId(@PathVariable Long preReceiptId) {
        return depositConditionService.getByPreReceiptId(preReceiptId);
    }

    @GetMapping("/pre-payment/{prePaymentId}")
    @Operation(summary = "根据预付款查询定金条件")
    public DepositCondition getByPrePaymentId(@PathVariable Long prePaymentId) {
        return depositConditionService.getByPrePaymentId(prePaymentId);
    }

    @GetMapping("/source")
    @Operation(summary = "根据来源单据查询定金条件")
    public List<DepositCondition> getBySource(@RequestParam String sourceType, @RequestParam Long sourceId) {
        return depositConditionService.getBySource(sourceType, sourceId);
    }

    @PostMapping
    @Operation(summary = "创建定金条件")
    public DepositCondition create(@RequestBody DepositCondition condition) {
        return depositConditionService.createCondition(condition);
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "转正（条件达成）")
    public DepositCondition convert(@PathVariable Long id) {
        return depositConditionService.convert(id);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "退还（到期退还）")
    public DepositCondition refund(@PathVariable Long id, @RequestParam String reason) {
        return depositConditionService.refund(id, reason);
    }

    @PostMapping("/{id}/forfeit")
    @Operation(summary = "没收（对方违约）")
    public DepositCondition forfeit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal forfeitAmount = body.get("forfeitAmount") != null
                ? new BigDecimal(body.get("forfeitAmount").toString())
                : BigDecimal.ZERO;
        String reason = (String) body.getOrDefault("reason", "");
        return depositConditionService.forfeit(id, forfeitAmount, reason);
    }

    @PostMapping("/{id}/deduct")
    @Operation(summary = "扣款（损坏扣款）")
    public DepositCondition deduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal deductAmount = body.get("deductAmount") != null
                ? new BigDecimal(body.get("deductAmount").toString())
                : BigDecimal.ZERO;
        String reason = (String) body.getOrDefault("reason", "");
        return depositConditionService.deduct(id, deductAmount, reason);
    }
}
