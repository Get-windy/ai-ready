package cn.aiedge.quality.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityInspection;
import cn.aiedge.quality.mapper.QualityInspectionMapper;
import cn.aiedge.quality.service.QualityInspectionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QualityInspectionServiceImpl implements QualityInspectionService {

    private final QualityInspectionMapper mapper;

    @Override
    @Transactional
    public QualityInspection create(QualityInspection inspection) {
        inspection.setInspectionResult("PENDING");
        mapper.insert(inspection);
        return inspection;
    }

    @Override
    @Transactional
    public void complete(Long id, String result, BigDecimal passQuantity, BigDecimal failQuantity) {
        QualityInspection inspection = mapper.selectById(id);
        if (inspection == null) {
            throw new IllegalArgumentException("检验记录不存在");
        }
        inspection.setInspectionResult(result);
        inspection.setPassQuantity(passQuantity);
        inspection.setFailQuantity(failQuantity);
        inspection.setInspectionTime(LocalDateTime.now());
        mapper.updateById(inspection);
    }

    @Override
    public PageResult<QualityInspection> page(Integer pageNum, Integer pageSize, String bizType, String result) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bizType != null, QualityInspection::getBizType, bizType);
        wrapper.eq(result != null, QualityInspection::getInspectionResult, result);
        wrapper.orderByDesc(QualityInspection::getCreateTime);

        Page<QualityInspection> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public QualityInspection get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<QualityInspection> listPending(String bizType) {
        LambdaQueryWrapper<QualityInspection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bizType != null, QualityInspection::getBizType, bizType);
        wrapper.eq(QualityInspection::getInspectionResult, "PENDING");
        return mapper.selectList(wrapper);
    }
}