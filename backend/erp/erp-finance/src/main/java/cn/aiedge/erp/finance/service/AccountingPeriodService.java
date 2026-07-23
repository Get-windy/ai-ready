package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.AccountingPeriodDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 会计期间Service接口
 */
public interface AccountingPeriodService {

    /**
     * 分页查询会计期间
     */
    IPage<AccountingPeriodDTO> page(Integer periodYear, Integer status, Page<AccountingPeriodDTO> page);

    /**
     * 查询期间列表（可按年度过滤）
     */
    List<AccountingPeriodDTO> list(Integer periodYear);

    /**
     * 新增会计期间
     */
    AccountingPeriodDTO create(AccountingPeriodDTO dto);

    /**
     * 启用/停用（关闭）期间
     */
    AccountingPeriodDTO updateStatus(Long id, Integer status);
}
