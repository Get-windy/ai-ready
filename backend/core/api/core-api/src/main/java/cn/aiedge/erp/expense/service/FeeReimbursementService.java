package cn.aiedge.erp.expense.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.*;
import cn.aiedge.erp.expense.entity.FeeReimbursement;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 费用报销服务接口
 */
public interface FeeReimbursementService extends IService<FeeReimbursement> {

    /**
     * 分页查询
     */
    PageResult<FeeReimbursementVO> pageList(FeeReimbursementQueryRequest request);

    /**
     * 获取详情
     */
    FeeReimbursementVO getDetail(Long id);

    /**
     * 创建费用报销
     */
    Long create(FeeReimbursementCreateRequest request);

    /**
     * 更新费用报销
     */
    void update(Long id, FeeReimbursementCreateRequest request);

    /**
     * 删除费用报销
     */
    void delete(Long id);

    /**
     * 提交审批
     */
    void submit(Long id);

    /**
     * 撤回申请
     */
    void withdraw(Long id);
}
