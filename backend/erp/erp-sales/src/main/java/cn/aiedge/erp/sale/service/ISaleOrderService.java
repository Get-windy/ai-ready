package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.SaleOrderDTO;
import cn.aiedge.erp.sale.dto.SaleOrderDetailDTO;
import cn.aiedge.erp.sale.dto.SaleOrderListDTO;
import cn.aiedge.erp.sale.entity.SaleOrder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * 销售订单服务接口
 * 列表查询返回 SaleOrderListDTO (~20字段)
 * 详情查询返回 SaleOrderDetailDTO (含子表数据)
 */
public interface ISaleOrderService extends IService<SaleOrder> {

    /** 分页查询订单（列表） */
    Page<SaleOrderListDTO> pageOrders(Page<SaleOrder> page, Long tenantId, String orderNo,
                                       Long customerId, Integer status, String startDate, String endDate);

    /** 获取订单详情（含子表数据） */
    SaleOrderDetailDTO getOrderDetail(Long id);

    /** 创建订单 */
    Long createOrder(SaleOrderDTO dto);

    /** 更新订单 */
    void updateOrder(SaleOrderDTO dto);

    /** 删除订单 */
    void deleteOrder(Long id);

    /** 提交审批 */
    void submitForApproval(Long id);

    /** 审批通过 */
    void approve(Long id, Long auditorId);

    /**
     * 审批通过（显式指定终审人姓名）：工作流回调等无 Sa-Token 会话的线程使用，
     * 避免 getCurrentUserName() 落"系统"；auditorName 为空时回退当前会话用户名
     */
    void approve(Long id, Long auditorId, String auditorName);

    /** 审批拒绝 */
    void reject(Long id, Long auditorId, String reason);

    /** 取消订单 */
    void cancelOrder(Long id, String reason);

    /** 确认出库 */
    void confirmShipment(Long id, Long warehouseId);

    /** 记录收款 */
    void recordPayment(Long id, BigDecimal amount);

    /** 获取待审批订单 */
    List<SaleOrderListDTO> getPendingOrders(Long tenantId);

    /** 生成订单号 */
    String generateOrderNo();

    /** 计算订单金额 */
    void calculateAmount(SaleOrderDTO dto);

    /** 导出销售订单列表 */
    List<SaleOrderListDTO> exportList(String keyword, Long customerId, Integer status);

    /** 获取订单统计数据 */
    Map<String, Object> getOrderStats(Long tenantId);

    // ═══ 订单处理中心 ═══

    /** 按单据tab分页查询 */
    Page<SaleOrderListDTO> orderCenterPageByDoc(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters);

    /** 按时间tab分组统计 */
    List<Map<String, Object>> orderCenterGroupByDate(Long tenantId, Map<String, Object> filters);

    /** 按线路tab分组统计 */
    List<Map<String, Object>> orderCenterGroupByRoute(Long tenantId, Map<String, Object> filters);

    /** 按客户tab分组统计 */
    List<Map<String, Object>> orderCenterGroupByCustomer(Page<?> page, Long tenantId, Map<String, Object> filters);

    /** 订单履约tab列表 */
    Page<SaleOrderListDTO> orderCenterFulfillmentPage(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters);

    /** 订单履约统计概览 */
    Map<String, Object> orderCenterFulfillmentOverview(Long tenantId, Map<String, Object> filters);

    /** 统计卡片 */
    Map<String, Object> orderCenterStats(Long tenantId, Map<String, Object> filters);

    /** 待审核列表 */
    Page<SaleOrderListDTO> pendingReviewPage(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters);

    /** 拣货/发货列表 */
    Page<SaleOrderListDTO> pickingShippingPage(Page<SaleOrder> page, Long tenantId, Map<String, Object> filters);

    /** 构建查询条件 */
    LambdaQueryWrapper<SaleOrder> buildQueryWrapper(Long tenantId, Map<String, Object> filters);

    /** 批量导入 */
    void batchImport(MultipartFile file) throws Exception;

    /** 批量增加打印次数 */
    void incrementPrintCount(List<Long> ids);

    /** 商品汇总查询 */
    List<Map<String, Object>> productSummary(Long tenantId, Map<String, Object> filters);

    /** 批量更新物流备注 */
    void batchUpdateLogisticsRemark(List<Long> ids, String remark);

    /** 获取客户信用信息 */
    Map<String, Object> getCustomerCreditInfo(Long customerId);

    /** 获取客户预收款/订金余额 */
    List<Map<String, Object>> getCustomerDepositBalance(Long customerId);
}
