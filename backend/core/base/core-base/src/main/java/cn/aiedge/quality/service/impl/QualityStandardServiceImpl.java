package cn.aiedge.quality.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityStandard;
import cn.aiedge.quality.mapper.QualityStandardMapper;
import cn.aiedge.quality.service.QualityStandardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QualityStandardServiceImpl implements QualityStandardService {

    private static final String NO_PREFIX = "QSTD-";

    private final QualityStandardMapper mapper;

    @Override
    @Transactional
    public QualityStandard create(QualityStandard standard) {
        mapper.insert(standard);
        return standard;
    }

    @Override
    @Transactional
    public QualityStandard update(Long id, QualityStandard standard) {
        standard.setId(id);
        mapper.updateById(standard);
        return standard;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public PageResult<QualityStandard> page(Integer pageNum, Integer pageSize, String standardCode, String standardName, String inspectionType, Integer status) {
        LambdaQueryWrapper<QualityStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(hasText(standardCode), QualityStandard::getStandardCode, standardCode);
        wrapper.like(hasText(standardName), QualityStandard::getStandardName, standardName);
        wrapper.eq(inspectionType != null && !inspectionType.isEmpty(), QualityStandard::getInspectionType, inspectionType);
        wrapper.eq(status != null, QualityStandard::getStatus, status);
        wrapper.orderByDesc(QualityStandard::getCreateTime);

        Page<QualityStandard> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public QualityStandard get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<QualityStandard> listByType(String inspectionType) {
        LambdaQueryWrapper<QualityStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QualityStandard::getInspectionType, inspectionType);
        wrapper.eq(QualityStandard::getStatus, 1);
        return mapper.selectList(wrapper);
    }

    @Override
    public String generateNo() {
        return NO_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}