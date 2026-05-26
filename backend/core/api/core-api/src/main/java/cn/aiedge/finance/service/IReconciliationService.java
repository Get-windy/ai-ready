package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Reconciliation;
import cn.aiedge.finance.dto.ReconciliationCreateRequest;
import cn.aiedge.finance.dto.ReconciliationUpdateRequest;
import cn.aiedge.finance.dto.ReconciliationQueryRequest;
import cn.aiedge.finance.dto.ReconciliationVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 对账记录服务接口
 */
public interface IReconciliationService extends IService<Reconciliation> {

    /**
     * 创建对账记录
     */
    Long createReconciliation(ReconciliationCreateRequest request);

    /**
     * 更新对账记录
     */
    void updateReconciliation(ReconciliationUpdateRequest request);

    /**
     * 分页查询对账记录
     */
    Page<ReconciliationVO> pageReconciliations(ReconciliationQueryRequest request);

    /**
     * 根据ID获取对账记录详情
     */
    ReconciliationVO getReconciliationById(Long id);

    /**
     * 删除对账记录
     */
    void deleteReconciliation(Long id);

    /**
     * 执行对账操作
     */
    void reconcile(Long id);

    /**
     * 处理差异
     */
    void handleDifference(Long id, String differenceReason);
}
