package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.entity.Party;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * **双写**用的镜像表语句（`party` / `party_tenant` ← `biz_party`）。
 *
 * <h3>为什么是手写 SQL 而不是 BaseMapper</h3>
 * 这两张表是阶段 3 的新载体，实体与 Mapper 尚未建立；而且双写要的语义是
 * **幂等 upsert**（"已存在就刷、不存在就插"），`BaseMapper` 的 `insert/update` 表达不了。
 *
 * <h3>⚠️ 手写 SQL 的两条本仓纪律（都实踩过）</h3>
 * <ol>
 *   <li><b>租户条件要自己写</b>：手写 SQL **不走**多租户插件。
 *       `party` 是**共享层**（`tenant_id` 恒 0，已登记 {@code IGNORE_TENANT_TABLES}）⇒ 插 0；
 *       `party_tenant` 是租户维度表、**不**在忽略清单里 ⇒ 它的 `tenant_id` 必须显式写对
 *       （= 该主体档案所属租户），否则又变成"表建了却读不到"；</li>
 *   <li><b>逻辑删除要自己写</b>：手写 SQL 不套 {@code @TableLogic} ⇒ 所有查询/更新都带 `deleted = 0`。</li>
 * </ol>
 *
 * <h3>幂等键</h3>
 * `party` 用主键 `id`（与 `biz_party.id` 一一对应）；
 * `party_tenant` 用唯一索引 {@code uk_party_tenant_edge (tenant_id, party_id, direction) WHERE deleted = 0}
 * —— 注意 PG 的部分唯一索引在 `ON CONFLICT` 里**必须重复其谓词**，见下面的写法。
 */
@Mapper
public interface PartyMirrorMapper {

    /**
     * 写入/刷新 `party` 主档（幂等）。
     *
     * <p>只写**已落到 `party` 的那几列**（A 组里的单值属性）；未迁的列不碰。</p>
     *
     * <p>⚠️ 若 `unified_code` 与**别的主体**重复，会撞 {@code uk_party_unified_code} 而抛错 ——
     * 那是唯一约束存在的意义（防重复主体）。调用方（{@code PartyMirrorWriter}）按约定
     * **捕获并记 ERROR、不阻断建档**，由对账脚本兜底。</p>
     */
    @Insert("INSERT INTO party (id, tenant_id, party_code, unified_code, party_name, short_name, "
            + "company_full_name, mnemonic_code, party_type, legal_person, phone, email, website, fax, "
            + "legal_person_phone, status, remark, create_time, update_time, deleted) "
            + "VALUES (#{p.id}, 0, #{p.partyCode}, #{p.unifiedCode}, #{p.partyName}, #{p.shortName}, "
            + "#{p.companyFullName}, #{p.mnemonicCode}, #{p.partyType}, #{p.legalPerson}, #{p.phone}, "
            + "#{p.email}, #{p.website}, #{p.fax}, #{p.legalPersonPhone}, COALESCE(#{p.status}, 1), "
            + "#{p.remark}, now(), now(), 0) "
            + "ON CONFLICT (id) DO UPDATE SET "
            + "party_code = EXCLUDED.party_code, unified_code = EXCLUDED.unified_code, "
            + "party_name = EXCLUDED.party_name, short_name = EXCLUDED.short_name, "
            + "company_full_name = EXCLUDED.company_full_name, mnemonic_code = EXCLUDED.mnemonic_code, "
            + "party_type = EXCLUDED.party_type, legal_person = EXCLUDED.legal_person, "
            + "phone = EXCLUDED.phone, email = EXCLUDED.email, website = EXCLUDED.website, "
            + "fax = EXCLUDED.fax, legal_person_phone = EXCLUDED.legal_person_phone, "
            + "status = EXCLUDED.status, remark = EXCLUDED.remark, update_time = now() "
            + "WHERE party.deleted = 0")
    int upsertParty(@Param("p") Party p);

    /** 软删 `party`（与 `biz_party` 的逻辑删除同步）。 */
    @Update("UPDATE party SET deleted = 1, update_time = now() WHERE id = #{id} AND deleted = 0")
    int softDeleteParty(@Param("id") Long id);

