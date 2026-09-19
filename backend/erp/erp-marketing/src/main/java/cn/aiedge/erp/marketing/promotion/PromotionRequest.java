package cn.aiedge.erp.marketing.promotion;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 促销引擎输入：一次结算上下文 */
@Data
public class PromotionRequest {

    private Long tenantId;
    /** 客户（往来单位）id，用于「促销客户」范围过滤与每客户次数判定；为空＝散客 */
    private Long customerId;
    /** 结算日期（活动起止判定用） */
    private LocalDate orderDate;
    /**
     * 结算渠道：OFFLINE 线下开单 / MALL 商城。
     * 与活动的「使用范围」（线下使用 / 线上线下 / 商城使用）做匹配。
     */
    private String channel;
    /** 明细行 */
    private List<CartLine> lines;
    /** 本单使用的优惠券 id 列表（可空；由开单侧传入） */
    private List<Long> couponIds;
    /** 排除的订单 id（改单时排除自身已核销的券占用） */
    private Long excludeOrderId;
}
