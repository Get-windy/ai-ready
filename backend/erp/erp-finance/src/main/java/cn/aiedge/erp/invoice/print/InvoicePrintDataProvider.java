package cn.aiedge.erp.invoice.print;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.repository.InvoiceRepository;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发票打印装配器 —— **手写**，因为发票是 JPA（{@code @Table(name="invoice")} + {@code JpaRepository}），
 * 通用装配器走的是 MyBatis-Plus 的 BaseMapper，两套持久化栈不通。
 *
 * <p>单表单据：{@code invoice_item} 在本库里没有对应的仓储（发票明细由申请单带出、
 * 库里目前也没有数据），所以只摊主表 —— 要打明细表时再补一个带明细的版本。</p>
 */
@Component
@RequiredArgsConstructor
public class InvoicePrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="finance-invoice">} 一致；不能含 `/` */
    public static final String PAGE_CODE = "finance-invoice";

    private final InvoiceRepository invoiceRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        Invoice invoice = invoiceRepository.findById(documentId).orElse(null);
        if (invoice == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>(objectMapper.convertValue(
                invoice, new TypeReference<Map<String, Object>>() {
                }));
        // 与其它装配器保持一致：没有明细也给空列表，模板可以统一按 items 写
        data.put("items", java.util.List.of());
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        return invoiceRepository.findById(documentId).map(Invoice::getInvoiceNumber).orElse(null);
    }
}
