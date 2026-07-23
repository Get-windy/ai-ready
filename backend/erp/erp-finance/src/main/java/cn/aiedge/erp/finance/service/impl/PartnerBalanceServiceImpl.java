package cn.aiedge.erp.finance.service.impl;

import cn.aiedge.erp.finance.dto.PartnerBalanceDTO;
import cn.aiedge.erp.finance.mapper.PartnerBalanceMapper;
import cn.aiedge.erp.finance.service.PartnerBalanceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 往来余额表Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartnerBalanceServiceImpl implements PartnerBalanceService {

    private final PartnerBalanceMapper partnerBalanceMapper;

    @Override
    public IPage<PartnerBalanceDTO> page(String partnerType, String keyword, boolean onlyNonZero, Page<PartnerBalanceDTO> page) {
        return partnerBalanceMapper.selectPartnerBalancePage(page, partnerType, keyword, onlyNonZero);
    }
}
