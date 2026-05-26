package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import cn.aiedge.erp.purchase.enums.OrderStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    @Select("SELECT * FROM erp_purchase_order WHERE id = #{id}")
    PurchaseOrder findById(Long id);

    @Update("UPDATE erp_purchase_order SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") OrderStatus status, @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE erp_purchase_order SET received_amount = #{receivedAmount}, fulfillment_percent = #{fulfillmentPercent}, update_time = #{updateTime} WHERE id = #{id}")
    int updateFulfillmentProgress(@Param("id") Long id, @Param("receivedAmount") BigDecimal receivedAmount, 
                                  @Param("fulfillmentPercent") BigDecimal fulfillmentPercent, @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE erp_purchase_order SET update_time = #{completionTime} WHERE id = #{id}")
    int updateCompletionTime(@Param("id") Long id, @Param("completionTime") LocalDateTime completionTime);
}