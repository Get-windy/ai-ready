package cn.aiedge.erp.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品货位设置查询条件
 * <p>
 * 字段口径对齐对标 ql361「商品货位设置」查询区实抓：
 * 筛选条件(商品名称/货号)、品牌、仓库(必填)、货位、条码、上架状态、显示状态(默认已启用)，
 * 以及「仅显示未设置货位的商品」「仅显示有库存的商品」两个复选框。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "商品货位设置查询条件")
public class ProductLocationQuery {

    @Schema(description = "仓库ID（必填，erp_warehouse.id）")
    private Long warehouseId;

    @Schema(description = "商品分类ID（含子分类）")
    private Long categoryId;

    @Schema(description = "商品名称/货号")
    private String keyword;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "货位编码")
    private String locationCode;

    @Schema(description = "条码 -1全部 1无条码 2有条码")
    private Integer hasBarcodeStatus;

    @Schema(description = "上架状态 -1全部 1已上架 0未上架")
    private Integer shelfStatus;

    @Schema(description = "显示状态 -1全部 2已启用 1已停用")
    private Integer showStop;

    @Schema(description = "仅显示未设置货位的商品")
    private Boolean onlyUnsettedGoods;

    @Schema(description = "仅显示有库存的商品")
    private Boolean onlyStockGoods;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 20;
}
