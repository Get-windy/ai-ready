package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseContractItem;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 合同明细Mapper接口
 */
@Mapper
public interface PurchaseContractItemMapper {

    @Select("SELECT * FROM purchase_contract_item WHERE id = #{id}")
    PurchaseContractItem findById(Long id);

    @Select("SELECT * FROM purchase_contract_item WHERE contract_id = #{contractId}")
    List<PurchaseContractItem> findByContractId(Long contractId);

    @Insert("INSERT INTO purchase_contract_item (contract_id, material_name, specification, " +
            "unit, quantity, unit_price, amount, tax_rate, tax_amount, brand, model, " +
            "quality_level, origin_country, lead_time, delivery_location, item_note) " +
            "VALUES (#{contractId}, #{materialName}, #{specification}, #{unit}, #{quantity}, " +
            "#{unitPrice}, #{amount}, #{taxRate}, #{taxAmount}, #{brand}, #{model}, " +
            "#{qualityLevel}, #{originCountry}, #{leadTime}, #{deliveryLocation}, #{itemNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseContractItem item);

    @Insert("<script>" +
            "INSERT INTO purchase_contract_item (contract_id, material_name, unit, " +
            "quantity, unit_price, amount) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.contractId}, #{item.materialName}, #{item.unit}, " +
            "#{item.quantity}, #{item.unitPrice}, #{item.amount})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<PurchaseContractItem> items);

    @Update("UPDATE purchase_contract_item SET material_name=#{materialName}, specification=#{specification}, " +
            "unit=#{unit}, quantity=#{quantity}, unit_price=#{unitPrice}, amount=#{amount} WHERE id=#{id}")
    int update(PurchaseContractItem item);

    @Delete("DELETE FROM purchase_contract_item WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM purchase_contract_item WHERE contract_id = #{contractId}")
    int deleteByContractId(Long contractId);
}