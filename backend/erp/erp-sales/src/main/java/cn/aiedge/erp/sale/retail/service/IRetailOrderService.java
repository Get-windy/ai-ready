package cn.aiedge.erp.sale.retail.service;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailOrderItem;
import cn.aiedge.erp.sale.retail.entity.RetailOrderPayment;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface IRetailOrderService extends IService<RetailOrder> {

    /**
     * 按单据分页查询零售单
     */
    IPage<RetailOrder> pageByDoc(Page<RetailOrder> page, RetailQueryDTO query);

    /**
     * 按明细分页查询零售单
     */
    IPage<Map<String, Object>> pageByDetail(Page<Map<String, Object>> page, RetailDetailQueryDTO query);

    /**
     * 保存零售单 + 明细行（事务）
     */
    RetailOrder saveWithItems(RetailOrder order, List<RetailOrderItem> items);

    /**
     * 更新零售单 + 明细行（事务）
     */
    RetailOrder updateWithItems(RetailOrder order, List<RetailOrderItem> items);

    /**
     * 查询某零售单的明细行列表
     */
    List<RetailOrderItem> listItemsByOrderId(Long orderId);

    /**
     * 查询零售单详情（含明细行）
     */
    RetailOrderDetailVO getDetailById(Long id);

    /**
     * 结算零售单
     */
    RetailOrder settle(Long orderId, List<RetailOrderPayment> payments);

    /**
     * 挂单
     */
    void hold(Long orderId);

    /**
     * 取单
     */
    void unhold(Long orderId);

    /**
     * 作废
     */
    void voidOrder(Long orderId, String reason);

    /**
     * 复制单据
     */
    RetailOrder copyOrder(Long orderId);

    /**
     * 查询挂单列表
     */
    List<RetailOrder> listHoldOrders(Long warehouseId);

    /**
     * 打印后更新打印次数
     */
    void incrementPrintCount(Long orderId);

    /**
     * 删除零售单（仅草稿状态可删除，软删除）
     */
    void deleteOrder(Long id);

    /**
     * 商品快速查找
     */
    List<Map<String, Object>> quickSearchProducts(String keyword, Long warehouseId);

    /**
     * 含明细行的零售单详情 VO
     */
    class RetailOrderDetailVO {
        private RetailOrder order;
        private List<RetailOrderItem> items;
        private List<RetailOrderPayment> payments;

        public RetailOrderDetailVO() {}
        public RetailOrderDetailVO(RetailOrder order, List<RetailOrderItem> items) {
            this.order = order;
            this.items = items;
        }

        public RetailOrder getOrder() { return order; }
        public void setOrder(RetailOrder order) { this.order = order; }
        public List<RetailOrderItem> getItems() { return items; }
        public void setItems(List<RetailOrderItem> items) { this.items = items; }
        public List<RetailOrderPayment> getPayments() { return payments; }
        public void setPayments(List<RetailOrderPayment> payments) { this.payments = payments; }
    }

    /**
     * 按单据查询条件DTO
     */
    class RetailQueryDTO {
        private String dateStart;
        private String dateEnd;
        private String retailNo;
        private String customerName;
        private Long customerId;
        private Long handlerId;
        private Long departmentId;
        private Long warehouseId;
        private Integer status;
        private String saleType;
        private String productAttribute;
        private String remark;
        private String creatorName;
        private String bookkeeperName;
        private String memberCardNo;
        private Boolean showRedFlush;
        private Integer pageNum = 1;
        private Integer pageSize = 20;

        // getters/setters
        public String getDateStart() { return dateStart; }
        public void setDateStart(String dateStart) { this.dateStart = dateStart; }
        public String getDateEnd() { return dateEnd; }
        public void setDateEnd(String dateEnd) { this.dateEnd = dateEnd; }
        public String getRetailNo() { return retailNo; }
        public void setRetailNo(String retailNo) { this.retailNo = retailNo; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public Long getHandlerId() { return handlerId; }
        public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getSaleType() { return saleType; }
        public void setSaleType(String saleType) { this.saleType = saleType; }
        public String getProductAttribute() { return productAttribute; }
        public void setProductAttribute(String productAttribute) { this.productAttribute = productAttribute; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public String getCreatorName() { return creatorName; }
        public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
        public String getBookkeeperName() { return bookkeeperName; }
        public void setBookkeeperName(String bookkeeperName) { this.bookkeeperName = bookkeeperName; }
        public String getMemberCardNo() { return memberCardNo; }
        public void setMemberCardNo(String memberCardNo) { this.memberCardNo = memberCardNo; }
        public Boolean getShowRedFlush() { return showRedFlush; }
        public void setShowRedFlush(Boolean showRedFlush) { this.showRedFlush = showRedFlush; }
        public Integer getPageNum() { return pageNum; }
        public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }

    /**
     * 按明细查询条件DTO
     */
    class RetailDetailQueryDTO {
        private String dateStart;
        private String dateEnd;
        private String retailNo;
        private String productName;
        private String barcode;
        private String customerName;
        private Long handlerId;
        private Long departmentId;
        private Long warehouseId;
        private Integer status;
        private String remark;
        private Boolean showRedFlush;
        private Integer pageNum = 1;
        private Integer pageSize = 20;

        // getters/setters
        public String getDateStart() { return dateStart; }
        public void setDateStart(String dateStart) { this.dateStart = dateStart; }
        public String getDateEnd() { return dateEnd; }
        public void setDateEnd(String dateEnd) { this.dateEnd = dateEnd; }
        public String getRetailNo() { return retailNo; }
        public void setRetailNo(String retailNo) { this.retailNo = retailNo; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public Long getHandlerId() { return handlerId; }
        public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        public Boolean getShowRedFlush() { return showRedFlush; }
        public void setShowRedFlush(Boolean showRedFlush) { this.showRedFlush = showRedFlush; }
        public Integer getPageNum() { return pageNum; }
        public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }
}
