package cn.aiedge.erp.sale.return.mapper;

import cn.aiedge.erp.sale.return.entity.SaleReturnItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleReturnItemMapper extends BaseMapper<SaleReturnItem> {

    @Select("SELECT * FROM erp_sale_return_item WHERE return_id = #{returnId} AND deleted = 0 ORDER BY line_no ASC")
    List<SaleReturnItem> selectByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(return_quantity) FROM erp_sale_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumReturnQuantityByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(return_amount) FROM erp_sale_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumReturnAmountByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(accepted_quantity) FROM erp_sale_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumAcceptedQuantityByReturnId(@Param("returnId") Long returnId);

    @Select("SELECT SUM(refund_amount) FROM erp_sale_return_item WHERE return_id = #{returnId} AND deleted = 0")
    java.math.BigDecimal sumRefundAmountByReturnId(@Param("returnId") Long returnId);
}