    /**
     * 补一条贸易边（幂等）：方向 + **该方向**的商务条件（B 组，批 2 / `V11.520.0` 补齐）。
     *
     * <p>⚠️ `ON CONFLICT` 的后半段 `WHERE deleted = 0` **不能省**：目标是一个**部分**唯一索引，
     * PostgreSQL 要求冲突目标与索引谓词一致。</p>
     *
     * <h3>⚠️ 三条口径在写之前必须掰开</h3>
     * <ol>
     *   <li><b>账期不可传递（裁定 ⑲）</b>：`credit_limit` `current_debt` `credit_days`
     *       `fixed_credit_day` 是 **SALE 独有**；`payment_days` `fixed_payment_day` 是
     *       **PURCHASE 独有**。这里用 `CASE WHEN #{direction} = … END` 显式把它们**钉在自己的方向**上，
     *       另一方向落 NULL。判据与完整归位表见 `V11.520.0` 头部
     *       （`credit_days` = 应收 / `payment_days` = 应付，由
     *       `BusinessAccountingServiceImpl.resolveIntraTenantDueDate` 的
     *       `receivableSide ? creditDays : paymentDays` 定性）。
     *       **不要**图省事改成"照抄到每条边" —— 那正是 ⑲ 明令禁止的。</li>
     *   <li><b>`price_track_enabled` 是 `boolean`</b>，而 `biz_party.price_track_enabled` 是
     *       `integer`（0/1）。直接传整型会被 PG 拒掉：
     *       「字段 price_track_enabled 的类型为 boolean，但表达式的类型为 integer」——
     *       而这一拒的后果远不止"边没写上"，见 {@code PartyMirrorWriter} 的保存点说明。
     *       ⇒ 这里用 `COALESCE(…, 0) <> 0` 显式转成布尔（NULL 按"未开"处理）。</li>
     *   <li><b>`settlement_type` 要转文案</b>：源是 {@code V9.14.0} 的**历史两值整数**
     *       （`0` = 现结；非 0 = 有账期），目标是 `varchar(32)` ⇒ 写本模块正在用的词表
     *       `'现结'`/`'挂账'`。⚠️ 与协议侧 `SETTLEMENT_TYPE` 那套五项枚举
     *       （`CASH_SPOT`/`CREDIT`/…）**不是同一个东西**（`PartySettlementProfile` 类注释有明确警告），
     *       两边**不许混用**。口径定案写在 `V11.520.0` 头部。</li>
     * </ol>
     */
    @Insert("INSERT INTO party_tenant (tenant_id, party_id, direction, party_level, "
            + "settlement_type, settlement_days, statement_day, settlement_day, payment_term_type, "
            + "price_track_enabled, status, "
            + "credit_limit, current_debt, credit_days, fixed_credit_day, "
            + "payment_days, fixed_payment_day, "
            + "default_handler_id, default_handler_name, promoter_id, promoter_name, "
            + "buyer_account, customer_source, roles, category_id, warehouse_name, last_trade_time, "
            + "create_time, update_time, deleted) "
            + "VALUES (#{tenantId}, #{partyId}, #{direction}, #{p.partyLevel}, "
            + "CASE WHEN #{p.settlementType} IS NULL THEN NULL "
            + "     WHEN #{p.settlementType} = 0 THEN '现结' ELSE '挂账' END, "
            + "#{p.settlementDays}, #{p.statementDay}, #{p.settlementDay}, #{p.paymentTermType}, "
            + "COALESCE(#{p.priceTrackEnabled}, 0) <> 0, COALESCE(#{p.status}, 1), "
            // ↓↓ 方向绑定（⑲ 账期不可传递）：只有本方向的那条边才拿得到值，另一边**显式 NULL** ↓↓
            + "CASE WHEN #{direction} = 'SALE'     THEN #{p.creditLimit}     END, "
            + "CASE WHEN #{direction} = 'SALE'     THEN #{p.currentDebt}     END, "
            + "CASE WHEN #{direction} = 'SALE'     THEN #{p.creditDays}      END, "
            + "CASE WHEN #{direction} = 'SALE'     THEN #{p.fixedCreditDay}  END, "
            + "CASE WHEN #{direction} = 'PURCHASE' THEN #{p.paymentDays}     END, "
            + "CASE WHEN #{direction} = 'PURCHASE' THEN #{p.fixedPaymentDay} END, "
            // ↓↓ 逐边照抄（源表单值、尚无按方向消费的代码；见 V11.520.0 头部说明）↓↓
            + "#{p.defaultHandlerId}, #{p.defaultHandlerName}, #{p.promoterId}, #{p.promoterName}, "
            + "#{p.buyerAccount}, #{p.customerSource}, #{p.roles}, #{p.categoryId}, "
            + "#{p.warehouseName}, #{p.lastTradeTime}, "
            + "now(), now(), 0) "
            + "ON CONFLICT (tenant_id, party_id, direction) WHERE deleted = 0 DO UPDATE SET "
            + "party_level = EXCLUDED.party_level, settlement_type = EXCLUDED.settlement_type, "
            + "settlement_days = EXCLUDED.settlement_days, statement_day = EXCLUDED.statement_day, "
            + "settlement_day = EXCLUDED.settlement_day, "
            + "payment_term_type = EXCLUDED.payment_term_type, "
            + "price_track_enabled = EXCLUDED.price_track_enabled, status = EXCLUDED.status, "
            + "credit_limit = EXCLUDED.credit_limit, current_debt = EXCLUDED.current_debt, "
            + "credit_days = EXCLUDED.credit_days, fixed_credit_day = EXCLUDED.fixed_credit_day, "
            + "payment_days = EXCLUDED.payment_days, fixed_payment_day = EXCLUDED.fixed_payment_day, "
            + "default_handler_id = EXCLUDED.default_handler_id, "
            + "default_handler_name = EXCLUDED.default_handler_name, "
            + "promoter_id = EXCLUDED.promoter_id, promoter_name = EXCLUDED.promoter_name, "
            + "buyer_account = EXCLUDED.buyer_account, customer_source = EXCLUDED.customer_source, "
            + "roles = EXCLUDED.roles, category_id = EXCLUDED.category_id, "
            + "warehouse_name = EXCLUDED.warehouse_name, last_trade_time = EXCLUDED.last_trade_time, "
            + "update_time = now()")
    int upsertEdge(@Param("tenantId") Long tenantId, @Param("partyId") Long partyId,
                   @Param("direction") String direction, @Param("p") Party p);

