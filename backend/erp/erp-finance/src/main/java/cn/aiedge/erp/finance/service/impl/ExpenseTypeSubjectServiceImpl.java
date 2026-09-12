package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import cn.aiedge.erp.finance.service.ExpenseTypeSubjectService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 费用类型（费用类会计科目视图）服务实现。
 *
 * <p>全部读写委托 {@link AccountSubjectService}，本类只负责费用口径与分页，不重复实现科目逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseTypeSubjectServiceImpl implements ExpenseTypeSubjectService {

    /** 损益类科目 */
    private static final int SUBJECT_TYPE_PROFIT_LOSS = 5;
    /** 借方（费用方向） */
    private static final int DIRECTION_DEBIT = 1;

    private final AccountSubjectService accountSubjectService;

    @Override
    public Page<AccountSubjectDTO> page(AccountSubjectQuery query, long pageNum, long pageSize) {
        long safePageNum = pageNum < 1 ? 1 : pageNum;
        long safePageSize = pageSize < 1 ? 20 : Math.min(pageSize, 200);

        List<AccountSubjectDTO> all = list(query);
        int from = (int) Math.min((long) (safePageNum - 1) * safePageSize, all.size());
        int to = (int) Math.min(from + safePageSize, all.size());

        Page<AccountSubjectDTO> page = new Page<>(safePageNum, safePageSize, all.size());
        page.setRecords(all.subList(from, to));
        return page;
    }

    @Override
    public List<AccountSubjectDTO> list(AccountSubjectQuery query) {
        AccountSubjectQuery expenseQuery = new AccountSubjectQuery();
        if (query != null) {
            expenseQuery.setKeyword(query.getKeyword());
            expenseQuery.setIncludeDisabled(query.includeDisabledOrDefault());
        }
        expenseQuery.setSubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        expenseQuery.setDirection(DIRECTION_DEBIT);
        List<AccountSubjectDTO> rows = accountSubjectService.search(expenseQuery);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public AccountSubjectDTO getById(Long id) {
        assertExpenseSubject(id);
        return accountSubjectService.getById(id);
    }

    @Override
    public AccountSubjectDTO create(AccountSubjectDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("费用科目不能为空");
        }
        // 费用科目固定口径：损益类（5），方向缺省借方（1，费用方向）
        dto.setSubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        if (dto.getDirection() == null) {
            dto.setDirection(DIRECTION_DEBIT);
        }
        return accountSubjectService.create(dto);
    }

    @Override
    public AccountSubjectDTO update(Long id, AccountSubjectDTO dto) {
        assertExpenseSubject(id);
        if (dto != null) {
            // 本页不提供科目分类编辑，保持损益类不变
            dto.setSubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        }
        return accountSubjectService.update(id, dto);
    }

    @Override
    public void delete(Long id) {
        assertExpenseSubject(id);
        accountSubjectService.delete(id);
    }

    @Override
    public AccountSubjectDTO toggleEnabled(Long id, boolean enabled) {
        assertExpenseSubject(id);
        return accountSubjectService.enable(id, enabled);
    }

    @Override
    public List<FinanceAuxiliaryTypeDTO> getAuxTypeOptions() {
        return accountSubjectService.getAuxTypeOptions();
    }

    /** 越界保护：本页只能操作费用类科目（损益类 + 借方） */
    private void assertExpenseSubject(Long id) {
        AccountSubjectDTO subject = accountSubjectService.getById(id);
        boolean expense = subject != null
                && Integer.valueOf(SUBJECT_TYPE_PROFIT_LOSS).equals(subject.getSubjectType())
                && Integer.valueOf(DIRECTION_DEBIT).equals(subject.getDirection());
        if (!expense) {
            throw BusinessException.notFound("费用科目不存在: " + id);
        }
    }
}
