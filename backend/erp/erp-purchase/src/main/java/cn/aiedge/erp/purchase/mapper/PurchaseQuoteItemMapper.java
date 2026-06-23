package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 报价明细Mapper接口
 */
@Mapper
public interface PurchaseQuoteItemMapper {

    @Select("SELECT * FROM purchase_quote_item WHERE id = #{id}")
    PurchaseQuoteItem findById(Long id);

    @Select("SELECT * FROM purchase_quote_item WHERE quote_id = #{quoteId}")
    List<PurchaseQuoteItem> findByQuoteId(Long quoteId);

    @Insert("INSERT INTO purchase_quote_item (quote_id, inquiry_item_id, material_name, " +
            "specification, unit, quantity, unit_price, amount, tax_rate, tax_amount, " +
            "brand, model, quality_level, origin_country, lead_time, delivery_location, item_note) " +
            "VALUES (#{quoteId}, #{inquiryItemId}, #{materialName}, #{specification}, " +
            "#{unit}, #{quantity}, #{unitPrice}, #{amount}, #{taxRate}, #{taxAmount}, " +
            "#{brand}, #{model}, #{qualityLevel}, #{originCountry}, #{leadTime}, #{deliveryLocation}, #{itemNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseQuoteItem item);

    @Update("UPDATE purchase_quote_item SET material_name=#{materialName}, specification=#{specification}, " +
            "unit=#{unit}, quantity=#{quantity}, unit_price=#{unitPrice}, amount=#{amount}, " +
            "tax_rate=#{taxRate}, tax_amount=#{taxAmount} WHERE id=#{id}")
    int update(PurchaseQuoteItem item);

    @Delete("DELETE FROM purchase_quote_item WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM purchase_quote_item WHERE quote_id = #{quoteId}")
    int deleteByQuoteId(Long quoteId);

    @Insert("<script>" +
            "INSERT INTO purchase_quote_item (quote_id, inquiry_item_id, material_name, " +
            "specification, unit, quantity, unit_price, amount) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.quoteId}, #{item.inquiryItemId}, #{item.materialName}, " +
            "#{item.specification}, #{item.unit}, #{item.quantity}, #{item.unitPrice}, #{item.amount})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<PurchaseQuoteItem> items);
}