    /** 软删某主体的全部贸易边（与主体一起消失）。 */
    @Update("UPDATE party_tenant SET deleted = 1, update_time = now() "
            + "WHERE party_id = #{partyId} AND deleted = 0")
    int softDeleteEdges(@Param("partyId") Long partyId);

    /**
     * 软删某主体的**某一个方向**的边 —— 用于"这个方向不再适用"（⑬ 角色可以变）。
     *
     * <p>⚠️ 这条是**必须**有的：只 upsert 适用方向、不清理不再适用的方向，
     * 就会出现"客户改成供应商之后，SALE 边还挂着 `deleted = 0`"的**陈旧边** ——
     * 而这类"**多出来的**"漂移**对账脚本看不见**（它只数"缺贸易边"，不数"多出来的边"），
     * 切读之后这个主体会被当成"既是客户又是供应商"。同一类"写≠删"不对称本轮已踩两次
     * （另一次是子表：见 `PartyMirrorWriter.onDelete`）。</p>
     */
    @Update("UPDATE party_tenant SET deleted = 1, update_time = now() "
            + "WHERE tenant_id = #{tenantId} AND party_id = #{partyId} "
            + "AND direction = #{direction} AND deleted = 0")
    int softDeleteEdge(@Param("tenantId") Long tenantId, @Param("partyId") Long partyId,
                       @Param("direction") String direction);

    // ══════════════════ 三张子表（批 2b / `V11.521.0`） ══════════════════
    //
    // ⚠️ 为什么子表也要双写：批 1 就把它们建好并回填了，但**双写没覆盖** ⇒ 客户表单里
    //    `business_license`/`tax_number`/`bank_name`/`bank_account`/`address` 全是可编辑项，
    //    并存期只要有人填一次**银行账号**，`party_bank` 就永远缺这一行，读路径一造就丢数据。
    //
    // ⚠️ 幂等键与**清空语义**：三张子表在 `V11.521.0` 才补上部分唯一索引
    //    （`uk_party_cert_type` / `uk_party_bank_default` / `uk_party_address_type`），
    //    键的口径与批 1 的回填**一一对应**（否则新旧两条路会造出形状不同的行）。
    //    源列被清空时必须**软删**镜像行 —— 只 upsert 不软删的话，用户把银行账号删掉后
    //    镜像里还留着一条陈旧账户，而这类漂移**对账脚本看不见**（它只比对"缺"不比对"多"）。

