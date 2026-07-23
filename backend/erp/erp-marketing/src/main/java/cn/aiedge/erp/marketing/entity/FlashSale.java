package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀场次
 */
@Data
@Accessors(chain = true)
@TableName("mkt_flash_sale")
public class FlashSale {

    /** 状态: 0=草稿 1=已发布 2=已取消 3=已结束 */
    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_FINISHED = 3;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 场次标题 */
    private String title;

    /** 商品信息(冗余快照) */
    private Long productId;
    private String productCode;
    private String productName;

    /** 秒杀价 */
    private BigDecimal flashPrice;
    /** 原价 */
    private BigDecimal originalPrice;
    /** 秒杀限量库存 */
    private Integer stockLimit;
    /** 已售数量(由参与记录累计) */
    private Integer soldCount;

    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;

    /** 状态: 0=草稿 1=已发布 2=已取消 3=已结束 */
    private Integer status;
    private Integer sort;
    private String remark;

    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
