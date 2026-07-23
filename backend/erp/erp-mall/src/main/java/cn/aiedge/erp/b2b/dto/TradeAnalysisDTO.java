package cn.aiedge.erp.b2b.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城交易分析结果
 * 数据来源: erp_sale_order (order_source=2 企业客户商城 / order_source=3 个人会员商城)
 */
@Data
public class TradeAnalysisDTO {

    /** 汇总 */
    private Summary summary;

    /** 按日分组 */
    private List<DailyItem> daily;

    /** 支付状态分布 */
    private List<PaymentStatusItem> paymentStatusDistribution;

    /**
     * 交易汇总
     */
    @Data
    public static class Summary {
        /** 总单数 (排除已取消) */
        private Long totalOrderCount;
        /** GMV (订单总金额) */
        private BigDecimal totalGmv;
        /** 客单价 = GMV / 总单数 */
        private BigDecimal avgOrderAmount;
        /** 退款单数 (payment_status=4 已退款) */
        private Long refundOrderCount;
        /** 退款率(%) = 退款单数 / 总单数 * 100 */
        private BigDecimal refundRate;
    }

    /**
     * 按日交易
     */
    @Data
    public static class DailyItem {
        /** 日期 (yyyy-MM-dd) */
        private String day;
        /** 订单数 */
        private Long orderCount;
        /** GMV */
        private BigDecimal gmv;
        /** 客单价 */
        private BigDecimal avgOrderAmount;
    }

    /**
     * 支付状态分布项
     */
    @Data
    public static class PaymentStatusItem {
        /** 支付状态: 0待支付,1支付中,2已支付,3部分支付,4已退款 */
        private Integer paymentStatus;
        /** 支付状态名称 */
        private String paymentStatusName;
        /** 订单数 */
        private Long count;
    }
}
