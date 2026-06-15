package cn.aiedge.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.finance.entity.Payable;
import cn.aiedge.finance.entity.Payment;
import cn.aiedge.finance.mapper.PayableMapper;
import cn.aiedge.finance.mapper.FinancePaymentMapper;
import cn.aiedge.finance.dto.PayableCreateRequest;
import cn.aiedge.finance.dto.PayableUpdateRequest;
import cn.aiedge.finance.dto.PayableQueryRequest;
import cn.aiedge.finance.dto.PayableVO;
import cn.aiedge.finance.service.IPayableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import cn.aiedge.common.utils.IdGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应付账款服务实现
 */
@Service
@Transactional
public class PayableServiceImpl extends ServiceImpl<PayableMapper, Payable> implements IPayableService {

    private final PayableMapper payableMapper;
    private final FinancePaymentMapper paymentMapper;

    public PayableServiceImpl(PayableMapper payableMapper, FinancePaymentMapper paymentMapper) {
        this.payableMapper = payableMapper;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Long createPayable(PayableCreateRequest request) {
        Payable payable = new Payable();
        BeanUtils.copyProperties(request, payable);
        
        // 设置编号
        payable.setPayableNo("PAY-" + System.currentTimeMillis());
        
        // 设置初始金额
        payable.setOriginalAmount(request.getOriginalAmount());
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setRemainingAmount(request.getOriginalAmount());
        
        // 设置状态
        payable.setStatus(0); // 未付款
        
        // 计算逾期天数
        if (request.getDueDate().isBefore(LocalDate.now())) {
            payable.setOverdueDays((int) java.time.temporal.ChronoUnit.DAYS.between(request.getDueDate(), LocalDate.now()));
        } else {
            payable.setOverdueDays(0);
        }
        
        // 设置租户ID和创建信息
        payable.setTenantId(getCurrentTenantId());
        payable.setCreateBy(getCurrentUser());
        payable.setCreateTime(LocalDateTime.now());
        
        payableMapper.insert(payable);
        return payable.getId();
    }

    @Override
    public void updatePayable(PayableUpdateRequest request) {
        Payable payable = new Payable();
        payable.setId(request.getId());
        payable.setRemark(request.getRemark());
        payable.setUpdateBy(getCurrentUser());
        payable.setUpdateTime(LocalDateTime.now());
        
        payableMapper.updateById(payable);
    }

    @Override
    public Page<PayableVO> pagePayables(PayableQueryRequest request) {
        LambdaQueryWrapper<Payable> wrapper = Wrappers.lambdaQuery(Payable.class)
                .eq(request.getSupplierId() != null, Payable::getSupplierId, request.getSupplierId())
                .like(request.getSupplierName() != null && !request.getSupplierName().isEmpty(), Payable::getSupplierName, request.getSupplierName())
                .eq(request.getStatus() != null, Payable::getStatus, request.getStatus())
                .ge(request.getBillDateStart() != null, Payable::getBillDate, request.getBillDateStart())
                .le(request.getBillDateEnd() != null, Payable::getBillDate, request.getBillDateEnd())
                .eq(Payable::getTenantId, getCurrentTenantId())
                .orderByDesc(Payable::getCreateTime);

        long pageNum = request.getPageNum() != null ? request.getPageNum() : 1L;
        long pageSize = request.getPageSize() != null ? request.getPageSize() : 20L;
        Page<Payable> page = new Page<>(pageNum, pageSize);
        Page<Payable> resultPage = payableMapper.selectPage(page, wrapper);

        Page<PayableVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        List<PayableVO> voList = resultPage.getRecords().stream().map(this::convertToVO).toList();
        voPage.setRecords(voList);

        return voPage;
    }

    @Override
    public PayableVO getPayableById(Long id) {
        Payable payable = payableMapper.selectById(id);
        if (payable == null || !payable.getTenantId().equals(getCurrentTenantId())) {
            return null;
        }
        return convertToVO(payable);
    }

    @Override
    public void deletePayable(Long id) {
        Payable payable = payableMapper.selectById(id);
        if (payable != null && payable.getTenantId().equals(getCurrentTenantId())) {
            // 检查是否有关联的付款记录
            LambdaQueryWrapper<Payment> paymentWrapper = Wrappers.lambdaQuery(Payment.class)
                    .eq(Payment::getPayableId, id);
            List<Payment> payments = paymentMapper.selectList(paymentWrapper);
            
            if (!payments.isEmpty()) {
                throw BusinessException.badRequest("存在关联的付款记录，无法删除");
            }
            
            payableMapper.deleteById(id);
        }
    }

    @Override
    public void batchDelete(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public List<PayableVO> exportList(PayableQueryRequest request) {
        LambdaQueryWrapper<Payable> wrapper = Wrappers.lambdaQuery(Payable.class)
                .eq(request.getSupplierId() != null, Payable::getSupplierId, request.getSupplierId())
                .eq(Payable::getTenantId, getCurrentTenantId())
                .orderByDesc(Payable::getCreateTime);

        List<Payable> list = payableMapper.selectList(wrapper);
        return list.stream().map(this::convertToVO).toList();
    }

    private PayableVO convertToVO(Payable payable) {
        PayableVO vo = new PayableVO();
        BeanUtils.copyProperties(payable, vo);
        
        // 计算账龄区间
        vo.setAgingPeriod(calculateAgingPeriod(payable.getBillDate()));
        
        return vo;
    }

    private String calculateAgingPeriod(LocalDate billDate) {
        if (billDate == null) {
            return "未知";
        }
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(billDate, LocalDate.now());
        
        if (daysBetween <= 30) {
            return "0-30天";
        } else if (daysBetween <= 60) {
            return "31-60天";
        } else if (daysBetween <= 90) {
            return "61-90天";
        } else if (daysBetween <= 180) {
            return "91-180天";
        } else {
            return "180天以上";
        }
    }

    private Long getCurrentTenantId() {
        // 获取当前租户ID，这里需要根据实际的租户管理实现来获取
        // 可能是从ThreadLocal、请求头或SaToken中获取
        return 1L; // 临时实现，实际项目中需要正确获取租户ID
    }

    private String getCurrentUser() {
        // 获取当前用户名
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsString();
        }
        return "system";
    }
}
