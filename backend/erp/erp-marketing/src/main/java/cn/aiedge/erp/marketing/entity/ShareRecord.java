package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推广分享触达台账（营销 → 营销推广 → 我要推广 / 推广历史查询）
 * 一行 = 一次分享及其触达效果（浏览/领取/下单）。
 */
@Data
@Accessors(chain = true)
@TableName("mkt_share_record")
public class ShareRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 分享类型：PRODUCT 商品 / COUPON 优惠券 / PROMOTION 促销 / GROUP_BUY 拼团 / FLASH_SALE 秒杀 */
    private String shareType;
    private Long targetId;
    /** 分享概要（如「商品 博多果酱」） */
    private String shareSummary;
    /** 关联对象名称 */
    private String targetName;

    private Long sharerId;
    /** 分享人 */
    private String sharerName;
    /** 分享时间 */
    private LocalDateTime shareTime;

    /** 浏览次数 */
    private Integer viewCount;
    /** 浏览人数 */
    private Integer viewerCount;
    /** 分享领取数 */
    private Integer receiveCount;
    /** 下单人数 */
    private Integer orderUserCount;
    /** 下单笔数 */
    private Integer orderCount;
    /** 下单金额 */
    private BigDecimal orderAmount;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
