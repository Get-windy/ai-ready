package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 发货查询（dispatch → 发货查询）打印数据装配器（pageCode = ship-query）。
 *
 * <p><b>它打的就是销售出库单本身</b>：该页的行取自 {@code outboundApi}，
 * 打开打印时用的也是 `record.id`（出库单主键），所以这里**不重复实现装配逻辑**，
 * 直接委托 {@link SaleOutboundPrintDataProvider}。</p>
 *
 * <p>单独占一个 pageCode 的理由与采购收货一致：入口不同、模板要能各自演进
 * （发货查询联常想看物流与收货信息），而 pageCode 同时是模板与打印链的键。</p>
 */
@Component
@RequiredArgsConstructor
public class ShipQueryPrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="ship-query">} 一致 */
    public static final String PAGE_CODE = "ship-query";

    private final SaleOutboundPrintDataProvider saleOutboundProvider;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        return saleOutboundProvider.load(documentId);
    }

    @Override
    public String documentNo(Long documentId) {
        return saleOutboundProvider.documentNo(documentId);
    }
}
