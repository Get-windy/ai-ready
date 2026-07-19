package cn.aiedge.erp.sale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单扩展信息 (1:1)
 * 自定义字段、附件、摘要
 */
@Data
@Accessors(chain = true)
@TableName("erp_sale_order_ext_info")
public class SaleOrderExtInfo {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单ID (1:1) */
    private Long orderId;

    /** 摘要 */
    private String summary;

    /** 附件(JSON) */
    private String attachment;

    // ═══ 自定义字段(表头) ═══
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;

    // ═══ 表尾自定义字段 ═══
    private String footerExtText1;
    private String footerExtText2;

    /** 扩展JSON */
    private String extJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
