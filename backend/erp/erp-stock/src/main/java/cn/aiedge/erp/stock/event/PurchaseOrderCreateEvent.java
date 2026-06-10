package cn.aiedge.erp.stock.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 采购订单创建事件
 * <p>
 * 由库存模块的补货建议触发，采购模块监听后创建实际的采购订单。
 * 当前为模块间解耦的事件集成点，采购模块尚未实现事件监听器。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public class PurchaseOrderCreateEvent extends ApplicationEvent {

    /**
     * 补货建议ID
     */
    private final Long suggestionId;

    /**
     * 供应商ID
     */
    private final Long supplierId;

    /**
     * 产品编码
     */
    private final String productCode;

    /**
     * 产品名称
     */
    private final String productName;

    /**
     * 建议采购数量
     */
    private final BigDecimal suggestedQty;

    /**
     * 仓库ID
     */
    private final Long warehouseId;

    /**
     * 仓库名称
     */
    private final String warehouseName;

    public PurchaseOrderCreateEvent(Object source, Long suggestionId, Long supplierId,
                                    String productCode, String productName, BigDecimal suggestedQty,
                                    Long warehouseId, String warehouseName) {
        super(source);
        this.suggestionId = suggestionId;
        this.supplierId = supplierId;
        this.productCode = productCode;
        this.productName = productName;
        this.suggestedQty = suggestedQty;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
    }
}
