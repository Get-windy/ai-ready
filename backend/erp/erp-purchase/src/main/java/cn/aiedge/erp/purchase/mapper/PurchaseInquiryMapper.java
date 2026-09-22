package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 询价单Mapper接口
 */
@Mapper
public interface PurchaseInquiryMapper {

    @Select("SELECT * FROM purchase_inquiry WHERE deleted = false ORDER BY created_at DESC")
    List<PurchaseInquiry> findAll();

    @Select("SELECT * FROM purchase_inquiry WHERE id = #{id} AND deleted = false")
    PurchaseInquiry findById(Long id);

    @Select("SELECT * FROM purchase_inquiry WHERE inquiry_no = #{inquiryNo} AND deleted = false")
    PurchaseInquiry findByInquiryNo(String inquiryNo);

    @Select("SELECT * FROM purchase_inquiry WHERE status = #{status} AND deleted = false ORDER BY deadline_date")
    List<PurchaseInquiry> findByStatus(String status);

    @Select("SELECT * FROM purchase_inquiry WHERE purchaser_id = #{purchaserId} AND deleted = false ORDER BY created_at DESC")
    List<PurchaseInquiry> findByPurchaserId(Long purchaserId);

    @Insert("INSERT INTO purchase_inquiry (inquiry_no, title, inquiry_type, status, requirement_desc, " +
            "urgency_level, deadline_date, department_id, requester_id, purchaser_id, " +
            "invited_supplier_ids, created_by) VALUES (#{inquiryNo}, #{title}, #{inquiryType}, " +
            "#{status}, #{requirementDesc}, #{urgencyLevel}, #{deadlineDate}, #{departmentId}, " +
            "#{requesterId}, #{purchaserId}, #{invitedSupplierIds}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseInquiry inquiry);

    @Update("UPDATE purchase_inquiry SET title=#{title}, inquiry_type=#{inquiryType}, " +
            "status=#{status}, requirement_desc=#{requirementDesc}, urgency_level=#{urgencyLevel}, " +
            "deadline_date=#{deadlineDate}, updated_by=#{updatedBy} WHERE id=#{id}")
    int update(PurchaseInquiry inquiry);

    @Update("UPDATE purchase_inquiry SET status=#{status}, publish_date=#{publishDate} WHERE id=#{id}")
    int updateStatus(Long id, String status, String publishDate);

    @Update("UPDATE purchase_inquiry SET quote_count=#{quoteCount} WHERE id=#{id}")
    int updateQuoteCount(Long id, Integer quoteCount);

    @Delete("UPDATE purchase_inquiry SET deleted = true WHERE id = #{id}")
    int deleteById(Long id);

    // ⚠️ `#{x,jdbcType=VARCHAR}` 不能省：PostgreSQL 对 `? IS NULL` 里的裸占位符
    // **无法推断类型**，会直接报 `无法确定参数 $1 的数据类型` ⇒ 整页 500。
    // 显式给 JDBC 类型后，NULL 参数也能被正确绑定。
    @Select("SELECT * FROM purchase_inquiry WHERE deleted = false "
            + "AND (#{keyword,jdbcType=VARCHAR} IS NULL OR title LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%') "
            + "     OR inquiry_no LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%')) "
            + "AND (#{status,jdbcType=VARCHAR} IS NULL OR status = #{status,jdbcType=VARCHAR}) "
            + "ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<PurchaseInquiry> findPage(int offset, int pageSize, String keyword, String status);

    @Select("SELECT COUNT(*) FROM purchase_inquiry WHERE deleted = false "
            + "AND (#{keyword,jdbcType=VARCHAR} IS NULL OR title LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%') "
            + "     OR inquiry_no LIKE CONCAT('%', #{keyword,jdbcType=VARCHAR}, '%')) "
            + "AND (#{status,jdbcType=VARCHAR} IS NULL OR status = #{status,jdbcType=VARCHAR})")
    long countByConditions(String keyword, String status);
}