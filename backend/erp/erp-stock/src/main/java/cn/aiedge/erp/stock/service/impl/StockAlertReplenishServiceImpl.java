package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.StockAlertReplenishVO;
import cn.aiedge.erp.stock.mapper.StockAlertReplenishMapper;
import cn.aiedge.erp.stock.service.StockAlertReplenishService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存预警补货查询ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAlertReplenishServiceImpl implements StockAlertReplenishService {

    private final StockAlertReplenishMapper stockAlertReplenishMapper;

    @Override
    public IPage<StockAlertReplenishVO> page(Long warehouseId, String keyword, String brand, String supplierName,
                                             String remark, Boolean onlyLowStock, Long categoryId,
                                             int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return stockAlertReplenishMapper.selectAlertReplenishPage(new Page<>(pageNum, pageSize),
                tenantId, warehouseId, keyword, brand, supplierName, remark, onlyLowStock, categoryId);
    }
}
