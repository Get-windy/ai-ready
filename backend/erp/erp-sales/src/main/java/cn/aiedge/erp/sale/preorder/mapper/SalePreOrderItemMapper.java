package cn.aiedge.erp.sale.preorder.mapper;

import cn.aiedge.erp.sale.preorder.dto.SalePreOrderDetailDTO;
import cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalePreOrderItemMapper extends BaseMapper<SalePreOrderItem> {

    /**
     * 分页查询预订货单明细视图（JOIN主表获取完整数据）
     * tenant_id 由 MyBatis-Plus TenantLineInnerInterceptor 自动注入
     */
    IPage<SalePreOrderDetailDTO> selectDetailPage(
        Page<?> page,
        @Param("keyword") String keyword,
        @Param("customerId") Long customerId,
        @Param("customerName") String customerName,
        @Param("handlerName") String handlerName,
        @Param("deptName") String deptName,
        @Param("orderNo") String orderNo,
        @Param("status") List<Integer> status,
        @Param("settlementStatus") Integer settlementStatus,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("productAttribute") String productAttribute,
        @Param("gift") Boolean gift,
        @Param("creatorName") String creatorName,
        @Param("auditorName") String auditorName,
        @Param("saleType") Integer saleType,
        @Param("remark") String remark,
        @Param("itemRemark") String itemRemark,
        @Param("categoryIds") List<Long> categoryIds
    );
}
