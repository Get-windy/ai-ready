package cn.aiedge.erp.sale.print;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.printing.support.GenericPrintDataProvider;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnItemMapper;
import cn.aiedge.erp.sale.salereturn.mapper.SaleReturnMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 销售侧「一行登记一个页面」的打印装配器（那些需要查表补名称、字段翻译的，
 * 仍然是手写类，见同包的 {@code SaleOrderPrintDataProvider} 等）。
 *
 * <p>⚠️ pageCode 必须与前端 {@code <PrintDialog page-code="…">} 一字不差，也不能含 `/`。</p>
 */
@Configuration
public class SalePrintProvidersConfig {

    /**
     * 退货申请单（pageCode = sale-return-apply）。
     *
     * <p>与「销售退货单」（{@code sale-return-doc} / {@code SaleReturnDoc}）**不是同一张单**：
     * 这是客户提的退货申请，那是审核通过后的退货单，实体/表都不同，pageCode 也分开。</p>
     */
    @Bean
    PrintDataProvider saleReturnApplyPrintProvider(SaleReturnMapper docMapper, SaleReturnItemMapper itemMapper,
                                                   ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("sale-return-apply", docMapper, itemMapper, "return_id", objectMapper);
    }
}
