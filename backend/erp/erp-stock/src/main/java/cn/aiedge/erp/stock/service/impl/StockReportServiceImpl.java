package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.stock.dto.InvSummaryVO;
import cn.aiedge.erp.stock.dto.PurchasePrepAnalysisVO;
import cn.aiedge.erp.stock.dto.StockFlowVO;
import cn.aiedge.erp.stock.mapper.StockReportMapper;
import cn.aiedge.erp.stock.service.StockReportService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * 库存分析报表ServiceImpl
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockReportServiceImpl implements StockReportService {

    private final StockReportMapper stockReportMapper;

    /** 单据类型中文名映射 */
    private static final Map<String, String> DOC_TYPE_NAMES = Map.of(
            "PURCHASE_IN", "采购入库",
            "SALE_OUT", "销售出库",
            "TRANSFER_IN", "调拨入库",
            "TRANSFER_OUT", "调拨出库",
            "OVERFLOW_IN", "报溢入库",
            "DAMAGE_OUT", "报损出库",
            "CHECK_ADJUST", "盘点调整");

    @Override
    public IPage<InvSummaryVO> pageInvSummary(LocalDate startDate, LocalDate endDate, Long warehouseId,
                                              String keyword, int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return stockReportMapper.selectInvSummaryPage(new Page<>(pageNum, pageSize),
                startDate, endDate, warehouseId, keyword, tenantId);
    }

    @Override
    public IPage<StockFlowVO> pageStockFlow(Long productId, Long warehouseId, String docType,
                                            LocalDate startDate, LocalDate endDate, int pageNum, int pageSize) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        IPage<StockFlowVO> page = stockReportMapper.selectStockFlowPage(new Page<>(pageNum, pageSize),
                productId, warehouseId, docType, startDate, endDate, tenantId);
        for (StockFlowVO row : page.getRecords()) {
            row.setDocTypeName(DOC_TYPE_NAMES.getOrDefault(row.getDocType(), row.getDocType()));
        }
        return page;
    }

    @Override
    public PurchasePrepAnalysisVO getPurchasePrepAnalysis(Long warehouseId) {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return stockReportMapper.selectPurchasePrepAnalysis(warehouseId, tenantId);
    }
}
