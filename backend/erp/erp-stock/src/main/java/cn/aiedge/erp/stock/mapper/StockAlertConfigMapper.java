package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.StockAlertConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockAlertConfigMapper extends BaseMapper<StockAlertConfig> {

    @Select("SELECT * FROM erp_stock_alert_config WHERE product_id = #{productId} AND warehouse_id = #{warehouseId} AND active = 1 AND deleted = 0")
    StockAlertConfig selectByProductAndWarehouse(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_alert_config WHERE warehouse_id = #{warehouseId} AND active = 1 AND deleted = 0")
    List<StockAlertConfig> selectByWarehouse(@Param("warehouseId") Long warehouseId);

    @Select("SELECT * FROM erp_stock_alert_config WHERE active = 1 AND deleted = 0")
    List<StockAlertConfig> selectAllActive();

    @Select("SELECT COUNT(*) FROM erp_stock_alert_config WHERE active = 1 AND deleted = 0 AND tenant_id = #{tenantId}")
    Integer countActive(@Param("tenantId") Long tenantId);
}