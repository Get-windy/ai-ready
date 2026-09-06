package cn.aiedge.erp.stock.event;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 盘点「货位转移差异」事件：盘点处理时，对有目标货位的明细发布，由 wms 监听生成移库单.
 * <p>wms 依赖 erp-stock，可监听本事件；同一 Spring 容器内进程内解耦，避免 HTTP 鉴权问题。</p>
 */
@Data
public class StocktakeMoveEvent {

    /** 盘点单号（作为移库单 sourceNo，sourceType=盘点差异） */
    private String sourceNo;
    /** 仓库 */
    private Long warehouseId;
    private String warehouseName;
    /** 货位转移明细 */
    private List<TransferItem> items;

    @Data
    public static class TransferItem {
        private Long productId;
        private String productCode;
        private String productName;
        private String productSpec;
        private String productUnit;
        /** 移库数量（账面库存） */
        private BigDecimal quantity;
        /** 源货位编码（盘点货位 location） */
        private String fromLocationCode;
        /** 目标货位ID */
        private Long toLocationId;
        /** 目标货位编码 */
        private String toLocationCode;
        private String batchNo;
        private String serialNo;
    }
}
