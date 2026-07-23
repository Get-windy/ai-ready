package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.PurchaseDetailListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseDetailQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchaseDocQueryDTO;
import cn.aiedge.erp.purchase.dto.PurchaseOrderListDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 按单据Tab查询服务
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchaseDocQueryService {

    /**
     * 按单据Tab分页查询（39列）
     */
    Page<PurchaseOrderListDTO> pageByDoc(PurchaseDocQueryDTO query);
}
