package cn.aiedge.wms.config;

import cn.aiedge.erp.printing.spi.PrintDataProvider;
import cn.aiedge.erp.printing.support.GenericPrintDataProvider;
import cn.aiedge.quality.mapper.QualityStandardMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowOrderItemMapper;
import cn.aiedge.wms.borrow.mapper.WmsBorrowOrderMapper;
import cn.aiedge.wms.move.mapper.WmsMoveDetailMapper;
import cn.aiedge.wms.move.mapper.WmsMoveTaskMapper;
import cn.aiedge.wms.pick.mapper.WmsPickDetailMapper;
import cn.aiedge.wms.pick.mapper.WmsPickTaskMapper;
import cn.aiedge.wms.ship.mapper.WmsShipDetailMapper;
import cn.aiedge.wms.ship.mapper.WmsShipTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仓储（WMS）单据的打印装配器登记表（pageCode → 主表 mapper + 明细 mapper + 外键列）。
 *
 * <p>这些单据都是「selectById 主表 + 按外键 selectList 明细」，字段名本来就是实体属性名，
 * 所以一律用 {@link GenericPrintDataProvider}，一行登记一个页面。</p>
 *
 * <p>⚠️ pageCode 必须与前端 {@code <PrintDialog page-code="…">} 一字不差
 * （前端在 `views/wh/*\/form/index.vue`），也不能含 `/`（它是路径段）。</p>
 */
@Configuration
public class WmsPrintProvidersConfig {

    /**
     * 借入单 / 借出单**共用同一张表**（{@code wms_borrow_order} 的 {@code direction} 区分方向），
     * 但入口不同、模板要能各自演进，所以登记成两个 pageCode。
     */
    @Bean
    PrintDataProvider whBorrowInPrintProvider(WmsBorrowOrderMapper docMapper, WmsBorrowOrderItemMapper itemMapper,
                                              ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("wh-borrow-in", docMapper, itemMapper, "order_id", objectMapper);
    }

    @Bean
    PrintDataProvider whBorrowOutPrintProvider(WmsBorrowOrderMapper docMapper, WmsBorrowOrderItemMapper itemMapper,
                                               ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("wh-borrow-out", docMapper, itemMapper, "order_id", objectMapper);
    }

    @Bean
    PrintDataProvider whMoveOrderPrintProvider(WmsMoveTaskMapper docMapper, WmsMoveDetailMapper itemMapper,
                                               ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("wh-move-order", docMapper, itemMapper, "task_id", objectMapper);
    }

    @Bean
    PrintDataProvider whPickingOrderPrintProvider(WmsPickTaskMapper docMapper, WmsPickDetailMapper itemMapper,
                                                  ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("wh-picking-order", docMapper, itemMapper, "task_id", objectMapper);
    }

    @Bean
    PrintDataProvider whShippingOrderPrintProvider(WmsShipTaskMapper docMapper, WmsShipDetailMapper itemMapper,
                                                   ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("wh-shipping-order", docMapper, itemMapper, "ship_id", objectMapper);
    }

    /**
     * 质量标准：单表主数据（没有明细表）。
     *
     * <p>它的实体与 mapper 在 core-base（{@code cn.aiedge.quality}），所以只能登记在
     * 「同时看得见 core-base 与 erp-printing SPI」的模块里 —— 仓储模块就是这样的模块
     * （质量标准服务于检验，检验在 wms）。</p>
     */
    @Bean
    PrintDataProvider qualityStandardPrintProvider(QualityStandardMapper docMapper, ObjectMapper objectMapper) {
        return new GenericPrintDataProvider("quality-standard", docMapper, null, null, objectMapper);
    }
}
