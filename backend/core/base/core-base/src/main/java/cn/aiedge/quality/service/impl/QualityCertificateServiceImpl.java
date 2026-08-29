package cn.aiedge.quality.service.impl;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.quality.entity.QualityCertificate;
import cn.aiedge.quality.mapper.QualityCertificateMapper;
import cn.aiedge.quality.service.QualityCertificateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class QualityCertificateServiceImpl implements QualityCertificateService {

    private final QualityCertificateMapper mapper;

    @Override
    @Transactional
    public QualityCertificate create(QualityCertificate certificate) {
        if (!StringUtils.hasText(certificate.getCertificateNo())) {
            certificate.setCertificateNo(generateCertificateNo());
        }
        mapper.insert(certificate);
        return certificate;
    }

    @Override
    @Transactional
    public QualityCertificate update(Long id, QualityCertificate certificate) {
        certificate.setId(id);
        mapper.updateById(certificate);
        return certificate;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public PageResult<QualityCertificate> page(Integer pageNum, Integer pageSize,
                                                String productName, String batchNo,
                                                String result, String startDate, String endDate) {
        LambdaQueryWrapper<QualityCertificate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(productName), QualityCertificate::getProductName, productName);
        wrapper.like(StringUtils.hasText(batchNo), QualityCertificate::getBatchNo, batchNo);
        wrapper.eq(StringUtils.hasText(result), QualityCertificate::getResult, result);

        if (StringUtils.hasText(startDate)) {
            wrapper.ge(QualityCertificate::getInspectionDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(QualityCertificate::getInspectionDate, LocalDate.parse(endDate));
        }

        wrapper.orderByDesc(QualityCertificate::getCreateTime);

        Page<QualityCertificate> page = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public QualityCertificate get(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 生成证书编号: COA-yyyyMMdd-xxxx
     */
    private String generateCertificateNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "COA-" + datePart + "-" + randomPart;
    }
}
