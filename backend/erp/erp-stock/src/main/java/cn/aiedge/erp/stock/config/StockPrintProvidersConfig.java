package cn.aiedge.erp.stock.config;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.printing.support.GenericPrintDataProvider;
import cn.aiedge.erp.stock.mapper.StockAssembleItemMapper;
import cn.aiedge.erp.stock.mapper.StockBomItemMapper;
import cn.aiedge.erp.stock.mapper.StockBomMapper;
import cn.aiedge.erp.stock.mapper.StockAssembleMapper;
import cn.aiedge.erp.stock.mapper.StockCostAdjustItemMapper;
import cn.aiedge.erp.stock.mapper.StockCostAdjustMapper;
import cn.aiedge.erp.stock.mapper.StockDamageItemMapper;
import cn.aiedge.erp.stock.mapper.StockDamageMapper;
import cn.aiedge.erp.stock.mapper.StockInItemMapper;
import cn.aiedge.erp.stock.mapper.StockInMapper;
import cn.aiedge.erp.stock.mapper.StockOverflowItemMapper;
import cn.aiedge.erp.stock.mapper.StockOverflowMapper;
import cn.aiedge.erp.stock.mapper.StockSplitItemMapper;
import cn.aiedge.erp.stock.mapper.StockSplitMapper;
import cn.aiedge.erp.stock.mapper.StockTakeItemMapper;
import cn.aiedge.erp.stock.mapper.StockTakeMapper;
import cn.aiedge.erp.stock.mapper.StockTransferItemMapper;
import cn.aiedge.erp.stock.mapper.StockTransferMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 库存单据的打印装配器登记表（pageCode → 主表 mapper + 明细 mapper + 外键列）。
 *
 * <p>这些单据的装配就是「selectById 主表 + 按外键 selectList 明细」，字段名本来就是实体属性名，
 * 所以一律用 {@link GenericPrintDataProvider}，不手写 —— 手写唯一的价值是把字段清单再抄一遍。</p>
 *
 * <p>⚠️ 这里的 pageCode 必须与前端 {@code <PrintDialog page-code="…">} 一字不差
 * （前端在 `views/erp/stock/*\/form.vue`），否则页面取不到模板；也不能含 `/`，它是路径段。
 * 加新单据只要在这个类里加一行。</p>
 */
@Configuration
public class StockPrintProvidersConfig {

    @Bean
    PrintDataProvider stockInPrintProvider(StockInMapper docMapper, StockInItemMapper itemMapper,
                                           ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-in", docMapper, itemMapper, "stock_in_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockTransferPrintProvider(StockTransferMapper docMapper, StockTransferItemMapper itemMapper,
                                                 ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-transfer", docMapper, itemMapper, "transfer_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockSplitPrintProvider(StockSplitMapper docMapper, StockSplitItemMapper itemMapper,
                                              ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-split", docMapper, itemMapper, "split_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockAssemblePrintProvider(StockAssembleMapper docMapper, StockAssembleItemMapper itemMapper,
                                                 ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-assemble", docMapper, itemMapper, "assemble_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockOverflowPrintProvider(StockOverflowMapper docMapper, StockOverflowItemMapper itemMapper,
                                                 ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-overflow", docMapper, itemMapper, "overflow_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockCostAdjustPrintProvider(StockCostAdjustMapper docMapper, StockCostAdjustItemMapper itemMapper,
                                                   ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-cost-adjust", docMapper, itemMapper, "adjust_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockTakePrintProvider(StockTakeMapper docMapper, StockTakeItemMapper itemMapper,
                                             ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-take", docMapper, itemMapper, "stock_take_id", objectMapper);
    }

    @Bean
    PrintDataProvider stockDamagePrintProvider(StockDamageMapper docMapper, StockDamageItemMapper itemMapper,
                                               ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-damage", docMapper, itemMapper, "damage_id", objectMapper);
    }

    /** BOM 清单（配方单）：主表 + 用料明细 */
    @Bean
    PrintDataProvider stockBomPrintProvider(StockBomMapper docMapper, StockBomItemMapper itemMapper,
                                            ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("stock-bom", docMapper, itemMapper, "bom_id", objectMapper);
    }
}
