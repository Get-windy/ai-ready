package cn.aiedge.erp.payment.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.DepositCondition;
import cn.aiedge.erp.payment.mapper.DepositConditionMapper;
import cn.aiedge.erp.payment.service.DepositConditionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositConditionServiceImpl extends ServiceImpl<DepositConditionMapper, DepositCondition>
        implements DepositConditionService {

    @Override
    public DepositCondition getByPreReceiptId(Long preReceiptId) {
        return lambdaQuery()
                .eq(DepositCondition::getPreReceiptId, preReceiptId)
                .one();
    }

    @Override
    public DepositCondition getByPrePaymentId(Long prePaymentId) {
        return lambdaQuery()
                .eq(DepositCondition::getPrePaymentId, prePaymentId)
                .one();
    }

    @Override
    public List<DepositCondition> getBySource(String sourceType, Long sourceId) {
        return lambdaQuery()
                .eq(DepositCondition::getSourceType, sourceType)
                .eq(DepositCondition::getSourceId, sourceId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositCondition createCondition(DepositCondition condition) {
        condition.setStatus("frozen");
        save(condition);
        log.info("定金条件创建成功: id={}, depositType={}, direction={}",
                condition.getId(), condition.getDepositType(), condition.getDirection());
        return condition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositCondition convert(Long id) {
        DepositCondition condition = getById(id);
        if (condition == null) {
            throw BusinessException.notFound("定金条件不存在");
        }
        if (!"frozen".equals(condition.getStatus())) {
            throw BusinessException.badRequest("只有冻结状态的定金条件可以执行转正操作");
        }
        condition.setStatus("converted");
        updateById(condition);
        log.info("定金条件转正成功: id={}", id);
        return condition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositCondition refund(Long id, String reason) {
        DepositCondition condition = getById(id);
        if (condition == null) {
            throw BusinessException.notFound("定金条件不存在");
        }
        if (!"frozen".equals(condition.getStatus())) {
            throw BusinessException.badRequest("只有冻结状态的定金条件可以执行退还操作");
        }
        condition.setStatus("refunded");
        condition.setRemark(reason);
        updateById(condition);
        log.info("定金条件退还成功: id={}, reason={}", id, reason);
        return condition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositCondition forfeit(Long id, BigDecimal forfeitAmount, String reason) {
        DepositCondition condition = getById(id);
        if (condition == null) {
            throw BusinessException.notFound("定金条件不存在");
        }
        if (!"frozen".equals(condition.getStatus())) {
            throw BusinessException.badRequest("只有冻结状态的定金条件可以执行没收操作");
        }
        condition.setStatus("forfeited");
        condition.setForfeitAmount(forfeitAmount);
        condition.setRemark(reason);
        updateById(condition);
        log.info("定金条件没收成功: id={}, forfeitAmount={}", id, forfeitAmount);
        return condition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepositCondition deduct(Long id, BigDecimal deductAmount, String reason) {
        DepositCondition condition = getById(id);
        if (condition == null) {
            throw BusinessException.notFound("定金条件不存在");
        }
        if (!"frozen".equals(condition.getStatus())) {
            throw BusinessException.badRequest("只有冻结状态的定金条件可以执行扣款操作");
        }
        condition.setStatus("deducted");
        condition.setForfeitAmount(deductAmount);
        condition.setRemark(reason);
        updateById(condition);
        log.info("定金条件扣款成功: id={}, deductAmount={}", id, deductAmount);
        return condition;
    }
}
