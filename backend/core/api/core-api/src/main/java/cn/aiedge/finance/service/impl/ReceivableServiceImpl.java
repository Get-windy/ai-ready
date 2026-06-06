package cn.aiedge.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.entity.Receivable;
import cn.aiedge.finance.entity.Receipt;
import cn.aiedge.finance.mapper.ReceivableMapper;
import cn.aiedge.finance.mapper.FinanceReceiptMapper;
import cn.aiedge.finance.service.ReceivableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应收账款服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableServiceImpl extends ServiceImpl<ReceivableMapper, Receivable> 
        implements ReceivableService {

    private final FinanceReceiptMapper receiptMapper;

    @Override
    public PageResult<ReceivableVO> pageList(ReceivableQueryRequest request) {
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        
        if (request.getCustomerId() != null) {
            wrapper.eq(Receivable::getCustomerId, request.getCustomerId());
        }
        if (StringUtils.hasText(request.getCustomerName())) {
            wrapper.like(Receivable::getCustomerName, request.getCustomerName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Receivable::getStatus, request.getStatus());
        }
        if (request.getBillDateStart() != null) {
            wrapper.ge(Receivable::getBillDate, request.getBillDateStart());
        }
        if (request.getBillDateEnd() != null) {
            wrapper.le(Receivable::getBillDate, request.getBillDateEnd());
        }
        
        wrapper.orderByDesc(Receivable::getCreateTime);
        
        Page<Receivable> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Receivable> result = this.page(page, wrapper);
        
        List<ReceivableVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReceivableCreateRequest request) {
        Receivable receivable = new Receivable();
        BeanUtils.copyProperties(request, receivable);
        
        // 生成应收编号
        String receivableNo = generateReceivableNo();
        receivable.setReceivableNo(receivableNo);
        
        // 设置初始金额
        receivable.setOriginalAmount(request.getOriginalAmount());
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setRemainingAmount(request.getOriginalAmount());
        
        // 设置状态
        receivable.setStatus(0); // 未收款
        
        // 计算逾期天数
        if (request.getDueDate().isBefore(LocalDate.now())) {
            long days = ChronoUnit.DAYS.between(request.getDueDate(), LocalDate.now());
            receivable.setOverdueDays(Math.toIntExact(days));
        } else {
            receivable.setOverdueDays(0);
        }
        
        this.save(receivable);
        
        log.info("创建应收账款成功: id={}, no={}", receivable.getId(), receivable.getReceivableNo());
        return receivable.getId();
    }

    @Override
    public void update(ReceivableUpdateRequest request) {
        Receivable receivable = this.getById(request.getId());
        if (receivable == null) {
            throw BusinessException.notFound("应收账款不存在");
        }
        
        if (StringUtils.hasText(request.getRemark())) {
            receivable.setRemark(request.getRemark());
        }
        
        this.updateById(receivable);
        log.info("更新应收账款成功: id={}", receivable.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Receivable receivable = this.getById(id);
        if (receivable == null) {
            throw BusinessException.notFound("应收账款不存在");
        }
        
        // 检查是否有关联的收款记录
        LambdaQueryWrapper<Receipt> receiptWrapper = new LambdaQueryWrapper<>();
        receiptWrapper.eq(Receipt::getReceivableId, id);
        if (receiptMapper.selectCount(receiptWrapper) > 0) {
            throw BusinessException.badRequest("该应收账款下存在收款记录，无法删除");
        }
        
        this.removeById(id);
        log.info("删除应收账款成功: id={}", id);
    }

    @Override
    public ReceivableVO getDetail(Long id) {
        Receivable receivable = this.getById(id);
        if (receivable == null) {
            throw BusinessException.notFound("应收账款不存在");
        }
        
        return convertToVO(receivable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receivePayment(ReceiptCreateRequest request) {
        Receivable receivable = this.getById(request.getReceivableId());
        if (receivable == null) {
            throw BusinessException.notFound("应收账款不存在");
        }
        
        if (request.getAmount().compareTo(receivable.getRemainingAmount()) > 0) {
            throw BusinessException.badRequest("收款金额不能超过剩余金额");
        }
        
        // 创建收款记录
        Receipt receipt = new Receipt();
        receipt.setReceivableId(request.getReceivableId());
        receipt.setCustomerId(receivable.getCustomerId());
        receipt.setCustomerName(receivable.getCustomerName());
        receipt.setAmount(request.getAmount());
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setBankAccount(request.getBankAccount());
        receipt.setVoucherNo(request.getVoucherNo());
        receipt.setRemark(request.getRemark());
        receipt.setStatus(1); // 已确认
        
        // 生成收款编号
        receipt.setReceiptNo(generateReceiptNo());
        
        receiptMapper.insert(receipt);
        
        // 更新应收账款金额
        receivable.setReceivedAmount(receivable.getReceivedAmount().add(request.getAmount()));
        receivable.setRemainingAmount(receivable.getOriginalAmount().subtract(receivable.getReceivedAmount()));
        
        // 更新状态
        if (receivable.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            receivable.setStatus(2); // 已收款
        } else {
            receivable.setStatus(1); // 部分收款
        }
        
        this.updateById(receivable);
        
        log.info("收款操作成功: receivableId={}, amount={}", request.getReceivableId(), request.getAmount());
    }

    @Override
    public List<AgingAnalysisVO> analyzeAging() {
        List<Receivable> receivables = this.list();
        List<AgingAnalysisVO> result = new ArrayList<>();
        
        // 按账龄区间统计
        long current = 0, thirty = 0, sixty = 0, ninety = 0, more = 0;
        BigDecimal currentAmt = BigDecimal.ZERO, thirtyAmt = BigDecimal.ZERO, 
                   sixtyAmt = BigDecimal.ZERO, ninetyAmt = BigDecimal.ZERO, 
                   moreAmt = BigDecimal.ZERO;
        
        for (Receivable r : receivables) {
            long overdueDays = r.getOverdueDays();
            BigDecimal remaining = r.getRemainingAmount();
            
            if (overdueDays <= 0) {
                current++;
                currentAmt = currentAmt.add(remaining);
            } else if (overdueDays <= 30) {
                thirty++;
                thirtyAmt = thirtyAmt.add(remaining);
            } else if (overdueDays <= 60) {
                sixty++;
                sixtyAmt = sixtyAmt.add(remaining);
            } else if (overdueDays <= 90) {
                ninety++;
                ninetyAmt = ninetyAmt.add(remaining);
            } else {
                more++;
                moreAmt = moreAmt.add(remaining);
            }
        }
        
        // 构建结果
        BigDecimal totalAmount = currentAmt.add(thirtyAmt).add(sixtyAmt).add(ninetyAmt).add(moreAmt);
        
        if (current > 0) {
            AgingAnalysisVO vo = new AgingAnalysisVO();
            vo.setPeriod("未逾期");
            vo.setCount((int) current);
            vo.setTotalAmount(currentAmt);
            vo.setPercentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? 
                currentAmt.multiply(new BigDecimal("100")).divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            result.add(vo);
        }
        
        if (thirty > 0) {
            AgingAnalysisVO vo = new AgingAnalysisVO();
            vo.setPeriod("1-30天");
            vo.setCount((int) thirty);
            vo.setTotalAmount(thirtyAmt);
            vo.setPercentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? 
                thirtyAmt.multiply(new BigDecimal("100")).divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            result.add(vo);
        }
        
        if (sixty > 0) {
            AgingAnalysisVO vo = new AgingAnalysisVO();
            vo.setPeriod("31-60天");
            vo.setCount((int) sixty);
            vo.setTotalAmount(sixtyAmt);
            vo.setPercentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? 
                sixtyAmt.multiply(new BigDecimal("100")).divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            result.add(vo);
        }
        
        if (ninety > 0) {
            AgingAnalysisVO vo = new AgingAnalysisVO();
            vo.setPeriod("61-90天");
            vo.setCount((int) ninety);
            vo.setTotalAmount(ninetyAmt);
            vo.setPercentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? 
                ninetyAmt.multiply(new BigDecimal("100")).divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            result.add(vo);
        }
        
        if (more > 0) {
            AgingAnalysisVO vo = new AgingAnalysisVO();
            vo.setPeriod("90天以上");
            vo.setCount((int) more);
            vo.setTotalAmount(moreAmt);
            vo.setPercentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ? 
                moreAmt.multiply(new BigDecimal("100")).divide(totalAmount, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            result.add(vo);
        }
        
        return result;
    }

    @Override
    public List<ReceivableVO> getByCustomer(Long customerId) {
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Receivable::getCustomerId, customerId)
               .orderByDesc(Receivable::getCreateTime);
        
        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public List<ReceivableVO> exportList(ReceivableQueryRequest request) {
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        if (request.getCustomerId() != null) {
            wrapper.eq(Receivable::getCustomerId, request.getCustomerId());
        }
        wrapper.orderByDesc(Receivable::getCreateTime);

        List<Receivable> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private ReceivableVO convertToVO(Receivable receivable) {
        ReceivableVO vo = new ReceivableVO();
        BeanUtils.copyProperties(receivable, vo);
        
        // 计算账龄区间
        if (receivable.getOverdueDays() <= 0) {
            vo.setAgingPeriod("未逾期");
        } else if (receivable.getOverdueDays() <= 30) {
            vo.setAgingPeriod("1-30天");
        } else if (receivable.getOverdueDays() <= 60) {
            vo.setAgingPeriod("31-60天");
        } else if (receivable.getOverdueDays() <= 90) {
            vo.setAgingPeriod("61-90天");
        } else {
            vo.setAgingPeriod("90天以上");
        }
        
        return vo;
    }

    /**
     * 生成应收编号
     */
    private String generateReceivableNo() {
        return "AR" + System.currentTimeMillis();
    }

    /**
     * 生成收款编号
     */
    private String generateReceiptNo() {
        return "RP" + System.currentTimeMillis();
    }
}
