package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 销售订单明细Mapper
 */
@Mapper
public interface SaleOrderItemMapper extends BaseMapper<SaleOrderItem> {

    /**
     * 根据订单ID查询明细
     */
    List<SaleOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新已出库数量
     */
    int addShippedQuantity(@Param("id") Long id, @Param("quantity") java.math.BigDecimal quantity);

    /**
     * 设置已出库数量
     */
    int updateShippedQuantity(@Param("id") Long id, @Param("quantity") java.math.BigDecimal quantity);

    /**
     * 设置已拣货数量（拣货作业回写）
     */
    int updatePickedQuantity(@Param("id") Long id, @Param("quantity") java.math.BigDecimal quantity);
}