package cn.aiedge.erp.finance.mapper;

import cn.aiedge.erp.finance.dto.PartySettlementProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 「往来单位结算档案」只读查询 —— 「**租户内**交易」的结算口径来源。
 *
 * <h2>为什么财务模块可以直接读 biz_party</h2>
 * 本仓已有先例：{@code PartnerLedgerServiceImpl} / {@code AuxBalanceMapper} 都直接读
 * {@code biz_party}（同库、只读、按主键或名称精确过滤）。走表而不是走模块依赖，
 * 是为了不让 erp-finance 反向依赖 erp-partner（同 {@code CounterpartyTenantMapper} 的取舍）。
 *
 * <h2>⚠️ 两个必须自己写、不能指望插件的条件</h2>
 * <ol>
 *   <li><b>{@code tenant_id}</b>：本仓实测「手写 SQL 不走多租户插件」，条件得自己写。
 *       这里**恰恰要**按会话租户过滤 —— 档案口径只在"这笔交易的两端同属一个租户"时才适用，
 *       读别的租户的档案就是把别人的信用政策套到自己的账上。</li>
 *   <li><b>{@code deleted = 0}</b>：同理，手写 SQL 不套逻辑删除。漏了会把已删档案读出来。</li>
 * </ol>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface PartySettlementProfileMapper {

    /**
     * 按 {@code biz_party.id} + 会话租户取结算档案。
     *
     * <p>⚠️ 别名**必须加双引号**：PostgreSQL 会把**不加引号的**标识符折叠成小写
     * （`AS settlementType` 实际返回列名 `settlementtype`）。
     * 本仓 `application-dev.yml` 并未开 `map-underscore-to-camel-case`
     * （只在 `application-local.yml` 里开了），所以不能指望下划线自动转换 ——
     * 加引号后无论哪种配置都映射得对。</p>
     *
     * @param partyId  往来单位主键（{@code biz_party.id}）
     * @param tenantId 会话租户（**必须**由调用方给出，见类注释）
     * @return 档案行；不存在（或已软删、或不属于该租户）时返回 {@code null} ——
     *         调用方据此走"现款现结"兜底，<b>不要</b>编一个默认天数
     */
    @Select("SELECT settlement_type AS \"settlementType\", credit_days AS \"creditDays\", "
            + "payment_days AS \"paymentDays\" "
            + "FROM biz_party WHERE id = #{partyId} AND tenant_id = #{tenantId} AND deleted = 0")
    PartySettlementProfile selectByPartyId(@Param("partyId") Long partyId, @Param("tenantId") Long tenantId);
}
