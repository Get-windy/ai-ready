package cn.aiedge.erp.sale.service;

import cn.aiedge.erp.sale.dto.AccountDeliveryQueryDTO;
import cn.aiedge.erp.sale.dto.AccountDeliveryResult;

/**
 * 账款交账服务接口
 * 数据源：销售出库单（配送代收/业务员代收款项，待交账 = 结算状态非 settled）
 * 对标：财务→收入支出→账款交账（按职员 / 按单据 双视图 + 五档统计 + 交账动作）
 */
public interface AccountDeliveryService {

    /**
     * 按单据视图：分页查询待交账单据 + 五档统计
     */
    AccountDeliveryResult docPage(AccountDeliveryQueryDTO query);

    /**
     * 按职员视图：按交账职员分组汇总 + 五档统计
     */
    AccountDeliveryResult staffList(AccountDeliveryQueryDTO query);

    /**
     * 交账动作（去交账 / 配送退货）：将选中单据结算状态回写为已结算
     * @param query 含 sourceType + sourceIds（或 deliverStaff 按职员交账）
     */
    void deliver(AccountDeliveryQueryDTO query);
}
