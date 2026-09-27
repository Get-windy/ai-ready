package cn.aiedge.erp.finance.config;

import cn.aiedge.erp.finance.arapadjust.mapper.ArApAdjustItemMapper;
import cn.aiedge.erp.finance.arapadjust.mapper.ArApAdjustMapper;
import cn.aiedge.erp.finance.cashtransfer.mapper.CashTransferItemMapper;
import cn.aiedge.erp.finance.cashtransfer.mapper.CashTransferMapper;
import cn.aiedge.erp.finance.expensedoc.mapper.ExpenseDocMapper;
import cn.aiedge.erp.payment.mapper.PrePaymentItemMapper;
import cn.aiedge.erp.payment.mapper.PrePaymentMapper;
import cn.aiedge.erp.payment.mapper.PreReceiptItemMapper;
import cn.aiedge.erp.payment.mapper.PreReceiptMapper;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.printing.support.GenericPrintDataProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 财务单据的打印装配器登记表（pageCode → 主表 mapper + 明细 mapper + 外键列）。
 *
 * <p>这些单据都是「selectById 主表 + 按外键 selectList 明细」，字段名本来就是实体属性名，
 * 所以一律用 {@link GenericPrintDataProvider}，一行登记一个页面。</p>
 *
 * <p>⚠️ pageCode 必须与前端 {@code <PrintDialog page-code="…">} 一字不差，也不能含 `/`。</p>
 */
@Configuration
public class FinancePrintProvidersConfig {

    @Bean
    PrintDataProvider financeAdvancePaymentPrintProvider(PrePaymentMapper docMapper, PrePaymentItemMapper itemMapper,
                                                         ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("finance-advance-payment", docMapper, itemMapper, "pre_payment_id", objectMapper);
    }

    @Bean
    PrintDataProvider financeAdvanceReceiptPrintProvider(PreReceiptMapper docMapper, PreReceiptItemMapper itemMapper,
                                                         ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("finance-advance-receipt", docMapper, itemMapper, "pre_receipt_id", objectMapper);
    }

    @Bean
    PrintDataProvider financeArApAdjustPrintProvider(ArApAdjustMapper docMapper, ArApAdjustItemMapper itemMapper,
                                                     ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("finance-ar-ap-adjust", docMapper, itemMapper, "adjust_id", objectMapper);
    }

    @Bean
    PrintDataProvider financeCashTransferPrintProvider(CashTransferMapper docMapper, CashTransferItemMapper itemMapper,
                                                       ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("finance-cash-transfer", docMapper, itemMapper, "transfer_id", objectMapper);
    }

    /** 费用单：单表单据（没有明细子表），只摊主表 */
    @Bean
    PrintDataProvider financeExpenseDocPrintProvider(ExpenseDocMapper docMapper, ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("finance-expense-doc", docMapper, null, null, objectMapper);
    }
}
