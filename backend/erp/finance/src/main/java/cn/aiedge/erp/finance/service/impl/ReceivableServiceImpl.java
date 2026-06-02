package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.mapper.ReceivableMapper;
import cn.aiedge.erp.finance.model.entity.Receivable;
import cn.aiedge.erp.finance.service.ReceivableService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应收账款Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceivableServiceImpl implements ReceivableService {

    private final ReceivableMapper receivableMapper;

    @Override
    @Transactional
    public ReceivableDTO create(ReceivableDTO dto) {
        Receivable entity = toEntity(dto);
        entity.setStatus("normal");
        entity.setPaidAmount(BigDecimal.ZERO);
        if (entity.getRemainingAmount() == null) {
            entity.setRemainingAmount(entity.getTotalAmount());
        }

        // 计算初始逾期天数
        if (entity.getDueDate() != null && entity.getDueDate().isBefore(LocalDate.now())) {
            entity.setStatus("overdue");
        }

        receivableMapper.insert(entity);
        log.info("创建应收账款: id={}, customer={}, amount={}",
                entity.getId(), entity.getCustomerName(), entity.getTotalAmount());
        return toDTO(entity);
    }

    @Override
    public ReceivableDTO getById(Long id) {
        Receivable entity = receivableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("应收账款不存在: " + id);
        }
        return toDTO(entity);
    }

    @Override
    public IPage<ReceivableDTO> list(String customerId, String status, Page<ReceivableDTO> page) {
        LambdaQueryWrapper<Receivable> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(customerId)) {
            wrapper.eq(Receivable::getCustomerId, customerId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Receivable::getStatus, status);
        }
        wrapper.orderByDesc(Receivable::getCreateTime);

        Page<Receivable> entityPage = receivableMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        Page<ReceivableDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(entityPage.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    public List<Map<String, Object>> getAgingAnalysis() {
        List<Receivable> allReceivables = receivableMapper.selectList(null).stream()
                .filter(r -> r.getDeletedFlag() == 0
                        && !"written_off".equals(r.getStatus())
                        && !"bad_debt".equals(r.getStatus()))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();

        long[][] buckets = {
                {0, 30},
                {31, 60},
                {61, 90},
                {91, 180},
                {181, Long.MAX_VALUE}
        };
        String[] bucketLabels = {"0-30天", "31-60天", "61-90天", "91-180天", "180天以上"};

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < buckets.length; i++) {
            long min = buckets[i][0];
            long max = buckets[i][1];
            String label = bucketLabels[i];

            List<Receivable> bucketItems = allReceivables.stream()
                    .filter(r -> {
                        if (r.getDueDate() == null) return false;
                        long days = ChronoUnit.DAYS.between(r.getDueDate(), today);
                        if (days < 0) return false;
                        return days >= min && days <= max;
                    })
                    .collect(Collectors.toList());

            long count = bucketItems.size();
            BigDecimal totalAmount = bucketItems.stream()
                    .map(r -> r.getRemainingAmount() != null ? r.getRemainingAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("agingPeriod", label);
            bucket.put("count", count);
            bucket.put("totalAmount", totalAmount);
            result.add(bucket);
        }

        return result;
    }

    @Override
    @Transactional
    public ReceivableDTO writeOff(Long id, BigDecimal amount) {
        Receivable entity = receivableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("应收账款不存在: " + id);
        }

        if ("written_off".equals(entity.getStatus()) || "bad_debt".equals(entity.getStatus())) {
            throw new RuntimeException("该应收账款已核销或已标记为坏账，无法再次核销");
        }

        BigDecimal writeOffAmount = amount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : amount;
        BigDecimal remaining = entity.getRemainingAmount() != null ? entity.getRemainingAmount() : BigDecimal.ZERO;

        if (writeOffAmount.compareTo(remaining) > 0) {
            throw new RuntimeException("核销金额不能超过剩余金额: remaining=" + remaining + ", writeOff=" + writeOffAmount);
        }

        entity.setPaidAmount((entity.getPaidAmount() != null ? entity.getPaidAmount() : BigDecimal.ZERO).add(writeOffAmount));
        entity.setRemainingAmount(remaining.subtract(writeOffAmount));

        if (entity.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            entity.setStatus("written_off");
        } else {
            entity.setStatus("partial");
        }

        receivableMapper.updateById(entity);
        log.info("核销应收账款: id={}, amount={}, remaining={}", id, writeOffAmount, entity.getRemainingAmount());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public ReceivableDTO markBadDebt(Long id) {
        Receivable entity = receivableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("应收账款不存在: " + id);
        }

        if ("written_off".equals(entity.getStatus())) {
            throw new RuntimeException("该应收账款已核销，无法再标记为坏账");
        }

        entity.setStatus("bad_debt");
        receivableMapper.updateById(entity);
        log.info("标记坏账: id={}, customer={}, amount={}", id, entity.getCustomerName(), entity.getRemainingAmount());
        return toDTO(entity);
    }

    // ======== DTO <-> Entity 转换 ========

    private ReceivableDTO toDTO(Receivable entity) {
        ReceivableDTO dto = new ReceivableDTO();
        dto.setId(entity.getId());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceId(entity.getSourceId());
        dto.setSourceNo(entity.getSourceNo());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerName(entity.getCustomerName());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPaidAmount(entity.getPaidAmount());
        dto.setRemainingAmount(entity.getRemainingAmount());
        dto.setDueDate(entity.getDueDate());
        dto.setInvoiceDate(entity.getInvoiceDate());
        dto.setInvoiceNo(entity.getInvoiceNo());
        dto.setStatus(entity.getStatus());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private Receivable toEntity(ReceivableDTO dto) {
        Receivable entity = new Receivable();
        entity.setSourceType(dto.getSourceType());
        entity.setSourceId(dto.getSourceId());
        entity.setSourceNo(dto.getSourceNo());
        entity.setCustomerId(dto.getCustomerId());
        entity.setCustomerName(dto.getCustomerName());
        entity.setTotalAmount(dto.getTotalAmount());
        entity.setPaidAmount(dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO);
        entity.setRemainingAmount(dto.getRemainingAmount());
        entity.setDueDate(dto.getDueDate());
        entity.setInvoiceDate(dto.getInvoiceDate());
        entity.setInvoiceNo(dto.getInvoiceNo());
        entity.setStatus(dto.getStatus());
        entity.setRemark(dto.getRemark());
        return entity;
    }
}
