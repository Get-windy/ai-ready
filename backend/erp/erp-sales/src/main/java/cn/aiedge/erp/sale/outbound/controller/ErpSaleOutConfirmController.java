package cn.aiedge.erp.sale.outbound.controller;

import cn.aiedge.erp.sale.outbound.entity.SaleOutbound;
import cn.aiedge.erp.sale.outbound.service.SaleOutboundService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WMS 发货确认回调控制器。
 * <p>WMS 发货确认（confirmShip）完成库存扣减后，HTTP 调用本接口生成/推进销售出库单。
 * 注意：库存扣减已在 WMS 侧用 {@code InventoryService.decrease} 完成（唯一扣减点），
 * 本接口只负责创建/定位出库单并推进为已发货，不再扣减，避免双扣链路。</p>
 */
@RestController
@RequestMapping("/api/erp/sale-out")
public class ErpSaleOutConfirmController {

    private final SaleOutboundService saleOutboundService;

    public ErpSaleOutConfirmController(SaleOutboundService saleOutboundService) {
        this.saleOutboundService = saleOutboundService;
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirmFromWms(@RequestBody Map<String, Object> body) {
        Object so = body.get("saleOrderId");
        if (so == null) {
            throw new IllegalArgumentException("缺少 saleOrderId");
        }
        Long saleOrderId = so instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(so));
        SaleOutbound outbound = saleOutboundService.confirmFromWms(saleOrderId);
        return Map.of(
                "success", true,
                "outboundId", outbound.getId(),
                "outboundNo", outbound.getOutboundNo() == null ? "" : outbound.getOutboundNo()
        );
    }
}
