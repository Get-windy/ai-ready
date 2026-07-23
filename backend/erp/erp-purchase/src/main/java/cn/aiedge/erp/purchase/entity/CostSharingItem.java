package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 采购费用分摊明细
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_cost_sharing_item")
public class CostSharingItem {

    /** 明细ID（主键） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分摊单ID */
    private Long costSharingId;

    /** 入库单ID */
    private Long inboundOrderId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 数量 */
    private BigDecimal quantity;

    /** 金额 */
    private BigDecimal amount;

    /** 重量 */
    private BigDecimal weight;

    /** 体积 */
    private BigDecimal volume;

    /** 分摊费用金额 */
    private BigDecimal allocatedCost;
}
