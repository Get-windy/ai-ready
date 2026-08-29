package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.UnifiedPurchaseDocumentDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 统一采购单据查询服务
 * 整合采购入库单、采购退货单、采购换货单的查询（分包分页合并）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface UnifiedPurchaseDocQueryService {

    /**
     * 统一分页查询采购单据（入库/退货/换货合并）
     *
     * @param query 查询参数
     * @return 合并后的分页结果
     */
    Page<UnifiedPurchaseDocumentDTO> unifiedPage(UnifiedPurchaseDocQueryDTO query);
}
