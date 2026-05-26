package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票明细实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_invoice_item")
public class InvoiceItem {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    /**
     * 发票ID
     */
    private Long invoiceId;
    
    /**
     * 行号
     */
    private Integer lineNo;
    
    /**
     * 商品名称
     */
    private String goodsName;
    
    /**
     * 规格型号
     */
    private String specification;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 数量
     */
    private BigDecimal quantity;
    
    /**
     * 单价（不含税）
     */
    private BigDecimal unitPrice;
    
    /**
     * 金额（不含税）
     */
    private BigDecimal amount;
    
    /**
     * 税率
     */
    private BigDecimal taxRate;
    
    /**
     * 税额
     */
    private BigDecimal taxAmount;
    
    /**
     * 价税合计
     */
    private BigDecimal totalAmount;
    
    /**
     * 税收分类编码
     */
    private String taxCategoryCode;
    
    /**
     * 是否享受优惠政策：0-否 1-是
     */
    private Integer preferential;
    
    /**
     * 优惠政策内容
     */
    private String preferentialContent;
    
    /**
     * 零税率标志：0-否 1-免税 2-不征税 3-普通零税率
     */
    private Integer zeroTaxFlag;
    
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
     * 删除标志
     */
    @TableLogic
    private Integer deleted;
}
