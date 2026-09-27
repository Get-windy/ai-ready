package cn.aiedge.erp.purchase.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 采购收货（dispatch → 采购收货）打印数据装配器（pageCode = purchase-receive）。
 *
 * <p><b>它打的就是采购订单本身</b>，不是另建的一张收货单：该页的文案是「请先勾选要打印的采购订单」，
 * 打印成功后回写的也是采购订单的打印次数。所以这里**不重复实现装配逻辑**，
 * 直接委托 {@link PurchaseOrderPrintDataProvider}（《功能/模块不重复开发》）。</p>
 *
 * <p>之所以仍要单独一个 pageCode：入口不同、模板可以各自演进（收货联常常要突出"应收数量/已收数量"），
 * 而 pageCode 同时是模板与打印链的键，不能两个入口共用一个。</p>
 */
@Component
@RequiredArgsConstructor
public class PurchaseReceivePrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="purchase-receive">} 一致 */
    public static final String PAGE_CODE = "purchase-receive";

    private final PurchaseOrderPrintDataProvider purchaseOrderProvider;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        return purchaseOrderProvider.load(documentId);
    }

    @Override
    public String documentNo(Long documentId) {
        return purchaseOrderProvider.documentNo(documentId);
    }
}
