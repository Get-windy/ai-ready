package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseQuoteContract;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 合同关联Mapper接口
 */
@Mapper
public interface PurchaseQuoteContractMapper {

    /**
     * 插入合同关联
     */
    @Insert("INSERT INTO purchase_quote_contract (quote_id, contract_id, conversion_date, converter_id, " +
            "contract_no, contract_amount, contract_status, fulfillment_status, fulfillment_progress, " +
            "performance_score, performance_date, performance_comment, created_at) " +
            "VALUES (#{quoteId}, #{contractId}, #{conversionDate}, #{converterId}, #{contractNo}, " +
            "#{contractAmount}, #{contractStatus}, #{fulfillmentStatus}, #{fulfillmentProgress}, " +
            "#{performanceScore}, #{performanceDate}, #{performanceComment}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseQuoteContract quoteContract);

    /**
     * 更新合同关联
     */
    @Update("UPDATE purchase_quote_contract SET contract_no=#{contractNo}, contract_amount=#{contractAmount}, " +
            "contract_status=#{contractStatus}, fulfillment_status=#{fulfillmentStatus}, " +
            "fulfillment_progress=#{fulfillmentProgress}, performance_score=#{performanceScore}, " +
            "performance_date=#{performanceDate}, performance_comment=#{performanceComment}, updated_at=NOW() " +
            "WHERE id=#{id}")
    int update(PurchaseQuoteContract quoteContract);

    /**
     * 根据ID查询合同关联
     */
    @Select("SELECT * FROM purchase_quote_contract WHERE id=#{id}")
    PurchaseQuoteContract selectById(Long id);

    /**
     * 根据报价单ID查询合同关联
     */
    @Select("SELECT * FROM purchase_quote_contract WHERE quote_id=#{quoteId}")
    PurchaseQuoteContract selectByQuoteId(Long quoteId);

    /**
     * 根据合同ID查询合同关联
     */
    @Select("SELECT * FROM purchase_quote_contract WHERE contract_id=#{contractId}")
    PurchaseQuoteContract selectByContractId(Long contractId);

    /**
     * 查询所有合同关联
     */
    @Select("SELECT * FROM purchase_quote_contract ORDER BY conversion_date DESC")
    List<PurchaseQuoteContract> selectAll();

    /**
     * 根据履约状态查询合同关联
     */
    @Select("SELECT * FROM purchase_quote_contract WHERE fulfillment_status=#{fulfillmentStatus} " +
            "ORDER BY conversion_date DESC")
    List<PurchaseQuoteContract> selectByFulfillmentStatus(String fulfillmentStatus);

    /**
     * 删除合同关联
     */
    @Delete("DELETE FROM purchase_quote_contract WHERE id=#{id}")
    int deleteById(Long id);
}