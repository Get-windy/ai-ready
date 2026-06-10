package cn.aiedge.erp.expense.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.*;
import cn.aiedge.erp.expense.entity.FeeApplication;
import cn.aiedge.erp.expense.entity.FeeApplicationItem;
import cn.aiedge.erp.expense.entity.FeeApprovalRecord;
import cn.aiedge.erp.expense.mapper.FeeApplicationItemMapper;
import cn.aiedge.erp.expense.mapper.FeeApplicationMapper;
import cn.aiedge.erp.expense.mapper.FeeApprovalRecordMapper;
import cn.aiedge.erp.expense.service.FeeApplicationService;
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
 * 费用申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeApplicationServiceImpl extends ServiceImpl<FeeApplicationMapper, FeeApplication>
        implements FeeApplicationService {

    private final FeeApplicationItemMapper itemMapper;
    private final FeeApprovalRecordMapper approvalRecordMapper;

    @Override
    public PageResult<FeeApplicationVO> pageList(FeeApplicationQueryRequest request) {
        LambdaQueryWrapper<FeeApplication> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getApplicationNo())) {
            wrapper.like(FeeApplication::getApplicationNo, request.getApplicationNo());
        }
        if (StringUtils.hasText(request.getApplicationTitle())) {
            wrapper.like(FeeApplication::getApplicationTitle, request.getApplicationTitle());
        }
        if (request.getApplicantId() != null) {
            wrapper.eq(FeeApplication::getApplicantId, request.getApplicantId());
        }
        if (request.getDepartmentId() != null) {
            wrapper.eq(FeeApplication::getDepartmentId, request.getDepartmentId());
        }
        if (StringUtils.hasText(request.getExpenseType())) {
            wrapper.eq(FeeApplication::getExpenseType, request.getExpenseType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(FeeApplication::getStatus, request.getStatus());
        }
        if (request.getApplyDateStart() != null) {
            wrapper.ge(FeeApplication::getApplyDate, request.getApplyDateStart());
        }
        if (request.getApplyDateEnd() != null) {
            wrapper.le(FeeApplication::getApplyDate, request.getApplyDateEnd());
        }

        wrapper.orderByDesc(FeeApplication::getCreateTime);

        Page<FeeApplication> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<FeeApplication> result = this.page(page, wrapper);

        List<FeeApplicationVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public FeeApplicationVO getDetail(Long id) {
        FeeApplication entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用申请不存在");
        }
        return convertToDetailVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FeeApplicationCreateRequest request) {
        FeeApplication entity = new FeeApplication();
        BeanUtils.copyProperties(request, entity);

        // 生成申请单号
        entity.setApplicationNo(generateApplicationNo());
        entity.setStatus("DRAFT");
        entity.setTotalAmount(BigDecimal.ZERO);
        entity.setPaidAmount(BigDecimal.ZERO);
        entity.setReimbursedAmount(BigDecimal.ZERO);
        entity.setReimbursementStatus("NONE");
        entity.setPaymentStatus("NONE");
        entity.setApplicantId(SecurityUtils.getCurrentUserId());
        entity.setApplicantName(SecurityUtils.getCurrentUsername());
        entity.setCreateBy(SecurityUtils.getCurrentUserId());

        this.save(entity);

        // 保存明细项
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            int seq = 1;
            for (FeeApplicationItemRequest itemReq : request.getItems()) {
                FeeApplicationItem item = new FeeApplicationItem();
                BeanUtils.copyProperties(itemReq, item);
                item.setApplicationId(entity.getId());
                item.setTenantId(entity.getTenantId());
                if (item.getSequenceNumber() == null) {
                    item.setSequenceNumber(seq++);
                }
                // 计算含税金额
                calculateItemAmounts(item);
                item.setCreateBy(SecurityUtils.getCurrentUserId());
                itemMapper.insert(item);
                totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            }
            // 更新总金额
            entity.setTotalAmount(totalAmount);
            this.updateById(entity);
        }

        log.info("创建费用申请成功: id={}, no={}", entity.getId(), entity.getApplicationNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FeeApplicationCreateRequest request) {
        FeeApplication entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用申请不存在");
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的申请可以编辑");
        }

        copyNonNullProperties(request, entity, "id", "applicationNo", "status", "tenantId",
                "createBy", "createTime", "updateBy", "updateTime",
                "paidAmount", "reimbursedAmount", "reimbursementStatus", "paymentStatus");

        entity.setUpdateBy(SecurityUtils.getCurrentUserId());
        this.updateById(entity);

        // 删除旧明细，插入新明细
        LambdaQueryWrapper<FeeApplicationItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FeeApplicationItem::getApplicationId, id);
        itemMapper.delete(deleteWrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int seq = 1;
        if (request.getItems() != null) {
            for (FeeApplicationItemRequest itemReq : request.getItems()) {
                FeeApplicationItem item = new FeeApplicationItem();
                BeanUtils.copyProperties(itemReq, item);
                item.setApplicationId(entity.getId());
                item.setTenantId(entity.getTenantId());
                if (item.getSequenceNumber() == null) {
                    item.setSequenceNumber(seq++);
                }
                calculateItemAmounts(item);
                item.setCreateBy(SecurityUtils.getCurrentUserId());
                itemMapper.insert(item);
                totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            }
        }
        entity.setTotalAmount(totalAmount);
        this.updateById(entity);

        log.info("更新费用申请成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FeeApplication entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用申请不存在");
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的申请可以删除");
        }
        this.removeById(id);

        // 删除明细
        LambdaQueryWrapper<FeeApplicationItem> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FeeApplicationItem::getApplicationId, id);
        itemMapper.delete(deleteWrapper);

        log.info("删除费用申请成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        FeeApplication entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用申请不存在");
        }
        if (!"DRAFT".equals(entity.getStatus())) {
            throw BusinessException.badRequest("只有草稿状态的申请可以提交");
        }
        entity.setStatus("SUBMITTED");
        this.updateById(entity);

        // 创建提交审批记录
        FeeApprovalRecord record = new FeeApprovalRecord();
        record.setBusinessType("APPLICATION");
        record.setBusinessId(entity.getId());
        record.setApprovalLevel(1);
        record.setApprovalAction("SUBMIT");
        record.setApprovalTime(LocalDateTime.now());
        record.setPreviousStatus("DRAFT");
        record.setCurrentStatus("SUBMITTED");
        record.setCreateBy(SecurityUtils.getCurrentUserId());
        approvalRecordMapper.insert(record);

        log.info("提交费用申请审批: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        FeeApplication entity = this.getById(id);
        if (entity == null) {
            throw BusinessException.notFound("费用申请不存在");
        }
        if (!"SUBMITTED".equals(entity.getStatus()) && !"APPROVING".equals(entity.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许撤回");
        }
        String previousStatus = entity.getStatus();
        entity.setStatus("WITHDRAWN");
        this.updateById(entity);

        FeeApprovalRecord record = new FeeApprovalRecord();
        record.setBusinessType("APPLICATION");
        record.setBusinessId(entity.getId());
        record.setApprovalAction("WITHDRAW");
        record.setApprovalTime(LocalDateTime.now());
        record.setPreviousStatus(previousStatus);
        record.setCurrentStatus("WITHDRAWN");
        record.setCreateBy(SecurityUtils.getCurrentUserId());
        approvalRecordMapper.insert(record);

        log.info("撤回费用申请: id={}", id);
    }

    @Override
    public PageResult<FeeApplicationVO> getMyApplications(FeeApplicationQueryRequest request, Long userId) {
        request.setApplicantId(userId);
        return pageList(request);
    }

    @Override
    public PageResult<FeeApplicationVO> getMyPendingApprovals(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<FeeApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeApplication::getCurrentApproverId, userId)
               .in(FeeApplication::getStatus, "SUBMITTED", "APPROVING")
               .orderByDesc(FeeApplication::getCreateTime);

        Page<FeeApplication> page = new Page<>(pageNum, pageSize);
        Page<FeeApplication> result = this.page(page, wrapper);

        List<FeeApplicationVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }

    // ========== 私有方法 ==========

    private FeeApplicationVO convertToVO(FeeApplication entity) {
        FeeApplicationVO vo = new FeeApplicationVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private FeeApplicationVO convertToDetailVO(FeeApplication entity) {
        FeeApplicationVO vo = convertToVO(entity);

        // 查询明细项
        LambdaQueryWrapper<FeeApplicationItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FeeApplicationItem::getApplicationId, entity.getId())
                   .orderByAsc(FeeApplicationItem::getSequenceNumber);
        List<FeeApplicationItem> items = itemMapper.selectList(itemWrapper);
        vo.setItems(items.stream().map(this::convertItemToVO).collect(Collectors.toList()));

        // 查询审批记录
        LambdaQueryWrapper<FeeApprovalRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(FeeApprovalRecord::getBusinessType, "APPLICATION")
                     .eq(FeeApprovalRecord::getBusinessId, entity.getId())
                     .orderByAsc(FeeApprovalRecord::getApprovalLevel)
                     .orderByAsc(FeeApprovalRecord::getApprovalTime);
        List<FeeApprovalRecord> records = approvalRecordMapper.selectList(recordWrapper);
        vo.setApprovalRecords(records.stream().map(this::convertRecordToVO).collect(Collectors.toList()));

        return vo;
    }

    private FeeApplicationItemVO convertItemToVO(FeeApplicationItem item) {
        FeeApplicationItemVO vo = new FeeApplicationItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private FeeApprovalRecordVO convertRecordToVO(FeeApprovalRecord record) {
        FeeApprovalRecordVO vo = new FeeApprovalRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private void calculateItemAmounts(FeeApplicationItem item) {
        if (item.getAmount() == null) {
            item.setAmount(BigDecimal.ZERO);
        }
        if (item.getQuantity() == null) {
            item.setQuantity(BigDecimal.ONE);
        }
        if (item.getUnitPrice() == null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            item.setUnitPrice(item.getAmount().divide(item.getQuantity(), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (item.getTaxRate() == null) {
            item.setTaxRate(BigDecimal.ZERO);
        }
        item.setTaxAmount(item.getAmount().multiply(item.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP));
        item.setTotalAmountWithTax(item.getAmount().add(item.getTaxAmount()));
    }

    private String generateApplicationNo() {
        return "FE" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }

    /**
     * 拷贝非空属性，避免 request 中 null 字段覆盖数据库已有值
     */
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
