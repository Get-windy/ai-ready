package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import cn.aiedge.erp.finance.service.OtherIncomeSubjectService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 其他收入（收入类会计科目视图）服务实现。
 *
 * <p>全部读写委托 {@link AccountSubjectService}，本类只负责收入口径与分页，不重复实现科目逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtherIncomeSubjectServiceImpl implements OtherIncomeSubjectService {

    /** 损益类科目 */
    private static final int SUBJECT_TYPE_PROFIT_LOSS = 5;
    /** 贷方（收入方向） */
    private static final int DIRECTION_CREDIT = 2;

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
        AccountSubjectQuery incomeQuery = new AccountSubjectQuery();
        if (query != null) {
            incomeQuery.setKeyword(query.getKeyword());
            incomeQuery.setIncludeDisabled(query.includeDisabledOrDefault());
        }
        incomeQuery.setSubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        incomeQuery.setDirection(DIRECTION_CREDIT);
        List<AccountSubjectDTO> rows = accountSubjectService.search(incomeQuery);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public AccountSubjectDTO getById(Long id) {
        assertIncomeSubject(id);
        return accountSubjectService.getById(id);
    }

    @Override
    public AccountSubjectDTO create(AccountSubjectDTO dto) {
        if (dto == null) {
            throw BusinessException.badRequest("收入科目不能为空");
        }
        // 收入科目固定口径：损益类（5），方向缺省贷方（2，收入方向）
        dto.setSubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        if (dto.getDirection() == null) {
            dto.setDirection(DIRECTION_CREDIT);
        }
        return accountSubjectService.create(dto);
    }

    @Override
    public AccountSubjectDTO update(Long id, AccountSubjectDTO dto) {
        assertIncomeSubject(id);
        if (dto != null) {
            // 本页不提供科目分类编辑，保持损益类不变
            dto.setSubjectType(SUBJECT_TYPE_PROFIT_LOSS);
        }
        return accountSubjectService.update(id, dto);
    }

    @Override
    public void delete(Long id) {
        assertIncomeSubject(id);
        accountSubjectService.delete(id);
    }

    @Override
    public AccountSubjectDTO toggleEnabled(Long id, boolean enabled) {
        assertIncomeSubject(id);
        return accountSubjectService.enable(id, enabled);
    }

    @Override
    public List<FinanceAuxiliaryTypeDTO> getAuxTypeOptions() {
        return accountSubjectService.getAuxTypeOptions();
    }

    /** 越界保护：本页只能操作收入类科目 */
    private void assertIncomeSubject(Long id) {
        AccountSubjectDTO subject = accountSubjectService.getById(id);
        boolean income = subject != null
                && Integer.valueOf(SUBJECT_TYPE_PROFIT_LOSS).equals(subject.getSubjectType())
                && Integer.valueOf(DIRECTION_CREDIT).equals(subject.getDirection());
        if (!income) {
            throw BusinessException.notFound("收入科目不存在: " + id);
        }
    }
}
