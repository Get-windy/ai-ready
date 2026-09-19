package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员积分明细行（会员管理 →「积分明细」，数据源 erp_sale_order_points_journal） */
@Data
public class PointsJournalRow {

    private Long id;
    /** 关联销售单据号 */
    private String orderNo;
    private String memberCardNo;
    private String memberName;
    /** 会员折扣 */
    private BigDecimal memberDiscount;
    /** 此前积分 */
    private BigDecimal prevPoints;
    /** 销售积分 */
    private BigDecimal salePoints;
    /** 退货积分 */
    private BigDecimal returnPoints;
    /** 兑换积分 */
    private BigDecimal exchangePoints;
    /** 使用积分 */
    private BigDecimal usedPoints;
    /** 当前积分 */
    private BigDecimal currentPoints;
    /** 发生时间 */
    private LocalDateTime createTime;
}
