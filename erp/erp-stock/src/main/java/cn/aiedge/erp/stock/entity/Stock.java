package cn.aiedge.erp.stock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_stock")
public class Stock {

    /**
     * 库存ID（主键）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID（多租户支持）
     */
    private Long tenantId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 库存数量
     */
    private BigDecimal quantity;

    /**
     * 可用数量（可用数量 = 总数量 - 冻结数量）
     */
    private BigDecimal availableQuantity;

    /**
     * 冻结数量
     */
    private BigDecimal frozenQuantity;

    /**
     * 安全库存
     */
    private BigDecimal safetyStock;

    /**
     * 最低库存
     */
    private BigDecimal minStock;

    /**
     * 最高库存
     */
    private BigDecimal maxStock;

    /**
     * 库存单位
     */
    private String unit;

    /**
     * 批次号（批次管理）
     */
    private String batchNo;

    /**
     * 生产日期
     */
    private LocalDateTime productionDate;

    /**
     * 有效期至
     */
    private LocalDateTime validityDate;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除（0-未删除 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
