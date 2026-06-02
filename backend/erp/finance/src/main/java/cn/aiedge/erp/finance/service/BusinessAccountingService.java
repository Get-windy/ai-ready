package cn.aiedge.erp.finance.service;

import cn.aiedge.erp.finance.dto.BusinessAccountingRequest;
import cn.aiedge.erp.finance.dto.PayableDTO;
import cn.aiedge.erp.finance.dto.ReceivableDTO;
import cn.aiedge.erp.finance.dto.VoucherDTO;

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
}
