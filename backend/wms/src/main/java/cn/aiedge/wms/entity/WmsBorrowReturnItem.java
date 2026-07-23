package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_borrow_return_item")
@Schema(description = "借进借出归还记录明细")
public class WmsBorrowReturnItem extends BaseEntity {
    @Schema(description = "关联归还记录ID")
    private Long returnId;
    @Schema(description = "关联借进借出单明细ID")
    private Long orderItemId;
    @Schema(description = "商品ID")
    private Long productId;
    @Schema(description = "商品编码")
    private String productCode;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "本次归还数量")
    private BigDecimal quantity;
}
