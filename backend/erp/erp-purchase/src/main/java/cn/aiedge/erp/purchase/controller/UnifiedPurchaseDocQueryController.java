package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocumentDTO;
import cn.aiedge.erp.purchase.inbound.entity.PurchaseInbound;
import cn.aiedge.erp.purchase.inbound.service.PurchaseInboundService;
import cn.aiedge.erp.purchase.purchaseexchange.entity.PurchaseExchange;
import cn.aiedge.erp.purchase.purchaseexchange.service.PurchaseExchangeService;
import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturn;
import cn.aiedge.erp.purchase.purchasereturn.service.PurchaseReturnService;
import cn.aiedge.erp.purchase.service.UnifiedPurchaseDocQueryService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 采购单据查询控制器（统一）
 * 提供采购入库单/退货单/换货单的合并查询与整单备注更新。
 *
 * <p>区别于 {@link PurchaseDocQueryController}（采购订单-按单据Tab），
 * 本控制器路由为 {@code /api/purchase/doc-query}，服务采购单据查询页。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "采购单据查询(统一)", description = "入库/退货/换货合并分页查询 + 整单备注")
@RestController
@RequestMapping("/api/purchase/doc-query")
@SaCheckLogin
@RequiredArgsConstructor
public class UnifiedPurchaseDocQueryController {

    private final UnifiedPurchaseDocQueryService unifiedPurchaseDocQueryService;
    private final PurchaseInboundService purchaseInboundService;
    private final PurchaseReturnService purchaseReturnService;
    private final PurchaseExchangeService purchaseExchangeService;

    @Operation(summary = "统一采购单据分页查询")
    @GetMapping("/page")
    public Page<UnifiedPurchaseDocumentDTO> unifiedPage(UnifiedPurchaseDocQueryDTO query) {
        return unifiedPurchaseDocQueryService.unifiedPage(query);
    }

    @Operation(summary = "更新单据整单备注")
    @PutMapping("/{docType}/{id}/remark")
    public Map<String, Object> updateRemark(
            @PathVariable String docType,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String remark = body.get("remark");
        boolean updated = false;
        switch (docType) {
            case "INBOUND": {
                PurchaseInbound e = purchaseInboundService.getById(id);
                if (e != null) { e.setRemark(remark); updated = purchaseInboundService.updateById(e); }
                break;
            }
            case "RETURN": {
                PurchaseReturn e = purchaseReturnService.getById(id);
                if (e != null) { e.setRemark(remark); updated = purchaseReturnService.updateById(e); }
                break;
            }
            case "EXCHANGE": {
                PurchaseExchange e = purchaseExchangeService.getById(id);
                if (e != null) { e.setRemark(remark); updated = purchaseExchangeService.updateById(e); }
                break;
            }
            default:
                throw new IllegalArgumentException("不支持的单据类型: " + docType);
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", updated);
        res.put("message", updated ? "更新成功" : "更新失败");
        return res;
    }
}
