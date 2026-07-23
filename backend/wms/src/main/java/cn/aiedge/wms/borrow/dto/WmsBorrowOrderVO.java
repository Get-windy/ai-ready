package cn.aiedge.wms.borrow.dto;

import cn.aiedge.wms.entity.WmsBorrowOrder;
import cn.aiedge.wms.entity.WmsBorrowOrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 借进借出单（含明细）视图对象，用于 create/update/getById.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "借进借出单（含明细）")
public class WmsBorrowOrderVO extends WmsBorrowOrder {

    @Schema(description = "明细列表")
    private List<WmsBorrowOrderItem> items;
}
