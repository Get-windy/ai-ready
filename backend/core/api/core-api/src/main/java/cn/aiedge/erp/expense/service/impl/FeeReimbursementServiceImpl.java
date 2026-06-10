package cn.aiedge.erp.expense.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.*;
import cn.aiedge.erp.expense.entity.FeeReimbursement;
import cn.aiedge.erp.expense.entity.FeeReimbursementItem;
import cn.aiedge.erp.expense.entity.FeeApprovalRecord;
import cn.aiedge.erp.expense.mapper.FeeReimbursementItemMapper;
import cn.aiedge.erp.expense.mapper.FeeReimbursementMapper;
import cn.aiedge.erp.expense.mapper.FeeApprovalRecordMapper;
import cn.aiedge.erp.expense.service.FeeReimbursementService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * 费用报销服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeReimbursementServiceImpl extends ServiceImpl<FeeReimbursementMapper, FeeReimbursement>
        implements FeeReimbursementService {

    private final FeeReimbursementItemMapper itemMapper;
    private final FeeApprovalRecordMapper approvalRecordMapper;

    @Override
    public PageResult<FeeReimbursementVO> pageList(FeeReimbursementQueryRequest request) {
        LambdaQueryWrapper<FeeReimbursement> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getReimbursementNo())) {
            wrapper.like(FeeReimbursement::getReimbursementNo, request.getReimbursementNo());
        }
        if (StringUtils.hasText(request.getReimbursementTitle())) {
            wrapper.like(FeeReimbursement::getReimbursementTitle, request.getReimbursementTitle());
        }
        if (request.getApplicantId() != null) {
            wrapper.eq(FeeReimbursement::getApplicantId, request.getApplicantId());
        }
        if (request.getDepartmentId() != null) {
            wrapper.eq(FeeReimbursement::getDepartmentId, request.getDepartmentId());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(FeeReimbursement::getStatus, request.getStatus());
        }
        if (request.getApplicationId() != null) {
            wrapper.eq(FeeReimbursement::getApplicationId, request.getApplicationId());
        }
        if (request.getReimbursementDateStart() != null) {
            wrapper.ge(FeeReimbursement::getReimbursementDate, request.getReimbursementDateStart());
        }
        if (request.getReimbursementDateEnd() != null) {
            wrapper.le(FeeReimbursement::getReimbursementDate, request.getReimbursementDateEnd());
        }

        wrapper.orderByDesc(FeeReimbursement::getCreateTime);

        Page<FeeReimbursement> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<FeeReimbursement> result = this.page(page, wrapper);

        List<FeeReimbursementVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public FeeReimbursementVO getDetail(Long id) {
        FeeReimbursement entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用报销不存在");
        }
        return convertToDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FeeReimbursementCreateRequest request) {
        FeeReimbursement entity = new FeeReimbursement();
        BeanUtils.copyProperties(request, entity);

        entity.setReimbursementNo(generateReimbursementNo());
        entity.setStatus("DRAFT");
        entity.setTotalAmount(BigDecimal.ZERO);
        entity.setPaidAmount(BigDecimal.ZERO);
        entity.setPaymentStatus("NONE");
        entity.setApplicantId(SecurityUtils.getCurrentUserId());
        entity.setApplicantName(SecurityUtils.getCurrentUsername());
        entity.setCreateBy(SecurityUtils.getCurrentUserId());

        this.save(entity);

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            int seq = 1;
            for (FeeReimbursementItemRequest itemReq : request.getItems()) {
                FeeReimbursementItem item = new FeeReimbursementItem();
                BeanUtils.copyProperties(itemReq, item);
                item.setReimbursementId(entity.getId());
                item.setTenantId(entity.getTenantId());
                if (item.getSequenceNumber() == null) {
                    item.setSequenceNumber(seq++);
                }
                if (item.getAmount() == null) {
                    item.setAmount(BigDecimal.ZERO);
                }
                item.setCreateBy(SecurityUtils.getCurrentUserId());
                itemMapper.insert(item);
                totalAmount = totalAmount.add(item.getAmount());
            }
            entity.setTotalAmount(totalAmount);
            this.updateById(entity);
        }

        log.info("创建费用报销成功: id={}, no={}", entity.getId(), entity.getReimbursementNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FeeReimbursementCreateRequest request) {
        FeeReimbursement entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用报销不存在");
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的报销单可以编辑");
        }

        copyNonNullProperties(request, entity, "id", "reimbursementNo", "status", "tenantId",
                "createBy", "createTime", "updateBy", "updateTime", "paidAmount", "paymentStatus");

        entity.setUpdateBy(SecurityUtils.getCurrentUserId());
        this.updateById(entity);

        LambdaQueryWrapper<FeeReimbursementItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FeeReimbursementItem::getReimbursementId, id);
        itemMapper.delete(deleteWrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int seq = 1;
        if (request.getItems() != null) {
            for (FeeReimbursementItemRequest itemReq : request.getItems()) {
                FeeReimbursementItem item = new FeeReimbursementItem();
                BeanUtils.copyProperties(itemReq, item);
                item.setReimbursementId(entity.getId());
                if (item.getSequenceNumber() == null) {
                    item.setSequenceNumber(seq++);
                }
                if (item.getAmount() == null) {
                    item.setAmount(BigDecimal.ZERO);
                }
                item.setCreateBy(SecurityUtils.getCurrentUserId());
                itemMapper.insert(item);
                totalAmount = totalAmount.add(item.getAmount());
            }
        }
        entity.setTotalAmount(totalAmount);
        this.updateById(entity);

        log.info("更新费用报销成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FeeReimbursement entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用报销不存在");
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的报销单可以删除");
        }
        this.removeById(id);

        LambdaQueryWrapper<FeeReimbursementItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FeeReimbursementItem::getReimbursementId, id);
        itemMapper.delete(deleteWrapper);

        log.info("删除费用报销成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        FeeReimbursement entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用报销不存在");
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的报销单可以提交");
        }
        entity.setStatus("SUBMITTED");
        this.updateById(entity);

        FeeApprovalRecord record = new FeeApprovalRecord();
        record.setBusinessType("REIMBURSEMENT");
        record.setBusinessId(entity.getId());
        record.setApprovalLevel(1);
        record.setApprovalAction("SUBMIT");
        record.setApprovalTime(LocalDateTime.now());
        record.setPreviousStatus("DRAFT");
        record.setCurrentStatus("SUBMITTED");
        record.setCreateBy(SecurityUtils.getCurrentUserId());
        approvalRecordMapper.insert(record);

        log.info("提交费用报销审批: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        FeeReimbursement entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用报销不存在");
        }
        if (!"SUBMITTED".equals(entity.getStatus()) && !"APPROVING".equals(entity.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许撤回");
        }
        String previousStatus = entity.getStatus();
        entity.setStatus("WITHDRAWN");
        this.updateById(entity);

        FeeApprovalRecord record = new FeeApprovalRecord();
        record.setBusinessType("REIMBURSEMENT");
        record.setBusinessId(entity.getId());
        record.setApprovalAction("WITHDRAW");
        record.setApprovalTime(LocalDateTime.now());
        record.setPreviousStatus(previousStatus);
        record.setCurrentStatus("WITHDRAWN");
        record.setCreateBy(SecurityUtils.getCurrentUserId());
        approvalRecordMapper.insert(record);

        log.info("撤回费用报销: id={}", id);
    }

    // ========== 私有方法 ==========

    private FeeReimbursementVO convertToVO(FeeReimbursement entity) {
        FeeReimbursementVO vo = new FeeReimbursementVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private FeeReimbursementVO convertToDetailVO(FeeReimbursement entity) {
        FeeReimbursementVO vo = convertToVO(entity);

        LambdaQueryWrapper<FeeReimbursementItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FeeReimbursementItem::getReimbursementId, entity.getId())
                   .orderByAsc(FeeReimbursementItem::getSequenceNumber);
        List<FeeReimbursementItem> items = itemMapper.selectList(itemWrapper);
        vo.setItems(items.stream().map(this::convertItemToVO).collect(Collectors.toList()));

        LambdaQueryWrapper<FeeApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(FeeApprovalRecord::getBusinessType, "REIMBURSEMENT")
                     .eq(FeeApprovalRecord::getBusinessId, entity.getId())
                     .orderByAsc(FeeApprovalRecord::getApprovalLevel)
                     .orderByAsc(FeeApprovalRecord::getApprovalTime);
        List<FeeApprovalRecord> records = approvalRecordMapper.selectList(recordWrapper);
        vo.setApprovalRecords(records.stream().map(this::convertRecordToVO).collect(Collectors.toList()));

        return vo;
    }

    private FeeReimbursementItemVO convertItemToVO(FeeReimbursementItem item) {
        FeeReimbursementItemVO vo = new FeeReimbursementItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private FeeApprovalRecordVO convertRecordToVO(FeeApprovalRecord record) {
        FeeApprovalRecordVO vo = new FeeApprovalRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private String generateReimbursementNo() {
        return "FR" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }

    private void copyNonNullProperties(Object source, Object target, String... ignoreProperties) {
        Set<String> ignoreSet = new HashSet<>(Arrays.asList(ignoreProperties));
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            String name = pd.getName();
            if ("class".equals(name) || ignoreSet.contains(name)) continue;
            Object value = src.getPropertyValue(name);
            if (value != null) {
                new BeanWrapperImpl(target).setPropertyValue(name, value);
            }
        }
    }
}
