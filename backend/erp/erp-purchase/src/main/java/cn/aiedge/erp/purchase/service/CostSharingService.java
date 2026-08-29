package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.CostSharingCreateRequest;
import cn.aiedge.erp.purchase.dto.CostSharingDetailDTO;
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
                                      String handlerName, String departmentName,
                                      String createByName, String bookkeeperName,
                                      String summary, String remark, Integer status,
                                      String startDate, String endDate);

    /**
     * 创建分摊单
     */
    Long createCostSharing(CostSharingCreateRequest request);

    /**
     * 更新分摊单（仅草稿状态可更新）
     */
    void updateCostSharing(Long id, CostSharingCreateRequest request);

    /**
     * 获取分摊单详情（主表 + 费用单明细 + 入库单分摊明细）
     */
    CostSharingDetailDTO getDetail(Long id);

    /**
     * 获取下一个分摊单号
     */
    String nextNo();

    /**
     * 完成分摊单
     */
    void complete(Long id);

    /**
     * 取消分摊单
     */
    void cancel(Long id);
}
