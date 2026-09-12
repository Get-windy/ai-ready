package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 商品推荐货位（商品 × 仓库 → 货位绑定）
 * <p>
 * 对标 ql361「商品货位设置」（GoodsGPositionList）：按商品维护其在指定仓库下的推荐货位。
 * 货位主数据为全局唯一口径 wms_location，本表只存绑定关系，不复制货位属性。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_location")
public class ProductLocation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 仓库ID（erp_warehouse.id，与 wms_location.warehouse_id 同源） */
    private Long warehouseId;

    /** 商品ID（erp_product.id） */
    private Long productId;

    /** 货位ID（wms_location.id） */
    private Long locationId;

    /** 货位编码快照（列表「推荐货位」列） */
    private String locationCode;

    /** 备注（对标 gpremark） */
    private String remark;

    @TableLogic
    private Integer deleted;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
