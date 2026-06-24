package cn.aiedge.quality.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityDefectHandle;
import cn.aiedge.quality.mapper.QualityDefectHandleMapper;
import cn.aiedge.quality.service.QualityDefectHandleService;
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
public class QualityDefectHandleServiceImpl implements QualityDefectHandleService {

    private final QualityDefectHandleMapper mapper;

    @Override
    @Transactional
    public QualityDefectHandle create(Long inspectionId, String defectType, String defectDesc, BigDecimal defectQuantity) {
        QualityDefectHandle handle = new QualityDefectHandle();
        handle.setInspectionId(inspectionId);
        handle.setDefectType(defectType);
        handle.setDefectDesc(defectDesc);
        handle.setHandleQuantity(defectQuantity);
        handle.setStatus(0);
        mapper.insert(handle);
        return handle;
    }

    @Override
    @Transactional
    public void handle(Long id, String handleType, BigDecimal handleQuantity, String handleResult) {
        QualityDefectHandle record = mapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("处理记录不存在");
        }
        record.setHandleType(handleType);
        record.setHandleQuantity(handleQuantity);
        record.setHandleResult(handleResult);
        record.setHandleTime(LocalDateTime.now());
        record.setStatus(1);
        mapper.updateById(record);
    }

    @Override
    public PageResult<QualityDefectHandle> page(Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<QualityDefectHandle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, QualityDefectHandle::getStatus, status);
        wrapper.orderByDesc(QualityDefectHandle::getCreateTime);

        Page<QualityDefectHandle> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public QualityDefectHandle get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<QualityDefectHandle> listPending() {
        LambdaQueryWrapper<QualityDefectHandle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QualityDefectHandle::getStatus, 0);
        return mapper.selectList(wrapper);
    }
}