package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.CostSharingCreateRequest;
import cn.aiedge.erp.purchase.dto.CostSharingPageDTO;
import cn.aiedge.erp.purchase.entity.CostSharing;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 采购费用分摊单服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface CostSharingService extends IService<CostSharing> {

    /**
     * 分页查询分摊单
     */
    Page<CostSharingPageDTO> pageList(int pageNum, int pageSize, String sharingNo,
                                       String supplierName, String startDate, String endDate);

    /**
     * 创建分摊单
     */
    Long createCostSharing(CostSharingCreateRequest request);

    /**
     * 完成分摊单
     */
    void complete(Long id);

    /**
     * 取消分摊单
     */
    void cancel(Long id);
}
