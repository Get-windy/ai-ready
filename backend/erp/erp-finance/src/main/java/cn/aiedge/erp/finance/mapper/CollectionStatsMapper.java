package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.dto.CollectionStatsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 回款统计Mapper
 * 数据源：收款单 erp_receipt
 * 口径：deleted=0 且状态为已审批(2)/待核销(4)/核销中(5)/已核销(6)/已完成(7)，
 * 排除草稿(0)/待审批(1)/已拒绝(3)/已取消(8)
 */
@Mapper
public interface CollectionStatsMapper {

    /**
     * 现金支付方式取值
     */
    String CASH_METHODS = "'1','cash','现金'";
    /**
     * 银行支付方式取值(转账/支票/信用卡/在线支付)
     */
    String BANK_METHODS = "'2','3','4','5','bank','bank_transfer','check','credit_card','online','银行转账','支票','信用卡','在线支付'";

    /**
     * 回款汇总
     */
    @Select("<script>"
            + "SELECT COUNT(*) AS receiptCount, COALESCE(SUM(receipt_amount), 0) AS totalAmount,"
            + " COALESCE(SUM(CASE WHEN lower(COALESCE(payment_method,'')) IN (" + CASH_METHODS + ") THEN receipt_amount END), 0) AS cashAmount,"
            + " COALESCE(SUM(CASE WHEN lower(COALESCE(payment_method,'')) IN (" + BANK_METHODS + ") THEN receipt_amount END), 0) AS bankAmount,"
            + " COALESCE(SUM(CASE WHEN lower(COALESCE(payment_method,'')) NOT IN (" + CASH_METHODS + "," + BANK_METHODS + ") THEN receipt_amount END), 0) AS otherAmount"
            + " FROM erp_receipt"
            + " WHERE deleted = 0 AND status IN (2,4,5,6,7)"
            + "<if test='startDate != null'> AND receipt_date &gt;= #{startDate}</if>"
            + "<if test='endDate != null'> AND receipt_date &lt;= #{endDate}</if>"
            + "</script>")
    CollectionStatsDTO.Summary selectSummary(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 按维度分组统计
     * groupExpr/nameExpr 由 Service 白名单控制，禁止前端直传
     */
    @Select("<script>"
            + "SELECT ${groupExpr} AS groupKey, ${nameExpr} AS groupName,"
            + " COUNT(*) AS receiptCount, COALESCE(SUM(receipt_amount), 0) AS totalAmount,"
            + " COALESCE(SUM(CASE WHEN lower(COALESCE(payment_method,'')) IN (" + CASH_METHODS + ") THEN receipt_amount END), 0) AS cashAmount,"
            + " COALESCE(SUM(CASE WHEN lower(COALESCE(payment_method,'')) IN (" + BANK_METHODS + ") THEN receipt_amount END), 0) AS bankAmount,"
            + " COALESCE(SUM(CASE WHEN lower(COALESCE(payment_method,'')) NOT IN (" + CASH_METHODS + "," + BANK_METHODS + ") THEN receipt_amount END), 0) AS otherAmount"
            + " FROM erp_receipt"
            + " WHERE deleted = 0 AND status IN (2,4,5,6,7)"
            + "<if test='startDate != null'> AND receipt_date &gt;= #{startDate}</if>"
            + "<if test='endDate != null'> AND receipt_date &lt;= #{endDate}</if>"
            + " GROUP BY ${groupExpr} ORDER BY ${groupExpr}"
            + "</script>")
    List<CollectionStatsDTO.Detail> selectDetails(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("groupExpr") String groupExpr,
                                                  @Param("nameExpr") String nameExpr);
}
