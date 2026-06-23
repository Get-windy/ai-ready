package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 订单明细Mapper接口
 */
@Mapper
public interface PurchaseOrderItemMapper {

    @Select("SELECT * FROM purchase_order_item WHERE id = #{id}")
    PurchaseOrderItem findById(Long id);

    @Select("SELECT * FROM purchase_order_item WHERE order_id = #{orderId}")
    List<PurchaseOrderItem> findByOrderId(Long orderId);

    @Insert("INSERT INTO purchase_order_item (order_id, material_name, specification, " +
            "unit, quantity, unit_price, amount, tax_rate, tax_amount, brand, model, " +
            "quality_level, origin_country, lead_time, delivery_location, item_note) " +
            "VALUES (#{orderId}, #{materialName}, #{specification}, #{unit}, #{quantity}, " +
            "#{unitPrice}, #{amount}, #{taxRate}, #{taxAmount}, #{brand}, #{model}, " +
            "#{qualityLevel}, #{originCountry}, #{leadTime}, #{deliveryLocation}, #{itemNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseOrderItem item);

    @Insert("<script>" +
            "INSERT INTO purchase_order_item (order_id, material_name, unit, " +
            "quantity, unit_price, amount) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.orderId}, #{item.materialName}, #{item.unit}, " +
            "#{item.quantity}, #{item.unitPrice}, #{item.amount})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<PurchaseOrderItem> items);

    @Update("UPDATE purchase_order_item SET received_quantity=#{receivedQuantity}, " +
            "fulfillment_percent=#{fulfillmentPercent} WHERE id=#{id}")
    int update(PurchaseOrderItem item);

    @Delete("DELETE FROM purchase_order_item WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM purchase_order_item WHERE order_id = #{orderId}")
    int deleteByOrderId(Long orderId);
}