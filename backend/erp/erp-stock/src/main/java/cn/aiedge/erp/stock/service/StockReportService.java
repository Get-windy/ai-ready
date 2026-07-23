package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.InvSummaryVO;
import cn.aiedge.erp.stock.dto.PurchasePrepAnalysisVO;
import cn.aiedge.erp.stock.dto.StockFlowVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDate;

/**
 * 库存分析报表Service
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface StockReportService {

    /**
     * 进销存汇总分页（每商品+仓库一行：期初/期间入库/期间出库/结存）
     *
     * @param startDate   期间开始日期（含），为空则期初为0、变动自最早单据起算
     * @param endDate     期间结束日期（含），为空则不限上界
     * @param warehouseId 仓库ID（可空）
     * @param keyword     商品编码/名称关键字（可空）
     */
    IPage<InvSummaryVO> pageInvSummary(LocalDate startDate, LocalDate endDate, Long warehouseId,
                                       String keyword, int pageNum, int pageSize);

    /**
     * 库存变动流水分页（每单据明细行一条变动）
     *
     * @param productId   商品ID（可空）
     * @param warehouseId 仓库ID（可空）
     * @param docType     单据类型（可空）：PURCHASE_IN/SALE_OUT/TRANSFER_IN/TRANSFER_OUT/OVERFLOW_IN/DAMAGE_OUT/CHECK_ADJUST
     * @param startDate   变动开始日期（含，可空）
     * @param endDate     变动结束日期（含，可空）
     */
    IPage<StockFlowVO> pageStockFlow(Long productId, Long warehouseId, String docType,
                                     LocalDate startDate, LocalDate endDate, int pageNum, int pageSize);

    /**
     * 采购准备分析汇总
     *
     * @param warehouseId 仓库ID（可空，为空统计全部仓库）
     */
    PurchasePrepAnalysisVO getPurchasePrepAnalysis(Long warehouseId);
}
