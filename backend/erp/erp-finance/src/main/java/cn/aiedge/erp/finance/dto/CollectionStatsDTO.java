package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 回款统计DTO
 * 汇总 + 按维度分组明细
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionStatsDTO {

    /**
     * 汇总信息
     */
    private Summary summary;

    /**
     * 分组明细列表
     */
    private List<Detail> details;

    /**
     * 回款汇总
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        /**
         * 回款笔数
         */
        private Long receiptCount = 0L;

        /**
         * 回款总额
         */
        private BigDecimal totalAmount = BigDecimal.ZERO;

        /**
         * 现金回款金额
         */
        private BigDecimal cashAmount = BigDecimal.ZERO;

        /**
         * 银行回款金额(转账/支票/信用卡/在线支付)
         */
        private BigDecimal bankAmount = BigDecimal.ZERO;

        /**
         * 其他方式回款金额
         */
        private BigDecimal otherAmount = BigDecimal.ZERO;
    }

    /**
     * 分组明细
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        /**
         * 分组键(日期/期间/人员ID/客户ID)
         */
        private String groupKey;

        /**
         * 分组名称(人员姓名/客户名称，日期维度为空)
         */
        private String groupName;

        /**
         * 回款笔数
         */
        private Long receiptCount = 0L;

        /**
         * 回款金额
         */
        private BigDecimal totalAmount = BigDecimal.ZERO;

        /**
         * 现金回款金额
         */
        private BigDecimal cashAmount = BigDecimal.ZERO;

        /**
         * 银行回款金额
         */
        private BigDecimal bankAmount = BigDecimal.ZERO;

        /**
         * 其他方式回款金额
         */
        private BigDecimal otherAmount = BigDecimal.ZERO;
    }
}
