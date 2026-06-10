package cn.aiedge.erp.expense.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.*;
import cn.aiedge.erp.expense.entity.FeeApplication;
import cn.aiedge.erp.expense.entity.FeePaymentRecord;
import cn.aiedge.erp.expense.entity.FeeReimbursement;
import cn.aiedge.erp.expense.mapper.FeeApplicationMapper;
import cn.aiedge.erp.expense.mapper.FeePaymentRecordMapper;
import cn.aiedge.erp.expense.mapper.FeeReimbursementMapper;
import cn.aiedge.erp.expense.service.FeePaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 付款服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeePaymentServiceImpl implements FeePaymentService {

    private final FeePaymentRecordMapper paymentRecordMapper;
    private final FeeApplicationMapper applicationMapper;
    private final FeeReimbursementMapper reimbursementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(FeePaymentCreateRequest request) {
        FeePaymentRecord record = new FeePaymentRecord();
        BeanUtils.copyProperties(request, record);

        record.setPaymentNo(generatePaymentNo());
        record.setStatus("PENDING");
        record.setCurrency("CNY");
        record.setCreateBy(SecurityUtils.getCurrentUserId());

        // 获取业务单号
        if ("APPLICATION".equals(request.getBusinessType())) {
            FeeApplication app = applicationMapper.selectById(request.getBusinessId());
            if (app == null) throw BusinessException.notFound("费用申请不存在");
            record.setBusinessNo(app.getApplicationNo());
        } else if ("REIMBURSEMENT".equals(request.getBusinessType())) {
            FeeReimbursement reimb = reimbursementMapper.selectById(request.getBusinessId());
            if (reimb == null) throw BusinessException.notFound("费用报销不存在");
            record.setBusinessNo(reimb.getReimbursementNo());
        }

        paymentRecordMapper.insert(record);
        log.info("创建付款记录成功: id={}, no={}", record.getId(), record.getPaymentNo());
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long paymentId, Long confirmUserId, String confirmUserName) {
        FeePaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) throw BusinessException.notFound("付款记录不存在");
        if (!"PENDING".equals(record.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许确认");
        }

        record.setStatus("COMPLETED");
        record.setConfirmTime(LocalDateTime.now());
        record.setConfirmUserId(confirmUserId);
        record.setConfirmUserName(confirmUserName);
        record.setUpdateBy(confirmUserId);
        paymentRecordMapper.updateById(record);

        // 更新业务对象的付款状态
        updateBusinessPaymentStatus(record.getBusinessType(), record.getBusinessId(), record.getPaymentAmount());

        log.info("确认付款成功: paymentId={}, confirmUser={}", paymentId, confirmUserName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPayment(Long paymentId, String reason) {
        FeePaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record == null) throw BusinessException.notFound("付款记录不存在");
        if (!"PENDING".equals(record.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许取消");
        }
        record.setStatus("CANCELLED");
        record.setFailReason(reason);
        record.setUpdateBy(SecurityUtils.getCurrentUserId());
        paymentRecordMapper.updateById(record);
        log.info("取消付款成功: paymentId={}", paymentId);
    }

    @Override
    public PageResult<FeePaymentRecordVO> pageList(FeePaymentQueryRequest request) {
        LambdaQueryWrapper<FeePaymentRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getBusinessType())) {
            wrapper.eq(FeePaymentRecord::getBusinessType, request.getBusinessType());
        }
        if (request.getBusinessId() != null) {
            wrapper.eq(FeePaymentRecord::getBusinessId, request.getBusinessId());
        }
        if (StringUtils.hasText(request.getPaymentNo())) {
            wrapper.like(FeePaymentRecord::getPaymentNo, request.getPaymentNo());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(FeePaymentRecord::getStatus, request.getStatus());
        }
        if (request.getPaymentDateStart() != null) {
            wrapper.ge(FeePaymentRecord::getPaymentDate, request.getPaymentDateStart());
        }
        if (request.getPaymentDateEnd() != null) {
            wrapper.le(FeePaymentRecord::getPaymentDate, request.getPaymentDateEnd());
        }

        wrapper.orderByDesc(FeePaymentRecord::getCreateTime);

        Page<FeePaymentRecord> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<FeePaymentRecord> result = paymentRecordMapper.selectPage(page, wrapper);

        List<FeePaymentRecordVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public FeePaymentRecordVO getDetail(Long id) {
        FeePaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) throw BusinessException.notFound("付款记录不存在");
        return convertToVO(record);
    }

    @Override
    public PageResult<FeePaymentRecordVO> getByBusiness(String businessType, Long businessId, Integer pageNum, Integer pageSize) {
        FeePaymentQueryRequest request = new FeePaymentQueryRequest();
        request.setBusinessType(businessType);
        request.setBusinessId(businessId);
        request.setPageNum(pageNum);
        request.setPageSize(pageSize);
        return pageList(request);
    }

    // ========== 私有方法 ==========

    private void updateBusinessPaymentStatus(String businessType, Long businessId, BigDecimal amount) {
        if ("APPLICATION".equals(businessType)) {
            FeeApplication app = applicationMapper.selectById(businessId);
            if (app != null) {
                BigDecimal paid = app.getPaidAmount() != null ? app.getPaidAmount() : BigDecimal.ZERO;
                paid = paid.add(amount);
                app.setPaidAmount(paid);
                if (paid.compareTo(app.getTotalAmount()) >= 0) {
                    app.setPaymentStatus("COMPLETED");
                } else {
                    app.setPaymentStatus("PARTIAL");
                }
                applicationMapper.updateById(app);
            }
        } else if ("REIMBURSEMENT".equals(businessType)) {
            FeeReimbursement reimb = reimbursementMapper.selectById(businessId);
            if (reimb != null) {
                BigDecimal paid = reimb.getPaidAmount() != null ? reimb.getPaidAmount() : BigDecimal.ZERO;
                paid = paid.add(amount);
                reimb.setPaidAmount(paid);
                if (paid.compareTo(reimb.getTotalAmount()) >= 0) {
                    reimb.setPaymentStatus("COMPLETED");
                } else {
                    reimb.setPaymentStatus("PENDING");
                }
                reimbursementMapper.updateById(reimb);
            }
        }
    }

    private FeePaymentRecordVO convertToVO(FeePaymentRecord record) {
        FeePaymentRecordVO vo = new FeePaymentRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private String generatePaymentNo() {
        return "FP" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
}
