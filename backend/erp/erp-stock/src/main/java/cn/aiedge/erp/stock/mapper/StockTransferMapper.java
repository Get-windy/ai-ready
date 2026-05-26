package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.StockTransfer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockTransferMapper extends BaseMapper<StockTransfer> {

    @Select("SELECT * FROM erp_stock_transfer WHERE from_warehouse_id = #{warehouseId} AND deleted = 0 ORDER BY create_time DESC")
    List<StockTransfer> selectByFromWarehouseId(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_transfer WHERE to_warehouse_id = #{warehouseId} AND deleted = 0 ORDER BY create_time DESC")
    List<StockTransfer> selectByToWarehouseId(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_transfer WHERE status = #{status} AND deleted = 0 AND tenant_id = #{tenantId}")
    List<StockTransfer> selectByStatus(@Param("status") Integer status, @Param("tenantId") Long tenantId);
}