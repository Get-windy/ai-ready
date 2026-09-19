package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预售活动
 */
@Data
@Accessors(chain = true)
@TableName("mkt_presale")
public class Presale {

    /** 状态: 0=未开始 1=进行中 2=已结束 3=已取消 */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_FINISHED = 2;
    public static final int STATUS_CANCELLED = 3;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 活动名称 */
    private String activityName;

    /** 商品信息(冗余快照) */
    private Long productId;
    private String productName;
    private String productCode;
    /** 商品图片（对标「商品预售」Tab「商品图片」列） */
    private String productImage;
    /** 规格 */
    private String productSpec;
    /** 型号 */
    private String productModel;
    /** 预售价（对标「商品预售」Tab「预售价」列） */
    private BigDecimal presalePrice;
    /** 是否支付订金：1 是 / 0 否 */
    private Integer depositRequired;
    /** 创建人姓名（对标「商品预售」Tab「创建人」列，写入时快照） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String creatorName;

    /** 定金 */
    private BigDecimal depositAmount;
    /** 尾款 */
    private BigDecimal finalAmount;

    /** 预售开始时间 */
    private LocalDateTime startTime;
    /** 预售结束时间 */
    private LocalDateTime endTime;
    /** 定金支付截止时间 */
    private LocalDateTime depositEndTime;
    /** 尾款支付开始时间 */
    private LocalDateTime finalStartTime;

    /** 预售限量库存 */
    private Integer stockLimit;
    /** 已售数量(由参与记录累计) */
    private Integer soldCount;

    /** 状态: 0=未开始 1=进行中 2=已结束 3=已取消 */
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
