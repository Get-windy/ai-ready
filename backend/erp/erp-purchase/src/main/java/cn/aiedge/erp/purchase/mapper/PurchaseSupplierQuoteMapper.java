package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商报价单Mapper接口
 */
@Mapper
public interface PurchaseSupplierQuoteMapper {

    @Select("SELECT * FROM purchase_supplier_quote ORDER BY created_at DESC")
    List<PurchaseSupplierQuote> findAll();

    @Select("SELECT * FROM purchase_supplier_quote WHERE id = #{id}")
    PurchaseSupplierQuote findById(Long id);

    @Select("SELECT * FROM purchase_supplier_quote WHERE quote_no = #{quoteNo}")
    PurchaseSupplierQuote findByQuoteNo(String quoteNo);

    @Select("SELECT * FROM purchase_supplier_quote WHERE inquiry_id = #{inquiryId} ORDER BY total_score DESC")
    List<PurchaseSupplierQuote> findByInquiryId(Long inquiryId);

    @Select("SELECT * FROM purchase_supplier_quote WHERE supplier_id = #{supplierId} ORDER BY created_at DESC")
    List<PurchaseSupplierQuote> findBySupplierId(Long supplierId);

    @Select("SELECT * FROM purchase_supplier_quote WHERE inquiry_id = #{inquiryId} AND quote_status = #{quoteStatus}")
    List<PurchaseSupplierQuote> findByInquiryIdAndStatus(Long inquiryId, String quoteStatus);

    @Insert("INSERT INTO purchase_supplier_quote (quote_no, inquiry_id, supplier_id, quote_status, " +
            "quote_date, valid_until, total_amount, tax_rate, payment_terms, delivery_terms, " +
            "warranty_terms, supplier_note, attachment_urls, created_by) VALUES " +
            "(#{quoteNo}, #{inquiryId}, #{supplierId}, #{quoteStatus}, #{quoteDate}, #{validUntil}, " +
            "#{totalAmount}, #{taxRate}, #{paymentTerms}, #{deliveryTerms}, #{warrantyTerms}, " +
            "#{supplierNote}, #{attachmentUrls}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseSupplierQuote quote);

    @Update("UPDATE purchase_supplier_quote SET quote_status=#{quoteStatus}, price_score=#{priceScore}, " +
            "quality_score=#{qualityScore}, service_score=#{serviceScore}, total_score=#{totalScore}, " +
            "is_recommended=#{isRecommended}, recommend_reason=#{recommendReason} WHERE id=#{id}")
    int update(PurchaseSupplierQuote quote);

    @Delete("DELETE FROM purchase_supplier_quote WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM purchase_supplier_quote WHERE inquiry_id = #{inquiryId}")
    int countByInquiryId(Long inquiryId);

    @Select("SELECT * FROM purchase_supplier_quote WHERE inquiry_id = #{inquiryId} " +
            "ORDER BY total_score DESC LIMIT 1")
    PurchaseSupplierQuote findTopByInquiryId(Long inquiryId);

    @Select("SELECT * FROM purchase_supplier_quote WHERE supplier_id = #{supplierId} " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<PurchaseSupplierQuote> findHistoryBySupplierId(Long supplierId, int limit);

    @Update("UPDATE purchase_supplier_quote SET quote_status = #{status}, updated_at = #{updateTime} WHERE id = #{id}")
    int updateStatus(Long id, String status, LocalDateTime updateTime);
}