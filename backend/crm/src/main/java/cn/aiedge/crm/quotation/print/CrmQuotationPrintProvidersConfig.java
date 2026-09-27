package cn.aiedge.crm.quotation.print;

import cn.aiedge.crm.quotation.mapper.QuotationItemMapper;
import cn.aiedge.crm.quotation.mapper.QuotationMapper;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.printing.support.GenericPrintDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CRM 侧打印装配器登记表（pageCode → 主表 mapper + 明细 mapper + 外键列）。
 *
 * <p>报价单就是「selectById 主表 + 按 quotation_id selectList 明细」，字段名本来就是实体属性名，
 * 所以用通用装配器一行登记。</p>
 */
@Configuration
public class CrmQuotationPrintProvidersConfig {

    /** 报价单；pageCode 与前端 {@code <PrintDialog page-code="crm-quotation">} 一致 */
    @Bean
    PrintDataProvider crmQuotationPrintProvider(QuotationMapper docMapper, QuotationItemMapper itemMapper,
                                                ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("crm-quotation", docMapper, itemMapper, "quotation_id", objectMapper);
    }
}
