package cn.aiedge.erp.marketing.service;

import cn.aiedge.erp.marketing.entity.PointsBatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 会员积分台账（批次 + 流水）：积分有效期的闭环实现。
 *
 * <p>口径：
 * <ul>
 *   <li><b>获得</b>：按 {@code mkt_member_config.points_valid_months} 生成到期时间（0/NULL＝永不过期），写批次 + EARN 流水</li>
 *   <li><b>使用</b>：<b>FIFO</b> 先进先出消耗各批次剩余（先到期的先用），写 USE 流水</li>
 *   <li><b>过期</b>：把 {@code expire_time < now} 且仍有剩余的批次置 EXPIRED 并清零，写 EXPIRE 流水</li>
 *   <li><b>到期提醒</b>：列出近 N 天内到期且仍有剩余的会员（供营销自动化触达）</li>
 * </ul>
 * 批次剩余之和恒等于账户可用积分（由调用方保证二者同步）。</p>
 */
public interface PointsLedgerService {

    /** 台账：某会员的批次列表（按到期时间升序） */
    List<PointsBatch> listBatches(String memberCardNo);

    /**
     * 记一笔积分获得（写批次 + EARN 流水）
     *
     * @return 新建批次 id
     */
    Long earn(String memberCardNo, Long partnerId, BigDecimal points, String source, String billNo);

    /**
     * 记一笔积分使用（FIFO 扣减批次 + USE 流水）
     *
     * @param points 正数表示要扣减的积分数
     * @return 实际扣减的积分数（额度不足时按可用额度扣，不产生负数）
     */
    BigDecimal use(String memberCardNo, BigDecimal points, String billNo);

    /** 手工调整（ADJUST，可正可负；负数同样走 FIFO） */
    BigDecimal adjust(String memberCardNo, Long partnerId, BigDecimal points, String remark);

    /**
     * 执行过期（把已到期批次清零并写 EXPIRE 流水）。由定时任务或页面「立即执行过期」触发。
     *
     * @return 过期处理的明细（会员卡号 → 过期积分）
     */
    Map<String, BigDecimal> expireDue(LocalDate asOf);

    /** 近 N 天内到期且仍有剩余的批次（到期提醒 / 沉睡唤醒数据源） */
    List<PointsBatch> expiringSoon(int days);

    /** 某会员当前可用积分（各有效批次剩余之和） */
    BigDecimal available(String memberCardNo);
}
