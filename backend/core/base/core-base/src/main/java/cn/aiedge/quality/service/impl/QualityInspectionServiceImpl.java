package cn.aiedge.quality.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.dto.QualityInspectionQuery;
import cn.aiedge.quality.entity.QualityInspection;
import cn.aiedge.quality.event.QualityInspectionCompletedEvent;
import cn.aiedge.quality.mapper.QualityInspectionMapper;
import cn.aiedge.quality.service.QualityInspectionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QualityInspectionServiceImpl implements QualityInspectionService {

    /** 质检单号前缀 */
    private static final String NO_PREFIX = "ZJD-";

    /** 合法检验结果（完成检验时限定，排除 PENDING 待检） */
    private static final Set<String> COMPLETE_RESULTS = Set.of("PASS", "CONCESSION", "FAIL");

    private final QualityInspectionMapper mapper;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public QualityInspection create(QualityInspection inspection) {
        if (inspection.getQuantity() == null || inspection.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.badRequest("检验数量不能为空且不能为负");
        }
        if (StringUtils.hasText(inspection.getQualityNo())) {
            QualityInspection exist = mapper.selectOne(
                    new LambdaQueryWrapper<QualityInspection>().eq(QualityInspection::getQualityNo, inspection.getQualityNo()));
            if (exist != null) {
                throw BusinessException.badRequest("质检单号已存在");
            }
        } else {
            inspection.setQualityNo(generateNo());
        }
        // 新建默认待检（质检单闭环：创建 → 待检 → 完成检验 → 不合格处理）
        inspection.setInspectionResult("PENDING");
        inspection.setStatus(0);
        mapper.insert(inspection);
        return inspection;
    }

    @Override
    @Transactional
    public QualityInspection updateInspection(Long id, QualityInspection inspection) {
        QualityInspection exist = mapper.selectById(id);
        if (exist == null) {
            throw BusinessException.notFound("检验记录不存在");
        }
        if (exist.getStatus() != null && exist.getStatus() != 0) {
            throw BusinessException.badRequest("仅待检状态的质检单可编辑");
        }
        exist.setBizNo(inspection.getBizNo());
        exist.setProductName(inspection.getProductName());
        exist.setBatchNo(inspection.getBatchNo());
        exist.setInspectionType(inspection.getInspectionType());
        exist.setQuantity(inspection.getQuantity());
        exist.setSampleQuantity(inspection.getSampleQuantity());
        exist.setInspectorId(inspection.getInspectorId());
        exist.setInspectorName(inspection.getInspectorName());
        exist.setInspectionTime(inspection.getInspectionTime());
        exist.setRemark(inspection.getRemark());
        if (inspection.getProductId() != null) {
            exist.setProductId(inspection.getProductId());
        }
        mapper.updateById(exist);
        return exist;
    }

    @Override
    public String generateNo() {
        // 前缀 + 日期 + UUID 前 6 位大写（与报损/报溢 single 号风格一致）
        String date = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return NO_PREFIX + date + rand;
    }

    @Override
    @Transactional
    public void complete(Long id, String result, BigDecimal passQuantity, BigDecimal failQuantity, String remark) {
        QualityInspection inspection = mapper.selectById(id);
        if (inspection == null) {
            throw BusinessException.notFound("检验记录不存在");
        }
        if (inspection.getStatus() != null && inspection.getStatus() == 2) {
            throw BusinessException.badRequest("已作废质检单不能完成检验");
        }
        if (result == null || !COMPLETE_RESULTS.contains(result)) {
            throw BusinessException.badRequest("检验结果不合法，仅支持 PASS/CONCESSION/FAIL");
        }
        if (passQuantity == null || failQuantity == null
                || passQuantity.compareTo(BigDecimal.ZERO) < 0
                || failQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.badRequest("合格/不合格数量不能为空且不能为负");
        }
        if (inspection.getQuantity() != null
                && passQuantity.add(failQuantity).compareTo(inspection.getQuantity()) > 0) {
            throw BusinessException.badRequest("合格与不合格数量之和不能超过检验数量");
        }
        inspection.setInspectionResult(result);
        inspection.setPassQuantity(passQuantity);
        inspection.setFailQuantity(failQuantity);
        inspection.setInspectionTime(LocalDateTime.now());
        inspection.setStatus(1);
        inspection.setRemark(remark);
        mapper.updateById(inspection);

        // 发布质检完成事件，由 wms/erp-stock 监听驱动库存放行/冻结
        eventPublisher.publishEvent(new QualityInspectionCompletedEvent(
                inspection.getId(),
                inspection.getQualityNo(),
                inspection.getBizType(),
                inspection.getBizNo(),
                inspection.getProductId(),
                inspection.getProductName(),
                inspection.getBatchNo(),
                inspection.getWarehouseId(),
                result,
                passQuantity,
                failQuantity));
    }

    @Override
    @Transactional
    public QualityInspection cancel(Long id) {
        QualityInspection inspection = mapper.selectById(id);
        if (inspection == null) {
            throw BusinessException.notFound("检验记录不存在");
        }
        if (inspection.getStatus() != null && inspection.getStatus() == 2) {
            throw BusinessException.badRequest("质检单已作废");
        }
        if (inspection.getStatus() != null && inspection.getStatus() == 1) {
            throw BusinessException.badRequest("已完成的质检单不能作废");
        }
        inspection.setStatus(2);
        mapper.updateById(inspection);
        return inspection;
    }

    @Override
    @Transactional
    public void deleteInspection(Long id) {
        QualityInspection exist = mapper.selectById(id);
        if (exist == null) {
            throw BusinessException.notFound("检验记录不存在");
        }
        if (exist.getStatus() != null && exist.getStatus() != 0) {
            throw BusinessException.badRequest("仅待检状态的质检单可删除");
        }
        mapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDeleteInspection(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            deleteInspection(id);
        }
    }

    @Override
    public PageResult<QualityInspection> page(QualityInspectionQuery query) {
        int pageNum = query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<QualityInspection> wrapper = buildWrapper(query);
        wrapper.orderByDesc(QualityInspection::getCreateTime);

        Page<QualityInspection> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    private LambdaQueryWrapper<QualityInspection> buildWrapper(QualityInspectionQuery query) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getBizType()), QualityInspection::getBizType, query.getBizType());
        wrapper.eq(StringUtils.hasText(query.getResult()), QualityInspection::getInspectionResult, query.getResult());
        wrapper.ne(StringUtils.hasText(query.getExcludeResult()), QualityInspection::getInspectionResult, query.getExcludeResult());
        wrapper.eq(StringUtils.hasText(query.getInspectionType()), QualityInspection::getInspectionType, query.getInspectionType());
        wrapper.eq(query.getStatus() != null, QualityInspection::getStatus, query.getStatus());
        wrapper.like(StringUtils.hasText(query.getBizNo()), QualityInspection::getBizNo, query.getBizNo());
        wrapper.like(StringUtils.hasText(query.getQualityNo()), QualityInspection::getQualityNo, query.getQualityNo());
        wrapper.like(StringUtils.hasText(query.getProductName()), QualityInspection::getProductName, query.getProductName());
        wrapper.like(StringUtils.hasText(query.getInspectorName()), QualityInspection::getInspectorName, query.getInspectorName());
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(QualityInspection::getQualityNo, query.getKeyword())
                    .or().like(QualityInspection::getBizNo, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getDateStart())) {
            wrapper.ge(QualityInspection::getInspectionTime, LocalDate.parse(query.getDateStart()).atStartOfDay());
        }
        if (StringUtils.hasText(query.getDateEnd())) {
            wrapper.le(QualityInspection::getInspectionTime, LocalDate.parse(query.getDateEnd()).atTime(LocalTime.MAX));
        }
        return wrapper;
    }

    @Override
    public QualityInspection get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public boolean hasPassed(String bizType, String bizNo, Long productId) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(bizType), QualityInspection::getBizType, bizType)
                .eq(StringUtils.hasText(bizNo), QualityInspection::getBizNo, bizNo)
                .eq(productId != null, QualityInspection::getProductId, productId)
                .ne(QualityInspection::getStatus, 2)
                .in(QualityInspection::getInspectionResult, List.of("PASS", "CONCESSION"));
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<QualityInspection> listPending(String bizType) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bizType != null, QualityInspection::getBizType, bizType);
        wrapper.eq(QualityInspection::getStatus, 0);
        return mapper.selectList(wrapper);
    }
}