    /** 补/刷一张证件（幂等）。`certType` 取 `BUSINESS_LICENSE` / `TAX`（与批 1 回填同一套）。 */
    @Insert("INSERT INTO party_cert (party_id, cert_type, cert_no, valid_to, remark, "
            + "create_time, update_time, deleted) "
            + "VALUES (#{partyId}, #{certType}, #{certNo}, #{validTo}, '双写', now(), now(), 0) "
            + "ON CONFLICT (party_id, cert_type) WHERE deleted = 0 DO UPDATE SET "
            + "cert_no = EXCLUDED.cert_no, valid_to = EXCLUDED.valid_to, update_time = now()")
    int upsertCert(@Param("partyId") Long partyId, @Param("certType") String certType,
                   @Param("certNo") String certNo, @Param("validTo") LocalDate validTo);

    /** 源列被清空 ⇒ 软删该证件，避免遗留学陈旧的一行。 */
    @Update("UPDATE party_cert SET deleted = 1, update_time = now() "
            + "WHERE party_id = #{partyId} AND cert_type = #{certType} AND deleted = 0")
    int softDeleteCert(@Param("partyId") Long partyId, @Param("certType") String certType);

    /** 补/刷**默认**银行账户（幂等）：源表就那一个账户，它天然是默认的那个。 */
    @Insert("INSERT INTO party_bank (party_id, bank_name, bank_account, bank_address, is_default, "
            + "create_time, update_time, deleted) "
            + "VALUES (#{partyId}, #{p.bankName}, #{p.bankAccount}, #{p.bankAddress}, 1, now(), now(), 0) "
            + "ON CONFLICT (party_id) WHERE deleted = 0 AND is_default = 1 DO UPDATE SET "
            + "bank_name = EXCLUDED.bank_name, bank_account = EXCLUDED.bank_account, "
            + "bank_address = EXCLUDED.bank_address, update_time = now()")
    int upsertDefaultBank(@Param("partyId") Long partyId, @Param("p") Party p);

    /** 银行信息被清空 ⇒ 软删默认账户。 */
    @Update("UPDATE party_bank SET deleted = 1, update_time = now() "
            + "WHERE party_id = #{partyId} AND is_default = 1 AND deleted = 0")
    int softDeleteDefaultBank(@Param("partyId") Long partyId);

    /**
     * 补/刷**注册地址**（幂等）：`address_type = 1`。
     *
     * <p>⚠️ `latitude`/`longitude` **本方法不写**（保持 NULL）：这两列 `Party` 实体根本
     * 没有映射、全仓也没有消费方（实测仅 1 行有值）⇒ 想在双写里带上它们，得先给实体补字段；
     * 在那之前"不写"比"写个死值"诚实。批 1 回填曾按源表搬过一次，故存量行可能有值。</p>
     */
    @Insert("INSERT INTO party_address (party_id, address_type, province, city, district, "
            + "detail_address, is_default, remark, create_time, update_time, deleted) "
            + "VALUES (#{partyId}, 1, #{p.province}, #{p.city}, #{p.district}, #{p.address}, "
            + "1, '双写', now(), now(), 0) "
            + "ON CONFLICT (party_id, address_type) WHERE deleted = 0 DO UPDATE SET "
            + "province = EXCLUDED.province, city = EXCLUDED.city, district = EXCLUDED.district, "
            + "detail_address = EXCLUDED.detail_address, update_time = now()")
    int upsertPrimaryAddress(@Param("partyId") Long partyId, @Param("p") Party p);

    /** 地址信息被清空 ⇒ 软删注册地址。 */
    @Update("UPDATE party_address SET deleted = 1, update_time = now() "
            + "WHERE party_id = #{partyId} AND address_type = 1 AND deleted = 0")
    int softDeletePrimaryAddress(@Param("partyId") Long partyId);
}
