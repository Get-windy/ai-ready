package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 协议版本 Mapper。
 *
 * <p><b>为什么插入必须走手写 SQL（不能用 {@code BaseMapper.insert}）</b>：
 * {@code snapshot_json} 是 PostgreSQL **jsonb** 列，而 JDBC 驱动默认以 varchar 发送字符串参数，
 * PG 会直接报「column "snapshot_json" is of type jsonb but expression is of type character varying」。
 * 这里显式 {@code CAST(#{snapshotJson} AS jsonb)} 绕开类型推断（本仓未开 {@code stringtype=unspecified}，
 * 且不打算为一个模块改全局连接串）。</p>
 *
 * <p><b>更新一律走 {@code BaseMapper} 的按字段更新（lambdaUpdate / 只填要改的字段的 updateById）</b>：
 * 这样 {@code snapshot_json} 永远不会出现在 UPDATE 语句里 —— 这是"已生效版本快照不可改"的
 * 结构性保证之一（另一重是服务层显式拒绝，见 {@code AgreementServiceImpl#updateDraft}）。</p>
 */
@Mapper
public interface AgreementVersionMapper extends BaseMapper<AgreementVersion> {

    /**
     * 插入版本（{@code snapshot_json} 显式 CAST 成 jsonb）。
     *
     * <p>协商时间线与变更单语义的五列也在这里落库（{@code V11.490.0} 新增）：
     * 它们回答"这一版是谁提的、从哪一版改出来的"，必须在**插入那一刻**就有值，
     * 否则协商时间线里会出现没有归属的版本行。</p>
     */
    @Insert("INSERT INTO agreement_version (id, agreement_id, version_no, snapshot_json, status, "
            + "party_a_confirmed_by, party_a_confirmed_at, party_a_sign_hash, "
            + "party_b_confirmed_by, party_b_confirmed_at, party_b_sign_hash, "
            + "effective_from, effective_to, change_reason, "
            + "proposed_by_side, proposed_by_person, proposal_note, origin_version_id, no_retroactive_note, "
            + "create_by, create_time, update_by, update_time, deleted) "
            + "VALUES (#{v.id}, #{v.agreementId}, #{v.versionNo}, CAST(#{v.snapshotJson} AS jsonb), #{v.status}, "
            + "#{v.partyAConfirmedBy}, #{v.partyAConfirmedAt}, #{v.partyASignHash}, "
            + "#{v.partyBConfirmedBy}, #{v.partyBConfirmedAt}, #{v.partyBSignHash}, "
            + "#{v.effectiveFrom}, #{v.effectiveTo}, #{v.changeReason}, "
            + "#{v.proposedBySide}, #{v.proposedByPerson}, #{v.proposalNote}, "
            + "#{v.originVersionId}, #{v.noRetroactiveNote}, "
            + "#{v.createBy}, now(), #{v.createBy}, now(), 0)")
    int insertVersion(@Param("v") AgreementVersion v);

    /**
     * 补写某草稿版本的**协商时间线**信息：这一版是谁提的、协商留言说了什么。
     *
     * <p>为什么单独一个方法：反要约（§13.4）是"先复用既有的「发起变更」链路造出 DRAFT 版本，
     * 再把'谁提的、说了什么'补上"。变更单语义（{@code origin_version_id} 与
     * {@code no_retroactive_note}）由 {@link #insertVersion} 在插入那一刻就写好，
     * 这里**不重复写**，避免两个地方各写一遍、以哪个为准说不清。</p>
     *
     * <p>{@code WHERE ... AND status = 0} 与 {@link #rewriteDraftSnapshot} 同一道结构性防线 ——
     * 已生效版本写不进去，受影响行数为 0，而不是静默改了举证材料。</p>
     */
    @Update("UPDATE agreement_version SET "
            + "proposed_by_side = #{v.proposedBySide}, "
            + "proposed_by_person = #{v.proposedByPerson}, "
            + "proposal_note = #{v.proposalNote}, "
            + "update_by = #{v.updateBy}, update_time = now() "
            + "WHERE id = #{v.id} AND deleted = 0 AND status = 0")
    int updateProposal(@Param("v") AgreementVersion v);

    /**
     * 重写草稿版本的快照正文（条款或期限变了才调用）。
     *
     * <p><b>WHERE 里的 {@code status = 0} 是不变量 1 的结构性防线</b>：
     * 即使服务层哪天漏判，已生效（status=1）版本的 snapshot_json 也**写不进去** ——
     * 受影响行数会是 0，而不是静默改掉举证材料。</p>
     *
     * <p>本方法同时清空双方确认痕迹：双签针对的是**内容**，内容一变，
     * 原有的"我确认过"对新内容即失效（否则会出现 A 确认后被偷改条款仍算确认齐全）。</p>
     */
    @Update("UPDATE agreement_version SET "
            + "snapshot_json = CAST(#{v.snapshotJson} AS jsonb), "
            + "effective_from = #{v.effectiveFrom}, effective_to = #{v.effectiveTo}, "
            + "party_a_confirmed_by = NULL, party_a_confirmed_at = NULL, party_a_sign_hash = NULL, "
            + "party_b_confirmed_by = NULL, party_b_confirmed_at = NULL, party_b_sign_hash = NULL, "
            + "update_by = #{v.updateBy}, update_time = now() "
            + "WHERE id = #{v.id} AND deleted = 0 AND status = 0")
    int rewriteDraftSnapshot(@Param("v") AgreementVersion v);

    /** 只取快照正文（供"快照是否被改写"的取证与哈希重算使用，避免把整行读成实体再序列化）。 */
    @Select("SELECT snapshot_json::text FROM agreement_version WHERE id = #{id}")
    String selectSnapshotJson(@Param("id") Long id);
}
