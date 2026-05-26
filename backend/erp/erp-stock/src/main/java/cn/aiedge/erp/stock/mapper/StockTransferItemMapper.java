package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.StockTransferItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockTransferItemMapper extends BaseMapper<StockTransferItem> {

    @Select("SELECT * FROM erp_stock_transfer_item WHERE transfer_id = #{transferId} AND deleted = 0 ORDER BY line_no ASC")
    List<StockTransferItem> selectByTransferId(@Param("transferId") Long transferId);

    @Select("SELECT SUM(plan_quantity) FROM erp_stock_transfer_item WHERE transfer_id = #{transferId} AND deleted = 0")
    java.math.BigDecimal sumPlanQuantityByTransferId(@Param("transferId") Long transferId);

    @Select("SELECT SUM(actual_quantity) FROM erp_stock_transfer_item WHERE transfer_id = #{transferId} AND deleted = 0")
    java.math.BigDecimal sumActualQuantityByTransferId(@Param("transferId") Long transferId);
}