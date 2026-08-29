package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.FinanceAuxiliaryBalanceDTO;
import cn.aiedge.erp.finance.mapper.FinanceAuxiliaryBalanceMapper;
import cn.aiedge.erp.finance.mapper.FinanceAuxiliaryItemMapper;
import cn.aiedge.erp.finance.mapper.FinanceAuxiliaryTypeMapper;
import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryBalance;
import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryItem;
import cn.aiedge.erp.finance.model.entity.FinanceAuxiliaryType;
import cn.aiedge.erp.finance.service.FinanceAuxiliaryBalanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 辅助核算余额Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceAuxiliaryBalanceServiceImpl implements FinanceAuxiliaryBalanceService {

    /**
     * 默认租户ID（本模块暂按单租户处理）
     */
    private static final Long DEFAULT_TENANT_ID = 1L;

    private final FinanceAuxiliaryBalanceMapper financeAuxiliaryBalanceMapper;
    private final FinanceAuxiliaryTypeMapper financeAuxiliaryTypeMapper;
    private final FinanceAuxiliaryItemMapper financeAuxiliaryItemMapper;

    @Override
    public IPage<FinanceAuxiliaryBalanceDTO> page(Long accountingPeriodId, Long subjectId, Long auxiliaryTypeId, Long auxiliaryItemId, Page<FinanceAuxiliaryBalanceDTO> page) {
        LambdaQueryWrapper<FinanceAuxiliaryBalance> wrapper = new LambdaQueryWrapper<FinanceAuxiliaryBalance>()
                .eq(FinanceAuxiliaryBalance::getTenantId, DEFAULT_TENANT_ID)
                .eq(accountingPeriodId != null, FinanceAuxiliaryBalance::getAccountingPeriodId, accountingPeriodId)
                .eq(subjectId != null, FinanceAuxiliaryBalance::getSubjectId, subjectId)
                .eq(auxiliaryTypeId != null, FinanceAuxiliaryBalance::getAuxiliaryTypeId, auxiliaryTypeId)
                .eq(auxiliaryItemId != null, FinanceAuxiliaryBalance::getAuxiliaryItemId, auxiliaryItemId)
                .orderByAsc(FinanceAuxiliaryBalance::getAuxiliaryTypeId)
                .orderByAsc(FinanceAuxiliaryBalance::getAuxiliaryItemId)
                .orderByAsc(FinanceAuxiliaryBalance::getId);
        Page<FinanceAuxiliaryBalance> entityPage = financeAuxiliaryBalanceMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), wrapper);
        return entityPage.convert(this::toDTO);
    }

    private FinanceAuxiliaryBalanceDTO toDTO(FinanceAuxiliaryBalance entity) {
        FinanceAuxiliaryBalanceDTO dto = new FinanceAuxiliaryBalanceDTO();
        dto.setId(entity.getId());
        dto.setAccountingPeriodId(entity.getAccountingPeriodId());
        dto.setSubjectId(entity.getSubjectId());
        dto.setAuxiliaryTypeId(entity.getAuxiliaryTypeId());
        dto.setAuxiliaryItemId(entity.getAuxiliaryItemId());
        dto.setBeginDebit(entity.getBeginDebit());
        dto.setBeginCredit(entity.getBeginCredit());
        dto.setPeriodDebit(entity.getPeriodDebit());
        dto.setPeriodCredit(entity.getPeriodCredit());
        dto.setEndDebit(entity.getEndDebit());
        dto.setEndCredit(entity.getEndCredit());
        dto.setYearDebit(entity.getYearDebit());
        dto.setYearCredit(entity.getYearCredit());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        // 查询辅助核算类型名称
        if (entity.getAuxiliaryTypeId() != null) {
            FinanceAuxiliaryType type = financeAuxiliaryTypeMapper.selectById(entity.getAuxiliaryTypeId());
            if (type != null) {
                dto.setAuxiliaryTypeName(type.getTypeName());
            }
        }

        // 查询辅助核算项目名称
        if (entity.getAuxiliaryItemId() != null) {
            FinanceAuxiliaryItem item = financeAuxiliaryItemMapper.selectById(entity.getAuxiliaryItemId());
            if (item != null) {
                dto.setAuxiliaryItemName(item.getItemName());
            }
        }

        return dto;
    }
}
