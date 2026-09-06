package cn.aiedge.wms.borrow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 借转采购登记请求：把借进商品转为正式采购，更新明细与单据台账（借转采购数量/未处理数量）。
 */
@Data
public class ConvertPurchaseRequest {

    @NotNull(message = "单据ID不能为空")
    private Long orderId;

    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "明细行ID不能为空")
        private Long orderItemId;
        @NotNull(message = "借转采购数量不能为空")
        private BigDecimal quantity;
    }
}
