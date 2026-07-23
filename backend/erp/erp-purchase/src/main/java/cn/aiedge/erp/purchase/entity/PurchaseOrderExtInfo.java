package cn.aiedge.erp.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单扩展信息 (1:1)
 * 对标 SaleOrderExtInfo
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order_ext_info")
public class PurchaseOrderExtInfo {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 采购订单ID */
    private Long orderId;

    /** 摘要 */
    private String summary;

    /** 附件(JSON) */
    private String attachment;

    /** 表头自定义1(数字) */
    private BigDecimal extNum1;

    /** 表头自定义2(数字) */
    private BigDecimal extNum2;

    /** 表头自定义3(文本) */
    private String extText1;

    /** 表头自定义4(文本) */
    private String extText2;

    /** 表头自定义5(文本) */
    private String extText3;

    /** 打印次数 */
    private Integer printCount;

    /** 扩展JSON */
    private String extJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
