package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.StockCheck;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheck> {

    @Select("SELECT * FROM erp_stock_check WHERE warehouse_id = #{warehouseId} AND deleted = 0 ORDER BY create_time DESC")
    List<StockCheck> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_check WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<StockCheck> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);

    @Select("SELECT * FROM erp_stock_check WHERE check_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0")
    List<StockCheck> selectByDateRange(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
}