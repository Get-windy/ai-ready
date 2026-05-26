package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.Payable;
import cn.aiedge.finance.dto.PayableCreateRequest;
import cn.aiedge.finance.dto.PayableUpdateRequest;
import cn.aiedge.finance.dto.PayableQueryRequest;
import cn.aiedge.finance.dto.PayableVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
