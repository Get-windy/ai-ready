package cn.aiedge.erp.purchase.purchasereturn.mapper;

import cn.aiedge.erp.purchase.purchasereturn.entity.PurchaseReturnItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseReturnItemMapper extends BaseMapper<PurchaseReturnItem> {

    @Select("SELECT * FROM erp_purchase_return_item WHERE return_id = #{returnId} AND deleted = 0 ORDER BY line_no ASC")
    List<PurchaseReturnItem> selectByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(return_quantity) FROM erp_purchase_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumReturnQuantityByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(line_amount) FROM erp_purchase_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumLineAmountByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(tax_amount) FROM erp_purchase_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumTaxAmountByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(line_total) FROM erp_purchase_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumLineTotalByReturnId(@Param("returnId") Long returnId);
}
