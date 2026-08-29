package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.ShortageReplenishVO;
import cn.aiedge.erp.stock.mapper.ShortageReplenishMapper;
import cn.aiedge.erp.stock.service.ShortageReplenishService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 缺货补货查询ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShortageReplenishServiceImpl implements ShortageReplenishService {

    private final ShortageReplenishMapper shortageReplenishMapper;

    @Override
    public IPage<ShortageReplenishVO> page(Integer orderStatus, String startDate, String endDate,
                                           Long customerId, Long salesmanId, Long warehouseId,
                                           Integer orderSource, String productKeyword, String supplierName,
                                           Long categoryId, Integer shortageMode, Boolean onlyShortage,
                                           int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return shortageReplenishMapper.selectShortagePage(new Page<>(pageNum, pageSize),
                tenantId, orderStatus, startDate, endDate, customerId, salesmanId, warehouseId,
                orderSource, productKeyword, supplierName, categoryId, shortageMode, onlyShortage);
    }
}
