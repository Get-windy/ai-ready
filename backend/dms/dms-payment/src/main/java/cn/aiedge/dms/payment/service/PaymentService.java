package cn.aiedge.dms.payment.service;

import cn.aiedge.dms.common.exception.DmsBusinessException;
import cn.aiedge.dms.payment.entity.DmsPayment;
import cn.aiedge.dms.payment.mapper.DmsPaymentMapper;
import cn.aiedge.dms.task.entity.DmsTask;
import cn.aiedge.dms.task.mapper.DmsTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 现场收款服务
 *
 * 处理二维码生成、收款确认、未付标记和收款记录查询等业务逻辑。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final DmsPaymentMapper paymentMapper;
    private final DmsTaskMapper taskMapper;

    /**
     * 生成收款二维码
     *
     * @param taskId 任务ID
     * @param amount 收款金额
     * @return 收款记录
     */
    @Transactional(rollbackFor = Exception.class)
    public DmsPayment generateQrcode(Long taskId, BigDecimal amount) {
        DmsTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new DmsBusinessException("任务不存在: " + taskId);
        }

        // 生成模拟二维码URL（实际应调用第三方支付API获取）
        String qrcodeUrl = "https://pay.example.com/qrcode/" + UUID.randomUUID().toString().replace("-", "");

        DmsPayment payment = new DmsPayment();
        payment.setTaskId(taskId);
        payment.setTenantId(task.getTenantId());
        payment.setAmount(amount);
        payment.setQrcodeUrl(qrcodeUrl);
        payment.setStatus(0); // 待支付
        payment.setAuditStatus(0);
        paymentMapper.insert(payment);

        log.info("收款二维码已生成: taskId={}, paymentId={}, amount={}", taskId, payment.getId(), amount);
        return payment;
    }

    /**
     * 确认收款
     *
     * @param taskId          任务ID
     * @param paymentType     支付方式（1-微信 2-支付宝 3-现金 4-其他）
     * @param amount          收款金额
     * @param externalOrderNo 外部订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPayment(Long taskId, Integer paymentType, BigDecimal amount, String externalOrderNo) {
        DmsPayment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<DmsPayment>()
                        .eq(DmsPayment::getTaskId, taskId)
                        .eq(DmsPayment::getStatus, 0) // 待支付
                        .orderByDesc(DmsPayment::getCreateTime)
                        .last("LIMIT 1")
        );

        if (payment == null) {
            throw new DmsBusinessException("未找到待支付的收款记录: taskId=" + taskId);
        }

        payment.setPaymentType(paymentType);
        payment.setAmount(amount);
        payment.setExternalOrderNo(externalOrderNo);
        payment.setPayTime(LocalDateTime.now());
        payment.setStatus(1); // 已支付
        paymentMapper.updateById(payment);

        log.info("收款确认成功: taskId={}, paymentId={}, type={}, amount={}",
                taskId, payment.getId(), paymentType, amount);
    }

    /**
     * 标记未付
     *
     * @param taskId 任务ID
     * @param remark 未付备注
     */
    @Transactional(rollbackFor = Exception.class)
    public void markUnpaid(Long taskId, String remark) {
        DmsPayment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<DmsPayment>()
                        .eq(DmsPayment::getTaskId, taskId)
                        .eq(DmsPayment::getStatus, 0) // 待支付
                        .orderByDesc(DmsPayment::getCreateTime)
                        .last("LIMIT 1")
        );

        if (payment == null) {
            // 没有待支付记录则新建一条
            DmsTask task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new DmsBusinessException("任务不存在: " + taskId);
            }
            payment = new DmsPayment();
            payment.setTaskId(taskId);
            payment.setTenantId(task.getTenantId());
            payment.setAuditStatus(0);
        }

        payment.setStatus(2); // 未付标记
        payment.setUnpaidRemark(remark);
        if (payment.getId() != null) {
            paymentMapper.updateById(payment);
        } else {
            paymentMapper.insert(payment);
        }

        log.info("未付标记成功: taskId={}, remark={}", taskId, remark);
    }

    /**
     * 根据任务ID获取收款记录
     *
     * @param taskId 任务ID
     * @return 收款记录
     */
    public DmsPayment getByTaskId(Long taskId) {
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<DmsPayment>()
                        .eq(DmsPayment::getTaskId, taskId)
                        .orderByDesc(DmsPayment::getCreateTime)
                        .last("LIMIT 1")
        );
    }
}
