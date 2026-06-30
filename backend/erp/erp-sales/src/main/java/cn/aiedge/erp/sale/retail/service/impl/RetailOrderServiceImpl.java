package cn.aiedge.erp.sale.retail.service.impl;

import cn.aiedge.erp.sale.retail.entity.RetailOrder;
import cn.aiedge.erp.sale.retail.entity.RetailOrderItem;
import cn.aiedge.erp.sale.retail.mapper.RetailOrderItemMapper;
import cn.aiedge.erp.sale.retail.mapper.RetailOrderMapper;
import cn.aiedge.erp.sale.retail.service.IRetailOrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetailOrderServiceImpl extends ServiceImpl<RetailOrderMapper, RetailOrder> implements IRetailOrderService {

    private final RetailOrderItemMapper retailOrderItemMapper;

    @Override
    public IPage<RetailOrder> pageRetails(Page<RetailOrder> page, String retailNo, String customerName,
                                          String startDate, String endDate) {
        LambdaQueryWrapper<RetailOrder> wrapper = new LambdaQueryWrapper<RetailOrder>()
                .eq(RetailOrder::getDeleted, 0)
                .like(StringUtils.hasText(retailNo), RetailOrder::getRetailNo, retailNo)
                .like(StringUtils.hasText(customerName), RetailOrder::getCustomerName, customerName)
                .ge(StringUtils.hasText(startDate), RetailOrder::getCreateTime, startDate + " 00:00:00")
                .le(StringUtils.hasText(endDate), RetailOrder::getCreateTime, endDate + " 23:59:59")
                .orderByDesc(RetailOrder::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailOrder saveWithItems(RetailOrder order, List<RetailOrderItem> items) {
        // 计算商品总金额
        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal amt = (i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                            .multiply(i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO);
                    i.setAmount(amt);
                    return amt;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(totalAmount);
        // 如果未设置应付金额，则等于商品总额减优惠
        if (order.getPayableAmount() == null || order.getPayableAmount().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal discount = (order.getDirectDiscount() != null ? order.getDirectDiscount() : BigDecimal.ZERO)
                    .add(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO)
                    .add(order.getPromoDiscount() != null ? order.getPromoDiscount() : BigDecimal.ZERO);
            order.setPayableAmount(totalAmount.subtract(discount));
        }
        save(order);
        if (items != null) {
            for (RetailOrderItem item : items) {
                item.setOrderId(order.getId());
                retailOrderItemMapper.insert(item);
            }
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RetailOrder updateWithItems(RetailOrder order, List<RetailOrderItem> items) {
        // 重新计算金额
        BigDecimal totalAmount = items.stream()
                .map(i -> {
                    BigDecimal amt = (i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                            .multiply(i.getUnitPrice() != null ? i.getUnitPrice() : BigDecimal.ZERO);
                    i.setAmount(amt);
                    return amt;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setAmount(totalAmount);
        if (order.getPayableAmount() == null || order.getPayableAmount().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal discount = (order.getDirectDiscount() != null ? order.getDirectDiscount() : BigDecimal.ZERO)
                    .add(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO)
                    .add(order.getPromoDiscount() != null ? order.getPromoDiscount() : BigDecimal.ZERO);
            order.setPayableAmount(totalAmount.subtract(discount));
        }
        updateById(order);
        // 删除旧明细，重新插入
        LambdaQueryWrapper<RetailOrderItem> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(RetailOrderItem::getOrderId, order.getId());
        retailOrderItemMapper.delete(delWrapper);
        if (items != null) {
            for (RetailOrderItem item : items) {
                item.setId(null);
                item.setOrderId(order.getId());
                retailOrderItemMapper.insert(item);
            }
        }
        return order;
    }

    @Override
    public List<RetailOrderItem> listItemsByOrderId(Long orderId) {
        LambdaQueryWrapper<RetailOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RetailOrderItem::getOrderId, orderId)
               .eq(RetailOrderItem::getDeleted, 0);
        return retailOrderItemMapper.selectList(wrapper);
    }

    @Override
    public RetailOrderDetailVO getDetailById(Long id) {
        RetailOrder order = getById(id);
        if (order == null) return null;
        List<RetailOrderItem> items = listItemsByOrderId(id);
        return new RetailOrderDetailVO(order, items);
    }
}
