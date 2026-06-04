package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.mapper.PayableMapper;
import cn.aiedge.erp.finance.model.entity.Payable;
import cn.aiedge.erp.finance.service.PayableService;
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
 * 应付账款Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayableServiceImpl implements PayableService {

    private final PayableMapper payableMapper;

    @Override
    @Transactional
    public PayableDTO create(PayableDTO dto) {
        Payable entity = toEntity(dto);
        entity.setStatus("normal");
        entity.setPaidAmount(BigDecimal.ZERO);
        if (entity.getRemainingAmount() == null) {
            entity.setRemainingAmount(entity.getTotalAmount());
        }

        if (entity.getDueDate() != null && entity.getDueDate().isBefore(LocalDate.now())) {
            entity.setStatus("overdue");
        }

        payableMapper.insert(entity);
        log.info("创建应付账款: id={}, supplier={}, amount={}",
                entity.getId(), entity.getSupplierName(), entity.getTotalAmount());
        return toDTO(entity);
    }

    @Override
    public PayableDTO getById(Long id) {
        Payable entity = payableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("应付账款不存在: " + id);
        }
        return toDTO(entity);
    }

    @Override
    public IPage<PayableDTO> list(String supplierId, String status, Page<PayableDTO> page) {
        LambdaQueryWrapper<Payable> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(Payable::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Payable::getStatus, status);
        }
        wrapper.orderByDesc(Payable::getCreateTime);

        Page<Payable> entityPage = payableMapper.selectPage(new Page<>(page.getCurrent(), page.getSize()), wrapper);
        Page<PayableDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(entityPage.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    public List<Map<String, Object>> getAgingAnalysis() {
        List<Payable> allPayables = payableMapper.selectList(null).stream()
                .filter(r -> r.getDeletedFlag() == 0 && !"written_off".equals(r.getStatus()))
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

            List<Payable> bucketItems = allPayables.stream()
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
    public PayableDTO writeOff(Long id, BigDecimal amount) {
        Payable entity = payableMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("应付账款不存在: " + id);
        }

        if ("written_off".equals(entity.getStatus())) {
            throw new RuntimeException("该应付账款已核销，无法再次核销");
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

        payableMapper.updateById(entity);
        log.info("核销应付账款: id={}, amount={}, remaining={}", id, writeOffAmount, entity.getRemainingAmount());
        return toDTO(entity);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        payableMapper.deleteBatchIds(ids);
        log.info("批量删除应付账款: ids={}", ids);
    }

    @Override
    public List<PayableDTO> exportList(String supplierId, String status) {
        LambdaQueryWrapper<Payable> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(supplierId)) {
            wrapper.eq(Payable::getSupplierId, supplierId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Payable::getStatus, status);
        }
        wrapper.orderByDesc(Payable::getCreateTime);

        List<Payable> entities = payableMapper.selectList(wrapper);
        return entities.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ======== DTO <-> Entity 转换 ========

    private PayableDTO toDTO(Payable entity) {
        PayableDTO dto = new PayableDTO();
        dto.setId(entity.getId());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceId(entity.getSourceId());
        dto.setSourceNo(entity.getSourceNo());
        dto.setSupplierId(entity.getSupplierId());
        dto.setSupplierName(entity.getSupplierName());
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

    private Payable toEntity(PayableDTO dto) {
        Payable entity = new Payable();
        entity.setSourceType(dto.getSourceType());
        entity.setSourceId(dto.getSourceId());
        entity.setSourceNo(dto.getSourceNo());
        entity.setSupplierId(dto.getSupplierId());
        entity.setSupplierName(dto.getSupplierName());
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
