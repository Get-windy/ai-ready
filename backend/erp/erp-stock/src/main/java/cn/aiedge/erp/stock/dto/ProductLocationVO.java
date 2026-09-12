package cn.aiedge.erp.stock.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品货位设置行（商品 × 仓库 → 推荐货位）
 * <p>
 * 一行 = 一个商品在所选仓库下的推荐货位状态（未设置时 locationId 为空）。
 * 列口径对齐对标 ql361 GoodsGPositionList 实测列配置（24 列 / 默认 11 列）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "商品货位设置行")
public class ProductLocationVO {

    @Schema(description = "绑定ID（未设置货位时为空）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "商品ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "货号")
    private String productCode;

    @Schema(description = "图片")
    private String imageUrl;

    @Schema(description = "上架状态 1已上架 0未上架")
    private Integer shelfStatus;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "条码")
    private String barcode;

    @Schema(description = "规格")
    private String spec;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "仓库ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "推荐货位ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long locationId;

    @Schema(description = "推荐货位编码")
    private String locationCode;

    @Schema(description = "零售价")
    private BigDecimal retailPrice;

    @Schema(description = "批发价")
    private BigDecimal wholesalePrice;

    @Schema(description = "价格等级1")
    private BigDecimal gradePrice1;

    @Schema(description = "价格等级2")
    private BigDecimal gradePrice2;

    @Schema(description = "价格等级3")
    private BigDecimal gradePrice3;

    @Schema(description = "价格等级4")
    private BigDecimal gradePrice4;

    @Schema(description = "价格等级5")
    private BigDecimal gradePrice5;

    @Schema(description = "价格等级6")
    private BigDecimal gradePrice6;

    @Schema(description = "价格等级7")
    private BigDecimal gradePrice7;

    @Schema(description = "价格等级8")
    private BigDecimal gradePrice8;

    @Schema(description = "备注（商品-货位绑定备注，对标 gpremark）")
    private String remark;

    @Schema(description = "修改时间（绑定时间；未设置时取商品更新时间）")
    private LocalDateTime modifyTime;
}
