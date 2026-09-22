package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;

import java.time.LocalDate;

/**
 * 业财集成网关Service接口
 * 接收来自业务系统（采购、销售、费用等）的记账请求
 */
public interface BusinessAccountingService {

    /**
     * 从业务系统创建凭证（主入口）
     */
    VoucherDTO createVoucherFromBusiness(BusinessAccountingRequest request);

    /**
     * 从业务系统创建应收账款
     */
    ReceivableDTO createReceivableFromBusiness(BusinessAccountingRequest request);

    /**
     * 从业务系统创建应付账款
     */
    PayableDTO createPayableFromBusiness(BusinessAccountingRequest request);

    /**
     * **应收**的结算口径预检（只读、不落库、不记账）。
     *
     * <p>与 {@link #createReceivableFromBusiness} 的区别只有一个，但是致命的：
     * 那个方法的调用方是 catch-and-continue（"记账失败不阻断单据"），
     * 会把业务异常一并吞掉；而本方法<b>必须</b>在单据流程里、在记账的 try/catch
     * <b>之外</b>调用 —— 拒单是业务校验，不是记账失败。</p>
     *
     * <p>唯一会抛业务异常的情形：协议约定「账期结算」却缺账期天数 / 方向
     * （DOMAIN-MODEL §13.3 铁律②：账期天数是条件必填，缺 ⇒ 提交不了）。</p>
     *
     * @return 按三层结算口径算出的到期日（仅用于提示/日志，不落库）
     */
    LocalDate precheckReceivableDueDate(BusinessAccountingRequest request);

    /**
     * **应付**的结算口径预检（见 {@link #precheckReceivableDueDate}，口径完全对称）。
     */
    LocalDate precheckPayableDueDate(BusinessAccountingRequest request);
}
