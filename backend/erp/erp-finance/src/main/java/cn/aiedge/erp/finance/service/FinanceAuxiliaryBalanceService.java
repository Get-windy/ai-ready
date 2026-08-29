package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.FinanceAuxiliaryBalanceDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 辅助核算余额Service接口
 */
public interface FinanceAuxiliaryBalanceService {

    /**
     * 分页查询辅助核算余额
     */
    IPage<FinanceAuxiliaryBalanceDTO> page(Long accountingPeriodId, Long subjectId, Long auxiliaryTypeId, Long auxiliaryItemId, Page<FinanceAuxiliaryBalanceDTO> page);
}
