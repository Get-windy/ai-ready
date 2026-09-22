package cn.aiedge.erp.sale.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 「对方主体（客户）所属租户」的只读查询 —— 跨模块只读主数据 {@code biz_party}。
 *
 * <h2>本 Mapper 存在的唯一理由：取到对方主体所属租户</h2>
 * 「协议账期 → 应收到期日」的判定需要知道<b>两端租户</b>（协议是跨租户契约，
 * 见 {@code cn.aiedge.base.credit.CreditTermQuery}）。本端租户取当前会话租户，
 * 而<b>对方（客户）所属租户</b>只能来自 {@code biz_party.tenant_id} ——
 * 销售出库单上只有 {@code customer_id}，没有任何租户快照。
 *
 * <h2>为什么必须 {@code @InterceptorIgnore(tenantLine = "true")}</h2>
 * {@code biz_party} 是租户维度表、<b>不在</b> {@code IGNORE_TENANT_TABLES} 清单里，
 * 全局租户拦截器会给每条查询注入 {@code AND tenant_id = <会话租户>}。
 * 本查询要解决的恰恰是「对方属于<b>另一个</b>租户」的情形：
 * 不跳过注入时，跨租户的客户行根本读不到（返回 {@code null}），
 * 协议账期将<b>永远</b>只能走回退 —— 同租户交易本就查不到协议（协议只在租户对之间成立），
 * 于是这条链路会变成一截永远不亮的死线。
 *
 * <p>与 {@code SupplierSnapshotMapper}（erp-purchase）、
 * {@code AgreementMapper#selectPartyNames}（agreement）同属「跨模块只读主数据」，
 * 口径一致：按主键精确过滤，只读一列，不构成访问控制。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface CounterpartyTenantMapper {

    /**
     * 按 {@code biz_party.id} 取该主体所属租户。
     *
     * <p>注意 {@code tenant_id} 的现状语义（2026-09-22 真库核实）：
     * 它是「这条往来单位档案归哪个租户所有」，取值 {@code 1}（业务租户）或 {@code 0}
     * （平台级公共档案，例如散客 {@code WALKIN}）。取不到行时返回 {@code null}，
     * 调用方据此按「身份不足」处理（不查协议、走回退），<b>不许</b>猜一个租户顶替。</p>
     *
     * @param partyId {@code biz_party.id}
     * @return 所属租户 ID；档案不存在（或已软删）时为 {@code null}
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT tenant_id FROM biz_party WHERE id = #{partyId} AND deleted = 0")
    Long selectTenantIdByPartyId(@Param("partyId") Long partyId);
}
