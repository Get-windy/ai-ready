package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseQuoteComparison;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 比价分析Mapper接口
 */
@Mapper
public interface PurchaseQuoteComparisonMapper {

    /**
     * 插入比价记录
     */
    @Insert("INSERT INTO purchase_quote_comparison (inquiry_id, comparison_date, comparator_id, " +
            "comparison_method, winning_quote_id, winning_supplier_id, winning_amount, savings_amount, " +
            "savings_rate, comparison_detail, price_analysis, quality_analysis, service_analysis, " +
            "recommendation, decision_basis, created_at) " +
            "VALUES (#{inquiryId}, #{comparisonDate}, #{comparatorId}, #{comparisonMethod}, " +
            "#{winningQuoteId}, #{winningSupplierId}, #{winningAmount}, #{savingsAmount}, #{savingsRate}, " +
            "#{comparisonDetail}, #{priceAnalysis}, #{qualityAnalysis}, #{serviceAnalysis}, " +
            "#{recommendation}, #{decisionBasis}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseQuoteComparison comparison);

    /**
     * 根据ID查询比价记录
     */
    @Select("SELECT * FROM purchase_quote_comparison WHERE id=#{id}")
    PurchaseQuoteComparison selectById(Long id);

    /**
     * 根据询价单ID查询比价记录列表
     */
    @Select("SELECT * FROM purchase_quote_comparison WHERE inquiry_id=#{inquiryId} ORDER BY comparison_date DESC")
    List<PurchaseQuoteComparison> selectByInquiryId(Long inquiryId);

    /**
     * 查询询价单的最新比价记录
     */
    @Select("SELECT * FROM purchase_quote_comparison WHERE inquiry_id=#{inquiryId} " +
            "ORDER BY comparison_date DESC LIMIT 1")
    PurchaseQuoteComparison selectLatestByInquiryId(Long inquiryId);

    /**
     * 根据比价人查询比价记录
     */
    @Select("SELECT * FROM purchase_quote_comparison WHERE comparator_id=#{comparatorId} " +
            "ORDER BY comparison_date DESC")
    List<PurchaseQuoteComparison> selectByComparatorId(Long comparatorId);

    /**
     * 删除比价记录
     */
    @Delete("DELETE FROM purchase_quote_comparison WHERE id=#{id}")
    int deleteById(Long id);

    /**
     * 统计询价单的比价次数
     */
    @Select("SELECT COUNT(*) FROM purchase_quote_comparison WHERE inquiry_id=#{inquiryId}")
    int countByInquiryId(Long inquiryId);
}