package cn.aiedge.erp.sale.preorder.service;

import cn.aiedge.erp.sale.preorder.entity.SalePreOrder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 预订货单服务接口
 */
public interface SalePreOrderService extends IService<SalePreOrder> {

    /**
     * 分页查询预订货单（按单据）
     */
    Page<SalePreOrder> pageList(String keyword, Long customerId, String customerName,
                                String handlerName, String deptName,
                                Integer[] status, Integer settlementStatus,
                                String depositDeadlineStart, String depositDeadlineEnd,
                                String startDate, String endDate,
                                String warehouseName, String creatorName,
                                String auditorName, Integer saleType, String remark,
                                BigDecimal extNum1, BigDecimal extNum2,
                                String extText1, String extText2, String extText3,
                                int pageNum, int pageSize);

    /**
     * 分页查询预订货单明细（按明细）
     */
    Page<Map<String, Object>> pageDetail(String keyword, Long customerId, String customerName,
                                          String handlerName, String deptName,
                                          String orderNo, Integer[] status,
                                          Integer settlementStatus, Long categoryId,
                                          String startDate, String endDate,
                                          String creatorName, String auditorName,
                                          Integer saleType, String productAttribute,
                                          String remark, String itemRemark, Boolean gift,
                                          int pageNum, int pageSize);

    /**
     * 获取预订货单详情（含明细）
     */
    SalePreOrder getDetail(Long id);

    /**
     * 验证并创建预订货单（包含外键验证）
     * @param order 订单主体
     * @param items 明细列表
     * @return 创建后的订单（含ID）
     */
    SalePreOrder validateAndCreate(SalePreOrder order, java.util.List<cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem> items);

    /**
     * 验证并更新预订货单（包含外键验证和状态检查）
     * @param id 订单ID
     * @param order 订单主体
     * @param items 明细列表
     */
    void validateAndUpdate(Long id, SalePreOrder order, java.util.List<cn.aiedge.erp.sale.preorder.entity.SalePreOrderItem> items);

    /**
     * 提交审批
     */
    void submit(Long id);

    /**
     * 审批通过
     */
    void approve(Long id);

    /**
     * 批量订货（将预订货单转为正式销售订单）
     */
    void batchOrder(Long id);

    /**
     * 打印计数递增
     */
    void incrementPrintCount(Long id);
}
