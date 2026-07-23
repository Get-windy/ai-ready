package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseOrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购订单明细Mapper
 */
@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    /**
     * 根据订单ID查询明细
     */
    List<PurchaseOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新已收货数量
     */
    int addReceivedQuantity(@Param("id") Long id, @Param("quantity") BigDecimal quantity);
}
