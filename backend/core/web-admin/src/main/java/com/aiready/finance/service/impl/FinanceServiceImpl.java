package com.aiready.finance.service.impl;

import com.aiready.finance.dto.*;
import com.aiready.finance.entity.*;
import com.aiready.finance.mapper.*;
import com.aiready.finance.service.FinanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceServiceImpl extends ServiceImpl<AccountSubjectMapper, AccountSubject> implements FinanceService {

    private final AccountSubjectMapper accountSubjectMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherItemMapper voucherItemMapper;
    private final LedgerMapper ledgerMapper;

    // ==================== 会计科目管理 ====================

    @Override
    @Transactional
    public AccountSubjectDTO createSubject(AccountSubjectSaveRequest request, Long operatorId) {
        // 验证科目编码唯一性
        if (!validateSubjectCode(request.getSubjectCode(), null)) {
            throw new RuntimeException("科目编码已存在：" + request.getSubjectCode());
        }

        // 创建科目
        AccountSubject subject = new AccountSubject();
        BeanUtils.copyProperties(request, subject);
        subject.setCurrentBalance(request.getOpeningBalance() != null ? request.getOpeningBalance() : BigDecimal.ZERO);
        subject.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        subject.setIsDetail(request.getIsDetail() != null ? request.getIsDetail() : 1);
        subject.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        subject.setCreateBy(operatorId);
        subject.setUpdateBy(operatorId);

        accountSubjectMapper.insert(subject);

        return convertToSubjectDTO(subject);
    }

    @Override
    @Transactional
    public AccountSubjectDTO updateSubject(Long subjectId, AccountSubjectSaveRequest request, Long operatorId) {
        // 验证科目编码唯一性
        if (!validateSubjectCode(request.getSubjectCode(), subjectId)) {
            throw new RuntimeException("科目编码已存在：" + request.getSubjectCode());
        }

        // 更新科目
        AccountSubject subject = accountSubjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new RuntimeException("科目不存在，ID：" + subjectId);
        }

        BeanUtils.copyProperties(request, subject);
        subject.setId(subjectId);
        subject.setUpdateBy(operatorId);

        accountSubjectMapper.updateById(subject);

        return convertToSubjectDTO(subject);
    }

    @Override
    @Transactional
    public void deleteSubject(Long subjectId, Long operatorId) {
        AccountSubject subject = accountSubjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new RuntimeException("科目不存在，ID：" + subjectId);
        }

        // 检查是否有子科目
        List<AccountSubject> children = accountSubjectMapper.selectByParentId(subjectId);
        if (!children.isEmpty()) {
            throw new RuntimeException("该科目存在子科目，无法删除");
        }

        accountSubjectMapper.deleteById(subjectId);
    }

    @Override
    @Transactional
    public void batchDeleteSubjects(List<Long> subjectIds, Long operatorId) {
        for (Long subjectId : subjectIds) {
            deleteSubject(subjectId, operatorId);
        }
    }

    @Override
    public AccountSubjectDTO getSubjectById(Long subjectId) {
        AccountSubject subject = accountSubjectMapper.selectById(subjectId);
        if (subject == null) {
            return null;
        }
        return convertToSubjectDTO(subject);
    }

    @Override
    public AccountSubjectDTO getSubjectByCode(String subjectCode) {
        AccountSubject subject = accountSubjectMapper.selectBySubjectCode(subjectCode);
        if (subject == null) {
            return null;
        }
        return convertToSubjectDTO(subject);
    }

    @Override
    public IPage<AccountSubjectDTO> querySubjects(AccountSubjectQueryRequest request) {
        LambdaQueryWrapper<AccountSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSubject::getDeleted, 0);
        
        if (request.getSubjectCode() != null && !request.getSubjectCode().isEmpty()) {
            wrapper.like(AccountSubject::getSubjectCode, request.getSubjectCode());
        }
        if (request.getSubjectName() != null && !request.getSubjectName().isEmpty()) {
            wrapper.like(AccountSubject::getSubjectName, request.getSubjectName());
        }
        if (request.getSubjectType() != null) {
            wrapper.eq(AccountSubject::getSubjectType, request.getSubjectType());
        }
        if (request.getParentId() != null) {
            wrapper.eq(AccountSubject::getParentId, request.getParentId());
        }
        if (request.getIsDetail() != null) {
            wrapper.eq(AccountSubject::getIsDetail, request.getIsDetail());
        }
        if (request.getStatus() != null) {
            wrapper.eq(AccountSubject::getStatus, request.getStatus());
        }
        
        wrapper.orderByAsc(AccountSubject::getSubjectCode);
        
        Page<AccountSubject> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<AccountSubject> subjectPage = accountSubjectMapper.selectPage(page, wrapper);
        
        return subjectPage.convert(this::convertToSubjectDTO);
    }

    @Override
    public List<AccountSubjectDTO> getSubjectTree() {
        List<AccountSubject> allSubjects = accountSubjectMapper.selectList(
            new LambdaQueryWrapper<AccountSubject>().eq(AccountSubject::getDeleted, 0)
        );
        
        // 构建树形结构
        Map<Long, AccountSubjectDTO> dtoMap = new HashMap<>();
        List<AccountSubjectDTO> rootList = new ArrayList<>();
        
        // 先转换为DTO
        for (AccountSubject subject : allSubjects) {
            dtoMap.put(subject.getId(), convertToSubjectDTO(subject));
        }
        
        // 构建树
        for (AccountSubjectDTO dto : dtoMap.values()) {
            if (dto.getParentId() == null) {
                rootList.add(dto);
            } else {
                AccountSubjectDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }
        
        // 排序
        rootList.sort(Comparator.comparing(AccountSubjectDTO::getSortOrder));
        
        return rootList;
    }

    @Override
    public List<AccountSubjectDTO> getDetailSubjects() {
        List<AccountSubject> subjects = accountSubjectMapper.selectDetailSubjects();
        return subjects.stream().map(this::convertToSubjectDTO).collect(Collectors.toList());
    }

    @Override
    public boolean validateSubjectCode(String subjectCode, Long excludeId) {
        Integer count = accountSubjectMapper.checkSubjectCodeExists(subjectCode, excludeId);
        return count == null || count == 0;
    }

    private AccountSubjectDTO convertToSubjectDTO(AccountSubject subject) {
        AccountSubjectDTO dto = new AccountSubjectDTO();
        BeanUtils.copyProperties(subject, dto);
        
        // 设置描述
        dto.setSubjectTypeDesc(getSubjectTypeDesc(subject.getSubjectType()));
        dto.setBalanceDirectionDesc(getBalanceDirectionDesc(subject.getBalanceDirection()));
        dto.setStatusDesc(subject.getStatus() != null && subject.getStatus() == 1 ? "启用" : "禁用");
        
        return dto;
    }

    private String getSubjectTypeDesc(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "资产";
            case 2: return "负债";
            case 3: return "权益";
            case 4: return "成本";
            case 5: return "损益";
            default: return "";
        }
    }

    private String getBalanceDirectionDesc(Integer direction) {
        if (direction == null) return "";
        return direction == 1 ? "借" : "贷";
    }

    // ==================== 凭证管理 ====================

    @Override
    @Transactional
    public VoucherDTO createVoucher(VoucherSaveRequest request, Long operatorId) {
        // 验证借贷平衡
        BigDecimal totalDebit = request.getItems().stream()
            .map(VoucherItemSaveRequest::getDebitAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = request.getItems().stream()
            .map(VoucherItemSaveRequest::getCreditAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("借贷不平衡，借方合计：" + totalDebit + "，贷方合计：" + totalCredit);
        }

        // 创建凭证
        Voucher voucher = new Voucher();
        voucher.setVoucherDate(request.getVoucherDate());
        voucher.setAccountingPeriod(request.getVoucherDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        voucher.setVoucherType(request.getVoucherType());
        voucher.setSummary(request.getSummary());
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setAttachmentCount(request.getAttachmentCount() != null ? request.getAttachmentCount() : 0);
        voucher.setCreatorId(operatorId);
        voucher.setStatus(1); // 待审核
        voucher.setSourceType(request.getSourceType());
        voucher.setSourceId(request.getSourceId());
        voucher.setRemark(request.getRemark());
        voucher.setCreateBy(operatorId);
        voucher.setUpdateBy(operatorId);

        voucherMapper.insert(voucher);

        // 生成凭证字号
        String voucherNo = voucherMapper.generateVoucherNo(request.getVoucherDate());
        voucher.setVoucherNo(voucherNo);
        voucherMapper.updateById(voucher);

        // 创建凭证分录
        createVoucherItems(voucher.getId(), request.getItems(), operatorId);

        return getVoucherById(voucher.getId());
    }

    @Override
    @Transactional
    public VoucherDTO updateVoucher(Long voucherId, VoucherSaveRequest request, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在，ID：" + voucherId);
        }

        // 只能修改未审核的凭证
        if (voucher.getStatus() != null && voucher.getStatus() > 1) {
            throw new RuntimeException("已审核或已记账的凭证不能修改");
        }

        // 验证借贷平衡
        BigDecimal totalDebit = request.getItems().stream()
            .map(VoucherItemSaveRequest::getDebitAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = request.getItems().stream()
            .map(VoucherItemSaveRequest::getCreditAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new RuntimeException("借贷不平衡");
        }

        // 更新凭证
        voucher.setVoucherDate(request.getVoucherDate());
        voucher.setAccountingPeriod(request.getVoucherDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        voucher.setVoucherType(request.getVoucherType());
        voucher.setSummary(request.getSummary());
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setAttachmentCount(request.getAttachmentCount() != null ? request.getAttachmentCount() : 0);
        voucher.setRemark(request.getRemark());
        voucher.setUpdateBy(operatorId);

        voucherMapper.updateById(voucher);

        // 删除旧分录，创建新分录
        voucherItemMapper.deleteByVoucherId(voucherId);
        createVoucherItems(voucherId, request.getItems(), operatorId);

        return getVoucherById(voucherId);
    }

    private void createVoucherItems(Long voucherId, List<VoucherItemSaveRequest> items, Long operatorId) {
        int itemNo = 1;
        for (VoucherItemSaveRequest itemReq : items) {
            VoucherItem item = new VoucherItem();
            item.setVoucherId(voucherId);
            item.setItemNo(itemNo++);
            item.setSubjectId(itemReq.getSubjectId());
            
            // 获取科目信息
            AccountSubject subject = accountSubjectMapper.selectById(itemReq.getSubjectId());
            if (subject != null) {
                item.setSubjectCode(subject.getSubjectCode());
                item.setSubjectName(subject.getSubjectName());
            }
            
            item.setSummary(itemReq.getSummary());
            item.setDebitAmount(itemReq.getDebitAmount() != null ? itemReq.getDebitAmount() : BigDecimal.ZERO);
            item.setCreditAmount(itemReq.getCreditAmount() != null ? itemReq.getCreditAmount() : BigDecimal.ZERO);
            item.setCustomerId(itemReq.getCustomerId());
            item.setSupplierId(itemReq.getSupplierId());
            item.setDepartmentId(itemReq.getDepartmentId());
            item.setProjectId(itemReq.getProjectId());
            item.setEmployeeId(itemReq.getEmployeeId());
            item.setCurrency(itemReq.getCurrency());
            item.setForeignAmount(itemReq.getForeignAmount());
            item.setExchangeRate(itemReq.getExchangeRate());
            item.setCreateBy(operatorId);
            item.setUpdateBy(operatorId);
            
            voucherItemMapper.insert(item);
        }
    }

    @Override
    @Transactional
    public void deleteVoucher(Long voucherId, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        
        if (voucher.getStatus() != null && voucher.getStatus() > 1) {
            throw new RuntimeException("已审核或已记账的凭证不能删除");
        }

        voucherMapper.deleteById(voucherId);
        voucherItemMapper.deleteByVoucherId(voucherId);
    }

    @Override
    public VoucherDTO getVoucherById(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            return null;
        }
        
        VoucherDTO dto = convertToVoucherDTO(voucher);
        
        // 查询分录
        List<VoucherItem> items = voucherItemMapper.selectByVoucherId(voucherId);
        dto.setItems(items.stream().map(this::convertToVoucherItemDTO).collect(Collectors.toList()));
        
        return dto;
    }

    @Override
    public IPage<VoucherDTO> queryVouchers(VoucherQueryRequest request) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getDeleted, 0);
        
        if (request.getVoucherNo() != null && !request.getVoucherNo().isEmpty()) {
            wrapper.like(Voucher::getVoucherNo, request.getVoucherNo());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(Voucher::getVoucherDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Voucher::getVoucherDate, request.getEndDate());
        }
        if (request.getAccountingPeriod() != null && !request.getAccountingPeriod().isEmpty()) {
            wrapper.eq(Voucher::getAccountingPeriod, request.getAccountingPeriod());
        }
        if (request.getVoucherType() != null) {
            wrapper.eq(Voucher::getVoucherType, request.getVoucherType());
        }
        if (request.getCreatorId() != null) {
            wrapper.eq(Voucher::getCreatorId, request.getCreatorId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Voucher::getStatus, request.getStatus());
        }
        if (request.getSourceType() != null && !request.getSourceType().isEmpty()) {
            wrapper.eq(Voucher::getSourceType, request.getSourceType());
        }
        if (request.getSummaryKeyword() != null && !request.getSummaryKeyword().isEmpty()) {
            wrapper.like(Voucher::getSummary, request.getSummaryKeyword());
        }
        
        wrapper.orderByDesc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        
        Page<Voucher> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<Voucher> voucherPage = voucherMapper.selectPage(page, wrapper);
        
        return voucherPage.convert(this::convertToVoucherDTO);
    }

    @Override
    @Transactional
    public void reviewVoucher(Long voucherId, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        
        if (voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new RuntimeException("只有待审核的凭证可以审核");
        }

        voucherMapper.reviewVoucher(voucherId, operatorId);
    }

    @Override
    @Transactional
    public void cancelReview(Long voucherId, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        
        if (voucher.getStatus() == null || voucher.getStatus() != 2) {
            throw new RuntimeException("只有已审核的凭证可以取消审核");
        }

        voucherMapper.updateStatus(voucherId, 1, operatorId);
    }

    @Override
    @Transactional
    public void bookkeeping(Long voucherId, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        
        if (voucher.getStatus() == null || voucher.getStatus() != 2) {
            throw new RuntimeException("只有已审核的凭证可以记账");
        }

        // 记账
        voucherMapper.bookkeeping(voucherId, operatorId);

        // 生成账簿记录
        generateLedgerRecords(voucherId);
    }

    private void generateLedgerRecords(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        List<VoucherItem> items = voucherItemMapper.selectByVoucherId(voucherId);
        
        for (VoucherItem item : items) {
            Ledger ledger = new Ledger();
            ledger.setLedgerType(1); // 明细账
            ledger.setSubjectId(item.getSubjectId());
            ledger.setSubjectCode(item.getSubjectCode());
            ledger.setSubjectName(item.getSubjectName());
            ledger.setAccountingPeriod(voucher.getAccountingPeriod());
            ledger.setBusinessDate(voucher.getVoucherDate());
            ledger.setVoucherId(voucherId);
            ledger.setVoucherNo(voucher.getVoucherNo());
            ledger.setVoucherItemId(item.getId());
            ledger.setSummary(item.getSummary());
            ledger.setDebitAmount(item.getDebitAmount());
            ledger.setCreditAmount(item.getCreditAmount());
            ledger.setCustomerId(item.getCustomerId());
            ledger.setSupplierId(item.getSupplierId());
            ledger.setDepartmentId(item.getDepartmentId());
            ledger.setProjectId(item.getProjectId());
            
            // 计算余额
            AccountSubject subject = accountSubjectMapper.selectById(item.getSubjectId());
            if (subject != null) {
                ledger.setBalanceDirection(subject.getBalanceDirection());
                // 简化计算，实际需要更复杂的逻辑
                BigDecimal balance = subject.getCurrentBalance();
                if (subject.getBalanceDirection() == 1) { // 借方
                    balance = balance.add(item.getDebitAmount()).subtract(item.getCreditAmount());
                } else {
                    balance = balance.add(item.getCreditAmount()).subtract(item.getDebitAmount());
                }
                ledger.setBalance(balance);
                
                // 更新科目余额
                subject.setCurrentBalance(balance);
                accountSubjectMapper.updateById(subject);
            }
            
            ledger.setCreateBy(voucher.getCreateBy());
            ledger.setUpdateBy(voucher.getUpdateBy());
            
            ledgerMapper.insert(ledger);
        }
    }

    @Override
    @Transactional
    public void cancelBookkeeping(Long voucherId, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        
        if (voucher.getStatus() == null || voucher.getStatus() != 3) {
            throw new RuntimeException("只有已记账的凭证可以取消记账");
        }

        // 删除账簿记录
        ledgerMapper.deleteByVoucherId(voucherId);
        
        // 更新凭证状态
        voucherMapper.updateStatus(voucherId, 2, operatorId);
    }

    @Override
    @Transactional
    public void cancelVoucher(Long voucherId, Long operatorId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new RuntimeException("凭证不存在");
        }
        
        if (voucher.getStatus() == null || voucher.getStatus() >= 3) {
            throw new RuntimeException("已记账的凭证需要先取消记账才能作废");
        }

        voucherMapper.cancelVoucher(voucherId, operatorId);
    }

    private VoucherDTO convertToVoucherDTO(Voucher voucher) {
        VoucherDTO dto = new VoucherDTO();
        BeanUtils.copyProperties(voucher, dto);
        
        // 设置描述
        dto.setVoucherTypeDesc(getVoucherTypeDesc(voucher.getVoucherType()));
        dto.setStatusDesc(getVoucherStatusDesc(voucher.getStatus()));
        
        return dto;
    }

    private VoucherItemDTO convertToVoucherItemDTO(VoucherItem item) {
        VoucherItemDTO dto = new VoucherItemDTO();
        BeanUtils.copyProperties(item, dto);
        return dto;
    }

    private String getVoucherTypeDesc(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "收款";
            case 2: return "付款";
            case 3: return "转账";
            default: return "";
        }
    }

    private String getVoucherStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待审核";
            case 2: return "已审核";
            case 3: return "已记账";
            case 4: return "已作废";
            default: return "";
        }
    }

    // ==================== 账簿管理 ====================

    @Override
    public IPage<LedgerDTO> queryDetailLedger(LedgerQueryRequest request) {
        LambdaQueryWrapper<Ledger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ledger::getDeleted, 0);
        wrapper.eq(Ledger::getLedgerType, 1); // 明细账
        
        if (request.getSubjectId() != null) {
            wrapper.eq(Ledger::getSubjectId, request.getSubjectId());
        }
        if (request.getAccountingPeriod() != null && !request.getAccountingPeriod().isEmpty()) {
            wrapper.eq(Ledger::getAccountingPeriod, request.getAccountingPeriod());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(Ledger::getBusinessDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Ledger::getBusinessDate, request.getEndDate());
        }
        
        wrapper.orderByAsc(Ledger::getBusinessDate, Ledger::getId);
        
        Page<Ledger> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<Ledger> ledgerPage = ledgerMapper.selectPage(page, wrapper);
        
        return ledgerPage.convert(this::convertToLedgerDTO);
    }

    @Override
    public IPage<LedgerDTO> queryGeneralLedger(LedgerQueryRequest request) {
        // 总账按月汇总
        LambdaQueryWrapper<Ledger> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ledger::getDeleted, 0);
        
        if (request.getSubjectId() != null) {
            wrapper.eq(Ledger::getSubjectId, request.getSubjectId());
        }
        if (request.getAccountingPeriod() != null && !request.getAccountingPeriod().isEmpty()) {
            wrapper.eq(Ledger::getAccountingPeriod, request.getAccountingPeriod());
        }
        
        wrapper.orderByAsc(Ledger::getSubjectCode, Ledger::getAccountingPeriod);
        
        Page<Ledger> page = new Page<>(request.getCurrent(), request.getSize());
        IPage<Ledger> ledgerPage = ledgerMapper.selectPage(page, wrapper);
        
        return ledgerPage.convert(this::convertToLedgerDTO);
    }

    @Override
    public BigDecimal getSubjectBalance(Long subjectId, String accountingPeriod) {
        AccountSubject subject = accountSubjectMapper.selectById(subjectId);
        if (subject == null) {
            return BigDecimal.ZERO;
        }
        return subject.getCurrentBalance();
    }

    private LedgerDTO convertToLedgerDTO(Ledger ledger) {
        LedgerDTO dto = new LedgerDTO();
        BeanUtils.copyProperties(ledger, dto);
        
        dto.setLedgerTypeDesc(ledger.getLedgerType() == 1 ? "明细账" : "总账");
        dto.setBalanceDirectionDesc(getBalanceDirectionDesc(ledger.getBalanceDirection()));
        
        return dto;
    }

    // ==================== 财务报表 ====================

    @Override
    public Map<String, Object> generateBalanceSheet(String accountingPeriod) {
        Map<String, Object> result = new HashMap<>();
        
        // 资产类科目
        List<Map<String, Object>> assets = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        
        LambdaQueryWrapper<AccountSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSubject::getSubjectType, 1) // 资产
            .eq(AccountSubject::getStatus, 1)
            .eq(AccountSubject::getDeleted, 0)
            .eq(AccountSubject::getIsDetail, 1);
        
        List<AccountSubject> assetSubjects = accountSubjectMapper.selectList(wrapper);
        for (AccountSubject subject : assetSubjects) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectCode", subject.getSubjectCode());
            item.put("subjectName", subject.getSubjectName());
            item.put("balance", subject.getCurrentBalance());
            assets.add(item);
            totalAssets = totalAssets.add(subject.getCurrentBalance());
        }
        
        // 负债类科目
        List<Map<String, Object>> liabilities = new ArrayList<>();
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSubject::getSubjectType, 2) // 负债
            .eq(AccountSubject::getStatus, 1)
            .eq(AccountSubject::getDeleted, 0)
            .eq(AccountSubject::getIsDetail, 1);
        
        List<AccountSubject> liabilitySubjects = accountSubjectMapper.selectList(wrapper);
        for (AccountSubject subject : liabilitySubjects) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectCode", subject.getSubjectCode());
            item.put("subjectName", subject.getSubjectName());
            item.put("balance", subject.getCurrentBalance());
            liabilities.add(item);
            totalLiabilities = totalLiabilities.add(subject.getCurrentBalance());
        }
        
        // 权益类科目
        List<Map<String, Object>> equity = new ArrayList<>();
        BigDecimal totalEquity = BigDecimal.ZERO;
        
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSubject::getSubjectType, 3) // 权益
            .eq(AccountSubject::getStatus, 1)
            .eq(AccountSubject::getDeleted, 0)
            .eq(AccountSubject::getIsDetail, 1);
        
        List<AccountSubject> equitySubjects = accountSubjectMapper.selectList(wrapper);
        for (AccountSubject subject : equitySubjects) {
            Map<String, Object> item = new HashMap<>();
            item.put("subjectCode", subject.getSubjectCode());
            item.put("subjectName", subject.getSubjectName());
            item.put("balance", subject.getCurrentBalance());
            equity.add(item);
            totalEquity = totalEquity.add(subject.getCurrentBalance());
        }
        
        result.put("accountingPeriod", accountingPeriod);
        result.put("assets", assets);
        result.put("totalAssets", totalAssets);
        result.put("liabilities", liabilities);
        result.put("totalLiabilities", totalLiabilities);
        result.put("equity", equity);
        result.put("totalEquity", totalEquity);
        result.put("totalLiabilitiesAndEquity", totalLiabilities.add(totalEquity));
        
        return result;
    }

    @Override
    public Map<String, Object> generateIncomeStatement(String startPeriod, String endPeriod) {
        Map<String, Object> result = new HashMap<>();
        
        // 收入类科目
        List<Map<String, Object>> revenues = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        
        LambdaQueryWrapper<AccountSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSubject::getSubjectType, 5) // 损益
            .eq(AccountSubject::getStatus, 1)
            .eq(AccountSubject::getDeleted, 0)
            .eq(AccountSubject::getIsDetail, 1);
        
        List<AccountSubject> subjects = accountSubjectMapper.selectList(wrapper);
        for (AccountSubject subject : subjects) {
            // 收入类科目编码通常以6001开头
            if (subject.getSubjectCode().startsWith("6001") || subject.getSubjectCode().startsWith("6051")) {
                Map<String, Object> item = new HashMap<>();
                item.put("subjectCode", subject.getSubjectCode());
                item.put("subjectName", subject.getSubjectName());
                item.put("amount", subject.getCurrentBalance().abs());
                revenues.add(item);
                totalRevenue = totalRevenue.add(subject.getCurrentBalance().abs());
            }
        }
        
        // 成本费用类科目
        List<Map<String, Object>> expenses = new ArrayList<>();
        BigDecimal totalExpense = BigDecimal.ZERO;
        
        for (AccountSubject subject : subjects) {
            // 成本费用类科目编码通常以6401开头
            if (subject.getSubjectCode().startsWith("6401") || subject.getSubjectCode().startsWith("6601") 
                || subject.getSubjectCode().startsWith("6602") || subject.getSubjectCode().startsWith("6701")) {
                Map<String, Object> item = new HashMap<>();
                item.put("subjectCode", subject.getSubjectCode());
                item.put("subjectName", subject.getSubjectName());
                item.put("amount", subject.getCurrentBalance().abs());
                expenses.add(item);
                totalExpense = totalExpense.add(subject.getCurrentBalance().abs());
            }
        }
        
        result.put("startPeriod", startPeriod);
        result.put("endPeriod", endPeriod);
        result.put("revenues", revenues);
        result.put("totalRevenue", totalRevenue);
        result.put("expenses", expenses);
        result.put("totalExpense", totalExpense);
        result.put("grossProfit", totalRevenue.subtract(totalExpense));
        
        return result;
    }

    @Override
    public Map<String, Object> generateCashFlowStatement(String startPeriod, String endPeriod) {
        Map<String, Object> result = new HashMap<>();
        
        // 简化实现，实际需要更复杂的逻辑
        result.put("startPeriod", startPeriod);
        result.put("endPeriod", endPeriod);
        result.put("operatingActivities", new HashMap<>());
        result.put("investingActivities", new HashMap<>());
        result.put("financingActivities", new HashMap<>());
        result.put("netCashFlow", BigDecimal.ZERO);
        
        return result;
    }
}
