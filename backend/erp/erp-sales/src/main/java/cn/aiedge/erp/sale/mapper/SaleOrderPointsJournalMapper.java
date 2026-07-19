package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderPointsJournal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaleOrderPointsJournalMapper extends BaseMapper<SaleOrderPointsJournal> {

    @Select("SELECT * FROM erp_sale_order_points_journal WHERE order_id = #{orderId}")
    SaleOrderPointsJournal selectByOrderId(@Param("orderId") Long orderId);
}
