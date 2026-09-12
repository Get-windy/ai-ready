package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 云商品库条目（云导入数据源）
 *
 * <p>商品列表「云导入」按钮的候选商品档案，按条目勾选后可批量导入为本地商品。</p>
 */
@Data
@Accessors(chain = true)
@TableName("erp_product_cloud_catalog")
public class CloudProduct {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 租户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tenantId;

    /** 云商品编码 */
    private String cloudCode;

    /** 云商品名称 */
    private String cloudName;

    /** 规格 */
    private String spec;

    /** 型号 */
    private String model;

    /** 产地 */
    private String origin;

    /** 品牌 */
    private String brand;

    /** 单位 */
    private String unit;

    /** 条码 */
    private String barcode;

    /** 分类名称 */
    private String categoryName;

    /** 行业类别 */
    private String industryCategory;

    /** 预设进价 */
    private BigDecimal presetPurchasePrice;

    /** 零售价 */
    private BigDecimal retailPrice;

    /** 批发价 */
    private BigDecimal wholesalePrice;

    /** 保质期天数 */
    private Integer shelfLifeDays;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
