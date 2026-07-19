package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.UnifiedSalesDocQueryDTO;
import cn.aiedge.erp.sale.dto.UnifiedSalesDocumentDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 统一销售单据查询服务接口
 * 提供跨类型销售单据的统一查询功能
 */
public interface UnifiedSalesDocQueryService {

    /**
     * 统一销售单据分页查询
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    Page<UnifiedSalesDocumentDTO> unifiedPage(UnifiedSalesDocQueryDTO queryDTO);
}