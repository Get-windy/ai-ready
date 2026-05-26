package cn.aiedge.erp.order.mapper;

import cn.aiedge.erp.order.entity.PurchaseOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单数据访问层接口
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /**
     * 根据供应商ID统计采购订单
     */
    @Select("SELECT COUNT(*) FROM purchase_order WHERE supplier_id = #{supplierId} AND deleted = 0")
    Long countBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 根据状态统计采购订单数量
     */
    @Select("SELECT COUNT(*) FROM purchase_order WHERE status = #{status} AND deleted = 0")
    Long countByStatus(@Param("status") String status);

    /**
     * 获取指定日期范围内的采购订单
     */
    @Select("SELECT * FROM purchase_order WHERE order_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY order_date DESC")
    List<PurchaseOrder> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 根据采购类型统计金额
     */
    @Select("SELECT SUM(total_amount) FROM purchase_order WHERE purchase_type = #{purchaseType} AND deleted = 0")
    BigDecimal sumAmountByPurchaseType(@Param("purchaseType") String purchaseType);

    /**
     * 更新采购订单状态
     */
    @Select("UPDATE purchase_order SET status = #{status}, updated_at = NOW() WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 根据审批状态查询采购订单
     */
    @Select("SELECT * FROM purchase_order WHERE approval_status = #{approvalStatus} AND deleted = 0 ORDER BY created_at DESC")
    List<PurchaseOrder> findByApprovalStatus(@Param("approvalStatus") String approvalStatus);

    /**
     * 根据付款状态查询采购订单
     */
    @Select("SELECT * FROM purchase_order WHERE payment_status = #{paymentStatus} AND deleted = 0 ORDER BY payment_due_date ASC")
    List<PurchaseOrder> findByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    /**
     * 统计供应商的采购金额
     */
    @Select("SELECT supplier_id, SUM(total_amount) as total_amount FROM purchase_order WHERE deleted = 0 GROUP BY supplier_id ORDER BY total_amount DESC LIMIT #{limit}")
    List<PurchaseOrder> findTopSuppliersByAmount(@Param("limit") int limit);

    /**
     * 获取临期付款的采购订单
     */
    @Select("SELECT * FROM purchase_order WHERE payment_status = 'UNPAID' AND payment_due_date <= #{dueDate} AND deleted = 0 ORDER BY payment_due_date ASC")
    List<PurchaseOrder> findDuePaymentOrders(@Param("dueDate") LocalDateTime dueDate);
}