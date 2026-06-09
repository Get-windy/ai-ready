package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.StockReplenishment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockReplenishmentMapper extends BaseMapper<StockReplenishment> {

    @Select("SELECT * FROM erp_stock_replenishment WHERE status = #{status} AND deleted = 0 ORDER BY priority ASC, create_time DESC")
    List<StockReplenishment> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM erp_stock_replenishment WHERE product_code = #{productCode} AND status = 'PENDING' AND deleted = 0")
    List<StockReplenishment> selectPendingByProductCode(@Param("productCode") String productCode);
}
