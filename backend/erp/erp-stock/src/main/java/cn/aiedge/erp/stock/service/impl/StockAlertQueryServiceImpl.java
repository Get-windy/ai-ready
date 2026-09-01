package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.StockAlertQueryVO;
import cn.aiedge.erp.stock.mapper.StockAlertQueryMapper;
import cn.aiedge.erp.stock.service.StockAlertQueryService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 预警查询ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAlertQueryServiceImpl implements StockAlertQueryService {

    private final StockAlertQueryMapper stockAlertQueryMapper;

    @Override
    public IPage<StockAlertQueryVO> page(Long warehouseId, String productKeyword,
                                         String brand, Long categoryId, String alertType,
                                         int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return stockAlertQueryMapper.selectAlertPage(new Page<>(pageNum, pageSize),
                tenantId, warehouseId, productKeyword, brand, categoryId, alertType);
    }
}
