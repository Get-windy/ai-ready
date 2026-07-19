package cn.aiedge.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.finance.entity.OtherIncomeDoc;
import cn.aiedge.finance.mapper.OtherIncomeDocMapper;
import cn.aiedge.finance.service.OtherIncomeDocService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtherIncomeDocServiceImpl extends ServiceImpl<OtherIncomeDocMapper, OtherIncomeDoc>
        implements OtherIncomeDocService {

    @Override
    public Page<OtherIncomeDoc> pageList(String docNo, String incomeType, Integer status,
                                          LocalDate startDate, LocalDate endDate,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<OtherIncomeDoc> wrapper = new LambdaQueryWrapper<OtherIncomeDoc>()
                .like(docNo != null && !docNo.isEmpty(), OtherIncomeDoc::getDocNo, docNo)
                .eq(incomeType != null && !incomeType.isEmpty(), OtherIncomeDoc::getIncomeType, incomeType)
                .eq(status != null, OtherIncomeDoc::getStatus, status)
                .ge(startDate != null, OtherIncomeDoc::getIncomeDate, startDate)
                .le(endDate != null, OtherIncomeDoc::getIncomeDate, endDate)
                .orderByDesc(OtherIncomeDoc::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OtherIncomeDoc createDoc(OtherIncomeDoc doc) {
        if (doc.getDocNo() == null || doc.getDocNo().isBlank()) {
            doc.setDocNo(generateDocNo());
        }
        if (doc.getStatus() == null) doc.setStatus(0);
        if (doc.getCurrency() == null) doc.setCurrency("CNY");
        save(doc);
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDoc(Long id) {
        OtherIncomeDoc doc = getById(id);
        if (doc == null) throw BusinessException.notFound("单据不存在");
        if (doc.getStatus() != 0) throw new BusinessException("只有草稿状态可以提交");
        doc.setStatus(1);
        updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveDoc(Long id, Long approvedBy, String note) {
        OtherIncomeDoc doc = getById(id);
        if (doc == null) throw BusinessException.notFound("单据不存在");
        if (doc.getStatus() != 1) throw new BusinessException("只有待审核状态可以审批");
        doc.setStatus(2);
        doc.setApprovedBy(approvedBy);
        doc.setApprovedTime(LocalDateTime.now());
        doc.setApprovedNote(note);
        updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelDoc(Long id, String reason) {
        OtherIncomeDoc doc = getById(id);
        if (doc == null) throw BusinessException.notFound("单据不存在");
        if (doc.getStatus() >= 3) throw new BusinessException("已入账/已作废的单据不可取消");
        doc.setStatus(4);
        doc.setApprovedNote(reason);
        updateById(doc);
    }

    private String generateDocNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(new LambdaQueryWrapper<OtherIncomeDoc>()
                .apply("DATE(create_time) = {0}", LocalDate.now()));
        return "OI" + datePart + String.format("%04d", count + 1);
    }
}
