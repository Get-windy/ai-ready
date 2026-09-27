package cn.aiedge.erp.party.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 往来单位信用（额度 / 欠款）专用 Mapper。
 *
 * <p><b>归属说明（2026-09-26）</b>：信用是**往来单位（{@code biz_party}）**的属性，不是 CRM 客户的属性。
 * 此前这套能力挂在 CRM 的 {@code CustomerCreditService} 上、按 {@code crm_customer.id} 取数，
 * 而调用方 {@code erp-sales} 的销售出库传的是**往来单位 ID** —— ID 域错位，信用判断落在错误的客户上。
 * 现已整体迁到 ERP（本模块）。</p>
 *
 * <p><b>为什么这里会读 finance 的表</b>：欠款的唯一事实源是应收表
 * （{@code finance_receivable.remaining_amount}）。本 Mapper 只做**一条只读 SUM**，
 * 不引入 {@code erp-finance} 的任何 Java 类依赖（避免模块耦合与循环依赖），
 * 也不写 finance 的任何数据 —— 只把汇总结果写回本模块自己的 {@code biz_party.current_debt}。</p>
 */
@Mapper
public interface PartyCreditMapper {

    /**
     * 某往来单位当前未收的应收余额合计。
     *
     * <p>{@code finance_receivable.customer_id} 是 varchar，存的是往来单位 ID 的文本形式
     * （由 {@code SalesAccountingService#createReceivableOnShipment} 传入
     * {@code SaleOutbound.customerId}），故这里用 {@code CAST} 对齐。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COALESCE(SUM(remaining_amount), 0) FROM finance_receivable "
            + "WHERE customer_id = CAST(#{partyId} AS varchar) "
            + "AND status = 'normal' AND deleted_flag = 0")
    BigDecimal sumRemainingReceivable(@Param("partyId") Long partyId);

    /** 把重算结果写回往来单位（信用数据的主人就是本模块，这里写自己的表） */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE biz_party SET current_debt = #{debt}, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{partyId} AND deleted = 0")
    int updateCurrentDebt(@Param("partyId") Long partyId, @Param("debt") BigDecimal debt);

    /** 所有配置了信用额度、且未删除的往来单位 ID（批量重算的扫描集） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id FROM biz_party WHERE deleted = 0 AND credit_limit IS NOT NULL AND credit_limit > 0")
    List<Long> selectPartiesWithCreditLimit();
}
