package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.Payment;
import cn.aiedge.finance.entity.Payable;
import cn.aiedge.finance.mapper.FinancePaymentMapper;
import cn.aiedge.finance.mapper.PayableMapper;
import cn.aiedge.finance.dto.PaymentCreateRequest;
import cn.aiedge.finance.dto.PaymentQueryRequest;
import cn.aiedge.finance.dto.PaymentVO;
import cn.aiedge.finance.service.IPaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录服务实现
 */
@Service
@Transactional
public class PaymentServiceImpl extends ServiceImpl<FinancePaymentMapper, Payment> implements IPaymentService {

    private final FinancePaymentMapper paymentMapper;
    private final PayableMapper payableMapper;

    public PaymentServiceImpl(FinancePaymentMapper paymentMapper, PayableMapper payableMapper) {
        this.paymentMapper = paymentMapper;
        this.payableMapper = payableMapper;
    }

    @Override
    public Long createPayment(PaymentCreateRequest request) {
        // 首先检查应付账款是否存在
        Payable payable = payableMapper.selectById(request.getPayableId());
        if (payable == null) {
            throw new RuntimeException("应付账款不存在");
        }

        // 检查付款金额是否超过剩余金额
        BigDecimal remainingAmount = payable.getOriginalAmount().subtract(payable.getPaidAmount());
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new RuntimeException("付款金额不能超过剩余应付金额");
        }

        Payment payment = new Payment();
        BeanUtils.copyProperties(request, payment);
        
        // 设置编号
        payment.setPaymentNo("PMT-" + System.currentTimeMillis());
        
        // 设置状态
        payment.setStatus(0); // 待审批
        
        // 设置租户ID和创建信息
        payment.setTenantId(getCurrentTenantId());
        payment.setCreateBy(getCurrentUser());
        payment.setCreateTime(LocalDateTime.now());
        
        paymentMapper.insert(payment);
        
        // 更新应付账款的已付金额和剩余金额
        updatePayableAmount(payable, request.getAmount());
        
        return payment.getId();
    }

    @Override
    public Page<PaymentVO> pagePayments(PaymentQueryRequest request) {
        LambdaQueryWrapper<Payment> wrapper = Wrappers.lambdaQuery(Payment.class)
                .eq(request.getPayableId() != null, Payment::getPayableId, request.getPayableId())
                .eq(request.getSupplierId() != null, Payment::getSupplierId, request.getSupplierId())
                .eq(request.getStatus() != null, Payment::getStatus, request.getStatus())
                .ge(request.getPaymentDateStart() != null, Payment::getPaymentDate, request.getPaymentDateStart())
                .le(request.getPaymentDateEnd() != null, Payment::getPaymentDate, request.getPaymentDateEnd())
                .eq(Payment::getTenantId, getCurrentTenantId())
                .orderByDesc(Payment::getCreateTime);

        Page<Payment> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Payment> resultPage = paymentMapper.selectPage(page, wrapper);

        Page<PaymentVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());

        resultPage.getRecords().forEach(payment -> {
            PaymentVO vo = convertToVO(payment);
            voPage.getRecords().add(vo);
        });

        return voPage;
    }

    @Override
    public PaymentVO getPaymentById(Long id) {
        Payment payment = paymentMapper.selectById(id);
        if (payment == null || !payment.getTenantId().equals(getCurrentTenantId())) {
            return null;
        }
        return convertToVO(payment);
    }

    @Override
    public void deletePayment(Long id) {
        Payment payment = paymentMapper.selectById(id);
        if (payment != null && payment.getTenantId().equals(getCurrentTenantId())) {
            paymentMapper.deleteById(id);
            
            // 如果付款已删除，需要回退应付账款的金额
            rollbackPayableAmount(payment);
        }
    }

    private PaymentVO convertToVO(Payment payment) {
        PaymentVO vo = new PaymentVO();
        BeanUtils.copyProperties(payment, vo);
        return vo;
    }

    private void updatePayableAmount(Payable payable, BigDecimal paymentAmount) {
        // 计算新的已付金额和剩余金额
        BigDecimal newPaidAmount = payable.getPaidAmount().add(paymentAmount);
        BigDecimal newRemainingAmount = payable.getOriginalAmount().subtract(newPaidAmount);
        
        // 更新状态
        int status = 0; // 默认未付款
        if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0 && newRemainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            status = 1; // 部分付款
        } else if (newRemainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            status = 2; // 已付款
        }
        
        payable.setPaidAmount(newPaidAmount);
        payable.setRemainingAmount(newRemainingAmount);
        payable.setStatus(status);
        payable.setUpdateBy(getCurrentUser());
        payable.setUpdateTime(LocalDateTime.now());
        
        payableMapper.updateById(payable);
    }

    private void rollbackPayableAmount(Payment payment) {
        // 回退应付账款的金额
        Payable payable = payableMapper.selectById(payment.getPayableId());
        if (payable != null) {
            BigDecimal newPaidAmount = payable.getPaidAmount().subtract(payment.getAmount());
            BigDecimal newRemainingAmount = payable.getOriginalAmount().subtract(newPaidAmount);
            
            // 重新计算状态
            int status = 0; // 未付款
            if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0 && newRemainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                status = 1; // 部分付款
            } else if (newRemainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                status = 2; // 已付款
            }
            
            payable.setPaidAmount(newPaidAmount);
            payable.setRemainingAmount(newRemainingAmount);
            payable.setStatus(status);
            payable.setUpdateBy(getCurrentUser());
            payable.setUpdateTime(LocalDateTime.now());
            
            payableMapper.updateById(payable);
        }
    }

    private Long getCurrentTenantId() {
        // 获取当前租户ID，这里需要根据实际的租户管理实现来获取
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
