package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.PartnerBalanceDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 往来余额表Service
 */
public interface PartnerBalanceService {

    /**
     * 分页查询往来余额表
     *
     * @param partnerType 往来单位类型(customer/supplier/空=全部)
     * @param keyword     名称或ID模糊查询
     * @param onlyNonZero 是否只显示余额非零的往来单位
     * @param page        分页参数
     */
    IPage<PartnerBalanceDTO> page(String partnerType, String keyword, boolean onlyNonZero, Page<PartnerBalanceDTO> page);
}
