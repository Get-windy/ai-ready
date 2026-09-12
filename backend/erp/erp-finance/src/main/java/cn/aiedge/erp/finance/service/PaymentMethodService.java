package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.model.dto.PaymentMethodQuery;
import cn.aiedge.erp.finance.model.dto.PaymentMethodVO;
import cn.aiedge.erp.finance.model.entity.PaymentMethod;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 支付方式Service接口
 *
 * 单一口径：支付方式为全局基础数据（md_payment_method），收款/付款/预收/预付统一引用，
 * 严禁另建重复字典。
 */
public interface PaymentMethodService extends IService<PaymentMethod> {

    /** 多条件分页查询（回填默认入账账户名称） */
    Page<PaymentMethodVO> pageQuery(PaymentMethodQuery query);

    /** 按条件取全量（导出用） */
    List<PaymentMethodVO> listByQuery(PaymentMethodQuery query);

    /** 详情 */
    PaymentMethodVO getDetail(Long id);

    /** 启用中的支付方式下拉（status=1，按 sort 升序） */
    List<PaymentMethodVO> listEnabled();

    /** 新增（编码唯一校验 + 默认唯一） */
    PaymentMethodVO create(PaymentMethod entity);

    /** 修改（编码唯一校验 + 默认唯一） */
    PaymentMethodVO updateMethod(Long id, PaymentMethod entity);

    /** 删除（引用保护：支付渠道 / 收款单 / 付款单引用则拒绝） */
    void delete(Long id);

    /** 启用/停用 */
    PaymentMethodVO updateStatus(Long id, Integer status);

    /** 批量启用/停用 */
    int batchStatus(List<Long> ids, Integer status);
}
