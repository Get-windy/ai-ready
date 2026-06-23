package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.CapitalFlow;
import cn.aiedge.erp.payment.entity.Offset;
import cn.aiedge.erp.payment.entity.OffsetItem;
import cn.aiedge.erp.payment.mapper.OffsetItemMapper;
import cn.aiedge.erp.payment.mapper.OffsetMapper;
import cn.aiedge.erp.payment.service.CapitalFlowService;
import cn.aiedge.erp.payment.service.OffsetService;
import cn.aiedge.erp.payment.service.integration.PaymentAccountingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OffsetServiceImpl extends ServiceImpl<OffsetMapper, Offset> implements OffsetService {

    private final OffsetItemMapper offsetItemMapper;
    private final CapitalFlowService capitalFlowService;
    private final PaymentAccountingService paymentAccountingService;

    @Override
    public Offset getByOffsetNo(String offsetNo) {
        return lambdaQuery()
                .eq(Offset::getOffsetNo, offsetNo)
                .eq(Offset::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Offset> pageList(String keyword, String partyType, Long partyId, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<Offset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Offset::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Offset::getOffsetNo, keyword)
                    .or().like(Offset::getPartyName, keyword));
        }
        if (partyType != null && !partyType.isEmpty()) {
            wrapper.eq(Offset::getPartyType, partyType);
        }
        if (partyId != null) {
            wrapper.eq(Offset::getPartyId, partyId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Offset::getStatus, status);
        }
        wrapper.orderByDesc(Offset::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Offset createOffset(Offset offset, List<OffsetItem> items) {
        // Validate amounts
        if (offset.getReceivableAmount() == null || offset.getReceivableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.badRequest("应收金额必须大于零");
        }
        if (offset.getPayableAmount() == null || offset.getPayableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.badRequest("应付金额必须大于零");
        }
        if (items == null || items.isEmpty()) {
            throw BusinessException.badRequest("对冲明细不能为空");
        }

        // Calculate offset amount = min(receivable, payable)
        BigDecimal offsetAmount = offset.getReceivableAmount().min(offset.getPayableAmount());
        BigDecimal balanceAmount = offset.getReceivableAmount().subtract(offset.getPayableAmount()).abs();

        offset.setOffsetNo(generateOffsetNo());
        offset.setTenantId(1L);
        offset.setOffsetAmount(offsetAmount);
        offset.setBalanceAmount(balanceAmount);
        offset.setOffsetDate(LocalDate.now());
        offset.setStatus("draft");
        save(offset);

        // Save offset items
        for (int i = 0; i < items.size(); i++) {
            OffsetItem item = items.get(i);
            item.setOffsetId(offset.getId());
            item.setLineNo(i + 1);
            item.setTenantId(1L);
            offsetItemMapper.insert(item);
        }

        return offset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Offset completeOffset(Long id) {
        Offset offset = getById(id);
        if (offset == null) {
            throw BusinessException.notFound("对冲单不存在");
        }
        if (!"draft".equals(offset.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的对冲单可以完成");
        }

        offset.setStatus("completed");
        updateById(offset);

        // Record capital flows for both sides
        capitalFlowService.recordOffsetFlow(offset);

        // Call accounting service for the offset
        try {
            // Record receivable reduction (IN flow showing receivable settled)
            paymentAccountingService.postReceiptVoucher(
                    offset.getId(),
                    offset.getOffsetNo(),
                    String.valueOf(offset.getPartyId()),
                    offset.getPartyName(),
                    offset.getOffsetAmount(),
                    null
            );
        } catch (Exception e) {
            log.error("调用财务模块创建对冲应收凭证失败: offsetNo={}, error={}", offset.getOffsetNo(), e.getMessage(), e);
        }

        try {
            // Record payable reduction (OUT flow showing payable settled)
            paymentAccountingService.postPaymentVoucher(
                    offset.getId(),
                    offset.getOffsetNo(),
                    String.valueOf(offset.getPartyId()),
                    offset.getPartyName(),
                    offset.getOffsetAmount(),
                    null
            );
        } catch (Exception e) {
            log.error("调用财务模块创建对冲应付凭证失败: offsetNo={}, error={}", offset.getOffsetNo(), e.getMessage(), e);
        }

        return offset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Offset cancelOffset(Long id, String reason) {
        Offset offset = getById(id);
        if (offset == null) {
            throw BusinessException.notFound("对冲单不存在");
        }
        if ("completed".equals(offset.getStatus())) {
            throw BusinessException.badRequest("已完成的对冲单不能取消");
        }

        offset.setStatus("cancelled");
        offset.setRemark(reason);
        updateById(offset);

        return offset;
    }

    @Override
    public List<OffsetItem> getItems(Long offsetId) {
        return offsetItemMapper.selectByOffsetId(offsetId);
    }

    @Override
    public String generateOffsetNo() {
        String prefix = "OF";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Offset> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Offset::getOffsetNo, prefix + dateStr)
                .eq(Offset::getDeleted, 0)
                .orderByDesc(Offset::getOffsetNo)
                .last("LIMIT 1");
        Offset last = getOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getOffsetNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }
}
