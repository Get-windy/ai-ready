package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.PurchaseDetailListDTO;
import cn.aiedge.erp.purchase.dto.PurchaseDetailQueryDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 按明细Tab查询服务
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchaseDetailQueryService {

    /**
     * 按明细Tab分页查询（59列）
     */
    Page<PurchaseDetailListDTO> pageByDetail(PurchaseDetailQueryDTO query);
}
