package cn.aiedge.erp.purchase.mapper;

import cn.aiedge.erp.purchase.dto.PurchaseDetailListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderListDTO;
import cn.aiedge.erp.purchase.entity.PurchaseOrder;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    @Update("UPDATE erp_purchase_order SET status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE erp_purchase_order SET received_amount = #{receivedAmount}, update_time = #{updateTime} WHERE id = #{id}")
    int updateReceivedAmount(@Param("id") Long id, @Param("receivedAmount") BigDecimal receivedAmount, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 按单据Tab查询：JOIN多表解析名称+聚合计算值
     * 通过自定义SQL一次性返回39列+名称解析+聚合值
     *
     * @param page    分页参数（MyBatis-Plus自动处理分页和count）
     * @param wrapper 查询条件（使用PurchaseOrder实体字段名，列名不加别名前缀）
     * @return 分页结果
     */
    Page<PurchaseOrderListDTO> selectDocListWithNames(Page<?> page, @Param("ew") Wrapper<PurchaseOrder> wrapper);

    /**
     * 按明细Tab查询：JOIN明细行+主表+名称解析
     * 以明细行为主表，每条记录是一个明细行（59列+名称解析）
     *
     * @param page    分页参数（按明细行分页）
     * @param wrapper 查询条件（列名需带表别名前缀，如 i.product_name, o.order_date）
     * @return 分页结果
     */
    Page<PurchaseDetailListDTO> selectDetailListWithNames(Page<?> page, @Param("ew") Wrapper<PurchaseOrder> wrapper);
}
