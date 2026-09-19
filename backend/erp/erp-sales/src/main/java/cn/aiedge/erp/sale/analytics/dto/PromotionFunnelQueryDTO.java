package cn.aiedge.erp.sale.analytics.dto;

import lombok.Data;

/**
 * 推广分析（分析 → 采销分析 → 销售分析 → 推广分析）职员分享漏斗查询参数
 */
@Data
public class PromotionFunnelQueryDTO {

    /** 页码（从 1 起） */
    private Integer page = 1;

    /** 每页行数 */
    private Integer size = 20;

    /** 起始日期 yyyy-MM-dd（含，按分享时间 share_time） */
    private String startDate;

    /** 结束日期 yyyy-MM-dd（含） */
    private String endDate;

    /** 职员名称模糊（分享人） */
    private String staffName;

    /** 分享类型：PRODUCT 商品 / COUPON 优惠券 / PROMOTION 促销 / GROUP_BUY 拼团 / FLASH_SALE 秒杀 */
    private String shareType;
}
