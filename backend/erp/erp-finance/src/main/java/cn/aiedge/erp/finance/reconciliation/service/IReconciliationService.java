package cn.aiedge.erp.finance.reconciliation.service;

import cn.aiedge.erp.finance.reconciliation.entity.Reconciliation;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationCreateRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationUpdateRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationQueryRequest;
import cn.aiedge.erp.finance.reconciliation.dto.ReconciliationVO;
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

    /**
     * 获取对账统计数据
     * @return 统计数据（bankPending, customerPending, supplierPending, differenceCount）
     */
    java.util.Map<String, Object> getStats();

    /**
     * 查询对方系统余额（银行=账户余额 / 客户=应收 / 供应商=应付），用于新增对账预填系统余额
     * @param reconciliationType BANK/CUSTOMER/SUPPLIER
     * @param targetId 对方ID（银行账户ID / 客户ID / 供应商ID）
     * @return 余额
     */
    java.math.BigDecimal getBalance(String reconciliationType, Long targetId);
}
