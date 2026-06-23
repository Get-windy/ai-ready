package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseInquiryItem;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 询价明细Mapper接口
 */
@Mapper
public interface PurchaseInquiryItemMapper {

    /**
     * 插入询价明细
     */
    @Insert("INSERT INTO purchase_inquiry_item (inquiry_id, item_seq, material_id, material_code, " +
            "material_name, specification, unit, quantity, min_quantity, quality_requirement, " +
            "delivery_requirement, brand_requirement, estimated_price, estimated_amount, created_at) " +
            "VALUES (#{inquiryId}, #{itemSeq}, #{materialId}, #{materialCode}, #{materialName}, " +
            "#{specification}, #{unit}, #{quantity}, #{minQuantity}, #{qualityRequirement}, " +
            "#{deliveryRequirement}, #{brandRequirement}, #{estimatedPrice}, #{estimatedAmount}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PurchaseInquiryItem item);

    /**
     * 批量插入询价明细
     */
    @Insert("<script>" +
            "INSERT INTO purchase_inquiry_item (inquiry_id, item_seq, material_id, material_code, " +
            "material_name, specification, unit, quantity, min_quantity, quality_requirement, " +
            "delivery_requirement, brand_requirement, estimated_price, estimated_amount, created_at) VALUES " +
            "<foreach collection='items' item='item' separator=','>" +
            "(#{item.inquiryId}, #{item.itemSeq}, #{item.materialId}, #{item.materialCode}, " +
            "#{item.materialName}, #{item.specification}, #{item.unit}, #{item.quantity}, " +
            "#{item.minQuantity}, #{item.qualityRequirement}, #{item.deliveryRequirement}, " +
            "#{item.brandRequirement}, #{item.estimatedPrice}, #{item.estimatedAmount}, NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("items") List<PurchaseInquiryItem> items);

    /**
     * 更新询价明细
     */
    @Update("UPDATE purchase_inquiry_item SET material_id=#{materialId}, material_code=#{materialCode}, " +
            "material_name=#{materialName}, specification=#{specification}, unit=#{unit}, " +
            "quantity=#{quantity}, min_quantity=#{minQuantity}, quality_requirement=#{qualityRequirement}, " +
            "delivery_requirement=#{deliveryRequirement}, brand_requirement=#{brandRequirement}, " +
            "estimated_price=#{estimatedPrice}, estimated_amount=#{estimatedAmount}, updated_at=NOW() " +
            "WHERE id=#{id}")
    int update(PurchaseInquiryItem item);

    /**
     * 根据ID删除询价明细
     */
    @Delete("DELETE FROM purchase_inquiry_item WHERE id=#{id}")
    int deleteById(Long id);

    /**
     * 根据询价单ID删除所有明细
     */
    @Delete("DELETE FROM purchase_inquiry_item WHERE inquiry_id=#{inquiryId}")
    int deleteByInquiryId(Long inquiryId);

    /**
     * 根据ID查询询价明细
     */
    @Select("SELECT * FROM purchase_inquiry_item WHERE id=#{id}")
    PurchaseInquiryItem selectById(Long id);

    /**
     * 根据询价单ID查询明细列表
     */
    @Select("SELECT * FROM purchase_inquiry_item WHERE inquiry_id=#{inquiryId} ORDER BY item_seq")
    List<PurchaseInquiryItem> selectByInquiryId(Long inquiryId);

    /**
     * 根据物料ID查询询价明细
     */
    @Select("SELECT * FROM purchase_inquiry_item WHERE material_id=#{materialId}")
    List<PurchaseInquiryItem> selectByMaterialId(Long materialId);

    /**
     * 统计询价单的明细数量
     */
    @Select("SELECT COUNT(*) FROM purchase_inquiry_item WHERE inquiry_id=#{inquiryId}")
    int countByInquiryId(Long inquiryId);
}