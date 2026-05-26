package cn.aiedge.finance.service;

import cn.aiedge.finance.dto.*;
import cn.aiedge.finance.entity.Receivable;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 应收账款服务接口
 */
public interface ReceivableService extends IService<Receivable> {

    /**
     * 分页查询应收账款
     */
    PageResult<ReceivableVO> pageList(ReceivableQueryRequest request);

    /**
     * 创建应收账款
     */
    Long create(ReceivableCreateRequest request);

    /**
     * 更新应收账款
     */
    void update(ReceivableUpdateRequest request);

    /**
     * 删除应收账款
     */
    void delete(Long id);

    /**
     * 获取应收账款详情
     */
    ReceivableVO getDetail(Long id);

    /**
     * 收款操作
     */
    void receivePayment(ReceiptCreateRequest request);

    /**
     * 计算账龄分析
     */
    List<AgingAnalysisVO> analyzeAging();

    /**
     * 根据客户ID查询应收账款
     */
    List<ReceivableVO> getByCustomer(Long customerId);
}
