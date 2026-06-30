package cn.aiedge.erp.sale.retail.service;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailOrderItem;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IRetailOrderService extends IService<RetailOrder> {

    /**
     * 分页查询零售单
     */
    IPage<RetailOrder> pageRetails(Page<RetailOrder> page, String retailNo, String customerName,
                                   String startDate, String endDate);

    /**
     * 保存零售单 + 明细行（事务）
     */
    RetailOrder saveWithItems(RetailOrder order, List<RetailOrderItem> items);

    /**
     * 更新零售单 + 明细行（事务）：先删旧明细，再插入新明细
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
     * 含明细行的零售单详情 VO
     */
    class RetailOrderDetailVO {
        private RetailOrder order;
        private List<RetailOrderItem> items;

        public RetailOrderDetailVO() {}
        public RetailOrderDetailVO(RetailOrder order, List<RetailOrderItem> items) {
            this.order = order;
            this.items = items;
        }

        public RetailOrder getOrder() { return order; }
        public void setOrder(RetailOrder order) { this.order = order; }
        public List<RetailOrderItem> getItems() { return items; }
        public void setItems(List<RetailOrderItem> items) { this.items = items; }
    }
}
