package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.DepositCondition;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface DepositConditionService extends IService<DepositCondition> {

    DepositCondition getByPreReceiptId(Long preReceiptId);

    DepositCondition getByPrePaymentId(Long prePaymentId);

    List<DepositCondition> getBySource(String sourceType, Long sourceId);

    DepositCondition createCondition(DepositCondition condition);

    /**
     * 状态变更: frozen → converted(已转正)
     */
    DepositCondition convert(Long id);

    /**
     * 状态变更: frozen → refunded(已退还)
     */
    DepositCondition refund(Long id, String reason);

    /**
     * 状态变更: frozen → forfeited(已没收)
     */
    DepositCondition forfeit(Long id, BigDecimal forfeitAmount, String reason);

    /**
     * 状态变更: frozen → deducted(已扣款)
     */
    DepositCondition deduct(Long id, BigDecimal deductAmount, String reason);
}
