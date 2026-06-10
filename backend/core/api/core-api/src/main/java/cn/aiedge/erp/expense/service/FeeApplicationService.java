package cn.aiedge.erp.expense.service;

import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.expense.dto.*;
import cn.aiedge.erp.expense.entity.FeeApplication;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 费用申请服务接口
 */
public interface FeeApplicationService extends IService<FeeApplication> {

    /**
     * 分页查询
     */
    PageResult<FeeApplicationVO> pageList(FeeApplicationQueryRequest request);

    /**
     * 获取详情
     */
    FeeApplicationVO getDetail(Long id);

    /**
     * 创建费用申请
     */
    Long create(FeeApplicationCreateRequest request);

    /**
     * 更新费用申请
     */
    void update(Long id, FeeApplicationCreateRequest request);

    /**
     * 删除费用申请
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

    /**
     * 获取我的申请列表
     */
    PageResult<FeeApplicationVO> getMyApplications(FeeApplicationQueryRequest request, Long userId);

    /**
     * 获取待我审批列表
     */
    PageResult<FeeApplicationVO> getMyPendingApprovals(Long userId, Integer pageNum, Integer pageSize);
}
