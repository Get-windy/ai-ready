package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Payable;
import cn.aiedge.finance.dto.PayableCreateRequest;
import cn.aiedge.finance.dto.PayableUpdateRequest;
import cn.aiedge.finance.dto.PayableQueryRequest;
import cn.aiedge.finance.dto.PayableVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 应付账款服务接口
 */
public interface IPayableService extends IService<Payable> {

    /**
     * 创建应付账款
     */
    Long createPayable(PayableCreateRequest request);

    /**
     * 更新应付账款
     */
    void updatePayable(PayableUpdateRequest request);

    /**
     * 分页查询应付账款
     */
    Page<PayableVO> pagePayables(PayableQueryRequest request);

    /**
     * 根据ID获取应付账款详情
     */
    PayableVO getPayableById(Long id);

    /**
     * 删除应付账款
     */
    void deletePayable(Long id);

    /**
     * 批量删除应付账款
     */
    void batchDelete(List<Long> ids);

    /**
     * 导出应付账款列表
     */
    List<PayableVO> exportList(PayableQueryRequest request);
}
