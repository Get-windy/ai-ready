package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.entity.Party;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
            + "party_type, legal_person, status, phone, email, remark, create_time, update_time, deleted) "
            + "VALUES (#{p.id}, 0, #{p.partyCode}, #{p.unifiedCode}, #{p.partyName}, #{p.shortName}, "
            + "#{p.partyType}, #{p.legalPerson}, COALESCE(#{p.status}, 1), #{p.phone}, #{p.email}, #{p.remark}, "
            + "now(), now(), 0) "
            + "ON CONFLICT (id) DO UPDATE SET "
            + "party_code = EXCLUDED.party_code, unified_code = EXCLUDED.unified_code, "
            + "party_name = EXCLUDED.party_name, short_name = EXCLUDED.short_name, "
            + "party_type = EXCLUDED.party_type, legal_person = EXCLUDED.legal_person, "
            + "status = EXCLUDED.status, phone = EXCLUDED.phone, email = EXCLUDED.email, "
            + "remark = EXCLUDED.remark, update_time = now() "
            + "WHERE party.deleted = 0")
    int upsertParty(@Param("p") Party p);

    /** 软删 `party`（与 `biz_party` 的逻辑删除同步）。 */
    @Update("UPDATE party SET deleted = 1, update_time = now() WHERE id = #{id} AND deleted = 0")
    int softDeleteParty(@Param("id") Long id);

    /**
     * 补一条贸易边（幂等）：方向 + 该方向上的商务条件（只写 `party_tenant` 已有的列）。
     *
     * <p>⚠️ `ON CONFLICT` 的后半段 `WHERE deleted = 0` **不能省**：目标是一个**部分**唯一索引，
     * PostgreSQL 要求冲突目标与索引谓词一致。</p>
     */
    @Insert("INSERT INTO party_tenant (tenant_id, party_id, direction, party_level, "
            + "settlement_type, settlement_days, credit_limit, price_track_enabled, status, "
            + "create_time, update_time, deleted) "
            + "VALUES (#{tenantId}, #{partyId}, #{direction}, #{p.partyLevel}, "
            + "#{p.settlementType}, #{p.settlementDays}, #{p.creditLimit}, #{p.priceTrackEnabled}, "
            + "COALESCE(#{p.status}, 1), now(), now(), 0) "
            + "ON CONFLICT (tenant_id, party_id, direction) WHERE deleted = 0 DO UPDATE SET "
            + "party_level = EXCLUDED.party_level, settlement_type = EXCLUDED.settlement_type, "
            + "settlement_days = EXCLUDED.settlement_days, credit_limit = EXCLUDED.credit_limit, "
            + "price_track_enabled = EXCLUDED.price_track_enabled, update_time = now()")
    int upsertEdge(@Param("tenantId") Long tenantId, @Param("partyId") Long partyId,
                   @Param("direction") String direction, @Param("p") Party p);

    /** 软删某主体的全部贸易边（与主体一起消失）。 */
    @Update("UPDATE party_tenant SET deleted = 1, update_time = now() "
            + "WHERE party_id = #{partyId} AND deleted = 0")
    int softDeleteEdges(@Param("partyId") Long partyId);
}
